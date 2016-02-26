package com.usda.fmsc.twotrails.activities.custom;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.utilities.StringEx;

import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;

public class AcquireGpsCustomToolbarActivity extends CustomToolbarActivity implements GpsService.Listener {
    public static final int GPS_NOT_FOUND = 1910;
    public static final int GPS_NOT_CONFIGURED = 1911;

    private static final String nVal = "*";

    private TextView tvGpsStatus, tvGpsMode, tvLat, tvLon, tvUtmX, tvUtmY,
            tvZone, tvDec, tvSat, tvElev, tvPdop, tvHdop;


    private GpsService.GpsBinder binder;
    private Integer zone = null;
    private boolean canceling = false, useLostConnectionWarning = false;
    private boolean logging;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binder = Global.getGpsBinder();

        if (binder != null) {
            binder.registerActiviy(this, this);

            if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
                canceling = true;
                setResult(GPS_NOT_CONFIGURED);
                finish();
                return;
            }

            if (!binder.isGpsRunning()) {
                binder.startGps();
            }
        } else {
            canceling = true;
            setResult(GPS_NOT_FOUND);
            finish();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvGpsStatus = (TextView)findViewById(R.id.gpsInfoTvGpsStatus);
        tvGpsMode = (TextView)findViewById(R.id.gpsInfoTvGpsMode);
        tvLat = (TextView)findViewById(R.id.gpsInfoTvLat);
        tvLon = (TextView)findViewById(R.id.gpsInfoTvLon);
        tvUtmX = (TextView)findViewById(R.id.gpsInfoTvUtmX);
        tvUtmY = (TextView)findViewById(R.id.gpsInfoTvUtmY);
        tvZone = (TextView)findViewById(R.id.gpsInfoTvZone);
        tvDec = (TextView)findViewById(R.id.gpsInfoTvDec);
        tvSat = (TextView)findViewById(R.id.gpsInfoTvSats);
        tvElev = (TextView)findViewById(R.id.gpsInfoTvElev);
        tvPdop = (TextView)findViewById(R.id.gpsInfoTvPdop);
        tvHdop = (TextView)findViewById(R.id.gpsInfoTvHdop);

        tvGpsMode.setText(GGASentence.GpsFixType.NoFix.toString());
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

    protected void setNmeaData(final NmeaBurst burst) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(String.format("%.4f", burst.getLatitude()));
                    tvLon.setText(String.format("%.4f", burst.getLongitude()));

                    tvUtmX.setText(String.format("%.3f", coords.getX()));
                    tvUtmY.setText(String.format("%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    tvElev.setText(String.format("%.2f", burst.getElevation()));
                } else {
                    tvLat.setText(nVal);
                    tvLon.setText(nVal);
                    tvUtmX.setText(nVal);
                    tvUtmY.setText(nVal);
                    tvElev.setText(nVal);

                    if (zone == null) {
                        tvZone.setText(nVal);
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }
                }

                if (burst.getRMC().hasMagVar()) {
                    tvDec.setText(String.format("%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
                } else {
                    tvDec.setText(nVal);
                }

                if (burst.getGGA().isValid()) {
                    tvGpsMode.setText(burst.getFixQuality().toStringX());
                } else {
                    tvGpsMode.setText(GGASentence.GpsFixType.NoFix.toString());
                }

                if (burst.getGSA().isValid()) {
                    tvGpsStatus.setText(burst.getFix().toString());
                    tvPdop.setText(String.format("%.2f", burst.getPDOP()));
                    tvHdop.setText(String.format("%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(nVal);
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                }

                if (burst.getGSV().isValid()) {
                    tvSat.setText(String.format("%d/%d/%d",
                            burst.getGSA().isValid() ? burst.getUsedSatellitesCount() : 0,
                            burst.getGGA().isValid() ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()));
                } else {
                    tvSat.setText(nVal);
                }
            }
        });
    }


    public void setZone(Integer zone) {
        this.zone = zone;
        tvZone.setText(zone != null ? Integer.toString(zone) : getText(R.string.str_nullvalue));
    }

    protected void startLogging() {
        logging = true;
    }
    protected void stopLogging() {
        logging = false;
    }

    protected boolean isLogging() {
        return logging;
    }


    protected void setUseLostConnectionWarning(boolean useLostConnectionWarning) {
        this.useLostConnectionWarning = useLostConnectionWarning;
    }


    //region Listener
    @Override
    public void gpsError(GpsService.GpsError error) {
        switch (error) {
            case LostDeviceConnection: {
                if (useLostConnectionWarning) {
                    AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                    final Activity activity = this;

                    dialog.setTitle("GPS Connection Lost");
                    dialog.setMessage("The GPS bluetooth connection has been broken. Would you like to try and reestablith the connection?");

                    dialog.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GpsService.GpsDeviceStatus status = Global.getGpsBinder().startGps();

                            if (status != GpsService.GpsDeviceStatus.ExternalGpsStarted &&
                                    status != GpsService.GpsDeviceStatus.InternalGpsStarted) {
                                Toast.makeText(Global.getMainActivity(), "Unabled to conenct to GPS.", Toast.LENGTH_SHORT).show();
                                activity.setResult(RESULT_CANCELED);
                                activity.finish();
                            } else {
                                AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_CONNECTED);
                            }
                        }
                    });

                    dialog.setNegativeButton(R.string.str_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.setResult(RESULT_CANCELED);
                            activity.finish();
                        }
                    });

                    dialog.show();
                }
                break;
            }
        }
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {

        setNmeaData(nmeaBurst);
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {
        //
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
    //endregion


    protected boolean isCanceling() {
        return canceling;
    }
}
