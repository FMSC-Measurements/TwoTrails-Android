package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units.OpType;

public class SideShotPoint extends TravPoint {
    public SideShotPoint() {
        super();
        _Op = OpType.SideShot;
    }

    public SideShotPoint(SideShotPoint p) {
        super(p);
        _Op = OpType.SideShot;
    }

    public SideShotPoint(TtPoint p) {
        super(p);
        _Op = OpType.SideShot;
    }

}
