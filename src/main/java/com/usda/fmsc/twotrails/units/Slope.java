package com.usda.fmsc.twotrails.units;

public enum Slope {
    Percent(0),
    Degrees(1);

    private final int value;

    private Slope(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Slope parse(int id) {
        Slope[] dts = values();
        if(dts.length > id && id > -1)
            return dts[id];
        throw new IllegalArgumentException("Invalid Slope id: " + id);
    }

    public static Slope parse(String value) {
        switch (value.toLowerCase()) {
            case "percent":
            case "0":
            case "p":
            case "%":
                return Percent;
            case "degrees":
            case "1":
            case "d":
            case "deg":
                return Degrees;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case Percent: return "Percent";
            case Degrees: return "Degrees";
            default: throw new IllegalArgumentException();
        }
    }


    public String toStringAbv() {
        switch(this) {
            case Percent: return "%";
            case Degrees: return "\u00B0";
            default: throw new IllegalArgumentException();
        }
    }
}