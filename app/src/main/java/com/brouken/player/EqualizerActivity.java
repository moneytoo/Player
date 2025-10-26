package com.brouken.player;

import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete Equalizer implementation matching MX Player's equalizer screen
 * Features: 5-band EQ, presets, reverb, bass boost, virtualizer
 */
public class EqualizerActivity extends AppCompatActivity {
    
    // UI Components
    private Switch equalizerSwitch;
    private LinearLayout presetsContainer;
    private LinearLayout bandsContainer;
    private Spinner reverbSpinner;
    private SeekBar bassBoostSeekBar;
    private SeekBar virtualizerSeekBar;
    private TextView bassBoostValue;
    private TextView virtualizerValue;
    
    // Audio Effects
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private PresetReverb reverb;
    
    // Preset buttons
    private List<Button> presetButtons = new ArrayList<>();
    private String[] presetNames = {"Custom", "Normal", "Classical", "Dance"};
    private int currentPreset = 1; // Normal
    
    // Band controls
    private List<SeekBar> bandSeekBars = new ArrayList<>();
    private List<TextView> bandLabels = new ArrayList<>();
    private List<TextView> bandValues = new ArrayList<>();
    
    // Frequency bands (Hz)
    private String[] frequencies = {"60 Hz", "230 Hz", "910 Hz", "3600 Hz", "14000 Hz"};
    
    // Preset configurations (dB values for each band)
    private float[][] presetConfigs = {
        {0, 0, 0, 0, 0},        // Custom
        {0, 0, 0, 0, 0},        // Normal
        {3, 0, 0, 0, 3},        // Classical
        {5, 2, 0, 2, 5}         // Dance
    };
    
