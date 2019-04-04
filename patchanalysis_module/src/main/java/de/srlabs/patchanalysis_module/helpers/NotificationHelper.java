package de.srlabs.patchanalysis_module.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.R;

/**
 * Handles creation of all notifications displayed by the Patchanalysis
 *
 */

public class NotificationHelper {
    public static final int BUILD_CHANGED_NOTIFICATION_ID = 1146;
    public static final int ONGOING_NOTIFICATION_ID = 1147;
    public static final int FINISHED_NOTIFICATION_ID = 1148;
    public static final int FAILED_NOTIFICATION_ID = 1149;

    public static final String NOTIFICATION_CHANNEL_ID = "pa-notification-channel";
    private Context context;
    private AppFlavor appFlavor;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context, AppFlavor appFlavor){
        this.context = context;
        this.appFlavor = appFlavor;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        initNotificationChannel(notificationManager);
    }

    private void initNotificationChannel(NotificationManager notificationManager){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(Constants.LOG_TAG, "Creating notification channel...");
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, context.getString(R.string.pa_notification_channel_title), NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(context.getString(R.string.pa_notification_channel_desc));
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true); //reflect default preference here (vibrate + ring)
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void cancelNonStickyNotifications() {
        notificationManager.cancel(BUILD_CHANGED_NOTIFICATION_ID);
        notificationManager.cancel(FINISHED_NOTIFICATION_ID);
        notificationManager.cancel(FAILED_NOTIFICATION_ID);
    }


    public void showBuildVersionChangedNotification() {
        String notificationSetting = appFlavor.getPatchAnalysisNotificationSetting(context);
        triggerSenseableNotification(notificationSetting,context);

        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_build_changed_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_build_changed_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        notificationManager.notify(BUILD_CHANGED_NOTIFICATION_ID, notification);
    }

    public void showAnalysisFinishedNotification() {
        String notificationSetting = appFlavor.getPatchAnalysisNotificationSetting(context);
        Log.d(Constants.LOG_TAG,"notification setting for fininished notification: "+notificationSetting);
        triggerSenseableNotification(notificationSetting,context);

        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_finished_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_finished_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification);
    }

    public void showAnalysisFailedNotification() {
        String notificationSetting = "vibrate";
        triggerSenseableNotification(notificationSetting,context);

        Log.d(Constants.LOG_TAG, "TestExeCutorService.showAnalysisFailedNotification called");
        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_failed_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_failed_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        notificationManager.notify(FAILED_NOTIFICATION_ID, notification);
    }

    // Displayed via Service.startForeground
    public Notification getAnalysisOngoingNotification() {
        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_running_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_running_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .build();

        return notification;
    }

    /**
     * Decide which notification shall be triggered.
     * For the different options please take a look at @array/notification_options_internal
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
     *
     * @param notificationSetting
     */
    public static void triggerSenseableNotification(String notificationSetting, Context context) {

        switch (notificationSetting) {
            case "vibrate":
                triggerVibrateNotification(context);
                break;
            case "ring":
                triggerSoundNotification(context);
                break;
            case "vibrate+ring":
                triggerVibrateNotification(context);
                triggerSoundNotification(context);
                break;
            default:
                break;
        }
    }

    /**
     * Play a sound notification
     * Can be used to make the user notice / signal status changes.
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
     */
    public static void triggerSoundNotification(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
            if (r != null)
                r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vibrate to notify user
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
     */
    public static void triggerVibrateNotification(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 400, 500, 400}; // vibrate 2 times with 0.5s interval
        if (v.hasVibrator()) {
            v.vibrate(pattern, -1);          // "-1" = vibrate exactly as pattern, no repeat
        }
    }

    public static int getPatchanalysisLogoId() {
        return R.drawable.ic_patchanalysis;
    }
}
