package com.brouken.player;

import android.app.Activity;
import android.view.WindowManager;

class BrightnessControl {

    private final Activity activity;

    public int currentBrightnessLevel = -1;

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

    public void changeBrightness(final CustomStyledPlayerView playerView, final boolean increase, final boolean canSetAuto) {
        int newBrightnessLevel = (increase ? currentBrightnessLevel + 1 : currentBrightnessLevel - 1);

        if (canSetAuto && newBrightnessLevel < 0)
            currentBrightnessLevel = -1;
        else if (newBrightnessLevel >= 0 && newBrightnessLevel <= 30)
            currentBrightnessLevel = newBrightnessLevel;

        if (currentBrightnessLevel == -1 && canSetAuto)
            setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
        else if (currentBrightnessLevel != -1)
            setScreenBrightness(levelToBrightness(currentBrightnessLevel));

        playerView.setHighlight(false);

        if (currentBrightnessLevel == -1 && canSetAuto) {
            playerView.setIconBrightnessAuto();
            playerView.setCustomErrorMessage("");
        } else {
            playerView.setIconBrightness();
            playerView.setCustomErrorMessage(" " + PlayerActivity.mBrightnessControl.currentBrightnessLevel);
        }
    }

    float levelToBrightness(final int level) {
        final double d = 0.064 + 0.936 / (double) 30 * (double) level;
        return (float) (d * d);
    }
}
