package com.usda.fmsc.twotrails;

import android.app.Application;

//android.support.multidex.MultiDexApplication pre 5.0
public class TwoTrailApp extends Application {
    private static TwoTrailApp _AppContext;

    @Override
    public void onCreate() {
        super.onCreate();

        _AppContext = (TwoTrailApp)getApplicationContext();
    }

    public static TwoTrailApp getAppContext() {
        return _AppContext;
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//    }
//
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//    }











    
}
