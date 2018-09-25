package de.srlabs.patchanalysis_module.util;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;

import de.srlabs.patchanalysis_module.analysis.PatchanalysisService;
import de.srlabs.patchanalysis_module.analysis.TestUtils;

/**
 * Dummy implementation to allow for builds without safetynet dependencies
 **/

public class CertifiedBuildChecker {
    private static CertifiedBuildChecker instance;
    private PatchanalysisService service;

    public static CertifiedBuildChecker getInstance(){
        if(instance == null){
            instance = new CertifiedBuildChecker();
        }
        return instance;
    }

    private CertifiedBuildChecker(){
    }

    public void startChecking(final PatchanalysisService context){
        this.service = context;
        Thread checkThread = new Thread(){
            @Override
            public void run(){
                tellServiceFinished();
            }
        };
        checkThread.start();
    }

    public String getResult() {
        return null;
    }

    public String getNonceBase64() {
        return null;
    }

    public boolean wasTestSuccesful(){
        return false;
    }

    public Boolean getCtsProfileMatchResponse(){
        return null;
    }

    public Boolean getBasicIntegrityResponse(){
        return null;
    }

    public void tellServiceFinished(){
        service.finishCertifiedBuildCheck();
    }
}
