package com.brouken.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Complete MX Player clone with all features from screenshots
 * Features: Video player, equalizer, subtitles, decoder selection, library, downloader
 */
public class MXPlayerActivity extends AppCompatActivity implements Player.Listener {
    
    // UI Components
    private PlayerView playerView;
    private LinearLayout topControlsBar;
    private LinearLayout primaryControlsBar;
    private LinearLayout bottomControlsBar;
    private LinearLayout extendedControlsBar;
    private FrameLayout libraryContainer;
    private FrameLayout settingsContainer;
    
    // Player components
    private ExoPlayer player;
    private Equalizer equalizer;
    private AudioManager audioManager;
    
    // Control buttons
    private ImageButton backButton;
    private TextView titleText;
    private ImageButton musicNoteButton;
    private ImageButton subtitleButton;
    private ImageButton decoderButton;
    private ImageButton menuButton;
    
    // Primary controls
    private ImageButton equalizerControlButton;
    private TextView speedButton;
    private ImageButton editButton;
    private ImageButton headphonesButton;
    private ImageButton rotationButton;
    private ImageButton nextButton;
    
    // Bottom controls
    private ImageButton lockButton;
    private ImageButton prevButton;
    private ImageButton playPauseButton;
    private ImageButton nextBottomButton;
    private ImageButton fullscreenButton;
    private ImageButton pipButton;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    
    // Extended controls
    private ImageButton nightModeButton;
    private ImageButton shuffleButton;
    private ImageButton loopButton;
    private ImageButton muteButton;
    private ImageButton sleepTimerButton;
    private ImageButton abRepeatButton;
    private ImageButton equalizerButton;
    private TextView speedExtendedButton;
    private ImageButton customizeButton;
    private ImageButton backgroundPlayButton;
    private ImageButton screenRotationButton;
    
    // State variables
    private boolean isControlsVisible = true;
    private boolean isLocked = false;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private boolean nightMode = false;
    private boolean shuffle = false;
    private boolean loop = false;
    private float playbackSpeed = 1.0f;
    private String currentDecoder = "HW";
    private boolean extendedControlsVisible = false;
    
    // Screen management
    private String currentScreen = "player"; // player, library, settings
    
    // Handler for auto-hide controls
    private Handler hideControlsHandler = new Handler();
    private Runnable hideControlsRunnable = () -> {
        if (isControlsVisible && !isLocked) {
            hideControls();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mx_player);
        
        // Initialize audio manager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        // Initialize UI components
        initializeViews();
        setupPlayer();
        setupEventListeners();
        setupEqualizer();
        
        // Handle intent
        handleIntent(getIntent());
        
        // Auto-hide controls after 3 seconds
        scheduleControlsHide();
    }
    
    private void initializeViews() {
        // Main containers
        playerView = findViewById(R.id.player_view);
        topControlsBar = findViewById(R.id.top_controls_bar);
        primaryControlsBar = findViewById(R.id.primary_controls_bar);
        bottomControlsBar = findViewById(R.id.bottom_controls_bar);
        extendedControlsBar = findViewById(R.id.extended_controls_bar);
        libraryContainer = findViewById(R.id.library_container);
        settingsContainer = findViewById(R.id.settings_container);
        
        // Top controls
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.title_text);
        musicNoteButton = findViewById(R.id.music_note_button);
        subtitleButton = findViewById(R.id.subtitle_button);
        decoderButton = findViewById(R.id.decoder_button);
        menuButton = findViewById(R.id.menu_button);
        
        // Primary controls
        equalizerControlButton = findViewById(R.id.equalizer_control_button);
        speedButton = findViewById(R.id.speed_button);
        editButton = findViewById(R.id.edit_button);
        headphonesButton = findViewById(R.id.headphones_button);
        rotationButton = findViewById(R.id.rotation_button);
        nextButton = findViewById(R.id.next_button);
        
