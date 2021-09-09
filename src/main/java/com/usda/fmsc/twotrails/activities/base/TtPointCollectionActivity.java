package com.usda.fmsc.twotrails.activities.base;


import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.exifinterface.media.ExifInterface;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.GetDirectionActivity;
import com.usda.fmsc.twotrails.activities.TtCameraActivity;
import com.usda.fmsc.twotrails.activities.contracts.GetImages;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtPanorama;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.ParseEx;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class TtPointCollectionActivity extends TtProjectAdjusterActivity {
    private Uri _CapturedImageUri;
    private String _CapturedImagePointCN;

    //region Requests
    private final ActivityResultLauncher<String> requestImagesForResult = registerForActivityResult(new GetImages(), results -> {
        try {
            List<TtImage> images = getImagesFromUris(results, _CapturedImagePointCN);

            onImagesSelected(images);

            _CapturedImageUri = null;
            _CapturedImagePointCN = null;

            if (images.size() == 1) {
                askAndUpdateImageOrientation(images.get(0));
            }
        } catch (IOException e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PointsActivity:oAR:ADD_IMAGES");
            Toast.makeText(this, "Error adding Images, check log for details.", Toast.LENGTH_LONG).show();
        }
    });
    protected void pickImages() {
        requestImagesForResult.launch(null);
    }

    private final ActivityResultLauncher<Intent> updateImageOrientationForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent intent = result.getData();

        if (result.getResultCode() != RESULT_CANCELED) {
            if (intent != null && intent.hasExtra(Consts.Codes.Data.ORIENTATION)) {
                DeviceOrientationEx.Orientation orientation = intent.getParcelableExtra(Consts.Codes.Data.ORIENTATION);

                TtImage image = (TtImage)getCurrentMedia();

                if (image != null) {
                    image.setAzimuth(orientation.getRationalAzimuth());
                    image.setPitch(orientation.getPitch());
                    image.setRoll(orientation.getRoll());

                    onImageOrientationUpdated(image);
                }
            }
        }
    });
    protected void updateImageOrientation(TtImage image) {
        Intent intent = new Intent(this, GetDirectionActivity.class);

        if (image != null) {
            intent.putExtra(Consts.Codes.Data.ORIENTATION, new DeviceOrientationEx.Orientation(image.getAzimuth(), image.getPitch(), image.getRoll()));
            intent.putExtra(Consts.Codes.Data.TTIMAGE_CN, image.getCN());
        }

        updateImageOrientationForResult.launch(intent);
    }

    protected void askAndUpdateImageOrientation(TtImage image) {
        new AlertDialog.Builder(this)
                .setMessage("Would you like to update the orientation (Azimuth) to this image?")
                .setPositiveButton(R.string.str_yes, (dialog, which) -> updateImageOrientation(image))
                .setNegativeButton(R.string.str_no, null)
                .show();
    }



    private final ActivityResultLauncher<Uri> captureImageForResult = registerForActivityResult(new ActivityResultContracts.TakePicture(), picTaken -> {
        if (picTaken) {
            if (AndroidUtils.Files.fileExists(getTtAppCtx(), _CapturedImageUri)) {

                try {
                    onImageCaptured(createImageFromFile(_CapturedImageUri, _CapturedImagePointCN));
                } catch (IOException e) {
                    getTtAppCtx().getReport().writeError(e.getMessage(), "TtPointCollectionActivity:captureImageForResult");
                    Toast.makeText(this, "Failed to create Image.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Image file not found.", Toast.LENGTH_LONG).show();
            }
        }

        _CapturedImageUri = null;
        _CapturedImagePointCN = null;
    });
    private final ActivityResultLauncher<Intent> captureTtImageForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == RESULT_OK) {
            Intent intent = result.getData();

            if (intent != null && intent.hasExtra(Consts.Codes.Data.TTIMAGE)) {
                TtImage ttImage = intent.getParcelableExtra(Consts.Codes.Data.TTIMAGE);
                onImageCaptured(ttImage);
            }
        }

        _CapturedImageUri = null;
        _CapturedImagePointCN = null;
    });
    private final ActivityResultLauncher<String> requestTtCameraPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissionGranted -> {
        if (permissionGranted) {

            captureTtImageForResult.launch(
                    new Intent(this, TtCameraActivity.class)
                        .putExtra(Consts.Codes.Data.POINT_CN, _CapturedImagePointCN)
                        .putExtra(Consts.Codes.Data.TTIMAGE_URI, _CapturedImageUri));
        } else {
            Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_LONG).show();
        }
    });

    public void captureImage(boolean useTtCamera, TtPoint currentPoint) {
        DateTime dateTime = DateTime.now();

        _CapturedImageUri = Uri.parse(Paths.get(
                getTtAppCtx().getProjectMediaDir().toString(),
                String.format(Locale.getDefault(), "IMG_%s_%d.jpg", TtUtils.Date.toStringDateMillis(dateTime))).toString());
        _CapturedImagePointCN = currentPoint.getCN();

        if (useTtCamera) {
            if (AndroidUtils.App.checkCameraPermission(this)) {
                captureTtImageForResult.launch(
                        new Intent(this, TtCameraActivity.class)
                            .putExtra(Consts.Codes.Data.POINT_CN, _CapturedImagePointCN)
                            .putExtra(Consts.Codes.Data.TTIMAGE_URI, _CapturedImageUri));
            } else {
                requestTtCameraPermission.launch(Manifest.permission.CAMERA);
            }
        } else {
            captureImageForResult.launch(_CapturedImageUri);
        }




//        if (AndroidUtils.App.requestCameraPermission(activity, Consts.Codes.Requests.CAMERA)) {
//            if (useTtCamera) {
//                Intent intent = new Intent(activity, TtCameraActivity.class);
//
//                if (currentPoint != null) {
//                    intent.putExtra(Consts.Codes.Data.POINT_CN, currentPoint.getCN());
//                }
//
//                activity.startActivityForResult(intent, Consts.Codes.Activities.TTCAMERA);
//            } else {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//
//                DateTime dateTime = DateTime.now();
//
//                File photo = new File(activity.getApplicationContext().getCacheDir(), String.format("IMG_%d%d%d_%d.jpg",
//                        dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getMillisOfDay()));
////                    File photo = new File(TtUtils.getTtMediaDir(), String.format("IMG_%d%d%d_%d.jpg",
////                            dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getMillisOfDay()));
//
//                intent.putExtra(MediaStore.EXTRA_OUTPUT,
//                        AndroidUtils.Files.getUri(activity, BuildConfig.APPLICATION_ID, photo)
//                );
//
//                if (intent.resolveActivity(activity.getPackageManager()) != null) {
//                    activity.startActivityForResult(intent, Consts.Codes.Requests.CAPTURE_IMAGE);
//                    return Uri.fromFile(photo);
//                } else {
//                    Toast.makeText(activity, "Unable to find a Camera application", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
    }
    //endregion


    //region Get
    protected TtMedia getCurrentMedia() { return null; }
    //endregion


    //region Updates
    protected void onImageCaptured(TtImage image) { }

    protected void onImagesSelected(List<TtImage> images) { }

    protected void onImageOrientationUpdated(TtImage image) { }


    protected void onMediaUpdated(TtMedia media) { }
    //endregion


    //region Image Tools
    protected TtImage createImageFromFile(Uri uri, String pointCN) throws IOException {
        return TtUtils.Media.createImageFromFile(getTtAppCtx(), uri, pointCN);
    }

    public ArrayList<TtImage> getImagesFromUris(List<Uri> imageUris, String pointCN) throws IOException {
        return TtUtils.Media.getImagesFromUris(getTtAppCtx(), imageUris, pointCN);
    }
    //endregion
}
