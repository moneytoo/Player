package com.brouken.player;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
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
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UtilsFeature {

    public static Uri convertToUTF(PlayerActivity activity, Uri subtitleUri) {
        try {
            String scheme = subtitleUri.getScheme();
            if (scheme != null && scheme.toLowerCase().startsWith("http")) {
                List<Uri> urls = new ArrayList<>();
                urls.add(subtitleUri);
                SubtitleFetcher subtitleFetcher = new SubtitleFetcher(activity, urls);
                subtitleFetcher.start();
                return null;
            } else {
                InputStream inputStream = activity.getContentResolver().openInputStream(subtitleUri);
                return convertInputStreamToUTF(activity, subtitleUri, inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subtitleUri;
    }

    public static Uri convertInputStreamToUTF(Context context, Uri subtitleUri, InputStream inputStream) {
        try {
            final CharsetDetector detector = new CharsetDetector();
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            detector.setText(bufferedInputStream);
            final CharsetMatch charsetMatch = detector.detect();

            if (!StandardCharsets.UTF_8.displayName().equals(charsetMatch.getName())) {
                String filename = subtitleUri.getPath();
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                final File file = new File(context.getCacheDir(), filename);
                final BufferedReader bufferedReader = new BufferedReader(charsetMatch.getReader());
                final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                char[] buffer = new char[512];
                int num;
                int pass = 0;
                boolean success = true;
                while ((num = bufferedReader.read(buffer)) != -1) {
                    bufferedWriter.write(buffer, 0, num);
                    pass++;
                    if (pass * 512 > 2_000_000) {
                        success = false;
                        break;
                    }
                }
                bufferedWriter.close();
                bufferedReader.close();
                if (success) {
                    subtitleUri = Uri.fromFile(file);
                } else {
                    subtitleUri = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return subtitleUri;
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
