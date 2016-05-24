package com.usda.fmsc.twotrails.units;

public enum MediaType {
    Picture(0),
    Video(1);

    private final int value;

    MediaType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MediaType parse(int id) {
        MediaType[] dists = values();
        if(dists.length > id && id > -1)
            return dists[id];
        throw new IllegalArgumentException("Invalid MediaType id: " + id);
    }

    public static MediaType parse(String value) {
        switch(value.toLowerCase()) {
            case "picture": return Picture;
            case "video": return Video;
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case Picture: return "Picture";
            case Video: return "Video";
            default: throw new IllegalArgumentException();
        }
    }
}
