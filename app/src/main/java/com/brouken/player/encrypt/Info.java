package com.brouken.player.encrypt;


public final class Info {
    public long id = 0L;
    public long size = 0L;
    public int duration = 0;
    public int width = 0;
    public int height = 0;
    public int progress = 0;
    public String thumbUrl;

    public Info() {
    }

    public Info(long id, long size, int duration, int width, int height, int progress, String thumbUrl) {
        this.id = id;
        this.size = size;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.progress = progress;
        this.thumbUrl = thumbUrl;

    }

}


