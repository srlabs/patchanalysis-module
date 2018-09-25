package de.srlabs.patchanalysis_module.helpers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import de.srlabs.patchanalysis_module.AppFlavor;
import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.R;

/**
 * Handles creation of all notifications displayed by the Patchanalysis
 */

public class NotificationHelper {
    public static final int BUILD_CHANGED_NOTIFICATION_ID = 1146;
    public static final int ONGOING_NOTIFICATION_ID = 1147;
    public static final int FINISHED_NOTIFICATION_ID = 1148;
    public static final int FAILED_NOTIFICATION_ID = 1149;

    public static void cancelNonStickyNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(BUILD_CHANGED_NOTIFICATION_ID);
        notificationManager.cancel(FINISHED_NOTIFICATION_ID);
        notificationManager.cancel(FAILED_NOTIFICATION_ID);
    }


    public static void showBuildVersionChangedNotification(AppFlavor appFlavor, Context context) {
        String notificationSetting = appFlavor.getPatchAnalysisNotificationSetting(context);
        triggerSenseableNotification(notificationSetting,context);

        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_build_changed_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_build_changed_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(BUILD_CHANGED_NOTIFICATION_ID, notification);

    }

    public static void showAnalysisFinishedNotification(AppFlavor appFlavor, Context context) {
        String notificationSetting = appFlavor.getPatchAnalysisNotificationSetting(context);
        Log.d(Constants.LOG_TAG,"notification setting for fininished notification: "+notificationSetting);
        triggerSenseableNotification(notificationSetting,context);

        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_finished_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_finished_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification);
    }

    public static void showAnalysisFailedNotification(AppFlavor appFlavor, Context context) {
        String notificationSetting = "vibrate";
        triggerSenseableNotification(notificationSetting,context);

        Log.d(Constants.LOG_TAG, "TestExeCutorService.showAnalysisFailedNotification called");
        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getResources().getString(R.string.patchanalysis_notification_failed_title))
                        .setContentText(context.getResources().getString(R.string.patchanalysis_notification_failed_text))
                        .setSmallIcon(getPatchanalysisLogoId())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(FAILED_NOTIFICATION_ID, notification);
    }

    // Displayed via Service.startForeground
    public static Notification getAnalysisOngoingNotification(AppFlavor appFlavor, Context context) {
        Intent notificationIntent = new Intent(context, appFlavor.getPatchAnalysisActivityClass());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
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
