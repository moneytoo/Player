package com.brouken.player;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;

public final class CustomStyledPlayerView extends StyledPlayerView implements StyledPlayerControlView.VisibilityListener, GestureDetector.OnGestureListener {

    private boolean controllerVisible;

    private GestureDetectorCompat mDetector;

    private Orientation gestureOrientation = Orientation.UNKNOWN;
    private float gestureScrollY = 0f;
    private float gestureScrollX = 0f;

    private final float IGNORE_BORDER = Utils.dpToPx(24);
    private final float SCROLL_STEP = Utils.dpToPx(16);
    private final long SEEK_STEP = 1000;

    private boolean restorePlayState;

    public final Runnable textClearRunnable = new Runnable() {
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
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(textClearRunnable);
                break;
            case MotionEvent.ACTION_UP:
                postDelayed(textClearRunnable, 400);

                // Reset timeout as it could be disabled during seek
                if (PlayerActivity.haveMedia)
                    setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);

                if (restorePlayState) {
                    restorePlayState = false;
                    PlayerActivity.player.play();
                }
                break;
        }

        mDetector.onTouchEvent(ev);

        // Handle all events to avoid conflict with internal handlers
        return true;
    }

    @Override
    public void onVisibilityChange(int visibility) {
        controllerVisible = visibility == View.VISIBLE;

        // https://developer.android.com/training/system-ui/immersive
        if (visibility == View.VISIBLE) {
            Utils.showSystemUi(this);
        } else {
            Utils.hideSystemUi(this);
        }
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        gestureScrollY = 0;
        gestureScrollX = 0;
        gestureOrientation = Orientation.UNKNOWN;

        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (!controllerVisible) {
            showController();
            return true;
        } else if (getControllerHideOnTouch()) {
            if (PlayerActivity.haveMedia)
                hideController();
            return true;
        }

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        // Exclude edge areas
        if (motionEvent.getY() < IGNORE_BORDER || motionEvent.getX() < IGNORE_BORDER ||
                motionEvent.getY() > getHeight() - IGNORE_BORDER || motionEvent.getX() > getWidth() - IGNORE_BORDER)
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

                if (gestureOrientation == Orientation.UNKNOWN) {
                    if (PlayerActivity.player.isPlaying()) {
                        restorePlayState = true;
                        PlayerActivity.player.pause();
                    }
                }

                gestureOrientation = Orientation.HORIZONTAL;
                final long position;

                if (PlayerActivity.haveMedia) {
                    if (gestureScrollX > 0) {
                        PlayerActivity.player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                        if (PlayerActivity.player.getCurrentPosition() - SEEK_STEP < 0)
                            position = 0;
                        else
                            position = PlayerActivity.player.getCurrentPosition() - SEEK_STEP;
                        PlayerActivity.player.seekTo(position);
                    } else {
                        PlayerActivity.player.setSeekParameters(SeekParameters.NEXT_SYNC);
                        PlayerActivity.player.seekTo(PlayerActivity.player.getCurrentPosition() + SEEK_STEP);
                    }
                    gestureScrollX = 0.0001f;
                }
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
                    Utils.adjustVolume(mAudioManager, this, gestureScrollY > 0);
                }

                gestureScrollY = 0.0001f;
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private enum Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN;
    }
}