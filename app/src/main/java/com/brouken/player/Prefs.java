package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;

class Prefs {

    private static final String PREF_KEY_MEDIA_URI = "mediaUri";
    private static final String PREF_KEY_MEDIA_TYPE = "mediaType";
    private static final String PREF_KEY_BRIGHTNESS = "brightness";
    private static final String PREF_KEY_FIRST_RUN = "firstRun";
    private static final String PREF_KEY_SUBTITLE_URI = "subtitleUri";
    private static final String PREF_KEY_AUDIO_TRACK = "audioTrack";
    private static final String PREF_KEY_SUBTITLE_TRACK = "subtitleTrack";
    private static final String PREF_KEY_RESIZE_MODE = "resizeMode";
    private static final String PREF_KEY_ORIENTATION = "orientation";

    final Context mContext;
    final SharedPreferences mSharedPreferences;

    public Uri mediaUri;
    public Uri subtitleUri;
    public String mediaType;
    public int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    public Utils.Orientation orientation = Utils.Orientation.VIDEO;

    public int subtitleTrack = -1;
    public int audioTrack = -1;

    public int brightness = -1;
    public boolean firstRun = true;

    private LinkedHashMap positions;

    public Prefs(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadSavedPreferences();
        loadPositions();
    }

    private void loadSavedPreferences() {
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_URI))
            mediaUri = Uri.parse(mSharedPreferences.getString(PREF_KEY_MEDIA_URI, null));
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_TYPE))
            mediaType = mSharedPreferences.getString(PREF_KEY_MEDIA_TYPE, null);
        brightness = mSharedPreferences.getInt(PREF_KEY_BRIGHTNESS, brightness);
        firstRun = mSharedPreferences.getBoolean(PREF_KEY_FIRST_RUN, firstRun);
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_URI))
            subtitleUri = Uri.parse(mSharedPreferences.getString(PREF_KEY_SUBTITLE_URI, null));
        if (mSharedPreferences.contains(PREF_KEY_AUDIO_TRACK))
            audioTrack = mSharedPreferences.getInt(PREF_KEY_AUDIO_TRACK, audioTrack);
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_TRACK))
            subtitleTrack = mSharedPreferences.getInt(PREF_KEY_SUBTITLE_TRACK, subtitleTrack);
        if (mSharedPreferences.contains(PREF_KEY_RESIZE_MODE))
            resizeMode = mSharedPreferences.getInt(PREF_KEY_RESIZE_MODE, resizeMode);
        orientation = Utils.Orientation.values()[mSharedPreferences.getInt(PREF_KEY_ORIENTATION, 1)];
    }

    public void updateMedia(final Uri uri, final String type) {
        mediaUri = uri;
        mediaType = type;
        updateSubtitle(null);
        updateAudioTrack(-1);
        updateSubtitleTrack(-1);
        updateResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (uri == null)
            sharedPreferencesEditor.remove(PREF_KEY_MEDIA_URI);
        else
            sharedPreferencesEditor.putString(PREF_KEY_MEDIA_URI, uri.toString());
        if (type == null)
            sharedPreferencesEditor.remove(PREF_KEY_MEDIA_TYPE);
        else
            sharedPreferencesEditor.putString(PREF_KEY_MEDIA_TYPE, mediaType);
        sharedPreferencesEditor.commit();
    }

    public void updateSubtitle(final Uri uri) {
        subtitleUri = uri;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (uri == null)
            sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_URI);
        else
            sharedPreferencesEditor.putString(PREF_KEY_SUBTITLE_URI, uri.toString());
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
        if (brightness >= 0) {
            this.brightness = brightness;
            final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
            sharedPreferencesEditor.putInt(PREF_KEY_BRIGHTNESS, brightness);
            sharedPreferencesEditor.commit();
        }
    }

    public void markFirstRun() {
        this.firstRun = false;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(PREF_KEY_FIRST_RUN, false);
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
            return 0L;
        else
            return (long) val;
    }

    public void updateAudioTrack(final int track) {
        this.audioTrack = track;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (track < 0)
            sharedPreferencesEditor.remove(PREF_KEY_AUDIO_TRACK);
        else
            sharedPreferencesEditor.putInt(PREF_KEY_AUDIO_TRACK, track);
        sharedPreferencesEditor.commit();
    }

    public void updateSubtitleTrack(final int track) {
        this.subtitleTrack = track;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (track < 0)
            sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK);
        else
            sharedPreferencesEditor.putInt(PREF_KEY_SUBTITLE_TRACK, track);
        sharedPreferencesEditor.commit();
    }

    public void updateResizeMode(final int mode) {
        this.resizeMode = mode;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putInt(PREF_KEY_RESIZE_MODE, resizeMode);
        sharedPreferencesEditor.commit();
    }

    public void updateOrientation() {
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putInt(PREF_KEY_ORIENTATION, orientation.value);
        sharedPreferencesEditor.commit();
    }
}
