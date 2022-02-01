package com.brouken.player;

import android.net.Uri;

import com.google.android.exoplayer2.util.Util;

import java.util.LinkedHashMap;

import okhttp3.HttpUrl;

public class SubtitleFinder {

    private PlayerActivity activity;
    private Uri baseUri;
    private String path;
    private final LinkedHashMap<Uri, Boolean> urls;

    public SubtitleFinder(PlayerActivity activity, Uri uri) {
        this.activity = activity;
        path = uri.getPath();
        path = path.substring(0, path.lastIndexOf('.'));
        baseUri = uri;
        urls = new LinkedHashMap<>();
    }

    public static boolean isUriCompatible(Uri uri) {
        String pth = uri.getPath();
        if (pth != null) {
            return pth.lastIndexOf('.') > -1;
        }
        return false;
    }

    private void addLanguage(String lang) {
        urls.put(buildUri(lang + ".srt"), false);
        urls.put(buildUri(Util.normalizeLanguageCode(lang) + ".srt"), false);
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

        urls.put(buildUri("srt"), false);
        for (String language : Utils.getDeviceLanguages()) {
            addLanguage(language);
        }
        urls.put(buildUri("ssa"), false);
        urls.put(buildUri("ass"), false);
        urls.put(buildUri("vtt"), false);

        SubtitleFetcher subtitleFetcher = new SubtitleFetcher(activity, urls);
        subtitleFetcher.start();
    }

}
