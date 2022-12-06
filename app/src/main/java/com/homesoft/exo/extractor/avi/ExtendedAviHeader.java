package com.homesoft.exo.extractor.avi;

import java.nio.ByteBuffer;

/**
 * Optional: Total frames from the AVI
 */
public class ExtendedAviHeader extends ResidentBox {
    public static final int DMLH=0x686C6D64;

    ExtendedAviHeader(ByteBuffer byteBuffer) {
        super(DMLH, byteBuffer);
    }

    public long getTotalFrames() {
        return byteBuffer.getInt(0) & AviExtractor.UINT_MASK;
    }

    @Override
    public String toString() {
        return "ExtendedAviHeader{frames=" + getTotalFrames() + "}";
    }
}
