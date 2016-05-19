package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class SideShotPoint extends TravPoint {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new SideShotPoint(source);
        }

        @Override
        public SideShotPoint[] newArray(int size) {
            return new SideShotPoint[size];
        }
    };


    public SideShotPoint() {
        super();
        _Op = OpType.SideShot;
    }

    public SideShotPoint(Parcel source) {
        super(source);
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
