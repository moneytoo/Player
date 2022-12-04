package com.brouken.player;

import android.net.Uri;

import androidx.media3.common.util.Util;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

public class SubtitleFinder {

    private PlayerActivity activity;
    private Uri baseUri;
    private String path;
    private final List<Uri> urls;

    public SubtitleFinder(PlayerActivity activity, Uri uri) {
        this.activity = activity;
        path = uri.getPath();
        path = path.substring(0, path.lastIndexOf('.'));
        baseUri = uri;
        urls = new ArrayList<>();
    }

    public static boolean isUriCompatible(Uri uri) {
        String pth = uri.getPath();
        if (pth != null) {
            return pth.lastIndexOf('.') > -1;
        }
        return false;
    }

    private void addLanguage(String lang, String suffix) {
        urls.add(buildUri(lang + "." + suffix));
        urls.add(buildUri(Util.normalizeLanguageCode(lang) + "." + suffix));
    }

    private Uri buildUri(String suffix) {
        final String newPath = path + "." + suffix;
        return baseUri.buildUpon().path(newPath).build();
    }

    public void start() {
        // Prevent IllegalArgumentException in okhttp3.Request.Builder
        if (HttpUrl.parse(baseUri.toString()) == null) {
            return;
        }

        for (String suffix : new String[] { "srt", "ssa", "ass" }) {
            urls.add(buildUri(suffix));
            for (String language : Utils.getDeviceLanguages()) {
                addLanguage(language, suffix);
            }
        }
        urls.add(buildUri("vtt"));

        SubtitleFetcher subtitleFetcher = new SubtitleFetcher(activity, urls);
        subtitleFetcher.start();
    }

}
