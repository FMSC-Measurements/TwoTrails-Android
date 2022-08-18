package com.usda.fmsc.twotrails.fragments.settings;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.dialogs.ProgressDialogEx;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.BuildConfig;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.SettingsActivity;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.dialogs.CheckNmeaDialogTt;
import com.usda.fmsc.twotrails.fragments.TtBasePrefFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.rangefinder.TtRangeFinderData;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class DeviceSettingsFragment extends TtBasePrefFragment {
    public static final String CURRENT_PAGE = "CurrentPage";

    private Preference prefGpsCheck, prefRFCheck;
    private SwitchPreferenceCompat swtUseExGpsDev;
    private PreferenceCategory exGpsCat;
    private ListPreference prefLstGpsDevice, prefLstRFDevice;

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
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Bundle bundle = getArguments();

        addPreferencesFromResource(R.xml.pref_device_setup);

        if (bundle != null && bundle.containsKey(CURRENT_PAGE)) {
            moveToPage = bundle.getString(CURRENT_PAGE);

            TtCustomToolbarActivity activity = (TtCustomToolbarActivity)getActivity();
            if (activity != null) {
                ActionBar actionBar = activity.getSupportActionBar();

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
        }

        swtUseExGpsDev = findPreference(getString(R.string.set_GPS_EXTERNAL));
        exGpsCat = findPreference(getString(R.string.set_GPS_CAT));
        prefLstGpsDevice = findPreference(getString(R.string.set_GPS_LIST_DEVICE));
        prefGpsCheck = findPreference(getString(R.string.set_GPS_CHECK));
        prefLstRFDevice = findPreference(getString(R.string.set_RF_LIST_DEVICE));
        prefRFCheck = findPreference(getString(R.string.set_RF_CHECK));
        Preference prefCheckNmea = findPreference(getString(R.string.set_GPS_CHECK_NMEA));

        swtUseExGpsDev.setOnPreferenceChangeListener(useExternalListener);
        swtUseExGpsDev.setSummary(getString(swtUseExGpsDev.isChecked() ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));

        prefGpsCheck.setOnPreferenceClickListener(gpsCheckListener);
        prefRFCheck.setOnPreferenceClickListener(rfCheckListener);

        prefLstGpsDevice.setOnPreferenceChangeListener(btnGPSList);
        prefLstRFDevice.setOnPreferenceChangeListener(btnRFList);

        if (prefCheckNmea != null)
            prefCheckNmea.setOnPreferenceClickListener(checkNmeaListener);

        //get initial bluetooth devices
        setBTValues(prefLstGpsDevice);
        setBTValues(prefLstRFDevice);

        exGpsCat.setEnabled(getTtAppCtx().getDeviceSettings().getGpsExternal());

        boolean requireBluetooth = false;

        String devName = getTtAppCtx().getDeviceSettings().getGpsDeviceName();
        if (StringEx.isEmpty(devName)) {
            prefLstGpsDevice.setSummary(R.string.ds_no_dev);
        } else {
            prefLstGpsDevice.setSummary(devName);
            requireBluetooth = true;
        }

        if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            if (getTtAppCtx().isGpsServiceStartedAndRunning()) {
                prefGpsCheck.setSummary(R.string.ds_gps_connected);
            } else {
                prefGpsCheck.setSummary(R.string.ds_dev_configured);
            }
        } else {
            prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
        }

        devName = getTtAppCtx().getDeviceSettings().getRangeFinderDeviceName();
        if (StringEx.isEmpty(devName)) {
            prefLstRFDevice.setSummary(R.string.ds_no_dev);
        } else {
            prefLstRFDevice.setSummary(devName);
            requireBluetooth = true;
        }

        if (getTtAppCtx().getDeviceSettings().isRangeFinderConfigured()) {
            if (getTtAppCtx().getRF().isRangeFinderRunning()) {
                prefRFCheck.setSummary(R.string.ds_rf_connected);
            } else {
                prefRFCheck.setSummary(R.string.ds_dev_configured);
            }
        } else {
            prefRFCheck.setSummary(R.string.ds_dev_not_configured);
        }

        if (requireBluetooth) {
            if (!AndroidUtils.App.checkBluetoothScanAndConnectPermission(getActivity())) {
                requestBluetoothPermission();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (moveToPage != null && !moveToPage.equals(SettingsActivity.MAIN_SETTINGS_PAGE)) {
            setPreferenceScreen(findPreference(moveToPage));
        }
    }

    private final ActivityResultLauncher<String[]> requestBluetoothPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (TtUtils.Collections.areAllTrue(result.values())) {
            setBTValues(prefLstGpsDevice);
            setBTValues(prefLstRFDevice);
            swtUseExGpsDev.setChecked(true);
            switchToExternal();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Toast.makeText(getActivity(), "Requires Bluetooth and Nearby Device Permission", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Requires Bluetooth Permission", Toast.LENGTH_LONG).show();
            }
        }
    });

    private boolean requestBluetoothPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
            AndroidUtils.App.requestBluetoothScanPermission(getActivity(), requestBluetoothPermissionOnResult, "Bluetooth is required to find and connect to nearby devices.") :
            AndroidUtils.App.requestBluetoothPermission(getActivity(), requestBluetoothPermissionOnResult, "Bluetooth is required to find and connect to nearby devices.");
    }


    private final ActivityResultLauncher<String[]> requestInternalLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            onLocationRequestResult(TtUtils.Collections.areAllTrue(result.values())));

    private final ActivityResultLauncher<String> requestBackgroundLocationPermissionOnResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onBackgroundLocationRequestResult);



    private void onLocationRequestResult(boolean hasPermissions) {
        if (hasPermissions) {
            swtUseExGpsDev.setChecked(false);
            switchToInternal();

            AndroidUtils.App.requestBackgroundLocationPermission(getActivity(),
                    requestBackgroundLocationPermissionOnResult,
                    getString(R.string.diag_back_loc));
        } else {
            Toast.makeText(getActivity(), "Requires GPS permissions to use the internal GPS receiver", Toast.LENGTH_LONG).show();
        }
    }
    private void onBackgroundLocationRequestResult(boolean hasPermissions) {
        if (!(hasPermissions || getTtAppCtx().getDeviceSettings().getKeepScreenOn())) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.diag_keep_on_req)
                    .setPositiveButton(R.string.str_ok, (dialog, which) -> {
                        getTtAppCtx().getDeviceSettings().setKeepScreenOn(true);
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        }
    }

    private boolean requestInternalGpsPermission() {
        return AndroidUtils.App.requestLocationPermission(getActivity(),
                requestInternalLocationPermissionOnResult,
                getString(R.string.diag_loc));
    }

    private void setBTValues(ListPreference lstPref) {
        if (AndroidUtils.App.checkBluetoothScanAndConnectPermission(getActivity())) {
            TtBluetoothManager btm = getTtAppCtx().getBluetoothManager();

            try {
                if (btm.isEnabled() && btm.isAvailable()) {
                    List<String> deviceNames = new ArrayList<>();
                    List<String> deviceIDs = new ArrayList<>();

                    for (BluetoothDevice btd : btm.getAdapter().getBondedDevices()) {
                        deviceNames.add(btd.getName());
                        deviceIDs.add(String.format("%s,%s", btd.getAddress(), btd.getName()));
                    }

                    lstPref.setEntries(deviceNames.toArray(new String[0]));
                    lstPref.setEntryValues(deviceIDs.toArray(new String[0]));
                }
            } catch (Exception e) {
                //
            }
        } else {
            lstPref.setEntries(new String[0]);
            lstPref.setEntryValues(new String[0]);
        }
    }

    //region GPSCheck
    private final Preference.OnPreferenceClickListener gpsCheckListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            if (StringEx.isEmpty(getTtAppCtx().getDeviceSettings().getGpsDeviceID())) {
                Toast.makeText(getActivity(), "GPS must first be selected", Toast.LENGTH_LONG).show();
            } else {
                try {
                    getTtAppCtx().getDeviceSettings().setGpsConfigured(false);

                    final Activity activity = getActivity();

                    if (activity == null) {
                        Toast.makeText(getActivity(), "An error has occurred. Please see log for details.", Toast.LENGTH_LONG).show();
                        getTtAppCtx().getReport().writeError("Null Activity", "DeviceSettingsFragment:gpsCheckListener");
                        return false;
                    }

                    if (!getTtAppCtx().isGpsServiceStarted()) {
                        getTtAppCtx().startGpsService();
                        Toast.makeText(getActivity(), "GPS Service not available. Please try again.", Toast.LENGTH_LONG).show();
                        getTtAppCtx().getReport().writeError("GPS Service Not Started", "DeviceSettingsFragment:gpsCheckListener");
                        return false;
                    }

                    final ProgressDialogEx pd = new ProgressDialogEx(activity);
                    final GpsService.GpsBinder gps = getTtAppCtx().getGps();

                    prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

                    final GpsService.Listener listener = new GpsService.Listener() {
                        @Override
                        public void nmeaBurstReceived(NmeaBurst nmeaBurst) {

                        }

                        @Override
                        public void nmeaStringReceived(String nmeaString) {
                            gps.removeListener(this);

                            getTtAppCtx().getDeviceSettings().setGpsConfigured(true);

                            activity.runOnUiThread(() -> {
                                pd.setMessage(activity.getString(R.string.ds_gps_connected));

                                if (getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                                    prefGpsCheck.setSummary(R.string.ds_gps_connected);
                                } else {
                                    prefGpsCheck.setSummary(R.string.ds_dev_configured);
                                    gps.stopGps();
                                }

                                if (getTtAppCtx().getDeviceSettings().getAutoSetGpsNameToMetaAsk()) {
                                    if (lastMetaAsk.isBefore(DateTime.now().minusSeconds(10)) && getTtAppCtx().hasDAL()) {
                                        DontAskAgainDialog dialog = new DontAskAgainDialog(getActivity(),
                                                DeviceSettings.AUTO_SET_GPS_NAME_TO_META_ASK,
                                                DeviceSettings.AUTO_SET_GPS_NAME_TO_META,
                                                getTtAppCtx().getDeviceSettings().getPrefs());

                                        dialog.setMessage("GPS is connected. Do you want to update metadata with the current GPS receiver?");

                                        dialog.setPositiveButton("Default", setMetaListener, 1);

                                        if (getTtAppCtx().hasDAL())
                                            dialog.setNegativeButton("All", setMetaListener, 2);

                                        dialog.setNeutralButton("No", null, 0);

                                        dialog.show();

                                        lastMetaAsk = DateTime.now();

                                        pd.dismiss();
                                    } else {
                                        new Handler().postDelayed(pd::dismiss, 1000);
                                    }
                                } else {
                                    setMetaListener.onClick(null, 0, getTtAppCtx().getDeviceSettings().getAutoSetGpsNameToMeta());
                                    new Handler().postDelayed(pd::dismiss, 1000);
                                }
                            });
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

                        }

                        @Override
                        public void gpsStarted() {
                            if (gps.isExternalGpsUsed()) {
                                activity.runOnUiThread(() -> {
                                    pd.setMessage("External GPS Connected. Listening for data.");
                                });
                            }
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

                            getTtAppCtx().getDeviceSettings().setGpsConfigured(false);

                            String message;

                            switch (error) {
                                case LostDeviceConnection: message = "Lost connection to bluetooth device."; break;
                                case DeviceConnectionEnded: message = "Bluetooth connection terminated."; break;
                                case NoExternalGpsSocket: message = "Failed to create bluetooth socket."; break;
                                case FailedToConnect: message = "Failed to connect to bluetooth device."; break;
                                case Unknown: message = error.toString(); break;
                                default: message = "An Unknown GPS error as occurred"; break;
                            }

                            activity.runOnUiThread(pd::dismiss);

                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        }
                    };

                    gps.addListener(listener);

                    final Runnable hideDialog = () -> activity.runOnUiThread(pd::dismiss);

                    pd.setMessage(getString(R.string.ds_gps_connecting));

                    pd.setOnDismissListener(dialog -> gps.stopGps());

                    Runnable runGPS = () -> {
                        gps.stopGps();
                        gps.setGpsProvider(getTtAppCtx().getDeviceSettings().getGpsDeviceID());

                        switch (gps.startGps()) {
                            case InternalGpsStarted: {
                                activity.runOnUiThread(() -> pd.setMessage("Internal GPS started. Listening for data.."));
                                break;
                            }
                            case InternalGpsNotEnabled: {
                                hideDialog.run();
                                Toast.makeText(activity, "The Internal GPS is not enabled.", Toast.LENGTH_LONG).show();
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
                                activity.runOnUiThread(() -> pd.setMessage("External GPS started. Listening for data.."));
                                break;
                            }
                            case ExternalGpsConnecting: {
                                activity.runOnUiThread(() -> pd.setMessage("Connecting to external GPS."));
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
                                Toast.makeText(activity,"GPS started. Listening for data.", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }

                        activity.runOnUiThread(pd::show);
                    };

                    if (gps.isGpsRunning()) {
                        gps.stopGps();
                        new Handler().postDelayed(runGPS, 1000);
                    } else {
                        runGPS.run();
                    }
                } catch (Exception ex) {
                    getTtAppCtx().getReport().writeError(ex.getMessage(), "DeviceSettingsFragment:checkGPS");
                    Toast.makeText(getActivity(), "Unknown Error. See log for details.", Toast.LENGTH_SHORT).show();
                }
            }

            return false;
        }
    };
    //endregion

    //region RangeFinderCheck
    private final Preference.OnPreferenceClickListener rfCheckListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            final DeviceSettings ds = getTtAppCtx().getDeviceSettings();

            if (StringEx.isEmpty(ds.getRangeFinderDeviceID())) {
                Toast.makeText(getActivity(), "RangeFinder must first be selected", Toast.LENGTH_LONG).show();
            } else {
                try {
                    ds.setRangeFinderConfigured(false);

                    final Activity activity = getActivity();

                    if (activity == null) {
                        getTtAppCtx().getReport().writeError("null Activity", "DeviceSettingsFragment:rfCheckListener");
                        return false;
                    }

                    final ProgressDialogEx pd = new ProgressDialogEx(activity);
                    final RangeFinderService.RangeFinderBinder rf = getTtAppCtx().getRF();

                    prefRFCheck.setSummary(R.string.ds_rf_not_connected);

                    final RangeFinderService.Listener listener = new RangeFinderService.Listener() {
                        @Override
                        public void rfDataReceived(TtRangeFinderData rfData) {

                        }

                        @Override
                        public void rfStringReceived(String rfString) {
                            try {
                                rf.removeListener(this);

                                ds.setRangeFinderConfigured(true);

                                activity.runOnUiThread(() -> {
                                    pd.setMessage(activity.getString(R.string.ds_rf_connected));

                                    if (getTtAppCtx().getDeviceSettings().isRangeFinderAlwaysOn()) {
                                        prefRFCheck.setSummary(R.string.ds_rf_connected);
                                    } else {
                                        prefRFCheck.setSummary(R.string.ds_dev_configured);
                                        rf.stopRangeFinder();
                                    }
                                });

                                new Handler().postDelayed(pd::dismiss, 1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void rfInvalidStringReceived(String rfString) {

                        }

                        @Override
                        public void rangeFinderStarted() {
                            getActivity().runOnUiThread(() -> pd.setMessage("RangeFinder Connected. Listening for data.."));
                        }

                        @Override
                        public void rangeFinderStopped() {

                        }

                        @Override
                        public void rangeFinderConnecting() {

                        }

                        @Override
                        public void rangeFinderServiceStarted() {

                        }

                        @Override
                        public void rangeFinderServiceStopped() {

                        }

                        @Override
                        public void rangeFinderError(RangeFinderService.RangeFinderError error) {
                            rf.removeListener(this);
                            rf.stopRangeFinder();

                            String message;

                            switch (error) {
                                case LostDeviceConnection: message = "Lost connection to bluetooth device."; break;
                                case DeviceConnectionEnded: message = "Bluetooth connection terminated."; break;
                                case NoExternalRangeFinderSocket: message = "Failed to create bluetooth socket."; break;
                                case FailedToConnect: message = "Failed to connect to bluetooth device."; break;
                                case Unknown: message = error.toString(); break;
                                default: message = "An unknown RangeFinder error as occurred"; break;
                            }

                            activity.runOnUiThread(pd::dismiss);

                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        }
                    };

                    rf.addListener(listener);

                    final Runnable hideDialog = () -> activity.runOnUiThread(pd::dismiss);

                    pd.setMessage(getString(R.string.ds_rf_connecting));

                    pd.setOnDismissListener(dialog -> rf.stopRangeFinder());

                    Runnable runRF = () -> {
                        rf.setRangeFinderProvider(ds.getRangeFinderDeviceID());

                        switch (rf.startRangeFinder()) {
                            case RangeFinderStarted:
                            case RangeFinderAlreadyStarted:
                                activity.runOnUiThread(() -> pd.setMessage("RangeFinder started. Listening for Data."));
                                break;
                            case RangeFinderConnecting: {
                                Toast.makeText(getActivity(), "Connecting to RangeFinder.", Toast.LENGTH_SHORT).show();
                                //activity.runOnUiThread(() -> pd.setMessage("Connecting to RangeFinder."));
                                break;
                            }
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
                    };

                    pd.show();

                    if (rf.isRangeFinderRunning()) {
                        rf.stopRangeFinder();
                        new Handler().postDelayed(runRF, 1000);
                    } else {
                        runRF.run();
                    }
                } catch (Exception ex) {
                    getTtAppCtx().getReport().writeError(ex.getMessage(), "DeviceSettingsFragment:checkRF");
                    Toast.makeText(getActivity(), "Unknown Error. See log for details.", Toast.LENGTH_SHORT).show();
                }
            }

            return false;
        }
    };
    //endregion


    //region External/Internal Switch & Listener
    private final Preference.OnPreferenceChangeListener useExternalListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            boolean useExternal = (boolean)newValue;

            boolean success;

            if (useExternal) {
                success = requestBluetoothPermission();

                if (success) {
                    switchToExternal();
                }
            } else {
                success = requestInternalGpsPermission();

                if (success) {
                    switchToInternal();
                }
            }

            if (success) {
                swtUseExGpsDev.setSummary(getString(useExternal ? R.string.ds_gps_use_external : R.string.ds_gps_use_internal));
            }

            return success;
        }
    };


    private void switchToExternal() {
        if (AndroidUtils.App.checkBluetoothPermission(getContext())) {
            TtBluetoothManager btm = getTtAppCtx().getBluetoothManager();
            getTtAppCtx().getDeviceSettings().setGpsConfigured(false);
            prefGpsCheck.setSummary(R.string.ds_gps_not_connected);

            if (btm.isAvailable()) {
                if (btm.isEnabled()) {
                    setBTValues(prefLstGpsDevice);
                    exGpsCat.setEnabled(true);
                } else {
                    //bluetooth isn't turned on, request that it should be
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } else {
                //no bluetooth option on device
                Toast.makeText(getTtAppCtx(), R.string.ds_no_bt, Toast.LENGTH_LONG).show();
            }
        } else {
            requestBluetoothPermission();
        }
    }

    private void switchToInternal() {
        if (!getTtAppCtx().isGpsServiceStarted()) {
            getTtAppCtx().startGpsService();
            Toast.makeText(getActivity(), "GPS Service not available. Please try again.", Toast.LENGTH_LONG).show();
            getTtAppCtx().getReport().writeError("GPS Service Not Started", "DeviceSettingsFragment:switchToInternal");
            return;
        }

        if (!getTtAppCtx().getGps().isInternalGpsEnabled()) {
            requestInternalGpsPermission();
        } else {
            getTtAppCtx().getGps().stopGps();
            getTtAppCtx().getGps().setGpsProvider(null);

            getTtAppCtx().getDeviceSettings().setGpsConfigured(true);
            exGpsCat.setEnabled(false);

            getTtAppCtx().getDeviceSettings().setGpsDeviceId(StringEx.Empty);
            getTtAppCtx().getDeviceSettings().setGpsDeviceName(StringEx.Empty);

            if (getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                getTtAppCtx().getGps().startGps();
            }
        }
    }
    //endregion


    //region GPS Selection
    private final Preference.OnPreferenceChangeListener btnGPSList = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

            try {
                String[] values = newValue.toString().split(",");

                if (values.length > 0) {
                    if (!getTtAppCtx().getDeviceSettings().getGpsDeviceID().equals(values[0])) {
                        getTtAppCtx().getDeviceSettings().setGpsDeviceId(values[0]);
                        getTtAppCtx().getDeviceSettings().setGpsDeviceName(values[1]);

                        prefLstGpsDevice.setSummary(values[1]);
                        prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
                        getTtAppCtx().getDeviceSettings().setGpsConfigured(false);

                        TtUtils.Misc.verifyGpsDevice(values[1], values[0], getActivity());
                    }
                } else {
                    getTtAppCtx().getDeviceSettings().setGpsDeviceId(StringEx.Empty);
                    getTtAppCtx().getDeviceSettings().setGpsDeviceName(StringEx.Empty);

                    prefLstGpsDevice.setSummary(getString(R.string.ds_no_dev));
                    prefGpsCheck.setSummary(R.string.ds_dev_not_configured);
                    getTtAppCtx().getDeviceSettings().setGpsConfigured(false);
                }
            } catch (Exception ex) {
                getTtAppCtx().getReport().writeError(ex.getMessage(), "DeviceSettingsFragment:btnGPSList");
            }

            return true;
        }
    };
    //endregion

    //region RangeFinder Selection
    private final Preference.OnPreferenceChangeListener btnRFList = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            try {
                String[] values = newValue.toString().split(",");

                final DeviceSettings ds = getTtAppCtx().getDeviceSettings();

                if (values.length > 0) {
                    if (!ds.getRangeFinderDeviceID().equals(values[0])) {
                        ds.setRangeFinderDeviceId(values[0]);
                        ds.setRangeFinderDeviceName(values[1]);

                        prefLstRFDevice.setSummary(values[1]);
                        prefRFCheck.setSummary(R.string.ds_dev_not_configured);
                        ds.setRangeFinderConfigured(false);
                    }
                } else {
                    ds.setRangeFinderDeviceId(StringEx.Empty);
                    ds.setRangeFinderDeviceName(StringEx.Empty);

                    prefLstRFDevice.setSummary(getString(R.string.ds_no_dev));
                    prefRFCheck.setSummary(R.string.ds_dev_not_configured);
                    ds.setRangeFinderConfigured(false);
                }
            } catch (Exception ex) {
                getTtAppCtx().getReport().writeError(ex.getMessage(), "DeviceSettingsFragment:btnRFList");
            }

            return true;
        }
    };
    //endregion


    //region Check NMEA
    private final Preference.OnPreferenceClickListener checkNmeaListener = preference -> {
        FragmentActivity activity = getActivity();

        if (activity != null) {
            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                CheckNmeaDialogTt.newInstance().show(activity.getSupportFragmentManager(), "CHECK_NMEA");
            } else {
                new AlertDialog.Builder(activity)
                        .setMessage("GPS needs to be configured before checking for its NMEA configuration.")
                        .setPositiveButton(R.string.str_ok, null)
                        .show();
            }
        }

        return false;
    };
    //endregion


    //region MetaSelection
    private final DontAskAgainDialog.OnClickListener setMetaListener = (dialogInterface, i, value) -> {
        String receiver = getTtAppCtx().getDeviceSettings().getGpsDeviceName();

        if ((int)value == 1) {
            TtMetadata metadata = getTtAppCtx().getDAL().getDefaultMetadata();

            if (metadata != null) {
                metadata.setGpsReceiver(receiver);
                getTtAppCtx().getDAL().updateMetadata(metadata);
                getTtAppCtx().getMetadataSettings().setReceiver(receiver);
            }
        } else if ((int)value == 2) {
            List<TtMetadata> metas = getTtAppCtx().getDAL().getMetadata();

            if (metas.size() > 0) {
                for (TtMetadata meta : metas) {
                    meta.setGpsReceiver(receiver);
                    getTtAppCtx().getDAL().updateMetadata(meta);
                }
            }
        }
    };
    //endregion
}
