package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class SideShotPoint extends TravPoint {
    public static final Parcelable.Creator<SideShotPoint> CREATOR = new Parcelable.Creator<SideShotPoint>() {
        @Override
        public SideShotPoint createFromParcel(Parcel source) {
            return new SideShotPoint(source);
        }

        @Override
        public SideShotPoint[] newArray(int size) {
            return new SideShotPoint[size];
        }
    };


    public SideShotPoint() {
        super();
    }

    public SideShotPoint(Parcel source) {
        super(source);
    }

    public SideShotPoint(TtPoint p) {
        super(p);
    }


    public OpType getOp() {
        return OpType.SideShot;
    }
}
