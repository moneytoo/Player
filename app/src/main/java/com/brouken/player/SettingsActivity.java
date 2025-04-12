package com.brouken.player;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

public class SettingsActivity extends AppCompatActivity {

    static RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
            getWindow().setNavigationBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= 35) {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    getWindow().getDecorView().setSystemUiVisibility(0);
                } else if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        }

        super.onCreate(savedInstanceState);

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

        if (Build.VERSION.SDK_INT >= 29) {
            LinearLayout layout = findViewById(R.id.settings_layout);
            layout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                view.setPadding(windowInsets.getSystemWindowInsetLeft(),
                        windowInsets.getSystemWindowInsetTop(),
                        windowInsets.getSystemWindowInsetRight(),
                        0);
                if (recyclerView != null) {
                    recyclerView.setPadding(0,0,0, windowInsets.getSystemWindowInsetBottom());
                }
                windowInsets.consumeSystemWindowInsets();
                return windowInsets;
            });
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
            ListPreference listPreferenceFileAccess = findPreference("fileAccess");
            if (listPreferenceFileAccess != null) {
                List<String> entries = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.file_access_entries)));
                List<String> values = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.file_access_values)));
                if (Build.VERSION.SDK_INT < 30) {
                    int index = values.indexOf("mediastore");
                    entries.remove(index);
                    values.remove(index);
                }
                if (!Utils.hasSAFChooser(getContext().getPackageManager())) {
                    int index = values.indexOf("saf");
                    entries.remove(index);
                    values.remove(index);
                }
                listPreferenceFileAccess.setEntries(entries.toArray(new String[0]));
                listPreferenceFileAccess.setEntryValues(values.toArray(new String[0]));
            }

            ListPreference listPreferenceLanguageAudio = findPreference("languageAudio");
            if (listPreferenceLanguageAudio != null) {
                LinkedHashMap<String, String> entries = new LinkedHashMap<>();
                entries.put(Prefs.TRACK_DEFAULT, getString(R.string.pref_language_track_default));
                entries.put(Prefs.TRACK_DEVICE, getString(R.string.pref_language_track_device));
                entries.putAll(getLanguages());
                listPreferenceLanguageAudio.setEntries(entries.values().toArray(new String[0]));
                listPreferenceLanguageAudio.setEntryValues(entries.keySet().toArray(new String[0]));
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (Build.VERSION.SDK_INT >= 29) {
                recyclerView = getListView();
            }
        }

        LinkedHashMap<String, String> getLanguages() {
            LinkedHashMap<String, String> languages = new LinkedHashMap<>();
            for (Locale locale : Locale.getAvailableLocales()) {
                try {
                    // MissingResourceException: Couldn't find 3-letter language code for zz
                    String key = locale.getISO3Language();
                    String language = locale.getDisplayLanguage();
                    int length = language.offsetByCodePoints(0, 1);
                    if (!language.isEmpty()) {
                        language = language.substring(0, length).toUpperCase(locale) + language.substring(length);
                    }
                    String value = language + " [" + key + "]";
                    languages.put(key, value);
                } catch (MissingResourceException e) {
                    e.printStackTrace();
                }
            }
            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
            Utils.orderByValue(languages, collator::compare);
            return languages;
        }
    }
}