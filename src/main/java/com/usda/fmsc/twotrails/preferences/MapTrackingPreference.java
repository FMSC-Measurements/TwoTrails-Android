package com.usda.fmsc.twotrails.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.usda.fmsc.android.preferences.ListCompatPreference;
import com.usda.fmsc.twotrails.Units;

import java.lang.reflect.Method;

public class MapTrackingPreference extends ListCompatPreference {
    private int[] itemValues;
    private CharSequence[] itemNames;
    private DialogInterface.OnClickListener listener;

    public MapTrackingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(21)
    public MapTrackingPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public MapTrackingPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        Units.MapTracking[] items = Units.MapTracking.values();

        itemNames = new String[items.length];
        itemValues = new int[items.length];

        for (int i = 0; i < items.length; i++) {
            itemNames[i] = items[i].toString();
            itemValues[i] = items[i].getValue();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        int selected = getSharedPreferences().getInt(getKey(), -1);
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setNegativeButton(getNegativeButtonText(), this)
                .setSingleChoiceItems(itemNames, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setValue(itemValues[i]);

                        if (listener != null) {
                            listener.onClick(dialogInterface, i);
                        }

                        dialogInterface.dismiss();
                    }
                });

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            // ignored, nothing we can do
        }

        mDialog = builder.create();
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
    }

    public void setValue(int value) {
        getSharedPreferences().edit().putInt(getKey(), value).commit();
        setSummary(itemNames[value]);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if (itemValues != null)
            return itemValues[index];
        return null;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int value = 0;

        if (restorePersistedValue) {
            value = defaultValue != null ? (int)defaultValue : getSharedPreferences().getInt(getKey(), 0);
            setValue(value);
        }

        setSummary(itemNames[value]);
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        this.listener = listener;
    }
}