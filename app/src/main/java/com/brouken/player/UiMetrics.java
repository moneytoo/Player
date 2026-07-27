package com.brouken.player;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

/**
 * Single source of truth for adaptive UI sizing across phone / tablet / TV.
 *
 * The player UI is built programmatically, so instead of resource-qualifier dimens (which would key on
 * uiMode and drift from this app's broader {@code Utils.isTvBox()} device switch) every size flows through
 * one device-classed token here. Device class is TV first (isTvBox), else by {@code smallestScreenWidthDp}
 * (orientation-stable). The PHONE column of every token equals the historical literal, so phones render
 * byte-for-byte as before (scale 1.0); only tablet/TV scale up. dp uses the activity's density (respects
 * foldable inner/outer panels, DeX, external displays), unlike {@link Utils#dpToPx} which uses system metrics.
 */
final class UiMetrics {

    enum DeviceClass { PHONE, TABLET_MEDIUM, TABLET_LARGE, TV }

    final DeviceClass deviceClass;
    private final float density;
    private final float scale;              // geometry (dp) multiplier
    private final int screenWidthDp;
    private final int orientation;

    private UiMetrics(Context ctx, boolean isTvBox) {
        final Configuration cfg = ctx.getResources().getConfiguration();
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        density = dm.density;
        screenWidthDp = cfg.screenWidthDp;
        orientation = cfg.orientation;
        if (isTvBox) {
            deviceClass = DeviceClass.TV;
            scale = 1.30f;
        } else if (cfg.smallestScreenWidthDp >= 720) {
            deviceClass = DeviceClass.TABLET_LARGE;
            scale = 1.25f;
        } else if (cfg.smallestScreenWidthDp >= 600) {
            deviceClass = DeviceClass.TABLET_MEDIUM;
            scale = 1.15f;
        } else {
            deviceClass = DeviceClass.PHONE;
            scale = 1.0f;
        }
    }

    static UiMetrics of(Context ctx, boolean isTvBox) {
        return new UiMetrics(ctx, isTvBox);
    }

    private boolean tv() {
        return deviceClass == DeviceClass.TV;
    }

    /** Raw dp → px at the activity's density (no device scaling). Use for values that must NOT scale. */
    int dp(float dp) {
        return Math.round(dp * density);
    }

    /** dp → px, multiplied by the device-class scale. Use for chrome geometry that should grow on tablet/TV. */
    int dpS(float dp) {
        return Math.round(dp * scale * density);
    }

    /** sp value multiplied by the device-class scale (for one-off text not covered by a role token). */
    float sp(float sp) {
        return sp * scale;
    }

    // ---- alignment / chrome tokens (single producer each — see plan §A) ----
    int gridH()             { return dpS(14); }   // shared right-edge content grid
    int pillCorner()        { return dpS(8); }
    int pillPadH()          { return dpS(4); }
    int clusterBox()        { return dpS(40); }
    int clusterPad()        { return dpS(8); }
    int heroBox()           { return dpS(90); }   // central play/pause tap target (was exo_icon_size)
    int heroInset()         { return dpS(10); }   // coral disc inset within the hero box
    int episodeDisc()       { return dpS(46); }
    int episodeDiscPad()    { return dpS(10); }
    int episodeDiscMargin() { return dpS(6); }
    int spinnerSize()       { return dpS(60); }   // loading ring, centered over the hero — keep ∝ hero
    int listPad()           { return dpS(10); }
    int lockMarginEnd()     { return dpS(8); }

    int posterHeight() {
        switch (deviceClass) {
            case TV:            return dp(96);
            case TABLET_LARGE:  return dp(88);
            case TABLET_MEDIUM: return dp(80);
            default:            return dp(74);
        }
    }

    int rowMinHeight() {
        switch (deviceClass) {
            case TV:            return dp(56);
            case TABLET_LARGE:
            case TABLET_MEDIUM: return dp(52);
            default:            return dp(48);
        }
    }

    // ---- TV overscan, synthesized as extra insets (0 on every non-TV class) ----
    int overscanH() { return tv() ? dp(24) : 0; }
    int overscanV() { return tv() ? dp(16) : 0; }
    int pickerTopPadLand() { return Math.max(dp(16), overscanV()); }

    /** Adaptive picker side-panel width in px: never covers a narrow phone, docks on tablet/TV. */
    int pickerWidthPx(Configuration cfg) {
        final int windowW = cfg.screenWidthDp;
        final int preferred;
        switch (deviceClass) {
            case TV:            preferred = 420; break;
            case TABLET_LARGE:  preferred = 440; break;
            case TABLET_MEDIUM: preferred = 400; break;
            default:            preferred = 360; break;
        }
        final int capPortrait = windowW - 56;                       // always leave a strip of video/scrim
        final int cap = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? Math.min(Math.round(windowW * 0.60f), capPortrait)
                : capPortrait;
        return dp(Math.min(preferred, cap));
    }

    // ---- typography (sp; keeps user font-scale). Columns: PHONE / sw600 / sw720 / TV ----
    private float t(float phone, float mediumTablet, float largeTablet, float tv) {
        switch (deviceClass) {
            case TV:            return tv;
            case TABLET_LARGE:  return largeTablet;
            case TABLET_MEDIUM: return mediumTablet;
            default:            return phone;
        }
    }
    float textTitle()       { return t(18, 20, 21, 22); }   // picker headers
    float textBody()        { return t(16, 17, 18, 20); }   // picker row title
    float textCaption()     { return t(13, 14, 15, 16); }   // row details / subtitle
    float textList()        { return t(15, 16, 17, 18); }   // playlist row
    float textInfo()        { return t(12, 13, 13, 14); }   // header meta lines
    float textHeaderTitle() { return t(22, 24, 25, 26); }   // header title (larger than a picker header)
    float textClock()       { return t(20, 21, 22, 22); }   // clock, header + overlay (must match)
    float textEndsAt()      { return t(16, 17, 18, 18); }   // "until …", a step below the clock
    float textSkip()        { return t(13, 14, 14, 15); }   // skip pill + notification
    float textBadge()       { return t(11, 11, 12, 13); }   // poster number chip
    float textValue()       { return t(40, 44, 46, 48); }   // skip-offset readout
    float textAction()      { return t(15, 16, 16, 18); }   // reset pill
    float textPlaceholder() { return t(20, 21, 22, 22); }   // poster fallback
    float textListNumber()  { return t(18, 19, 20, 20); }   // playlist row number

    /** True when a config change would not affect any adaptive size (skip the re-apply pass). */
    boolean sameClassAndWidth(UiMetrics other) {
        return other != null
                && deviceClass == other.deviceClass
                && screenWidthDp == other.screenWidthDp
                && orientation == other.orientation;
    }
}
