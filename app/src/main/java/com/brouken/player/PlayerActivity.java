package com.brouken.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;

public class PlayerActivity extends Activity {

    private PlaybackStateListener playbackStateListener;
    private static final String TAG = PlayerActivity.class.getName();

    private CustomStyledPlayerView playerView;
    public static SimpleExoPlayer player;

    private Prefs mPrefs;
    public static BrightnessControl mBrightnessControl;

    public static final int CONTROLLER_TIMEOUT = 3500;

    private boolean dialogAction = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.video_view);

        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        playerView.setControllerHideOnTouch(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerShowTimeoutMs(CONTROLLER_TIMEOUT);

        // https://github.com/google/ExoPlayer/issues/5765
        DefaultTimeBar timeBar = (DefaultTimeBar) playerView.findViewById(R.id.exo_progress);
        timeBar.setBufferedColor(0x33FFFFFF);

        StyledPlayerControlView controlView = playerView.findViewById(R.id.exo_controller);

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

        playbackStateListener = new PlaybackStateListener();

        mPrefs = new Prefs(this);
        if (getIntent().getData() != null) {
            mPrefs.updateMedia(getIntent().getData(), getIntent().getType());

//            getContentResolver().takePersistableUriPermission(getIntent().getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
//
//            for (UriPermission perm : getContentResolver().getPersistedUriPermissions()) {
//                Log.d(TAG, perm.toString());
//                if (perm.getUri().equals(getIntent().getData())) {
//                    Log.d(TAG, "OKKK");
//                }
//            }
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
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == RESULT_OK) {
            mPrefs.updateMedia(data.getData(),data.getType());
            initializePlayer();
        }
    }

    private void initializePlayer() {
        if (mPrefs.mediaUri == null || !Utils.mediaExists(this, mPrefs.mediaUri)) {
            dialogAction = false;
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("To play a video, open it from any file manager or use the action bellow.");
            builder.setNeutralButton("Open video", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogAction = true;
                    openFile(mPrefs.mediaUri);
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (!dialogAction)
                        finish();
                }
            });
        } else {
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

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            playerView.setPlayer(player);
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(mPrefs.mediaUri)
                    .setMimeType(mPrefs.mediaType)
                    .build();
            player.setMediaItem(mediaItem);

            final boolean play = mPrefs.playbackPosition == 0l;
            player.setPlayWhenReady(play);
            if (play) {
                playerView.hideController();
            }

            player.seekTo(mPrefs.currentWindow, mPrefs.playbackPosition);
            player.addListener(playbackStateListener);
            player.prepare();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            mPrefs.updatePosition(player.getCurrentWindowIndex(), player.getCurrentPosition());
            mPrefs.updateBrightness(mBrightnessControl.currentBrightnessLevel);
            player.removeListener(playbackStateListener);
            player.release();
            player = null;
        }
    }

    private class PlaybackStateListener implements Player.EventListener{

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case Player.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case Player.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    mPrefs.updateMedia(null, null);
                    // buggy alertdialog
                    // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    releasePlayer();
                    initializePlayer();
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playerView.setKeepScreenOn(isPlaying);
        }
    }

    private void openFile(Uri pickerInitialUri) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");

        if (Build.VERSION.SDK_INT >= 26)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, 0);
    }
}