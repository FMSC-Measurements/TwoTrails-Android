package com.usda.fmsc.twotrails.units;

public enum DeclinationType {
    MagDec(0),
    DeedRot(1);

    private final int value;

    DeclinationType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DeclinationType parse(int id) {
        DeclinationType[] dts = values();
        if(dts.length > id && id > -1)
            return dts[id];
        throw new IllegalArgumentException("Invalid DeclinationType id: " + id);
    }

    @Override
    public String toString() {
        switch(this) {
            case MagDec: return "MagDec";
            case DeedRot: return "DeedRot";
            default: throw new IllegalArgumentException();
        }
    }
}
