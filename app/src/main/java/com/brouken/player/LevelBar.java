package com.brouken.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * Vertical level indicator for the volume/brightness OSD, filling from the bottom up. Everything above
 * 100 is drawn in the boost color, so the volume boost zone reads as a distinct segment of the same bar.
 */
public class LevelBar extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int trackColor;
    private final int fillColor;
    private final int boostColor;

    private float value = 0f;
    private float max = 100f;

    public LevelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        trackColor = ContextCompat.getColor(context, R.color.level_bar_track);
        fillColor = ContextCompat.getColor(context, R.color.white);
        boostColor = ContextCompat.getColor(context, R.color.volume_boost);
    }

    public void setValue(final float value, final float max) {
        this.value = value;
        this.max = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float width = getWidth();
        final float height = getHeight();
        final float radius = width / 2;

        paint.setColor(trackColor);
        canvas.drawRoundRect(0, 0, width, height, radius, radius, paint);

        // The fill is always one rounded shape from the bottom up to the value; the boost part is the same
        // shape redrawn clipped to the band above 100, so the bar reads as a single unbroken bar.
        final float top = height * (1 - value / max);
        paint.setColor(fillColor);
        canvas.drawRoundRect(0, top, width, height, radius, radius, paint);

        if (value > 100f) {
            canvas.save();
            canvas.clipRect(0, top, width, height * (1 - 100f / max));
            paint.setColor(boostColor);
            canvas.drawRoundRect(0, top, width, height, radius, radius, paint);
            canvas.restore();
        }
    }
}
