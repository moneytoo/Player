package com.brouken.player;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Prefs.initDefaults(this);

        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference preferenceAutoPiP = findPreference("autoPiP");
            if (preferenceAutoPiP != null) {
                preferenceAutoPiP.setEnabled(Utils.isPiPSupported(this.getContext()));
            }
            Preference preferenceFrameRateMatching = findPreference("frameRateMatching");
            if (preferenceFrameRateMatching != null) {
                preferenceFrameRateMatching.setEnabled(Build.VERSION.SDK_INT >= 23);
            }
        }
    }
}