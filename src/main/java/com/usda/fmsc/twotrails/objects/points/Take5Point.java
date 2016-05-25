package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.OpType;

public class Take5Point extends GpsPoint {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Take5Point(source);
        }

        @Override
        public Take5Point[] newArray(int size) {
            return new Take5Point[size];
        }
    };


    public Take5Point() {
        super();
    }

    public Take5Point(Parcel source) {
        super(source);
    }


    public Take5Point(TtPoint p) {
        super(p);
    }

    public OpType getOp() {
        return OpType.Take5;
    }
}
