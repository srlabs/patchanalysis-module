package de.srlabs.patchanalysis_module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.srlabs.patchanalysis_module.analysis.TestUtils;
import de.srlabs.patchanalysis_module.helpers.NotificationHelper;
import de.srlabs.patchanalysis_module.helpers.SharedPrefsHelper;

/**
 * Checks whether a new build version was installed and prompts the user to perform a new analysis
 */

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOG_TAG,"Boot completed event received...");
        SharedPreferences sharedPrefs = SharedPrefsHelper.getPersistentSharedPrefs(context);
        long currentBuildDate = TestUtils.getBuildDateUtc();
        if(currentBuildDate == -1)
            Log.d(Constants.LOG_TAG, "Found invalid builddate timestamp");
        long buildDateUtcAtLastSuccessfulAnalysis = sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE_LAST_ANALYSIS, -1);
        long buildDateNotificationDisplayed = sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE_NOTIFICATION_DISPLAYED, -1);

        if (currentBuildDate != buildDateNotificationDisplayed && currentBuildDate != buildDateUtcAtLastSuccessfulAnalysis) {
            SharedPrefsHelper.clearSavedAnalysisResult(context);
            SharedPrefsHelper.putLongPersistent(SharedPrefsHelper.KEY_BUILD_DATE_NOTIFICATION_DISPLAYED, currentBuildDate, context);
            if (!(buildDateUtcAtLastSuccessfulAnalysis == -1)) {
                NotificationHelper notificationHelper = new NotificationHelper(context, AppFlavor.getAppFlavor());
                notificationHelper.showBuildVersionChangedNotification();
            }
        }

        // Get information when an upgrade was installed
        long currentTime = System.currentTimeMillis() / 1000;
        String currentSPL = TestUtils.getPatchlevelDate();
        String currentBuildFingerprint = TestUtils.getBuildFingerprint();

        String previousSPL = sharedPrefs.getString(SharedPrefsHelper.KEY_BUILD_SPL, "not_set");
        String previousBuildFingerprint = sharedPrefs.getString(SharedPrefsHelper.KEY_BUILD_FINGERPRINT, "not_set");
        long previousBuildDate = sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE, -1);

        if (currentSPL == null) {
            currentSPL = "NULL";
            Log.d(Constants.LOG_TAG,"Found invalid patchlevel date");
        }
        if (currentBuildFingerprint == "NULL") {
            Log.d(Constants.LOG_TAG,"Found invalid build fingerprint");
        }

        if (previousBuildFingerprint != "not_set") {

            if (currentBuildFingerprint != previousBuildFingerprint || currentBuildDate != previousBuildDate ||
                    currentSPL != previousSPL) {

                Log.d(Constants.LOG_TAG,"Detected firmware upgrade");
                JSONObject updateInfo = new JSONObject();
                try {
                    updateInfo.put("updateTimestamp", currentTime);
                    updateInfo.put("previousBuildFingerprint", previousBuildFingerprint);
                    updateInfo.put("previousBuildTimestamp", previousBuildDate);
                    updateInfo.put("previousSPL", previousSPL);
                    updateInfo.put("buildFingerprint", currentBuildFingerprint);
                    updateInfo.put("buildTimestamp", currentBuildDate);
                    updateInfo.put("SPL", currentSPL);
                } catch (JSONException e) {
                    Log.d(Constants.LOG_TAG,"Could not create JSON object containing update info", e);
                }

                SharedPrefsHelper.putStringPersistent(SharedPrefsHelper.KEY_UPDATE_INFO, updateInfo.toString(), context);

            }

        }

        SharedPrefsHelper.putLongPersistent(SharedPrefsHelper.KEY_BUILD_DATE, currentBuildDate, context);
        SharedPrefsHelper.putStringPersistent(SharedPrefsHelper.KEY_BUILD_FINGERPRINT, currentBuildFingerprint, context);
        SharedPrefsHelper.putStringPersistent(SharedPrefsHelper.KEY_BUILD_SPL, currentSPL, context);
    }
}
