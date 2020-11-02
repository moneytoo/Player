package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

class Prefs {
    SharedPreferences mSharedPreferences;

    public Uri mediaUri;
    public String mediaType;

    public int currentWindow = 0;
    public long playbackPosition = 0;

    public float brightness = 0.5f;

    public Prefs(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadSavedPreferences();
    }

    private void loadSavedPreferences() {
        if (mSharedPreferences.contains("mediaUri"))
            mediaUri = Uri.parse(mSharedPreferences.getString("mediaUri", null));
        if (mSharedPreferences.contains("mediaType"))
            mediaType = mSharedPreferences.getString("mediaType", null);
        currentWindow = mSharedPreferences.getInt("currentWindow", 0);
        playbackPosition = mSharedPreferences.getLong("playbackPosition", 0);
        brightness = mSharedPreferences.getFloat("brightness", 0.5f);
    }

    public void updateMedia(final Uri uri, final String type) {
        if (!uri.equals(mediaUri)) {
            updatePosition(0, 0);
        }

        mediaUri = uri;
        mediaType = type;

        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putString("mediaUri", uri.toString());
        sharedPreferencesEditor.putString("mediaType", mediaType);
        sharedPreferencesEditor.commit();
    }

    public void updatePosition(final int window, final long position) {
        currentWindow = window;
        playbackPosition = position;

        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putInt("currentWindow", window);
        sharedPreferencesEditor.putLong("playbackPosition", position);
        sharedPreferencesEditor.commit();
    }

    public void updateBrightness(final float brightness) {
        this.brightness = brightness;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putFloat("brightness", brightness);
        sharedPreferencesEditor.commit();

    }


}
