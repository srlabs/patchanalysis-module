package de.srlabs.patchanalysis_module.analysis;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.CodeItem;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.ApkFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexClass;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexContainer;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.EncodedField;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.EncodedMethod;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.VDexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests_old.BuildPropertyTest;
import de.srlabs.patchanalysis_module.helpers.ProcessHelper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests_old.AslrTest;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests_old.JavaBasicTest;
import de.srlabs.patchanalysis_module.analysis.signatures.MaskSignature;
import de.srlabs.patchanalysis_module.analysis.signatures.MultiSignatureScanner;
import de.srlabs.patchanalysis_module.analysis.signatures.RollingSignature;
import de.srlabs.patchanalysis_module.analysis.signatures.Signature;
import de.srlabs.patchanalysis_module.analysis.signatures.SymbolInformation;

/**
 * This class actually contains all the algorithms to perform basic tests
 * and calculate the results for vulnerabilities (according to their defined logic equation) referencing basic test results
 */
public class TestEngine {

    public static Boolean runTest(BasicTestCache cache, Object testObject) throws JSONException, IOException {
        if (testObject instanceof String) {
            return cache.getOrExecute((String) testObject);
        } else {
            //Log.d(Constants.LOG_TAG,"runTest for "+testObject.toString());
            JSONObject test = (JSONObject) testObject;
            if (!test.has("testType")) {
                throw new JSONException("basictest is missing testType field!");
            }
            String testType = test.getString("testType");
            switch (testType) {
                case "TRUE":
                    return true;
                case "FALSE":
                    return false;
                case "AND": {
                    JSONArray subtests = test.getJSONArray("subtests");
                    boolean nullFound = false;
                    for (int i = 0; i < subtests.length(); i++) {
                        Object subtest = subtests.get(i);
                        Boolean subtestResult = runTest(cache, subtest);
                        if (subtestResult == null)
                            nullFound = true;
                        else if (!subtestResult)
                            return false;
                    }
                    if (nullFound)
                        return null;
                    return true;
                }
                case "NAND": {
                    JSONArray subtests = test.getJSONArray("subtests");
                    boolean nullFound = false;
                    for (int i = 0; i < subtests.length(); i++) {
                        Object subtest = subtests.get(i);
                        Boolean subtestResult = runTest(cache, subtest);
                        if (subtestResult == null)
                            nullFound = true;
                        else if (!subtestResult)
                            return true;
                    }
                    if (nullFound)
                        return null;
                    return false;
                }
                case "OR": {
                    JSONArray subtests = test.getJSONArray("subtests");
                    boolean nullFound = false;
                    for (int i = 0; i < subtests.length(); i++) {
                        Object subtest = subtests.get(i);
                        Boolean subtestResult = runTest(cache, subtest);
                        if (subtestResult == null)
                            nullFound = true;
                        else if (subtestResult == true)
                            return true;
                    }
                    if (nullFound)
                        return null;
                    return false;
                }
                case "NOR": {
                    JSONArray subtests = test.getJSONArray("subtests");
                    boolean nullFound = false;
                    for (int i = 0; i < subtests.length(); i++) {
                        Object subtest = subtests.get(i);
                        Boolean subtestResult = runTest(cache, subtest);
                        if (subtestResult == null)
                            nullFound = true;
                        else if (subtestResult == true)
                            return false;
                    }
                    if (nullFound)
                        return null;
                    return true;
                }
                case "NOT":
                    Object subtest = test.get("subtest");
                    Boolean subtestResult = runTest(cache, subtest);
                    if (subtestResult == null)
                        return null;
                    return !subtestResult;
                default:
                    throw new IllegalArgumentException("Unknown testType " + testType);
            }
        }
    }

