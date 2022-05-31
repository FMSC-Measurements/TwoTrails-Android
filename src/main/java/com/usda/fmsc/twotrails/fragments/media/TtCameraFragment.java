package com.usda.fmsc.twotrails.fragments.media;

import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.fragments.CameraFragment;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtPanorama;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.io.File;

public class TtCameraFragment extends CameraFragment {
    private static final String POINT_CN = "PointCN";
    private static final String SAVE_IMAGE = "SaveImage";
    private static final String SAVE_IMAGE_URI = "SaveImageUri";

    private TtCameraListener listener;
    private DeviceOrientationEx deviceOrientationEx;


    private TtImage ttImage;
    private DeviceOrientationEx.Orientation orientation;
    private String pointCN;
    private DateTime captureTime;
    private int width, height;
    private boolean saveImage;
    private Uri saveImageUri;


    public static TtCameraFragment newInstance(String pointCN, Uri saveFilePath) {
        TtCameraFragment fragment = new TtCameraFragment();
        Bundle args = new Bundle();
        args.putString(POINT_CN, pointCN);
        args.putParcelable(SAVE_IMAGE_URI, saveFilePath);
        args.putBoolean(SAVE_IMAGE, true);
        fragment.setArguments(args);
        return fragment;
    }

    public static TtCameraFragment newInstanceWithOutSave() {
        TtCameraFragment fragment = new TtCameraFragment();
        Bundle args = new Bundle();
        args.putString(POINT_CN, StringEx.Empty);
        args.putBoolean(SAVE_IMAGE, false);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AndroidUtils.Device.isFullOrientationAvailable(getContext())) {
            deviceOrientationEx = new DeviceOrientationEx(getContext(), 3);
        }

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(POINT_CN)) {
            pointCN = bundle.getString(POINT_CN);
            saveImage = bundle.getBoolean(SAVE_IMAGE);

            saveImageUri = bundle.getParcelable(SAVE_IMAGE_URI);
        } else {
            throw new IllegalArgumentException("Requires Point CN");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (deviceOrientationEx != null) {
            deviceOrientationEx.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (deviceOrientationEx != null) {
            deviceOrientationEx.pause();
        }
    }

    @Override
    protected void takePicture() {
        if (saveImage) {
            super.takePicture();
        }
    }

    @Override
    protected File createImageFile() {
        return saveImageUri != null ? new File(saveImageUri.getPath()) : super.createImageFile();
    }

    @Override
    protected void onImageSaved(Uri filePath) {
        super.onImageSaved(filePath);

        String path = filePath.getPath();

        String fileName, name;

        if (path != null) {
            fileName = FileUtils.getFileName(filePath.getPath());
            name = FileUtils.getFileNameWoExt(fileName);
        } else {
            fileName = null;
            name = "Untitled Image";
        }

        PictureType type  = PictureType.Regular;

        if ((double)width / height > 1.5 || (double)width / height < 0.66) {
            type = PictureType.Panorama;
        }

        if (type == PictureType.Panorama) {
            ttImage = new TtPanorama(name, fileName, "", captureTime, pointCN, true, orientation.getRationalAzimuth(), orientation.getPitch(), orientation.getRoll());
        } else {
            ttImage = new TtImage(name, fileName, "", captureTime, pointCN, true, orientation.getRationalAzimuth(), orientation.getPitch(), orientation.getRoll());
        }

        if (listener != null) {
            listener.onTtImageReady(ttImage);
        }
    }

    @Override
    protected void onImageReady(Image image) {
        super.onImageReady(image);

        width = image.getWidth();
        height = image.getHeight();
        if (deviceOrientationEx != null) {
            orientation = deviceOrientationEx.getOrientation();
        } else {
            orientation = new DeviceOrientationEx.Orientation(0f, 0f, 0f);
        }

        captureTime = DateTime.now();
    }

    public DeviceOrientationEx.Orientation getOrientation() {
        return deviceOrientationEx != null ? deviceOrientationEx.getOrientation() : new DeviceOrientationEx.Orientation(0f, 0f, 0f);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (TtCameraListener)context;
        } catch (Exception ex) {
            //
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
    }

    public TtImage getLastTtImage() {
        return ttImage;
    }

    public interface TtCameraListener {
        void onTtImageReady(TtImage image);
    }
}
