package com.usda.fmsc.twotrails.activities.base;


import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.usda.fmsc.twotrails.TwoTrailsApp;

public abstract class TtActivity extends AppCompatActivity {
    private TwoTrailsApp TtAppCtx;

    public TwoTrailsApp getTtAppCtx() {
        return TtAppCtx != null ? TtAppCtx : (TtAppCtx = (TwoTrailsApp)getApplicationContext());
    }

    public boolean requiresGpsService() {
        return false;
    }

    public boolean requiresRFService() {
        return false;
    }

    public boolean requiresInsService() {
        return false;
    }
}
