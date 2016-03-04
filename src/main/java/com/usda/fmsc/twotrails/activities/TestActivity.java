package com.usda.fmsc.twotrails.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;

public class TestActivity extends AppCompatActivity implements GpsService.Listener {
    GpsService.GpsBinder binder;
    GpsStatusSkyView gskyv;
    GpsStatusSatView gsatv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        gskyv = (GpsStatusSkyView)findViewById(R.id.testGpsStatusSkyView);
        gsatv = (GpsStatusSatView)findViewById(R.id.testGpsStatusSatView);

        binder = Global.getGpsBinder();

        if (binder != null) {
            binder.registerActiviy(this, this);

            if (!binder.isGpsRunning()) {
                binder.startGps();
            }
        }


        gskyv.lockCompass(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (binder != null) {
            binder.unregisterActivity(this);

            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gskyv != null) {
            gskyv.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (gskyv != null) {
            gskyv.pause();
        }
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        if (gskyv != null) {
            gskyv.update(nmeaBurst);
            gsatv.update(nmeaBurst);
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

    }

    @Override
    public void gpsStarted() {

    }

    @Override
    public void gpsStopped() {

    }

    @Override
    public void gpsServiceStarted() {

    }

    @Override
    public void gpsServiceStopped() {

    }

    @Override
    public void gpsError(GpsService.GpsError error) {

    }

}
