package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.dialogs.PointColorPickerDialog;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        @ColorInt int[] colors = new int[] {
                AndroidUtils.UI.getColor(this, R.color.map_adj_bnd),
                AndroidUtils.UI.getColor(this, R.color.map_adj_nav),
                AndroidUtils.UI.getColor(this, R.color.map_unadj_bnd),
                AndroidUtils.UI.getColor(this, R.color.map_unadj_nav),
                AndroidUtils.UI.getColor(this, R.color.map_way_pts),
                AndroidUtils.UI.getColor(this, R.color.map_adj_pts),
                AndroidUtils.UI.getColor(this, R.color.map_unadj_pts)
        };

        PointColorPickerDialog d = PointColorPickerDialog.newInstance(colors, "Test");

        d.show(getSupportFragmentManager(), "");
    }
}
