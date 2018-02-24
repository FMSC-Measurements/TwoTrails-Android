package com.usda.fmsc.twotrails.objects;

public class DataActionType {
    public static final int None                = 0;
    public static final int ModifiedProject     = 1;
    public static final int InsertedPoints      = 1 << 1;
    public static final int ModifiedPoints      = 1 << 2;
    public static final int DeletedPoints       = 1 << 3;
    public static final int InsertedPolygons    = 1 << 4;
    public static final int ModifiedPolygons    = 1 << 5;
    public static final int DeletedPolygons     = 1 << 6;
    public static final int InsertedMetadata    = 1 << 7;
    public static final int ModifiedMetadata    = 1 << 8;
    public static final int DeletedMetadata     = 1 << 9;
    public static final int InsertedGroups      = 1 << 10;
    public static final int ModifiedGroups      = 1 << 11;
    public static final int DeletedGroups       = 1 << 12;
    public static final int InsertedMedia       = 1 << 13;
    public static final int ModifiedMedia       = 1 << 14;
    public static final int DeletedMedia        = 1 << 15;
    public static final int ManualPointCreation = 1 << 16;
    public static final int MovePoints          = 1 << 17;
    public static final int RetracePoints       = 1 << 18;
    public static final int ReindexPoints       = 1 << 19;
    public static final int ConvertPoints       = 1 << 20;
    public static final int ModifiedDataDictionary  = 1 << 21;
    public static final int DataImported        = 1 << 22;
            

    private int value;

    public DataActionType() {
        this.value = None;
    }

    public DataActionType(int value) {
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
