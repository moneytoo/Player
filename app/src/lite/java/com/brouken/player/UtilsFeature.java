package com.brouken.player;

import android.content.Context;
import android.net.Uri;

import androidx.media3.ui.PlayerControlView;

import java.io.InputStream;

public class UtilsFeature {

    public static Uri convertToUTF(PlayerActivity activity, Uri subtitleUri) {
        return subtitleUri;
    }

    public static Uri convertInputStreamToUTF(Context context, Uri subtitleUri, InputStream inputStream) {
        return subtitleUri;
    }

    public static boolean switchFrameRate(final PlayerActivity activity, final Uri uri, final boolean play) {
        return false;
    }

    public static void markChapters(final PlayerActivity activity, final Uri uri, PlayerControlView controlView) {}
}
