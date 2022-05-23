package com.usda.fmsc.twotrails.fragments;


import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
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
            }
        }

        return TtAppCtx;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (TtAppCtx == null) {
            Context ctx = context.getApplicationContext();
            if (ctx != null) {
                TtAppCtx = (TwoTrailsApp)ctx;
            } else {
                throw new RuntimeException("Null app context");
            }
        }
    }
}
