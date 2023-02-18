/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.homesoft.exo.extractor.avi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Log;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Extractor based on the official MicroSoft spec
 * https://docs.microsoft.com/en-us/windows/win32/directshow/avi-riff-file-reference
 */
public class AviExtractor implements Extractor {
  //Minimum time between keyframes in the AviSeekMap
  static final long MIN_KEY_FRAME_RATE_US = 2_000_000L;
  static final long UINT_MASK = 0xffffffffL;
  static final int USHORT_MASK = 0xffff;
  private static final int RELOAD_MINIMUM_SEEK_DISTANCE = 256 * 1024;

  static long getUInt(@NonNull ByteBuffer byteBuffer) {
    return byteBuffer.getInt() & UINT_MASK;
  }

  @NonNull
  static String toString(int tag) {
    final StringBuilder sb = new StringBuilder(4);
    for (int i=0;i<4;i++) {
      sb.append((char)(tag & 0xff));
      tag >>=8;
    }
    return sb.toString();
  }

  @NonNull
  static ByteBuffer allocate(int bytes) {
    final byte[] buffer = new byte[bytes];
    final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return byteBuffer;
  }

  @VisibleForTesting
  static int getStreamId(int chunkId) {
    final int upperChar = chunkId & 0xff;
    if (Character.isDigit(upperChar)) {
      final int lowerChar = (chunkId >> 8) & 0xff;
      if (Character.isDigit(upperChar)) {
        return (lowerChar & 0xf) + ((upperChar & 0xf) * 10);
      }
    }
    return -1;
  }

  static final String TAG = "AviExtractor";
  @VisibleForTesting
  static final int PEEK_BYTES = 28;

  static final int AVIIF_KEYFRAME = 16;


  static final int RIFF = 0x46464952; // RIFF
  static final int AVIX_MASK = 0x00ffffff;
  static final int AVIX = 0x00495641;
  static final int AVI_ = 0x20495641; // AVI<space>
  //movie data box
  static final int MOVI = 0x69766f6d; // movi
  //Index
  static final int IDX1 = 0x31786469; // idx1

  static final int JUNK = 0x4b4e554a; // JUNK
  static final int REC_ = 0x20636572; // rec<space>

  @VisibleForTesting
  final Deque<IReader> readerStack = new ArrayDeque<>(4);
  @VisibleForTesting
  final ArrayList<MoviBox> moviList = new ArrayList<>();

  @VisibleForTesting
  ExtractorOutput output;
  /**
   * From the AviHeader
   */
  private long durationUs = C.TIME_UNSET;
  /**
   * ChunkHandlers by StreamId
   */
  private StreamHandler[] streamHandlers = new StreamHandler[0];
  @VisibleForTesting
  SeekMap seekMap;

  @Override
  public boolean sniff(@NonNull ExtractorInput input) throws IOException {
    final BoxReader.HeaderPeeker headerPeeker = new BoxReader.HeaderPeeker();
    headerPeeker.peak(input, BoxReader.PARENT_HEADER_SIZE);
    return headerPeeker.getChunkId() == RIFF && headerPeeker.getType() == AVI_;
  }

  /**
   * Build and set the SeekMap based on the indices
   */
  private void buildSeekMap() {
    long maxStreamDurationUs = 0;
    for (final StreamHandler streamHandler : streamHandlers) {
      if (streamHandler instanceof AudioStreamHandler) {
        if ((streamHandler.getDurationUs() - durationUs) / (float)durationUs > .05f) {
          w("Audio #" + streamHandler.getId() + " duration is off, using videoDuration");
          ((AudioStreamHandler)streamHandler).setDurationUs(durationUs);
        }
      }
      maxStreamDurationUs = Math.max(maxStreamDurationUs, streamHandler.getDurationUs());
    }

    final StreamHandler seekStreamHandler = getSeekStreamHandler();
    if (seekStreamHandler == null) {
      setSeekMap(new SeekMap.Unseekable(durationUs));
      w("No video track found");
      return;
    }
    long[] positions = seekStreamHandler.setSeekStream();

    for (StreamHandler streamHandler : streamHandlers) {
      // Currently, only Audio streams can be secondary.
      if (streamHandler instanceof AudioStreamHandler && streamHandler != seekStreamHandler) {
        ((AudioStreamHandler) streamHandler).setSeekFrames(positions);
      }
    }
    // The AviHeader value can have rounding errors, so use the max stream duration if it's larger
    setSeekMap(new AviSeekMap(Math.max(maxStreamDurationUs, durationUs),
            seekStreamHandler, moviList.get(0).getStart()));
  }

