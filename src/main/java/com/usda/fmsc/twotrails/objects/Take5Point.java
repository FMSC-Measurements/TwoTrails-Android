package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units.OpType;

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
