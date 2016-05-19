package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class WalkPoint extends GpsPoint {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new WalkPoint(source);
        }

        @Override
        public WalkPoint[] newArray(int size) {
            return new WalkPoint[size];
        }
    };


    public WalkPoint() {
        super();
        _Op = OpType.Walk;
    }

    public WalkPoint(Parcel source) {
        super(source);
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
