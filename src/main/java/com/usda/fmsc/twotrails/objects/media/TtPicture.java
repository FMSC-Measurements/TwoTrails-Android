package com.usda.fmsc.twotrails.objects.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtPicture extends TtMedia implements IOrientation {
    private Double _Azimuth;
    private Double _Pitch;
    private Double _Roll;

    private Bitmap image;

    public TtPicture() {

    }

    public TtPicture(String name, String filePath, DateTime timeCreated, String pointCN) {
        this(name, filePath, StringEx.Empty, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPicture(String name, String filePath, String comment, DateTime timeCreated, String pointCN) {
        this(name, filePath, comment, timeCreated, pointCN, 0, 0, 0);
    }

    public TtPicture(String name, String filePath, String comment, DateTime timeCreated, String pointCN, double azimuth, double pitch, double roll) {
        super(name, filePath, comment, timeCreated, pointCN);

        _Azimuth = azimuth;
        _Pitch = pitch;
        _Roll = roll;
    }

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

//    public void loadImage() {
//        if (FileUtils.fileExists(getFilePath())) {
//            image = BitmapFactory.decodeFile(getFilePath());
//        } else {
//            throw new RuntimeException("Image Path Not Found");
//        }
//    }
//
//    public Bitmap getImage() {
//        if (image == null)
//            loadImage();
//
//        if (image.isRecycled()) {
//            loadImage();
//            return getImage();
//        }
//
//        return Bitmap.createBitmap(image);
//    }
//
//
//    public void getImageAsync(final AsyncImageListener listener){
//        ImageLoader.getInstance().loadImage("file://" + getFilePath(), new SimpleImageLoadingListener() {
//            @Override
//            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                image = Bitmap.createBitmap(loadedImage);
//                listener.imageLoaded(TtPicture.this);
//            }
//
//            @Override
//            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
//                Log.e(Consts.LOG_TAG, failReason.getCause().getMessage());
//            }
//
//            @Override
//            public void onLoadingCancelled(String imageUri, View view) {
//                Log.v(Consts.LOG_TAG, "canceled");
//            }
//        });
//    }
//
//    public interface AsyncImageListener {
//        void imageLoaded(TtPicture picture);
//    }
}
