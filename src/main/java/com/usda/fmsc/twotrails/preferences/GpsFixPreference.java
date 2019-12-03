package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.EnumPreference;
import com.usda.fmsc.geospatial.nmea41.sentences.GSASentence;

public class GpsFixPreference extends EnumPreference {
    private int[] itemValues;
    private CharSequence[] itemNames;

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
    protected void parseEnums() {
        GSASentence.Fix[] items = GSASentence.Fix.values();

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