package com.homesoft.exo.extractor.avi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Open DML Index Box
 */
public class IndexBox extends ResidentBox {
    public static final int INDX = 0x78646E69;
    //Supported IndexType(s)
    public static final byte AVI_INDEX_OF_INDEXES = 0;
    public static final byte AVI_INDEX_OF_CHUNKS = 1;

    IndexBox(ByteBuffer byteBuffer) {
        super(INDX, byteBuffer);
    }
    int getLongsPerEntry() {
        return byteBuffer.getShort(0) & 0xffff;
    }
    byte getIndexType() {
        return byteBuffer.get(3);
    }
    int getEntriesInUse() {
        return byteBuffer.get(4);
    }
    //8 = IndexChunkId

    List<Long> getPositions() {
        final int entriesInUse = getEntriesInUse();
        final ArrayList<Long> list = new ArrayList<>(getEntriesInUse());
        final int entrySize = getLongsPerEntry() * 4;
        for (int i=0;i<entriesInUse;i++) {
            list.add(byteBuffer.getLong(0x18 + i * entrySize));
        }
        return list;
    }
}
