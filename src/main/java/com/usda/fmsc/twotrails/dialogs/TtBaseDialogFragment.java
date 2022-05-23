package com.usda.fmsc.twotrails.dialogs;


import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.DialogFragment;

import com.usda.fmsc.twotrails.TwoTrailsApp;

public abstract class TtBaseDialogFragment extends DialogFragment {
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
}
