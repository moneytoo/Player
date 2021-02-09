package com.brouken.player;

import android.content.Context;
import android.net.Uri;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class UtilsSubtitles {
    public static Uri convertSubtitlesIfNecessary(final Context context, final Uri uri) {
        // Convert subtitles to UTF-8 if necessary
        try {
            final CharsetDetector detector = new CharsetDetector();
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            detector.setText(bufferedInputStream);
            final CharsetMatch charsetMatch = detector.detect();

            if (!StandardCharsets.ISO_8859_1.displayName().equals(charsetMatch.getName()) &&
                    !StandardCharsets.UTF_8.displayName().equals(charsetMatch.getName())) {
                String filename = uri.getPath();
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                final File file = new File(context.getCacheDir(), filename);
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    stream.write(charsetMatch.getString().getBytes());
                    return Uri.fromFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }
}
