package com.usda.fmsc.twotrails.objects.media;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPanorama extends TtPicture {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtPanorama(source);
        }

        @Override
        public TtPanorama[] newArray(int size) {
            return new TtPanorama[size];
        }
    };


    public TtPanorama() {
        super();
    }

    public TtPanorama(Parcel source) {
        super(source);
    }

    public TtPanorama(String name, String filePath, DateTime timeCreated, String pointCN) {
        this(name, filePath, StringEx.Empty, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPanorama(String name, String filePath, String comment, DateTime timeCreated, String pointCN) {
        this(name, filePath, comment, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPanorama(String name, String filePath, String comment, DateTime timeCreated, String pointCN, double azimuth, double pitch, double roll) {
        super(name, filePath, comment, timeCreated, pointCN, azimuth, pitch, roll);
    }

    public TtPanorama(TtPanorama panorama) {
        super(panorama);
    }

    @Override
    public PictureType getPictureType() {
        return PictureType.Panorama;
    }
}
