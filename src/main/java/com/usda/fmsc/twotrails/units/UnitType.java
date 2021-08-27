package com.usda.fmsc.twotrails.units;

public enum UnitType {
    Polygon(0),
    Exclusion(1),
    PolyLine(2),
    LogFace(3),
    General(4);

    private final int value;

    UnitType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UnitType parse(int id) {
        UnitType[] units = values();
        if(units.length > id && id > -1)
        {
            for (UnitType u : units)
            {
                if (u.getValue() == id)
                    return u;
            }
        }
        throw new IllegalArgumentException("Invalid Unit id: " + id);
    }

    public static UnitType parse(String value) {
        switch(value.toLowerCase()) {
            case "polygon": return Polygon;
            case "exclusion": return Exclusion;
            case "polyline": return PolyLine;
            case "logface": return LogFace;
            case "general": return General;
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case Polygon: return "Polygon";
            case Exclusion: return "Exclusion";
            case PolyLine: return "PolyLine";
            case LogFace: return "LogFace";
            case General: return "General";
            default: throw new IllegalArgumentException();
        }
    }
}
