package com.usda.fmsc.twotrails.objects.media;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPanorama extends TtImage {
    public static final Parcelable.Creator<TtPanorama> CREATOR = new Parcelable.Creator<TtPanorama>() {
        @Override
        public TtPanorama createFromParcel(Parcel source) {
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

    public TtPanorama(String name, String filename, DateTime timeCreated, String pointCN, boolean isExternal) {
        this(name, filename, StringEx.Empty, timeCreated, pointCN, isExternal, null, null, null);
    }

    public TtPanorama(String name, String filename, String comment, DateTime timeCreated, String pointCN, boolean isExternal) {
        this(name, filename, comment, timeCreated, pointCN, isExternal, null, null, null);
    }

    public TtPanorama(String name, String filename, String comment, DateTime timeCreated, String pointCN, boolean isExternal, Float azimuth, Float pitch, Float roll) {
        super(name, filename, comment, timeCreated, pointCN, isExternal, azimuth, pitch, roll);
    }

    public TtPanorama(TtPanorama panorama) {
        super(panorama);
    }

    @Override
    public PictureType getPictureType() {
        return PictureType.Panorama;
    }
}
