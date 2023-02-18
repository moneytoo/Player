package com.homesoft.exo.video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.VideoSize;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.video.VideoRendererEventListener;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BitmapFactoryVideoRenderer extends BaseRenderer {
  static final String TAG = "BitmapFactoryRenderer";

  private static int threadId;

  private final Rect rect = new Rect();
  final VideoRendererEventListener.EventDispatcher eventDispatcher;
  final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, 1, 1,
          TimeUnit.SECONDS, new ArrayBlockingQueue<>(2), new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, TAG + "#" + threadId++);
    }
  });
  final DecoderInputBuffer decoderInputBuffer =
          new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);

  @Nullable
  volatile Surface surface;

  private VideoSize lastVideoSize = VideoSize.UNKNOWN;
  private boolean firstFrameRendered;
  @Nullable
  private DecoderCounters decoderCounters;
  @Nullable
  private DecodeRunnable decodeRunnable;

  public BitmapFactoryVideoRenderer(@Nullable Handler eventHandler,
                                    @Nullable VideoRendererEventListener eventListener) {
    super(C.TRACK_TYPE_VIDEO);
    eventDispatcher = new VideoRendererEventListener.EventDispatcher(eventHandler, eventListener);
  }

  @NonNull
  @Override
  public String getName() {
    return TAG;
  }

  @Override
  protected void onEnabled(boolean joining, boolean mayRenderStartOfStream) {
    decoderCounters = new DecoderCounters();
    eventDispatcher.enabled(decoderCounters);
  }

  @Override
  protected void onPositionReset(long positionUs, boolean joining) {
    // Prevent the current pending frame from being rendered
    decodeRunnable = null;
  }

  @Override
  protected void onDisabled() {
    super.onDisabled();
    decodeRunnable = null;

    @Nullable final DecoderCounters decoderCounters = this.decoderCounters;
    if (decoderCounters != null) {
      eventDispatcher.disabled(decoderCounters);
    }
  }

  @Override
  protected void onReset() {
    super.onReset();
    threadPoolExecutor.shutdownNow();
  }

  private static boolean isFrameLate(long earlyUs) {
    // Class a buffer as late if it should have been presented more than 30 ms ago.
    return earlyUs < -30000;
  }

  /**
   * If the dropped frame was not caused by a positionReset(), report it
   */
  private void maybeReportDroppedFrame(long timeUs, long elapsedRealtimeUs) {
    if (timeUs > getReadingPositionUs()) {
      eventDispatcher.droppedFrames(1, elapsedRealtimeUs);
    }
  }

  @Override
  public void render(long positionUs, long elapsedRealtimeUs) {
    final DecodeRunnable myDecodeRunnable = decodeRunnable;
    if (myDecodeRunnable != null) {
      final long earlyUs = myDecodeRunnable.timeUs - positionUs;
      // If we are within 1 ms, display the frame
      if (earlyUs >= 1_000L) {
        return;
      }
      try {
        final Bitmap bitmap = myDecodeRunnable.getBitmap();
        if (bitmap == null) {
          // Decoder is running late!
          return;
        }
        if (isFrameLate(earlyUs)) {
          maybeReportDroppedFrame(myDecodeRunnable.timeUs, elapsedRealtimeUs);
        } else {
          renderBitmap(bitmap);
        }
      } catch (Exception e) {
        eventDispatcher.videoCodecError(e);
      }
      decodeRunnable = null;
    }

    while (true) {
      decoderInputBuffer.clear();
      final int result = readSource(getFormatHolder(), decoderInputBuffer,0);
      switch (result) {
        case C.RESULT_BUFFER_READ:
          if (decoderInputBuffer.isEndOfStream()) {
            return;
          }
          if (decoderInputBuffer.timeUs < positionUs) {
            // When seeking the player sends all the frames from the last I frame to the current frame.
            // This is necessary for progressive video (P/B frames)
            // These frames seems to always be late and since we are all I frames, we can ignore them.
            maybeReportDroppedFrame(decoderInputBuffer.timeUs, elapsedRealtimeUs);
            continue;
          }
          final ByteBuffer byteBuffer = decoderInputBuffer.data;
          if (byteBuffer != null) {
            byteBuffer.flip();
            final byte[] buffer = new byte[byteBuffer.remaining()];
            byteBuffer.get(buffer);
            //Log.d("Test", "Queued " + decoderInputBuffer.timeUs + " leadUs: " + (decoderInputBuffer.timeUs - positionUs));
            decodeRunnable = new DecodeRunnable(decoderInputBuffer.timeUs, buffer);
            threadPoolExecutor.execute(decodeRunnable);
          }
          return;
        case C.RESULT_NOTHING_READ:
          return;
        case C.RESULT_FORMAT_READ:
          // Intentionally blank
      }
    }
  }

  @Override
  public void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
    if (messageType == MSG_SET_VIDEO_OUTPUT) {
      if (message instanceof Surface) {
        surface = (Surface) message;
      } else {
        surface = null;
      }
    }
    super.handleMessage(messageType, message);
  }

  @Override
  public boolean isReady() {
    return surface != null;
  }

  @Override
  public boolean isEnded() {
    return decoderInputBuffer.isEndOfStream();
  }

  @Override
  public int supportsFormat(Format format) throws ExoPlaybackException {
    //Technically could support any format BitmapFactory supports
    if (MimeTypes.VIDEO_MJPEG.equals(format.sampleMimeType)) {
      return RendererCapabilities.create(C.FORMAT_HANDLED);
    }
    return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
  }

  @WorkerThread
  void renderBitmap(@NonNull final Bitmap bitmap) {
    @Nullable
    final Surface surface = this.surface;
    if (surface == null) {
      return;
    }
    //Log.d(TAG, "Drawing: " + bitmap.getWidth() + "x" + bitmap.getHeight());
    try {
      final Canvas canvas = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
              bitmap.getConfig() == Bitmap.Config.HARDWARE ? surface.lockHardwareCanvas() :
              surface.lockCanvas(null);

      renderBitmap(bitmap, canvas);

      surface.unlockCanvasAndPost(canvas);

    } catch (IllegalStateException e) {
      // For some reason Samsung devices running 12 crash sometimes.
      eventDispatcher.videoCodecError(e);
      return;
    }
    @Nullable
    final DecoderCounters decoderCounters = BitmapFactoryVideoRenderer.this.decoderCounters;
    if (decoderCounters != null) {
      decoderCounters.renderedOutputBufferCount++;
    }
    if (!firstFrameRendered) {
      firstFrameRendered = true;
      eventDispatcher.renderedFirstFrame(surface);
    }
  }

  @WorkerThread
  @VisibleForTesting
  void renderBitmap(Bitmap bitmap, Canvas canvas) {
    final VideoSize videoSize = new VideoSize(bitmap.getWidth(), bitmap.getHeight());
    if (!videoSize.equals(lastVideoSize)) {
      lastVideoSize = videoSize;
      eventDispatcher.videoSizeChanged(videoSize);
    }
    rect.set(0,0,canvas.getWidth(), canvas.getHeight());
    canvas.drawBitmap(bitmap, null, rect, null);
  }

  static class DecodeRunnable implements Runnable {
    static final BitmapFactory.Options options = new BitmapFactory.Options();
    static {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        options.inPreferredConfig = Bitmap.Config.HARDWARE;
      }
    }

    final long timeUs;
    @NonNull
    final private byte[] buffer;
    @Nullable
    volatile private Bitmap bitmap;
    @Nullable private volatile Exception exception;

    public DecodeRunnable(long timeUs, @NonNull byte[] buffer) {
      this.timeUs = timeUs;
      this.buffer = buffer;
    }

    @Override
    public void run() {
      try {
        bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
        if (bitmap == null) {
          exception = new NullPointerException("Decode bytes failed");
        }
      } catch (Exception e) {
        exception = e;
      }
    }

    /**
     *
     * @return the Bitmap if processing complete or null if still running
     * @throws Exception if processing failed with an exception or produced a null Bitmap
     */
    @Nullable
    public Bitmap getBitmap() throws Exception {
      if (bitmap != null) {
        return bitmap;
      } else if (exception != null) {
        throw exception;
      } else {
        return null;
      }
    }

    @Override
    public String toString() {
      return "DecodeRunnable{" +
              "timeUs=" + timeUs +
              ", bitmap=" + bitmap +
              ", exception=" + exception +
              '}';
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Rect getRect() {
    return rect;
  }

  @Nullable
  @VisibleForTesting
  DecoderCounters getDecoderCounters() {
    return decoderCounters;
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Surface getSurface() {
    return surface;
  }
}
