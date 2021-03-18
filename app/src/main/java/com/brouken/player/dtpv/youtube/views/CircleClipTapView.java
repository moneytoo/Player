package com.brouken.player.dtpv.youtube.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.brouken.player.R;

/**
 * View class
 *
 * Draws a arc shape and provides a circle scaling animation.
 * Used by [YouTubeOverlay][com.github.vkay94.dtpv.youtube.YouTubeOverlay].
 */
public final class CircleClipTapView extends View {

    private Paint backgroundPaint;
    private Paint circlePaint;

    private int widthPx;
    private int heightPx;

    // Background

    private Path shapePath;
    private boolean isLeft;

    // Circle

    private float cX;
    private float cY;

    private float currentRadius;
    private int minRadius;
    private int maxRadius;

    // Animation

    private ValueAnimator valueAnimator;
    private boolean forceReset;

    private float arcSize;

    Runnable performAtEnd;

    public CircleClipTapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        backgroundPaint = new Paint();
        circlePaint = new Paint();

        widthPx = 0;
        heightPx = 0;

        // Background

        shapePath = new Path();
        isLeft = true;

        cX = 0f;
        cY = 0f;

        currentRadius = 0f;
        minRadius = 0;
        maxRadius = 0;

        valueAnimator = null;
        forceReset = false;

        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.dtpv_yt_background_circle_color));

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(ContextCompat.getColor(context, R.color.dtpv_yt_tap_circle_color));

        // Pre-configuations depending on device display metrics
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        widthPx = dm.widthPixels;
        heightPx = dm.heightPixels;

        minRadius = (int)(30f * dm.density);
        maxRadius = (int)(400f * dm.density);

        updatePathShape();

        valueAnimator = getCircleAnimator();

        arcSize = 80f;

        performAtEnd = new Runnable() {
            @Override
            public void run() {

            }
        };
    }

    /*
        Getter and setter
     */

    public final Runnable getPerformAtEnd() {
        return performAtEnd;
    }

    public final void setPerformAtEnd(Runnable value) {
        performAtEnd = value;
    }

    public final float getArcSize() {
        return arcSize;
    }

    public final void setArcSize(float value) {
        arcSize = value;
        updatePathShape();
    }

    public final int getCircleBackgroundColor() {
        return backgroundPaint.getColor();
    }

    public final void setCircleBackgroundColor(int value) {
        backgroundPaint.setColor(value);
    }

    public final int getCircleColor() {
        return circlePaint.getColor();
    }

    public final void setCircleColor(int value) {
        circlePaint.setColor(value);
    }

    public final long getAnimationDuration() {
        return valueAnimator != null ? valueAnimator.getDuration() : 650L;
    }

    public final void setAnimationDuration(long value) {
        getCircleAnimator().setDuration(value);
    }

    /*
       Methods
    */

    /*
        Circle
     */

    public final void updatePosition(float x, float y) {
        cX = x;
        cY = y;

        boolean newIsLeft = x <= (float)(getResources().getDisplayMetrics().widthPixels / 2);
        if (isLeft != newIsLeft) {
            isLeft = newIsLeft;
            updatePathShape();
        }
    }

    private final void invalidateWithCurrentRadius(float factor) {
        currentRadius = (float)minRadius + (float)(maxRadius - minRadius) * factor;
        invalidate();
    }

    /*
        Background
     */

    private final void updatePathShape() {
        float halfWidth = (float)widthPx * 0.5f;

        shapePath.reset();

        float w = isLeft ? 0.0f : (float)widthPx;
        int f = isLeft ? 1 : -1;

        shapePath.moveTo(w, 0.0F);
        shapePath.lineTo((float)f * (halfWidth - arcSize) + w, 0.0F);
        shapePath.quadTo(
                (float)f * (halfWidth + arcSize) + w,
                (float)heightPx / (float)2,
                (float)f * (halfWidth - arcSize) + w,
                (float)heightPx
        );
        shapePath.lineTo(w, (float)heightPx);
        shapePath.close();
        invalidate();
    }

    /*
        Animation
     */

    private final ValueAnimator getCircleAnimator() {
        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofFloat(new float[]{0.0F, 1.0F});
            valueAnimator.setDuration(getAnimationDuration());

            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    invalidateWithCurrentRadius((float)animation.getAnimatedValue());
                }
            });

            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!forceReset)
                        performAtEnd.run();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }

        return valueAnimator;
    }

    public final void resetAnimation(Runnable body) {
        forceReset = true;
        getCircleAnimator().end();
        body.run();
        forceReset = false;
        getCircleAnimator().start();
    }

    public final void endAnimation() {
        getCircleAnimator().end();
    }

    /*
        Others: Drawing and Measurements
     */

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        widthPx = w;
        heightPx = h;
        updatePathShape();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        if (canvas != null) {
            canvas.clipPath(this.shapePath);
        }
        if (canvas != null) {
            canvas.drawPath(this.shapePath, this.backgroundPaint);
        }

        // Circle
        if (canvas != null) {
            canvas.drawCircle(this.cX, this.cY, this.currentRadius, this.circlePaint);
        }
    }
}
