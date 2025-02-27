package com.brouken.player.osd.item;

public class LeftOrRightOsdSettingsItem implements OsdSettingsItem {

    public static final int VIEW_TYPE = 0;

    public final String title;
    public LeftOrRightOsdSettingsItem.Listener listener;
    public String summary;

    public LeftOrRightOsdSettingsItem(String title, Listener listener) {
        this.title = title;
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return VIEW_TYPE;
    }

    public interface Listener {

        void onSettingLeftClick(int position);

        void onSettingRightClick(int position);

    }

}