    public static Boolean executeBasicTest(Context context, JSONObject test) throws Exception {
        if (Constants.IS_TEST_MODE) {
            File folder = new File(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + "/system/");
            if (!folder.exists()) {
                String errorMessage = "Extracted build does not exist here:" + folder.getAbsolutePath();
                Log.e(Constants.LOG_TAG, errorMessage);
                throw new IOException(errorMessage);
            }
        }

        if (!test.has("testType"))
            throw new IllegalStateException("basic test has no testtype information: " + test.toString());

        String testType = test.getString("testType");
        switch (testType) {
            case "CHIPSET_VENDOR":
                return TestUtils.getChipVendor().equals(test.getString("vendor"));
            case "CHIPSET_VENDOR_OR_UNKNOWN":
                String vendor = TestUtils.getChipVendor();
                return vendor.equals("UNKNOWN") || vendor.equals(test.getString("vendor"));
            case "ANDROID_VERSION_EQUALS":
                String currentAndroidVersion = TestUtils.getAndroidVersion();
                if (currentAndroidVersion == null)
                    return null;
                String androidVersionCompare = test.getString("androidVersion");
                return currentAndroidVersion.equals(androidVersionCompare);
            case "FILE_EXISTS": {
                String filename = test.getString("filename");
                if (Constants.IS_TEST_MODE)
                    filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
                TestUtils.validateFilename(filename);
                File f = new File(filename);
                return f.exists();
            }
            case "FILE_CONTAINS_SUBSTRING": {
                String filename = test.getString("filename");
                if (Constants.IS_TEST_MODE)
                    filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
                byte[] needle;
                if (test.has("substring")) {
                    if (test.has("substringB64"))
                        throw new IllegalArgumentException("Test FILE_CONTAINS_SUBSTRING can only use SUBSTRING or SUBSTRING_B64, not both");
                    needle = test.getString("substring").getBytes();
                } else {
                    needle = Base64.decode(test.getString("substringB64"), 0);
                }
                TestUtils.validateFilename(filename);
                File f = new File(filename);
                if (!f.exists())
                    return null;
                FileInputStream fis = new FileInputStream(f);
                BufferedInputStream bis = new BufferedInputStream(fis, 4096);
                return TestUtils.streamContainsSubstring(bis, needle);
            }
            case "XZ_CONTAINS_SUBSTRING": {
                String filename = test.getString("filename");
                if (Constants.IS_TEST_MODE)
                    filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
                byte[] needle;
                if (test.has("substring")) {
                    if (test.has("substringB64"))
                        throw new IllegalArgumentException("Test XZ_CONTAINS_SUBSTRING can only use SUBSTRING or SUBSTRING_B64, not both");
                    needle = test.getString("substring").getBytes();
                } else {
                    needle = Base64.decode(test.getString("substringB64"), 0);
                }
                TestUtils.validateFilename(filename);
                File f = new File(filename);
                if (!f.exists())
                    return null;
                String[] cmd = new String[3];
                cmd[0] = AppFlavor.getAppFlavor().getBinaryPath() + "libbusybox.so";
                cmd[1] = "xzcat";
                cmd[2] = filename;
                Process p = Runtime.getRuntime().exec(cmd);
                BufferedInputStream bis = new BufferedInputStream(p.getInputStream(), 4096);
                return TestUtils.streamContainsSubstring(bis, needle);
            }
            case "ZIP_CONTAINS_SUBSTRING": {
                String filename = test.getString("zipFile");
                if (Constants.IS_TEST_MODE)
                    filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
                String zipitem = test.getString("zipItem");
                byte[] needle;
                if (filename.equals("/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL01");
                } else if (Constants.IS_TEST_MODE && filename.equals(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + "/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL01");
                }
                if (test.has("substring")) {
                    if (test.has("substringB64"))
                        throw new IllegalArgumentException("Test FILE_CONTAINS_SUBSTRING can only use SUBSTRING or SUBSTRING_B64, not both");
                    needle = test.getString("substring").getBytes();
                } else {
                    needle = Base64.decode(test.getString("substringB64"), 0);
                }
                if (filename.equals("/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL02: needle=" + new String(needle));
                } else if (Constants.IS_TEST_MODE && filename.equals(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + "/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL02: needle=" + new String(needle));
                }
                TestUtils.validateFilename(filename);
                File f = new File(filename);
                if (!f.exists())
                    return null;
                if (filename.equals("/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL03: needle=" + new String(needle));
                } else if (Constants.IS_TEST_MODE && filename.equals(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + "/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL03: needle=" + new String(needle));
                }
                ZipFile zf = new ZipFile(f);
                ZipEntry ze = zf.getEntry(zipitem);
                if (ze == null)
                    return null;
                InputStream is = zf.getInputStream(ze);
                BufferedInputStream bis = new BufferedInputStream(is, 4096);
                boolean result = TestUtils.streamContainsSubstring(bis, needle);
                if (filename.equals("/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL06: needle=" + new String(needle) + "  result=" + result);
                } else if (Constants.IS_TEST_MODE && filename.equals(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + "/system/framework/services.jar")) {
                    //Log.i(Constants.LOG_TAG, "ZIP_CONTAINS_SUBSTRING JL06: needle=" + new String(needle) + "  result=" + result);
                }
                return result;
            }
            case "ZIP_ENTRY_EXISTS": {
                String filename = test.getString("zipFile");
                if (Constants.IS_TEST_MODE)
                    filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
                String zipitem = test.getString("zipItem");
                TestUtils.validateFilename(filename);
                File f = new File(filename);
                if (!f.exists())
                    return null;
                ZipFile zf = new ZipFile(f);
                ZipEntry ze = zf.getEntry(zipitem);
                if (ze == null)
                    return false;
                return true;
            }
            case "BINARY_CONTAINS_SYMBOL":
                return runBinaryContainsSymbolTest(test, null);
            case "DISAS_FUNCTION_CONTAINS_STRING":
                return runDisasFunctionContainsStringTest(test, null);
            case "DISAS_FUNCTION_MATCHES_REGEX":
                return runDisasFunctionMatchesRegexTest(test, null);
            case "MASK_SIGNATURE_SYMBOL":
                return runMaskSignatureTest(test, null);
            case "BUILD_PROP_EQUALS": {
                String buildProperty = test.getString("buildProperty");
                String expectedValue = test.getString("value");

                BuildPropertyTest buildPropertyTest = new BuildPropertyTest(buildProperty, expectedValue);
                return buildPropertyTest.runTest(context);
            }
            case "JAVA_TEST":
                String testClassName = test.getString("testClassName");
                // Make sure that the class name only contains valid characters
                for (int i = 0; i < testClassName.length(); i++) {
                    char c = testClassName.charAt(i);
                    if (c >= 'a' && c <= 'z') {
                        continue;
                    }
                    if (c >= 'A' && c <= 'Z') {
                        continue;
                    }
                    if (c >= '0' && c <= '9') {
                        continue;
                    }
                    if (c == '_') {
                        continue;
                    }
                    throw new IllegalStateException("Invalid character '" + c + "' in  testClassName '" + testClassName + "'");
                }
                // Get an instance of testClassName via Reflection
                // If the test class isn't included, it will throw an Exception (which will be reported to the server).
                Class c = Class.forName(AslrTest.class.getPackage().getName() + "." + testClassName);
                JavaBasicTest javaBasicTest = (JavaBasicTest) c.newInstance();
                // Run the test, it may also throw an Exception.
                return javaBasicTest.runTest(context);
            case "ROLLING_SIGNATURE": {
                String filename = test.getString("filename");
                String rollingSignature = test.getString("rollingSignature");
                RollingSignature signature = getRollingSignatureForTest(test);

                MultiSignatureScanner scanner = new MultiSignatureScanner();
                scanner.addSignatureChecker(signature);

                Set<SymbolInformation> results = scanner.scanFile(filename);
                for (SymbolInformation symbolInformation : results) {
                    if (symbolInformation.getSymbolName().equals(rollingSignature))
                        return true;
                }
                return false;

            }
            case "COMBINED_SIGNATURE": {
                String filename = test.getString("filename");
                String maskSignatureString = test.getString("maskSignature");

                RollingSignature rollingSignature = getRollingSignatureForTest(test);

                MultiSignatureScanner scanner = new MultiSignatureScanner();
                scanner.addSignatureChecker(rollingSignature);


                MaskSignature maskSignatureChecker = new MaskSignature();
                maskSignatureChecker.parse(maskSignatureString);

                Set<SymbolInformation> results = scanner.scanFile(filename);

                for (SymbolInformation symbolInfo : results) {
                    long symbolPos = symbolInfo.getPosition();
                    int symbolLength = maskSignatureChecker.getCodeLength();

                    //read file content region to byte array
                    byte[] codeBuf = new byte[symbolLength];
                    RandomAccessFile file = new RandomAccessFile(filename, "r");
                    file.seek(symbolPos);
                    file.read(codeBuf);
                    file.close();

                    //TODO cache codeBuf for mask signature test in TestBundle

                    if (maskSignatureChecker.checkCodeBuf(codeBuf))
                        return true;
                }
                return false;
            }
            case "DEVICE_HAS_FEATURE": {
                PackageManager packageManager = context.getPackageManager();
                String requestedFeature = test.getString("feature");
                if (requestedFeature == null || requestedFeature == "") {
                    return false;
                } else {
                    return packageManager.hasSystemFeature(requestedFeature);
                }
            }

            default:
                throw new IllegalArgumentException("Unknown testType " + testType);
        }
    }