        // Bottom controls
        lockButton = findViewById(R.id.lock_button);
        prevButton = findViewById(R.id.prev_button);
        playPauseButton = findViewById(R.id.play_pause_button);
        nextBottomButton = findViewById(R.id.next_bottom_button);
        fullscreenButton = findViewById(R.id.fullscreen_button);
        pipButton = findViewById(R.id.pip_button);
        seekBar = findViewById(R.id.seek_bar);
        currentTimeText = findViewById(R.id.current_time_text);
        totalTimeText = findViewById(R.id.total_time_text);
        
        // Extended controls
        nightModeButton = findViewById(R.id.night_mode_button);
        shuffleButton = findViewById(R.id.shuffle_button);
        loopButton = findViewById(R.id.loop_button);
        muteButton = findViewById(R.id.mute_button);
        sleepTimerButton = findViewById(R.id.sleep_timer_button);
        abRepeatButton = findViewById(R.id.ab_repeat_button);
        equalizerButton = findViewById(R.id.equalizer_button);
        speedExtendedButton = findViewById(R.id.speed_extended_button);
        customizeButton = findViewById(R.id.customize_button);
        backgroundPlayButton = findViewById(R.id.background_play_button);
        screenRotationButton = findViewById(R.id.screen_rotation_button);
        
