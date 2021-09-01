package com.usda.fmsc.twotrails.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.NmeaIDs;
import com.usda.fmsc.geospatial.nmea41.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea41.sentences.GSASentence;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Settings;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;
import com.usda.fmsc.utilities.StringEx;

@SuppressLint("DefaultLocale")
public class GpsStatusActivity extends CustomToolbarActivity implements GpsService.Listener {
    private static final String nVal = "*";

    private Integer zone = null;

    private TextView tvGpsStatus, tvGpsFix, tvLat, tvLon, tvUtmX, tvUtmY,
            tvZone, tvDec, tvSat, tvElev, tvPdop, tvHdop;

    private GpsStatusSkyView skyView;
    private GpsStatusSatView statusView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getTtAppCtx().getDeviceSettings().getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_gps_status);

        if (getTtAppCtx().hasDAL()) {
            TtMetadata metadata = getTtAppCtx().getDAL().getDefaultMetadata();

            if (metadata != null) {
                zone = metadata.getZone();
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvGpsStatus = findViewById(R.id.gpsInfoTvGpsStatus);
        tvGpsFix = findViewById(R.id.gpsInfoTvGpsFix);
        tvLat = findViewById(R.id.gpsInfoTvLat);
        tvLon = findViewById(R.id.gpsInfoTvLon);
        tvUtmX = findViewById(R.id.gpsInfoTvUtmX);
        tvUtmY = findViewById(R.id.gpsInfoTvUtmY);
        tvZone = findViewById(R.id.gpsInfoTvZone);
        tvDec = findViewById(R.id.gpsInfoTvDec);
        tvSat = findViewById(R.id.gpsInfoTvSats);
        tvElev = findViewById(R.id.gpsInfoTvElev);
        tvPdop = findViewById(R.id.gpsInfoTvPdop);
        tvHdop = findViewById(R.id.gpsInfoTvHdop);

        tvGpsFix.setText(GGASentence.GpsFixType.NoFix.toString());

        skyView = findViewById(R.id.gpsInfoSatSky);
        statusView = findViewById(R.id.gpsInfoSatStatus);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            configGPS();
        }
    }

    @Override
    public boolean requiresGpsService() {
        return true;
    }

    protected void setNmeaData(final NmeaBurst burst) {
        runOnUiThread(() -> {
            try {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(String.format("%.4f", burst.getLatitudeSD()));
                    tvLon.setText(String.format("%.4f", burst.getLongitudeSD()));

                    tvUtmX.setText(String.format("%.3f", coords.getX()));
                    tvUtmY.setText(String.format("%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    if (burst.hasElevation()) {
                        tvElev.setText(String.format("%.2f", burst.getElevation()));
                    } else {
                        tvElev.setText(nVal);
                    }
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

                if (burst.isValid(NmeaIDs.SentenceID.RMC) && burst.getMagVar() != null) {
                    tvDec.setText(String.format("%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
                } else {
                    tvDec.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GGA)) {
                    tvGpsFix.setText(burst.getFixQuality().toStringX());
                } else {
                    tvGpsFix.setText(GGASentence.GpsFixType.NoFix.toString());
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSA)) {
                    tvGpsStatus.setText(burst.getFix().toString());
                    tvPdop.setText(burst.getPDOP() == null ? nVal : String.format("%.2f", burst.getPDOP()));
                    tvHdop.setText(burst.getHDOP() == null ? nVal : String.format("%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(GSASentence.Fix.NoFix.toString());
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSV)) {

                    tvSat.setText(String.format("%d/%d/%d",
                            burst.getUsedSatellitesCount(),
                            burst.isValid(NmeaIDs.SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()));
                } else {
                    tvSat.setText(nVal);
                }

                skyView.update(burst);
                statusView.update(burst);
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError("GpsStatusActivity:setNmeaData", e.getMessage(), e.getStackTrace());
            }
        });
    }


    private void configGPS() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("The GPS is currently not configured. Would you like to configure it now?");

        dialog.setPositiveButton("Configure", (dialog1, which) -> openSettings(SettingsActivity.GPS_SETTINGS_PAGE));

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst burst) {
        setNmeaData(burst);
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsAreValid) {

    }

    @Override
    public void receivingNmeaStrings(boolean receiving) {
        if (!receiving) {
            Toast.makeText(GpsStatusActivity.this, "Not receiving NMEA data.", Toast.LENGTH_LONG).show();
        }
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
        if (error == GpsService.GpsError.LostDeviceConnection) {
            AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            final Activity activity = this;
            dialog.setTitle("GPS Connection Lost");
            dialog.setMessage("The GPS bluetooth connection has been broken. Would you like to try and reestablish the connection?");
            dialog.setPositiveButton("Connect", (d, which) -> {
                GpsService.GpsDeviceStatus status = getTtAppCtx().getGps().startGps();

                if (status != GpsService.GpsDeviceStatus.ExternalGpsStarted &&
                        status != GpsService.GpsDeviceStatus.InternalGpsStarted) {
                    Toast.makeText(GpsStatusActivity.this, "Unable to connect to GPS.", Toast.LENGTH_SHORT).show();
                    activity.setResult(RESULT_CANCELED);
                    activity.finish();
                } else {
                    AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_CONNECTED);
                }
            });
            dialog.setNegativeButton(R.string.str_exit, (d, which) -> {
                activity.setResult(RESULT_CANCELED);
                activity.finish();
            });
            dialog.show();
        }
    }

    public void btnGpsInfoClick(View view) {

    }
}
