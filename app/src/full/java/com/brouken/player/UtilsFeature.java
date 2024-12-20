package com.brouken.player;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;

import androidx.media3.common.Format;
import androidx.media3.ui.PlayerControlView;

import com.arthenica.ffmpegkit.Chapter;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.StreamInformation;

import java.util.List;

public class UtilsFeature {

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
                        Utils.playIfCan(activity, play);
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
                Utils.handleFrameRate(activity, frameRate, play);
            });
            activity.frameRateSwitchThread.start();
            return true;
        } else {
            return false;
        }
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


}
