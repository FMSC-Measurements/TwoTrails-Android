package com.usda.fmsc.twotrails.activities;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.activities.contracts.CreateZipDocument;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.usda.fmsc.geospatial.nmea.sentences.NmeaSentence;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.MimeTypes;

public class GpsLoggerActivity extends TtCustomToolbarActivity implements GpsService.Listener {
    private final String STRINGS_KEY = "strings";
    private final String LOGGING_KEY = "logging";

    private ArrayList<String> strings;
    private ArrayAdapter<String> a;
    private ListView lvNmea;

    private Button btnLog;
    private MenuItem miCheckLtf;

    private boolean logging;
    private File _CurrentLogFile;
    private final ArrayList<File> _AllGpsLogFiles = new ArrayList<>();


    private final ActivityResultLauncher<String> exportAllGpsLogs = registerForActivityResult(new CreateZipDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        File tmpZip = new File(getTtAppCtx().getCacheDir(), "tmpZip.zip");
                        FileUtils.zipFiles(tmpZip, _AllGpsLogFiles);
                        AndroidUtils.Files.copyFile(getTtAppCtx(), Uri.fromFile(tmpZip), uri);
                        tmpZip.delete();
                    } catch (Exception e) {
                        getTtAppCtx().getReport().writeError(e.getMessage(), "GpsLoggerActivity:exportAllGpsLogs");
                        Toast.makeText(GpsLoggerActivity.this, "Error exporting GPS Logs. See log file for details.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(GpsLoggerActivity.this, "Error selecting file for export", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> exportGpsLog = registerForActivityResult(new ActivityResultContracts.CreateDocument(MimeTypes.Text.PLAIN),
            uri -> {
                if (uri != null) {
                    try {
                        AndroidUtils.Files.copyFile(GpsLoggerActivity.this, Uri.fromFile(_CurrentLogFile), uri);
                    } catch (IOException e) {
                        getTtAppCtx().getReport().writeError(e.getMessage(), "GpsLoggerActivity:exportGpsLog");
                        Toast.makeText(GpsLoggerActivity.this, "Error exporting GPS Log. See log file for details.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(GpsLoggerActivity.this, "Error selecting file for export", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getTtAppCtx().getDeviceSettings().getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_gps_logger);

        lvNmea = findViewById(R.id.logLvNmea);

        if (lvNmea != null) {
            lvNmea.setFadingEdgeLength(0);

            btnLog = findViewById(R.id.loggerBtnLog);

            if (savedInstanceState != null) {
                restoreState(savedInstanceState);
            } else {
                strings = new ArrayList<>();

                a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, strings);

                lvNmea.setAdapter(a);

                if (getTtAppCtx().isGpsServiceStartedAndRunning() && getTtAppCtx().getGps().isLogging()) {
                    btnLog.setText(R.string.aqr_log_pause);
                    logging = true;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getTtAppCtx().isGpsServiceStarted()) {
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
            }
        }

        super.onBackPressed();
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
                item.setChecked(true);

                if (logging) {
                    getTtAppCtx().getGps().stopLogging();
                    _CurrentLogFile = getTtAppCtx().getGpsLogFile();
                    getTtAppCtx().getGps().startLogging(_CurrentLogFile);
                }
            }
            return true;
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.loggerMenuExportLog) {
            _AllGpsLogFiles.clear();
            for (File file : getTtAppCtx().getCacheDir().listFiles()) {
                if (file.getName().contains(Consts.Files.GPS_LOG_FILE_PREFIX)) {
                    _AllGpsLogFiles.add(file);
                }
            }

            Runnable exportCurrent = () -> exportGpsLog.launch(_CurrentLogFile.getName());
            Runnable exportAll = () -> exportAllGpsLogs.launch("TwoTrailsGPSLogFiles.zip");

            if (_CurrentLogFile != null) {
                if (_AllGpsLogFiles.size() > 1) {
                    new AlertDialog.Builder(GpsLoggerActivity.this)
                        .setMessage("Would you like to export the current GPS log or all the GPS logs?")
                        .setPositiveButton("Current", (dialog, which) -> {
                            exportCurrent.run();
                        })
                        .setNeutralButton("All", (dialog, which) -> {
                            exportAll.run();
                        })
                        .setNegativeButton(R.string.str_cancel, null)
                        .show();
                } else {
                    exportCurrent.run();
                }
            } else if (_AllGpsLogFiles.size() > 0) {
                exportAll.run();
            } else {
                Toast.makeText(GpsLoggerActivity.this, "There are no GPS Log files found to export", Toast.LENGTH_LONG).show();
            }
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean requiresGpsService() {
        return true;
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

        dialog.setPositiveButton("Configure", (dialog1, which) -> openSettings(SettingsActivity.GPS_SETTINGS_PAGE));

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }



    private void updateList() {
        a.notifyDataSetChanged();
    }

    public void btnLogClick(View view) {
        logging = !logging;

        if (logging) {
            strings.add(DateTime.now().toString());

            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                if (!getTtAppCtx().isGpsServiceStartedAndRunning()) {
                    Toast.makeText(GpsLoggerActivity.this, "GPS is not Receiving", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLog.setText(R.string.aqr_log_pause);

                if (miCheckLtf.isChecked()) {
                    _CurrentLogFile = getTtAppCtx().getGpsLogFile();
                    getTtAppCtx().getGps().startLogging(_CurrentLogFile);
                }
            } else {
                logging = false;
                configGPS();
            }
        } else {
            btnLog.setText(R.string.aqr_log);
            if (!getTtAppCtx().isGpsServiceStartedAndRunning()) {
                getTtAppCtx().getGps().stopLogging();
            }
        }
    }



    //region GpsService
    @Override
    public void gpsError(GpsService.GpsError error) {

    }

    @Override
    public void nmeaBurstReceived(GnssNmeaBurst nmeaBurst) {
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
    public void receivingNmeaStrings(boolean receiving) {
        if (!receiving) {
            Toast.makeText(GpsLoggerActivity.this, "Not receiving NMEA data.", Toast.LENGTH_LONG).show();
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
    //endregion
}
