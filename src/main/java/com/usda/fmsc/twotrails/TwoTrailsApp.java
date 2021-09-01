package com.usda.fmsc.twotrails;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.activities.MainActivity;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessManager;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.logic.Segment;
import com.usda.fmsc.twotrails.logic.SegmentFactory;
import com.usda.fmsc.twotrails.logic.SegmentList;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.TwoTrailsProject;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.rangefinder.TtRangeFinderData;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtNotifyManager;
import com.usda.fmsc.twotrails.utilities.TtReport;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;

public class TwoTrailsApp extends Application {
    private static final long ADJUSTING_SLOW_TIME = 30000;

    private TwoTrailsProject _CurrentProject;
    private DataAccessManager _DAM;
    private MediaAccessManager _MAM;

    private TtReport _Report;

    private DocumentFile _ExternalRootDir;
    private Boolean _HasExternalDirAccess = false;

    private DocumentFile _TwoTrailsExternalDir, _OfflineMapsDir, _ImportDir, _ImportedDir, _ExportDir;

    private Boolean silentConnectToExternalGps = false, scanningForGps = false;

    private TtBluetoothManager _BluetoothManager;

    private DeviceSettings _DeviceSettings;
    private ProjectSettings _ProjectSettings;
    private MetadataSettings _MetadataSettings;
    private MapSettings _MapSettings;

    private TtNotifyManager _TtNotifyManager;

    private ArcGISTools _ArcGISTools;

    private GpsService.GpsBinder gpsServiceBinder;
    private RangeFinderService.RangeFinderBinder rfServiceBinder;
    private Activity _CurrentActivity;
    private ProjectAdjusterListener _CurrentListener;


    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsServiceBinder = (GpsService.GpsBinder)service;

