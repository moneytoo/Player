package com.brouken.player.osd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.media3.common.util.Util;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brouken.player.PlayerActivity;
import com.brouken.player.Prefs;
import com.brouken.player.R;
import com.brouken.player.osd.subtitle.SubtitleEdgeType;
import com.brouken.player.osd.subtitle.SubtitleOsdSettingsAdapter;
import com.brouken.player.osd.subtitle.SubtitleTypeface;

public class OsdSettingsController {

    private final PlayerActivity playerActivity;
    private final Prefs prefs;

    private final PopupWindow osdSettingsWindow;

    @SuppressLint("InflateParams")
    public OsdSettingsController(PlayerActivity playerActivity) {
        this.playerActivity = playerActivity;
        this.prefs = playerActivity.mPrefs;

        Context context = playerActivity.playerView.getContext();

        SubtitleOsdSettingsAdapter subtitleAdapter =
                new SubtitleOsdSettingsAdapter(context, createSubtitleSettingsListener());

        subtitleAdapter.setInitialValues(
                prefs.subtitleVerticalPosition,
                prefs.subtitleSize,
                prefs.subtitleEdgeType,
                prefs.subtitleStyleBold ? SubtitleTypeface.Bold : SubtitleTypeface.Regular,
                prefs.subtitleStyleEmbedded
        );

        View settingsView = LayoutInflater.from(context).inflate(R.layout.osd_settings, null);
        RecyclerView recyclerView = settingsView.findViewById(android.R.id.list);
        recyclerView.setAdapter(subtitleAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        osdSettingsWindow =
                new PopupWindow(settingsView, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, true);
        if (Util.SDK_INT < 23) {
            // Work around issue where tapping outside of the menu area or pressing the back button
            // doesn't dismiss the menu as expected. See: https://github.com/google/ExoPlayer/issues/8272.
            osdSettingsWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void showSubtitleSettings() {
        int margin = playerActivity.getResources().getDimensionPixelSize(R.dimen.osd_settings_margin);
        TextView titleTextView = osdSettingsWindow.getContentView().findViewById(android.R.id.text1);
        titleTextView.setText(R.string.osd_subtitle_title);
        osdSettingsWindow.showAtLocation(playerActivity.playerView, Gravity.END | Gravity.TOP, margin, margin);

        // Without delaying hide, controller's UI reappears when
        // using physical button on a remote to open settings
        playerActivity.playerView.postDelayed(playerActivity.playerView::hideController, 100);
    }

    private SubtitleOsdSettingsAdapter.Listener createSubtitleSettingsListener() {
        return new SubtitleOsdSettingsAdapter.Listener() {
            @Override
            public void onSubtitlePositionChange(int position) {
                prefs.updateSubtitleVerticalPosition(position);
            }

            @Override
            public void onSubtitleSizeChange(int size) {
                prefs.updateSubtitleSize(size);
            }

            @Override
            public void onSubtitleEdgeTypeChange(SubtitleEdgeType edgeType) {
                prefs.updateSubtitleEdgeType(edgeType);
            }

            @Override
            public void onSubtitleTypefaceChange(SubtitleTypeface typeface) {
                prefs.updateSubtitleStyleBold(typeface == SubtitleTypeface.Bold);
            }

            @Override
            public void onSubtitleEmbeddedStylesChange(boolean embeddedStyles) {
                prefs.updateSubtitleStyleEmbedded(embeddedStyles);
            }

            @Override
            public void onOpenCaptionPreferences() {
                osdSettingsWindow.dismiss();
                playerActivity.enableRotation();
                Intent intent = new Intent(Settings.ACTION_CAPTIONING_SETTINGS);
                playerActivity.safelyStartActivityForResult(intent, PlayerActivity.REQUEST_SYSTEM_CAPTIONS);
            }
        };
    }

}