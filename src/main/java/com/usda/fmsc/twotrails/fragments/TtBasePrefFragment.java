package com.usda.fmsc.twotrails.fragments;


import android.app.Activity;
import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;

import com.usda.fmsc.twotrails.TwoTrailsApp;

public abstract class TtBasePrefFragment extends PreferenceFragmentCompat {
    private TwoTrailsApp TtAppCtx;

    public TwoTrailsApp getTtAppCtx() {
        if (TtAppCtx == null) {
            Activity act =  getActivity();

            if (act != null) {
                Context ctx = act.getApplicationContext();
                if (ctx != null) {
                    TtAppCtx = (TwoTrailsApp)ctx;
                } else {
                    throw new RuntimeException("Null app context");
                }
            } else {
                throw new RuntimeException("Activity not found");
            }
        }

        return TtAppCtx;
    }
}
