package com.usda.fmsc.twotrails.units;

public enum MapType {
    None(0),
    Google(1),
    ArcGIS(2),;

    private final int value;

    MapType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MapType parse(int id) {
        MapType[] types = values();
        if(types.length > id && id > -1)
            return types[id];
        throw new IllegalArgumentException("Invalid MapType id: " + id);
    }

    @Override
    public String toString() {
        switch (this) {
            case Google: return "Google";
            case ArcGIS: return "ArcGIS";
            case None:
            default: return "None";
        }
    }
}
