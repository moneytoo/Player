package com.brouken.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;

public class PlayerActivity extends Activity {

    private PlaybackStateListener playbackStateListener;
    private static final String TAG = PlayerActivity.class.getName();

    private CustomStyledPlayerView playerView;
    public static SimpleExoPlayer player;

    private Prefs mPrefs;
    public static BrightnessControl mBrightnessControl;

    public static final int CONTROLLER_TIMEOUT = 2500;


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

    private void initializePlayer() {

        if (mPrefs.mediaUri == null || !Utils.mediaExists(this, mPrefs.mediaUri)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("To play a video, open it in this Player from any file manager.");
            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
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

            playerView.setPlayer(player);
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(mPrefs.mediaUri)
                    .setMimeType(mPrefs.mediaType)
                    .build();
            player.setMediaItem(mediaItem);

            player.setPlayWhenReady(mPrefs.playbackPosition == 0l);
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
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
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
}