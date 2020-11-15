package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;

class Prefs {
    Context mContext;
    SharedPreferences mSharedPreferences;

    public Uri mediaUri;
    public Uri subtitleUri;
    public String mediaType;

    public int brightness = 20;
    public boolean firstRun = true;

    private LinkedHashMap positions;

    public Prefs(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadSavedPreferences();
        loadPositions();
    }

    private void loadSavedPreferences() {
        if (mSharedPreferences.contains("mediaUri"))
            mediaUri = Uri.parse(mSharedPreferences.getString("mediaUri", null));
        if (mSharedPreferences.contains("mediaType"))
            mediaType = mSharedPreferences.getString("mediaType", null);
        brightness = mSharedPreferences.getInt("brightness", brightness);
        firstRun = mSharedPreferences.getBoolean("firstRun", firstRun);
        if (mSharedPreferences.contains("subtitleUri"))
            subtitleUri = Uri.parse(mSharedPreferences.getString("subtitleUri", null));
    }

    public void updateMedia(final Uri uri, final String type) {
        mediaUri = uri;
        mediaType = type;
        updateSubtitle(null);

        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (uri == null)
            sharedPreferencesEditor.remove("mediaUri");
        else
            sharedPreferencesEditor.putString("mediaUri", uri.toString());
        if (type == null)
            sharedPreferencesEditor.remove("mediaType");
        else
            sharedPreferencesEditor.putString("mediaType", mediaType);
        sharedPreferencesEditor.commit();
    }

    public void updateSubtitle(final Uri uri) {
        subtitleUri = uri;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (uri == null)
            sharedPreferencesEditor.remove("subtitleUri");
        else
            sharedPreferencesEditor.putString("subtitleUri", uri.toString());
        sharedPreferencesEditor.commit();
    }

    public void updatePosition(final long position) {
        if (mediaUri == null)
            return;

        while (positions.size() > 100)
            positions.remove(positions.keySet().toArray()[0]);

        positions.put(mediaUri.toString(), position);
        savePositions();
    }

    public void updateBrightness(final int brightness) {
        this.brightness = brightness;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putInt("brightness", brightness);
        sharedPreferencesEditor.commit();

    }

    public void markFirstRun() {
        this.firstRun = false;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("firstRun", false);
        sharedPreferencesEditor.commit();
    }

    private void savePositions() {
        try {
            FileOutputStream fos = mContext.openFileOutput("positions", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(positions);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        try {
            FileInputStream fis = mContext.openFileInput("positions");
            ObjectInputStream is = new ObjectInputStream(fis);
            positions = (LinkedHashMap) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            positions = new LinkedHashMap(10);
        }
    }

    public long getPosition() {
        Object val = positions.get(mediaUri.toString());
        if (val == null)
            return 0l;
        else
            return (long) positions.get(mediaUri.toString());
    }
}
