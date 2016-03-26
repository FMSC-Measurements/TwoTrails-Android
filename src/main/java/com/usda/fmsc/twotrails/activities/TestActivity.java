package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.custom.MultiMapTypeActivity;
import com.usda.fmsc.twotrails.gps.GpsService;

public class TestActivity extends MultiMapTypeActivity implements GpsService.Listener {
    GpsService.GpsBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        binder = Global.getGpsBinder();

        if (binder != null) {
            binder.addListener(this);

            if (!binder.isGpsRunning()) {
                //binder.startGps();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mmSelectMap) {
            selectMapType();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (binder != null) {
            binder.removeListener(this);

            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
            }
        }
    }


    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {

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
