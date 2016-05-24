package com.usda.fmsc.twotrails.units;

public enum PictureType {
    Regular(0),
    Panorama(1),
    PhotoSphere(2);

    private final int value;

    PictureType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PictureType parse(int id) {
        PictureType[] dists = values();
        if(dists.length > id && id > -1)
            return dists[id];
        throw new IllegalArgumentException("Invalid PictureType id: " + id);
    }

    public static PictureType parse(String value) {
        switch(value.toLowerCase()) {
            case "regular": return Regular;
            case "panorama":
            case "pano": return Panorama;
            case "photosphere":
            case "sphere": return PhotoSphere;
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case Regular: return "Regular";
            case Panorama: return "Panorama";
            case PhotoSphere: return "PhotoSphere";
            default: throw new IllegalArgumentException();
        }
    }
}