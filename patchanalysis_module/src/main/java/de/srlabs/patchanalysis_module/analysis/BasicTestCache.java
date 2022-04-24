package de.srlabs.patchanalysis_module.analysis;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.R;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexContainer;
import de.srlabs.patchanalysis_module.helpers.database.DBHelper;
import de.srlabs.patchanalysis_module.helpers.ProcessHelper;
import de.srlabs.patchanalysis_module.helpers.SharedPrefsHelper;

public class BasicTestCache {
    private final String testSuiteVersion;
    private int apiLevel;
    private static final int TEST_BATCH_SIZE = 1024;
    private static final int TEST_BUNDLE_SIZE = 256;
    LinkedBlockingQueue<TestBundle> testQueue = new LinkedBlockingQueue<TestBundle>();
    LinkedBlockingQueue<BasicTestResult> resultQueue = new LinkedBlockingQueue<BasicTestResult>();
    private SharedPreferences sharedPrefs;
    private DBHelper database;
    private PatchanalysisService service;
    int progressTotal = 0;
    int progressDone = 0;
    final Object progressLock = new Object();
    private HashMap<String,String> exceptionsByTestId = new HashMap<String, String>();
    private ProgressItem progressItem;
    private Runnable finishedRunnable = null;
    private volatile boolean stopTesting = false;
    private HashMap<String,Boolean> cacheResults;
    private MasterWorkingThread masterWorkingThread;

