package de.srlabs.patchanalysis_module;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.MenuItem;

/**
 * This interface helps declaring environmental dependencies for certain app flavors
 * We suggest extending android.app.Application and calling setAppFlavor
 * with the respective implementation in onCreate
 */
public abstract class AppFlavor {
    public static String BINARIES_PATH;

    private static AppFlavor activeFlavor;

    public AppFlavor(Context context) {
        this.BINARIES_PATH = context.getApplicationInfo().dataDir + "/lib/";
        try {
            ApplicationInfo ainfo = context.getApplicationContext().getPackageManager().getApplicationInfo("de.srlabs.snoopsnitch", PackageManager.GET_SHARED_LIBRARY_FILES);
            this.BINARIES_PATH = ainfo.nativeLibraryDir + "/";

        } catch(PackageManager.NameNotFoundException e){
            Log.d(Constants.LOG_TAG, "Could not retrieve location of binaries, using fallback.");
        }

        Log.d(Constants.LOG_TAG, "set binaries path to: " + this.BINARIES_PATH);
    }

    public String getBinaryPath() {
        return BINARIES_PATH;
    }

    public static AppFlavor getAppFlavor() {
        return activeFlavor;
    }

    public static void setAppFlavor(AppFlavor appFlavor) {
        activeFlavor = appFlavor;
    }

    public abstract String setAppId(Context context);

    public abstract void setShowInconclusiveResults(Context context, boolean showInconclusive);

    public abstract boolean getShowInconclusivePatchAnalysisTestResults(Context context);

    public abstract void setShowOptionalCVEs(Context context, boolean showOptionalCVEs);

    public abstract boolean getShowOptionalCVES(Context context);

    public abstract String getPatchAnalysisNotificationSetting(Context context);

    public abstract Class<?> getMainActivityClass();

    public abstract Class<?> getPatchAnalysisActivityClass();

    public abstract void homeUpButtonMainActivitiyCallback(Activity activity, MenuItem item);

}
