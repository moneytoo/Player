package com.brouken.player.osd.item;

import com.brouken.player.osd.OsdSettingsAdapter;

public class IntegerOsdSettingsItem extends LeftOrRightOsdSettingsItem {

    private final Listener listener;
    private final OsdSettingsAdapter adapter;
    private final String labelDefault;
    private final boolean addPlusToValue;

    private int currentValue;

    public IntegerOsdSettingsItem(String title, String labelDefault, boolean addPlusToValue, int value, Listener listener, OsdSettingsAdapter adapter) {
        super(title, null);

        super.listener = createLeftOrRightListener();
        this.listener = listener;
        this.adapter = adapter;
        this.labelDefault = labelDefault;
        this.addPlusToValue = addPlusToValue;

        this.currentValue = value;
        summary = withPlusOrDefault(value);
    }

    private String withPlusOrDefault(int value) {
        if (value == 0 && labelDefault != null) {
            return labelDefault;
        } else if (value > 0 && addPlusToValue) {
            return "+" + value;
        } else {
            return String.valueOf(value);
        }
    }

    private void updateCurrentValue(int position, int newValue) {
        currentValue = newValue;
        summary = withPlusOrDefault(newValue);
        adapter.notifyItemChanged(position);
        listener.onSettingChanged(position, newValue);
    }

    private LeftOrRightOsdSettingsItem.Listener createLeftOrRightListener() {
        return new LeftOrRightOsdSettingsItem.Listener() {
            @Override
            public void onSettingLeftClick(int position) {
                updateCurrentValue(position, currentValue - 1);
            }

            @Override
            public void onSettingRightClick(int position) {
                updateCurrentValue(position, currentValue + 1);
            }
        };
    }

    public interface Listener {
        void onSettingChanged(int position, int newValue);
    }

}