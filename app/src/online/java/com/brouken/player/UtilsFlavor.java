package com.brouken.player;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.StreamInformation;
import com.google.android.exoplayer2.Format;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

    public static int normRate(float rate) {
        return (int)(rate * 100f);
    }

    public static boolean switchFrameRate(final Activity activity, final float frameRateExo, final Uri uri) {
        if (!Utils.isTvBox(activity))
            return false;

        float frameRate = Format.NO_VALUE;

        // preferredDisplayModeId only available on SDK 23+
        // ExoPlayer already uses Surface.setFrameRate() on Android 11+ but may not detect actual video frame rate
        if (Build.VERSION.SDK_INT >= 23 && (Build.VERSION.SDK_INT < 30 || (frameRateExo == Format.NO_VALUE))) {
            String path;
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                path = FFmpegKitConfig.getSafParameterForRead(activity, uri);
            } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // TODO: FFprobeKit doesn't accept encoded uri (like %20) (?!)
                path = uri.getSchemeSpecificPart();
            } else {
                path = uri.toString();
            }
            // Use ffprobe as ExoPlayer doesn't detect video frame rate for lots of videos
            // and has different precision than ffprobe (so do not mix that)
            MediaInformationSession mediaInformationSession = FFprobeKit.getMediaInformation(path);
            MediaInformation mediaInformation = mediaInformationSession.getMediaInformation();
            if (mediaInformation == null)
                return false;
            List<StreamInformation> streamInformations = mediaInformation.getStreams();
            for (StreamInformation streamInformation : streamInformations) {
                if (streamInformation.getType().equals("video")) {
                    String averageFrameRate = streamInformation.getAverageFrameRate();
                    if (averageFrameRate.contains("/")) {
                        String[] vals = averageFrameRate.split("/");
                        frameRate = Float.parseFloat(vals[0]) / Float.parseFloat(vals[1]);
                        break;
                    }
                }
            }

            if (BuildConfig.DEBUG)
                Toast.makeText(activity, "Video frameRate: " + frameRate, Toast.LENGTH_LONG).show();

            if (frameRate != Format.NO_VALUE) {
                Display display = activity.getWindow().getDecorView().getDisplay();
                Display.Mode[] supportedModes = display.getSupportedModes();
                Display.Mode activeMode = display.getMode();

                if (supportedModes.length > 1) {
                    // Refresh rate >= video FPS
                    List<Display.Mode> modesHigh = new ArrayList<>();
                    // Max refresh rate
                    Display.Mode modeTop = activeMode;
                    int modesResolutionCount = 0;

                    // Filter only resolutions same as current
                    for (Display.Mode mode : supportedModes) {
                        if (mode.getPhysicalWidth() == activeMode.getPhysicalWidth() &&
                                mode.getPhysicalHeight() == activeMode.getPhysicalHeight()) {
                            modesResolutionCount++;

                            if (normRate(mode.getRefreshRate()) >= normRate(frameRate))
                                modesHigh.add(mode);

                            if (normRate(mode.getRefreshRate()) > normRate(modeTop.getRefreshRate()))
                                modeTop = mode;
                        }
                    }

                    if (modesResolutionCount > 1) {
                        Display.Mode modeBest = null;

                        for (Display.Mode mode : modesHigh) {
                            if (normRate(mode.getRefreshRate()) % normRate(frameRate) <= 0.0001f) {
                                if (modeBest == null || normRate(mode.getRefreshRate()) > normRate(modeBest.getRefreshRate())) {
                                    modeBest = mode;
                                }
                            }
                        }

                        Window window = activity.getWindow();
                        WindowManager.LayoutParams layoutParams = window.getAttributes();

                        if (modeBest == null)
                            modeBest = modeTop;

                        final boolean switchingModes = !(modeBest.getModeId() == activeMode.getModeId());
                        if (switchingModes) {
                            layoutParams.preferredDisplayModeId = modeBest.getModeId();
                            window.setAttributes(layoutParams);
                        }
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(activity, "Video frameRate: " + frameRate + "\nDisplay refreshRate: " + modeBest.getRefreshRate(), Toast.LENGTH_LONG).show();
                        return switchingModes;
                    }
                }
            }
        }
        return false;
    }

    public static boolean alternativeChooser(PlayerActivity activity, Uri initialUri, boolean video) {
        String startPath;
        if (initialUri != null && (new File(initialUri.getSchemeSpecificPart())).exists()) {
            startPath = initialUri.getSchemeSpecificPart();
        } else {
            startPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        }

        final String[] suffixes = (video ? new String[] { "3gp", "m4v", "mkv", "mov", "mp4", "webm" } :
                new String[] { "srt", "ssa", "ass", "vtt", "ttml", "dfxp", "xml" });

        ChooserDialog chooserDialog = new ChooserDialog(activity)
                .withStartFile(startPath)
                .withFilter(false, false, suffixes)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        activity.releasePlayer();
                        Uri uri = Uri.parse(pathFile.toURI().toString());
                        if (video) {
                            activity.mPrefs.updateMedia(activity, uri, null);
                            activity.searchSubtitles();
                        } else {
                            // Convert subtitles to UTF-8 if necessary
                            SubtitleUtils.clearCache(activity);
                            uri = SubtitleUtils.convertToUTF(activity, uri);

                            activity.mPrefs.updateSubtitle(uri);
                        }
                        activity.initializePlayer();
                    }
                })
                // to handle the back key pressed or clicked outside the dialog:
                .withOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.cancel(); // MUST have
                    }
                });
        chooserDialog
                .withOnBackPressedListener(dialog -> chooserDialog.goBack())
                .withOnLastBackPressedListener(dialog -> dialog.cancel());
        chooserDialog.build().show();

        return true;
    }
}
