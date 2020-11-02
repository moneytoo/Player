package com.brouken.player;

import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;

class BrightnessControl {

    public static Activity activity;

    public BrightnessControl(Activity activity) {
        this.activity = activity;
    }

    public float getScreenBrightness() {
        Log.d("BRIGHTNESS", "b=" + activity.getWindow().getAttributes().screenBrightness);
        return activity.getWindow().getAttributes().screenBrightness;
    }

    public void setScreenBrightness(final float brightness) {
        Log.d("BRIGHTNESS", "setScreenBrightness " + brightness);
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = brightness;
        activity.getWindow().setAttributes(lp);
    }

    public void changeBrightness(final boolean increase) {
        Log.d("BRIGHTNESS", "changeBrightness " + increase);
        final float step = 1f / 30f;

        float brightness = getScreenBrightness();
        if ((brightness <= 0f && !increase) || (brightness >= 1f && increase))
            return;
        if (increase) {
            if (brightness + step > 1f)
                setScreenBrightness(1f);
            else
                setScreenBrightness(brightness + step);
        } else {
            if (brightness - step < 0f)
                setScreenBrightness(0f);
            else
                setScreenBrightness(brightness - step);
        }
    }
}
