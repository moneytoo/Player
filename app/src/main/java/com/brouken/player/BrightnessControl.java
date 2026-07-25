package com.brouken.player;

import android.app.Activity;
import android.content.res.Resources;
import android.provider.Settings;
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
        final float newPercent = (percent < 0 ? systemPercent() : percent) + delta;

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

    private float brightnessToPercent(final float brightness) {
        final double d = Math.sqrt(Math.max(0f, brightness));
        return (float) Math.max(0, Math.min(100, (d - 0.064) / 0.936 * 100));
    }

    /**
     * Device brightness on our percent scale, used as the starting point when the player has no brightness
     * of its own yet, so the first gesture continues from what is already on screen instead of from zero.
     */
    private float systemPercent() {
        final int max = systemBrightnessMax();
        final int raw = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
        // Out of range means the ROM stores brightness on a scale we cannot normalize; start from the middle
        if (raw < 0 || raw > max)
            return 50f;
        return brightnessToPercent((float) raw / max);
    }

    /** The scale system brightness is stored on. OEMs widen it well past the classic 0-255. */
    private int systemBrightnessMax() {
        final Resources res = Resources.getSystem();
        final int id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android");
        if (id != 0) {
            final int max = res.getInteger(id);
            if (max > 0)
                return max;
        }
        return 255;
    }
}
