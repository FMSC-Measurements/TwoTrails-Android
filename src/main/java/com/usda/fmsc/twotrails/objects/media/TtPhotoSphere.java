package com.usda.fmsc.twotrails.objects.media;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPhotoSphere extends TtImage {
    public static final Parcelable.Creator<TtPhotoSphere> CREATOR = new Parcelable.Creator<TtPhotoSphere>() {
        @Override
        public TtPhotoSphere createFromParcel(Parcel source) {
            return new TtPhotoSphere(source);
        }

        @Override
        public TtPhotoSphere[] newArray(int size) {
            return new TtPhotoSphere[size];
        }
    };


    public TtPhotoSphere() {
        super();
    }

    public TtPhotoSphere(Parcel source) {
        super(source);
    }

    public TtPhotoSphere(String name, String filename, DateTime timeCreated, String pointCN, boolean isExternal) {
        this(name, filename, StringEx.Empty, timeCreated, pointCN, isExternal, null, null, null);
    }

    public TtPhotoSphere(String name, String filename, String comment, DateTime timeCreated, String pointCN, boolean isExternal) {
        this(name, filename, comment, timeCreated, pointCN, isExternal, null, null, null);
    }

    public TtPhotoSphere(String name, String filename, String comment, DateTime timeCreated, String pointCN, boolean isExternal, Float azimuth, Float pitch, Float roll) {
        super(name, filename, comment, timeCreated, pointCN, isExternal, azimuth, pitch, roll);
    }

    public TtPhotoSphere(TtPhotoSphere photoSphere) {
        super(photoSphere);
    }

    @Override
    public PictureType getPictureType() {
        return PictureType.PhotoSphere;
    }
}
