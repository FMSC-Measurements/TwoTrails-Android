package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units.OpType;

import java.io.Serializable;


public class WayPoint extends GpsPoint implements Serializable {
    public WayPoint() {
        super();
        _Op = OpType.WayPoint;
    }

    public WayPoint(WayPoint p) {
        super(p);
        _Op = OpType.WayPoint;
    }

    public WayPoint(TtPoint p) {
        super(p);
        _Op = OpType.WayPoint;
    }
}
