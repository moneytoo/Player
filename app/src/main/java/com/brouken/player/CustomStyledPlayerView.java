package com.brouken.player;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;

public final class CustomStyledPlayerView extends StyledPlayerView implements StyledPlayerControlView.VisibilityListener, GestureDetector.OnGestureListener {

    private boolean controllerVisible;

    private GestureDetectorCompat mDetector;

    private Orientation gestureOrientation = Orientation.UNKNOWN;
    private float gestureScrollY = 0f;
    private float gestureScrollX = 0f;

    private final float SCROLL_STEP = Utils.dpToPx(16);

    private final Runnable textClearRunnable = new Runnable() {
        @Override
        public void run() {
            setCustomErrorMessage(null);
        }
    };

    private AudioManager mAudioManager;


    public CustomStyledPlayerView(Context context) {
        this(context, null);
    }

    public CustomStyledPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomStyledPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setControllerVisibilityListener(this);
        mDetector = new GestureDetectorCompat(context,this);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("CUSTOM", ev.toString());

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(textClearRunnable);
                break;
            case MotionEvent.ACTION_UP:
                postDelayed(textClearRunnable, 400);

                // Reset timeout as it could be disabled during seek
                setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
                break;
        }

        mDetector.onTouchEvent(ev);

        // Handle all events to avoid conflict with internal handlers
        return true;
    }

    @Override
    public void onVisibilityChange(int visibility) {
        Log.d("CUSTOM", "onVisibilityChange " + visibility);
        controllerVisible = visibility == View.VISIBLE;

        Log.d("CUSTOM", "getControllerShowTimeoutMs " + getControllerShowTimeoutMs());

        // https://developer.android.com/training/system-ui/immersive
        if (visibility == View.VISIBLE) {
            Utils.showSystemUi(this);
        } else {
            Utils.hideSystemUi(this);
        }
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.d("CUSTOM", "onDown");

        gestureScrollY = 0;
        gestureScrollX = 0;
        gestureOrientation = Orientation.UNKNOWN;

        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        Log.d("CUSTOM", "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        Log.d("CUSTOM", "onSingleTapUp");

        if (!controllerVisible) {
            showController();
            return true;
        } else if (getControllerHideOnTouch()) {
            hideController();
            return true;
        }

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        Log.d("CUSTOM", "onScroll");

        // Exclude top status bar area
        if (motionEvent.getY() < Utils.dpToPx(24))
            return false;

        if (gestureScrollY == 0 || gestureScrollX == 0) {
            gestureScrollY = 0.0001f;
            gestureScrollX = 0.0001f;
            return false;
        }

        if (gestureOrientation == Orientation.HORIZONTAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollX += distanceX;
            if (Math.abs(gestureScrollX) > SCROLL_STEP) {

                // Make controller always visible and not hiding during seek
                if (!controllerVisible)
                    showController();
                setControllerShowTimeoutMs(0);

                gestureOrientation = Orientation.HORIZONTAL;
//                PlayerActivity.player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
                if (gestureScrollX > 0)
                    PlayerActivity.player.seekTo(PlayerActivity.player.getCurrentPosition() - 2000);
                else
                    PlayerActivity.player.seekTo(PlayerActivity.player.getCurrentPosition() + 2000);
                gestureScrollX = 0.0001f;
            }
        }

        // LEFT = Brightness  |  RIGHT = Volume
        if (gestureOrientation == Orientation.VERTICAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollY += distanceY;
            if (Math.abs(gestureScrollY) > SCROLL_STEP) {
                gestureOrientation = Orientation.VERTICAL;

                if (motionEvent.getX() < getWidth() / 2) {
                    PlayerActivity.mBrightnessControl.changeBrightness(gestureScrollY > 0);
                    setCustomErrorMessage("Brightness: " + PlayerActivity.mBrightnessControl.currentBrightnessLevel);
                } else {
                    if (gestureScrollY > 0) {
                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    } else {
                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    }
                    setCustomErrorMessage("Volume: " + PlayerActivity.player.getDeviceVolume());
                }

                gestureScrollY = 0.0001f;
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.d("CUSTOM", "onLongPress");

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private enum Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN;
    }
}