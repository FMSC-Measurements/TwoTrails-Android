package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.EnumPreference;
import com.usda.fmsc.twotrails.units.MapTracking;

public class MapTrackingPreference extends EnumPreference {
    private int[] itemValues;
    private CharSequence[] itemNames;

    public MapTrackingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(21)
    public MapTrackingPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public MapTrackingPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void parseEnums() {
        MapTracking[] items = MapTracking.values();

        itemNames = new String[items.length];
        itemValues = new int[items.length];

        for (int i = 0; i < items.length; i++) {
            itemNames[i] = items[i].toString();
            itemValues[i] = items[i].getValue();
        }
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