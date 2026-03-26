package com.brouken.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.media3.ui.DefaultTimeBar;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomDefaultTimeBar extends DefaultTimeBar {

    private Rect scrubberBar;
    private Rect progressBar;
    private boolean scrubbing;
    private int scrubbingStartX;

    private long backMs = 0;
    private long forwardMs = 0;
    private long durationMs = 0;
    private Paint textPaint;
    private Paint redDotPaint; // <-- Add this

    public CustomDefaultTimeBar(Context context) {
        this(context, null);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs) {
        this(context, attrs, defStyleAttr, timebarAttrs, 0);
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet timebarAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttr, timebarAttrs, defStyleRes);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Utils.dpToPx(12)); // Adjust size if needed
        textPaint.setAntiAlias(true);

        redDotPaint = new Paint();
        redDotPaint.setColor(Color.RED);
        redDotPaint.setAntiAlias(true);

        try {
            Field sField = DefaultTimeBar.class.getDeclaredField("scrubberBar");
            sField.setAccessible(true);
            scrubberBar = (Rect) sField.get(this);

            Field pField = DefaultTimeBar.class.getDeclaredField("progressBar");
            pField.setAccessible(true);
            progressBar = (Rect) pField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Call this from PlayerActivity to update the numbers
    public void setBufferInfo(long backMs, long forwardMs, long durationMs) {
        this.backMs = backMs;
        this.forwardMs = forwardMs;
        this.durationMs = durationMs;
        invalidate(); // Force a redraw
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas); // Draw the standard ExoPlayer bar first

        if (durationMs <= 0 || progressBar == null || scrubberBar == null) return;

        // Calculate how many pixels represent 1 millisecond
        float pixelPerMs = (float) progressBar.width() / durationMs;

        // 1. Draw the Backbuffer Red Dot
        if (backMs > 0) {
            // Find the X coordinate: Scrubber center minus the width of the backbuffer
            float backDotX = scrubberBar.centerX() - (backMs * pixelPerMs);

            // Ensure the dot doesn't draw to the left of the actual progress bar
            backDotX = Math.max(progressBar.left, backDotX);

            // Draw the dot in the vertical center of the progress bar track
            int centerY = progressBar.centerY();
            float dotRadius = Utils.dpToPx(4); // Adjust this to make the dot bigger/smaller

            canvas.drawCircle(backDotX, centerY, dotRadius, redDotPaint);

            // Optional: Draw the text directly above the red dot instead of the scrubber
            int textY = progressBar.top - Utils.dpToPx(8);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("-" + (backMs / 1000) + "s", backDotX, textY, textPaint);
        }

        // 2. Draw Forward buffer text
        if (forwardMs > 0) {
            int textY = progressBar.top - Utils.dpToPx(8);
            String forwardText = "+" + (forwardMs / 1000) + "s";
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(forwardText, scrubberBar.right + Utils.dpToPx(4), textY, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && scrubberBar != null) {
            scrubbing = false;
            scrubbingStartX = (int)event.getX();
            final int distanceFromScrubber = Math.abs(scrubberBar.right - scrubbingStartX);
            if (distanceFromScrubber > Utils.dpToPx(24))
                return true;
            else
                scrubbing = true;
        }
        if (!scrubbing && event.getAction() == MotionEvent.ACTION_MOVE && scrubberBar != null) {
            final int distanceFromStart = Math.abs(((int)event.getX()) - scrubbingStartX);
            if (distanceFromStart > Utils.dpToPx(6)) {
                scrubbing = true;
                try {
                    final Method method = DefaultTimeBar.class.getDeclaredMethod("startScrubbing", long.class);
                    method.setAccessible(true);
                    method.invoke(this, (long) 0);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}