package com.brouken.player;

import android.content.res.Resources;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.util.MimeTypes;

class CustomDefaultTrackNameProvider extends DefaultTrackNameProvider {
    public CustomDefaultTrackNameProvider(Resources resources) {
        super(resources);
    }

    @Override
    public String getTrackName(Format format) {
        String trackName = super.getTrackName(format);

        // https://github.com/google/ExoPlayer/issues/9452
        trackName = trackName.substring(0, 1).toUpperCase() + (trackName.length() > 1 ? trackName.substring(1) : "");

        if (format.sampleMimeType != null) {
            String sampleFormat = formatNameFromMime(format.sampleMimeType);
            if (BuildConfig.DEBUG && sampleFormat == null) {
                sampleFormat = format.sampleMimeType;
            }
            trackName += " (" + sampleFormat + ")";
        }
        if (format.label != null) {
            if (!trackName.startsWith(format.label)) { // HACK
                trackName += " - " + format.label;
            }
        }
        return trackName;
    }

    private String formatNameFromMime(final String mimeType) {
        switch (mimeType) {
            case MimeTypes.AUDIO_DTS:
                return "DTS";
            case MimeTypes.AUDIO_DTS_HD:
                return "DTS-HD";
            case MimeTypes.AUDIO_DTS_EXPRESS:
                return "DTS Express";
            case MimeTypes.AUDIO_TRUEHD:
                return "TrueHD";
            case MimeTypes.AUDIO_AC3:
                return "AC-3";
            case MimeTypes.AUDIO_E_AC3:
                return "E-AC-3";
            case MimeTypes.AUDIO_E_AC3_JOC:
                return "E-AC-3-JOC";
            case MimeTypes.AUDIO_AC4:
                return "AC-4";
            case MimeTypes.AUDIO_AAC:
                return "AAC";
            case MimeTypes.AUDIO_MPEG:
                return "MPEG";
            case MimeTypes.AUDIO_VORBIS:
                return "Vorbis";
            case MimeTypes.AUDIO_OPUS:
                return "Opus";
            case MimeTypes.AUDIO_FLAC:
                return "FLAC";
            case MimeTypes.AUDIO_ALAC:
                return "ALAC";
            case MimeTypes.AUDIO_WAV:
                return "WAV";
            case MimeTypes.AUDIO_AMR:
                return "AMR";
            case MimeTypes.AUDIO_AMR_NB:
                return "AMR-NB";
            case MimeTypes.AUDIO_AMR_WB:
                return "AMR-WB";

            case MimeTypes.APPLICATION_PGS:
                return "PGS";
            case MimeTypes.APPLICATION_SUBRIP:
                return "SRT";
            case MimeTypes.TEXT_SSA:
                return "SSA";
            case MimeTypes.TEXT_VTT:
                return "VTT";
            case MimeTypes.APPLICATION_TTML:
                return "TTML";
            case MimeTypes.APPLICATION_TX3G:
                return "TX3G";
        }
        return null;
    }
}
