package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.preferences.EnumPreference;
import com.usda.fmsc.android.preferences.ListCompatPreference;

public class GpsFixPreference extends EnumPreference {
    private static int[] itemValues = new int[] { 0, 1, 2, 3, 5, 4 };
    private static CharSequence[] itemNames = new String[] { "None", "3D", "3D+DIFF", "PPS", "Float RTK", "RTK"};

    public GpsFixPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(21)
    public GpsFixPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public GpsFixPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected CharSequence[] getItemNames() {
        return itemNames;
    }

    @Override
    protected int[] getItemValues() {
        return itemValues;
    }
}