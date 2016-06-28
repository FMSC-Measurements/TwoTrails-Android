package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class WalkPoint extends GpsPoint {
    public static final Parcelable.Creator<WalkPoint> CREATOR = new Parcelable.Creator<WalkPoint>() {
        @Override
        public WalkPoint createFromParcel(Parcel source) {
            return new WalkPoint(source);
        }

        @Override
        public WalkPoint[] newArray(int size) {
            return new WalkPoint[size];
        }
    };


    public WalkPoint() {
        super();
    }

    public WalkPoint(Parcel source) {
        super(source);
    }

    public WalkPoint(TtPoint p) {
        super(p);
    }

    public OpType getOp() {
        return OpType.Walk;
    }
}