  @VisibleForTesting
  void setSeekMap(SeekMap seekMap) {
    this.seekMap = seekMap;
    output.seekMap(seekMap);
    //Parsing complete, load movi(s)
    seek(0L, 0L);
  }

  long getDuration() {
    return durationUs;
  }

  @Override
  public void init(@NonNull ExtractorOutput output) {
    this.output = output;
    readerStack.add(new RootReader());
  }

  @VisibleForTesting
  StreamHandler buildStreamHandler(final ListBox streamList, int streamId) {
    final StreamHeaderBox streamHeader = streamList.getChild(StreamHeaderBox.class);
    final StreamFormatBox streamFormat = streamList.getChild(StreamFormatBox.class);
    if (streamHeader == null) {
      w("Missing Stream Header");
      return null;
    }
    //i(streamHeader.toString());
    if (streamFormat == null) {
      w("Missing Stream Format");
      return null;
    }
    final long durationUs = streamHeader.getDurationUs();
    final Format.Builder builder = new Format.Builder();
    builder.setId(streamId);
    final int suggestedBufferSize = streamHeader.getSuggestedBufferSize();
    if (suggestedBufferSize != 0) {
      builder.setMaxInputSize(suggestedBufferSize);
    }
    final StreamNameBox streamName = streamList.getChild(StreamNameBox.class);
    if (streamName != null) {
      builder.setLabel(streamName.getName());
    }
    final StreamHandler streamHandler;
    if (streamHeader.isVideo()) {
      final VideoFormat videoFormat = streamFormat.getVideoFormat();
      final String mimeType = videoFormat.getMimeType();
      if (mimeType == null) {
        Log.w(TAG, "Unknown FourCC: " + toString(videoFormat.getCompression()));
        return null;
      }
      final TrackOutput trackOutput = output.track(streamId, C.TRACK_TYPE_VIDEO);
      builder.setWidth(videoFormat.getWidth());
      builder.setHeight(videoFormat.getHeight());
      builder.setFrameRate(streamHeader.getFrameRate());
      builder.setSampleMimeType(mimeType);

      if (MimeTypes.VIDEO_H264.equals(mimeType)) {
        streamHandler = new AvcStreamHandler(streamId, durationUs, trackOutput, builder);
      } else if (MimeTypes.VIDEO_MP4V.equals(mimeType)) {
        streamHandler = new Mp4VStreamHandler(streamId, durationUs, trackOutput, builder);
      } else {
        streamHandler = new VideoStreamHandler(streamId, durationUs, trackOutput);
      }
      trackOutput.format(builder.build());
    } else if (streamHeader.isAudio()) {
      final AudioFormat audioFormat = streamFormat.getAudioFormat();
      final TrackOutput trackOutput = output.track(streamId, C.TRACK_TYPE_AUDIO);
      final String mimeType = audioFormat.getMimeType();
      builder.setSampleMimeType(mimeType);
      builder.setChannelCount(audioFormat.getChannels());
      builder.setSampleRate(audioFormat.getSamplesPerSecond());
      final int bytesPerSecond = audioFormat.getAvgBytesPerSec();
      if (bytesPerSecond != 0) {
        builder.setAverageBitrate(bytesPerSecond * 8);
      }
      if (MimeTypes.AUDIO_RAW.equals(mimeType)) {
        final short bps = audioFormat.getBitsPerSample();
        if (bps == 8) {
          builder.setPcmEncoding(C.ENCODING_PCM_8BIT);
        } else if (bps == 16){
          builder.setPcmEncoding(C.ENCODING_PCM_16BIT);
        }
      }
      if (MimeTypes.AUDIO_AAC.equals(mimeType) && audioFormat.getCbSize() > 0) {
        builder.setInitializationData(Collections.singletonList(audioFormat.getCodecData()));
      }
      trackOutput.format(builder.build());
      if (MimeTypes.AUDIO_MPEG.equals(mimeType)) {
        streamHandler = new MpegAudioStreamHandler(streamId, durationUs, trackOutput,
            audioFormat.getSamplesPerSecond());
      } else {
        streamHandler = new AudioStreamHandler(streamId, durationUs,
            trackOutput);
      }
    }else {
      streamHandler = null;
    }
    if (streamHandler != null) {
      final IndexBox indexBox = streamList.getChild(IndexBox.class);
      if (indexBox != null && indexBox.getIndexType() == IndexBox.AVI_INDEX_OF_INDEXES) {
        streamHandler.setIndexBox(indexBox);
      }
    }
    return streamHandler;
  }

