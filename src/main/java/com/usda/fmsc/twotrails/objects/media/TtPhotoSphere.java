package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPhotoSphere extends TtPicture {

    public TtPhotoSphere() {

    }

    public TtPhotoSphere(String name, String filePath, DateTime timeCreated, String pointCN) {
        this(name, filePath, StringEx.Empty, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPhotoSphere(String name, String filePath, String comment, DateTime timeCreated, String pointCN) {
        this(name, filePath, comment, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPhotoSphere(String name, String filePath, String comment, DateTime timeCreated, String pointCN, double azimuth, double pitch, double roll) {
        super(name, filePath, comment, timeCreated, pointCN, azimuth, pitch, roll);
    }

    public TtPhotoSphere(TtPhotoSphere photoSphere) {
        super(photoSphere);
    }

    @Override
    public PictureType getPictureType() {
        return PictureType.PhotoSphere;
    }
}
