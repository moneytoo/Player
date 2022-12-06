package com.homesoft.exo;

import android.content.Context;
import android.os.Handler;

import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.video.VideoRendererEventListener;

import com.homesoft.exo.video.BitmapFactoryVideoRenderer;

import java.util.ArrayList;

public class MjpegRenderersFactory extends DefaultRenderersFactory {
    public MjpegRenderersFactory(Context context) {
        super(context);
    }

    @Override
    protected void buildVideoRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        super.buildVideoRenderers(context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, allowedVideoJoiningTimeMs, out);
        out.add(new BitmapFactoryVideoRenderer(eventHandler, eventListener));
    }
}