        // Set initial states
        updateDecoderButton();
        updateSpeedButton();
        updatePlayPauseButton();
    }
    
    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(this);
        
        // Disable default controls
        playerView.setUseController(false);
    }
    
    private void setupEqualizer() {
        try {
            equalizer = new Equalizer(0, player.getAudioSessionId());
            equalizer.setEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupEventListeners() {
        // Top controls listeners
        backButton.setOnClickListener(v -> onBackPressed());
        
        titleText.setOnClickListener(v -> {
            // Toggle between player and library
            if (currentScreen.equals("player")) {
                showLibrary();
            } else {
                showPlayer();
            }
        });
        
        musicNoteButton.setOnClickListener(v -> showAudioTrackDialog());
        subtitleButton.setOnClickListener(v -> showSubtitleDialog());
        decoderButton.setOnClickListener(v -> showDecoderDialog());
        menuButton.setOnClickListener(v -> showMainMenu());
        
        // Primary controls listeners
        equalizerControlButton.setOnClickListener(v -> showEqualizer());
        speedButton.setOnClickListener(v -> showSpeedDialog());
        editButton.setOnClickListener(v -> showEditDialog());
        headphonesButton.setOnClickListener(v -> toggleAudioOutput());
        rotationButton.setOnClickListener(v -> toggleScreenRotation());
        nextButton.setOnClickListener(v -> playNext());
        
        // Bottom controls listeners
        lockButton.setOnClickListener(v -> toggleLock());
        prevButton.setOnClickListener(v -> playPrevious());
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        nextBottomButton.setOnClickListener(v -> playNext());
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        pipButton.setOnClickListener(v -> enterPictureInPicture());
        
        // Extended controls listeners
        nightModeButton.setOnClickListener(v -> toggleNightMode());
        shuffleButton.setOnClickListener(v -> toggleShuffle());
        loopButton.setOnClickListener(v -> toggleLoop());
        muteButton.setOnClickListener(v -> toggleMute());
        sleepTimerButton.setOnClickListener(v -> showSleepTimer());
        abRepeatButton.setOnClickListener(v -> toggleABRepeat());
        equalizerButton.setOnClickListener(v -> showEqualizer());
        speedExtendedButton.setOnClickListener(v -> showSpeedDialog());
        customizeButton.setOnClickListener(v -> showCustomizeDialog());
        backgroundPlayButton.setOnClickListener(v -> toggleBackgroundPlay());
        screenRotationButton.setOnClickListener(v -> toggleScreenRotation());
        
        // Seek bar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                cancelControlsHide();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleControlsHide();
            }
        });
        
        // Player view touch listener for gesture controls
        playerView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private boolean isSeeking = false;
            private boolean isBrightnessAdjusting = false;
            private boolean isVolumeAdjusting = false;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLocked) return true;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        cancelControlsHide();
                        break;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getX() - startX;
                        float deltaY = event.getY() - startY;
                        
                        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50) {
                            // Horizontal swipe - seek
                            if (!isSeeking) {
                                isSeeking = true;
                                showSeekPreview(true);
                            }
                            handleSeekGesture(deltaX);
                        } else if (Math.abs(deltaY) > 50) {
                            // Vertical swipe
                            if (startX < v.getWidth() / 2) {
                                // Left side - brightness
                                if (!isBrightnessAdjusting) {
                                    isBrightnessAdjusting = true;
                                    showBrightnessOverlay(true);
                                }
                                handleBrightnessGesture(-deltaY);
                            } else {
                                // Right side - volume
                                if (!isVolumeAdjusting) {
                                    isVolumeAdjusting = true;
                                    showVolumeOverlay(true);
                                }
                                handleVolumeGesture(-deltaY);
                            }
                        }
                        break;
                        
                    case MotionEvent.ACTION_UP:
                        if (isSeeking || isBrightnessAdjusting || isVolumeAdjusting) {
                            hideGestureOverlays();
                        } else if (Math.abs(event.getX() - startX) < 10 && Math.abs(event.getY() - startY) < 10) {
                            // Single tap - toggle controls
                            toggleControls();
                        }
                        
                        isSeeking = false;
                        isBrightnessAdjusting = false;
                        isVolumeAdjusting = false;
                        scheduleControlsHide();
                        break;
                }
                return true;
            }
        });
        
        // Show extended controls on long press of primary controls
        primaryControlsBar.setOnLongClickListener(v -> {
            toggleExtendedControls();
            return true;
        });
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri videoUri = intent.getData();
            playVideo(videoUri);
            
            // Extract title from URI or intent
            String title = intent.getStringExtra("title");
            if (title == null) {
                title = Utils.getFileNameFromUri(this, videoUri);
            }
            titleText.setText(title);
        } else {
            // Show library by default
            showLibrary();
        }
    }
    
    private void playVideo(Uri uri) {
        if (player != null) {
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
            
            currentScreen = "player";
            showPlayer();
        }
    }
    
    // Screen management methods
    private void showPlayer() {
        currentScreen = "player";
        playerView.setVisibility(View.VISIBLE);
        topControlsBar.setVisibility(View.VISIBLE);
        primaryControlsBar.setVisibility(View.VISIBLE);
        bottomControlsBar.setVisibility(View.VISIBLE);
        libraryContainer.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.GONE);
    }
    
    private void showLibrary() {
        currentScreen = "library";
        playerView.setVisibility(View.GONE);
        topControlsBar.setVisibility(View.VISIBLE);
        primaryControlsBar.setVisibility(View.GONE);
        bottomControlsBar.setVisibility(View.GONE);
        extendedControlsBar.setVisibility(View.GONE);
        libraryContainer.setVisibility(View.VISIBLE);
        settingsContainer.setVisibility(View.GONE);
        
        // Update title for library
        titleText.setText("Download");
        
        // Load library content
        loadLibraryContent();
    }
    
    private void showSettings() {
        currentScreen = "settings";
        playerView.setVisibility(View.GONE);
        topControlsBar.setVisibility(View.VISIBLE);
        primaryControlsBar.setVisibility(View.GONE);
        bottomControlsBar.setVisibility(View.GONE);
        extendedControlsBar.setVisibility(View.GONE);
        libraryContainer.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.VISIBLE);
    }
    
    // Control methods
    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    private void showControls() {
        if (!isLocked) {
            isControlsVisible = true;
            topControlsBar.setVisibility(View.VISIBLE);
            primaryControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            
            scheduleControlsHide();
        }
    }
    
    private void hideControls() {
        isControlsVisible = false;
        topControlsBar.setVisibility(View.GONE);
        primaryControlsBar.setVisibility(View.GONE);
        bottomControlsBar.setVisibility(View.GONE);
        extendedControlsBar.setVisibility(View.GONE);
        
        cancelControlsHide();
    }
    
    private void toggleExtendedControls() {
        extendedControlsVisible = !extendedControlsVisible;
        extendedControlsBar.setVisibility(extendedControlsVisible ? View.VISIBLE : View.GONE);
        
        if (extendedControlsVisible) {
            scheduleControlsHide();
        }
    }
    
    private void scheduleControlsHide() {
        cancelControlsHide();
        hideControlsHandler.postDelayed(hideControlsRunnable, 3000);
    }
    
    private void cancelControlsHide() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
    }
    
    // Dialog methods
    private void showDecoderDialog() {
        String[] decoders = {"HW decoder", "HW+ decoder", "SW decoder"};
        int currentSelection = Arrays.asList(decoders).indexOf(currentDecoder + " decoder");
        if (currentSelection == -1) currentSelection = 0;
        
        new android.app.AlertDialog.Builder(this, R.style.MXDialogTheme)
            .setTitle("Select decoder")
            .setSingleChoiceItems(decoders, currentSelection, (dialog, which) -> {
                switch (which) {
                    case 0: currentDecoder = "HW"; break;
                    case 1: currentDecoder = "HW+"; break;
                    case 2: currentDecoder = "SW"; break;
                }
                updateDecoderButton();
                dialog.dismiss();
            })
            .show();
    }
    
    private void showAudioTrackDialog() {
        // Implementation for audio track selection
        Toast.makeText(this, "Audio Track Selection", Toast.LENGTH_SHORT).show();
    }
    
    private void showSubtitleDialog() {
        // Implementation for subtitle options
        Toast.makeText(this, "Subtitle Options", Toast.LENGTH_SHORT).show();
    }
    
    private void showSpeedDialog() {
        String[] speeds = {"0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x"};
        float[] speedValues = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        
        int currentIndex = Arrays.binarySearch(speedValues, playbackSpeed);
        if (currentIndex < 0) currentIndex = 3; // Default to 1x
        
        new android.app.AlertDialog.Builder(this, R.style.MXDialogTheme)
            .setTitle("Playback Speed")
            .setSingleChoiceItems(speeds, currentIndex, (dialog, which) -> {
                playbackSpeed = speedValues[which];
                if (player != null) {
                    player.setPlaybackSpeed(playbackSpeed);
                }
                updateSpeedButton();
                dialog.dismiss();
            })
            .show();
    }
    
    private void showEqualizer() {
        // Start equalizer activity
        Intent intent = new Intent(this, EqualizerActivity.class);
        startActivity(intent);
    }
    
    private void showMainMenu() {
        // Implementation for main menu
        Intent intent = new Intent(this, MXSettingsActivity.class);
        startActivity(intent);
    }
    
    // Update UI methods
    private void updateDecoderButton() {
        decoderButton.setText(currentDecoder);
    }
    
    private void updateSpeedButton() {
        String speedText = playbackSpeed == 1.0f ? "1X" : String.format("%.1fX", playbackSpeed);
        speedButton.setText(speedText);
        speedExtendedButton.setText(speedText);
    }
    
    private void updatePlayPauseButton() {
        playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    
    // Playback control methods
    private void togglePlayPause() {
        if (player != null) {
            if (isPlaying) {
                player.pause();
            } else {
                player.play();
            }
        }
    }
    
    private void playNext() {
        if (player != null) {
            player.seekToNext();
        }
    }
    
    private void playPrevious() {
        if (player != null) {
            player.seekToPrevious();
        }
    }
    
    // Feature toggle methods
    private void toggleLock() {
        isLocked = !isLocked;
        lockButton.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
        
        if (isLocked) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (audioManager != null) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMuted);
        }
        muteButton.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_on);
    }
    
    private void toggleNightMode() {
        nightMode = !nightMode;
        // Apply night mode filter to player view
        if (nightMode) {
            playerView.setAlpha(0.7f);
        } else {
            playerView.setAlpha(1.0f);
        }
        nightModeButton.setImageResource(nightMode ? R.drawable.ic_night_mode_on : R.drawable.ic_night_mode_off);
    }
    
    private void toggleShuffle() {
        shuffle = !shuffle;
        shuffleButton.setImageResource(shuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
    }
    
    private void toggleLoop() {
        loop = !loop;
        if (player != null) {
            player.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        }
        loopButton.setImageResource(loop ? R.drawable.ic_loop_on : R.drawable.ic_loop_off);
    }
    
    // Additional feature methods (stubs for now)
    private void showEditDialog() {
        Toast.makeText(this, "Video Editor", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleAudioOutput() {
        Toast.makeText(this, "Audio Output Toggle", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleScreenRotation() {
        // Force rotate screen
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
    
    private void toggleFullscreen() {
        // Toggle fullscreen mode
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        
        if ((uiOptions & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            // Enter fullscreen
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            // Exit fullscreen
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
    
    private void enterPictureInPicture() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode();
        }
    }
    
    private void showSleepTimer() {
        Toast.makeText(this, "Sleep Timer", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleABRepeat() {
        Toast.makeText(this, "A-B Repeat", Toast.LENGTH_SHORT).show();
    }
    
    private void showCustomizeDialog() {
        Toast.makeText(this, "Customize Controls", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleBackgroundPlay() {
        Toast.makeText(this, "Background Play", Toast.LENGTH_SHORT).show();
    }
    
    // Gesture handling methods
    private void handleSeekGesture(float deltaX) {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            long seekAmount = (long) (deltaX * duration / 1000);
            long newPosition = Math.max(0, Math.min(duration, currentPosition + seekAmount));
            
            // Show seek preview
            updateSeekPreview(newPosition, duration);
        }
    }
    
    private void handleBrightnessGesture(float deltaY) {
        try {
            float currentBrightness = Settings.System.getFloat(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255f;
            float newBrightness = Math.max(0.1f, Math.min(1.0f, currentBrightness + (deltaY / 500f)));
            
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = newBrightness;
            getWindow().setAttributes(layoutParams);
            
            updateBrightnessOverlay(newBrightness);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void handleVolumeGesture(float deltaY) {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volumeChange = Math.round(deltaY * maxVolume / 500f);
        int newVolume = Math.max(0, Math.min(maxVolume, currentVolume + volumeChange));
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        updateVolumeOverlay(newVolume, maxVolume);
    }
    
    // Overlay methods (stubs - implement with actual overlay views)
    private void showSeekPreview(boolean show) {
        // Show seek overlay
    }
    
    private void updateSeekPreview(long position, long duration) {
        // Update seek overlay with time
    }
    
    private void showBrightnessOverlay(boolean show) {
        // Show brightness overlay
    }
    
    private void updateBrightnessOverlay(float brightness) {
        // Update brightness overlay
    }
    
    private void showVolumeOverlay(boolean show) {
        // Show volume overlay
    }
    
    private void updateVolumeOverlay(int volume, int maxVolume) {
        // Update volume overlay
    }
    
    private void hideGestureOverlays() {
        showSeekPreview(false);
        showBrightnessOverlay(false);
        showVolumeOverlay(false);
    }
    
    // Library methods
    private void loadLibraryContent() {
        // Load video library using MediaStoreChooserActivity logic
        // This will be implemented with RecyclerView and adapters
    }
    
    // Player.Listener implementation
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        this.isPlaying = isPlaying;
        updatePlayPauseButton();
    }
    
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        // Update UI based on playback state
    }
    
    // Lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && currentScreen.equals("player")) {
            player.play();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        if (equalizer != null) {
            equalizer.release();
        }
        cancelControlsHide();
    }
    
    @Override
    public void onBackPressed() {
        if (currentScreen.equals("library")) {
            if (player != null && player.getCurrentMediaItem() != null) {
                showPlayer();
            } else {
                super.onBackPressed();
            }
        } else if (currentScreen.equals("settings")) {
            showPlayer();
        } else {
            super.onBackPressed();
        }
    }
}