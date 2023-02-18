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

import android.util.SparseArray;

import androidx.media3.common.MimeTypes;

import java.nio.ByteBuffer;

/**
 * Wrapper for the WAVEFORMAT structure
 */
public class AudioFormat {
  private static final int WAVE_FORMAT_MPEGLAYER3=0x55;
  private static final SparseArray<String> FORMAT_MAP = new SparseArray<>();
  static {
    FORMAT_MAP.put(0x1, MimeTypes.AUDIO_RAW);    // WAVE_FORMAT_PCM
    FORMAT_MAP.put(WAVE_FORMAT_MPEGLAYER3, MimeTypes.AUDIO_MPEG);
    FORMAT_MAP.put(0xff, MimeTypes.AUDIO_AAC);   // WAVE_FORMAT_AAC
    FORMAT_MAP.put(0x2000, MimeTypes.AUDIO_AC3); // WAVE_FORMAT_DVM - AC3
    FORMAT_MAP.put(0x2001, MimeTypes.AUDIO_DTS); // WAVE_FORMAT_DTS2
  }

  private final ByteBuffer byteBuffer;

  public AudioFormat(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public String getMimeType() {
    return FORMAT_MAP.get(getFormatTag());
  }

  public int getFormatTag() {
    return byteBuffer.getShort(0) & AviExtractor.USHORT_MASK;
  }
  public short getChannels() {
    return byteBuffer.getShort(2);
  }
  public int getSamplesPerSecond() {
    return byteBuffer.getInt(4);
  }
  public int getAvgBytesPerSec() {
    return byteBuffer.getInt(8);
  }
  public int getBlockAlign() {
    return byteBuffer.getShort(12) & AviExtractor.USHORT_MASK;
  }
  public short getBitsPerSample() {
    return byteBuffer.getShort(14);
  }
  public int getCbSize() {
    return byteBuffer.getShort(16) & AviExtractor.USHORT_MASK;
  }

  // Only valid for MPEGLAYER3WAVEFORMAT
  public short getId() {
    return byteBuffer.getShort(18);
  }
  public int getFlags() {
    return byteBuffer.getInt(20);
  }
  public int getBlockSize() {
    return byteBuffer.getShort(24) & AviExtractor.USHORT_MASK;
  }

  public int getFramesPerBlock() {
    return byteBuffer.getShort(26) & AviExtractor.USHORT_MASK;
  }


  public byte[] getCodecData() {
    final int size = getCbSize();
    final ByteBuffer temp = byteBuffer.duplicate();
    temp.clear();
    temp.position(18);
    temp.limit(18 + size);
    final byte[] data = new byte[size];
    temp.get(data);
    return data;
  }

  @Override
  public String toString() {
    final int formatTag = getFormatTag();
    final StringBuilder sb = new StringBuilder(
      "AudioFormat{" +
            "formatTag=" + formatTag +
            ", channels=" + getChannels() +
            ", samplesPerSecond=" + getSamplesPerSecond() +
            ", avgBytesPerSecond=" + getAvgBytesPerSec() +
            ", blockAlign=" + getBlockAlign() +
            ", bitsPerSample=" + getBitsPerSample());
    if (byteBuffer.capacity() > 16) {
      // WAVEFORMATEX
      sb.append(", cbSize=");
      sb.append(getCbSize());
      if (formatTag == WAVE_FORMAT_MPEGLAYER3) {
        sb.append(", ID=");
        sb.append(getId());
        sb.append(", flags=");
        sb.append(getFlags());
        sb.append(", blockSize=");
        sb.append(getBlockSize());
        sb.append(", framesPerBlock=");
        sb.append(getFramesPerBlock());
      }

    }
    sb.append('}');
    return sb.toString();
  }
}
