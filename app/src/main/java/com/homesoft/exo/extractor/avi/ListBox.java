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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.media3.extractor.ExtractorInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class ListBox extends BoxReader implements Box {
    final private int type;
    private final ArrayList<Box> list = new ArrayList<>();
    public static final int LIST = 0x5453494c; // LIST
    public static final int TYPE_HDRL = 0x6c726468; // hdrl - Header List
    public static final int TYPE_STRL = 0x6c727473; // strl - Stream List
    public static final int TYPE_ODML = 0x6C6D646F; // odlm - OpenDML List
    private static final int[] SUPPORTED_TYPES = {TYPE_HDRL, TYPE_STRL, TYPE_ODML};

    static {
        Arrays.sort(SUPPORTED_TYPES);
    }

    private final Deque<IReader> readerStack;
    public ListBox(long position, int size, int type, @NonNull Deque<IReader> readerStack) {
        super(position, size);
        this.type = type;
        this.readerStack = readerStack;
    }

    @Override
    public int getChunkId() {
        return LIST;
    }

    @VisibleForTesting
    void add(Box box) {
        list.add(box);
    }

    @Override
    protected boolean isComplete() {
        if (super.isComplete()) {
            for (Box box : list) {
                if (box instanceof BoxReader && !((BoxReader) box).isComplete()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean read(@NonNull ExtractorInput input) throws IOException {
        if (isComplete()) {
            return true;
        }
        final int chunkId = headerPeeker.peak(input, CHUNK_HEADER_SIZE);
        final int size = headerPeeker.getSize();
        switch (chunkId) {
            case AviHeaderBox.AVIH:
                add(new AviHeaderBox(getByteBuffer(input, size)));
                break;
            case StreamHeaderBox.STRH:
                add(new StreamHeaderBox(getByteBuffer(input, size)));
                break;
            case StreamFormatBox.STRF:
                add(new StreamFormatBox(getByteBuffer(input, size)));
                break;
            case StreamNameBox.STRN:
                add(new StreamNameBox(getByteBuffer(input, size)));
                break;
            case IndexBox.INDX:
                add(new IndexBox(getByteBuffer(input, size)));
                break;
            case ExtendedAviHeader.DMLH:
                add(new ExtendedAviHeader(getByteBuffer(input, size)));
                break;
            case LIST:
                final int type = headerPeeker.peakType(input);
                if (Arrays.binarySearch(SUPPORTED_TYPES, type) >= 0) {
                    ListBox listBox = new ListBox(position + PARENT_HEADER_SIZE, size - 4, type, readerStack);
                    add(listBox);
                    readerStack.push(listBox);
                }
                break;
        }
        return advancePosition();
    }

    public List<Box> getChildren() {
        return Collections.unmodifiableList(list);
    }

    @Nullable
    public <T extends Box> T getChild(Class<T> type) {
        for (Box box : list) {
            if (box.getClass() == type) {
                return type.cast(box);
            }
        }
        return null;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ListBox{" +
                "type=" + AviExtractor.toString(type) +
                ", position=" + position +
                ", list=" + list +
                '}';
    }
}