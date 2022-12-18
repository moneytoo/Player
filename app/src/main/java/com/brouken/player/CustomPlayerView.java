package com.brouken.player;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.view.GestureDetectorCompat;
import androidx.media3.common.C;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.Collections;

public class CustomPlayerView extends PlayerView implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private final GestureDetectorCompat mDetector;

    private Orientation gestureOrientation = Orientation.UNKNOWN;
    private float gestureScrollY = 0f;
    private float gestureScrollX = 0f;
    private boolean handleTouch;
    private long seekStart;
    private long seekChange;
    private long seekMax;
    private long seekLastPosition;
    public boolean seekProgress;
    private boolean canBoostVolume = false;
    private boolean canSetAutoBrightness = false;

    private final float IGNORE_BORDER = Utils.dpToPx(24);
    private final float SCROLL_STEP = Utils.dpToPx(16);
    private final float SCROLL_STEP_SEEK = Utils.dpToPx(8);
    @SuppressWarnings("FieldCanBeLocal")
    private final long SEEK_STEP = 1000;
    public static final int MESSAGE_TIMEOUT_TOUCH = 400;
    public static final int MESSAGE_TIMEOUT_KEY = 800;
    public static final int MESSAGE_TIMEOUT_LONG = 1400;

    private boolean restorePlayState;
    private boolean canScale = true;
    private boolean isHandledLongPress = false;
    public long keySeekStart = -1;
    public int volumeUpsInRow = 0;

    private final ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private float mScaleFactorFit;
    Rect systemGestureExclusionRect = new Rect();

    public final Runnable textClearRunnable = () -> {
        setCustomErrorMessage(null);
        clearIcon();
        keySeekStart = -1;
    };

    private final AudioManager mAudioManager;
    private BrightnessControl brightnessControl;

    private final TextView exoErrorMessage;
    private final View exoProgress;

    public CustomPlayerView(Context context) {
        this(context, null);
    }

    public CustomPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context,this);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        exoErrorMessage = findViewById(R.id.exo_error_message);
        exoProgress = findViewById(R.id.exo_progress);

        mScaleDetector = new ScaleGestureDetector(context, this);

        if (!Utils.isTvBox(getContext())) {
            exoErrorMessage.setOnClickListener(v -> {
                if (PlayerActivity.locked) {
                    PlayerActivity.locked = false;
                    Utils.showText(CustomPlayerView.this, "", MESSAGE_TIMEOUT_LONG);
                    setIconLock(false);
                }
            });
        }
    }

    public void clearIcon() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        setHighlight(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (PlayerActivity.restoreControllerTimeout) {
            setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
            PlayerActivity.restoreControllerTimeout = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gestureOrientation == Orientation.UNKNOWN)
            mScaleDetector.onTouchEvent(ev);

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
            case MotionEvent.ACTION_CANCEL:
                if (handleTouch) {
                    if (gestureOrientation == Orientation.HORIZONTAL) {
                        setCustomErrorMessage(null);
                    } else {
                        postDelayed(textClearRunnable, isHandledLongPress ? MESSAGE_TIMEOUT_LONG : MESSAGE_TIMEOUT_TOUCH);
                    }

                    if (restorePlayState) {
                        restorePlayState = false;
                        if (PlayerActivity.player != null) {
                            PlayerActivity.player.play();
                        }
                    }

                    setControllerAutoShow(true);

                    if (seekProgress) {
                        seekProgress = false;
                        hideControllerImmediately();
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
        isHandledLongPress = false;

        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }



    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    public boolean tap() {
        if (PlayerActivity.locked) {
            Utils.showText(this, "", MESSAGE_TIMEOUT_LONG);
            setIconLock(true);
            return true;
        }

        if (!PlayerActivity.controllerVisibleFully) {
            showController();
            return true;
        } else if (PlayerActivity.haveMedia && PlayerActivity.player != null && PlayerActivity.player.isPlaying()) {
            hideController();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        if (mScaleDetector.isInProgress() || PlayerActivity.player == null || PlayerActivity.locked)
            return false;

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
            if (Math.abs(gestureScrollX) > SCROLL_STEP || (gestureOrientation == Orientation.HORIZONTAL && Math.abs(gestureScrollX) > SCROLL_STEP_SEEK)) {
                // Do not show controller if not already visible
                setControllerAutoShow(false);

                if (gestureOrientation == Orientation.UNKNOWN) {
                    if (PlayerActivity.player.isPlaying()) {
                        restorePlayState = true;
                        PlayerActivity.player.pause();
                    }
                    clearIcon();
                    seekLastPosition = seekStart = PlayerActivity.player.getCurrentPosition();
                    seekChange = 0L;
                    seekMax = PlayerActivity.player.getDuration();

                    if (!isControllerFullyVisible()) {
                        seekProgress = true;
                        showProgress();
                    }
                }

                gestureOrientation = Orientation.HORIZONTAL;
                long position = 0;
                float distanceDiff = Math.max(0.5f, Math.min(Math.abs(Utils.pxToDp(distanceX) / 4), 10.f));

                if (PlayerActivity.haveMedia) {
                    if (gestureScrollX > 0) {
                        if (seekStart + seekChange - SEEK_STEP  * distanceDiff >= 0) {
                            PlayerActivity.player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                            seekChange -= SEEK_STEP * distanceDiff;
                            position = seekStart + seekChange;
                            PlayerActivity.player.seekTo(position);
                        }
                    } else {
                        PlayerActivity.player.setSeekParameters(SeekParameters.NEXT_SYNC);
                        if (seekMax == C.TIME_UNSET) {
                            seekChange += SEEK_STEP * distanceDiff;
                            position = seekStart + seekChange;
                            PlayerActivity.player.seekTo(position);
                        } else if (seekStart + seekChange + SEEK_STEP < seekMax) {
                            seekChange += SEEK_STEP  * distanceDiff;
                            position = seekStart + seekChange;
                            PlayerActivity.player.seekTo(position);
                        }
                    }
                    for (long start : PlayerActivity.chapterStarts) {
                        if ((seekLastPosition < start && position >= start) || (seekLastPosition > start && position <= start)) {
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        }
                    }
                    seekLastPosition = position;
                    String message = Utils.formatMilisSign(seekChange);
                    if (!isControllerFullyVisible()) {
                        message += "\n" + Utils.formatMilis(position);
                    }
                    setCustomErrorMessage(message);
                    gestureScrollX = 0.0001f;
                }
            }
        }

        // LEFT = Brightness  |  RIGHT = Volume
        if (gestureOrientation == Orientation.VERTICAL || gestureOrientation == Orientation.UNKNOWN) {
            gestureScrollY += distanceY;
            if (Math.abs(gestureScrollY) > SCROLL_STEP) {
                if (gestureOrientation == Orientation.UNKNOWN) {
                    canBoostVolume = Utils.isVolumeMax(mAudioManager);
                    canSetAutoBrightness = brightnessControl.currentBrightnessLevel <= 0;
                }
                gestureOrientation = Orientation.VERTICAL;

                if (motionEvent.getX() < (float)(getWidth() / 2)) {
                    brightnessControl.changeBrightness(this, gestureScrollY > 0, canSetAutoBrightness);
                } else {
                    Utils.adjustVolume(getContext(), mAudioManager, this, gestureScrollY > 0, canBoostVolume, false);
                }

                gestureScrollY = 0.0001f;
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (PlayerActivity.locked || (getPlayer() != null && getPlayer().isPlaying())) {
            PlayerActivity.locked = !PlayerActivity.locked;
            isHandledLongPress = true;
            Utils.showText(this, "", MESSAGE_TIMEOUT_LONG);
            setIconLock(PlayerActivity.locked);

            if (PlayerActivity.locked && PlayerActivity.controllerVisible) {
                hideController();
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        if (PlayerActivity.locked)
            return false;

        if (canScale) {
            final float factor = scaleGestureDetector.getScaleFactor();
            mScaleFactor *= factor + (1 - factor) / 3 * 2;
            mScaleFactor = Utils.normalizeScaleFactor(mScaleFactor, mScaleFactorFit);
            setScale(mScaleFactor);
            restoreSurfaceView();
            clearIcon();
            setCustomErrorMessage((int)(mScaleFactor * 100) + "%");
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        if (PlayerActivity.locked)
            return false;

        mScaleFactor = getVideoSurfaceView().getScaleX();
        if (getResizeMode() != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            canScale = false;
            setAspectRatioListener((targetAspectRatio, naturalAspectRatio, aspectRatioMismatch) -> {
                setAspectRatioListener(null);
                mScaleFactor = mScaleFactorFit = getScaleFit();
                canScale = true;
            });
            getVideoSurfaceView().setAlpha(0);
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        } else {
            mScaleFactorFit = getScaleFit();
            canScale = true;
        }
        ImageButton buttonAspectRatio = findViewById(Integer.MAX_VALUE - 100);
        buttonAspectRatio.setImageResource(R.drawable.ic_fit_screen_24dp);
        hideController();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        if (PlayerActivity.locked)
            return;
        if (mScaleFactor - mScaleFactorFit < 0.001) {
            setScale(1.f);
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

            ImageButton buttonAspectRatio = findViewById(Integer.MAX_VALUE - 100);
            buttonAspectRatio.setImageResource(R.drawable.ic_aspect_ratio_24dp);
        }
        if (PlayerActivity.player != null && !PlayerActivity.player.isPlaying()) {
            showController();
        }
        restoreSurfaceView();
    }

    private void restoreSurfaceView() {
        if (getVideoSurfaceView().getAlpha() != 1) {
            getVideoSurfaceView().setAlpha(1);
        }
    }

    public float getScaleFit() {
        return Math.min((float)getHeight() / (float)getVideoSurfaceView().getHeight(),
                (float)getWidth() / (float)getVideoSurfaceView().getWidth());
    }

    private enum Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN
    }

    public void setIconVolume(boolean volumeActive) {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(volumeActive ? R.drawable.ic_volume_up_24dp : R.drawable.ic_volume_off_24dp, 0, 0, 0);
    }

    public void setHighlight(boolean active) {
        if (active)
            exoErrorMessage.getBackground().setTint(Color.RED);
        else
            exoErrorMessage.getBackground().setTintList(null);
    }

    public void setIconBrightness() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_brightness_medium_24, 0, 0, 0);
    }

    public void setIconBrightnessAuto() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_brightness_auto_24dp, 0, 0, 0);
    }

    public void setIconLock(boolean locked) {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(locked ? R.drawable.ic_lock_24dp : R.drawable.ic_lock_open_24dp, 0, 0, 0);
    }

    public void setScale(final float scale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final View videoSurfaceView = getVideoSurfaceView();
            try {
                videoSurfaceView.setScaleX(scale);
                videoSurfaceView.setScaleY(scale);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            //videoSurfaceView.animate().setStartDelay(0).setDuration(0).scaleX(scale).scaleY(scale).start();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (Build.VERSION.SDK_INT >= 29) {
            exoProgress.getGlobalVisibleRect(systemGestureExclusionRect);
            systemGestureExclusionRect.left = left;
            systemGestureExclusionRect.right = right;
            setSystemGestureExclusionRects(Collections.singletonList(systemGestureExclusionRect));
        }
    }

    public void setBrightnessControl(BrightnessControl brightnessControl) {
        this.brightnessControl = brightnessControl;
    }
}