package com.homesoft.exo.extractor;

import android.net.Uri;

import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.mp3.Mp3Extractor;

import com.homesoft.exo.extractor.avi.AviExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AviExtractorsFactory implements ExtractorsFactory {
    private final DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();

    @Override
    public Extractor[] createExtractors() {
        return patchExtractors(defaultExtractorsFactory.createExtractors());
    }

    @Override
    public Extractor[] createExtractors(Uri uri, Map<String, List<String>> responseHeaders) {
        return patchExtractors(defaultExtractorsFactory.createExtractors());
    }

    /**
     * Hack to work-around DefaultExtractorsFactory being final
     */
    private Extractor[] patchExtractors(Extractor[] extractors) {
        final ArrayList<Extractor> list = new ArrayList<>(Arrays.asList(extractors));
        final int aviIndex = findExtractor(list, androidx.media3.extractor.avi.AviExtractor.class);
        if (aviIndex != -1) {
            list.remove(aviIndex);
        }
        final int mp3Index = findExtractor(list, Mp3Extractor.class);
        if (mp3Index != -1) {
            //Mp3Extractor falsely sniff()s AVI files, so insert the AviExtractor before it
            // trhak.avi
            list.add(mp3Index, new AviExtractor());
        } else {
            list.add(new AviExtractor());
        }
        return list.toArray(new Extractor[0]);
    }

    private static int findExtractor(List<Extractor> list, Class<? extends Extractor> extractorClass) {
        for (int i=0;i<list.size();i++) {
            if (extractorClass.isInstance(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the underlying DefaultExtractorsFactory
     */
    public DefaultExtractorsFactory getDefaultExtractorsFactory() {
        return defaultExtractorsFactory;
    }
}
