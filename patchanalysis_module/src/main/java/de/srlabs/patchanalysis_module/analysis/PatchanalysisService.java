package de.srlabs.patchanalysis_module.analysis;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.ITestExecutorCallbacks;
import de.srlabs.patchanalysis_module.ITestExecutorDashboardCallbacks;
import de.srlabs.patchanalysis_module.ITestExecutorServiceInterface;
import de.srlabs.patchanalysis_module.R;
import de.srlabs.patchanalysis_module.helpers.database.DBHelper;
import de.srlabs.patchanalysis_module.helpers.database.PADatabaseManager;
import de.srlabs.patchanalysis_module.helpers.database.PASQLiteOpenHelper;
import de.srlabs.patchanalysis_module.util.CertifiedBuildChecker;
import de.srlabs.patchanalysis_module.util.FoundInvalidBasicTestException;
import de.srlabs.patchanalysis_module.util.ServerApi;
import de.srlabs.patchanalysis_module.views.PatchanalysisSumResultChart;
import de.srlabs.patchanalysis_module.helpers.NotificationHelper;
import de.srlabs.patchanalysis_module.helpers.SharedPrefsHelper;

public class PatchanalysisService extends Service {
    private JSONObject deviceInfoJson = null;
    private TestSuite testSuite = null;
    private DeviceInfoThread deviceInfoThread = null;
    private BasicTestCache basicTestCache = null;
    private static ITestExecutorCallbacks patchanalysisMainActivityCallback = null;
    private static ITestExecutorDashboardCallbacks dashboardActivityCallback = null;
    private boolean apiRunning = false;
    private boolean basicTestsRunning = false;
    private boolean deviceInfoRunning = false;
    private boolean downloadingTestSuite = false;
    private Handler handler = null;
    private ServerApi api = null;
    private Vector<ProgressItem> progressItems;
    public static final String NO_INTERNET_CONNECTION_ERROR = "no_uplink";
    private boolean isAnalysisRunning = false;
    private long currentAnalysisTimestamp;
    public static AppFlavor APP_FLAVOR;
    private long timeBeforeTesting;
    private int secondsNeededForTesting = 0;
    private PowerManager.WakeLock wakeLock;
    private String lastStatusMessage = null;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.LOG_TAG,"onCreate() of PatchanalysisService called...");

        handler = new Handler();
        api = new ServerApi();

        APP_FLAVOR = AppFlavor.getAppFlavor();

        initDatabase();

        notificationHelper = new NotificationHelper(PatchanalysisService.this, AppFlavor.getAppFlavor());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SnoopSnitch Patchanalysis");
    }

    private void initDatabase(){
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(Constants.LOG_TAG,"Creating SQLite database...");
                    PADatabaseManager.initializeInstance(new PASQLiteOpenHelper(PatchanalysisService.this));
                    PADatabaseManager paDatabaseManager = PADatabaseManager.getInstance();
                    SQLiteDatabase db = paDatabaseManager.openDatabaseReadOnly();
                    //testing DB init
                    Cursor cursor = db.rawQuery("SELECT * FROM basictests", null);
                    Log.d(Constants.LOG_TAG,"Got "+cursor.getCount()+" test entries!");
                    cursor.close();
                    paDatabaseManager.closeDatabase();

                }catch(SQLException e){
                    // Testing if the DB creation worked successfully failed
                    Log.e(Constants.LOG_TAG,"DB creation failed, maybe App assets are corrupted: "+ e.getMessage());
                }
            }
        };
        t.start();
    }

    private void doWorkAsync() {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (Constants.IS_TEST_MODE) {
                        startWork(true, true, true, false, false);
                    } else {
                        startWork(true, true, true, true, true);
                    }
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, "startTest Exception", e);
                }
            }
        };
        t.start();
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.LOG_TAG,"PatchanalysisService.onDestroy called");
        isAnalysisRunning = false;
    }

    protected void cancelAnalysis() {
        Log.d(Constants.LOG_TAG,"PatchanalysisService.cancelAnalysis called");

        //close DB
        DBHelper dbHelper = new DBHelper(this);
        dbHelper.closeDB();

        stopForeground(true);
        stopSelf();
        System.exit(0);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void parseTestSuiteFile(File testSuiteFile,final ProgressItem parseTestSuiteProgress) throws IOException{
        Log.d(Constants.LOG_TAG,"PatchanalysisService: Parsing testsuite...");
        Log.d(Constants.LOG_TAG, "PatchanalysisService: testSuiteFile:" + testSuiteFile.getAbsolutePath());
        testSuite = new TestSuite(this, testSuiteFile);
        parseTestSuiteProgress.update(0.1, PatchanalysisService.this.getResources().getString(R.string.pa_status_start_fetching_test_chunks));
        try{
            testSuite.parseInfoFromJSON();
        } catch (FoundInvalidBasicTestException e){
            Log.e(Constants.LOG_TAG, "Found invalid basicTest", e);
            handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_invalid_basic_test));
        }
        parseTestSuiteProgress.update(1.0, PatchanalysisService.this.getResources().getString(R.string.pa_status_finished_parsing_testsuite));
        Log.i(Constants.LOG_TAG,"Parsing testsuite and additional data chunks finished.");
        basicTestCache = new BasicTestCache(this, testSuite.getVersion(), Build.VERSION.SDK_INT);
        Log.d(Constants.LOG_TAG,"PatchanalysisService: Finished parsing testsuite!");
    }

    private boolean assertAppIsUpToDate(){
        if(testSuite != null && testSuite.getMinAppVersion() != -1){
            int minAppVersion = testSuite.getMinAppVersion();
            Log.i(Constants.LOG_TAG, "assertAppIsUpToDate(): Found minAppVersion=" + minAppVersion);

            boolean outdated = minAppVersion > Constants.APP_VERSION;
            if(outdated){
                Log.i(Constants.LOG_TAG, "Outdated app version! minAppVersion=" + minAppVersion);
                String errorMessage = "<p style=\"font-weight:bold;\">"+PatchanalysisService.this.getResources().getString(R.string.patchanalysis_new_version_available_heading)
                        +"</p>"+ PatchanalysisService.this.getResources().getString(R.string.patchanalysis_new_version_available_instructions);
                handleFatalErrorViaCallback(errorMessage);
            }
            return outdated;
        } else{
            return false;
        }
    }

    private final ITestExecutorServiceInterface.Stub mBinder = new ITestExecutorServiceInterface.Stub() {
        @Override
        public void updateCallback(final ITestExecutorCallbacks callback){
            //Log.d(Constants.LOG_TAG,"Updating callbacks.");
            PatchanalysisService.patchanalysisMainActivityCallback = callback;
            if (isAnalysisRunning) {
                updateProgress(lastStatusMessage);
            }
        }

        @Override
        public void updateDashboardCallback(final ITestExecutorDashboardCallbacks callback){
            //Log.d(Constants.LOG_TAG,"Updating callbacks.");
            PatchanalysisService.dashboardActivityCallback = callback;
        }

        @Override
        public void requestCancelAnalysis() {
            PatchanalysisService.this.cancelAnalysis();
        }

        @Override
        public boolean isAnalysisRunning() {
            return isAnalysisRunning;
        }
    };

    private String evaluateVulnerabilitiesTests() throws RemoteException{
        try {
            Log.d(Constants.LOG_TAG, "Starting to create result JSON...");
            boolean is64BitSystem = TestUtils.is64BitSystem();
            JSONObject result = new JSONObject();

            if(testSuite == null)
                return null;

            Log.i(Constants.LOG_TAG,"Creating result overview...");
            JSONObject vulnerabilities = null;
            try {
                vulnerabilities = testSuite.getVulnerabilities();
            } catch(IOException e){
                Log.e(Constants.LOG_TAG, "Exception in evaluateVulnerabilitiesTests", e);
                handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                return e.toString();
            }
            Iterator<String> identifierIterator = vulnerabilities.keys();
            Vector<String> identifiers = new Vector<String>();

            while (identifierIterator.hasNext()) {
                identifiers.add(identifierIterator.next());
            }
            Collections.sort(identifiers);
            Log.d(Constants.LOG_TAG, "number of vulnerabilities to test: " + identifiers.size());

            for (String identifier : identifiers) {
                JSONObject vulnerability = vulnerabilities.getJSONObject(identifier);

                try {
                    Boolean testRequires64Bit = vulnerability.getBoolean("testRequires64bit");
                    if (!is64BitSystem && testRequires64Bit) {
                        //test will not be possible; skip test
                        continue;
                    }
                } catch (JSONException e) {
                    //ignoring exception here, as the old test might not come with this info
                }

                String category = vulnerability.getString("category");
                JSONObject test_not_affected = vulnerability.getJSONObject("testNotAffected");
                Boolean notAffected = TestEngine.runTest(basicTestCache, test_not_affected);

                JSONObject vulnerabilityResult = new JSONObject();
                if(vulnerability.has("patchlevelDate"))
                    vulnerabilityResult.put("patchlevelDate",vulnerability.getString("patchlevelDate"));
                vulnerabilityResult.put("identifier", identifier);
                vulnerabilityResult.put("title", vulnerability.getString("title"));
                vulnerabilityResult.put("notAffected", notAffected);
                if(vulnerability.has("optional"))
                    vulnerabilityResult.put("optional", vulnerability.getBoolean("optional"));
                if (notAffected == null || !notAffected) {
                    JSONObject testVulnerable = vulnerability.getJSONObject("testVulnerable");
                    Boolean vulnerable = TestEngine.runTest(basicTestCache, testVulnerable);
                    vulnerabilityResult.put("vulnerable", vulnerable);
                    JSONObject testFixed = vulnerability.getJSONObject("testFixed");
                    Boolean fixed = TestEngine.runTest(basicTestCache, testFixed);
                    vulnerabilityResult.put("fixed", fixed);
                }
                if (!result.has(category)) {
                    result.put(category, new JSONArray());
                }
                result.getJSONArray(category).put(vulnerabilityResult);
            }

            basicTestCache.clearTemporaryTestResultCache();
            PatchanalysisSumResultChart.setResultToDrawFromOnNextUpdate(result);

            return SharedPrefsHelper.saveAnalysisResult(result, PatchanalysisService.this.isBuildCertified(), PatchanalysisService.this);

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Exception in evaluateVulnerabilitiesTests", e);
            handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_error_creating_result_overview));
            return e.toString();
        }
    }

    private void startWork(boolean updateTests, boolean generateDeviceInfo, final boolean evaluateTests, final boolean uploadTestResults, final boolean uploadDeviceInfo){

        if(downloadingTestSuite){
            Log.i(Constants.LOG_TAG,"Still downloading test suite...please be patient!");
            return;
        }
        if(assertAppIsUpToDate()){
            return;
        }
        if(apiRunning){
            Log.i(Constants.LOG_TAG,"Already work in progress, not starting: apiRunning");
            return;
        }
        if(basicTestsRunning) {
            Log.i(Constants.LOG_TAG,"Already work in progress, not starting: basicTestsRunning");
            return;
        }
        if(deviceInfoRunning){
            Log.i(Constants.LOG_TAG,"Already work in progress, not starting: deviceInfoRunning");
            return;
        }

        clearProgress();
        updateProgress(null);

        //acquire wake lock
        wakeLock.acquire();

        final CertifiedBuildChecker certifiedBuildChecker = CertifiedBuildChecker.getInstance();

        //prepare progressitems
        final ProgressItem uploadDeviceInfoProgress;
        if(uploadDeviceInfo) {
            uploadDeviceInfoProgress = addProgressItem("uploadDeviceInfo", 2.0);
        } else{
            uploadDeviceInfoProgress = null;
        }

        final ProgressItem uploadTestResultsProgress;
        if(uploadTestResults) {
            uploadTestResultsProgress = addProgressItem("uploadTestResults", 1.0);
        } else{
            uploadTestResultsProgress = null;
        }

        final ProgressItem basicTestsProgress;
        if(evaluateTests) {
            basicTestsProgress = addProgressItem("basicTests", 6.0);
        } else{
            basicTestsProgress = null;
        }

        final ProgressItem downloadTestSuiteProgress;
        final ProgressItem parseTestSuiteProgress;
        if(updateTests){
            downloadTestSuiteProgress = addProgressItem("downloadTestSuite", 1);
            parseTestSuiteProgress = addProgressItem("parseTestSuite",1.5);
        }
        else{
            downloadTestSuiteProgress = null;
            parseTestSuiteProgress = null;
        }

        updateProgress(null);

        ProgressItem downloadRequestsProgress = addProgressItem("downloadRequests", 0.5);
        Thread requestsThread = new RequestsThread(downloadRequestsProgress, certifiedBuildChecker);
        requestsThread.start();

        //prepare finish runnables
        final Runnable pendingTestResultsUploadRunnable = new Runnable(){
            @Override
            public void run() {
                basicTestsRunning = false;
                try{
                    if(uploadTestResults){
                        apiRunning = true;
                        if(!isConnectedToInternet()){
                            handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                            return;
                        }
                        Log.i(Constants.LOG_TAG,"Reporting test results to server...");
                        JSONObject results = basicTestCache.toJson();
                        results.put("timeNeeded", secondsNeededForTesting);
                        api.reportTest(results, getAppId(), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(),
                                TestUtils.getBuildDateUtc(), Constants.APP_VERSION, certifiedBuildChecker.getCtsProfileMatchResponse(), certifiedBuildChecker.getBasicIntegrityResponse());
                        Log.i(Constants.LOG_TAG,"Uploading test results finished...");
                        uploadTestResultsProgress.update(1.0, null);
                        apiRunning = false;
                    }
                } catch(IOException | IllegalStateException e){
                    reportError(NO_INTERNET_CONNECTION_ERROR);
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                    Log.e(Constants.LOG_TAG, "Exception in pendingTestResultsUploadRunnable", e);
                    apiRunning = false;
                } catch( JSONException e){
                    reportError(NO_INTERNET_CONNECTION_ERROR);
                    Log.e(Constants.LOG_TAG,"JSONException in pendingTestResultsUploadRunnable: "+e.getMessage());
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                    apiRunning = false;
                } catch(OutOfMemoryError e) {
                    Log.e(Constants.LOG_TAG,"OutOfMemoryError in pendingTestResultsUploadRunnable: "+e.getMessage());
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_error_out_of_memory));
                } catch (BasicTestCache.NoTestsAvailableException e) {
                    Log.e(Constants.LOG_TAG,"There are no basic tests in the DB: "+e.getMessage());
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_no_tests_available));
                }
            }
        };

        final Runnable pendingDeviceInfoUploadRunnable = new Runnable(){
            @Override
            public void run() {
                basicTestsRunning = false;
                try{
                    if(uploadDeviceInfo){
                        apiRunning = true;
                        if (deviceInfoJson != null) {
                            if(!isConnectedToInternet()){
                                handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                                return;
                            }
                            Context context = getApplicationContext();
                            SharedPreferences sharedPrefs = SharedPrefsHelper.getPersistentSharedPrefs(context);
                            String updateInfo = sharedPrefs.getString(SharedPrefsHelper.KEY_UPDATE_INFO, "{}");
                            api.reportSys(deviceInfoJson, getAppId(), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(),
                                    TestUtils.getBuildDateUtc(), Constants.APP_VERSION, certifiedBuildChecker.getCtsProfileMatchResponse(), certifiedBuildChecker.getBasicIntegrityResponse(), updateInfo);
                        }
                        Log.i(Constants.LOG_TAG,"Uploading device info finished...");
                        uploadDeviceInfoProgress.update(1.0, null);
                        apiRunning = false;
                    }
                } catch(IllegalStateException | IOException e){
                    reportError(NO_INTERNET_CONNECTION_ERROR);
                    Log.e(Constants.LOG_TAG, "Exception in pendingDeviceInfoUploadRunnable()", e);
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                    apiRunning = false;
                }
            }
        };

        if(generateDeviceInfo) {
            ProgressItem deviceInfoProgress = addProgressItem("deviceInfo", 2);
            // Run sysinfo in background
            if (deviceInfoThread != null && deviceInfoThread.isAlive()) {
                return;
            }
            if (deviceInfoJson != null)
                deviceInfoJson = null;
            deviceInfoThread = new DeviceInfoThread(deviceInfoProgress, pendingDeviceInfoUploadRunnable);
            Log.i(Constants.LOG_TAG, "Starting DeviceInfoThread");
            deviceInfoRunning = true;
            deviceInfoThread.start();
        }

        if(updateTests){
            Thread downloadAndParseTestSuiteThread = new DownloadThread(downloadTestSuiteProgress, parseTestSuiteProgress,evaluateTests, basicTestsProgress, pendingTestResultsUploadRunnable);
            downloadAndParseTestSuiteThread.start();
        } else{
            if(evaluateTests){
                Log.i(Constants.LOG_TAG, "Calling basicTestCache.startTesting()");
                timeBeforeTesting = System.currentTimeMillis();
                secondsNeededForTesting = 0;
                basicTestCache.startTesting(basicTestsProgress, pendingTestResultsUploadRunnable);
            }
        }
    }

    private void checkIfCVETestsAvailable(TestSuite testSuite) {
        if(testSuite != null){
            String noCVETestsMessage = testSuite.getNoCVETestMessage();
            if(noCVETestsMessage != null){
                //show dialog in UI after testing
                showNoCVETestsForApiLevel(noCVETestsMessage);
            }
        }
        else{
            Log.e(Constants.LOG_TAG,"checkIfCVETestsAvailable: testSuite is null");
        }
    }

    public void finishedBasicTests(){
        secondsNeededForTesting = (int)((System.currentTimeMillis()-timeBeforeTesting)/1000);
        Log.i(Constants.LOG_TAG,"Finished performing basic tests in "+secondsNeededForTesting+" s");
        timeBeforeTesting = 0;
        //vulnerabilitiesJSONResult = getJSONFromVulnerabilitiesResults();
    }

    private void clearProgress(){
        this.progressItems = new Vector<ProgressItem>();
    }

    private ProgressItem addProgressItem(String name, double weight){
        //Log.d(Constants.LOG_TAG,"Adding progressItem: "+name+" weight:"+weight);
        ProgressItem item = new ProgressItem(this, name, weight);
        progressItems.add(item);
        return item;
    }

    private double getTotalProgress(){
        double weightSum = 0;
        double progressSum = 0;
        if(progressItems == null){
            return 0;
        }
        for(ProgressItem progressItem : progressItems){
            //Log.i(Constants.LOG_TAG, "getTotalProgres(): name=" + progressItem.getName() + "  weight=" + progressItem.getWeight() + "  progress=" + progressItem.getProgress());
            weightSum += progressItem.getWeight();
            progressSum += progressItem.getProgress() * progressItem.getWeight();
        }
        double totalProgress;
        if(weightSum == 0){
            totalProgress = 0;
        } else {
            totalProgress = progressSum / weightSum;
        }
        //Log.i(Constants.LOG_TAG, "getTotalProgres() returns "+ totalProgress);
        return totalProgress;
    }

    public void updateProgress(String statusMessage) {
        if (statusMessage != null) {
            lastStatusMessage = statusMessage;
        }
        double totalProgress = getTotalProgress();
        sendProgressToCallback(totalProgress, statusMessage);
        if(totalProgress == 1.0) {
            onFinishedAnalysis();
        }
    }

    private boolean isBuildCertified() {
        CertifiedBuildChecker certifiedBuildChecker = CertifiedBuildChecker.getInstance();
        Boolean basicIntegrity = certifiedBuildChecker.getBasicIntegrityResponse();
        Boolean ctsProfileMatch = certifiedBuildChecker.getCtsProfileMatchResponse();
        return basicIntegrity == Boolean.TRUE && ctsProfileMatch == Boolean.TRUE;
    }

    private void onFinishedAnalysis() {

        if(wakeLock.isHeld())
            wakeLock.release();

        String analysisResultString = null;
        try {
            analysisResultString = evaluateVulnerabilitiesTests();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        isAnalysisRunning = false;
        sendFinishedToCallback(analysisResultString, isBuildCertified());

        notificationHelper.showAnalysisFinishedNotification();

        stopForeground(true);
        stopSelf();
    }

    private void sendFinishedToCallback(final String analysisResultString, final boolean isBuildCertified){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (dashboardActivityCallback != null) {
                        dashboardActivityCallback.finished(analysisResultString, isBuildCertified, PatchanalysisService.this.currentAnalysisTimestamp);
                    }
                } catch (RemoteException e) {
                    Log.i(Constants.LOG_TAG, "PatchanalysisService.updateProgress() => dashboardActivityCallback.sendFinishedToCallback() RemoteException");
                }

                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.finished(analysisResultString, isBuildCertified, PatchanalysisService.this.currentAnalysisTimestamp);
                    }
                } catch (RemoteException e) {
                    Log.i(Constants.LOG_TAG, "PatchanalysisService.updateProgress() => patchanalysisMainActivityCallback.sendFinishedToCallback() RemoteException");
                }
            }
        });
    }

    // Calling this will cause the service to be killed
    public void handleFatalErrorViaCallback(final String stickyErrorMessage){
        SharedPrefsHelper.saveStickyErrorMessage(stickyErrorMessage, PatchanalysisService.this);
        notificationHelper.showAnalysisFailedNotification();
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.handleFatalError(stickyErrorMessage, PatchanalysisService.this.currentAnalysisTimestamp);
                    }
                } catch (RemoteException e) {
                    Log.i(Constants.LOG_TAG, "PatchanalysisService => patchanalysisMainActivityCallback.handleFatalErrorViaCallback() RemoteException");
                }

                try {
                    if (dashboardActivityCallback != null) {
                        dashboardActivityCallback.handleFatalError(stickyErrorMessage, PatchanalysisService.this.currentAnalysisTimestamp);
                    }
                } catch (RemoteException e) {
                    Log.i(Constants.LOG_TAG, "PatchanalysisService => dashboardActivityCallback.handleFatalErrorViaCallback() RemoteException");
                }
                cancelAnalysis();
            }
        });
    }

    private void sendProgressToCallback(final double totalProgress, final String statusMessage){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.updateProgress(totalProgress, statusMessage);
                    }
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "PatchanalysisService.updateProgress() RemoteException: "+ e.getClass());
                }
            }
        });
    }

    private void sendReloadViewStateToCallback(){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.reloadViewState();
                    }
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "PatchanalysisService.sendReloadViewStateToCallback() RemoteException: "+ e.getClass());
                }
            }
        });
    }

    private void reportError(final String error){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.showErrorMessage(error);
                    }
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "PatchanalysisService.reportError() RemoteException: " + e.getClass());
                }
            }
        });
    }

    private void showNoCVETestsForApiLevel(final String message){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    if (patchanalysisMainActivityCallback != null) {
                        patchanalysisMainActivityCallback.showNoCVETestsForApiLevel(message);
                    }
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "PatchanalysisService.showNoCVETestsForApiLevel() RemoteException: " + e.getClass());
                }
            }
        });
    }

    private class DownloadThread extends Thread{
        private ProgressItem downloadProgress;
        private ProgressItem parsingProgress;
        private boolean evaluateTests = false;
        private ProgressItem basicTestsProgress = null;
        private Runnable pendingTestResultsUploadRunnable = null;

        public DownloadThread(ProgressItem downloadProgress, ProgressItem parsingProgress, boolean evaluateTests, ProgressItem basicTestsProgress, Runnable pendingTestResultsUploadRunnable){
            this.evaluateTests = evaluateTests;
            this.downloadProgress = downloadProgress;
            this.parsingProgress = parsingProgress;
            this.basicTestsProgress = basicTestsProgress;
            this.pendingTestResultsUploadRunnable = pendingTestResultsUploadRunnable;
        }

        @Override
        public void run(){
            try {
                Log.i(Constants.LOG_TAG, "Starting to download testsuite");
                apiRunning = true;
                if(!isConnectedToInternet()){
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                    return;
                }
                downloadingTestSuite = true;
                downloadProgress.update(0.1, PatchanalysisService.this.getResources().getString(R.string.pa_status_downloading_testsuite));
                Log.d(Constants.LOG_TAG,"Downloading testsuite from server...");
                File f = api.downloadTestSuite("newtestsuite",PatchanalysisService.this, getAppId(), Build.VERSION.SDK_INT,"0", Constants.APP_VERSION);
                Log.i(Constants.LOG_TAG,"Downloading testsuite finished. Fetching additional data chunks...");
                downloadProgress.update(1.0, PatchanalysisService.this.getResources().getString(R.string.pa_status_finished_downloading_testsuite));
                Log.d(Constants.LOG_TAG,"Finished downloading testsuite JSON to file:"+f.getAbsolutePath());
                downloadingTestSuite = false;
                parseTestSuiteFile(f,parsingProgress);
                if(assertAppIsUpToDate()) {
                    return;
                }
                checkIfCVETestsAvailable(testSuite);

            } catch (IllegalStateException | IOException e) {
                Log.e(Constants.LOG_TAG, "Exception in DownloadThread", e);
                reportError(NO_INTERNET_CONNECTION_ERROR);
                handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                return;
            } finally{
                apiRunning = false;
            }
            Log.i(Constants.LOG_TAG, "Calling basicTestCache.startTesting()");
            if(evaluateTests) {
                basicTestsRunning = true;
                timeBeforeTesting = System.currentTimeMillis();
                secondsNeededForTesting = 0;
                basicTestCache.startTesting(basicTestsProgress, pendingTestResultsUploadRunnable);
            }
        }
    }

    private class DeviceInfoThread extends Thread{
        private ProgressItem progress;
        private Runnable onFinishedRunnable;

        public DeviceInfoThread(ProgressItem progress, Runnable onFinishedRunnable){
            this.progress = progress;
            this.onFinishedRunnable = onFinishedRunnable;
        }

        @Override
        public void run() {
            deviceInfoJson = makeDeviceinfoJson(PatchanalysisService.this, progress);
            deviceInfoThread = null;
            deviceInfoRunning = false;
            if(onFinishedRunnable != null) {
                onFinishedRunnable.run();
            }
        }
    }



    private class RequestsThread extends Thread{
        private ProgressItem downloadRequestsProgress;
        private CertifiedBuildChecker certifiedBuildChecker;
        public RequestsThread(ProgressItem downloadRequestsProgress, CertifiedBuildChecker certifiedBuildChecker){
            this.downloadRequestsProgress = downloadRequestsProgress;
            this.certifiedBuildChecker = certifiedBuildChecker;
        }
        @Override
        public void run(){
            Vector<ProgressItem> requestProgress = new Vector<>();
            try {
                if(!isConnectedToInternet()){
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                    return;
                }
                JSONArray requestsJson = api.getRequests(getAppId(), Build.VERSION.SDK_INT, TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                downloadRequestsProgress.update(1.0, null);
                Log.i(Constants.LOG_TAG,"Downloading requests finished...");
                for(int i=0;i<requestsJson.length();i++){
                    //Log.i(Constants.LOG_TAG, "Adding progress item for request " + i);
                    requestProgress.add(addProgressItem("Request_" + i, 1.0));
                }
                updateProgress(null);
                for(int i=0;i<requestsJson.length();i++){
                    JSONObject request = requestsJson.getJSONObject(i);
                    String requestType = request.getString("requestType");
                    if(requestType.equals("UPLOAD_FILE")){
                        String filename = request.getString("filename");
                        TestUtils.validateFilename(filename);
                        //Log.d(Constants.LOG_TAG,"Uploading file: "+filename);
                        if(!isConnectedToInternet()){
                            handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                            return;
                        }
                        api.reportFile(filename, getAppId(), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(),
                                Constants.APP_VERSION, certifiedBuildChecker.getCtsProfileMatchResponse(), certifiedBuildChecker.getBasicIntegrityResponse());
                        requestProgress.get(i).update(1.0, null);
                        updateProgress(null);
                    } else{
                        Log.e(Constants.LOG_TAG, "Received invalid request type " + requestType);
                        requestProgress.get(i).update(1.0, null);
                        updateProgress(null);
                    }
                }
                Log.i(Constants.LOG_TAG,"Reporting files to server finished...");
            } catch(Exception e){
                Log.e(Constants.LOG_TAG, "RequestsThread.run() exception", e);
                if(e instanceof IOException || e instanceof IllegalStateException) {
                    reportError(NO_INTERNET_CONNECTION_ERROR);
                    handleFatalErrorViaCallback(PatchanalysisService.this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
                }
                downloadRequestsProgress.update(1.0, null);
                for(ProgressItem x:requestProgress){
                    x.update(1.0, null);
                }
                updateProgress(null);
            }
        }
    }

    private boolean isConnectedToInternet(){
        if(!TestUtils.isConnectedToInternet(PatchanalysisService.this)){
            reportError(NO_INTERNET_CONNECTION_ERROR);
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.v(Constants.LOG_TAG, "onStartCommand called");
        if (isAnalysisRunning) {
            return START_NOT_STICKY;
        }

        SharedPrefsHelper.clearSavedAnalysisResult(this);
        SharedPrefsHelper.clearSavedStickyErrorMessage(this);
        isAnalysisRunning = true;
        currentAnalysisTimestamp = System.currentTimeMillis();

        Notification notification = notificationHelper.getAnalysisOngoingNotification();
        startForeground(NotificationHelper.ONGOING_NOTIFICATION_ID, notification);

        sendReloadViewStateToCallback();

        //first this, when finished will start doWorkAsync()
        checkBuildCertifiedAndStartWorking();

        // stopSelf is called in updateProgress when 100% progress has been reached
        return START_NOT_STICKY;
    }

    public void checkBuildCertifiedAndStartWorking(){
        Log.d(Constants.LOG_TAG,"Starting CertifiedBuildChecker test...");
        CertifiedBuildChecker certifiedBuildChecker = CertifiedBuildChecker.getInstance();
        //will call finishCertifiedBuildCheck when finished, independently of the result
        certifiedBuildChecker.startChecking(this);
    }

    public void finishCertifiedBuildCheck(){
        Log.d(Constants.LOG_TAG,"CertifiedBuildChecker is finished testing!");
        doWorkAsync();
    }

    public String getAppId(){
        return TestUtils.getAppId(APP_FLAVOR, PatchanalysisService.this);
    }

    private JSONObject makeDeviceinfoJson(Service service, ProgressItem progressItem) {
        JSONObject info = TestUtils.makeDeviceinfoJson(APP_FLAVOR, service, progressItem);
        try {
            if (info != null) {
                info.put("safetyNetApiNonce", CertifiedBuildChecker.getInstance().getNonceBase64());
                info.put("safetyNetApiResponse", CertifiedBuildChecker.getInstance().getResult());
            }
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "Failed to add SafetyNet API result to device info", e);
        }
        return info;
    }
}
