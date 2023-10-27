package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.EnumPreference;
import com.usda.fmsc.geospatial.gnss.codes.GnssFixQuality;
import com.usda.fmsc.geospatial.gnss.nmea.sentences.GSASentence;

public class GpsFixPreference extends EnumPreference {
    private int[] itemValues;
    private CharSequence[] itemNames;

    public GpsFixPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GpsFixPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GpsFixPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void parseEnums() {
        GnssFixQuality[] items = GnssFixQuality.values();

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