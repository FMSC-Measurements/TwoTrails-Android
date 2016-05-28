package com.usda.fmsc.twotrails.units;

public enum Dist {
    FeetTenths(0),
    FeetInches(1),
    Meters(2),
    Chains(3),
    Yards(4);

    private final int value;

    Dist(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Dist parse(int id) {
        Dist[] dists = values();
        if(dists.length > id && id > -1)
            return dists[id];
        throw new IllegalArgumentException("Invalid Dist id: " + id);
    }

    public static Dist parse(String value) {
        switch (value.toLowerCase()) {
            case "feettenths":
            case "0":
            case "ftt":
                return FeetTenths;
            case "feetinches":
            case "1":
            case "fti":
                return FeetInches;
            case "meters":
            case "2":
            case "m":
                return Meters;
            case "chains":
            case "3":
            case "c":
            case "chn":
                return Chains;
            case "yards":
            case "4":
            case "y":
            case "yd":
            case "yard":
                return Yards;
            default: throw new IllegalArgumentException();


        }
    }

    @Override
    public String toString() {
        switch(this) {
            case FeetTenths: return "FeetTenths";
            case FeetInches: return "FeetInches";
            case Meters: return "Meters";
            case Chains: return "Chains";
            case Yards: return "Yards";
            default: throw new IllegalArgumentException();
        }
    }

    public String toStringAbv() {
        switch(this) {
            case FeetTenths: return "FtT";
            case FeetInches: return "FtI";
            case Meters: return "Mt";
            case Chains: return "Chn";
            case Yards: return "Yd";
            default: throw new IllegalArgumentException();
        }
    }

    public static Dist[] getUsedValues() {
        return new Dist[] {
                FeetTenths,
                Meters,
                Yards,
                Chains
        };
    }
}
