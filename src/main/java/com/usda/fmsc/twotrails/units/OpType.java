package com.usda.fmsc.twotrails.units;

public enum OpType {
    GPS(0),
    Take5(6),
    Traverse(1),
    SideShot(4),
    Quondam(3),
    Walk(5),
    WayPoint(2);

    private final int value;

    OpType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OpType parse(int id) {
        OpType[] ops = values();
        if(ops.length > id && id > -1)
        {
            for (OpType o : ops)
            {
                if (o.getValue() == id)
                    return o;
            }
        }
        throw new IllegalArgumentException("Invalid OpType id: " + id);
    }

    public static OpType parse(String value) {
        switch(value.toLowerCase()) {
            case "gps": return GPS;
            case "trav":
            case "traverse": return Traverse;
            case "way":
            case "waypoint":
            case "way point": return WayPoint;
            case "qndm":
            case "quondam": return Quondam;
            case "ss":
            case "sideshot":
            case "side shot": return SideShot;
            case "walk": return Walk;
            case "take5":
            case "take 5": return Take5;
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case GPS: return "GPS";
            case Traverse: return "Traverse";
            case WayPoint: return "WayPoint";
            case Quondam: return "Quondam";
            case SideShot: return "SideShot";
            case Walk: return "Walk";
            case Take5: return "Take 5";
            default: throw new IllegalArgumentException();
        }
    }

    public boolean isGpsType() {
        return this == GPS || this == Take5 || this == Walk || this == WayPoint;
    }

    public boolean isTravType() {
        return this == Traverse || this == SideShot;
    }
}
