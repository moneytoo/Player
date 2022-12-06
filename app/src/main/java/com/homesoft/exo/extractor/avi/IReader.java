package com.homesoft.exo.extractor.avi;

import androidx.annotation.NonNull;
import androidx.media3.extractor.ExtractorInput;

import java.io.IOException;

public interface IReader {
    long getPosition();
    /**
     * @return true if the reader is complete
     *
     */
    boolean read(@NonNull ExtractorInput input) throws IOException;
}