    private boolean isInitializing = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);
        
        initializeViews();
        setupAudioEffects();
        setupEventListeners();
        loadSavedSettings();
    }
    
    private void initializeViews() {
        // Main controls
        equalizerSwitch = findViewById(R.id.equalizer_switch);
        presetsContainer = findViewById(R.id.presets_container);
        bandsContainer = findViewById(R.id.bands_container);
        reverbSpinner = findViewById(R.id.reverb_spinner);
        bassBoostSeekBar = findViewById(R.id.bass_boost_seekbar);
        virtualizerSeekBar = findViewById(R.id.virtualizer_seekbar);
        bassBoostValue = findViewById(R.id.bass_boost_value);
        virtualizerValue = findViewById(R.id.virtualizer_value);
        
        // Create preset buttons
        createPresetButtons();
        
        // Create band controls
        createBandControls();
        
        // Setup reverb spinner
        setupReverbSpinner();
    }
    
    private void createPresetButtons() {
        presetsContainer.removeAllViews();
        presetButtons.clear();
        
        for (int i = 0; i < presetNames.length; i++) {
            Button presetButton = new Button(this);
            presetButton.setText(presetNames[i]);
            presetButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                1.0f
            ));
            
            // Style the button
            presetButton.setBackgroundResource(R.drawable.preset_button_selector);
            presetButton.setTextColor(getResources().getColorStateList(R.color.preset_button_text_color));
            presetButton.setPadding(16, 8, 16, 8);
            
            final int presetIndex = i;
            presetButton.setOnClickListener(v -> selectPreset(presetIndex));
            
            presetButtons.add(presetButton);
            presetsContainer.addView(presetButton);
            
            // Add margin between buttons
            if (i < presetNames.length - 1) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) presetButton.getLayoutParams();
                params.setMarginEnd(8);
            }
        }
        
        // Select current preset
        updatePresetSelection();
    }
    
    private void createBandControls() {
        bandsContainer.removeAllViews();
        bandSeekBars.clear();
        bandLabels.clear();
        bandValues.clear();
        
        for (int i = 0; i < frequencies.length; i++) {
            // Create container for each band
            LinearLayout bandContainer = new LinearLayout(this);
            bandContainer.setOrientation(LinearLayout.VERTICAL);
            bandContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ));
            bandContainer.setGravity(android.view.Gravity.CENTER);
            
            // Value label (dB)
            TextView valueLabel = new TextView(this);
            valueLabel.setText("0 dB");
            valueLabel.setTextColor(getResources().getColor(android.R.color.white));
            valueLabel.setTextSize(12);
            valueLabel.setGravity(android.view.Gravity.CENTER);
            bandValues.add(valueLabel);
            
            // Vertical SeekBar container
            FrameLayout seekBarContainer = new FrameLayout(this);
            seekBarContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200
            ));
            
            // SeekBar (will be rotated to vertical)
            SeekBar bandSeekBar = new SeekBar(this);
            bandSeekBar.setLayoutParams(new FrameLayout.LayoutParams(
                200, // Width becomes height when rotated
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bandSeekBar.setMax(60); // -30 to +30 dB (60 total range)
            bandSeekBar.setProgress(30); // 0 dB (middle)
            bandSeekBar.setRotation(270); // Rotate to vertical
            
            // Style seekbar
            bandSeekBar.setProgressTint(getResources().getColor(R.color.equalizer_progress_color));
            bandSeekBar.setThumbTint(getResources().getColor(R.color.equalizer_thumb_color));
            
            final int bandIndex = i;
            bandSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isInitializing) {
                        float dbValue = (progress - 30) / 2.0f; // Convert to -15 to +15 dB range
                        setBandLevel(bandIndex, dbValue);
                        updateBandValue(bandIndex, dbValue);
                        
                        // Switch to Custom preset if user manually adjusts
                        if (currentPreset != 0) {
                            selectPreset(0);
                        }
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            
            bandSeekBars.add(bandSeekBar);
            seekBarContainer.addView(bandSeekBar);
            
            // Frequency label
            TextView frequencyLabel = new TextView(this);
            frequencyLabel.setText(frequencies[i]);
            frequencyLabel.setTextColor(getResources().getColor(android.R.color.white));
            frequencyLabel.setTextSize(10);
            frequencyLabel.setGravity(android.view.Gravity.CENTER);
            bandLabels.add(frequencyLabel);
            
            // Add views to container
            bandContainer.addView(valueLabel);
            bandContainer.addView(seekBarContainer);
            bandContainer.addView(frequencyLabel);
            
            bandsContainer.addView(bandContainer);
        }
    }
    
    private void setupReverbSpinner() {
        String[] reverbOptions = {"None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, reverbOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        reverbSpinner.setAdapter(adapter);
    }
    
    private void setupAudioEffects() {
        try {
            // Get audio session ID (you might need to pass this from the player)
            int audioSessionId = 0; // This should come from your media player
            
            // Initialize equalizer
            equalizer = new Equalizer(0, audioSessionId);
            
            // Initialize bass boost
            bassBoost = new BassBoost(0, audioSessionId);
            
            // Initialize virtualizer
            virtualizer = new Virtualizer(0, audioSessionId);
            
            // Initialize reverb
            reverb = new PresetReverb(0, audioSessionId);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Show error message
            Toast.makeText(this, "Audio effects not supported on this device", Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupEventListeners() {
        // Equalizer switch
        equalizerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (equalizer != null) {
                equalizer.setEnabled(isChecked);
            }
            enableBandControls(isChecked);
        });
        
        // Bass boost seekbar
        bassBoostSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bassBoost != null) {
                    try {
                        short strength = (short) (progress * 10); // 0-1000 range
                        bassBoost.setStrength(strength);
                        bassBoostValue.setText(progress + "%");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Virtualizer seekbar
        virtualizerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && virtualizer != null) {
                    try {
                        short strength = (short) (progress * 10); // 0-1000 range
                        virtualizer.setStrength(strength);
                        virtualizerValue.setText(progress + "%");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Reverb spinner
        reverbSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (reverb != null) {
                    try {
                        if (position == 0) {
                            reverb.setEnabled(false);
                        } else {
                            reverb.setPreset((short) (position - 1));
                            reverb.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void selectPreset(int presetIndex) {
        currentPreset = presetIndex;
        updatePresetSelection();
        
        if (presetIndex == 0) {
            // Custom preset - don't change bands
            return;
        }
        
        // Apply preset configuration
        isInitializing = true;
        float[] config = presetConfigs[presetIndex];
        
        for (int i = 0; i < Math.min(config.length, bandSeekBars.size()); i++) {
            float dbValue = config[i];
            setBandLevel(i, dbValue);
            updateBandSeekBar(i, dbValue);
            updateBandValue(i, dbValue);
        }
        
        isInitializing = false;
    }
    
    private void updatePresetSelection() {
        for (int i = 0; i < presetButtons.size(); i++) {
            Button button = presetButtons.get(i);
            button.setSelected(i == currentPreset);
            
            if (i == currentPreset) {
                button.setBackgroundResource(R.drawable.preset_button_selected);
                button.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                button.setBackgroundResource(R.drawable.preset_button_normal);
                button.setTextColor(getResources().getColor(R.color.preset_button_text_normal));
            }
        }
    }
    
    private void setBandLevel(int bandIndex, float dbValue) {
        if (equalizer != null && equalizer.getEnabled()) {
            try {
                short bandLevel = (short) (dbValue * 100); // Convert dB to millibels
                equalizer.setBandLevel((short) bandIndex, bandLevel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void updateBandSeekBar(int bandIndex, float dbValue) {
        if (bandIndex < bandSeekBars.size()) {
            SeekBar seekBar = bandSeekBars.get(bandIndex);
            int progress = Math.round((dbValue * 2) + 30); // Convert dB to progress (0-60)
            seekBar.setProgress(progress);
        }
    }
    
    private void updateBandValue(int bandIndex, float dbValue) {
        if (bandIndex < bandValues.size()) {
            TextView valueLabel = bandValues.get(bandIndex);
            String valueText = String.format("%.0f dB", dbValue);
            valueLabel.setText(valueText);
        }
    }
    
    private void enableBandControls(boolean enabled) {
        for (SeekBar seekBar : bandSeekBars) {
            seekBar.setEnabled(enabled);
            seekBar.setAlpha(enabled ? 1.0f : 0.5f);
        }
        
        for (Button button : presetButtons) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1.0f : 0.5f);
        }
        
        bassBoostSeekBar.setEnabled(enabled);
        virtualizerSeekBar.setEnabled(enabled);
        reverbSpinner.setEnabled(enabled);
    }
    
    private void loadSavedSettings() {
        // Load saved equalizer settings from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("equalizer_prefs", MODE_PRIVATE);
        
        // Load equalizer enabled state
        boolean isEnabled = prefs.getBoolean("equalizer_enabled", false);
        equalizerSwitch.setChecked(isEnabled);
        
        // Load current preset
        currentPreset = prefs.getInt("current_preset", 1);
        updatePresetSelection();
        
        // Load band levels for custom preset
        if (currentPreset == 0) {
            isInitializing = true;
            for (int i = 0; i < frequencies.length; i++) {
                float dbValue = prefs.getFloat("band_" + i, 0.0f);
                setBandLevel(i, dbValue);
                updateBandSeekBar(i, dbValue);
                updateBandValue(i, dbValue);
            }
            isInitializing = false;
        } else {
            selectPreset(currentPreset);
        }
        
        // Load bass boost
        int bassBoostLevel = prefs.getInt("bass_boost", 0);
        bassBoostSeekBar.setProgress(bassBoostLevel);
        bassBoostValue.setText(bassBoostLevel + "%");
        
        // Load virtualizer
        int virtualizerLevel = prefs.getInt("virtualizer", 0);
        virtualizerSeekBar.setProgress(virtualizerLevel);
        virtualizerValue.setText(virtualizerLevel + "%");
        
        // Load reverb
        int reverbPreset = prefs.getInt("reverb", 0);
        reverbSpinner.setSelection(reverbPreset);
        
        enableBandControls(isEnabled);
    }
    
    private void saveSettings() {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("equalizer_prefs", MODE_PRIVATE).edit();
        
        // Save equalizer enabled state
        editor.putBoolean("equalizer_enabled", equalizerSwitch.isChecked());
        
        // Save current preset
        editor.putInt("current_preset", currentPreset);
        
        // Save band levels for custom preset
        for (int i = 0; i < bandSeekBars.size(); i++) {
            SeekBar seekBar = bandSeekBars.get(i);
            float dbValue = (seekBar.getProgress() - 30) / 2.0f;
            editor.putFloat("band_" + i, dbValue);
        }
        
        // Save bass boost
        editor.putInt("bass_boost", bassBoostSeekBar.getProgress());
        
        // Save virtualizer
        editor.putInt("virtualizer", virtualizerSeekBar.getProgress());
        
        // Save reverb
        editor.putInt("reverb", reverbSpinner.getSelectedItemPosition());
        
        editor.apply();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Release audio effects
        if (equalizer != null) {
            equalizer.release();
        }
        if (bassBoost != null) {
            bassBoost.release();
        }
        if (virtualizer != null) {
            virtualizer.release();
        }
        if (reverb != null) {
            reverb.release();
        }
    }
    
    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }
}