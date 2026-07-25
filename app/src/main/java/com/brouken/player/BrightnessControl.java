package com.brouken.player;

import android.app.Activity;
import android.view.WindowManager;

class BrightnessControl {

    private final Activity activity;

    /** 0-100, or -1 for system/auto brightness. Float so the absolute gesture keeps sub-percent precision. */
    public float percent = -1;

    public BrightnessControl(Activity activity) {
        this.activity = activity;
    }

    public float getScreenBrightness() {
        return activity.getWindow().getAttributes().screenBrightness;
    }

    public void setScreenBrightness(final float brightness) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = brightness;
        activity.getWindow().setAttributes(lp);
    }

    public void changeBrightness(final CustomPlayerView playerView, final float delta, final boolean canSetAuto) {
        final float newPercent = (percent < 0 ? 0 : percent) + delta;

        if (canSetAuto && newPercent < 0)
            percent = -1;
        else
            percent = Math.max(0f, Math.min(100f, newPercent));

        if (percent < 0)
            setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
        else
            setScreenBrightness(percentToBrightness(percent));

        playerView.showBrightness(Math.round(percent), percent < 0);
    }

    float percentToBrightness(final float percent) {
        final double d = 0.064 + 0.936 / 100 * percent;
        return (float) (d * d);
    }
}
