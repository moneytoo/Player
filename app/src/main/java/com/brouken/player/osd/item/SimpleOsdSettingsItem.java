package com.brouken.player.osd.item;

import android.graphics.drawable.Drawable;

public class SimpleOsdSettingsItem implements OsdSettingsItem {

    public static final int VIEW_TYPE = 1;

    public final String title;
    public final Drawable icon;
    public final Listener listener;

    public SimpleOsdSettingsItem(String title, Drawable icon, Listener listener) {
        this.title = title;
        this.icon = icon;
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return VIEW_TYPE;
    }

    public interface Listener {
        void onSettingClicked(int position);
    }

}