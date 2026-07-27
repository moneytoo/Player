package com.brouken.player;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
    private boolean boostAllowed = false;
    private boolean boostWarned = false;
    private boolean canSetAutoBrightness = false;
    // Volume in percent (0-200, above 100 = boost) tracked as a float so the absolute gesture keeps
    // sub-step precision between events.
    private float gestureVolume = 0f;

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

    // Hold-to-speed (YouTube-style): while the screen is long-pressed during playback, speed jumps to 2x
    // and the previous speed is restored on release.
    private static final float SPEED_BOOST = 2.f;
    private boolean speedBoostActive = false;
    private float speedBeforeBoost = 1.f;
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
    private final LevelBar levelBar;

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
        levelBar = findViewById(R.id.level_bar);

        mScaleDetector = new ScaleGestureDetector(context, this);
    }

    public void clearIcon() {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        levelBar.setVisibility(GONE);
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
                if (speedBoostActive) {
                    speedBoostActive = false;
                    if (PlayerActivity.player != null)
                        PlayerActivity.player.setPlaybackSpeed(speedBeforeBoost);
                    if (getContext() instanceof PlayerActivity)
                        ((PlayerActivity) getContext()).setSpeedBoostIndicatorVisible(false);
                }
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
        boostWarned = false;

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
            if (getContext() instanceof PlayerActivity) {
                ((PlayerActivity) getContext()).showSwipeToUnlock();
            }
            return true;
        }

        if (!PlayerActivity.controllerVisibleFully) {
            showController();
            return true;
        } else if (PlayerActivity.haveMedia) {
            // Hide on tap even while paused, so the interface can be cleared for a clean screenshot —
            // and after a failure, so the message left on screen can be read without the controls.
            hideController();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        // No player check here: brightness and volume must stay reachable even when playback has died
        // (a failed stream releases the player, and brightness has no other control at all). Seeking is
        // gated on its own branch below, where the player is actually needed.
        if (mScaleDetector.isInProgress() || PlayerActivity.locked)
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

        if (PlayerActivity.player != null
                && (gestureOrientation == Orientation.HORIZONTAL || gestureOrientation == Orientation.UNKNOWN)) {
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
            if (gestureOrientation == Orientation.UNKNOWN) {
                if (Math.abs(gestureScrollY) <= SCROLL_STEP)
                    return true;
                // Entering the boost zone requires the volume to be maxed out already, so a single swipe
                // can never run past 100% into boost by accident.
                gestureVolume = Utils.getVolumePercent(getContext(), mAudioManager);
                boostAllowed = gestureVolume >= 100 && Utils.canBoostVolume();
                canSetAutoBrightness = brightnessControl.percent <= 0;
                gestureOrientation = Orientation.VERTICAL;
                // Apply the distance accumulated up to the activation threshold as the first delta
                distanceY = gestureScrollY;
            }

            // A full swipe over the screen height covers 1.25x the range, as in VLC.
            // distanceY is positive when the finger moves up, which is the "increase" direction.
            final float delta = distanceY / getHeight() * 100f * 1.25f;

            if (motionEvent.getX() < (float)(getWidth() / 2)) {
                brightnessControl.changeBrightness(this, delta, canSetAutoBrightness);
            } else {
                gestureVolume = Math.max(0f, Math.min(boostAllowed ? 200f : 100f, gestureVolume + delta));
                // Warn once per gesture when reaching the loud zone, whether boost engages or is refused
                if (gestureVolume >= 100f && delta > 0 && !boostWarned) {
                    boostWarned = true;
                    Toast.makeText(getContext(), R.string.volume_high_warning, Toast.LENGTH_SHORT).show();
                }
                Utils.setVolumePercent(getContext(), mAudioManager, this, gestureVolume);
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (PlayerActivity.locked || mScaleDetector.isInProgress() || gestureOrientation != Orientation.UNKNOWN)
            return;
        if (!PlayerActivity.haveMedia || PlayerActivity.player == null || !PlayerActivity.player.isPlaying())
            return;
        speedBeforeBoost = PlayerActivity.player.getPlaybackParameters().speed;
        speedBoostActive = true;
        isHandledLongPress = true;
        PlayerActivity.player.setPlaybackSpeed(SPEED_BOOST);
        hideController();
        if (getContext() instanceof PlayerActivity)
            ((PlayerActivity) getContext()).setSpeedBoostIndicatorVisible(true);
    }

    // Toggles the touch lock (triggered by the lock button). While locked the controller stays hidden
    // and gestures are ignored; a tap re-shows the swipe-to-unlock bar, which unlocks when swiped.
    public void toggleLock() {
        PlayerActivity.locked = !PlayerActivity.locked;
        isHandledLongPress = true;
        if (PlayerActivity.locked && PlayerActivity.controllerVisible) {
            hideController();
        }
        // onLockChanged shows/hides the floating lock (at the button's spot) as visual feedback.
        if (getContext() instanceof PlayerActivity) {
            ((PlayerActivity) getContext()).onLockChanged();
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

    // Applies a resize mode plus an optional forced display aspect ratio (>0). A forced ratio is set
    // on the content frame directly; ratio 0 restores the video's natural AR (Media3 only recomputes
    // that on the next video-size change, so we compute it here to switch out of a forced ratio at once).
    public void applyAspectMode(int resizeMode, float forcedRatio) {
        setScale(1.f);
        setResizeMode(resizeMode);
        final AspectRatioFrameLayout frame = findViewById(R.id.exo_content_frame);
        final float ratio = forcedRatio > 0 ? forcedRatio : naturalVideoAspectRatio();
        if (frame != null && ratio > 0)
            frame.setAspectRatio(ratio);
    }

    private float naturalVideoAspectRatio() {
        if (PlayerActivity.player == null)
            return 0;
        final androidx.media3.common.Format format = PlayerActivity.player.getVideoFormat();
        if (format == null || format.width <= 0 || format.height <= 0)
            return 0;
        final float par = format.pixelWidthHeightRatio > 0 ? format.pixelWidthHeightRatio : 1f;
        return format.width * par / format.height;
    }

    private enum Orientation {
        HORIZONTAL, VERTICAL, UNKNOWN
    }

    /** Volume OSD: percent text plus the bar on the volume (right) side, scaled 0-200 to expose the boost zone. */
    public void showVolume(int percent) {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(percent > 0 ? R.drawable.ic_volume_up_24dp : R.drawable.ic_volume_off_24dp, 0, 0, 0);
        setCustomErrorMessage(" " + percent + "%");
        showLevelBar(percent, 200f, Gravity.CENTER_VERTICAL | Gravity.END);
    }

    /** Brightness OSD: percent text plus the bar on the brightness (left) side; auto mode has no value. */
    public void showBrightness(int percent, boolean auto) {
        exoErrorMessage.setCompoundDrawablesWithIntrinsicBounds(auto ? R.drawable.ic_brightness_auto_24dp : R.drawable.ic_brightness_medium_24, 0, 0, 0);
        setCustomErrorMessage(auto ? "" : " " + percent + "%");
        if (auto) {
            levelBar.setVisibility(GONE);
        } else {
            showLevelBar(percent, 100f, Gravity.CENTER_VERTICAL | Gravity.START);
        }
    }

    private void showLevelBar(int value, float max, int gravity) {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) levelBar.getLayoutParams();
        if (lp.gravity != gravity) {
            lp.gravity = gravity;
            levelBar.setLayoutParams(lp);
        }
        levelBar.setValue(value, max);
        levelBar.setVisibility(VISIBLE);
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