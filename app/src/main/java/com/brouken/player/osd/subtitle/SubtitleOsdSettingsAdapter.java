package com.brouken.player.osd.subtitle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.brouken.player.R;
import com.brouken.player.osd.OsdSettingsAdapter;
import com.brouken.player.osd.item.BooleanOsdSettingsItem;
import com.brouken.player.osd.item.ChoiceOsdSettingsItem;
import com.brouken.player.osd.item.IntegerOsdSettingsItem;
import com.brouken.player.osd.item.OsdSettingsItem;
import com.brouken.player.osd.item.SimpleOsdSettingsItem;

public class SubtitleOsdSettingsAdapter extends OsdSettingsAdapter {

    private final Listener listener;

    public SubtitleOsdSettingsAdapter(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
    }

    public void setInitialValues(int subtitlePosition, int size, SubtitleEdgeType edgeType, SubtitleTypeface typeface, boolean embeddedStyles) {
        this.items = createSubtitleSettingsArray(subtitlePosition, size, edgeType, typeface, embeddedStyles);
    }

    private OsdSettingsItem[] createSubtitleSettingsArray(int subtitlePosition, int size, SubtitleEdgeType edgeType, SubtitleTypeface typeface, boolean embeddedStyles) {
        return new OsdSettingsItem[]{
                createPositionItem(subtitlePosition),
                createSizeItem(size),
                createEdgeTypeItem(edgeType),
                createTypefaceItem(typeface),
                createEmbeddedStylesItem(embeddedStyles),
                createCaptioningPreferenceItem()
        };
    }

    private static Drawable getDrawable(Context context, @DrawableRes int id) {
        return ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
    }

    private OsdSettingsItem createPositionItem(int subtitlePosition) {
        String title = context.getString(R.string.osd_subtitle_position_title);
        String labelDefault = context.getString(R.string.osd_item_integer_default);

        IntegerOsdSettingsItem.Listener itemListener = (position, newValue) -> listener.onSubtitlePositionChange(newValue);
        return new IntegerOsdSettingsItem(title, labelDefault, true, subtitlePosition, itemListener, this);
    }

    private OsdSettingsItem createSizeItem(int size) {
        String title = context.getString(R.string.osd_subtitle_size_title);
        String labelDefault = context.getString(R.string.osd_item_integer_default);

        IntegerOsdSettingsItem.Listener itemListener = (position, newValue) -> listener.onSubtitleSizeChange(newValue);
        return new IntegerOsdSettingsItem(title, labelDefault, true, size, itemListener, this);
    }

    private OsdSettingsItem createEdgeTypeItem(SubtitleEdgeType edgeType) {
        String title = context.getString(R.string.osd_subtitle_edge_type_title);

        ChoiceOsdSettingsItem.Element[] elements = new ChoiceOsdSettingsItem.Element[SubtitleEdgeType.values().length];
        for (int i = 0; i < elements.length; i++) {
            switch (SubtitleEdgeType.values()[i]) {
                case Default:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_default));
                    break;
                case None:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_none));
                    break;
                case Outline:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_outline));
                    break;
                case DropShadow:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_drop_shadow));
                    break;
                case Raised:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_raised));
                    break;
                case Depressed:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_edge_type_depressed));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown SubtitleEdgeType: " + SubtitleEdgeType.values()[i]);
            }
        }

        ChoiceOsdSettingsItem.Listener itemListener = (position, newValueIndex) -> listener.onSubtitleEdgeTypeChange(SubtitleEdgeType.values()[newValueIndex]);
        return new ChoiceOsdSettingsItem(title, elements, edgeType.ordinal(), itemListener, this);
    }

    private OsdSettingsItem createTypefaceItem(SubtitleTypeface typeface) {
        String title = context.getString(R.string.osd_subtitle_typeface_title);

        ChoiceOsdSettingsItem.Element[] elements = new ChoiceOsdSettingsItem.Element[SubtitleTypeface.values().length];
        for (int i = 0; i < elements.length; i++) {
            switch (SubtitleTypeface.values()[i]) {
                case Regular:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_typeface_regular));
                    break;
                case Bold:
                    elements[i] = new ChoiceOsdSettingsItem.Element(context.getString(R.string.osd_subtitle_typeface_bold));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown SubtitleTypeface: " + SubtitleEdgeType.values()[i]);
            }
        }

        ChoiceOsdSettingsItem.Listener itemListener = (position, newValueIndex) -> listener.onSubtitleTypefaceChange(SubtitleTypeface.values()[newValueIndex]);
        return new ChoiceOsdSettingsItem(title, elements, typeface.ordinal(), itemListener, this);
    }

    private OsdSettingsItem createEmbeddedStylesItem(boolean embeddedStyles) {
        String title = context.getString(R.string.osd_subtitle_embedded_styles_title);
        String labelTrue = context.getString(R.string.osd_item_boolean_true);
        String labelFalse = context.getString(R.string.osd_item_boolean_false);

        BooleanOsdSettingsItem.Listener itemListener = (position, newValue) -> SubtitleOsdSettingsAdapter.this.listener.onSubtitleEmbeddedStylesChange(newValue);
        return new BooleanOsdSettingsItem(title, labelTrue, labelFalse, embeddedStyles, itemListener, this);
    }

    private OsdSettingsItem createCaptioningPreferenceItem() {
        String title = context.getString(R.string.osd_subtitle_caption_preferences_title);
        @SuppressLint("PrivateResource")
        Drawable icon = getDrawable(context, androidx.media3.ui.R.drawable.exo_styled_controls_settings);
        SimpleOsdSettingsItem.Listener itemListener = position -> listener.onOpenCaptionPreferences();
        return new SimpleOsdSettingsItem(title, icon, itemListener);
    }

    public interface Listener {

        void onSubtitlePositionChange(int position);

        void onSubtitleSizeChange(int size);

        void onSubtitleEdgeTypeChange(SubtitleEdgeType edgeType);

        void onSubtitleTypefaceChange(SubtitleTypeface typeface);

        void onSubtitleEmbeddedStylesChange(boolean embeddedStyles);

        void onOpenCaptionPreferences();

    }

}