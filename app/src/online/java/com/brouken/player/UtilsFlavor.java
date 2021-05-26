package com.brouken.player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.net.URL;
import java.util.concurrent.TimeUnit;

class UtilsFlavor {
    public static void onAppLaunch(final Context context) {
        checkScheduled(context);
    }

    private static void schedule(final Context context) {
        // DEBUG:
        // adb shell cmd jobscheduler run -f com.brouken.player.online 0
        // adb shell dumpsys jobscheduler
        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
        final JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(0, new ComponentName(context, UpdateCheckJobService.class))
                .setPersisted(true)
                //.setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if (Build.VERSION.SDK_INT >= 24) {
            jobInfoBuilder.setPeriodic(TimeUnit.DAYS.toMillis(3), TimeUnit.DAYS.toMillis(3));
        } else {
            jobInfoBuilder.setPeriodic(TimeUnit.DAYS.toMillis(4));
        }
        jobScheduler.schedule(jobInfoBuilder.build());
    }

    private static void checkScheduled(final Context context) {
        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (Build.VERSION.SDK_INT >= 24) {
            // User disable notification channel or all notifications for the app -
            // created notification won't be visible so it doesn't make sense to run update check job
            if (areUpdateNotificationsDisabled(context)) {
                jobScheduler.cancelAll();
                return;
            }

            // Job is not scheduled but it should be
            if (jobScheduler.getPendingJob(0) == null || jobScheduler.getAllPendingJobs().size() == 0) {
                schedule(context);
            }
        }
    }

    private static boolean areUpdateNotificationsDisabled(Context context) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // All notifications turned off for the app
        if (Build.VERSION.SDK_INT >= 24 && !notificationManager.areNotificationsEnabled())
            return true;

        // No channels support - no possible optimization
        if (Build.VERSION.SDK_INT <= 26)
            return false;

        // Updates notification channel turned off
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(context.getString(R.string.appupdater_channel));
        if (notificationChannel != null && notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE)
            return true;

        return false;
    }

    // From https://github.com/javiersantos/AppUpdater

    static void showUpdateAvailableNotification(Context context, String title, String content, URL apk) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        initNotificationChannel(context, notificationManager);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingIntentUpdate = PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW, Uri.parse(apk.toString())), PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = getBaseNotification(context, contentIntent, title, content)
                .addAction(R.drawable.ic_system_update_white_24dp, context.getResources().getString(R.string.appupdater_btn_update), pendingIntentUpdate);

        notificationManager.notify(0, builder.build());
    }

    private static NotificationCompat.Builder getBaseNotification(Context context, PendingIntent contentIntent, String title, String content) {
        return new NotificationCompat.Builder(context, context.getString(R.string.appupdater_channel))
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setSmallIcon(R.drawable.ic_stat_name)
                //.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
    }

    private static void initNotificationChannel(Context context, NotificationManager notificationManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    context.getString(R.string.appupdater_channel),
                    context.getString(R.string.appupdater_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
        }
    }

}
