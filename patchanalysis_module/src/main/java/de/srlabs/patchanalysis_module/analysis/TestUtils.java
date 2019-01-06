package de.srlabs.patchanalysis_module.analysis;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Process;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.helpers.ProcessHelper;
import de.srlabs.patchanalysis_module.analysis.signatures.Section;
import de.srlabs.patchanalysis_module.analysis.signatures.SymbolInformation;

public class TestUtils {
    public static HashMap<String,String> buildProperties = null;
    private static final Object buildPropertiesLock = new Object();
    private static JSONArray protectedBroadcasts;
    private static Pattern datePattern = Pattern.compile("^\\d{4}\\-\\d{2}(-\\d{2})?$");

    public static boolean checkAffectedAndroidVersion(String[] affectedAndroidVersions) {
        for (String affectedVersion : affectedAndroidVersions) {
            if (Build.VERSION.RELEASE.startsWith(affectedVersion)) {
                return true;
            }
        }
        return false;
    }
    private static JSONObject readManifest(XmlResourceParser xml) throws JSONException, IOException, XmlPullParserException {
        Vector<JSONObject> tagTree = new Vector<>();
        JSONObject manifest = null;
        while (true) {
            int state = xml.next();
            if (state == XmlResourceParser.START_TAG) {
                JSONObject tag = new JSONObject();
                if (manifest == null) {
                    manifest = tag;
                }
                tag.put("tagName", xml.getName());
                for (int i = 0; i < xml.getAttributeCount(); i++) { // TODO

                    tag.put(xml.getAttributeName(i), xml.getAttributeValue(i));
                }
                tag.put("children", new JSONArray());
                if (tagTree.size() > 0)
                    tagTree.lastElement().getJSONArray("children").put(tag);
                tagTree.add(tag);
            } else if (state == XmlResourceParser.END_TAG) {
                tagTree.remove(tagTree.size() - 1);
            } else if (state == XmlResourceParser.END_DOCUMENT) {
                break;
            }
        }
        return manifest;
    }
    protected static JSONObject readAllManifests(Context context) throws JSONException {
        try{
            JSONObject result = new JSONObject();
            JSONObject cookies = new JSONObject();
            JSONObject manifests = new JSONObject();
            HashSet<String> done = new HashSet<>();

            // https://github.com/michalbednarski/IntentsLab/blob/master/IntentsLab/src/main/java/com/github/michalbednarski/intentslab/XmlViewerFragment.java
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> pilist = pm.getInstalledPackages(0);
            for (PackageInfo pi : pilist) {
                if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    continue; // Ignore non-system applications
                }
                String apkFile = pi.applicationInfo.sourceDir;
                Resources r = pm.getResourcesForApplication(pi.applicationInfo);
                AssetManager assets = r.getAssets();

                if (Build.VERSION.SDK_INT < 28) { //only for Android versions smaller than 9, as reflection for non-SDK methods is not possible anymore + "getCookieName" method was removed
                    if (cookies.has(apkFile)) {
                        throw new JSONException("readAllManifests: cookies already has '" + apkFile + "' field");
                    }
                    JSONArray cookielist = new JSONArray();
                    for (int cookie = 1; cookie < 10; cookie++) {
                        try {
                            String cookieName = getCookie(assets, cookie);
                            cookielist.put(cookieName);
                            if (!manifests.has(cookieName)) {
                                XmlResourceParser xml = assets.openXmlResourceParser(cookie, "AndroidManifest.xml");
                                manifests.put(cookieName, readManifest(xml));
                            }
                        } catch (InvocationTargetException e) {
                            break;
                        } catch (IOException e) {
                            Log.e(Constants.LOG_TAG, "Could not parse manifest file for apk file: " + apkFile, e);
                        }
                    }
                    cookies.put(apkFile, cookielist);
                }
                XmlResourceParser xml = assets.openXmlResourceParser(0, "AndroidManifest.xml");
                manifests.put(apkFile, readManifest(xml));
            }

            result.put("manifestCookies", cookies);
            result.put("manifests", manifests);
            return result;
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "readAllManifests", e);
            return null;
        }
    }

    private static String getCookie(AssetManager assetManager, int cookie) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException{
        final Method getCookieName = AssetManager.class.getDeclaredMethod("getCookieName", int.class);
        return getCookieName.invoke(assetManager, cookie).toString();
    }

    private static JSONArray getProtectedBroadcasts(Context context) throws JSONException {
        // TODO: Reimplement this based on manifest json
        if(protectedBroadcasts == null) {
            protectedBroadcasts = new JSONArray();
            HashSet<String> done = new HashSet<>();
            PackageManager pm = context.getPackageManager();
            for (PackageInfo pi : pm.getInstalledPackages(0)) {
                try {
                    if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        continue; // Ignore non-system applications
                    }
                    Resources r = pm.getResourcesForApplication(pi.applicationInfo);
                    for (int cookie = 0; cookie < 10; cookie++) {
                        XmlResourceParser xml = null;
                        try {
                            xml = r.getAssets().openXmlResourceParser(cookie, "AndroidManifest.xml");
                        } catch(FileNotFoundException e){
                            break;
                        }
                        if (xml == null)
                            break;
                        while (true) {
                            int state = xml.next();
                            if (state == XmlResourceParser.START_TAG) {
                                if (xml.getName().equals("protected-broadcast")) {
                                    String pbName = null;
                                    for (int i = 0; i < xml.getAttributeCount(); i++) {
                                        if (xml.getAttributeName(i).equals("name")) {
                                            pbName = xml.getAttributeValue(i);
                                        }
                                    }
                                    assert (pbName != null);
                                    if (!done.contains(pbName)) {
                                        protectedBroadcasts.put(pbName);
                                        done.add(pbName);
                                    }
                                }
                            } else if (state == XmlResourceParser.END_DOCUMENT) {
                                break;
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(Constants.LOG_TAG, "readProtectedBroadcasts", e);
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, "readProtectedBroadcasts", e);
                } catch (XmlPullParserException e) {
                    Log.e(Constants.LOG_TAG, "readProtectedBroadcasts", e);
                }
            }
        }
        return protectedBroadcasts;
    }
    protected static boolean readBuildProperties(){
        HashMap<String, String> tempBuildProperties = new HashMap<>();
        try{
            Log.w("TestUtils", "Running getprop");
            String[] cmd = {"getprop"};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while(true) {
                String line = br.readLine();
                if (line == null){
                    break;
                }
                line = line.trim();
                if(line.charAt(0) == '[' && line.contains(":")){
                    Pattern pattern = Pattern.compile("^\\[(.*)\\]\\s*:\\s*\\[(.*)\\]\\s*$");
                    Matcher m = pattern.matcher(line);
                    if(m.matches()) {
                        MatchResult mr = m.toMatchResult();
                        tempBuildProperties.put(m.group(1).trim(), m.group(2).trim());
                        //Log.w("TestUtils", "GETPROP: " + m.group(1) + " = " + m.group(2));
                    }
                    else{
                        Log.w("TestUtils", "Invalid getprop line " + line);
                    }
                } else {
                    Log.w("TestUtils", "Invalid getprop line " + line);
                }
            }
        } catch(Exception e) {
            Log.e("TestUtils", "Error reading build properties from getprop, falling back to reading /system/build.prop", e);
            try {
                tempBuildProperties = new HashMap<>();
                File f = new File("/system/build.prop");
                FileInputStream fis = new FileInputStream(f);
                boolean eof = false;
                while (true) {
                    boolean lineDone = false;
                    boolean keyDone = false;
                    Vector<Byte> key = new Vector<Byte>();
                    while (true) {
                        int b = fis.read();
                        if (b == -1) {
                            return false;
                        }
                        if (b == '\r' || b == '\n') {
                            lineDone = true;
                            break;
                        }
                        if (b == ' ' || b == '\t') {
                            continue;
                        }
                        if (b == '=') {
                            keyDone = true;
                            break;
                        }
                        if (key.size() == 0 && b == '#')
                            break;
                        key.add((byte) b);
                    }
                    if (lineDone)
                        continue;
                    if (keyDone && key.size() > 0) {
                        Vector<Byte> value = new Vector<Byte>();
                        boolean valueDone = false;
                        while (true) {
                            int b = fis.read();
                            if (b == -1) {
                                return false;
                            }
                            if (b == '\r' || b == '\n') {
                                valueDone = true;
                                break;
                            }
                            if (value.size() == 0 && (b == ' ' || b == '\t')) {
                                continue;
                            }
                            value.add((byte) b);
                        }
                        tempBuildProperties.put(byteVectorToString(key).trim(), byteVectorToString(value).trim());
                    } else {
                        while (true) {
                            int b = fis.read();
                            if (b == -1)
                                return false;
                            if (b == '\r' || b == '\n') {
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e1) {
                Log.e("TestUtils", "Error reading build properties from build.prop", e1);
                return false;
            }
        }
        buildProperties = tempBuildProperties;
        return true;
    }
    public static String readProcSelfMaps() throws Exception{
        String[] cmd = {"cat","/proc/self/maps"};
        Process p = Runtime.getRuntime().exec(cmd);
        byte[] buf = new byte[128*1024];
        int numRead = p.getInputStream().read(buf);
        String stdoutData = "";
        if(numRead > 0){
            stdoutData = new String(buf, 0, numRead);
        }
        numRead = p.getErrorStream().read(buf);
        String stderrData = "";
        if(numRead > 0){
            stderrData = new String(buf, 0, numRead);
        }
        p.waitFor();
        int exitCode = p.exitValue();
        if(exitCode != 0 || stderrData.length() > 0){
            throw new IllegalStateException("cat /proc/self/maps error: exitCode=" + exitCode + "  stderr='" + stderrData + "'");
        }
        return stdoutData;
    }

    protected static String readFileForDeviceinfo(String filename, int maxLen){
        try{
            File f = new File(filename);
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
            Vector<Byte> buf = new Vector<Byte>();
            for(int i=0;i<maxLen;i++){
                int b = is.read();
                if(b < 0)
                    break;
                buf.add((byte)b);
            }
            return byteVectorToString(buf);
        } catch(IOException e){
            return "IOException reading " + filename + ": " + e.toString();
        }
    }
    private static String byteVectorToString(Vector<Byte> v){
        byte[] b = new byte[v.size()];
        for(int i=0;i<v.size();i++){
            b[i] = v.get(i);
        }
        try{
            ByteBuffer bb = ByteBuffer.wrap(b);
            CharsetDecoder decode = Charset.forName("UTF8").newDecoder();
            return decode.decode(bb).toString();
        } catch(Exception e){
            return "b64:" + Arrays.toString(Base64.encode(b, Base64.NO_WRAP));
        }
    }
    public static String getPatchlevelDate(){
        String result = getBuildProperty("ro.build.version.security_patch");
        if(result == null || !result.startsWith("20")) {
            return null;
        } else {
            return result;
        }
    }

    public static boolean isPatchDateClaimed(String patchReleaseDate) {
        // Prevent NPE in some rare cases
        if (getPatchlevelDate() == null) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            if(patchReleaseDate != null && patchReleaseDate.length() == 7) {
                //if we just get the category patchlevel, make sure we use the first of the month to compare
                patchReleaseDate += "-01";
            }
            Date requestedDate = format.parse(patchReleaseDate);
            Date claimedDate = format.parse(getPatchlevelDate());
            return (claimedDate.compareTo(requestedDate) >= 0);
        } catch (ParseException e) {
           Log.d(Constants.LOG_TAG,"ParseException in isPatchDateClaimed()",e);
        }

        return false;
    }

    public static boolean isValidDateFormat(String date) {
        Matcher m = datePattern.matcher(date);
        return m.matches();
    }


    public static String getBuildProperty(String name){
        synchronized (buildPropertiesLock) {
            if (buildProperties == null)
                readBuildProperties();
        }
        if(!buildProperties.containsKey(name))
            return null;
        return buildProperties.get(name);
    }
    public static boolean isQualcomm(){
        return getBuildProperty("ro.board.platform").toUpperCase().startsWith("MSM");
    }
    public static boolean isMtk(){
        return getBuildProperty("ro.board.platform").toUpperCase().startsWith("MT");
    }
    public static boolean isSpreadrum(){
        return getBuildProperty("ro.board.platform").toUpperCase().startsWith("SC");
    }
    public static boolean isNvidia(){
        return getBuildProperty("ro.board.platform").toUpperCase().startsWith("TEGRA");
    }
    public static boolean isSamsung(){
        return getBuildProperty("ro.board.platform").toUpperCase().startsWith("EXYNOS");
    }
    public static String getChipVendor(){
        if(isQualcomm())
            return "QUALCOMM";
        else if(isMtk())
            return "MTK";
        else if(isNvidia())
            return "NVIDIA";
        else if(isSamsung())
            return "SAMSUNG";
        else if(isSpreadrum())
            return "SPREADTRUM";
        else
            return "UNKNOWN";
    }

    public static void validateFilename(String filename){
        if (Constants.IS_TEST_MODE && !filename.startsWith(Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX+"/system/")){
                throw new IllegalArgumentException("Filename " + filename + " doesn't start with '"+ Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX+"/system/'");
        }
        else if (!Constants.IS_TEST_MODE && !filename.startsWith("/system/")) {
                throw new IllegalArgumentException("Filename " + filename + " doesn't start with '/system/'");
        }
        else if (filename.contains("/../")) {
            throw new IllegalArgumentException("Filename " + filename + " contains directory traversal");
        }
    }
    public static long getBuildDateUtc(){
        String buildDateUtc = getBuildProperty("ro.build.date.utc");
        if(buildDateUtc == null)
            return -1;

        try {
            long buildDate = Long.parseLong(buildDateUtc);
            return buildDate;
        }catch(NumberFormatException e){
            Log.d(Constants.LOG_TAG,"Found invalid, malformated builddate: "+buildDateUtc);
            return -1;
        }

    }
    public static String getBuildFingerprint(){
        String result = getBuildProperty("ro.build.fingerprint");
        if(result != null)
            return result;
        if(Build.FINGERPRINT != null)
            return Build.FINGERPRINT;
        return "NULL";
    }
    public static boolean streamContainsSubstring(BufferedInputStream stream, byte[] needle) throws IOException {
        byte[] buf = new byte[8192];
        int pos = 0;
        while(true){
            int bytesRead = stream.read(buf, pos, 4096);
            int searchStartPos = (8192 + pos - needle.length) % 8192;
            for(int i=0;i<needle.length + bytesRead;i++){
                boolean found = true;
                for(int j=0;j<needle.length;j++){
                    int comparePos = (8192 + searchStartPos + i + j) % 8192;
                    if(buf[comparePos] != needle[j]) {
                        found = false;
                        break;
                    }
                }
                if(found) {
                    return true;
                }
            }
            if(bytesRead < 4096)
                return false;
            pos = (pos + bytesRead) % 8192;
        }
    }

    public static String getDeviceModel() {
        String result = getBuildProperty("ro.product.model");
        if(result != null)
            return result;
        if(Build.MODEL != null)
            return Build.MODEL;
        return "NULL";
    }
    public static String getBuildDisplayName() {
        String result = getBuildProperty("ro.build.display.id");
        if(result == null)
            return "NULL";
        return result;
    }
    public static JSONObject streamToJson(InputStream stream) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream,"UTF-8"));
        StringBuilder jsonStringBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = reader.readLine()) != null) {
            jsonStringBuilder.append(inputStr);
        }
        return new JSONObject(jsonStringBuilder.toString());
    }
    public static boolean isConnectedToInternet(Context context){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public static boolean is64BitSystem(){
        // we assume here that a 64bit system comes at least with these two files
        // FIXME once we did the test, we can save the result to sharedPrefs
        String indicatorFile1Path = "/system/lib64/libstagefright.so";
        String indicatorFile2Path = "/system/lib64/libskia.so";
        if(Constants.IS_TEST_MODE){
            indicatorFile1Path = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + indicatorFile1Path;
            indicatorFile2Path = Constants.TEST_MODE_BASIC_TEST_FILE_PREFIX + indicatorFile2Path;
        }
        boolean result = new File(indicatorFile1Path).exists() || new File(indicatorFile2Path).exists();
        Log.d(Constants.LOG_TAG,"Testing a 64Bit system: "+result);
        return result;
    }
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static HashMap<String, SymbolInformation> readSymbolTable(String filePath) throws Exception{
        //Log.i(Constants.LOG_TAG,"Creating symbol table for file: "+filePath);
        HashMap<String, SymbolInformation> symtable = new HashMap<>();

        if (filePath == null) {
            throw new IllegalStateException("filePath argument == null!");
        }

        Pattern patternWhitespaces = Pattern.compile("\\s+");

        List<String> lines = ProcessHelper.getObjDumptTOutput(filePath);
        for (String line : lines) {
            line = line.trim(); //.decode()
            if (!line.contains(".text"))
                continue;
            if (line.contains(".text.unlikely"))
                continue;
            if (line.contains(".text."))
                continue;


            String[] components = patternWhitespaces.split(line);
            if (components.length < 4)
                continue;

            String symbolName = components[components.length - 1];
            String addrHex = components[0];
            String lenHex = null;
            for (int i = 0; i < components.length - 1; i++) {
                if (components[i].equals(".text")) {
                    lenHex = components[i + 1];
                }
            }
            if (lenHex == null) {
                throw new IllegalStateException("Invalid line: " + line);
            }
            int addr = Integer.parseInt(addrHex, 16);
            int length = Integer.parseInt(lenHex, 16);

            symtable.put(symbolName, new SymbolInformation(symbolName, addr, length));
        }
        ArrayList<Section> sections = new ArrayList<>();
        for (String line : ProcessHelper.getObjDumpHW(filePath)) {
            line = line.trim(); //.decode()
            if (line.contains("CODE")) {
                // Idx Name Size VMA LMA File off
                String[] items = patternWhitespaces.split(line);
                int size = Integer.parseInt(items[2], 16);
                int vma = Integer.parseInt(items[3], 16);
                int fileOffset = Integer.parseInt(items[5], 16);
                sections.add(new Section(size, vma, fileOffset));
            }
        }

        // add pos
        for (SymbolInformation symbolInformation : symtable.values()) {
            int addr = symbolInformation.getAddr();
            int pos = addr;
            for (Section section : sections) {
                if (addr >= section.getVma() && addr < section.getVma() + section.getSize()) {
                    pos = section.getFileOffset() + (addr - section.getVma());
                    symbolInformation.setPosition(pos);
                }
            }
        }

        if (filePath.endsWith(".o")) {
            for (String line : ProcessHelper.getObjDumpHWwithCheck(filePath)) {
                line = line.trim(); //.decode()
                if (!line.contains(".text.")) {
                    continue;
                }
                String[] components = patternWhitespaces.split(line);
                for (int i = 0; i < components.length - 1; i++) {
                    if (components[i].startsWith(".text.")) {
                        String symbolName = components[i].substring((".text.").length());
                        int codeLen = Integer.parseInt(components[i + 1], 16);
                        int pos = Integer.parseInt(components[i + 4], 16);
                        SymbolInformation symbolInformation = new SymbolInformation(symbolName, pos, codeLen);
                        symbolInformation.setPosition(pos);
                        symtable.put(symbolName, symbolInformation);
                    }
                }
            }
        }
        return symtable;
    }

    public static void writeInputstreamToFile(InputStream inputStream, File file) throws IOException{
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int length;
            while((length=inputStream.read(buf))>0){
                outputStream.write(buf,0,length);
            }
        }
        finally {
            if (outputStream != null) {
                outputStream.close();
            }
            inputStream.close();
        }
    }

    public static void writeStringToFile(String string, File file) throws IOException{
        if(string != null && file != null) {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                outputStream.write(string.getBytes());
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, "Exception while writing string to file: " + file);
            }
            finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }
    }

    public static boolean isTooOldAndroidAPIVersion() {
        return Build.VERSION.SDK_INT < 21;
    }

    public static String getAppId(AppFlavor appFlavor, ContextWrapper service) {
        return setAppId(appFlavor, service);
    }

    public static String setAppId(AppFlavor appFlavor, Context context) {
        return appFlavor.setAppId(context);
    }

    public static JSONObject makeDeviceinfoJson(AppFlavor appFlavor, ContextWrapper context, final ProgressItem progressItem) {
        try {
            JSONObject devinfo = new JSONObject();
            if (buildProperties == null)
                readBuildProperties();
            JSONObject buildProps = new JSONObject(buildProperties);
            devinfo.put("buildProperties", buildProps);
            devinfo.put("appId", getAppId(appFlavor, context));
            devinfo.put("androidApiLevel", Build.VERSION.SDK_INT);
            devinfo.put("chipVendor", getChipVendor());
            devinfo.put("proc_version", readFileForDeviceinfo("/proc/version", 4096));
            devinfo.put("proc_cpuinfo", readFileForDeviceinfo("/proc/cpuinfo", 16384));
            devinfo.put("proc_cmdline", readFileForDeviceinfo("/proc/cmdline", 4096));
            devinfo.put("proc_mounts", readFileForDeviceinfo("/proc/mounts", 4096));
            devinfo.put("proc_filesystems", readFileForDeviceinfo("/proc/filesystems", 4096));
            devinfo.put("proc_mtd", readFileForDeviceinfo("/proc/mtd", 4096));
            devinfo.put("proc_devices", readFileForDeviceinfo("/proc/devices", 4096));
            devinfo.put("proc_modules", readFileForDeviceinfo("/proc/modules", 4096));
            devinfo.put("proc_sys_kernel_randomize_va_space", readFileForDeviceinfo("/proc/sys/kernel/randomize_va_space", 4096));
            devinfo.put("aslrTest1", readProcSelfMaps());
            devinfo.put("aslrTest2", readProcSelfMaps());
            devinfo.put("aslrTest3", readProcSelfMaps());
            progressItem.update(0.1); // 10% progress for build properties and basic system info
            long filelistStartTime = System.currentTimeMillis();
            devinfo.put("systemPartition", DirectoryTreeLister.makeFilelist(new File("/system/"), new DirectoryTreeLister.ProgressCallback() {
                @Override
                public void reportProgress(double progress) {
                    progressItem.update(0.1 + 0.6*progress); // 60% progress for filesystem listing/hashing
                }
            }));
            long filelistDuration = System.currentTimeMillis() - filelistStartTime;
            Log.i(Constants.LOG_TAG, "Generating filelist took " + filelistDuration + " ms");
            progressItem.update(0.7);
            Log.i(Constants.LOG_TAG,"Reading all manifests...");
            JSONObject manifests = readAllManifests(context);

            if (manifests != null) {
                devinfo.put("manifests", manifests.getJSONObject("manifests"));
                devinfo.put("manifestCookies", manifests.getJSONObject("manifestCookies"));
            } else {
                devinfo.put("manifests", null);
                devinfo.put("manifestCookies", null);
            }

            progressItem.update(1.0); // Final 30% progress for Android manifests of system applications
            Log.i(Constants.LOG_TAG,"Finished reading all manifests.");
            return devinfo;
        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "Exception in makeDeviceinfoJson", e);
            return null;
        }
    }


    public static String getAndroidVersion(){
        return getBuildProperty("ro.build.version.release");
    }
}
