package com.usda.fmsc.twotrails.objects.media;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtImage extends TtMedia implements TtOrientation {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtImage(source);
        }

        @Override
        public TtImage[] newArray(int size) {
            return new TtImage[size];
        }
    };

    private Float _Azimuth;
    private Float _Pitch;
    private Float _Roll;


    public TtImage() {
        super();
    }

    public TtImage(Parcel source) {
        super(source);

        _Azimuth = ParcelTools.readNFloat(source);
        _Pitch = ParcelTools.readNFloat(source);
        _Roll = ParcelTools.readNFloat(source);
    }

    public TtImage(String name, String filePath, DateTime timeCreated, String pointCN) {
        this(name, filePath, StringEx.Empty, timeCreated, pointCN, null, null, null);
    }

    public TtImage(String name, String filePath, String comment, DateTime timeCreated, String pointCN) {
        this(name, filePath, comment, timeCreated, pointCN, null, null, null);
    }

    public TtImage(String name, String filePath, String comment, DateTime timeCreated, String pointCN, Float azimuth, Float pitch, Float roll) {
        super(name, filePath, comment, timeCreated, pointCN);

        _Azimuth = azimuth;
        _Pitch = pitch;
        _Roll = roll;
    }

    public TtImage(TtImage picture) {
        super(picture);

        _Azimuth = picture._Azimuth;
        _Pitch = picture._Pitch;
        _Roll = picture._Roll;
    }

    @Override
    public final MediaType getMediaType() {
        return MediaType.Picture;
    }

    public PictureType getPictureType() {
        return PictureType.Regular;
    }

    @Override
    public Float getAzimuth() {
        return _Azimuth;
    }

    public void setAzimuth(Float azimuth) {
        _Azimuth = azimuth;
    }

    @Override
    public Float getPitch() {
        return _Pitch;
    }

    public void setPitch(Float pitch) {
        _Pitch = pitch;
    }

    @Override
    public Float getRoll() {
        return _Roll;
    }

    public void setRoll(Float roll) {
        _Roll = roll;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        ParcelTools.writeNFloat(dest, _Azimuth);
        ParcelTools.writeNFloat(dest, _Pitch);
        ParcelTools.writeNFloat(dest, _Roll);
    }
}
