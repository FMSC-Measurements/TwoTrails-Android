package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;

import org.joda.time.DateTime;

import java.util.ArrayList;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class GpsLoggerActivity extends CustomToolbarActivity implements GpsService.Listener {
    private final String STRINGS_KEY = "strings";
    private final String LOGGING_KEY = "logging";

    private ArrayList<String> strings;
    private ArrayAdapter<String> a;
    private ListView lvNmea;

    private Button btnLog;
    private MenuItem miCheckLtf;

    private boolean logging;

    private GpsService.GpsBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_logger);

        lvNmea = findViewById(R.id.logLvNmea);

        if (lvNmea != null) {
            lvNmea.setFadingEdgeLength(0);

            binder = getTtAppCtx().getGps();
            binder.addListener(this);

            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                binder.startGps();
            }

            btnLog = findViewById(R.id.loggerBtnLog);

            if (savedInstanceState != null) {
                restoreState(savedInstanceState);
            } else {
                strings = new ArrayList<>();
                strings.add(DateTime.now().toString());

                a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, strings);

                lvNmea.setAdapter(a);

                if (binder.isLogging()) {
                    btnLog.setText(R.string.aqr_log_pause);
                    logging = true;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        final GpsService.GpsBinder binder = getTtAppCtx().getGps();
        final Activity activity = this;

        if (binder.isLogging()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("GPS Logging");
            alert.setMessage("The GPS is currently logging data to a file. Would you like to keep logging in the background?");

            alert.setPositiveButton("Continue", (dialogInterface, i) -> activity.onBackPressed());

            alert.setNegativeButton("Stop", (dialogInterface, i) -> {
                if (!getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                    binder.stopGps();
                }
                activity.onBackPressed();
            });

            alert.setNeutralButton(R.string.str_cancel, null);
        } else {
            if (!getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                binder.stopGps();
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        inflateMenu(R.menu.menu_gps_logger, menu);

        miCheckLtf = menu.findItem(R.id.loggerMenuLtf);
        miCheckLtf.setCheckable(true);

        miCheckLtf.setOnMenuItemClickListener(item -> {
            if (item.isChecked()) {
                item.setChecked(false);

                if (logging) {
                    getTtAppCtx().getGps().stopLogging();
                    logging = false;
                }
            } else {
                if (AndroidUtils.App.requestStoragePermission(GpsLoggerActivity.this, Consts.Codes.Requests.OPEN_FILE)) {
                    item.setChecked(true);

                    if (logging) {
                        getTtAppCtx().getGps().startLogging(TtUtils.getGpsLogFilePath());
                    }
                }
            }
            return true;
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Consts.Codes.Activites.SETTINGS) {
            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                binder.startGps();
            }
        }
    }


    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(STRINGS_KEY)) {
            strings = savedInstanceState.getStringArrayList(STRINGS_KEY);

            if (strings == null) {
                strings = new ArrayList<>();
            }

            a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, strings);

            lvNmea.setAdapter(a);
        }

        if (!logging && savedInstanceState.containsKey(LOGGING_KEY)) {
            if (savedInstanceState.getBoolean(LOGGING_KEY)) {
                btnLog.setText(R.string.aqr_log_pause);
                logging = true;
            }
        }
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (strings != null && strings.size() > 1) {
            outState.putStringArrayList(STRINGS_KEY, strings);
            outState.putBoolean(LOGGING_KEY, logging);
        }
    }


    private void configGPS() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("The GPS is currently not configured. Would you like to configure it now?");

        dialog.setPositiveButton("Configure", (dialog1, which) -> startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE), Consts.Codes.Activites.SETTINGS));

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }



    private void updateList() {
        a.notifyDataSetChanged();
    }

    public void btnLogClick(View view) {
        logging = !logging;

        if (logging) {
            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                if (!binder.isGpsRunning()) {
                    Toast.makeText(GpsLoggerActivity.this, "GPS is not Receiving", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLog.setText(R.string.aqr_log_pause);

                if (miCheckLtf.isChecked()) {
                    binder.startLogging(TtUtils.getGpsLogFilePath());
                }
            } else {
                logging = false;
                configGPS();
            }
        } else {
            btnLog.setText(R.string.aqr_log);
            binder.stopLogging();
        }
    }



    //region GpsService
    @Override
    public void gpsError(GpsService.GpsError error) {

    }

    @Override
    public void nmeaBurstReceived(INmeaBurst nmeaBurst) {
        if (logging) {
            strings.add(0, nmeaBurst.toString());//String.format("Burst Received- Valid: %s", Boolean.toString(nmeaBurst.isValid())));
            updateList();
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {
        if (logging) {
            strings.add(0, nmeaString);

//            if (strings.size() > 300) {
//                strings = strings.subList(0, 200);
//                lvNmea.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, strings));
//            }

            updateList();
        }
    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {
        //
    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsAreValid) {

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
}
