package com.brouken.player.osd.item;

import com.brouken.player.osd.OsdSettingsAdapter;

public class ChoiceOsdSettingsItem extends LeftOrRightOsdSettingsItem {

    private final Element[] elements;
    Listener listener;
    private final OsdSettingsAdapter adapter;

    private int currentElementIndex;

    public ChoiceOsdSettingsItem(String title, Element[] elements, int elementIndex, Listener listener, OsdSettingsAdapter adapter) {
        super(title, null);

        super.listener = createLeftOrRightListener();
        this.elements = elements;
        this.listener = listener;
        this.adapter = adapter;

        this.currentElementIndex = elementIndex;
        summary = elements[elementIndex].name;
    }

    private void updateCurrentElementIndex(int position, int newElementIndex) {
        currentElementIndex = newElementIndex;
        summary = elements[newElementIndex].name;
        adapter.notifyItemChanged(position);
        listener.onSettingChanged(position, newElementIndex);
    }

    private LeftOrRightOsdSettingsItem.Listener createLeftOrRightListener() {
        return new LeftOrRightOsdSettingsItem.Listener() {
            @Override
            public void onSettingLeftClick(int position) {
                int newElementIndex = currentElementIndex == 0 ? elements.length - 1 : currentElementIndex - 1;
                updateCurrentElementIndex(position, newElementIndex);
            }

            @Override
            public void onSettingRightClick(int position) {
                int newElementIndex = currentElementIndex == elements.length - 1 ? 0 : currentElementIndex + 1;
                updateCurrentElementIndex(position, newElementIndex);
            }
        };
    }

    public interface Listener {
        void onSettingChanged(int position, int newElementIndex);
    }

    public static class Element {

        private final String name;

        public Element(String name) {
            this.name = name;
        }

    }

}