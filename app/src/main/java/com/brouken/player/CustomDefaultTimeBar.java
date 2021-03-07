package com.brouken.player;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ui.DefaultTimeBar;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class CustomDefaultTimeBar extends DefaultTimeBar {

    private final Point touchPosition;
    Rect scrubberBar;
    private boolean scrubbing;

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
        touchPosition = new Point();
        try {
            Field field = DefaultTimeBar.class.getDeclaredField("scrubberBar");
            field.setAccessible(true);
            scrubberBar = (Rect) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Point resolveRelativeTouchPosition(MotionEvent motionEvent) {
        touchPosition.set((int) motionEvent.getX(), (int) motionEvent.getY());
        return touchPosition;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && scrubberBar != null) {
            scrubbing = false;
            final Point touchPosition = resolveRelativeTouchPosition(event);
            final int x = touchPosition.x;
            final int distanceFromScrubber = Math.abs(scrubberBar.right - x);
            if (distanceFromScrubber > Utils.dpToPx(24))
                return true;
        }
        if (!scrubbing && event.getAction() == MotionEvent.ACTION_MOVE && scrubberBar != null) {
            scrubbing = true;
            try {
                final Method method = DefaultTimeBar.class.getDeclaredMethod("startScrubbing", long.class);
                method.setAccessible(true);
                method.invoke(this, (long) 0);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return super.onTouchEvent(event);
    }
}
