package com.brouken.player;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

class UtilsFlavor {
    public static void onAppLaunch(final Context context) {
        // NOP
    }

    public static boolean switchFrameRate(final Activity activity, final float frameRateExo, final Uri uri) {
        // NOP
        return false;
    }

    public static boolean alternativeChooser(PlayerActivity activity, Uri initialUri) {
        // NOP
        return false;
    }
}
