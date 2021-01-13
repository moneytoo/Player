package com.brouken.player;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.ui.StyledPlayerView;

public final class CustomStyledPlayerView extends StyledPlayerView implements GestureDetector.OnGestureListener {

    private final GestureDetectorCompat mDetector;

    private Orientation gestureOrientation = Orientation.UNKNOWN;
    private float gestureScrollY = 0f;
    private float gestureScrollX = 0f;
    private boolean handleTouch;

    private final float IGNORE_BORDER = Utils.dpToPx(24);
    private final float SCROLL_STEP = Utils.dpToPx(16);
    @SuppressWarnings("FieldCanBeLocal")
    private final long SEEK_STEP = 1000;
    public static final int MESSAGE_TIMEOUT_TOUCH = 400;
    public static final int MESSAGE_TIMEOUT_KEY = 800;

    private boolean restorePlayState;

    public final Runnable textClearRunnable = () -> {
        setCustomErrorMessage(null);
        clearIcon();
    };

    private final AudioManager mAudioManager;

    private final TextView exoErrorMessage;

    public CustomStyledPlayerView(Context context) {
        this(context, null);
    }

    public CustomStyledPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomStyledPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context,this);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        exoErrorMessage = findViewById(R.id.exo_error_message);
    }

    public void clearIcon() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (PlayerActivity.snackbar != null && PlayerActivity.snackbar.isShown()) {
                    PlayerActivity.snackbar.dismiss();
                    handleTouch = false;
                } else {
                    removeCallbacks(textClearRunnable);
                    handleTouch = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (handleTouch) {
                    postDelayed(textClearRunnable, MESSAGE_TIMEOUT_TOUCH);

                    // Reset timeout as it could be disabled during seek
                    if (PlayerActivity.haveMedia)
                        setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);

                    if (restorePlayState) {
                        restorePlayState = false;
                        PlayerActivity.player.play();
                    }
                    break;
                }
        }

        if (handleTouch)
            mDetector.onTouchEvent(ev);

        // Handle all events to avoid conflict with internal handlers
        return true;
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
        if (!PlayerActivity.controllerVisible) {
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
                if (!PlayerActivity.controllerVisible)
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

                if (motionEvent.getX() < (float)(getWidth() / 2)) {
                    PlayerActivity.mBrightnessControl.changeBrightness(gestureScrollY > 0);
                    exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_brightness_medium_24, 0, 0, 0);
                    setCustomErrorMessage(" " + PlayerActivity.mBrightnessControl.currentBrightnessLevel);
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
        HORIZONTAL, VERTICAL, UNKNOWN
    }

    public void setIconVolume() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_volume_up_24dp, 0, 0, 0);
    }
}