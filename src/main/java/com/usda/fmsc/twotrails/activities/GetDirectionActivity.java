package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.fragments.CameraFragment;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.fragments.media.TtCameraFragment;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class GetDirectionActivity extends CustomToolbarActivity implements CameraFragment.CameraListener {
    private TtCameraFragment cameraFragment;

    private View cameraView;
    private EditText txtAzimuth, txtRoll, txtPitch;
    private boolean _canceled = true, updating;

    private DeviceOrientationEx.Orientation _Orientation =  new DeviceOrientationEx.Orientation(0f, 0f, 0f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_direction);


        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            try {
                if (intent.getExtras().containsKey(Consts.Codes.Data.ORIENTATION)) {
                    _Orientation = intent.getParcelableExtra(Consts.Codes.Data.ORIENTATION);
                }
            } catch (Exception e) {
                TtAppCtx.getReport().writeError(e.getMessage(), "GetDirectionActivity:onCreate");
            }
        }

        txtAzimuth = findViewById(R.id.gdTxtAzimuth);
        txtRoll = findViewById(R.id.gdTxtRoll);
        txtPitch = findViewById(R.id.gdTxtPitch);

        Button btnAqr = findViewById(R.id.gdBtnAcquire);
        if (!AndroidUtils.Device.isFullOrientationAvailable(this)) {
            btnAqr.setEnabled(false);
            btnAqr.setAlpha(Consts.DISABLED_ALPHA);
        }

        updateOrientation();

        cameraView = findViewById(R.id.lay1);

        txtAzimuth.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!updating) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Orientation.setAzimuth(value);
                }
            }
        });

        txtPitch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!updating) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Orientation.setPitch(value);
                }
            }
        });

        txtRoll.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!updating) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Orientation.setRoll(value);
                }
            }
        });
    }

    @Override
    public void finish() {
        if (_canceled) {
            setResult(RESULT_CANCELED);
        } else {
            setResult(Consts.Codes.Results.ORIENTATION_UPDATED,
                    new Intent().putExtra(Consts.Codes.Data.ORIENTATION, _Orientation));
        }

        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (cameraView.getVisibility() == View.VISIBLE) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(cameraFragment)
                    .commit();

            cameraView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    private void updateOrientation() {
        updating = true;

        txtAzimuth.setText(StringEx.toString(_Orientation.getRationalAzimuth(), 4));
        txtRoll.setText(StringEx.toString(_Orientation.getRoll(), 4));
        txtPitch.setText(StringEx.toString(_Orientation.getPitch(), 4));

        updating = false;
    }

    @Override
    public void onCapturePressed() {
        _Orientation = cameraFragment.getOrientation();

        getSupportFragmentManager()
                .beginTransaction()
                .remove(cameraFragment)
                .commit();

        updateOrientation();

        cameraView.setVisibility(View.GONE);
    }



    public void btnAcquireClick(View view) {
        if (cameraFragment == null) {
            cameraFragment = TtCameraFragment.newInstanceWithOutSave();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.frag_camera, cameraFragment)
                .commit();

        cameraView.setVisibility(View.VISIBLE);
    }

    public void btnSaveClick(View view) {
        _canceled = false;
        finish();
    }

    public void btnCancelClick(View view) {
        finish();
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
