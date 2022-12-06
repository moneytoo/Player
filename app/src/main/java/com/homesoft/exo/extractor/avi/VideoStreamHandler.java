package com.homesoft.exo.extractor.avi;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.C;
import androidx.media3.extractor.TrackOutput;

import java.util.Arrays;

public class VideoStreamHandler extends StreamHandler {
    private long frameUs;
    protected int index;
    private boolean allKeyFrames;
    @VisibleForTesting
    int[] indices=new int[0];
    /**
     * Secondary chunk id.  Bad muxers sometimes use uncompressed for key frames
     */
    final int chunkIdAlt;

    VideoStreamHandler(int id, long durationUs, @NonNull TrackOutput trackOutput) {
        super(id, TYPE_VIDEO, durationUs, trackOutput);
        chunkIdAlt = getChunkIdLower(id) | ('d' << 16) | ('b' << 24);
    }

    public boolean handlesChunkId(int chunkId) {
        return super.handlesChunkId(chunkId) || chunkIdAlt == chunkId;
    }

    protected boolean isKeyFrame() {
        // -8 because the position array includes the header, but the read skips it.
        return allKeyFrames || Arrays.binarySearch(positions, readEnd - readSize - 8) >= 0;
    }

    protected void advanceTime() {
        index++;
    }

    @Override
    protected void sendMetadata(int size) {
        if (size > 0) {
            //System.out.println("VideoStream: " + getId() + " Us: " + getTimeUs() + " size: " + size + " key: " + isKeyFrame());
            trackOutput.sampleMetadata(
                    getTimeUs(), (isKeyFrame() ? C.BUFFER_FLAG_KEY_FRAME : 0), size, 0, null);
        }
        advanceTime();
    }

    @Override
    public long[] setSeekStream() {
        final int[] seekFrameIndices;
        if (chunkIndex.isAllKeyFrames()) {
            allKeyFrames = true;
            seekFrameIndices = chunkIndex.getChunkSubset(durationUs, 3);
        } else {
            seekFrameIndices = chunkIndex.getChunkSubset();
        }
        final int frames = chunkIndex.getCount();
        frameUs = durationUs / frames;
        setSeekPointSize(seekFrameIndices.length);
        for (int i=0;i<seekFrameIndices.length;i++) {
            final int index = seekFrameIndices[i];
            positions[i] = chunkIndex.getChunkPosition(index);
            indices[i] = index;
        }
        chunkIndex.release();
        return positions;
    }

    /**
     * Get the stream time for a chunk index
     * @param index the index of chunk in the stream
     */
    protected long getChunkTimeUs(int index) {
        return durationUs * index / this.chunkIndex.getCount();
    }

    @Override
    public long getTimeUs(int seekIndex) {
        if (seekIndex == 0) {
            return 0L;
        }
        return getChunkTimeUs(indices[seekIndex]);
    }

    @Override
    public long getTimeUs() {
        return getChunkTimeUs(index);
    }

    @Override
    public int getTimeUsSeekIndex(long timeUs) {
        if (timeUs == 0L) {
            return 0;
        }
        final int index = (int)(timeUs / frameUs);
        final int seekIndex = Arrays.binarySearch(indices, index);
        if (seekIndex >= 0) {
            // The search rounds down to the nearest chunk time,
            // if we aren't an exact time match fix up the result
            if (getChunkTimeUs(indices[seekIndex]) != timeUs) {
                return -seekIndex -1;
            }
        }
        return seekIndex;
    }

    @Override
    public void seekPosition(long position) {
        final int seekIndex = getSeekIndex(position);
        index = indices[seekIndex];
    }

    @Override
    protected void setSeekPointSize(int seekPointCount) {
        super.setSeekPointSize(seekPointCount);
        indices = new int[seekPointCount];
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setFps(int fps) {
        final int chunks = (int)(durationUs * fps / C.MICROS_PER_SECOND);
        chunkIndex.setCount(chunks);
        frameUs = C.MICROS_PER_SECOND / fps;
    }
}
