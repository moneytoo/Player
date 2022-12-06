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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A box that is resident in memory
 */
public class ResidentBox implements Box {
  final ByteBuffer byteBuffer;
  final private int chunkId;

  ResidentBox(int chunkId, ByteBuffer byteBuffer) {
    this.chunkId = chunkId;
    this.byteBuffer = byteBuffer;
  }

  @Override
  public int getChunkId() {
    return chunkId;
  }

  @Override
  public long getSize() {
    return byteBuffer.capacity();
  }

  /**
   * Returns shallow copy of this ByteBuffer with the position at 0
   */
  @NonNull
  public ByteBuffer getByteBuffer() {
    final ByteBuffer clone = byteBuffer.duplicate();
    clone.order(ByteOrder.LITTLE_ENDIAN);
    clone.clear();
    return clone;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
