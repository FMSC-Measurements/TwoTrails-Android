package com.usda.fmsc.twotrails.units;

public enum MapTracking {
    NONE(0),
    FOLLOW(1),
    POLY_BOUNDS(2),
    COMPLETE_BOUNDS(3);

    private final int value;

    MapTracking(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MapTracking parse(int id) {
        MapTracking[] dts = values();
        if(dts.length > id && id > -1)
            return dts[id];
        throw new IllegalArgumentException("Invalid MapTracking id: " + id);
    }

    public static MapTracking parse(String value) {
        switch(value.toLowerCase()) {
            case "none":
            case "0": return NONE;
            case "follow":
            case "1": return FOLLOW;
            case "poly":
            case "poly_bound":
            case "poly_bounds":
            case "polybnd":
            case "polybnds":
            case "poly_bnd":
            case "poly_bnds":
            case "polygon boundary":
            case "2": return POLY_BOUNDS;
            case "complete":
            case "complete_bound":
            case "complete_bounds":
            case "completebnd":
            case "completebnds":
            case "complete_bnd":
            case "complete_bnds":
            case "complete boundary":
            case "complete_boundary":
            case "3": return COMPLETE_BOUNDS;
            default: throw new IllegalArgumentException("Invalid MapTracking Name: " + value);
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case NONE: return "None";
            case FOLLOW: return "Follow";
            case POLY_BOUNDS: return "Polygon Boundary";
            case COMPLETE_BOUNDS: return "Complete Boundary";
            default: throw new IllegalArgumentException();
        }
    }
}
