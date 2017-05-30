package com.usda.fmsc.twotrails.activities;

import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.usda.fmsc.android.fragments.CameraFragment;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.fragments.media.TtCameraFragment;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

public class GetDirectionActivity extends CustomToolbarActivity implements CameraFragment.CameraListener {
    private TtCameraFragment cameraFragment;

    private View cameraView;
    private EditText txtAzimuth, txtRoll, txtPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_direction);

        txtAzimuth = (EditText)findViewById(R.id.gdTxtAzimuth);
        txtRoll = (EditText)findViewById(R.id.gdTxtRoll);
        txtPitch = (EditText)findViewById(R.id.gdTxtPitch);

        cameraView = findViewById(R.id.frag_camera);

    }




    @Override
    public void onCapturePressed() {
        DeviceOrientationEx.Orientation orientation = cameraFragment.getOrientation();

        txtAzimuth.setText(StringEx.toString(orientation.getAzimuth(), 2));
        txtRoll.setText(StringEx.toString(orientation.getRoll(), 2));
        txtPitch.setText(StringEx.toString(orientation.getPitch(), 2));

        cameraView.setVisibility(View.GONE);
    }



    public void btnAcquireClick(View view) {

        if (cameraFragment == null) {
            cameraFragment = TtCameraFragment.newInstanceWithOutSave();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frag_camera, cameraFragment)
                .commit();

        cameraView.setVisibility(View.VISIBLE);
    }




    //region Not Useds
    @Override
    public void onPrecapture() {

    }

    @Override
    public void onCaptured() {

    }

    @Override
    public void onImageReady(Image image) {

    }

    @Override
    public void onImageSaved(String filePath) {

    }

    @Override
    public void onError(String message) {

    }
    //endregion
}
