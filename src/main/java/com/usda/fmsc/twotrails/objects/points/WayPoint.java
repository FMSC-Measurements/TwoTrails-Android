package com.usda.fmsc.twotrails.objects.points;

import com.usda.fmsc.twotrails.units.OpType;

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