    public static Boolean runDisasFunctionMatchesRegexTest(JSONObject test, Vector<String> objdumpLines) throws Exception {
        String filename = test.getString("filename");
        if (Constants.IS_TEST_MODE)
            filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
        String symbol = test.getString("symbol");
        String regex = test.getString("regex");
        Pattern p = Pattern.compile(regex);
        TestUtils.validateFilename(filename);
        File f = new File(filename);
        if (!f.exists())
            return null;
        try {
            JSONObject entry = null;
            if (objdumpLines != null) {
                entry = ProcessHelper.getSymbolTableEntry(objdumpLines, symbol);
            } else
                entry = ProcessHelper.getSymbolTableEntry(filename, symbol);

            if (entry == null)
                return null;
            long addr = entry.getLong("addr");
            long size = entry.getLong("len");
            String addrHex = Long.toString(addr, 16);
            String addrEndHex = Long.toString(addr + size, 16);
            Vector<String> lines = ProcessHelper.runObjdumpCommand("-d", "--start-address=0x" + addrHex, "--stop-address=0x" + addrEndHex, filename);
            StringBuilder builder = new StringBuilder();
            for (String line : lines) {
                builder.append(line.trim() + "\n");
            }
            Matcher m = p.matcher(builder);
            return m.matches();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean runDisasFunctionContainsStringTest(JSONObject test, Vector<String> objdumpLines) throws Exception {
        String filename = test.getString("filename");
        if (Constants.IS_TEST_MODE)
            filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
        String symbol = test.getString("symbol");
        String substringB64 = test.getString("substringB64");
        String substring = new String(Base64.decode(substringB64.getBytes(), Base64.DEFAULT));
        TestUtils.validateFilename(filename);
        File f = new File(filename);
        if (!f.exists())
            return null;
        try {
            JSONObject entry = null;
            if (objdumpLines != null)
                entry = ProcessHelper.getSymbolTableEntry(objdumpLines, symbol);
            else
                entry = ProcessHelper.getSymbolTableEntry(filename, symbol);

            if (entry == null)
                return false;
            long addr = entry.getLong("addr");
            long size = entry.getLong("len");
            String addrHex = Long.toString(addr, 16);
            String addrEndHex = Long.toString(addr + size, 16);
            Vector<String> lines = ProcessHelper.runObjdumpCommand("-d", "--start-address=0x" + addrHex, "--stop-address=0x" + addrEndHex, filename);
            for (String line : lines) {
                if (line.contains(substring)) {
                    return true;
                }
            }
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean runBinaryContainsSymbolTest(JSONObject test, Vector<String> objdumpLines) throws Exception {
        String filename = test.getString("filename");
        if (Constants.IS_TEST_MODE)
            filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
        String symbol = test.getString("symbol");
        TestUtils.validateFilename(filename);
        File f = new File(filename);
        if (!f.exists())
            return null;
        try {
            if (objdumpLines != null) {
                return ProcessHelper.getSymbolTableEntry(objdumpLines, symbol) != null;
            }
            return ProcessHelper.getSymbolTableEntry(filename, symbol) != null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean runMaskSignatureTest(JSONObject test, HashMap<String, SymbolInformation> symbolTable) throws Exception {

        String signature = test.getString("signature");
        String filename = test.getString("filename");
        if (Constants.IS_TEST_MODE)
            filename = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + filename;
        String symbol = test.getString("symbol");

        TestUtils.validateFilename(filename);
        File f = new File(filename);
        if (!f.exists())
            return null;

        MaskSignature signatureChecker = new MaskSignature();
        signatureChecker.parse(signature);

        if (symbolTable == null)
            symbolTable = signatureChecker.readSymbolTable(filename);

        if (symbolTable == null) {
            throw new IllegalStateException("Error: creating symbol table failed for file: " + filename);
        }

        SymbolInformation symbolInfo = symbolTable.get(symbol);
        if (symbolInfo == null) {
            return null;
        }
        long symbolPos = symbolInfo.getPosition();
        int symbolLength = symbolInfo.getLength();

        //read file content region to byte array
        byte[] codeBuf = new byte[symbolLength];
        RandomAccessFile file = new RandomAccessFile(filename, "r");
        file.seek(symbolPos);
        file.read(codeBuf);
        file.close();

        //TODO cache codeBuf for mask signature test in TestBundle

        //Log.d(Constants.LOG_TAG, "Signature check result: " + result);
        return signatureChecker.checkCodeBuf(codeBuf);
    }

    private static RollingSignature getRollingSignatureForTest(JSONObject test) {
        try {
            String testType = test.getString("testType");
            if (test != null && (testType.equals("ROLLING_SIGNATURE") || testType.equals("COMBINED_SIGNATURE"))) {
                String rollingSignature = test.getString("rollingSignature");

                Signature signature = Signature.getInstance(rollingSignature);
                if (!(signature instanceof RollingSignature)) {
                    throw new IllegalStateException("ROLLING_SIGNATURE: Not a valid rolling signature string!");
                }
                return (RollingSignature) signature;
            }
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "JSONException in getRollingSignatureForTest()", e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "IOException in getRollingSignatureForTest", e);
        }

        return null;
    }

    public static Set<BasicTestResult> performCollectedRollingSignatureTests(TestBundle bundle, Set<JSONObject> rollingSignatureTests) {
        if (rollingSignatureTests != null) {
            Set<BasicTestResult> results = new HashSet<>();
            try {
                MultiSignatureScanner scanner = new MultiSignatureScanner();
                for (JSONObject rollingSignatureBasicTest : rollingSignatureTests) {
                    RollingSignature signature = getRollingSignatureForTest(rollingSignatureBasicTest);
                    scanner.addSignatureChecker(signature);
                }
                Set<SymbolInformation> symbolInformationsResults = scanner.scanFile(bundle.getFilename());
                Set<String> symbolNames = new HashSet<>();
                for (SymbolInformation symbolInformation : symbolInformationsResults) {
                    symbolNames.add(symbolInformation.getSymbolName());
                }
                for (JSONObject rollingSignatureBasicTest : rollingSignatureTests) {
                    boolean result = false;
                    if (symbolNames.contains(rollingSignatureBasicTest.getString("rollingSignature"))) {
                        result = true;
                    }

                    results.add(new BasicTestResult(rollingSignatureBasicTest.getString("uuid"), result, null));

                }
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "IOException while scanning file " + bundle.getFilename() + " for rolling signatures: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, "Missing info in basicTest while scanning for rolling signatures: " + e.getMessage());
            } catch (IllegalStateException e) {
                //File does not exist -> all rollingsignature test are resulting in exception
                results = new HashSet<>();
                for (JSONObject rollingSignatureBasicTest : rollingSignatureTests) {
                    try {
                        results.add(new BasicTestResult(rollingSignatureBasicTest.getString("uuid"), null, e.getMessage()));
                    } catch (JSONException e1) {
                        Log.e(Constants.LOG_TAG, "Missing uuid info in basicTest: " + e1.getMessage());
                    }
                }
            }
            return results;
        }
        return null;
    }

    public static DexContainer getDexContainer(JSONObject test) throws IOException, JSONException {

        String dexPath = test.getString("dexPath");
        String[] dexPathParts = dexPath.split(":");
        String dexPathType = dexPathParts[0];
        String dexPathFileName = dexPathParts[1];

        DexContainer dexContainer = null;

        if (dexPathType.equals("VDEX")) {
            byte[] buf = Helper.open(dexPathFileName);
            dexContainer = new VDexFile(buf);
        } else if (dexPathType.equals("APK")) {
            dexContainer = new ApkFile(dexPathFileName);
        } else {
            Log.d(Constants.LOG_TAG, "Invalid dexPathType: " + dexPathType);
            throw new RuntimeException("Invalid dexPathType: " + dexPathType);
        }

        return dexContainer;
    }

    public static Boolean evaluateDexContainsClass(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            return classList.length > 0;
        } catch (Exception e) {
            return null;
        }

    }

    public static Boolean evaluateDexClassContainsMethod(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                if (classList[i].hasMethod(test.getString("method"))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexClassContainsStaticField(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                ArrayList<EncodedField> fields = classList[i].getStaticFields(test.getString("field"));
                if (fields != null && fields.size() > 0) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexClassContainsInstanceField(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                ArrayList<EncodedField> fields = classList[i].getInstanceFields(test.getString("field"));
                if (fields != null && fields.size() > 0) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexStaticFieldFlags(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                ArrayList<EncodedField> fields = classList[i].getStaticFields(test.getString("field"));
                if (fields == null) {
                    break;
                }
                for (EncodedField field : fields) {
                    int accessFlags = field.getAccessFlags();
                    int flagsMask = Integer.parseInt(test.getString("flagsMask"));
                    int flagsValue = Integer.parseInt(test.getString("flagsValue"));
                    if ((accessFlags & flagsMask) == flagsValue) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexInstanceFieldsFlags(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                ArrayList<EncodedField> fields = classList[i].getInstanceFields(test.getString("field"));

                if (fields == null) {
                    break;
                }
                for (EncodedField field : fields) {
                    int accessFlags = field.getAccessFlags();
                    int flagsMask = Integer.parseInt(test.getString("flagsMask"));
                    int flagsValue = Integer.parseInt(test.getString("flagsValue"));
                    if ((accessFlags & flagsMask) == flagsValue) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexMethodFlags(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            for (int i = 0; i < classList.length; i++) {
                if (classList[i].hasMethod(test.getString("method"))) {
                    EncodedMethod method = classList[i].getMethod(test.getString("method"));
                    int accessFlags = method.getAccessFlags();
                    int flagsMask = Integer.parseInt(test.getString("flagsMask"));
                    int flagsValue = Integer.parseInt(test.getString("flagsValue"));

                    if ((accessFlags & flagsMask) == flagsValue) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexMethodHasCode(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }

            boolean foundMethodWithoutCode = false;
            for (int i = 0; i < classList.length; i++) {
                if (classList[i].hasMethod(test.getString("method"))) {
                    EncodedMethod method = classList[i].getMethod(test.getString("method"));
                    if (method.getCode() != null) {
                        return true;
                    } else {
                        foundMethodWithoutCode = true;
                    }
                }
            }

            if (foundMethodWithoutCode) {
                return false;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean evaluateDexMethodSignature(JSONObject test, DexContainer dexContainer) {

        try {
            DexClass[] classList = dexContainer.getDexClass(test.getString("class"));
            if (classList.length == 0) {
                return null;
            }
            boolean foundMethodWithDifferentSig = false;
            for (int i = 0; i < classList.length; i++) {
                if (classList[i].hasMethod(test.getString("method"))) {
                    EncodedMethod method = classList[i].getMethod(test.getString("method"));
                    CodeItem code = method.getCode();

                    if (method.getCode() != null) {
                        String expectedSignatureVersion = test.getString("signature").split(":")[0];
                        String sig = code.getSignature(expectedSignatureVersion, false);

                        if (sig.equalsIgnoreCase(test.getString("signature"))) {
                            return true;
                        } else {
                            foundMethodWithDifferentSig = true;
                        }
                    }
                }
            }
            if (foundMethodWithDifferentSig) {
                return false;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}