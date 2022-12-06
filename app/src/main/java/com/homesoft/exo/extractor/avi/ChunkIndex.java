package com.homesoft.exo.extractor.avi;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Used to parse Indexes and build the SeekMap
 * In this class position is absolute file position
 */
public class ChunkIndex {
    public static final int[] ALL_KEY_FRAMES = new int[0];
    private static final long[] RELEASED = new long[0];

    final BitSet keyFrames = new BitSet();
    @NonNull
    @VisibleForTesting
    long[] positions = new long[8];
    int[] sizes = new int[8];
    /**
     * Chunks in the stream
     */
    private int count = 0;
    /**
     * Total size of the stream
     */
    private long size = 0;

    /**
     * Add a chunk
     * @param key key frame
     */
    public void add(long position, int size, boolean key) {
        if (positions.length <= count) {
            checkReleased();
            grow();
        }
        this.positions[count] = position;
        this.sizes[count] = size;
        this.size += size;
        if (key) {
            keyFrames.set(count);
        }
        count++;
    }

    boolean isAllKeyFrames() {
        return keyFrames.cardinality() == count;
    }

    public int getCount() {
        return count;
    }

    public long getSize() {
        return size;
    }

    public int getKeyFrameCount() {
        return keyFrames.cardinality();
    }

    /**
     * Get the key frame indices
     * @return the key frame indices or {@link #ALL_KEY_FRAMES}
     */
    public int[] getChunkSubset() {
        checkReleased();
        if (isAllKeyFrames()) {
            return ALL_KEY_FRAMES;
        } else {
            final int[] keyFrameIndices = new int[getKeyFrameCount()];
            int i=0;
            for (int f = 0; f < count; f++) {
                if (keyFrames.get(f)) {
                    keyFrameIndices[i++] = f;
                }
            }
            return keyFrameIndices;
        }
    }

    /**
     * Used for creating the SeekMap
     * @param seekPositions array of positions, usually key frame positions of another stream
     * @return the chunk indices after the seekPosition (next frame).
     */
    public int[] getIndices(final long[] seekPositions) {
        checkReleased();

        final int[] work = new int[seekPositions.length];
        int i = 0;
        final int maxI = getCount() - 1;
        // Start p at 1, so seekPosition[0] is always mapped to index 0
        for (int p=1;p<seekPositions.length;p++) {
            while (i < maxI && positions[i] < seekPositions[p]) {
                i++;
            }
            work[p] = i;
        }
        return work;
    }

    public long getChunkPosition(int index) {
        return positions[index];
    }

    public int getChunkSize(int index) {
        return sizes[index];
    }

    /**
     * Build a subset of chunk indices given the stream duration and a chunk rate per second
     * Useful for generating a sparse set of key frames
     * @param durationUs stream length in Us
     * @param chunkRate secs between chunks
     */
    public int[] getChunkSubset(final long durationUs, final int chunkRate) {
        checkReleased();
        final long chunkDurUs = durationUs / count;
        final long chunkRateUs = chunkRate * 1_000_000L;
        final int[] work = new int[count]; //This is overkill, but keeps the logic simple.
        long clockUs = 0;
        long nextChunkUs = 0;
        int k = 0;
        for (int f = 0; f < count; f++) {
            if (clockUs >= nextChunkUs) {
                work[k++] = f;
                nextChunkUs += chunkRateUs;
            }
            clockUs+= chunkDurUs;
        }
        return Arrays.copyOf(work, k);
    }

    private void checkReleased() {
        if (positions == RELEASED) {
            throw new IllegalStateException("ChunkIndex released.");
        }
    }

    /**
     * Release the arrays at this point only getChunks() and isAllKeyFrames() are allowed
     */
    public void release() {
        positions = RELEASED;
        keyFrames.clear();
    }

    private void grow() {
        int newLength = positions.length * 5 / 4;
        positions = Arrays.copyOf(positions, newLength);
        sizes = Arrays.copyOf(sizes, newLength);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void setCount(int count) {
        this.count = count;
    }
}
