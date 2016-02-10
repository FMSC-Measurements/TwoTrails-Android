package com.usda.fmsc.twotrails;

public class Units {

    public enum OpType {
        GPS(0),
        Take5(6),
        Traverse(1),
        SideShot(4),
        Quondam(3),
        Walk(5),
        WayPoint(2);

        private final int value;

        private OpType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OpType parse(int id) {
            OpType[] dists = values();
            if(dists.length > id && id > -1)
                return dists[id];
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

    public enum Dist {
        FeetTenths(0),
        FeetInches(1),
        Meters(2),
        Chains(3),
        Yards(4);

        private final int value;

        private Dist(int value) {
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
    }


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

    public enum MapProjections {
        Meters,
        Feet
    }

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

    public enum DeclinationType {
        MagDec(0),
        DeedRot(1);

        private final int value;

        private DeclinationType(int value) {
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

    public enum TravAdjustType {
        Magnetic,
        DeedRotation
    }


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



    public enum MapTracking {
        NONE(0),
        FOLLOW(1),
        POLY_BOUNDS(2),
        COMPLETE_BOUNDS(3);

        private final int value;

        private MapTracking(int value) {
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
}
