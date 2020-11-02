package com.brouken.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerView;

public class PlayerActivity extends Activity implements GestureDetector.OnGestureListener {

    private PlaybackStateListener playbackStateListener;
    private static final String TAG = PlayerActivity.class.getName();

    private StyledPlayerView playerView;
    private SimpleExoPlayer player;

    private Prefs mPrefs;
    private BrightnessControl mBrightnessControl;

    private GestureDetectorCompat mDetector;

    private Orientation gestureOrientation = Orientation.UNKNOWN;
    private float gestureScrollY = 0f;
    private float gestureScrollX = 0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.video_view);

        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        playerView.setControllerHideOnTouch(false);
        playerView.setControllerAutoShow(true);
        playerView.setControllerShowTimeoutMs(3000);

        playbackStateListener = new PlaybackStateListener();

        mPrefs = new Prefs(this);
        if (getIntent().getData() != null) {
            mPrefs.updateMedia(getIntent().getData(), getIntent().getType());
        }

        mBrightnessControl = new BrightnessControl(this);

        /*set(this);
        Brightness(Orientation.VERTICAL);
        Volume(Orientation.CIRCULAR);*/
        mBrightnessControl.setScreenBrightness(mPrefs.brightness);

        mDetector = new GestureDetectorCompat(this,this);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d(TAG, event.toString());

        if (event.getAction() == MotionEvent.ACTION_UP) {
            gestureScrollY = 0;
            gestureScrollX = 0;
            gestureOrientation = Orientation.UNKNOWN;
            playerView.setCustomErrorMessage(null);
        }

        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.d(TAG, "onDown");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        Log.d(TAG, "onShowPress");
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.d(TAG, "onFling");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.d(TAG, "onLongPress");
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        //Log.d(TAG, "onScroll " + distanceX + " " + distanceY + " " + System.currentTimeMillis());
        if (gestureScrollY == 0 || gestureScrollX == 0) {
            gestureScrollY = 0.0001f;
            gestureScrollX = 0.0001f;
            return false;
        }

        if (gestureOrientation == Orientation.HORIZONTAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollX += distanceX;
            if (Math.abs(gestureScrollX) > 40f) {
                gestureOrientation = Orientation.HORIZONTAL;
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
                if (gestureScrollX > 0)
                    player.seekTo(player.getCurrentPosition() - 3000);
                else
                    player.seekTo(player.getCurrentPosition() + 3000);
                gestureScrollX = 0.0001f;
            }
        }
        if (gestureOrientation == Orientation.VERTICAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollY += distanceY;
            if (Math.abs(gestureScrollY) > 40f) {
                gestureOrientation = Orientation.VERTICAL;
                mBrightnessControl.changeBrightness(gestureScrollY > 0);
                gestureScrollY = 0.0001f;
                playerView.setCustomErrorMessage("Brightness: " + (int) (mBrightnessControl.getScreenBrightness() * 100) + "%");
            }
        }

        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        initializePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void initializePlayer() {
        //Log.d(TAG, getIntent().getData().toString());

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

    private void releasePlayer() {
        if (player != null) {
            mPrefs.updatePosition(player.getCurrentWindowIndex(), player.getCurrentPosition());
            mPrefs.updateBrightness(mBrightnessControl.getScreenBrightness());
            player.removeListener(playbackStateListener);
            player.release();
            player = null;
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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
    }

    private enum Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN;
    }
}