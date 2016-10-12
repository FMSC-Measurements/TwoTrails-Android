package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.EnumPreference;

public class GpsFixTypePreference extends EnumPreference {
    private static int[] itemValues = new int[] { 0, 1, 2, 3, 5, 4 };
    private static CharSequence[] itemNames = new String[] { "None", "3D", "3D+DIFF", "PPS", "Float RTK", "RTK"};

    public GpsFixTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(21)
    public GpsFixTypePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public GpsFixTypePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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