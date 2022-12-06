package com.homesoft.exo.extractor.avi;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.extractor.ExtractorInput;

import com.brouken.player.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads the Boxes(Chunks) contains within a parent Box
 */
public abstract class BoxReader implements IReader {
    public static final int CHUNK_HEADER_SIZE = 8;
    public static final int PARENT_HEADER_SIZE = 12;
    protected final HeaderPeeker headerPeeker = new HeaderPeeker();
    protected long position;
    private final int size;
    private final long end;

    static ByteBuffer getByteBuffer(@NonNull ExtractorInput input, int size) throws IOException {
        //This bit of grossness makes sure that the input pointer is always aligned to a chunk
        final byte[] buffer = new byte[CHUNK_HEADER_SIZE + size];
        input.readFully(buffer, 0, buffer.length);
        final ByteBuffer temp = ByteBuffer.wrap(buffer, CHUNK_HEADER_SIZE, buffer.length - CHUNK_HEADER_SIZE);
        final ByteBuffer byteBuffer = temp.slice();
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer;
    }
    /**
     *
     * @param start Start of first chunk usually there is a type preceding the chunk collection
     * @param size Not including the enclosing Box
     */
    BoxReader(long start, int size) {
        position = start;
        this.size = size;
        this.end = start + size;
    }

    @Override
    public long getPosition() {
        return position;
    }

    public long getSize() {
        return size & AviExtractor.UINT_MASK;
    }

    protected long getEnd() {
        return end;
    }

    public long getStart() {
        return end - getSize();
    }

    protected boolean advancePosition() {
        return advancePosition(8 + headerPeeker.getSize());
    }

    protected boolean isComplete() {
        if (BuildConfig.DEBUG && position > getEnd()) {
            Log.wtf(getClass().getSimpleName(), "position(" + position + ") > end("+ getEnd() + ")");
        }
        return position == getEnd();
    }

    protected boolean advancePosition(int bytes) {
        position += bytes;
        //AVI's are byte aligned
        if ((position & 1)==1) {
            position++;
        }
        return isComplete();
    }

    public static class HeaderPeeker {
        final private ByteBuffer peakBuffer = AviExtractor.allocate(12);

        public int getChunkId() {
            return peakBuffer.getInt(0);
        }

        public int getSize() {
            return peakBuffer.getInt(4);
        }

        public int getType() {
            return peakBuffer.getInt(8);
        }


        public boolean peakSafe(@NonNull ExtractorInput input) throws IOException {
            if (input.peekFully(peakBuffer.array(), 0, PARENT_HEADER_SIZE, true)) {
                input.resetPeekPosition();
                peakBuffer.position(PARENT_HEADER_SIZE);
                return true;
            }
            return false;
        }

        public int peak(@NonNull ExtractorInput input, int bytes) throws IOException {
            input.peekFully(peakBuffer.array(), 0, bytes);
            input.resetPeekPosition();
            peakBuffer.position(bytes);
            return getChunkId();
        }

        public int peakType(@NonNull ExtractorInput input) throws IOException {
            input.advancePeekPosition(CHUNK_HEADER_SIZE);
            input.peekFully(peakBuffer.array(), CHUNK_HEADER_SIZE, 4);
            input.resetPeekPosition();
            peakBuffer.position(PARENT_HEADER_SIZE);
            return getType();
        }
    }

    public String toString() {
        return getClass().getSimpleName()+"{" +
                "position=" + position +
                ", start=" + getStart() +
                ", end=" + getEnd() +
                '}';
    }
}