  @VisibleForTesting
  StreamHandler getSeekStreamHandler() {
    if (streamHandlers.length == 0) {
      return null;
    }

    for (StreamHandler streamHandler : streamHandlers) {
      if (streamHandler instanceof VideoStreamHandler) {
        return streamHandler;
      }
    }
    //Can't find video? just default to first stream
    return streamHandlers[0];
  }

  @NonNull
  List<IndexBox> getIndexBoxList() {
    final ArrayList<IndexBox> list = new ArrayList<>();
    for (StreamHandler streamHandler : streamHandlers) {
      final IndexBox indexBox = streamHandler.getIndexBox();
      if (indexBox != null) {
        list.add(indexBox);
      }
    }
    if (list.size() > 0 && list.size() != streamHandlers.length) {
      w("StreamHandlers.length != IndexBoxes.length");
      list.clear();
    }
    return list;
  }

  /**
   * Reads the index and sets the keyFrames and creates the SeekMap
   */
  void parseIdx1(ByteBuffer indexByteBuffer) {
    if (indexByteBuffer.capacity() < 16) {
      setSeekMap(new SeekMap.Unseekable(durationUs));
      w("Index too short");
      return;
    }
    final long firstChunkPos = getFirstChunkPosition();
    // Specifies the location of the data chunk in the file.
    // The value should be specified as an offset, in bytes, from the start of the 'movi' list;
    // however, in some AVI files it is given as an offset from the start of the file.
    final long baseOffset;
    if (indexByteBuffer.getInt(8) < firstChunkPos) {
      // This is offset from the box start not the first chunk, so subtract 'movi'
      baseOffset = firstChunkPos -4;
    } else {
      //Bug: Some muxers use absolute position
      baseOffset = 0L;
    }

    while (indexByteBuffer.remaining() >= 16) {
      final int chunkId = indexByteBuffer.getInt(); //0
      final int flags = indexByteBuffer.getInt(); //4
      final int offset = indexByteBuffer.getInt(); //8
      final int size = indexByteBuffer.getInt(); // 12 Size

      final StreamHandler streamHandler = getStreamHandler(chunkId);
      if (streamHandler != null) {
        streamHandler.getChunkIndex().add(baseOffset + (offset & UINT_MASK), size,
                (flags & AVIIF_KEYFRAME) == AVIIF_KEYFRAME);
      }
    }
    buildSeekMap();
  }

  @Nullable
  @VisibleForTesting
  StreamHandler getStreamHandler(int chunkId) {
    for (StreamHandler streamHandler : streamHandlers) {
      if (streamHandler.handlesChunkId(chunkId)) {
        return streamHandler;
      }
    }
    return null;
  }

  void createStreamHandlers(ListBox headerListBox) {
    final AviHeaderBox aviHeader = headerListBox.getChild(AviHeaderBox.class);
    if (aviHeader == null) {
      throw new IllegalArgumentException("Expected AviHeader in header ListBox");
    }
    long totalFrames = aviHeader.getTotalFrames();
    for (Box box : headerListBox.getChildren()) {
      if (box instanceof ListBox) {
        final ListBox listBox = (ListBox) box;
        if (listBox.getType() == ListBox.TYPE_STRL) {
          final ListBox streamListBox = (ListBox) box;
          final int streamId = streamHandlers.length;
          final StreamHandler streamHandler = buildStreamHandler(streamListBox, streamId);
          if (streamHandler != null) {
            streamHandlers = Arrays.copyOf(streamHandlers, streamId + 1);
            streamHandlers[streamId] = streamHandler;
          }
        } else if (listBox.getType() == ListBox.TYPE_ODML) {
          final ExtendedAviHeader extendedAviHeader = listBox.getChild(ExtendedAviHeader.class);
          if (extendedAviHeader != null) {
            totalFrames = extendedAviHeader.getTotalFrames();
          }
        }
      }
    }
    durationUs = totalFrames * aviHeader.getMicroSecPerFrame();
    output.endTracks();
  }

  private int maybeSetPosition(@NonNull ExtractorInput input, @NonNull PositionHolder positionHolder, long position) throws IOException {
    final long skip = position - input.getPosition();
    if (skip == 0) {
      return RESULT_CONTINUE;
    } else if (skip < 0 || skip > RELOAD_MINIMUM_SEEK_DISTANCE) {
      positionHolder.position = position;
      return RESULT_SEEK;
    } else {
      input.skipFully((int)skip);
      return RESULT_CONTINUE;
    }
  }

