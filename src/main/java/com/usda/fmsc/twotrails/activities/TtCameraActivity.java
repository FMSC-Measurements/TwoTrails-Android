package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.fragments.media.TtCameraFragment;
import com.usda.fmsc.twotrails.objects.media.TtImage;

public class TtCameraActivity extends AppCompatActivity implements TtCameraFragment.TtCameraListener {
    private TtImage image;
    private boolean error = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();

            if (bundle.containsKey(Consts.Codes.Data.POINT_CN)) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, TtCameraFragment.newInstance(bundle.getString(Consts.Codes.Data.POINT_CN)))
                        .commit();

                if (!AndroidUtils.Device.isFullOrientationAvailable(TtCameraActivity.this)) {
                    Toast.makeText(TtCameraActivity.this, "Orientation Not Supported", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

        error = true;
        finish();
    }


    @Override
    public void finish() {
        super.finish();

        if (error) {
            setResult(Consts.Codes.Results.ERROR);
        } else if (image == null) {
            setResult(RESULT_CANCELED);
        }
    }

    @Override
    public void onTtImageReady(TtImage image) {
        this.image = image;
        Bundle bundle = new Bundle();
        bundle.putParcelable(Consts.Codes.Data.TTIMAGE, image);
        setResult(Consts.Codes.Results.IMAGE_CAPTURED, new Intent().putExtras(bundle));
        finish();
    }
}
