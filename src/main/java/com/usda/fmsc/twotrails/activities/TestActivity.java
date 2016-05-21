package com.usda.fmsc.twotrails.activities;

import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.Toast;

import com.usda.fmsc.android.fragments.CameraFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.BaseMapActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;

public class TestActivity extends AppCompatActivity implements CameraFragment.CameraListener {

    CameraFragment cameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraFragment = CameraFragment.newInstance();

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, cameraFragment).commit();
    }


    @Override
    public void onCaptured() {
        //Toast.makeText(getBaseContext(), "Image Captured", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onImageSaved(String filePath) {
        Toast.makeText(getBaseContext(), "File Saved: " + filePath, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getBaseContext(), "Error: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPrecapture() {

    }

    @Override
    public void onImageReady(Image image) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
