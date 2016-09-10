package com.usda.fmsc.twotrails.objects;

public class DataActivityType {
    public static final int Opened = 0;
    public static final int ModifiedProject = 1;
    public static final int InsertedPoints = 2;
    public static final int ModifiedPoints = 4;
    public static final int DeletedPoints = 8;
    public static final int InsertedPolygons = 16;
    public static final int ModifiedPolygons = 32;
    public static final int DeletedPolygons = 64;
    public static final int InsertedMetadata = 128;
    public static final int ModifiedMetadata = 256;
    public static final int DeletedMetadata = 512;
    public static final int InsertedGroups = 1024;
    public static final int ModifiedGroups = 2048;
    public static final int DeletedGroups = 4096;

    private int value;

    public DataActivityType() {
        this.value = Opened;
    }

    public DataActivityType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setFlag(int value) {
        this.value |= value;
    }

    public void removeFlag(int value) {
        this.value &= ~value;
    }

}
