package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class WayPoint extends GpsPoint {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new WayPoint(source);
        }

        @Override
        public WayPoint[] newArray(int size) {
            return new WayPoint[size];
        }
    };

    public WayPoint() {
        super();
    }

    public WayPoint(Parcel source) {
        super(source);
    }

    public WayPoint(WayPoint p) {
        super(p);
    }

    public WayPoint(TtPoint p) {
        super(p);
    }

    public OpType getOp() {
        return OpType.WayPoint;
    }
}
