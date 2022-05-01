package de.srlabs.patchanalysis_module.analysis.java_basic_tests_old;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.srlabs.patchanalysis_module.Constants;

public class SELinuxTest implements JavaBasicTest {
    @Override
    public Boolean runTest(Context c) throws IOException, InterruptedException {
        File testFile = new File("/sys/fs/selinux/enforce");

        if(!testFile.exists())
            throw new IOException("SELinux testfile does not exist: /sys/fs/selinux/enforce");

        BufferedReader reader = new BufferedReader(new FileReader(testFile));
        String line = reader.readLine();
        Log.d(Constants.LOG_TAG,"SELinux Test: "+line);
        return line.equals("1");
    }
}
