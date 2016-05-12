package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;

public class TtPicture extends TtMedia implements IOrientation {
    private Double _Azimuth;
    private Double _Pitch;
    private Double _Roll;

    @Override
    public final MediaType getMediaType() {
        return MediaType.Picture;
    }

    public PictureType getPictureType() {
        return PictureType.Regular;
    }

    @Override
    public Double getAzimuth() {
        return _Azimuth;
    }

    public void setAzimuth(Double azimuth) {
        _Azimuth = azimuth;
    }

    @Override
    public Double getPitch() {
        return _Pitch;
    }

    public void setPitch(Double pitch) {
        _Pitch = pitch;
    }

    @Override
    public Double getRoll() {
        return _Roll;
    }

    public void setRoll(Double roll) {
        _Roll = roll;
    }

    //get picture details (size, shutter, flash, aperature, etc..)
}
