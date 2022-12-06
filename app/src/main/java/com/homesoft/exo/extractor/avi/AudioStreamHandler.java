package com.homesoft.exo.extractor.avi;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.extractor.TrackOutput;

import java.util.Arrays;

public class AudioStreamHandler extends StreamHandler {
    long[] times = new long[0];

    /**
     * Current time in the stream
     */
    protected long timeUs;

    private long calcTimeUs(long streamPosition) {
        return durationUs * streamPosition / chunkIndex.getSize();
    }

    AudioStreamHandler(int id, long durationUs, @NonNull TrackOutput trackOutput) {
        super(id, TYPE_AUDIO, durationUs, trackOutput);
    }

    protected void advanceTime(int sampleSize) {
        timeUs += calcTimeUs(sampleSize);
    }

    @Override
    protected void sendMetadata(int size) {
        if (size > 0) {
            //System.out.println("AudioStream: " + getId() + " Us: " + getTimeUs() + " size: " + size);
            trackOutput.sampleMetadata(
                    getTimeUs(), C.BUFFER_FLAG_KEY_FRAME, size, 0, null);
        }
        advanceTime(size);
    }

    private void setSeekFrames(int[] seekFrameIndices) {
        setSeekPointSize(seekFrameIndices.length);
        final int chunks = chunkIndex.getCount();

        int k = 0;
        long streamBytes = 0;
        for (int c=0;c<chunks;c++) {
            if (seekFrameIndices[k] == c) {
                positions[k] = chunkIndex.getChunkPosition(c);
                times[k] = calcTimeUs(streamBytes);
                k++;
                if (k == positions.length) {
                    //We have moved beyond this streams length
                    break;
                }
            }
            streamBytes += chunkIndex.getChunkSize(c);
        }
        chunkIndex.release();
    }

    @Override
    public long[] setSeekStream() {
        setSeekFrames(chunkIndex.getChunkSubset(durationUs, 3));
        return positions;
    }

    public void setSeekFrames(long[] positions) {
        setSeekFrames(chunkIndex.getIndices(positions));
    }

    void setDurationUs(long durationUs) {
        this.durationUs = durationUs;
    }

    @Override
    public long getTimeUs() {
        return timeUs;
    }

    @Override
    public void seekPosition(long position) {
        final int seekIndex = getSeekIndex(position);
        timeUs = times[seekIndex];
    }

    @Override
    protected void setSeekPointSize(int seekPointCount) {
        super.setSeekPointSize(seekPointCount);
        times = new long[seekPointCount];
    }

    @Override
    public int getTimeUsSeekIndex(long timeUs) {
        return Arrays.binarySearch(times, timeUs);
    }

    @Override
    public long getTimeUs(int seekIndex) {
        return times[seekIndex];
    }
}
