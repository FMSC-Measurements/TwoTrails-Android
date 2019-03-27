package com.usda.fmsc.twotrails.fragments.settings;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.preferences.ListCompatPreference;
import com.usda.fmsc.android.preferences.SwitchCompatPreference;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.TwoTrailApp;
import com.usda.fmsc.twotrails.activities.SettingsActivity;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.dialogs.CheckNmeaDialog;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.rangefinder.TtRangeFinderData;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class DeviceSettingsFragment extends PreferenceFragment {
    public static final String CURRENT_PAGE = "CurrentPage";

    private TwoTrailApp TtAppCtx;

    private Preference prefGpsCheck, prefRFCheck;
    private SwitchCompatPreference swtUseExGpsDev;
    private PreferenceCategory exGpsCat;
    private ListCompatPreference prefLstGpsDevice, prefLstRFDevice;

    private String moveToPage;

    private DateTime lastMetaAsk = DateTime.now();


    public static DeviceSettingsFragment newInstance(String currPageKey) {
        DeviceSettingsFragment fragment = new DeviceSettingsFragment();
        Bundle args = new Bundle();
        args.putString(CURRENT_PAGE, currPageKey);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TtAppCtx = TwoTrailApp.getContext();

        Bundle bundle = getArguments();

        addPreferencesFromResource(R.xml.pref_device_setup);

        if (bundle != null && bundle.containsKey(CURRENT_PAGE)) {
            moveToPage = bundle.getString(CURRENT_PAGE);

            ActionBar actionBar = ((CustomToolbarActivity)getActivity()).getSupportActionBar();

            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);

                if (moveToPage != null) {
                    switch (moveToPage) {
                        case SettingsActivity.GPS_SETTINGS_PAGE:
                            actionBar.setTitle("GPS Setup");
                            break;
                        case SettingsActivity.LASER_SETTINGS_PAGE:
                            actionBar.setTitle("Range Finder Setup");
                            break;
                        default:
                            actionBar.setTitle("Settings");
                            break;
                    }
                } else {
                    actionBar.setTitle(R.string.str_settings);
                }
            }
        }

        swtUseExGpsDev = (SwitchCompatPreference)findPreference(getString(R.string.set_GPS_EXTERNAL));
        exGpsCat = (PreferenceCategory)findPreference(getString(R.string.set_GPS_CAT));
        prefLstGpsDevice = (ListCompatPreference)findPreference(getString(R.string.set_GPS_LIST_DEVICE));
        prefGpsCheck = findPreference(getString(R.string.set_GPS_CHECK));
        prefLstRFDevice = (ListCompatPreference)findPreference(getString(R.string.set_RF_LIST_DEVICE));
        prefRFCheck = findPreference(getString(R.string.set_RF_CHECK));
        Preference prefCheckNmea = findPreference(getString(R.string.set_GPS_CHECK_NMEA));

        swtUseExGpsDev.setOnPreferenceChangeListener(useExternalListener);
        swtUseExGpsDev.setSummary(getString(swtUseExGpsDev.isChecked() ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));

        prefGpsCheck.setOnPreferenceClickListener(gpsCheckListener);
        prefRFCheck.setOnPreferenceClickListener(rfCheckListener);

        prefLstGpsDevice.setOnPreferenceChangeListener(btnGPSList);
        prefLstRFDevice.setOnPreferenceChangeListener(btnRFList);

        prefCheckNmea.setOnPreferenceClickListener(checkNmeaListener);

        //get initial bluetooth devices
        setBTValues(prefLstGpsDevice);
        setBTValues(prefLstRFDevice);

        exGpsCat.setEnabled(TtAppCtx.getDeviceSettings().getGpsExternal());

        String devName = TtAppCtx.getDeviceSettings().getGpsDeviceName();
        if (StringEx.isEmpty(devName)) {
            prefLstGpsDevice.setSummary(R.string.ds_no_dev);
        } else {
            prefLstGpsDevice.setSummary(devName);
        }

        if (TtAppCtx.getDeviceSettings().isGpsConfigured()) {
            if (TtAppCtx.getGps().isGpsRunning()) {
                prefGpsCheck.setSummary(R.string.ds_gps_connected);
            } else {
                prefGpsCheck.setSummary(R.string.ds_dev_configured);
            }
        } else {
            prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
        }

        devName = TtAppCtx.getDeviceSettings().getRangeFinderDeviceName();
        if (StringEx.isEmpty(devName)) {
            prefLstRFDevice.setSummary(R.string.ds_no_dev);
        } else {
            prefLstRFDevice.setSummary(devName);
        }

        if (TtAppCtx.getDeviceSettings().isRangeFinderConfigured()) {
            if (TtAppCtx.getRF().isRangeFinderRunning()) {
                prefRFCheck.setSummary(R.string.ds_rf_connected);
            } else {
                prefRFCheck.setSummary(R.string.ds_dev_configured);
            }
        } else {
            prefRFCheck.setSummary(R.string.ds_dev_not_configured);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (moveToPage != null && !moveToPage.equals(SettingsActivity.MAIN_SETTINGS_PAGE)) {
            setPreferenceScreen((PreferenceScreen) findPreference(moveToPage));
        }
    }

    private void setBTValues(ListPreference lstPref) {
        TtBluetoothManager btm = new TtBluetoothManager();

        try {
            if (btm.isEnabled() && btm.isAvailable()) {
                List<String> deviceNames = new ArrayList<>();
                List<String> deviceIDs = new ArrayList<>();

                for (BluetoothDevice btd : btm.getAdapter().getBondedDevices()) {
                    deviceNames.add(btd.getName());
                    deviceIDs.add(String.format("%s,%s", btd.getAddress(), btd.getName()));
                }

                lstPref.setEntries(deviceNames.toArray(new String[deviceNames.size()]));
                lstPref.setEntryValues(deviceIDs.toArray(new String[deviceIDs.size()]));
            }
        } catch (Exception e) {
            //
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Consts.Codes.Requests.BLUETOOH && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switchToExternal();
        }
    }


    //region GPSCheck
    Preference.OnPreferenceClickListener gpsCheckListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (StringEx.isEmpty(TtAppCtx.getDeviceSettings().getGpsDeviceID())) {
                Toast.makeText(getActivity(), "GPS must first be selected", Toast.LENGTH_LONG).show();
            } else {
                try {
                    TtAppCtx.getDeviceSettings().setGpsConfigured(false);

                    final Activity activity = getActivity();
                    final ProgressDialog pd = new ProgressDialog(activity);
                    final GpsService.GpsBinder gps = TtAppCtx.getGps();

                    prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

                    final GpsService.Listener listener = new GpsService.Listener() {
                        @Override
                        public void nmeaBurstReceived(INmeaBurst nmeaBurst) {

                        }

                        @Override
                        public void nmeaStringReceived(String nmeaString) {
                            gps.removeListener(this);

                            TtAppCtx.getDeviceSettings().setGpsConfigured(true);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage(activity.getString(R.string.ds_gps_connected));

                                    if (TtAppCtx.getDeviceSettings().isGpsAlwaysOn()) {
                                        prefGpsCheck.setSummary(R.string.ds_gps_connected);
                                    } else {
                                        prefGpsCheck.setSummary(R.string.ds_dev_configured);
                                        gps.stopGps();
                                    }

                                    if (TtAppCtx.getDeviceSettings().getAutoSetGpsNameToMetaAsk()) {
                                        if (lastMetaAsk.isBefore(DateTime.now().minusSeconds(10))) {
                                            DontAskAgainDialog dialog = new DontAskAgainDialog(getActivity(),
                                                    DeviceSettings.AUTO_SET_GPS_NAME_TO_META_ASK,
                                                    DeviceSettings.AUTO_SET_GPS_NAME_TO_META,
                                                    TtAppCtx.getDeviceSettings().getPrefs());

                                            dialog.setMessage("GPS is connected. Do you want to update metadata with the current GPS receiver?");

                                            dialog.setPositiveButton("Default", setMetaListener, 1);

                                            if (TtAppCtx.getDAL() != null)
                                                dialog.setNegativeButton("All", setMetaListener, 2);

                                            dialog.setNeutralButton("None", null, 0);

                                            dialog.show();

                                            lastMetaAsk = DateTime.now();

                                            pd.hide();
                                        } else {
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    pd.hide();
                                                }
                                            }, 1000);
                                        }
                                    } else {
                                        setMetaListener.onClick(null, 0, TtAppCtx.getDeviceSettings().getAutoSetGpsNameToMeta());
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                pd.hide();
                                            }
                                        }, 1000);
                                    }
                                }
                            });
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

                        @Override
                        public void gpsError(GpsService.GpsError error) {
                            gps.removeListener(this);
                            gps.stopGps();

                            TtAppCtx.getDeviceSettings().setGpsConfigured(false);

                            String message = "";

                            switch (error) {
                                case LostDeviceConnection: message = "Lost connection to bluetooth device."; break;
                                case DeviceConnectionEnded: message = "Bluetooth connection terminated."; break;
                                case NoExternalGpsSocket: message = "Failed to create bluetooth socket."; break;
                                case FailedToConnect: message = "Failed to connect to bluetooth device."; break;
                                case Unknown: message = error.toString(); break;
                            }

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage(getString(R.string.ds_gps_not_connected));
                                    pd.hide();
                                }
                            });

                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        }
                    };

                    gps.addListener(listener);

                    final Runnable hideDialog = new Runnable() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.hide();
                                }
                            });
                        }
                    };

                    pd.setMessage(getString(R.string.ds_gps_connecting));

                    pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            gps.stopGps();
                        }
                    });

                    Runnable runGPS = new Runnable() {
                        @Override
                        public void run() {
                            gps.setGpsProvider(TtAppCtx.getDeviceSettings().getGpsDeviceID());

                            switch (gps.startGps()) {
                                case InternalGpsStarted: {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            pd.setMessage("Internal GPS Started. Listening For Data.");
                                        }
                                    });
                                    break;
                                }
                                case InternalGpsNotEnabled: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The internal GPS is not enabled.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case InternalGpsNeedsPermissions: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The GpsService needs location permissions to use the internal GPS.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case InternalGpsError: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The was an error starting the Internal GPS.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case ExternalGpsStarted: {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            pd.setMessage("External GPS started. Listening for Data.");
                                        }
                                    });
                                    break;
                                }
                                case ExternalGpsNotFound: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The external bluetooth GPS was not found.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case ExternalGpsNotConnected: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The external bluetooth GPS is not connected.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case ExternalGpsError: {
                                    hideDialog.run();
                                    Toast.makeText(activity,"External GPS error.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case GpsAlreadyStarted: {
                                    hideDialog.run();
                                    Toast.makeText(activity,"GPS started. Listening for Data.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                default: hideDialog.run();
                                    break;
                            }
                        }
                    };

                    pd.show();

                    if (gps.isGpsRunning()) {
                        gps.stopGps();
                        new Handler().postDelayed(runGPS, 1000);
                    } else {
                        runGPS.run();
                    }
                } catch (Exception ex) {
                    TtUtils.TtReport.writeError(ex.getMessage(), "DeviceSettingsFragment:checkGPS");
                    Toast.makeText(getActivity(), "Unknown Error. See log for details.", Toast.LENGTH_SHORT).show();
                }
            }

            return false;
        }
    };
    //endregion

    //region RangeFinderCheck
    Preference.OnPreferenceClickListener rfCheckListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (StringEx.isEmpty(TtAppCtx.getDeviceSettings().getGpsDeviceID())) {
                Toast.makeText(getActivity(), "RangeFinder must first be selected", Toast.LENGTH_LONG).show();
            } else {
                try {
                    TtAppCtx.getDeviceSettings().setRangeFinderConfigured(false);

                    final Activity activity = getActivity();
                    final ProgressDialog pd = new ProgressDialog(activity);
                    final RangeFinderService.RangeFinderBinder rf = TtAppCtx.getRF();

                    prefRFCheck.setSummary(R.string.ds_rf_not_connected);

                    final RangeFinderService.Listener listener = new RangeFinderService.Listener() {
                        @Override
                        public void rfDataReceived(TtRangeFinderData rfData) {

                        }

                        @Override
                        public void rfStringReceived(String rfString) {
                            try {
                                rf.removeListener(this);

                                TtAppCtx.getDeviceSettings().setRangeFinderConfigured(true);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.setMessage(activity.getString(R.string.ds_rf_connected));

                                        if (TtAppCtx.getDeviceSettings().isRangeFinderAlwaysOn()) {
                                            prefRFCheck.setSummary(R.string.ds_rf_connected);
                                        } else {
                                            prefRFCheck.setSummary(R.string.ds_dev_configured);
                                            rf.stopRangeFinder();
                                        }
                                    }
                                });

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.hide();
                                    }
                                }, 1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void rfInvalidStringReceived(String rfString) {

                        }

                        @Override
                        public void rangeFinderStarted() {

                        }

                        @Override
                        public void rangeFinderStopped() {

                        }

                        @Override
                        public void rangeFinderServiceStarted() {

                        }

                        @Override
                        public void rangeFinderServiceStopped() {

                        }

                        @Override
                        public void rangeFinderError(RangeFinderService.RangeFinderError error) {
                            TtAppCtx.getDeviceSettings().setRangeFinderConfigured(false);

                            Toast.makeText(activity, error.toString(), Toast.LENGTH_SHORT).show();

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage(getString(R.string.ds_rf_not_connected));
                                    pd.hide();
                                }
                            });

                            rf.removeListener(this);
                            rf.stopRangeFinder();
                        }
                    };

                    rf.addListener(listener);

                    final Runnable hideDialog = new Runnable() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.hide();
                                }
                            });
                        }
                    };

                    pd.setMessage(getString(R.string.ds_rf_connecting));

                    pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            rf.stopRangeFinder();
                        }
                    });

                    Runnable runRF = new Runnable() {
                        @Override
                        public void run() {
                            rf.setRangeFinderProvider(TtAppCtx.getDeviceSettings().getRangeFinderDeviceID());

                            switch (rf.startRangeFinder()) {
                                case RangeFinderStarted:
                                case RangeFinderAlreadyStarted:
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            pd.setMessage("RangeFinder started. Listening for Data.");
                                        }
                                    });
                                    break;
                                case RangeFinderError: {
                                    hideDialog.run();
                                    Toast.makeText(activity,"RangeFinder error.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                case RangeFinderNotFound: {
                                    hideDialog.run();
                                    Toast.makeText(activity, "The external bluetooth RangeFinder was not found.", Toast.LENGTH_LONG).show();
                                    break;
                                }
                                default: hideDialog.run();
                                    break;
                            }
                        }
                    };

                    pd.show();

                    if (rf.isRangeFinderRunning()) {
                        rf.stopRangeFinder();
                        new Handler().postDelayed(runRF, 1000);
                    } else {
                        runRF.run();
                    }
                } catch (Exception ex) {
                    TtUtils.TtReport.writeError(ex.getMessage(), "DeviceSettingsFragment:checkRF");
                    Toast.makeText(getActivity(), "Unknown Error. See log for details.", Toast.LENGTH_SHORT).show();
                }
            }

            return false;
        }
    };
    //endregion


    //region External/Internal Switch & Listener
    Preference.OnPreferenceChangeListener useExternalListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean useExternal = (boolean)newValue;

            boolean success = false;

            if (useExternal) {
                if (AndroidUtils.App.requestPermission(getActivity(),
                        new String [] {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                        Consts.Codes.Requests.BLUETOOH,
                        "Bluetooth is required for connecting to the external GPS receiver.")) {
                    success = switchToExternal();
                }
            } else {
                if (AndroidUtils.App.requestPermission(getActivity(),
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        Consts.Codes.Requests.LOCATION,
                        "Location is required for accessing the internal GPS receiver.")) {
                    success = switchToInternal();
                }
            }

            if (success) {
                swtUseExGpsDev.setSummary(getString(useExternal ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));
            }

            return success;
        }
    };


    private boolean switchToExternal() {
        TtBluetoothManager btm = new TtBluetoothManager();
        TtAppCtx.getDeviceSettings().setGpsConfigured(false);
        prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

        if (btm.isAvailable()) {
            if (btm.isEnabled()) {
                setBTValues(prefLstGpsDevice);
                exGpsCat.setEnabled(true);
                return true;
            } else {
                //bluetooth isn't turned on, request that it should be
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            //no bluetooth option on device
            Toast.makeText(TtAppCtx, R.string.ds_no_bt, Toast.LENGTH_LONG).show();
        }

        return false;
    }

    private boolean switchToInternal() {
        if (!TtAppCtx.getGps().isInternalGpsEnabled()) {

            //DialogFragmentEx
            AndroidUtils.App.requestLocationPermission(getActivity(), Consts.Codes.Services.REQUEST_GPS_SERVICE);
        } else {
            TtAppCtx.getGps().stopGps();
            TtAppCtx.getGps().setGpsProvider(null);

            TtAppCtx.getDeviceSettings().setGpsConfigured(true);
            exGpsCat.setEnabled(false);
            return true;
        }

        return false;
    }
    //endregion


    //region GPS Selection
    Preference.OnPreferenceChangeListener btnGPSList = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            try {
                String[] values = newValue.toString().split(",");

                if (values.length > 0) {
                    if (!TtAppCtx.getDeviceSettings().getGpsDeviceID().equals(values[0])) {
                        TtAppCtx.getDeviceSettings().setGpsDeviceId(values[0]);
                        TtAppCtx.getDeviceSettings().setGpsDeviceName(values[1]);

                        prefLstGpsDevice.setSummary(values[1]);
                        prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
                        TtAppCtx.getDeviceSettings().setGpsConfigured(false);
                    }
                } else {
                    TtAppCtx.getDeviceSettings().setGpsDeviceId(StringEx.Empty);
                    TtAppCtx.getDeviceSettings().setGpsDeviceName(StringEx.Empty);

                    prefLstGpsDevice.setSummary(getString(R.string.ds_no_dev));
                    prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
                    TtAppCtx.getDeviceSettings().setGpsConfigured(false);
                }
            } catch (Exception ex) {
                TtUtils.TtReport.writeError(ex.getMessage(), "DeviceSettingsFragment:btnGPSList");
            }

            return true;
        }
    };
    //endregion

    //region RangeFinder Selection
    Preference.OnPreferenceChangeListener btnRFList = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            try {
                String[] values = newValue.toString().split(",");

                if (values.length > 0) {
                    if (!TtAppCtx.getDeviceSettings().getRangeFinderDeviceID().equals(values[0])) {
                        TtAppCtx.getDeviceSettings().setRangeFinderDeviceId(values[0]);
                        TtAppCtx.getDeviceSettings().setRangeFinderDeviceName(values[1]);

                        prefLstRFDevice.setSummary(values[1]);
                        prefRFCheck.setSummary(R.string.ds_dev_not_configured);
                        TtAppCtx.getDeviceSettings().setRangeFinderConfigured(false);
                    }
                } else {
                    TtAppCtx.getDeviceSettings().setRangeFinderDeviceId(StringEx.Empty);
                    TtAppCtx.getDeviceSettings().setRangeFinderDeviceName(StringEx.Empty);

                    prefLstRFDevice.setSummary(getString(R.string.ds_no_dev));
                    prefRFCheck.setSummary(R.string.ds_dev_not_configured);
                    TtAppCtx.getDeviceSettings().setRangeFinderConfigured(false);
                }
            } catch (Exception e) {
                //
            }

            return true;
        }
    };
    //endregion


    //region Check NMEA
    Preference.OnPreferenceClickListener checkNmeaListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (TtAppCtx.getDeviceSettings().isGpsConfigured()) {
                CheckNmeaDialog.newInstance().show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "CHECK_NMEA");
            } else {
                new AlertDialog.Builder(getActivity())
                        .setMessage("GPS needs to be configured before checking for its NMEA configuration.")
                        .setPositiveButton(R.string.str_ok, null)
                        .show();
            }

            return false;
        }
    };
    //endregion


    //region MetaSelection
    DontAskAgainDialog.OnClickListener setMetaListener = new DontAskAgainDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i, Object value) {
            String receiver = TtAppCtx.getDeviceSettings().getGpsDeviceName();

            if ((int)value == 1) {
                TtMetadata metadata = TtAppCtx.getDAL().getDefaultMetadata();

                if (metadata != null) {
                    metadata.setGpsReceiver(receiver);
                    TtAppCtx.getDAL().updateMetadata(metadata);
                    TtAppCtx.getMetadataSettings().setReceiver(receiver);
                }
            } else if ((int)value == 2) {
                List<TtMetadata> metas = TtAppCtx.getDAL().getMetadata();

                if (metas.size() > 0) {
                    for (TtMetadata meta : metas) {
                        meta.setGpsReceiver(receiver);
                        TtAppCtx.getDAL().updateMetadata(meta);
                    }
                }
            }
        }
    };
    //endregion
}
