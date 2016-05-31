package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.EnumPreference;
import com.usda.fmsc.twotrails.units.DopType;

public class DopTypePreference extends EnumPreference {
    private int[] itemValues;
    private CharSequence[] itemNames;

    public DopTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(21)
    public DopTypePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public DopTypePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void parseEnums() {
        DopType[] items = DopType.values();

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