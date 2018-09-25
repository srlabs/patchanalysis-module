package de.srlabs.patchanalysis_module.helpers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.views.PatchanalysisSumResultChart;

/**
 * This class ensures that for every Patch analysis, only one of the methods executeFinishedOncePerAnalysis
 * and executeCancelledOncePerAnalysis gets executed. These methods can be called safely via the handler
 * from within PatchanalysisMainStandaloneActivity and DashboardActivity's service callbacks.
 */
public class ServiceConnectionHelper {
    private static long lastProcessedAnalysisTimestamp = -1;

    public static synchronized void executeFinishedOncePerAnalysis (String analysisResultString,
                               boolean isBuildCertified, long currentAnalysisTimestamp) {
        // Don't process this if it has already been processed
        if (currentAnalysisTimestamp <= lastProcessedAnalysisTimestamp) {
            return;
        }
        lastProcessedAnalysisTimestamp = currentAnalysisTimestamp;

        JSONObject resultJSON = null;
        try {
            resultJSON = new JSONObject(analysisResultString);
        } catch (JSONException e) {
            Log.d(Constants.LOG_TAG,"Could not parse JSON from SharedPrefs", e);
        }
        PatchanalysisSumResultChart.setAnalysisRunning(false);
        PatchanalysisSumResultChart.setResultToDrawFromOnNextUpdate(resultJSON);
        SharedPrefsHelper.saveAnalysisResultNonPersistent(resultJSON, isBuildCertified);
    }


    public static void executeCancelledOncePerAnalysis (String stickyErrorMessage, long currentAnalysisTimestamp) {
        // Don't process this if it has already been processed
        if (currentAnalysisTimestamp <= lastProcessedAnalysisTimestamp) {
            return;
        }
        lastProcessedAnalysisTimestamp = currentAnalysisTimestamp;

        PatchanalysisSumResultChart.setAnalysisRunning(false);
        SharedPrefsHelper.saveStickyErrorMessageNonPersistent(stickyErrorMessage);
    }

}
