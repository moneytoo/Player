package com.brouken.player;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class Utils {
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static boolean mediaExists(final Context context, final Uri uri) {
        if ("file".equals(uri.getScheme())) {
            final File file = new File(uri.getPath());
            return file.exists();
        } else {
            try {
                final InputStream inputStream = context.getContentResolver().openInputStream(uri);
                inputStream.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
