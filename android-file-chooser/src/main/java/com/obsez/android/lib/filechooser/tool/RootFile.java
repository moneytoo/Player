package com.obsez.android.lib.filechooser.tool;

import java.io.File;

public final class RootFile extends File {
    private String name;

    public RootFile(String path, String name) {
        super(path);
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public long lastModified() {
        return 0L;
    }
}