  @Override
  public int read(@NonNull ExtractorInput input, @NonNull PositionHolder positionHolder) throws IOException {
    final IReader reader = readerStack.peek();
    if (reader == null) {
      return RESULT_END_OF_INPUT;
    }
    if (reader.getPosition() != input.getPosition()) {
      final int op = maybeSetPosition(input, positionHolder, reader.getPosition());
//      if (op == RESULT_SEEK) {
//        i("Seek from: " + input.getPosition() + " for " + reader);
//      }
      return op;
    }
    if (reader.read(input)) {
      readerStack.remove(reader);
      if (reader instanceof Runnable) {
        ((Runnable) reader).run();
      }
    }
    return RESULT_CONTINUE;
  }

  @Override
  public void seek(long position, long timeUs) {
    //i("Seek pos=" + position +", us="+timeUs);
    if (seekMap == null) {
      //Until we have the seekMap assume we are still parsing
      return;
    }
    readerStack.clear();
    for (MoviBox moviBox : moviList) {
      if (moviBox.setPosition(position)) {
        readerStack.add(moviBox);
      }
    }
    for (@NonNull StreamHandler streamHandler : streamHandlers) {
      streamHandler.seekPosition(position);
    }
  }

  @Override
  public void release() {
    readerStack.clear();
    moviList.clear();
    streamHandlers = new StreamHandler[0];
  }

  @VisibleForTesting
  void setChunkHandlers(StreamHandler[] streamHandlers) {
    this.streamHandlers = streamHandlers;
  }

  private static void w(String message) {
    Log.w(TAG, message);
  }

  private static void i(String message) {
    Log.i(TAG, message);
  }

  /**
   * Queue the IReader to run next
   * @param reader If the reader is Runnable, it will be run on completion
   */
  public void push(IReader reader) {
    readerStack.push(reader);
  }

  /**
   * Add a MoviBox to the list
   */
  void addMovi(@NonNull MoviBox moviBox) {
    moviList.add(moviBox);
  }

  /**
   * Get the position of first chunk
   */
  long getFirstChunkPosition() {
    return moviList.get(0).getStart();
  }

  class RootReader extends BoxReader implements Runnable {
    private long size = Long.MIN_VALUE;

    RootReader() {
      super(0L, -1);
    }

    @Override
    public long getSize() {
      return size;
    }

    @Override
    protected long getEnd() {
      return size;
    }

    @Nullable
    private RiffReader riffReader;

    @Override
    protected boolean isComplete() {
      return super.isComplete() && (riffReader == null || riffReader.isComplete());
    }

    @Override
    public boolean read(@NonNull ExtractorInput input) throws IOException {
      if (size == Long.MIN_VALUE) {
        size = input.getLength();
      }
      if (isComplete()) {
        return true;
      }
      final int chunkId;
      if (headerPeeker.peakSafe(input)) {
        chunkId = headerPeeker.getChunkId();
      } else {
        return true;
      }
      if (chunkId != RIFF) {
        throw new IOException("Expected RIFF");
      }
      final int type = headerPeeker.getType();
      if ((type & AVIX_MASK) != AVIX) {
        throw new IOException("Expected AVI?");
      }
      riffReader = new RiffReader(position + PARENT_HEADER_SIZE, headerPeeker.getSize() - 4, type);
      push(riffReader);
      return advancePosition(CHUNK_HEADER_SIZE + headerPeeker.getSize());
    }

    @Override
    public void run() {
      //After the last RiffBox finishes process the OpenDML indexes
      final List<IndexBox> indexBoxList = getIndexBoxList();
      if (!indexBoxList.isEmpty()) {
        final List<Long> list = new ArrayList<>();
        for (IndexBox indexBox : indexBoxList) {
          list.addAll(indexBox.getPositions());
        }
        readerStack.push(new IdxxBox(list));
      }
    }
  }

  class RiffReader extends BoxReader {
    private final int riffType;
    public RiffReader(long start, int size, int type) {
      super(start, size);
      this.riffType = type;
    }

