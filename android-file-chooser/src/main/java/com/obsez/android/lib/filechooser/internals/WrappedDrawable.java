package com.obsez.android.lib.filechooser.internals;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class WrappedDrawable extends Drawable {

    private final Drawable _drawable;

    protected Drawable getDrawable() {
        return _drawable;
    }

    public WrappedDrawable(Drawable drawable) {
        super();
        _drawable = drawable;
    }

    public WrappedDrawable(Drawable drawable, float widthInDp, float heightInDp) {
        super();
        _drawable = drawable;
        setBounds(0, 0, (int) UiUtil.dip2px(widthInDp), (int) UiUtil.dip2px(heightInDp));
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        //update bounds to get correctly
        super.setBounds(left, top, right, bottom);
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds(left, top, right, bottom);
        }
    }

    public void setBoundsInDp(float left, float top, float right, float bottom) {
        //update bounds to get correctly
        super.setBounds((int) UiUtil.dip2px(left),
            (int) UiUtil.dip2px(top),
            (int) UiUtil.dip2px(right),
            (int) UiUtil.dip2px(bottom));
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds((int) UiUtil.dip2px(left),
                (int) UiUtil.dip2px(top),
                (int) UiUtil.dip2px(right),
                (int) UiUtil.dip2px(bottom));
        }
    }

    @Override
    public void setAlpha(int alpha) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        Drawable drawable = getDrawable();
        return drawable != null
            ? drawable.getOpacity()
            : PixelFormat.UNKNOWN;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        Drawable drawable = getDrawable();
        return drawable != null
            ? drawable.getBounds().width()
            : 0;
    }

    @Override
    public int getIntrinsicHeight() {
        Drawable drawable = getDrawable();
        return drawable != null ?
            drawable.getBounds().height()
            : 0;
    }
}
