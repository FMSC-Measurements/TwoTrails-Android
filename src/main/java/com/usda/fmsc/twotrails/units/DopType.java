package com.usda.fmsc.twotrails.units;

public enum DopType {
    HDOP(0),
    PDOP(1);

    private final int value;

    private DopType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DopType parse(int id) {
        DopType[] dts = values();
        if(dts.length > id && id > -1)
            return dts[id];
        throw new IllegalArgumentException("Invalid DopType id: " + id);
    }

    public static DopType parse(String value) {
        switch(value.toLowerCase()) {
            case "hdop":
            case "0": return HDOP;
            case "pdop":
            case "1": return PDOP;
            default: throw new IllegalArgumentException("Invalid DopType Name: " + value);
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case HDOP: return "HDOP";
            case PDOP: return "PDOP";
            default: throw new IllegalArgumentException();
        }
    }
}
