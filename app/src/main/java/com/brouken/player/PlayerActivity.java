package com.brouken.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerActivity extends Activity {

    private Context mContext;
    private PlaybackStateListener playbackStateListener;

    private CustomStyledPlayerView playerView;
    public static SimpleExoPlayer player;

    private Prefs mPrefs;
    public static BrightnessControl mBrightnessControl;
    public static boolean haveMedia;

    public static final int CONTROLLER_TIMEOUT = 3500;

    private TextView titleView;

    private boolean restoreOrientationLock;
    private boolean restorePlayState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mContext = getApplicationContext();
        playerView = findViewById(R.id.video_view);

        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        playerView.setControllerHideOnTouch(true);
        playerView.setControllerAutoShow(true);

        // https://github.com/google/ExoPlayer/issues/5765
        DefaultTimeBar timeBar = (DefaultTimeBar) playerView.findViewById(R.id.exo_progress);
        timeBar.setBufferedColor(0x33FFFFFF);

        final StyledPlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
        controlView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                if (windowInsets != null) {
                    view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(),
                            windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
                    windowInsets.consumeSystemWindowInsets();
                }
                return windowInsets;
            }
        });

        /*
        final FrameLayout bottomBar = playerView.findViewById(R.id.exo_bottom_bar);
        final SubtitleView subtitleView = playerView.findViewById(R.id.exo_subtitles);
        subtitleView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                if (windowInsets != null) {
                    view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(),
                            windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom() + bottomBar.getHeight());
                    windowInsets.consumeSystemWindowInsets();
                }
                return windowInsets;
            }
        });
        */

        LinearLayout controls = playerView.findViewById(R.id.exo_basic_controls);
        ImageButton buttonOpen = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonOpen.setImageResource(R.drawable.ic_baseline_folder_open_24);
        controls.addView(buttonOpen, 0);

        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFile(mPrefs.mediaUri);
            }
        });

        buttonOpen.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(mContext,"Load subtitles", Toast.LENGTH_SHORT).show();
                loadSubtitleFile(mPrefs.mediaUri);
                return true;
            }
        });

        int padding = getResources().getDimensionPixelOffset(R.dimen.exo_time_view_padding);
        FrameLayout centerView = playerView.findViewById(R.id.exo_center_view);
        titleView = new TextView(this);
        titleView.setBackgroundResource(R.color.exo_bottom_bar_background);
        titleView.setTextColor(Color.WHITE);
        titleView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        titleView.setPadding(padding, padding, padding, padding);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setVisibility(View.GONE);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        centerView.addView(titleView);

        playbackStateListener = new PlaybackStateListener();

        mPrefs = new Prefs(this);
        if (getIntent().getData() != null) {
            mPrefs.updateMedia(getIntent().getData(), getIntent().getType());
        }

        mBrightnessControl = new BrightnessControl(this);
        mBrightnessControl.currentBrightnessLevel = mPrefs.brightness;
        mBrightnessControl.setScreenBrightness(mBrightnessControl.levelToBrightness(mBrightnessControl.currentBrightnessLevel));
    }



    @Override
    public void onStart() {
        super.onStart();

        initializePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (restoreOrientationLock) {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                restoreOrientationLock = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                final Uri uri = data.getData();
                boolean uriAlreadyTaken = false;

                // https://commonsware.com/blog/2020/06/13/count-your-saf-uri-permission-grants.html
                final ContentResolver contentResolver = getContentResolver();
                for (UriPermission persistedUri : contentResolver.getPersistedUriPermissions()) {
                    if (persistedUri.getUri().equals(uri)) {
                        uriAlreadyTaken = true;
                    } else {
                        contentResolver.releasePersistableUriPermission(persistedUri.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }

                if (!uriAlreadyTaken) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }

                mPrefs.updateMedia(uri, data.getType());
                initializePlayer();
            }
        } else if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                final Uri uri = data.getData();

                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                mPrefs.updateSubtitle(uri);
                initializePlayer();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initializePlayer() {
        haveMedia = mPrefs.mediaUri != null && Utils.fileExists(this, mPrefs.mediaUri);

        if (mPrefs.firstRun) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("To play a video, open it from any file manager or use the action in the bottom bar.\nUse vertical and horizontal gestures to change brightness, volume and seek in video.");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
            mPrefs.markFirstRun();
        }

        if (player == null) {
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
            /*trackSelector.setParameters(
                    trackSelector.buildUponParameters().setMaxVideoSizeSd());*/
            RenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
            player = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .build();
        }

        playerView.setPlayer(player);

        if (haveMedia) {
            playerView.setControllerShowTimeoutMs(CONTROLLER_TIMEOUT);

            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                    .setUri(mPrefs.mediaUri)
                    .setMimeType(mPrefs.mediaType);
            if (mPrefs.subtitleUri != null && Utils.fileExists(this, mPrefs.subtitleUri)) {
                final String subtitleMime = Utils.getSubtitleMime(mPrefs.subtitleUri);
                final String subtitleLanguage = Utils.getSubtitleLanguage(this, mPrefs.subtitleUri);

                MediaItem.Subtitle subtitle = new MediaItem.Subtitle(mPrefs.subtitleUri, subtitleMime, subtitleLanguage);
                mediaItemBuilder.setSubtitles(new ArrayList<>(Arrays.asList(subtitle)));
            }
            player.setMediaItem(mediaItemBuilder.build());

            final boolean play = mPrefs.getPosition() == 0l;
            player.setPlayWhenReady(play);
            if (play) {
                playerView.hideController();
            }

            player.seekTo(mPrefs.getPosition());

            titleView.setText(Utils.getFileName(this, mPrefs.mediaUri));
            titleView.setVisibility(View.VISIBLE);
        } else {
            playerView.setControllerShowTimeoutMs(-1);
            playerView.showController();
        }

        player.addListener(playbackStateListener);
        player.prepare();

        if (restorePlayState) {
            restorePlayState = false;
            player.play();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            mPrefs.updatePosition(player.getCurrentPosition());
            mPrefs.updateBrightness(mBrightnessControl.currentBrightnessLevel);
            //TrackSelectionArray trackSelectionArray = player.getCurrentTrackSelections();
            if (player.isPlaying()) {
                restorePlayState = true;
            }
            player.removeListener(playbackStateListener);
            player.release();
            player = null;
        }
    }

    private class PlaybackStateListener implements Player.EventListener{
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playerView.setKeepScreenOn(isPlaying);
        }
    }

    private void enableRotation() {
        try {
            if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0) {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                restoreOrientationLock = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openFile(Uri pickerInitialUri) {
        enableRotation();

        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");

        if (Build.VERSION.SDK_INT >= 26)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, 0);
    }

    private void loadSubtitleFile(Uri pickerInitialUri) {
        enableRotation();

        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        final String[] supportedMimeTypes = {
                MimeTypes.APPLICATION_SUBRIP,
                MimeTypes.TEXT_SSA,
                MimeTypes.TEXT_VTT,
                MimeTypes.APPLICATION_TTML,
                "text/*",
                "application/octet-stream"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes);

        if (Build.VERSION.SDK_INT >= 26)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, 1);
    }
}