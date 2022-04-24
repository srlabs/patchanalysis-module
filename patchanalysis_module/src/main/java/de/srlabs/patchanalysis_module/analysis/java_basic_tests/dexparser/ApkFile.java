package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkFile extends DexContainer {

    ArrayList<DexFile> dexFiles = new ArrayList<>();

    public ApkFile(String fileName) throws  IOException {

        File f = new File(fileName);
        ZipFile zf = new ZipFile(f);
        Enumeration<? extends ZipEntry> entries = zf.entries();

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String zipFileName = zipEntry.getName();
            if (Helper.stringFollowsRegex(zipFileName, "dexClasses")) {
                byte[] buf = Helper.readAllBytes(zf.getInputStream(zipEntry));
                DexFile dex = new DexFile(buf);
                dexFiles.add(dex);
            }
        }
        zf.close();

        if (dexFiles.size() == 0) {
            throw new RuntimeException("No dex in APK");
        }
    }

    public ArrayList<DexFile> getDexFiles() {
        return dexFiles;
    }
}