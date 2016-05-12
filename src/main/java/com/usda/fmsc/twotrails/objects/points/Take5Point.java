package com.usda.fmsc.twotrails.objects.points;

import com.usda.fmsc.twotrails.units.OpType;

import java.io.Serializable;


public class Take5Point extends GpsPoint implements Serializable {
    public Take5Point() {
        super();
        _Op = OpType.Take5;
    }

    public Take5Point(Take5Point p) {
        super(p);
        _Op = OpType.Take5;
    }

    public Take5Point(TtPoint p) {
        super(p);
        _Op = OpType.Take5;
    }
}
