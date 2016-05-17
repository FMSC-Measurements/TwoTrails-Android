package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPanorama extends TtPicture {

    public TtPanorama() {

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
