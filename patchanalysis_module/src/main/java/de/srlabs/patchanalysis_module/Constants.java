package de.srlabs.patchanalysis_module;

import android.graphics.Color;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.srlabs.patchanalysis_module.analysis.TestUtils;

/**
 * Keeps some constant values, which are used by PatchAnalysis
 */
public class Constants  {
    public static final String LOG_TAG="PatchAnalysis";

    public static final int APP_VERSION = 13;
    // needed so that subsequent calls to loaddata work on the same webview
    public static final String WEBVIEW_URL_LOADDATA = "arbitrary://invalid/url";

    //testmode (decide whether we want to test files on OS or external files extracted to /sdcard/system)
    // can be useful when testing 64Bit builds on a 32Bit device
    public static final boolean IS_TEST_MODE=false;
    public static final String TEST_MODE_BASIC_TEST_FILE_PREFIX = "/sdcard";

    public enum ActivityState {PATCHLEVEL_DATES, VULNERABILITY_LIST}

    //colors
    public static final int COLOR_INCONCLUSIVE=0xFF7575EC;
    public static final int COLOR_MISSING= 0xFFD36031;
    public static final int COLOR_PATCHED=0xFF84B538;
    public static final int COLOR_NOTAFFECTED=Color.GRAY;
    public static final int COLOR_NOTCLAIMED=0xFFFF8000;


    /**
     * Assign color for test result for certain vulnerability
     * @param vulnerability JSON representation of vulnerability test result
     * @param refPatchlevelDate reference patch level date
     * @return Color representation in int
     */
    public static int getVulnerabilityIndicatorColor(JSONObject vulnerability, String refPatchlevelDate) {
        try {
            if(vulnerability.has("patchlevelDate")) //if the vulnerability has more detailed information about patch level date
                refPatchlevelDate = vulnerability.getString("patchlevelDate");
            if(!vulnerability.isNull("notAffected") && vulnerability.getBoolean("notAffected")) {
                return Constants.COLOR_NOTAFFECTED;
            } else if (vulnerability.isNull("fixed") || vulnerability.isNull("vulnerable") ||
                    (vulnerability.getBoolean("fixed") && vulnerability.getBoolean("vulnerable"))) {
                return Constants.COLOR_INCONCLUSIVE;
            } else if (vulnerability.getBoolean("fixed") && !vulnerability.getBoolean("vulnerable")) {
                return Constants.COLOR_PATCHED;
            } else if (!vulnerability.getBoolean("fixed") && vulnerability.getBoolean("vulnerable")) {
                if (TestUtils.isValidDateFormat(refPatchlevelDate) && !TestUtils.isPatchDateClaimed(refPatchlevelDate))
                    return Constants.COLOR_NOTCLAIMED;
                return Constants.COLOR_MISSING;
            }
        }catch(JSONException e){
            Log.e(Constants.LOG_TAG,"Problem assigning color for tests", e);
        }
        // default color
        return Constants.COLOR_INCONCLUSIVE;
    }

}
