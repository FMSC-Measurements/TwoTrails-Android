package com.usda.fmsc.twotrails.objects;

public class PolyDrawOptions {
    public static final String VISIBLE = "Visible";
    public static final String ADJBND = "AdjBnd";
    public static final String UNADJBND = "UnadjBnd";
    public static final String ADJBNDPTS = "AdjBndPts";
    public static final String UNADJBNDPTS = "UnadjBndPts";
    public static final String ADJBNDCLOSE = "AdjBndClose";
    public static final String UNADJBNDCLOSE = "UnadjBndClose";
    public static final String ADJNAV = "AdjNav";
    public static final String UNADJNAV = "UnadjNav";
    public static final String ADJNAVPTS = "AdjNavPts";
    public static final String UNADJNAVPTS = "UnadjNavPts";
    public static final String ADJMISCPTS = "AdjMiscPts";
    public static final String UNADJMISCPTS = "UnadjMiscPts";
    public static final String WAYPTS = "WayPts";

    public boolean Visible;
    public boolean AdjBnd, UnadjBnd, AdjBndPts, UnadjBndPts, AdjBndClose, UnadjBndClose;
    public boolean AdjNav, UnadjNav, AdjNavPts, UnadjNavPts;
    public boolean AdjMiscPts, UnadjMiscPts, WayPts;


    public PolyDrawOptions() {
        Visible = true;
        AdjBnd = true;
        AdjBndClose = true;
        AdjBndPts = true;
    }

    public PolyDrawOptions(PolyDrawOptions options) {
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
}