package com.brouken.player.osd;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.Util;
import androidx.recyclerview.widget.RecyclerView;

import com.brouken.player.R;
import com.brouken.player.osd.item.LeftOrRightOsdSettingsItem;
import com.brouken.player.osd.item.OsdSettingsItem;
import com.brouken.player.osd.item.SimpleOsdSettingsItem;

public class OsdSettingsAdapter extends RecyclerView.Adapter<OsdSettingsAdapter.OsdSettingsViewHolder> {

    protected final Context context;
    private final LayoutInflater layoutInflater;
    protected OsdSettingsItem[] items;

    public OsdSettingsAdapter(@NonNull Context context) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public OsdSettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case LeftOrRightOsdSettingsItem.VIEW_TYPE:
                view = layoutInflater.inflate(R.layout.osd_settings_item_left_or_right, parent, false);
                return new ValueOsdSettingsViewHolder(view);
            case SimpleOsdSettingsItem.VIEW_TYPE:
                view = layoutInflater.inflate(R.layout.osd_settings_item_simple, parent, false);
                return new SimpleOsdSettingsViewHolder(view);
            default:
                throw new IllegalArgumentException("Unknown viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull OsdSettingsViewHolder holder, int position) {
        OsdSettingsItem item = items[position];
        holder.bind(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    @Override
    public int getItemViewType(int position) {
        return items[position].getViewType();
    }

    public static abstract class OsdSettingsViewHolder extends RecyclerView.ViewHolder {

        public OsdSettingsViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(OsdSettingsItem item);

    }

    private class ValueOsdSettingsViewHolder extends OsdSettingsViewHolder {

        private final TextView titleTextView;
        private final TextView summaryTextView;

        private ValueOsdSettingsViewHolder(View itemView) {
            super(itemView);

            if (Util.SDK_INT < 26) {
                // Workaround for https://github.com/google/ExoPlayer/issues/9061.
                itemView.setFocusable(true);
            }

            titleTextView = itemView.findViewById(android.R.id.title);
            summaryTextView = itemView.findViewById(android.R.id.summary);

            View button1 = itemView.findViewById(android.R.id.button1);
            View button2 = itemView.findViewById(android.R.id.button2);

            button1.setOnClickListener(v -> notifySettingLeftPressed());
            button2.setOnClickListener(v -> notifySettingRightPressed());

            itemView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    boolean isLtr = v.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL;
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            if (isLtr) {
                                button1.setPressed(true);
                                button1.setPressed(false);
                                notifySettingLeftPressed();
                            } else {
                                button2.setPressed(true);
                                button2.setPressed(false);
                                notifySettingRightPressed();
                            }
                            return true;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (isLtr) {
                                button2.setPressed(true);
                                button2.setPressed(false);
                                notifySettingRightPressed();
                            } else {
                                button1.setPressed(true);
                                button1.setPressed(false);
                                notifySettingLeftPressed();
                            }
                            return true;
                    }
                }
                return false;
            });
        }

        @Override
        void bind(OsdSettingsItem item) {
            LeftOrRightOsdSettingsItem valueItem = (LeftOrRightOsdSettingsItem) item;
            titleTextView.setText(valueItem.title);
            if (valueItem.summary == null) {
                summaryTextView.setVisibility(View.GONE);
            } else {
                summaryTextView.setText(valueItem.summary);
            }
        }

        private void notifySettingLeftPressed() {
            int position = getBindingAdapterPosition();
            LeftOrRightOsdSettingsItem item = (LeftOrRightOsdSettingsItem) items[position];
            item.listener.onSettingLeftClick(position);
        }

        private void notifySettingRightPressed() {
            int position = getBindingAdapterPosition();
            LeftOrRightOsdSettingsItem item = (LeftOrRightOsdSettingsItem) items[position];
            item.listener.onSettingRightClick(position);
        }

    }

    private class SimpleOsdSettingsViewHolder extends OsdSettingsViewHolder {

        private final TextView titleTextView;
        private final ImageView iconView;

        private SimpleOsdSettingsViewHolder(View itemView) {
            super(itemView);

            if (Util.SDK_INT < 26) {
                // Workaround for https://github.com/google/ExoPlayer/issues/9061.
                itemView.setFocusable(true);
            }

            titleTextView = itemView.findViewById(android.R.id.title);
            iconView = itemView.findViewById(android.R.id.icon);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                SimpleOsdSettingsItem item = (SimpleOsdSettingsItem) items[position];
                item.listener.onSettingClicked(position);
            });
        }

        @Override
        void bind(OsdSettingsItem item) {
            SimpleOsdSettingsItem simpleItem = (SimpleOsdSettingsItem) item;
            titleTextView.setText(simpleItem.title);
            if (simpleItem.icon == null) {
                iconView.setVisibility(View.GONE);
            } else {
                iconView.setImageDrawable(simpleItem.icon);
            }
        }
    }

}