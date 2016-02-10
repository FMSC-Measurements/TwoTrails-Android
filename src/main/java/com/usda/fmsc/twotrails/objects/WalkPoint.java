package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units.OpType;

import java.io.Serializable;


public class WalkPoint extends GpsPoint implements Serializable {
    public WalkPoint() {
        super();
        _Op = OpType.Walk;
    }

    public WalkPoint(WalkPoint p) {
        super(p);
        _Op = OpType.Walk;
    }

    public WalkPoint(TtPoint p) {
        super(p);
        _Op = OpType.Walk;
    }
}
