package com.usda.fmsc.twotrails.activities.base;


import androidx.appcompat.app.AppCompatActivity;

import com.usda.fmsc.twotrails.TwoTrailsApp;

public abstract class TtActivity extends AppCompatActivity {
    private TwoTrailsApp TtAppCtx;

    public TwoTrailsApp getTtAppCtx() {
        return TtAppCtx != null ? TtAppCtx : (TtAppCtx = (TwoTrailsApp)getApplicationContext());
    }


}