    public BasicTestCache(PatchanalysisService service, String testSuiteVersion, int apiLevel){
        this.testSuiteVersion = testSuiteVersion;
        this.apiLevel = apiLevel;
        this.service = service;
        this.sharedPrefs = SharedPrefsHelper.getSharedPrefs(service);
        this.database = new DBHelper(service);
        long currentBuildDate = TestUtils.getBuildDateUtc();


        // Invalidate cached results if the build fingerprint or the build timestamp (seconds since 1970) changes => Make sure that teste are repeated after a firmware upgrade
        // Also invalidate cache and rerun test if this app is updated.
        if(currentBuildDate != sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE, -1) ||
                !TestUtils.getBuildFingerprint().equals(sharedPrefs.getString(SharedPrefsHelper.KEY_BUILD_FINGERPRINT, "INVALID")) ||
                !TestUtils.getBuildDisplayName().equals(sharedPrefs.getString(SharedPrefsHelper.KEY_BUILD_DISPLAY_NAME, "INVALID")) ||
                ! ( Constants.APP_VERSION == sharedPrefs.getInt(SharedPrefsHelper.KEY_BUILD_APPVERSION, -1) )
                ) {
            clearCache();
        }
        cacheResults = new HashMap<String,Boolean>();
    }
    public void clearCache(){
        Log.i(Constants.LOG_TAG, "BasicTestCache.clearCache() called");
        SharedPrefsHelper.resetSharedPrefs(service);
        resetDBInformation();
        clearTemporaryTestResultCache();
    }

    public void clearTemporaryTestResultCache(){
        if(cacheResults != null)
            cacheResults.clear();
    }

    private void resetDBInformation() {
        // set all basic test exceptions back and reset all the result int values to -1
        database.resetAllBasicTests();
    }

    public Boolean getOrExecute(String uuid) throws JSONException, IOException {
        //check if we queried this result from the DB already
        if(cacheResults.containsKey(uuid)){
            //Log.d(Constants.LOG_TAG,"Found temp. cached basic test result.");
            return cacheResults.get(uuid);
        }
        if(uuid.startsWith("!")){
            Boolean subtestResult = getOrExecute(uuid.substring(1));
            if(subtestResult == null) {
                cacheResult(uuid,null);
                return null;
            }
            cacheResult(uuid,!subtestResult);
            return !subtestResult;
        }
        JSONObject basicTest = database.getBasicTestByUUID(uuid);
        if (basicTest == null){
            Log.d(Constants.LOG_TAG,"Could not find basicTest " + uuid + " in db");
            cacheResult(uuid, null);
            return null;
        }
        if(basicTest.has("exception") && !basicTest.getString("exception").equals("")) {
            exceptionsByTestId.put(uuid, basicTest.getString("exception"));
            cacheResult(uuid,null);
            return null;
        }
        Boolean result =  basicTest.isNull("result") ? null : basicTest.getBoolean("result");
        cacheResult(uuid,result);
        return result;
    }

    public void cacheResult(String uuid, Boolean result){
        if(cacheResults == null){
            cacheResults = new HashMap<String,Boolean>();
        }
        cacheResults.put(uuid,result);
    }

    public void startTesting(ProgressItem progressItem, Runnable finishedRunnable){
        this.progressItem = progressItem;
        this.finishedRunnable = finishedRunnable;
        progressTotal = database.getNumberOfTotalNotPerformedTests();
        Log.d(Constants.LOG_TAG,""+progressTotal+" basicTests to be performed!");
        progressDone = 0;

        Log.i(Constants.LOG_TAG, "Setting progressTotal to " + progressTotal);
        startWorking();

    }

    public void startWorking() {
        stopTesting = false;
        masterWorkingThread = new MasterWorkingThread(4);
        masterWorkingThread.start();
    }

    public int getQueueSize(){
        return testQueue.size();
    }

    public JSONObject toJson() throws IOException, JSONException {
        JSONObject result = new JSONObject();
        Vector<String> uuids = database.getAllBasicTestsUUIDs();
        if (uuids == null) {
            throw new NoTestsAvailableException("No basic tests in the DB for this API level");
        }

        for(String uuid : uuids){
            Boolean subtestResult = getOrExecute(uuid);
            if(subtestResult == null) {
                result.put(uuid, JSONObject.NULL);
            } else {
                result.put(uuid, subtestResult);
            }
        }
        JSONObject exceptionsJson = new JSONObject(exceptionsByTestId);
        result.put("exceptions", exceptionsJson);
        result.put("testSuiteVersion", testSuiteVersion);
        result.put("apiLevel", apiLevel);
        return result;
    }

    /**
     * This thread is going to handle the DB connection and fill the working
     */
    class MasterWorkingThread extends Thread{

        private int nThreads = 0;
        Vector<RegularWorkingThread> workingThreads = null;

        public MasterWorkingThread(int nThreads){
            this.nThreads = nThreads;
        }

        @Override
        public void run(){

            addNewThreads();
            int testBatchSize = addNextMissingTestBatch();

            while(true){
                if(stopTesting){
                    break;
                }
                //check if results queue contains all results
                database.getDBInstance().beginTransaction();
                try {
                    for(int i=0; i < testBatchSize; i++) {
                        try {
                            BasicTestResult testResult = resultQueue.take();
                            if (testResult.getException() != null) {
                                if (!testResult.getException().contains("File does not exist")) {
                                    Log.d(Constants.LOG_TAG,"TestResult: "+testResult.getBasicTestUUID()+" exception:"+testResult.getException());
                                }
                                database.addTestExceptionToDB(testResult.getBasicTestUUID(), testResult.getException());
                            } else {
                                //Log.d(Constants.LOG_TAG,"TestResult: "+testResult.getBasicTestUUID()+" result:"+testResult.getResult());
                                database.addTestResultToDB(testResult.getBasicTestUUID(), testResult.getResult());
                            }
                            updateTotalProgress();
                        }catch(InterruptedException e){
                            Log.e(Constants.LOG_TAG,"InterruptedException in MasterWorkingThread.run():"+e.getMessage());
                        }
                    }
                    database.getDBInstance().setTransactionSuccessful();
                } finally {
                    database.getDBInstance().endTransaction();
                }
                testBatchSize = addNextMissingTestBatch();
                //Log.d(Constants.LOG_TAG,"testBatchSize:"+testBatchSize);

                if(testBatchSize == 0){
                    break;
                }
            }
            terminateThreads();
            database.closeDB();

            if(!stopTesting) {
                progressItem.update(1.0, "Finished testing! Preparing results...");
                service.finishedBasicTests();
                if(finishedRunnable != null) {
                    finishedRunnable.run();
                }
            }
        }

        private void terminateThreads(){
          Log.d(Constants.LOG_TAG,"terminating threads...");
          for(int i =0; i < nThreads; i++){
                  testQueue.add(TestBundle.getStopMarker());
          }
          for(RegularWorkingThread thread : workingThreads){
              thread.interrupt();
          }
          Log.d(Constants.LOG_TAG, "Shut down all worker threads.");
        }

        private void addNewThreads() {
            workingThreads = new Vector<>();
            resultQueue = new LinkedBlockingQueue<BasicTestResult>();
            Log.d(Constants.LOG_TAG,"Starting test threads:"+nThreads);
            for (int i = 0; i < nThreads; i++) {
                RegularWorkingThread thread = new RegularWorkingThread(service,testQueue,resultQueue);
                thread.start();
                workingThreads.add(thread);
            }
        }

        public int addNextMissingTestBatch() {
            //Log.d(Constants.LOG_TAG,"adding new test bundles to testQueue!");
            int result = 0;
            Vector<JSONObject> basicTestBatch = database.getNotPerformedTestsSortedByFilenameAndTestType(TEST_BATCH_SIZE);
            if (basicTestBatch != null) {
                String lastFilename = "";
                TestBundle currentTestBundle = null;
                TestBundle testsWithoutFilename = new TestBundle(null);
                for (JSONObject basicTest : basicTestBatch) {
                    try {
                        //Log.d(Constants.LOG_TAG,"Adding basic test to bundle: "+basicTest.toString());
                        if (!basicTest.has("filename") && !basicTest.has("dexPath")) {
                            testsWithoutFilename.add(basicTest);//FIXME restrict size of this as well!!!
                        } else {
                            String currentFilename = "";
                            if (basicTest.has("filename")) {
                                currentFilename = basicTest.getString("filename");
                            } else {
                                currentFilename = basicTest.getString("dexPath").split(":")[1];
                            }

                            if (!lastFilename.equals(currentFilename)) {
                                // Log.d(Constants.LOG_TAG,"Creating new test bundle: " + currentFilename);
                                lastFilename = currentFilename;

                                if (currentTestBundle != null) {
                                    //add current testBundle to queue
                                    testQueue.add(currentTestBundle);
                                }
                                //create new testbundle
                                currentTestBundle = new TestBundle(currentFilename);
                            }
                            // Log.d(Constants.LOG_TAG,"Adding basic test to bundle: "+currentTestBundle.getFilename());
                            currentTestBundle.add(basicTest);
                            if (currentTestBundle.getTestCount() == TEST_BUNDLE_SIZE) {
                                testQueue.add(currentTestBundle);
                                currentTestBundle = new TestBundle(currentFilename);
                            }
                        }
                        result++;
                    } catch(Exception e){
                        Log.e(Constants.LOG_TAG, "Exception when creating test bundle for test queue:", e);
                    }
                }
                synchronized (testQueue) {
                    if(currentTestBundle != null && currentTestBundle.getTestCount() > 0){
                        testQueue.add(currentTestBundle);
                    }
                    testQueue.add(testsWithoutFilename);
                }
            }
            return result;
        }

        private void updateTotalProgress() {
            synchronized (progressLock){
                progressDone += 1;
            }
            //Log.i(Constants.LOG_TAG, "BasicTestCache Thread evaluated " + uuid + " to " + result + "  progressDone=" + progressDone + "  progressTotal=" + progressTotal);
            if(progressTotal == 0){
                progressItem.update(1.0, service.getResources().getString(R.string.pa_status_finished_performing_basic_tests));
            } else {
                progressItem.update(((double) progressDone) / ((double) progressTotal), service.getResources().getString(R.string.pa_status_performing_basic_tests));
            }
        }

    }

    /**
     * These threads will work through the test queue, pick a basic test execute it and report back the result to the masterworking thread
     */
    class RegularWorkingThread extends Thread{

        private Context context = null;
        private LinkedBlockingQueue<TestBundle> tasks = null;
        private Queue<BasicTestResult> results = null;

        public RegularWorkingThread(Context context, LinkedBlockingQueue<TestBundle> tasks, Queue<BasicTestResult> results){
            this.context = context;
            this.tasks = tasks;
            this.results = results;
        }

        @Override
        public void run(){
            while(true){
                try {
                    TestBundle bundle = tasks.take();
                    Set<JSONObject> rollingSignatureTests = null;
                    if(bundle.isStopMarker()) {
                        Log.d(Constants.LOG_TAG," shutting down worker thread..");
                        return;
                    }
                    if(bundle.getFilename() != null){
                        bundle.checkTargetFileExists();
                    }
                    for (JSONObject basicTest : bundle.getBasicTests()) {
                        try {
                            //collect all rolling signature tests
                            if (basicTest.getString("testType") == "ROLLING_SIGNATURE") {
                                if (rollingSignatureTests == null) {
                                    rollingSignatureTests = new HashSet<>();
                                }
                                rollingSignatureTests.add(basicTest);
                                continue;
                            }

                            BasicTestResult result = performTest(bundle, basicTest);
                            synchronized (results) {
                                results.add(result);
                            }
                        }catch (JSONException e){
                            Log.e(Constants.LOG_TAG, "Basic test has no testType info:"+ basicTest);
                        }
                    }

                    //execute all rolling signature test for the same file now
                    if(rollingSignatureTests != null && rollingSignatureTests.size() > 0){
                        Set<BasicTestResult> testResults = TestEngine.performCollectedRollingSignatureTests(bundle, rollingSignatureTests);
                        for(BasicTestResult testResult : testResults){
                            synchronized (results){
                                results.add(testResult);
                            }
                        }
                    }

                }catch(InterruptedException e){
                    Log.d(Constants.LOG_TAG,"InterruptedException while dequeuing from tasks: "+e.getMessage());
                }
            }
        }

        private DexContainer getCachedDexContainer(JSONObject basicTest, TestBundle bundle) throws JSONException, IOException {
            if (!bundle.dexCache.getFileHasBeenProcessed()) {
                DexContainer dexContainer = null;
                Boolean isValid = true;
                try {
                    dexContainer = TestEngine.getDexContainer(basicTest);
                } catch (IOException | JSONException e) {
                    isValid = null;
                } catch (Exception e) {
                    isValid = false;
                }
                bundle.dexCache.setIsValid(isValid);
                bundle.dexCache.setContainer(dexContainer);
                bundle.dexCache.setFileHasBeenProcessed(true);
            }
            return bundle.dexCache.getContainer();
        }

        private BasicTestResult performTest(TestBundle bundle, JSONObject basicTest){
            try {
                // run basic test and add result to result queue
                //Log.d(Constants.LOG_TAG," working on task: "+basicTest.toString());
                Boolean result = null;
                String exception = null;

                try {
                    String testType = basicTest.getString("testType");
                    String filepath = "";
                    if(bundle.getFilename() != null){
                        if (basicTest.has("filename")) {
                            filepath = basicTest.getString("filename");
                        } else {
                            filepath = basicTest.getString("dexPath").split(":")[1];
                        }
                        //if target file is missing, skip all testing and return test specific result immediately
                        if(!bundle.isTargetFileExisting()) {
                            switch (testType) {
                                case "FILE_EXISTS":
                                    return new BasicTestResult(basicTest.getString("uuid"), false, null);
                                case "FILE_CONTAINS_SUBSTRING":
                                case "XZ_CONTAINS_SUBSTRING":
                                    return new BasicTestResult(basicTest.getString("uuid"), null, null);
                                default:
                                    return new BasicTestResult(basicTest.getString("uuid"), null, "File does not exist: " + filepath);
                            }
                        }
                    }
                    // all optimized tests here:
                    //      and all the information cached temporarily to aggregate test requirements and avoid e.g. redundant objdump calls
                    switch (testType) {
                        case "MASK_SIGNATURE_SYMBOL":
                            if (bundle.getSymbolTable() == null) {
                                String currentFilename = basicTest.getString("filename");
                                bundle.setSymbolTable(TestUtils.readSymbolTable(currentFilename));
                            }
                            result = TestEngine.runMaskSignatureTest(basicTest, bundle.getSymbolTable());
                            break;
                        case "BINARY_CONTAINS_SYMBOL":
                            if (bundle.getObjdumpLines() == null) {
                                String currentFilename = basicTest.getString("filename");
                                bundle.setObjdumpLines(ProcessHelper.runObjdumpCommand("-tT", currentFilename));
                            }
                            result = TestEngine.runBinaryContainsSymbolTest(basicTest, bundle.getObjdumpLines());
                            break;
                        case "DISAS_FUNCTION_CONTAINS_STRING":
                            if (bundle.getObjdumpLines() == null) {
                                String currentFilename = basicTest.getString("filename");
                                bundle.setObjdumpLines(ProcessHelper.runObjdumpCommand("-tT", currentFilename));
                            }
                            result = TestEngine.runDisasFunctionContainsStringTest(basicTest, bundle.getObjdumpLines());
                            break;
                        case "DISAS_FUNCTION_MATCHES_REGEX":
                            if (bundle.getObjdumpLines() == null) {
                                String currentFilename = basicTest.getString("filename");
                                bundle.setObjdumpLines(ProcessHelper.runObjdumpCommand("-tT", currentFilename));
                            }
                            result = TestEngine.runDisasFunctionMatchesRegexTest(basicTest, bundle.getObjdumpLines());
                            break;
                        case "DEX_IS_VALID":
                            if (!bundle.dexCache.getFileHasBeenProcessed()) {
                                Boolean isValid = true;
                                DexContainer dexContainer = null;
                                try {
                                    dexContainer = TestEngine.getDexContainer(basicTest);
                                } catch (IOException | JSONException e) {
                                    isValid = null;
                                } catch (Exception e) {
                                    isValid = false;
                                }
                                bundle.dexCache.setIsValid(isValid);
                                bundle.dexCache.setContainer(dexContainer);
                                bundle.dexCache.setFileHasBeenProcessed(true);
                            }
                            result = bundle.dexCache.getIsValid();
                            break;
                        case "DEX_CONTAINS_CLASS":
                            result = TestEngine.evaluateDexContainsClass(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_CLASS_CONTAINS_METHOD":
                            result = TestEngine.evaluateDexClassContainsMethod(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_CLASS_CONTAINS_STATIC_FIELD":
                            result = TestEngine.evaluateDexClassContainsStaticField(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_CLASS_CONTAINS_INSTANCE_FIELD":
                            result = TestEngine.evaluateDexClassContainsInstanceField(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_STATIC_FIELD_FLAGS":
                            result = TestEngine.evaluateDexStaticFieldFlags(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_INSTANCE_FIELD_FLAGS":
                            result = TestEngine.evaluateDexInstanceFieldsFlags(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_METHOD_FLAGS":
                            result = TestEngine.evaluateDexMethodFlags(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_METHOD_HAS_CODE":
                            result = TestEngine.evaluateDexMethodHasCode(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        case "DEX_METHOD_SIGNATURE":
                            result = TestEngine.evaluateDexMethodSignature(basicTest,
                                    getCachedDexContainer(basicTest, bundle));
                            break;
                        default:
                            result = TestEngine.executeBasicTest(context, basicTest);
                            break;
                    }
                } catch(OutOfMemoryError e) {
                    Log.e(Constants.LOG_TAG,"OutOfMemoryError in performTest: " + e.getMessage());
                    service.handleFatalErrorViaCallback(service.getResources().getString(R.string.patchanalysis_error_out_of_memory));
                } catch (Exception e) {
                    exception = e.getMessage();
                }
                if (exception == null) {
                    Boolean val = null;
                    if (result != null && result) {
                        val = true;
                    } else if (result != null && !result) {
                        val = false;
                    }
                    return new BasicTestResult(basicTest.getString("uuid"), val, null);

                } else {
                    return new BasicTestResult(basicTest.getString("uuid"), null, exception);
                }
            }catch(JSONException e){
                Log.e(Constants.LOG_TAG, "working thread:"+e.getMessage());
                return null;
            }
        }
    }

    public class NoTestsAvailableException extends RuntimeException {
        public NoTestsAvailableException(String errorMessage) {
            super(errorMessage);
        }
    }
}
