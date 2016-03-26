package com.usda.fmsc.twotrails.objects;

public class PolygonDrawOptions {
    public enum GraphicCode {
        VISIBLE,
        ADJBND,
        UNADJBND,
        ADJBNDPTS,
        UNADJBNDPTS,
        ADJBNDCLOSE,
        UNADJBNDCLOSE,
        ADJNAV,
        UNADJNAV,
        ADJNAVPTS,
        UNADJNAVPTS,
        ADJMISCPTS,
        UNADJMISCPTS,
        WAYPTS
    }

//    public static final String VISIBLE = "Visible";
//    public static final String ADJBND = "AdjBnd";
//    public static final String UNADJBND = "UnadjBnd";
//    public static final String ADJBNDPTS = "AdjBndPts";
//    public static final String UNADJBNDPTS = "UnadjBndPts";
//    public static final String ADJBNDCLOSE = "AdjBndClose";
//    public static final String UNADJBNDCLOSE = "UnadjBndClose";
//    public static final String ADJNAV = "AdjNav";
//    public static final String UNADJNAV = "UnadjNav";
//    public static final String ADJNAVPTS = "AdjNavPts";
//    public static final String UNADJNAVPTS = "UnadjNavPts";
//    public static final String ADJMISCPTS = "AdjMiscPts";
//    public static final String UNADJMISCPTS = "UnadjMiscPts";
//    public static final String WAYPTS = "WayPts";

    public boolean Visible;
    public boolean AdjBnd, UnadjBnd, AdjBndPts, UnadjBndPts, AdjBndClose, UnadjBndClose;
    public boolean AdjNav, UnadjNav, AdjNavPts, UnadjNavPts;
    public boolean AdjMiscPts, UnadjMiscPts, WayPts;


    public PolygonDrawOptions() {
        Visible = true;
        AdjBnd = true;
        AdjBndClose = true;
        AdjBndPts = true;
    }

    public PolygonDrawOptions(PolygonDrawOptions options) {
        Visible = options.Visible;

        AdjBnd = options.AdjBnd;
        UnadjBnd = options.UnadjBnd;
        AdjBndPts = options.AdjBndPts;
        UnadjBndPts = options.UnadjBndPts;
        AdjBndClose = options.AdjBndClose;
        UnadjBndClose = options.UnadjBndClose;

        AdjNav = options.AdjNav;
        UnadjNav = options.UnadjNav;
        AdjNavPts = options.AdjNavPts;
        UnadjNavPts = options.UnadjNavPts;

        AdjMiscPts = options.AdjMiscPts;
        UnadjMiscPts = options.UnadjMiscPts;
        WayPts = options.WayPts;
    }

    public boolean getValue(GraphicCode code) {
        switch (code) {
            case VISIBLE: return Visible;
            case ADJBND: return AdjBnd;
            case UNADJBND: return UnadjBnd;
            case ADJBNDPTS: return AdjBndPts;
            case UNADJBNDPTS: return UnadjBndPts;
            case ADJBNDCLOSE: return AdjBndClose;
            case UNADJBNDCLOSE: return UnadjBndClose;
            case ADJNAV: return AdjNav;
            case UNADJNAV: return UnadjNav;
            case ADJNAVPTS: return AdjNavPts;
            case UNADJNAVPTS: return UnadjNavPts;
            case ADJMISCPTS: return AdjMiscPts;
            case UNADJMISCPTS: return UnadjMiscPts;
            case WAYPTS: return WayPts;
        }

        return false;
    }

    public void setValue(GraphicCode code, boolean value) {
        switch (code) {
            case VISIBLE:
                Visible = value;
                break;
            case ADJBND:
                AdjBnd = value;
                break;
            case UNADJBND:
                UnadjBnd = value;
                break;
            case ADJBNDPTS:
                AdjBndPts = value;
                break;
            case UNADJBNDPTS:
                UnadjBndPts = value;
                break;
            case ADJBNDCLOSE:
                AdjBndClose = value;
                break;
            case UNADJBNDCLOSE:
                UnadjBndClose = value;
                break;
            case ADJNAV:
                AdjNav = value;
                break;
            case UNADJNAV:
                UnadjNav = value;
                break;
            case ADJNAVPTS:
                AdjNavPts = value;
                break;
            case UNADJNAVPTS:
                UnadjNavPts = value;
                break;
            case ADJMISCPTS:
                AdjMiscPts = value;
                break;
            case UNADJMISCPTS:
                UnadjMiscPts = value;
                break;
            case WAYPTS:
                WayPts = value;
                break;
        }
    }

    public interface Listener {
        void onOptionChanged(GraphicCode code, boolean value);
    }
}
