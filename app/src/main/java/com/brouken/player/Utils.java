package com.brouken.player;

import static android.content.Context.UI_MODE_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.ui.PlayerControlView;

import com.arthenica.ffmpegkit.Chapter;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.StreamInformation;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class Utils {

    public static final String FEATURE_FIRE_TV = "amazon.hardware.fire_tv";

    public static final String[] supportedExtensionsVideo = new String[] { "3gp", "avi", "m4v", "mkv", "mov", "mp4", "ts", "webm" };
    public static final String[] supportedExtensionsSubtitle = new String[] { "srt", "ssa", "ass", "vtt", "ttml", "dfxp", "xml" };

    public static final String[] supportedMimeTypesVideo = new String[] {
            // Local mime types on Android:
            MimeTypes.VIDEO_MATROSKA, // .mkv
            MimeTypes.VIDEO_MP4, // .mp4, .m4v
            MimeTypes.VIDEO_WEBM, // .webm
            "video/quicktime", // .mov
            "video/mp2ts", // .ts, but also incompatible .m2ts
            MimeTypes.VIDEO_H263, // .3gp
            "video/avi",
            // For remote storages:
            "video/x-m4v", // .m4v
    };
    public static final String[] supportedMimeTypesSubtitle = new String[] {
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.TEXT_SSA,
            MimeTypes.TEXT_VTT,
            MimeTypes.APPLICATION_TTML,
            "text/*",
            "application/octet-stream"
    };

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static float pxToDp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    public static boolean fileExists(final Context context, final Uri uri) {
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try {
                final InputStream inputStream = context.getContentResolver().openInputStream(uri);
                inputStream.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            String path;
            if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                path = uri.getPath();
            } else {
                path = uri.toString();
            }
            final File file = new File(path);
            return file.exists();
        }
    }

    public static void toggleSystemUi(final Activity activity, final CustomPlayerView playerView, final boolean show) {
        if (Build.VERSION.SDK_INT >= 31) {
            Window window = activity.getWindow();
            if (window != null) {
                WindowInsetsController windowInsetsController = window.getInsetsController();
                if (windowInsetsController != null) {
                    if (show) {
                        windowInsetsController.show(WindowInsets.Type.systemBars());
                    } else {
                        windowInsetsController.hide(WindowInsets.Type.systemBars());
                    }
                }
            }
        } else {
            if (show) {
                playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            } else {
                playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex > -1)
                            result = cursor.getString(columnIndex);
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
            if (result.indexOf(".") > 0)
                result = result.substring(0, result.lastIndexOf("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isVolumeMax(final AudioManager audioManager) {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static boolean isVolumeMin(final AudioManager audioManager) {
        int min = Build.VERSION.SDK_INT >= 28 ? audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) : 0;
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == min;
    }

    public static void adjustVolume(final Context context, final AudioManager audioManager, final CustomPlayerView playerView, final boolean raise, boolean canBoost, boolean clear) {
        playerView.removeCallbacks(playerView.textClearRunnable);

        final int volume = getVolume(context,false, audioManager);
        final int volumeMax = getVolume(context,true, audioManager);
        boolean volumeActive = volume != 0;

        // Handle volume changes outside the app (lose boost if volume is not maxed out)
        if (volume != volumeMax) {
            PlayerActivity.boostLevel = 0;
        }

        if (PlayerActivity.loudnessEnhancer == null)
            canBoost = false;

        if (volume != volumeMax || (PlayerActivity.boostLevel == 0 && !raise)) {
            if (PlayerActivity.loudnessEnhancer != null)
                PlayerActivity.loudnessEnhancer.setEnabled(false);
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, raise ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            final int volumeNew = getVolume(context, false, audioManager);
            // Custom volume step on Samsung devices (Sound Assistant)
            if (raise && volume == volumeNew) {
                playerView.volumeUpsInRow++;
            } else {
                playerView.volumeUpsInRow = 0;
            }
            if (playerView.volumeUpsInRow > 4 && !isVolumeMin(audioManager)) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
            } else {
                volumeActive = volumeNew != 0;
                playerView.setCustomErrorMessage(volumeActive ? " " + volumeNew : "");
            }
        } else {
            if (canBoost && raise && PlayerActivity.boostLevel < 10)
                PlayerActivity.boostLevel++;
            else if (!raise && PlayerActivity.boostLevel > 0)
                PlayerActivity.boostLevel--;

            if (PlayerActivity.loudnessEnhancer != null) {
                try {
                    PlayerActivity.loudnessEnhancer.setTargetGain(PlayerActivity.boostLevel * 200);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            playerView.setCustomErrorMessage(" " + (volumeMax + PlayerActivity.boostLevel));
        }

        playerView.setIconVolume(volumeActive);
        if (PlayerActivity.loudnessEnhancer != null)
            PlayerActivity.loudnessEnhancer.setEnabled(PlayerActivity.boostLevel > 0);
        playerView.setHighlight(PlayerActivity.boostLevel > 0);

        if (clear) {
            playerView.postDelayed(playerView.textClearRunnable, CustomPlayerView.MESSAGE_TIMEOUT_KEY);
        }
    }

    private static int getVolume(final Context context, final boolean max, final AudioManager audioManager) {
        if (Build.VERSION.SDK_INT >= 30 && Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            try {
                Method method;
                Object result;
                Class<?> clazz = Class.forName("com.samsung.android.media.SemSoundAssistantManager");
                Constructor<?> constructor = clazz.getConstructor(Context.class);
                final Method getMediaVolumeInterval = clazz.getDeclaredMethod("getMediaVolumeInterval");
                result = getMediaVolumeInterval.invoke(constructor.newInstance(context));
                if (result instanceof Integer) {
                    int mediaVolumeInterval = (int) result;
                    if (mediaVolumeInterval < 10) {
                        method = AudioManager.class.getDeclaredMethod("semGetFineVolume", int.class);
                        result = method.invoke(audioManager, AudioManager.STREAM_MUSIC);
                        if (result instanceof Integer) {
                            if (max) {
                                return 150 / mediaVolumeInterval;
                            } else {
                                int fineVolume = (int) result;
                                return fineVolume / mediaVolumeInterval;
                            }
                        }
                    }
                }
            } catch (Exception e) {}
        }
        if (max) {
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        } else {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    }

    public static void setButtonEnabled(final Context context, final ImageButton button, final boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ?
                        (float) context.getResources().getInteger(R.integer.exo_media_button_opacity_percentage_enabled) / 100 :
                        (float) context.getResources().getInteger(R.integer.exo_media_button_opacity_percentage_disabled) / 100
                );
    }

    public static void showText(final CustomPlayerView playerView, final String text, final long timeout) {
        playerView.removeCallbacks(playerView.textClearRunnable);
        playerView.clearIcon();
        playerView.setCustomErrorMessage(text);
        playerView.postDelayed(playerView.textClearRunnable, timeout);
    }

    public static void showText(final CustomPlayerView playerView, final String text) {
        showText(playerView, text, 1200);
    }

    public enum Orientation {
        VIDEO(0, R.string.video_orientation_video),
        SYSTEM(1, R.string.video_orientation_system),
        UNSPECIFIED(2, R.string.video_orientation_system);

        public final int value;
        public final int description;

        Orientation(int type, int description) {
            this.value = type;
            this.description = description;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void setOrientation(Activity activity, Orientation orientation) {
        switch (orientation) {
            case VIDEO:
                if (PlayerActivity.player != null) {
                    final Format format = PlayerActivity.player.getVideoFormat();
                    if (format != null && isPortrait(format))
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    else
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }

                break;
            case SYSTEM:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            /*case SENSOR:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;*/
        }
    }

    public static Orientation getNextOrientation(Orientation orientation) {
        switch (orientation) {
            case VIDEO:
                return Orientation.SYSTEM;
            case SYSTEM:
            default:
                return Orientation.VIDEO;
        }
    }

    public static boolean isRotated(final Format format) {
        return format.rotationDegrees == 90 || format.rotationDegrees == 270;
    }

    public static boolean isPortrait(final Format format) {
        if (isRotated(format)) {
            return format.width > format.height;
        } else {
            return format.height > format.width;
        }
    }

    public static Rational getRational(final Format format) {
        if (isRotated(format))
            return new Rational(format.height, format.width);
        else
            return new Rational(format.width, format.height);
    }

    public static String formatMilis(long time) {
        final int totalSeconds = Math.abs((int) time / 1000);
        final int seconds = totalSeconds % 60;
        final int minutes = totalSeconds % 3600 / 60;
        final int hours = totalSeconds / 3600;

        return (hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds));
    }

    public static String formatMilisSign(long time) {
        if (time > -1000 && time < 1000)
            return formatMilis(time);
        else
            return (time < 0 ? "−" : "+") + formatMilis(time);
    }

    public static void log(final String text) {
        if (BuildConfig.DEBUG) {
            Log.d("JustPlayer", text);
        }
    }

    public static void setViewMargins(final View view, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        view.setLayoutParams(layoutParams);
    }

    public static void setViewParams(final View view, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        setViewMargins(view, marginLeft, marginTop, marginRight, marginBottom);
    }

    public static boolean isDeletable(final Context context, final Uri uri) {
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_FLAGS}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
                        if (columnIndex > -1) {
                            int flags = cursor.getInt(columnIndex);
                            return (flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) == DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
                        }
                    }
                }
            } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                if (Build.VERSION.SDK_INT >= 23) {
                    boolean hasPermission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
                    if (!hasPermission) {
                        return false;
                    }
                }
                final File file = new File(uri.getSchemeSpecificPart());
                return file.canWrite();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportedNetworkUri(final Uri uri) {
        if (uri == null)
            return false;
        final String scheme = uri.getScheme();
        if (scheme == null)
            return false;
        return scheme.startsWith("http") || scheme.equals("rtsp");
    }

    public static boolean isTvBox(Context context) {
        final PackageManager pm = context.getPackageManager();

        // TV for sure
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }

        if (pm.hasSystemFeature(FEATURE_FIRE_TV)) {
            return true;
        }

        // Missing Files app (DocumentsUI) means box (some boxes still have non functional app or stub)
        if (!hasSAFChooser(pm)) {
            return true;
        }

        // Legacy storage no longer works on Android 11 (level 30)
        if (Build.VERSION.SDK_INT < 30) {
            // (Some boxes still report touchscreen feature)
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
                return true;
            }

            if (pm.hasSystemFeature("android.hardware.hdmi.cec")) {
                return true;
            }

            if (Build.MANUFACTURER.equalsIgnoreCase("zidoo")) {
                return true;
            }
        }

        // Default: No TV - use SAF
        return false;
    }

    public static boolean hasSAFChooser(final PackageManager pm) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        return intent.resolveActivity(pm) != null;
    }

    public static int normRate(float rate) {
        return (int)(rate * 100f);
    }

    public static boolean switchFrameRate(final PlayerActivity activity, final Uri uri, final boolean play) {
        // preferredDisplayModeId only available on SDK 23+
        // ExoPlayer already uses Surface.setFrameRate() on Android 11+
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.frameRateSwitchThread != null) {
                activity.frameRateSwitchThread.interrupt();
            }
            activity.frameRateSwitchThread = new Thread(() -> {
                // Use ffprobe as ExoPlayer doesn't detect video frame rate for lots of videos
                // and has different precision than ffprobe (so do not mix that)
                float frameRate = Format.NO_VALUE;
                MediaInformation mediaInformation = getMediaInformation(activity, uri);
                if (mediaInformation == null) {
                    activity.runOnUiThread(() -> {
                        playIfCan(activity, play);
                    });
                    return;
                }
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
                handleFrameRate(activity, frameRate, play);
            });
            activity.frameRateSwitchThread.start();
            return true;
        } else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void handleFrameRate(final PlayerActivity activity, float frameRate, boolean play) {
        activity.runOnUiThread(() -> {
            boolean switchingModes = false;

            if (BuildConfig.DEBUG)
                Toast.makeText(activity, "Video frameRate: " + frameRate, Toast.LENGTH_LONG).show();

            if (frameRate > 0) {
                Display display = activity.getWindow().getDecorView().getDisplay();
                if (display == null) {
                    return;
                }
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
                        String modes = "Available refreshRates:";

                        for (Display.Mode mode : modesHigh) {
                            modes += " " + mode.getRefreshRate();
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

                        switchingModes = !(modeBest.getModeId() == activeMode.getModeId());
                        if (switchingModes) {
                            layoutParams.preferredDisplayModeId = modeBest.getModeId();
                            window.setAttributes(layoutParams);
                        }
                        if (BuildConfig.DEBUG)
                            Toast.makeText(activity, modes + "\n" +
                                    "Video frameRate: " + frameRate + "\n" +
                                    "Current display refreshRate: " + modeBest.getRefreshRate(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            if (!switchingModes) {
                playIfCan(activity, play);
            }
        });
    }

    private static void playIfCan(final PlayerActivity activity, boolean play) {
        if (play) {
            if (PlayerActivity.player != null)
                PlayerActivity.player.play();
            if (activity.playerView != null)
                activity.playerView.hideController();
        }
    }

    public static boolean alternativeChooser(PlayerActivity activity, Uri initialUri, boolean video) {
        String startPath;
        if (initialUri != null && (new File(initialUri.getSchemeSpecificPart())).exists()) {
            startPath = initialUri.getSchemeSpecificPart();
        } else {
            startPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        }

        final String[] suffixes = (video ? supportedExtensionsVideo : supportedExtensionsSubtitle);

        ChooserDialog chooserDialog = new ChooserDialog(activity, R.style.FileChooserStyle_Dark)
                .withStartFile(startPath)
                .withFilter(false, false, suffixes)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        activity.releasePlayer();
                        Uri uri = DocumentFile.fromFile(pathFile).getUri();
                        if (video) {
                            activity.mPrefs.setPersistent(true);
                            activity.mPrefs.updateMedia(activity, uri, null);
                            activity.searchSubtitles();
                        } else {
                            // Convert subtitles to UTF-8 if necessary
                            SubtitleUtils.clearCache(activity);
                            uri = SubtitleUtils.convertToUTF(activity, uri);

                            activity.mPrefs.updateSubtitle(uri);
                        }
                        PlayerActivity.focusPlay = true;
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

    public static boolean isPiPSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (BuildConfig.FLAVOR_distribution.equals("amazon") && packageManager.hasSystemFeature(FEATURE_FIRE_TV)) {
            return false;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public static Uri getMoviesFolderUri() {
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= 26) {
            final String authority = "com.android.externalstorage.documents";
            final String documentId = "primary:" + Environment.DIRECTORY_MOVIES;
            uri = DocumentsContract.buildDocumentUri(authority, documentId);
        }
        return uri;
    }

    public static boolean isProgressiveContainerUri(final Uri uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        path = path.toLowerCase();
        for (String extension : supportedExtensionsVideo) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getDeviceLanguages() {
        final List<String> locales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 24) {
            final LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
            for (int i = 0; i < localeList.size(); i++) {
                locales.add(localeList.get(i).getISO3Language());
            }
        } else {
            final Locale locale = Resources.getSystem().getConfiguration().locale;
            locales.add(locale.getISO3Language());
        }
        return locales.toArray(new String[0]);
    }

    public static ComponentName getSystemComponent(Context context, Intent intent) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos.size() < 2) {
            return null;
        }
        int systemCount = 0;
        ComponentName componentName = null;
        for (ResolveInfo resolveInfo : resolveInfos) {
            int flags = resolveInfo.activityInfo.applicationInfo.flags;
            boolean system = (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system) {
                systemCount++;
                componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            }
        }
        if (systemCount == 1) {
            return componentName;
        }
        return null;
    }

    public static float normalizeScaleFactor(float scaleFactor, float min) {
        return Math.max(min, Math.min(scaleFactor, 2.0f));
    }

    private static MediaInformation getMediaInformation(final Activity activity, final Uri uri) {
        String path;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try {
                path = FFmpegKitConfig.getSafParameterForRead(activity, uri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            // TODO: FFprobeKit doesn't accept encoded uri (like %20) (?!)
            path = uri.getSchemeSpecificPart();
        } else {
            path = uri.toString();
        }
        MediaInformationSession mediaInformationSession = FFprobeKit.getMediaInformation(path);
        return mediaInformationSession.getMediaInformation();
    }

    public static void markChapters(final PlayerActivity activity, final Uri uri, PlayerControlView controlView) {
        if (activity.chaptersThread != null) {
            activity.chaptersThread.interrupt();
        }
        activity.chaptersThread = new Thread(() -> {
            MediaInformation mediaInformation = getMediaInformation(activity, uri);
            if (mediaInformation == null)
                return;
            final List<Chapter> chapters = mediaInformation.getChapters();
            final long[] starts = new long[chapters.size()];
            final boolean[] played = new boolean[chapters.size()];

            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                final long start = chapter.getStart();
                if (start > 0) {
                    starts[i] = start / 1_000_000;
                    played[i] = true;
                }
            }
            activity.chapterStarts = starts;
            activity.runOnUiThread(() -> controlView.setExtraAdGroupMarkers(starts, played));
        });
        activity.chaptersThread.start();
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 720;
    }

    public static <K, V> void orderByValue(LinkedHashMap<K, V> m, final Comparator<? super V> c) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(m.entrySet());
        Collections.sort(entries, (lhs, rhs) -> c.compare(lhs.getValue(), rhs.getValue()));
        m.clear();
        for(Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
    }
}
