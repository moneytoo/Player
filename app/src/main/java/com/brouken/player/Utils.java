package com.brouken.player;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;

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
            } catch (IOException | SecurityException e) {
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
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
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
        return result;
    }
}
