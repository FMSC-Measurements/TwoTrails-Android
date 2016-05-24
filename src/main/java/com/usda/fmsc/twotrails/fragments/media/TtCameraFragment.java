package com.usda.fmsc.twotrails.fragments.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.usda.fmsc.android.fragments.CameraFragment;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtPanorama;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.utilities.FileUtils;

import org.joda.time.DateTime;

@TargetApi(21)
public class TtCameraFragment extends CameraFragment {
    private static final String POINT_CN = "PointCN";

    private TtCameraListener listener;
    private DeviceOrientationEx deviceOrientationEx;


    private TtImage ttImage;
    private DeviceOrientationEx.Orientation orientation;
    private String pointCN;
    private DateTime captureTime;
    private int width, height;


    public static TtCameraFragment newInstance(String pointCN) {
        TtCameraFragment fragment = new TtCameraFragment();
        Bundle args = new Bundle();
        args.putString(POINT_CN, pointCN);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceOrientationEx = new DeviceOrientationEx(getContext(), 3);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(POINT_CN)) {
            pointCN = bundle.getString(POINT_CN);
        } else {
            throw new IllegalArgumentException("Requires Point CN");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        deviceOrientationEx.resume();
    }

    @Override
    public void onPause() {
        super.onPause();

        deviceOrientationEx.pause();
    }

    @Override
    protected void onImageSaved(String filePath) {
        super.onImageSaved(filePath);

        String name = FileUtils.getFileNameWoType(filePath);

        PictureType type  = PictureType.Regular;

        if (width / height > 1 || width / height > 1) {
            type = PictureType.Panorama;
        }

        if (type == PictureType.Panorama) {
            ttImage = new TtPanorama(name, filePath, null, captureTime, pointCN, orientation.getAzimuth(), orientation.getPitch(), orientation.getRoll());
        } else {
            ttImage = new TtImage(name, filePath, null, captureTime, pointCN, orientation.getAzimuth(), orientation.getPitch(), orientation.getRoll());
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
        orientation = deviceOrientationEx.getOrientation();
        captureTime = DateTime.now();
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

    @Override
    protected String getImageSaveDir() {
        return Global.getTtMediaDir();
    }

    public TtImage getLastTtImage() {
        return ttImage;
    }

    public interface TtCameraListener {
        void onTtImageReady(TtImage image);
    }
}
