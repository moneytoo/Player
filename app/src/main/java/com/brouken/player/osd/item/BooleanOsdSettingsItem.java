package com.brouken.player.osd.item;

import com.brouken.player.osd.OsdSettingsAdapter;

public class BooleanOsdSettingsItem extends ChoiceOsdSettingsItem {

    private final Listener listener;

    public BooleanOsdSettingsItem(String title, String labelTrue, String labelFalse, boolean value, Listener listener, OsdSettingsAdapter adapter) {
        super(title, createElements(labelTrue, labelFalse), value ? 1 : 0, null, adapter);

        super.listener = createChoiceListener();
        this.listener = listener;
    }

    private ChoiceOsdSettingsItem.Listener createChoiceListener() {
        return (position, newElementIndex) -> listener.onSettingChanged(position, newElementIndex == 1);
    }

    private static Element[] createElements(String labelTrue, String labelFalse) {
        return new Element[]{
                new Element(labelFalse),
                new Element(labelTrue)
        };
    }

    public interface Listener {
        void onSettingChanged(int position, boolean newValue);
    }

}