            gpsServiceBinder.addListener(new GpsService.Listener() {
                @Override
                public void nmeaBurstReceived(NmeaBurst NmeaBurst) {

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

                }

                @Override
                public void gpsStarted() {
                    if (silentConnectToExternalGps && TwoTrailsApp.this.isGpsServiceStarted() && getGps().isExternalGpsUsed()) {
                        silentConnectToExternalGps = false;
                        _CurrentActivity.runOnUiThread(() -> Toast.makeText(_CurrentActivity, "External GPS Connected", Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void gpsStopped() {

                }

                @Override
                public void gpsServiceStarted() {
                    if (getDeviceSettings().isGpsAlwaysOn() && TwoTrailsApp.this.isGpsServiceStarted()) {
                        getGps().startGps();
                    }
                }

                @Override
                public void gpsServiceStopped() {
                    //gpsServiceBinder = null;
                }

                @Override
                public void gpsError(GpsService.GpsError error) {
                    if (_CurrentActivity instanceof MainActivity) {
                        if (!silentConnectToExternalGps) {
                            String msg = null;
                            switch (error) {
                                case LostDeviceConnection:
                                    msg = "Lost connection to external GPS";
                                    delayAndSearchForGps.run();
                                    break;
                                case DeviceConnectionEnded:
                                    break;
                                case NoExternalGpsSocket:
                                    msg = "Error creating connection to external GPS.";
                                    break;
                                case FailedToConnect:
                                    msg = "Failed to connect to external GPS.";
                                    delayAndSearchForGps.run();
                                    break;
                                //case Unknown:
                                //    break;
                            }

                            if (msg != null) {
                                Toast.makeText(_CurrentActivity, msg, Toast.LENGTH_LONG).show();
                            }
                        }

                        silentConnectToExternalGps = false;
                    }
                }
            });


            if (_CurrentActivity instanceof TtActivity) {
                TtActivity act = (TtActivity) _CurrentActivity;
                if (act.requiresGpsService()) {
                    getGps().startGps();
                }

                if (act instanceof GpsService.Listener) {
                    getGps().addListener((GpsService.Listener)act);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gpsServiceBinder = null;
        }
    };

    private final ServiceConnection rfServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfServiceBinder = (RangeFinderService.RangeFinderBinder)service;

            rfServiceBinder.addListener(new RangeFinderService.Listener() {
                @Override
                public void rfDataReceived(TtRangeFinderData rfData) {

                }

                @Override
                public void rfStringReceived(String rfString) {

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
                public void rangeFinderConnecting() {

                }

                @Override
                public void rangeFinderServiceStarted() {

                }

                @Override
                public void rangeFinderServiceStopped() {
                    //rfServiceBinder = null;
                }

                @Override
                public void rangeFinderError(RangeFinderService.RangeFinderError error) {

                }
            });

            if (_CurrentActivity instanceof TtActivity) {
                TtActivity act = (TtActivity) _CurrentActivity;

                if (act.requiresRFService() && isRFServiceStarted()) {
                    getRF().addListener((RangeFinderService.Listener)act);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfServiceBinder = null;
        }
    };

    private final AppLifecycle.Listener appLifecycleListener = new AppLifecycle.Listener() {
        @Override
        public void onBecameForeground(Activity activity) {
            Log.d(Consts.LOG_TAG, "Foreground: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
        }

        @Override
        public void onBecameBackground(Activity activity) {
            Log.d(Consts.LOG_TAG, "Background: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
        }

        @Override
        public void onCreated(Activity activity) {
            if (getDeviceSettings().isDebugMode()) {
                getReport().writeDebug("Created: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName(), "TtApp");
            } else {
                Log.d(Consts.LOG_TAG, "Created: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
            }

            if (activity instanceof TtActivity) {
                TtActivity act = (TtActivity) activity;

                if (isGpsServiceStarted()) {
                    if (act.requiresGpsService()) {
                        getGps().startGps();
                    }

                    if (act instanceof GpsService.Listener) {
                        getGps().addListener((GpsService.Listener)act);
                    }
                }

                if (isRFServiceStarted()) {
                    if (act.requiresRFService()) {
                        getRF().startRangeFinder();
                    }

                    if (act instanceof RangeFinderService.Listener) {
                        getRF().addListener((RangeFinderService.Listener)act);
                    }
                }
            }
        }

        @Override
        public void onResume(Activity activity) {
            if (getDeviceSettings().isDebugMode()) {
                getReport().writeDebug("Resume: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName(), "TtApp");
            } else {
                Log.d(Consts.LOG_TAG, "Resume: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
            }

            _CurrentActivity = activity;

            if (activity instanceof MainActivity) {
                if (!AndroidUtils.App.isServiceRunning(TwoTrailsApp.this, GpsService.class) || !isGpsServiceStarted()) {
                    startGpsService();
                } else if (getDeviceSettings().isGpsConfigured() && !scanningForGps) {
                    gpsServiceBinder.startGps();
                }

                if (!AndroidUtils.App.isServiceRunning(TwoTrailsApp.this, RangeFinderService.class) || !isRFServiceStarted()) {
                    startRangefinderService();
                }

                if (getDeviceSettings().isExternalSyncEnabled()) {
                    syncExternalDir();
                }
            }

            if (activity instanceof ProjectAdjusterListener) {
                _CurrentListener = (ProjectAdjusterListener)activity;
            }
        }

        @Override
        public void onDestroyed(Activity activity) {
            if (getDeviceSettings().isDebugMode()) {
                getReport().writeDebug("Destroyed: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName(), "TtApp");
            } else {
                Log.d(Consts.LOG_TAG, "Destroyed: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
            }

            if (activity instanceof TtActivity) {
                TtActivity act = (TtActivity) activity;

                if (isGpsServiceStarted()) {
                    if (act.requiresGpsService()) {
                        getGps().removeListener((GpsService.Listener)act);
                    }

                    if (!(getDeviceSettings().isGpsAlwaysOn() || getGps().isLogging())) {
                        getGps().stopGps();
                    }
                }

                if (isRFServiceStarted()) {
                    if (act.requiresRFService()) {
                        getRF().removeListener((RangeFinderService.Listener)act);
                    }

                    if (!(getDeviceSettings().isRangeFinderAlwaysOn() || getRF().isLogging())) {
                        getRF().stopRangeFinder();
                    }
                }
            }

            if (activity instanceof MainActivity) {
                if (isGpsServiceStarted()) {
                    getGps().stopService();
                    stopService(new Intent(TwoTrailsApp.this, GpsService.class));
                    //gpsServiceBinder = null;
                }

                if (isRFServiceStarted()) {
                    getRF().stopService();
                    stopService(new Intent(TwoTrailsApp.this, RangeFinderService.class));
                    //rfServiceBinder = null;
                }

                if (_Report != null) {
                    _Report.writeEvent("TwoTrails Stopped");
                    _Report.closeReport();
                }

                _CurrentProject = null;
                _DAM = null;
                _MAM = null;
            }
        }
    };


    //region Bluetooth Broadcast Receiver
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (isGpsServiceStarted() && !getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                    if (device != null && device.getAddress().equals(getDeviceSettings().getGpsDeviceID())) {
                        silentConnectToExternalGps = true;
                        new Handler().postDelayed(() -> getGps().startGps(), 1000);
                    }
                }
            }
        }
    };
    //endregion

    //region UncaughtExceptionHandler
    private final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread errorThread, @NonNull Throwable exception) {
            boolean restart = true;

            try {
                if (_Report == null) {
                    _Report = new TtReport(getLogFile());
                }

                _Report.writeError(exception.getMessage(), errorThread.getName(), exception.getStackTrace());

                if (isGpsServiceStarted()) {
                    getGps().stopService();
                }
            } catch (Exception e) {
                Log.e(Consts.LOG_TAG, "Error in TwoTrailsApp:uncaughtException");
            }

            try {
                DateTime lastCrash = getDeviceSettings().getLastCrashTime();
                DateTime currentCrash = DateTime.now();
                getDeviceSettings().setLastCrashTime(currentCrash);

                if (lastCrash != null && lastCrash.isAfter(currentCrash.minusMinutes(1))) {
                    restart = false;
                }
            } catch (Exception ex) {
                restart = false;
            }

            try
            {
                if (restart) {
                    Intent intent = new Intent(TwoTrailsApp.this, MainActivity.class);
                    intent.putExtra(Consts.Codes.Data.CRASH, true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                    AlarmManager mgr = (AlarmManager)getBaseContext().getSystemService(Context.ALARM_SERVICE);
                    if (mgr != null) {
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
                    }
                } else {
                    new Thread(() -> {
                        Looper.prepare();
                        Toast.makeText(getBaseContext(),"TwoTrails crashed twice in the past minute. Check Log for details and contact development team if needed.", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }).start();

                    Thread.sleep(4000); // Let the Toast display before app will get shutdown
                }

                getReport().closeReport();
            }
            catch (Exception e) {
                //
            }

            _CurrentActivity.finishAndRemoveTask();
            //_CurrentActivity.finish();
            System.exit(2);
        }
    };
    //endregion

    //region SearchForGps
    private final Runnable delayAndSearchForGps = new Runnable() {
        @Override
        public void run() {
            if (Looper.getMainLooper() == null)
                Looper.prepareMainLooper();

            new Handler(Looper.getMainLooper()).postDelayed(searchForGps, 3000);
        }
    };

    private final Runnable searchForGps = new Runnable() {
        @Override
        public void run() {
            if (!scanningForGps && isGpsServiceStarted() && !getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                final BluetoothAdapter adapter = getBluetoothManager().getAdapter();

                if (adapter != null) {
                    scanningForGps = true;
                    adapter.startDiscovery();
                    Log.d(Consts.LOG_TAG, "Scanning for BT devices.");

                    new Handler().postDelayed(() -> {
                        adapter.cancelDiscovery();
                        Log.d(Consts.LOG_TAG, "Stopped scan for BT devices.");

                        if (!silentConnectToExternalGps && isGpsServiceStarted() && !getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                            for (BluetoothDevice device : adapter.getBondedDevices()) {
                                if (device.getAddress().equals(getDeviceSettings().getGpsDeviceID())) {
                                    silentConnectToExternalGps = true;
                                    Log.d(Consts.LOG_TAG, "GPS found, starting reconnect.");
                                    new Handler().postDelayed(() -> getGps().startGps(), 1000);

                                    break;
                                }
                            }

                            if (!silentConnectToExternalGps) {
                                new Handler().postDelayed(searchForGps, 15000);
                            }
                        }

                        scanningForGps = false;
                    }, 4000);
                }
            }
        }
    };
    //endregion


    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

        try {
            _Report = new TtReport(getLogFile());
        } catch (IOException e) {
            Log.e(Consts.LOG_TAG, "Unable to start log");
            e.printStackTrace();
        }

        _Report.writeEvent(String.format("TwoTrails Started (%s)", AndroidUtils.App.getAppVersion(this)));

        //ArcGISRuntime.setClientId(this.getString(R.string.arcgis_client_id)); //100.2.9
        ArcGISRuntimeEnvironment.setLicense(this.getString(R.string.arcgis_runtime_license)); //100.10.0

//        ImageLoader.getInstance().init(new ImageLoaderConfiguration.Builder(this).build());

        AppLifecycle.get(this).addListener(appLifecycleListener);

        //listen for bluetooth interactions
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);

        DeviceSettings ds = getDeviceSettings();

        if (ds.isExternalSyncEnabled()) {
            String dir = ds.getExternalSyncDir();

            if (dir != null) {
                try {
                    setExternalRootDir(Uri.parse(ds.getExternalSyncDir()));
                } catch (Exception e) {
                    ds.setExternalSyncEnabled(false);
                }
            } else {
                //dir does not exist
                ds.setExternalSyncEnabled(false);
            }
        }
    }

    //region Settings / Tools
    public DeviceSettings getDeviceSettings() {
        return _DeviceSettings != null ? _DeviceSettings : (_DeviceSettings = new DeviceSettings(TwoTrailsApp.this));
    }

    public ProjectSettings getProjectSettings() {
        return _ProjectSettings != null ? _ProjectSettings : (_ProjectSettings = new ProjectSettings(TwoTrailsApp.this));
    }

    public MetadataSettings getMetadataSettings() {
        return _MetadataSettings != null ? _MetadataSettings : (_MetadataSettings = new MetadataSettings(TwoTrailsApp.this));
    }

    public MapSettings getMapSettings() {
        return _MapSettings != null ? _MapSettings : (_MapSettings = new MapSettings(TwoTrailsApp.this));
    }


    public ArcGISTools getArcGISTools() {
        return _ArcGISTools != null ? _ArcGISTools : (_ArcGISTools = new ArcGISTools(TwoTrailsApp.this));
    }


    public boolean hasReport() {
        return _Report != null;
    }

    public TtReport getReport() {
        return _Report;
    }
    //endregion

    //region DAL / MAL
    public TwoTrailsProject getCurrentProject() {
        return _CurrentProject;
    }

    public boolean hasDAL() {
        return _DAM != null;
    }

    public DataAccessLayer getDAL() {
        if (hasDAL())
            return _DAM.getDAL();

        if (_CurrentProject != null) {
            setDAM(DataAccessManager.openDAL(TwoTrailsApp.this, _CurrentProject.TTXFile));
            return _DAM.getDAL();
        }

        if (getDeviceSettings().getLastOpenedProject() != null) {
            _CurrentProject = getDeviceSettings().getLastOpenedProject();

            if (DataAccessManager.localDALExists(TwoTrailsApp.this, _CurrentProject.TTXFile)) {
                setDAM(DataAccessManager.openDAL(TwoTrailsApp.this, _CurrentProject.TTXFile));
                return _DAM.getDAL();
            }

            _DAM = null;
        }

        throw new RuntimeException("DAL not set");
    }

    public void setDAM(DataAccessManager dam) {
        _DAM = dam;

        if (dam != null) {
            _CurrentProject = new TwoTrailsProject(
                    dam.getDAL().getProjectDeviceID(),
                    dam.getDatabaseName()
            );

            getReport().writeEvent(String.format("DAL Loaded: %s (%s)", _CurrentProject.Name, _CurrentProject.TTXFile));

            getMapSettings().reset();
            getProjectSettings().initProjectSettings(getDAL());

            getProjectSettings().updateRecentProjects(_CurrentProject);
            getDeviceSettings().setLastOpenedProject(_CurrentProject);
        } else {
            getReport().writeEvent("DAL Unloaded");
        }
    }

    public DataAccessManager getDAM() {
        return _DAM;
    }

    public MediaAccessLayer getMAL() {
        if (_MAM == null && hasDAL()) {

            if (_CurrentProject.TTMPXFile == null) {
                //create TTMPX
                _CurrentProject = new TwoTrailsProject(
                        _CurrentProject.Name,
                        _CurrentProject.TTXFile,
                        _CurrentProject.TTXFile.replace(Consts.FILE_EXTENSION, Consts.MEDIA_PACKAGE_EXTENSION));

                getProjectSettings().updateRecentProjects(_CurrentProject);
                getDeviceSettings().setLastOpenedProject(_CurrentProject);
            }

            setMAM(MediaAccessManager.openMAL(TwoTrailsApp.this, _CurrentProject.TTMPXFile));
        }

        return _MAM.getMAL();
    }

    public boolean hasMAL() {
        return _MAM != null ||
                (_CurrentProject != null &&
                        (_CurrentProject.TTMPXFile != null ||
                            (hasDAL() &&
                    MediaAccessManager.localMALExists(TwoTrailsApp.this, _CurrentProject.TTXFile.replace(Consts.FILE_EXTENSION, Consts.MEDIA_PACKAGE_EXTENSION))))
                );
    }

    public MediaAccessManager getMAM() {
        return _MAM;
    }

    public void setMAM(MediaAccessManager mam) {
        if (mam != null) {
            _MAM = mam;
            getReport().writeEvent(String.format("MAL Loaded: %s (%s)", _CurrentProject.Name, _CurrentProject.TTMPXFile));
        } else {
            getReport().writeEvent("MAL Unloaded");
        }
    }
    //endregion

    //region Storage Access

    public boolean hasExternalDirAccess() {
        return _HasExternalDirAccess || (_HasExternalDirAccess = (_ExternalRootDir != null && _ExternalRootDir.exists()));
    }

    public void setExternalRootDir(Uri externalFolder) throws Exception {
        if (externalFolder != null) {
            _ExternalRootDir = DocumentFile.fromTreeUri(this, externalFolder);

            Uri externalRootDirUri;
            if (_ExternalRootDir == null) {
                getReport().writeError("Unable to create DocumentFile", "TwoTrailsApp:setExternalRootDir");
                throw new Exception("Unable to create DocumentFile");
            } else {
                externalRootDirUri = _ExternalRootDir.getUri();
            }

            if (!hasExternalDirAccess()) {
                getReport().writeError("No folder access permissions", "TwoTrailsApp:setExternalRootDir");
                throw new Exception("No External Directory Access");
            }

            try {
                _TwoTrailsExternalDir = AndroidUtils.Files.getDocumentFromTree(TwoTrailsApp.this, externalRootDirUri, Consts.FolderLayout.External.TwoTrailsFolderPath);
                if (_TwoTrailsExternalDir == null || !_TwoTrailsExternalDir.exists()) {
                    _TwoTrailsExternalDir = _ExternalRootDir.createDirectory(Consts.FolderLayout.External.TwoTrailsFolderName);
                    if (_TwoTrailsExternalDir == null) {
                        throw new Exception("TwoTrails Root Dir not created");
                    }
                }

                _OfflineMapsDir = AndroidUtils.Files.getDocumentFromTree(TwoTrailsApp.this, _TwoTrailsExternalDir.getUri(), Consts.FolderLayout.External.OfflineMapsPath);
                if (_OfflineMapsDir == null || !_OfflineMapsDir.exists()) {
                    _OfflineMapsDir = _TwoTrailsExternalDir.createDirectory(Consts.FolderLayout.External.OfflineMapsName);
                }

                _ImportDir = AndroidUtils.Files.getDocumentFromTree(TwoTrailsApp.this, _TwoTrailsExternalDir.getUri(), Consts.FolderLayout.External.ImportFolderPath);
                if (_ImportDir == null || !_ImportDir.exists()) {
                    _ImportDir = _TwoTrailsExternalDir.createDirectory(Consts.FolderLayout.External.ImportFolderName);
                }

                _ImportedDir = AndroidUtils.Files.getDocumentFromTree(TwoTrailsApp.this, _TwoTrailsExternalDir.getUri(), Consts.FolderLayout.External.ImportedFolderPath);
                if (_ImportedDir == null || !_ImportedDir.exists()) {
                    _ImportedDir = _ImportDir.createDirectory(Consts.FolderLayout.External.ImportedFolderName);
                }

                _ExportDir = AndroidUtils.Files.getDocumentFromTree(TwoTrailsApp.this, _TwoTrailsExternalDir.getUri(), Consts.FolderLayout.External.ExportFolderPath);
                if (_ExportDir == null || !_ExportDir.exists()) {
                    _ExportDir = _TwoTrailsExternalDir.createDirectory(Consts.FolderLayout.External.ExportFolderName);
                }
            } catch (Exception e) {
                getReport().writeError(e.getMessage(), "TwoTrailsApp:setExternalRootDir", e.getStackTrace());
                getDeviceSettings().setExternalSyncEnabled(false);
            }

            getDeviceSettings().setExternalSyncDir(externalRootDirUri.toString());
            getDeviceSettings().setExternalSyncEnabled(true);
        }
    }

    //endregion



    //region GPS / RangeFinder
    public void startGpsService() {
        if (!isGpsServiceStarted()) {
            this.startService(new Intent(this, GpsService.class));
            this.bindService(new Intent(this, GpsService.class), gpsServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void startRangefinderService() {
        if (!isRFServiceStarted() && AndroidUtils.App.checkBluetoothPermission(this)) {
            this.startService(new Intent(this, RangeFinderService.class));
            this.bindService(new Intent(this, RangeFinderService.class), rfServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }


    public TtBluetoothManager getBluetoothManager(){
        return _BluetoothManager != null ? _BluetoothManager : (_BluetoothManager = new TtBluetoothManager());
    }

    public TtNotifyManager getTtNotifyManager() {
        return _TtNotifyManager != null ? _TtNotifyManager : (_TtNotifyManager = new TtNotifyManager(this));
    }


    public boolean isGpsServiceStarted() {
        return gpsServiceBinder != null;
    }

    public boolean isGpsServiceStartedAndRunning() {
        return gpsServiceBinder != null && getGps().isGpsRunning();
    }

    public GpsService.GpsBinder getGps() {
        if (gpsServiceBinder == null) throw new RuntimeException("Not bound to GpsService");
        return gpsServiceBinder;
    }

    public boolean isRFServiceStarted() {
        return rfServiceBinder != null;
    }

    public RangeFinderService.RangeFinderBinder getRF() {
        if (rfServiceBinder == null) throw new RuntimeException("Not bound to RfService");
        return rfServiceBinder;
    }
    //endregion


    //For use with logging only
//    public static synchronized TtReport getTtReport() {
//        return _AppContext.getReport();
//    }
//
//    public static TwoTrailsApp getInstance(Context baseContext) {
//        return (TwoTrailsApp)baseContext.getApplicationContext();
//    }

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


    //region Syncing


    public void syncExternalDir() {

    }


    //endregion


    //region DAL Adjusting
    private static boolean _adjusting = false;
    private static boolean _cancelToken = false;

    public ProjectAdjusterResult adjustProject() {
        return adjustProject(false);
    }

    public ProjectAdjusterResult adjustProject(final boolean updateIndexes) {
        final DataAccessLayer dal = getDAL();

        if (!_adjusting) {
            onAdjusterStarted();

            _cancelToken = false;

            //check for point issues
            if (dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName) < 1)
                return ProjectAdjusterResult.NO_POLYS;
            else {
                for (TtPolygon poly : dal.getPolygons()) {
                    TtPoint p = dal.getFirstPointInPolygon(poly.getCN());

                    if (p != null) {
                        if (p.isTravType() || (p.getOp() == OpType.Quondam &&
                                ((QuondamPoint) p).getParentPoint().isTravType()))
                            return ProjectAdjusterResult.STARTS_WITH_TRAV_TYPE;
                    }
                }
            }

            _adjusting = true;

            new Thread(() -> {
                ProjectAdjusterResult result = ProjectAdjusterResult.ADJUSTING;
                boolean success = false;

                AdjustingException.AdjustingError error = AdjustingException.AdjustingError.None;

                try {

                    if (updateIndexes)
                        updatePointIndexes(dal);

                    if (!_cancelToken) {
                        success = adjustPoints(dal);
                    }

                    if (success) {
                        result = ProjectAdjusterResult.SUCCESSFUL;
                    } else {
                        result = ProjectAdjusterResult.ERROR;
                    }
                } catch (AdjustingException ex) {
                    getReport().writeError(ex.getMessage(), "PolygonAdjuster:adjust", ex.getStackTrace());
                    result = ProjectAdjusterResult.ERROR;
                    error = ex.getErrorType();
                } catch (Exception ex) {
                    getReport().writeError(ex.getMessage(), "PolygonAdjuster:adjust", ex.getStackTrace());
                    result = ProjectAdjusterResult.ERROR;
                } finally {
                    _adjusting = false;

                    if (_cancelToken) {
                        result = ProjectAdjusterResult.CANCELED;
                    }

                    onAdjusterStopped(result, error);
                }
            }).start();
        }

        return ProjectAdjusterResult.ADJUSTING;
    }


    private void updatePointIndexes(DataAccessLayer dal) {
        ArrayList<TtPoint> savePoints = new ArrayList<>();

        for (TtPolygon poly : dal.getPolygons()) {
            int index = 0;

            for(TtPoint point : dal.getPointsInPolygon(poly.getCN())) {
                if (point.getIndex() != index)
                {
                    point.setIndex(index);
                    savePoints.add(point);
                }

                index++;
            }
        }

        dal.updatePoints(savePoints, savePoints);
    }

    private boolean adjustPoints(DataAccessLayer dal) throws AdjustingException {
        long startTime = System.currentTimeMillis();
        boolean slowTimeTriggered = false;

        SegmentFactory sf = new SegmentFactory(dal);

        if(sf.hasNext()) {
            SegmentList sl = new SegmentList();
            ArrayList<Segment> adjusted = new ArrayList<>();

            while (sf.hasNext()) {
                sl.addSegment(sf.next());
            }

            Segment seg;
            while (sl.hasNext()) {
                if(_cancelToken)
                    return false;

                seg = sl.next();

                if (seg.calculate()) {
                    seg.adjust();
                    adjusted.add(seg);
                } else {
                    seg.setWeight(seg.getWeight() - 1);
                    sl.addSegment(seg);
                }

                if (!slowTimeTriggered && System.currentTimeMillis() - startTime > ADJUSTING_SLOW_TIME) {

                    onAdjusterRunningSlow();

                    slowTimeTriggered = true;
                }
            }

            if (_cancelToken)
                return false;

            TtPoint p;
            Hashtable<String, TtPoint> pointsTable = new Hashtable<>();

            for (int s = 0; s < adjusted.size(); s++)
            {
                for (int i = 0; i < adjusted.get(s).getPointCount(); i++)
                {
                    p = adjusted.get(s).get(i);

                    if (!pointsTable.containsKey(p.getCN()))
                        pointsTable.put(p.getCN(), p);
                }
            }

            ArrayList<TtPoint> points = new ArrayList<>(pointsTable.values());

            dal.updatePoints(points);

            calculateAreaAndPerimeter(dal);
        }

        return true;
    }

    private void calculateAreaAndPerimeter(DataAccessLayer dal) {
        ArrayList<TtPolygon> polys = dal.getPolygons();

        if (polys != null && polys.size() > 0) {
            for (TtPolygon poly : polys) {
                ArrayList<TtPoint> points = dal.getBoundaryPointsInPoly(poly.getCN());

                if (points.size() > 2) {
                    double perim = 0, area = 0;

                    TtPoint p1, p2;
                    for (int i = 0; i < points.size() - 1; i++) {
                        p1 = points.get(i);
                        p2 = points.get(i + 1);

                        perim += TtUtils.Math.distance(p1, p2);
                        area += ((p2.getAdjX() - p1.getAdjX()) * (p2.getAdjY() + p1.getAdjY()) / 2);
                    }

                    poly.setPerimeterLine(perim);

                    p1 = points.get(points.size() - 1);
                    p2 = points.get(0);
                    perim += TtUtils.Math.distance(p1, p2);
                    area += ((p2.getAdjX() - p1.getAdjX()) * (p2.getAdjY() + p1.getAdjY()) / 2);

                    poly.setPerimeter(perim);
                    poly.setArea(Math.abs(area));
                } else {
                    poly.setPerimeter(0);
                    poly.setArea(0);
                }

                dal.updatePolygon(poly);
            }
        }
    }


    public void cancelAdjuster() {
        _cancelToken = true;
    }


    public boolean isAdjusting() {
        return false;
    }


    protected void onAdjusterStarted() {
        if (_CurrentListener != null)
            _CurrentListener.onAdjusterStarted();
    }

    protected void onAdjusterStopped(final ProjectAdjusterResult result, final AdjustingException.AdjustingError error) {
        if (_CurrentListener != null)
            _CurrentListener.onAdjusterStopped(result, error);

        if (getDeviceSettings().isExternalSyncEnabled()) {
            syncExternalDir();
        }
    }

    protected void onAdjusterRunningSlow() {
        if (_CurrentListener != null)
            _CurrentListener.onAdjusterRunningSlow();
    }


    public interface ProjectAdjusterListener {
        void onAdjusterStarted();
        void onAdjusterStopped(final ProjectAdjusterResult result, final AdjustingException.AdjustingError error);
        void onAdjusterRunningSlow();
    }

    public enum ProjectAdjusterResult {
        ADJUSTING,
        STARTS_WITH_TRAV_TYPE,
        NO_POLYS,
        BAD_POINT,
        ERROR,
        SUCCESSFUL,
        CANCELED
    }
    //endregion


    //region File / Folder Paths
    private File _MediaDir;

    public File getLogFile() {
        return new File(getFilesDir(), Consts.Files.LOG_FILE);
    }

    public File getGpsLogFile() {
        return new File(getCacheDir(), String.format("%s_%s", Consts.Files.GPS_LOG_FILE_PREFIX, DateTime.now().toString()));
    }

    public File getProjectMediaDir() {
        if (!hasDAL()) throw new RuntimeException("Project not opened");
        return _MediaDir != null ? _MediaDir : (_MediaDir = Paths.get(getFilesDir().getPath(), Consts.FolderLayout.Internal.MediaDir, FileUtils.getFileNameWoExt(getDAL().getFileName())).toFile());
    }

    public Uri getMediaFileByFileName(String fileName) {
        return  Uri.parse(Paths.get(getProjectMediaDir().toString(), fileName).toString());
    }

    public File getSettingsFile() {
        return new File(getCacheDir(), Consts.Files.SETTINGS_FILE);
    }

    public DocumentFile getTwoTrailsExternalDir() {
        return _TwoTrailsExternalDir;
    }

    public DocumentFile getOfflineMapsDir() {
        return _OfflineMapsDir;
    }
    public DocumentFile getImportDir() {
        return _ImportDir;
    }
    public DocumentFile getImportedDir() {
        return _ImportedDir;
    }
    public DocumentFile getExportDir() {
        return _ExportDir;
    }

    //endregion
}
