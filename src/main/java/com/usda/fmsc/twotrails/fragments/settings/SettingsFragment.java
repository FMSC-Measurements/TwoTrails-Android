package com.usda.fmsc.twotrails.fragments.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.preferences.ListCompatPreference;
import com.usda.fmsc.android.preferences.SwitchCompatPreference;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.activities.SettingsActivity;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.dialogs.CheckNmeaDialog;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.SettingsLogic;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.utilities.StringEx;

public class SettingsFragment extends PreferenceFragment {
    public static final String CURRENT_PAGE = "CurrentPage";

    private GpsService.GpsBinder binder;
    private Preference prefGpsCheck;
    private SwitchCompatPreference swtUseExDev;
    private PreferenceCategory exGpsCat;
    private ListCompatPreference perfLstGpsDevice;

    private String moveToPage;

    private int stringRecvCount = 0;


    public static SettingsFragment newInstance(String currPageKey) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(CURRENT_PAGE, currPageKey);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        addPreferencesFromResource(R.xml.settings);

        if (bundle != null && bundle.containsKey(CURRENT_PAGE)) {
            moveToPage = bundle.getString(CURRENT_PAGE);

            ActionBar actionBar = ((SettingsActivity)getActivity()).getSupportActionBar();

            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(getPreferenceScreen().getTitle());

                if (moveToPage != null) {
                    switch (moveToPage) {
                        case SettingsActivity.GPS_SETTINGS_PAGE:
                            actionBar.setTitle("GPS Setup");
                            break;
                        case SettingsActivity.LASER_SETTINGS_PAGE:
                            actionBar.setTitle("Range Finder Setup");
                            break;
                        case SettingsActivity.FILTER_GPS_SETTINGS_PAGE:
                            actionBar.setTitle("GPS Point Settings");
                            break;
                        case SettingsActivity.FILTER_TAKE5_SETTINGS_PAGE:
                            actionBar.setTitle("Take5 Point Settings");
                            break;
                        case SettingsActivity.FILTER_WALK_SETTINGS_PAGE:
                            actionBar.setTitle("Walk Point Settings");
                            break;
                        case SettingsActivity.MAP_SETTINGS_PAGE:
                            actionBar.setTitle("Map Options");
                            break;
                        case SettingsActivity.DIALOG_SETTINGS_PAGE:
                            actionBar.setTitle("Dialog Options");
                            break;
                        case SettingsActivity.MISC_SETTINGS_PAGE:
                            actionBar.setTitle("Misc Settings");
                            break;
                    }
                } else {
                    actionBar.setTitle(R.string.str_settings);
                }
            }
        }

        swtUseExDev = (SwitchCompatPreference)findPreference(Global.Settings.DeviceSettings.GPS_EXTERNAL);
        exGpsCat = (PreferenceCategory)findPreference(getString(R.string.set_GPS_CAT));
        perfLstGpsDevice = (ListCompatPreference)findPreference(getString(R.string.set_GPS_LIST_DEVICE));
        prefGpsCheck = findPreference(getString(R.string.set_GPS_CHECK));
        Preference prefClearLog = findPreference(getString(R.string.set_CLEAR_LOG));
        Preference prefExportReport = findPreference(getString(R.string.set_EXPORT_REPORT));
        Preference prefResetDevice = findPreference(getString(R.string.set_RESET));
        Preference prefCheckNmea = findPreference(getString(R.string.set_GPS_CHECK_NMEA));
        Preference prefCode = findPreference(getString(R.string.set_CODE));

        binder = Global.getGpsBinder();

        swtUseExDev.setOnPreferenceChangeListener(useExternalListener);
        swtUseExDev.setSummary(getString(swtUseExDev.isChecked() ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));

        prefGpsCheck.setOnPreferenceClickListener(gpsCheckListener);

        perfLstGpsDevice.setOnPreferenceChangeListener(btGPSList);

        prefResetDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.reset(getActivity());
                return false;
            }
        });

        prefClearLog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.clearLog(getActivity());
                return false;
            }
        });

        prefCheckNmea.setOnPreferenceClickListener(checkNmeaListener);

        prefExportReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.exportReport(getActivity());
                return false;
            }
        });

        prefCode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.enterCode(getActivity());
                return false;
            }
        });

        //get initial bluetooth devices
        setBTValues(perfLstGpsDevice);

        exGpsCat.setEnabled(Global.Settings.DeviceSettings.getGpsExternal());

        String devName = Global.Settings.DeviceSettings.getGpsDeviceName();
        if(StringEx.isEmpty(devName)) {
            perfLstGpsDevice.setSummary(R.string.ds_no_dev);
        } else {
            perfLstGpsDevice.setSummary(devName);
        }

        if (Global.Settings.DeviceSettings.isGpsConfigured()) {
            if (Global.getGpsBinder().isGpsRunning()) {
                prefGpsCheck.setSummary(R.string.ds_gps_connected);
            } else {
                prefGpsCheck.setSummary(R.string.ds_dev_configured);
            }
        } else {
            prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (moveToPage != null && !moveToPage.equals(SettingsActivity.MAIN_SETTINGS_PAGE)) {
            setPreferenceScreen((PreferenceScreen) findPreference(moveToPage));
        }
    }

    private void setBTValues(ListPreference perfLstGpsDevice) {
        TtBluetoothManager btm = Global.getBluetoothManager();

        try {
            if (btm.isEnabled() && btm.isAvailable()) {
                Set<BluetoothDevice> btds = btm.getAdapter().getBondedDevices();

                List<String> deviceNames = new ArrayList<>();
                List<String> deviceIDs = new ArrayList<>();

                for (BluetoothDevice btd : btds) {
                    deviceNames.add(btd.getName());
                    deviceIDs.add(String.format("%s,%s", btd.getAddress(), btd.getName()));
                }


                perfLstGpsDevice.setEntries(deviceNames.toArray(new String[deviceNames.size()]));
                perfLstGpsDevice.setEntryValues(deviceIDs.toArray(new String[deviceIDs.size()]));
            }
        } catch (Exception e) {
            //
        }
    }


    //region GPS Check
    Preference.OnPreferenceClickListener gpsCheckListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            try {
                Global.Settings.DeviceSettings.setGpsConfigured(false);

                final ProgressDialog pd = new ProgressDialog(getActivity());


                prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

                stringRecvCount = 0;

                new Thread(new Runnable() {

                    final Activity activity = getActivity();

                    @Override
                    public void run() {
                        pd.setMessage(getString(R.string.ds_gps_connecting));

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.show();
                            }
                        });

                        final GpsService.GpsBinder binder = Global.getGpsBinder();

                        binder.stopGps();

                        binder.setGpsProvider(Global.Settings.DeviceSettings.getGpsDeviceID());


                        GpsService.Listener listener = new GpsService.Listener() {
                            @Override
                            public void nmeaBurstReceived(INmeaBurst nmeaBurst) {

                            }

                            @Override
                            public void nmeaStringReceived(String nmeaString) {
                                try {
                                    binder.removeListener(this);

                                    Global.Settings.DeviceSettings.setGpsConfigured(true);

                                    if (1 > stringRecvCount++) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pd.setMessage(getString(R.string.ds_gps_connected));

                                                if (Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                                                    prefGpsCheck.setSummary(R.string.ds_gps_connected);
                                                } else {
                                                    prefGpsCheck.setSummary(R.string.ds_dev_configured);
                                                    binder.stopGps();
                                                }

                                                if (Global.Settings.DeviceSettings.getAutoSetGpsNameToMetaAsk()) {
                                                    DontAskAgainDialog dialog = new DontAskAgainDialog(getActivity(),
                                                            Global.Settings.DeviceSettings.AUTO_SET_GPS_NAME_TO_META_ASK,
                                                            Global.Settings.DeviceSettings.AUTO_SET_GPS_NAME_TO_META,
                                                            Global.Settings.PreferenceHelper.getPrefs());

                                                    dialog.setMessage("Do you want to update metadata with the current GPS receiver?");

                                                    dialog.setPositiveButton("Default", setMetaListener, 1);

                                                    if (Global.getDAL() != null)
                                                        dialog.setNegativeButton("All", setMetaListener, 2);

                                                    dialog.setNeutralButton("None", null, 0);

                                                    dialog.show();
                                                } else {
                                                    setMetaListener.onClick(null, 0, Global.Settings.DeviceSettings.getAutoSetGpsNameToMeta());
                                                }
                                            }
                                        });


                                        Thread.sleep(1000);

                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pd.hide();
                                            }
                                        });
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
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
                                Global.Settings.DeviceSettings.setGpsConfigured(false);

                                Toast.makeText(activity, error.toString(), Toast.LENGTH_SHORT).show();

                                activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            pd.setMessage(getString(R.string.ds_gps_not_connected));
                                            pd.hide();
                                        }
                                    });
                            }
                        };

                        binder.addListener(listener);

                        binder.startGps();
                    }
                }).start();

            } catch (Exception ex) {
                TtUtils.TtReport.writeError(ex.getMessage(), "SettingsFragment:checkGPS");
                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
            }

            return false;
        }
    };
    //endregion

    //region External Switch
    Preference.OnPreferenceChangeListener useExternalListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean useExternal = (boolean)newValue;
            swtUseExDev.setSummary(getString(useExternal ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));

            if (useExternal) {
                TtBluetoothManager btm = Global.getBluetoothManager();
                Global.Settings.DeviceSettings.setGpsConfigured(false);
                prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

                if (btm.isAvailable()) {
                    if (btm.isEnabled()) {
                        setBTValues(perfLstGpsDevice);
                        exGpsCat.setEnabled(true);
                        return true;
                    } else {
                        //bluetooth isnt turned on, request that it should be
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                } else {
                    //no bluetooth option on device
                    Toast.makeText(getActivity().getApplicationContext(), R.string.ds_no_bt, Toast.LENGTH_LONG).show();
                }
            } else {
                binder.stopGps();
                binder.setGpsProvider(null);

                Global.Settings.DeviceSettings.setGpsConfigured(true);
                exGpsCat.setEnabled(false);
                return true;
            }

            return false;
        }
    };
    //endregion


    //region External GPS List
    Preference.OnPreferenceChangeListener btGPSList = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            try {
                String[] values = newValue.toString().split(",");

                if (values.length > 0) {

                    if (!Global.Settings.DeviceSettings.getGpsDeviceID().equals(values[0])) {
                        Global.Settings.DeviceSettings.setGpsDeviceId(values[0]);
                        Global.Settings.DeviceSettings.setGpsDeviceName(values[1]);

                        perfLstGpsDevice.setSummary(values[1]);
                        Global.Settings.DeviceSettings.setGpsConfigured(false);
                    }
                } else {
                    Global.Settings.DeviceSettings.setGpsDeviceId(StringEx.Empty);
                    Global.Settings.DeviceSettings.setGpsDeviceName(StringEx.Empty);

                    perfLstGpsDevice.setSummary(getString(R.string.ds_no_dev));
                    Global.Settings.DeviceSettings.setGpsConfigured(false);
                }
            } catch (Exception e) {
                //
            }

            return true;
        }
    };
    //endregion


    Preference.OnPreferenceClickListener checkNmeaListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (Global.Settings.DeviceSettings.isGpsConfigured()) {
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

    DontAskAgainDialog.OnClickListener setMetaListener = new DontAskAgainDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i, Object value) {
            String receiver = Global.Settings.DeviceSettings.getGpsDeviceName();

            if (value == 1) {
                TtMetadata metadata = Global.getDAL().getDefaultMetadata();

                if (metadata != null) {
                    metadata.setGpsReceiver(receiver);
                    Global.getDAL().updateMetadata(metadata);
                    Global.Settings.MetaDataSetting.setReceiver(receiver);
                }
            } else if (value == 2) {
                List<TtMetadata> metas = Global.getDAL().getMetadata();

                if (metas.size() > 0) {
                    for (TtMetadata meta : metas) {
                        meta.setGpsReceiver(receiver);
                        Global.getDAL().updateMetadata(meta);
                    }
                }
            }
        }
    };
}
