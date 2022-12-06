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

import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.TrackOutput;

import java.io.IOException;
import java.util.Arrays;

/**
 * Handles chunk data from a given stream.
 * This acts a bridge between AVI and ExoPlayer
 */
public abstract class StreamHandler implements IReader {

  public static final int TYPE_VIDEO = ('d' << 16) | ('c' << 24);
  public static final int TYPE_AUDIO = ('w' << 16) | ('b' << 24);

  @NonNull
  final TrackOutput trackOutput;

  /**
   * The chunk id as it appears in the index and the movi
   */
  final int chunkId;

  long durationUs;

  /**
   * Seek point variables
   */
  long[] positions = new long[0];

//  /**
//   * Size total size of the stream in bytes calculated by the index
//   */
//  int size;


  /**
   * Open DML IndexBox, currently we just support one, but multiple are possible
   * Will be null if non-exist or if processed (to ChunkIndex)
   */
  @Nullable
  private IndexBox indexBox;

  @NonNull
  protected ChunkIndex chunkIndex = new ChunkIndex();

  /**
   * Size of the current chunk in bytes
   */
  transient int readSize;
  /**
   * Bytes remaining in the chunk to be processed
   */
  transient int readRemaining;

  transient long readEnd;

  /**
   * Get stream id in ASCII
   */
  protected static int getChunkIdLower(int id) {
    int tens = id / 10;
    int ones = id % 10;
    return  ('0' + tens) | (('0' + ones) << 8);
  }

  static int getId(int chunkId) {
    return ((chunkId >> 8) & 0xf) + (chunkId & 0xf) * 10;
  }

  StreamHandler(int id, int chunkType, long durationUs, @NonNull TrackOutput trackOutput) {
    this.chunkId = getChunkIdLower(id) | chunkType;
    this.durationUs = durationUs;
    this.trackOutput = trackOutput;
  }

  /**
   *
   * @return true if this can handle the chunkId
   */
  public boolean handlesChunkId(int chunkId) {
    return this.chunkId == chunkId;
  }

  public abstract long getTimeUs();

  public abstract void seekPosition(long position);

  public long getDurationUs() {
    return durationUs;
  }

  public long getPosition() {
    return readEnd - readRemaining;
  }

  public void setRead(final long position, final int size) {
    readEnd = position + size;
    readRemaining = readSize = size;
  }

  protected boolean readComplete() {
    return readRemaining == 0;
  }

  /**
   * Resume a partial read of a chunk
   * May be called multiple times
   */
  public boolean read(@NonNull ExtractorInput input) throws IOException {
    readRemaining -= trackOutput.sampleData(input, readRemaining, false);
    if (readComplete()) {
      sendMetadata(readSize);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Done reading a chunk.  Send the timing info and advance the clock
   * @param size the amount of data passed to the trackOutput
   */
  protected abstract void sendMetadata(final int size);

  /**
   * Set this stream as the primary seek stream
   * Populate our seekPosition
   * @return the positions of seekFrames
   */
  public abstract long[] setSeekStream();

  /**
   * Perform a BinarySearch to get the correct index
   * @return the exact match or a negative as defined in Arrays.binarySearch()
   */
  protected abstract int getTimeUsSeekIndex(long timeUs);

  public abstract long getTimeUs(int seekIndex);

    /**
     * Gets the streamId.
     * @return The unique stream id for this file
     */
  public int getId() {
    return getId(chunkId);
  }

  protected void setSeekPointSize(int seekPointCount) {
    positions = new long[seekPointCount];
  }

  @NonNull
  public ChunkIndex getChunkIndex() {
    return chunkIndex;
  }

  public IndexBox getIndexBox() {
    return indexBox;
  }

  public void setIndexBox(IndexBox indexBox) {
    this.indexBox = indexBox;
  }

  public int getSeekPointCount() {
    return positions.length;
  }

  public long getPosition(int index) {
    return positions[index];
  }

  protected int getValidSeekIndex(int index) {
    if (index < 0) {
      index = -index - 1;
      if (index >= positions.length) {
        index = positions.length - 1;
      }
    }
    return index;
  }

  protected int getSeekIndex(final long position) {
    if (position == 0) {
      return 0;
    }
    return getValidSeekIndex(Arrays.binarySearch(positions, position));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{position=" + getPosition() + "}";
  }
}