    @Override
    public boolean read(@NonNull ExtractorInput input) throws IOException {
      final int chunkId = headerPeeker.peak(input, CHUNK_HEADER_SIZE);
      final int size = headerPeeker.getSize();
      switch (chunkId) {
        case ListBox.LIST:
          final int type = headerPeeker.peakType(input);
          if (type == MOVI) {
            addMovi(new MoviBox(position + PARENT_HEADER_SIZE, size - 4));
            if (riffType == AVIX || getIndexBoxList().size() > 0) {
              //If we have OpenDML Indexes exit early and skip the IDX1 Index
              position = getEnd();
              return true;
            }
          } else if (type == ListBox.TYPE_HDRL){
            readerStack.push(new HeaderListBox(position + PARENT_HEADER_SIZE, size - 4, readerStack));
          }
          break;
        case IDX1: {
          final ByteBuffer byteBuffer = getByteBuffer(input, size);
          parseIdx1(byteBuffer);
        }
      }
      return advancePosition();
    }
  }

  /**
   * Box of stream chunks
   */
  class MoviBox extends BoxReader {
    MoviBox(long start, int size) {
      super(start, size);
    }

    /**
     * Prepares the MoviBox to be added to the readerQueue
     * @param position will be set to {@link #getStart()}
     * @return false if position after end (don't  use)
     */
    public boolean setPosition(long position) {
      if (position > getEnd()) {
        return false;
      }
      this.position = Math.max(getStart(), position);
      return true;
    }

    @Override
    public boolean read(@NonNull ExtractorInput input) throws IOException {
      final int chunkId = headerPeeker.peak(input, CHUNK_HEADER_SIZE);
      final StreamHandler streamHandler = getStreamHandler(chunkId);
      if (streamHandler != null) {
        streamHandler.setRead(position + CHUNK_HEADER_SIZE, headerPeeker.getSize());
        push(streamHandler);
      } else if (chunkId == ListBox.LIST) {
        final int type = headerPeeker.peakType(input);
        if (type == REC_) {
          return advancePosition(PARENT_HEADER_SIZE);
        }
      }
      return advancePosition();
    }
  }

  class IdxxBox implements IReader {
    private final ArrayDeque<Long> deque;

    IdxxBox(List<Long> positionList) {
      Collections.sort(positionList);
      deque = new ArrayDeque<>(positionList);
    }

    @Override
    public long getPosition() {
      return deque.peekFirst();
    }

    @Override
    public boolean read(@NonNull ExtractorInput input) throws IOException {
      final BoxReader.HeaderPeeker headerPeeker = new BoxReader.HeaderPeeker();
      headerPeeker.peak(input, BoxReader.CHUNK_HEADER_SIZE);
      ByteBuffer byteBuffer = BoxReader.getByteBuffer(input, headerPeeker.getSize());
      deque.pop();
      byteBuffer.position(byteBuffer.position() + 2); //Skip longs per entry
      final byte indexSubType = byteBuffer.get();
      if (indexSubType != 0) {
        throw new IllegalArgumentException("Expected IndexSubType 0 got " + indexSubType);
      }
      final byte indexType = byteBuffer.get();
      if (indexType != IndexBox.AVI_INDEX_OF_CHUNKS) {
        throw new IllegalArgumentException("Expected IndexType 1 got " + indexType);
      }
      final int entriesInUse = byteBuffer.getInt();
      final int chunkId = byteBuffer.getInt();
      final StreamHandler streamHandler = getStreamHandler(chunkId);
      if (streamHandler == null) {
        w("No StreamHandler for " + AviExtractor.toString(chunkId));
      } else {
        final ChunkIndex chunkIndex = streamHandler.getChunkIndex();
        //baseOffset does not include the chunk header, so -8 to be compatible with IDX1
        final long baseOffset = byteBuffer.getLong() - 8;
        byteBuffer.position(byteBuffer.position() + 4); // Skip reserved

        for (int i=0;i<entriesInUse;i++) {
          final int offset = byteBuffer.getInt();
          final int size = byteBuffer.getInt();
          final int size31 = size & 0x7f_ff_ff_ff;
          chunkIndex.add(baseOffset + (offset & AviExtractor.UINT_MASK), size31,
                  size == size31);
        }
      }
      if (!deque.isEmpty()) {
        return false;
      }
      buildSeekMap();
      return true;
    }
    @Override
    public String toString() {
      return "IdxxBox{positions=" + deque +
              "}";
    }
  }

  class HeaderListBox extends ListBox implements Runnable {
    public HeaderListBox(long position, int size, @NonNull Deque<IReader> readerStack) {
      super(position, size, ListBox.TYPE_HDRL, readerStack);
    }

    @Override
    public void run() {
      createStreamHandlers(this);
    }
  }
}
