package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.PictureType;

public class TtPanorama extends TtPicture {

    @Override
    public PictureType getPictureType() {
        return PictureType.Panorama;
    }
}
