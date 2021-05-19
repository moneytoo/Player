package com.brouken.player;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.exoplayer2.Format;

import java.io.File;
import java.io.InputStream;

class Utils {
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static float pxToDp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    public static boolean fileExists(final Context context, final Uri uri) {
        if ("file".equals(uri.getScheme())) {
            final File file = new File(uri.getPath());
            return file.exists();
        } else {
            try {
                final InputStream inputStream = context.getContentResolver().openInputStream(uri);
                inputStream.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static void hideSystemUi(final CustomStyledPlayerView playerView) {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // demo
//        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public static void showSystemUi(final CustomStyledPlayerView playerView) {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (uri.getScheme().equals("content")) {
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

    public static void adjustVolume(final AudioManager audioManager, final CustomStyledPlayerView playerView, final boolean raise, boolean canBoost) {
        final int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
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
            final int volumeNew = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (raise && volume == volumeNew && !isVolumeMin(audioManager)) {
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
    }

    public static void setButtonEnabled(final Context context, final ImageButton button, final boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ?
                        (float) context.getResources().getInteger(R.integer.exo_media_button_opacity_percentage_enabled) / 100 :
                        (float) context.getResources().getInteger(R.integer.exo_media_button_opacity_percentage_disabled) / 100
                );
    }

    public static void showText(final CustomStyledPlayerView playerView, final String text, final long timeout) {
        playerView.removeCallbacks(playerView.textClearRunnable);
        playerView.clearIcon();
        playerView.setCustomErrorMessage(text);
        playerView.postDelayed(playerView.textClearRunnable, timeout);
    }

    public static void showText(final CustomStyledPlayerView playerView, final String text) {
        showText(playerView, text, 1200);
    }

    public enum Orientation {
        VIDEO(0, R.string.video_orientation_video),
        SENSOR(1, R.string.video_orientation_sensor);

        public final int value;
        public final int description;

        Orientation(int type, int description) {
            this.value = type;
            this.description = description;
        }
    }

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
            case SENSOR:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
            /*case SYSTEM:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;*/
        }
    }

    public static Orientation getNextOrientation(Orientation orientation) {
        switch (orientation) {
            case VIDEO:
                return Orientation.SENSOR;
            case SENSOR:
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

    public static void setViewParams(final View view, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        view.setLayoutParams(layoutParams);
    }

    public static boolean isDeletable(final Context context, final Uri uri) {
        try {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_FLAGS}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
                        if (columnIndex > -1) {
                            int flags = cursor.getInt(columnIndex);
                            return (flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) == DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportedUri(final Uri uri) {
        final String scheme = uri.getScheme();
        return scheme.startsWith("http") || scheme.equals("rtsp");
    }
}
