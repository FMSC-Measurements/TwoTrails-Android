package com.usda.fmsc.twotrails.units;

public enum Datum {
    NAD83(0),
    WGS84(1),
    ITRF(2),
    NAD27(3),
    Local(4);

    private final int value;

    private Datum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Datum parse(int id) {
        Datum[] dts = values();
        if(dts.length > id && id > -1)
            return dts[id];
        throw new IllegalArgumentException("Invalid Datum id: " + id);
    }

    @Override
    public String toString() {
        switch(this) {
            case NAD83: return "NAD83";
            case WGS84: return "WGS84";
            case ITRF: return "ITRF";
            case NAD27: return "NAD27";
            case Local: return "Local";
            default: throw new IllegalArgumentException();
        }
    }
}
