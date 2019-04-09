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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.runtime.ArcGISRuntime;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.activities.MainActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.rangefinder.TtRangeFinderData;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtNotifyManager;
import com.usda.fmsc.twotrails.utilities.TtReport;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;

public class TwoTrailsApp extends Application {
    private static TwoTrailsApp _AppContext;

    private DataAccessLayer _DAL;
    private MediaAccessLayer _MAL;

    private TtReport _Report;

    private Boolean _FoldersInitiated = false;
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

    private ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsServiceBinder = (GpsService.GpsBinder)service;

            gpsServiceBinder.addListener(new GpsService.Listener() {
                @Override
                public void nmeaBurstReceived(INmeaBurst INmeaBurst) {

                }

                @Override
                public void nmeaStringReceived(String nmeaString) {

                }

                @Override
                public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

                }

                @Override
                public void gpsStarted() {
                    if (silentConnectToExternalGps && getGps().isExternalGpsUsed()) {
                        silentConnectToExternalGps = false;
                        _CurrentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(_CurrentActivity, "External GPS Connected", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void gpsStopped() {

                }

                @Override
                public void gpsServiceStarted() {
                    if (getDeviceSettings().isGpsAlwaysOn()) {
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
                                msg = "Failed to connected to external GPS.";
                                delayAndSearchForGps.run();
                                break;
                            case Unknown:
                                break;
                        }

                        if (msg != null) {
                            Toast.makeText(_CurrentActivity, msg, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gpsServiceBinder = null;
        }
    };

    private ServiceConnection rfServiceConnection = new ServiceConnection() {
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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfServiceBinder = null;
        }
    };

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (!getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                    if (device.getAddress().equals(getDeviceSettings().getGpsDeviceID())) {
                        silentConnectToExternalGps = true;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getGps().startGps();
                            }
                        }, 1000);
                    }
                }
            } //else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            //   delayAndSearchForGps.run();
            //} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                //Done searching
//            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//                //Device is about to disconnect
//            }
        }
    };

    private AppLifecycle.Listener appLifecycleListener = new AppLifecycle.Listener() {
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
            Log.d(Consts.LOG_TAG, "Created: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());
        }

        @Override
        public void onResume(Activity activity) {
            Log.d(Consts.LOG_TAG, "Resume: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());

            _CurrentActivity = activity;

            if (activity instanceof MainActivity) {
                if (!AndroidUtils.App.isServiceRunning(_AppContext, GpsService.class)) {
                    startGpsService();
                } else if (getDeviceSettings().isGpsConfigured() && !scanningForGps) {
                    gpsServiceBinder.startGps();
                }

                if (!AndroidUtils.App.isServiceRunning(_AppContext, RangeFinderService.class)) {
                    startRangefinderService();
                }
            }
        }

        @Override
        public void onDestroyed(Activity activity) {
            Log.d(Consts.LOG_TAG, "Destroyed: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());

            if (activity instanceof MainActivity) {
                if (gpsServiceBinder != null) {
                    gpsServiceBinder.stopService();
                    stopService(new Intent(_AppContext, GpsService.class));
                    //gpsServiceBinder = null;
                }

                if (rfServiceBinder != null) {
                    rfServiceBinder.stopService();
                    stopService(new Intent(_AppContext, RangeFinderService.class));
                    //rfServiceBinder = null;
                }

                if (_Report != null) {
                    _Report.writeEvent("TwoTrails Stopped");
                    _Report.closeReport();
                }
            }
        }
    };

    private Runnable delayAndSearchForGps = new Runnable() {
        @Override
        public void run() {
            if (Looper.getMainLooper() == null)
                Looper.prepareMainLooper();

            new Handler(Looper.getMainLooper()).postDelayed(searchForGps, 3000);
        }
    };

    Runnable searchForGps = new Runnable() {
        @Override
        public void run() {
            if (!scanningForGps && !getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                final BluetoothAdapter adapter = getBluetoothManager().getAdapter();

                if (adapter != null) {
                    scanningForGps = true;
                    adapter.startDiscovery();
                    Log.d(Consts.LOG_TAG, "Scanning for BT devices.");

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            adapter.cancelDiscovery();
                            Log.d(Consts.LOG_TAG, "Stopped scan for BT devices.");

                            if (!silentConnectToExternalGps && !getGps().isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                                for (BluetoothDevice device : adapter.getBondedDevices()) {
                                    if (device.getAddress().equals(getDeviceSettings().getGpsDeviceID())) {
                                        silentConnectToExternalGps = true;
                                        Log.d(Consts.LOG_TAG, "GPS found, starting reconnect.");
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                getGps().startGps();
                                            }
                                        }, 1000);

                                        break;
                                    }
                                }

                                if (!silentConnectToExternalGps) {
                                    new Handler().postDelayed(searchForGps, 15000);
                                }
                            }

                            scanningForGps = false;
                        }
                    }, 4000);
                }
            }
        }
    };


    //region Get/Set
    public boolean areFoldersInitiated() {
        return _FoldersInitiated;
    }

    public DeviceSettings getDeviceSettings() {
        //return _DeviceSettings;
        return _DeviceSettings != null ? _DeviceSettings : (_DeviceSettings = new DeviceSettings(this));
    }

    public ProjectSettings getProjectSettings() {
        //return _ProjectSettings;
        return _ProjectSettings != null ? _ProjectSettings : (_ProjectSettings = new ProjectSettings(this));
    }

    public MetadataSettings getMetadataSettings() {
        //return _MetadataSettings;
        return _MetadataSettings != null ? _MetadataSettings : (_MetadataSettings = new MetadataSettings(this));
    }

    public MapSettings getMapSettings() {
        return _MapSettings != null ? _MapSettings : (_MapSettings = new MapSettings(this));
    }

    public TtBluetoothManager getBluetoothManager(){
        return _BluetoothManager != null ? _BluetoothManager : (_BluetoothManager = new TtBluetoothManager());
    }

    public TtNotifyManager getTtNotifyManager() {
        return _TtNotifyManager != null ? _TtNotifyManager : (_TtNotifyManager = new TtNotifyManager(this));
    }

    public ArcGISTools getArcGISTools() {
        return _ArcGISTools != null ? _ArcGISTools : (_ArcGISTools = new ArcGISTools(this));
    }

    public TtReport getReport() {
        if (!_FoldersInitiated)
            initFolders();
        
        return _Report;
    }

    public boolean hasDAL() {
        return _DAL != null;
    }

    public DataAccessLayer getDAL() {
        return _DAL;
    }

    public void setDAL(DataAccessLayer dal) {
        _DAL = dal;

        if (dal != null) {
            getMapSettings().reset();
        }
    }


    public MediaAccessLayer getMAL() {
        return _MAL;
    }

    public MediaAccessLayer getOrCreateMAL() {
        if (_MAL == null && _DAL != null)
        {
            _MAL = new MediaAccessLayer(getMALFilename(), this);
        }

        return _MAL;
    }

    private String getMALFilename() {
        if (_DAL != null) {
            String filename = _DAL.getFilePath();

            return filename.substring(0, filename.length() - 3) + "ttmpx";
        }

        throw new RuntimeException("DAL does not exist.");
    }

    public boolean hasMAL() {
        return _MAL != null || FileUtils.fileExists(getMALFilename());
    }

    public void setMAL(MediaAccessLayer mal) {
        _MAL = mal;
    }
    //endregion


    @Override
    public void onCreate() {
        super.onCreate();

        _AppContext = this;

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread errorThread, Throwable exception) {
                if (!areFoldersInitiated()) {
                    initFolders();
                    _Report.changeDirectory(TtUtils.getTtFileDir());
                }

                _Report.writeError(exception.getMessage(), errorThread.getName(), exception.getStackTrace());

//                new Thread() {
//                    @Override
//                    public void run() {
//                        Looper.prepare();
//                        Toast.makeText(getInstance(),"Fatal Error. Check Log for details.", Toast.LENGTH_LONG).show();
//                        Looper.loop();
//                    }
//                }.start();
//                try
//                {
//                    Thread.sleep(4000); // Let the Toast display before app will get shutdown
//                }
//                catch (InterruptedException e) {
//                    //
//                } catch (Throwable throwable) {
//                    throwable.printStackTrace();
//                }

                Intent intent = new Intent(_AppContext, MainActivity.class);
                intent.putExtra(Consts.Codes.Data.CRASH, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager mgr = (AlarmManager) getInstance().getBaseContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
                _CurrentActivity.finish();
                System.exit(2);
            }
        });

        if (initFolders()) {
            _Report.writeEvent(StringEx.format("TwoTrails Started (%s)", AndroidUtils.App.getVersionName(this)));
        }

        ArcGISRuntime.setClientId(this.getString(R.string.arcgis_client_id));

        ImageLoader.getInstance().init(new ImageLoaderConfiguration.Builder(this).build());

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
    }

    public boolean initFolders() {
        _FoldersInitiated = false;

        if (AndroidUtils.App.checkStoragePermission(_AppContext)) {
            _FoldersInitiated = true;

            File dir = new File(TtUtils.getTtFileDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }

            dir = new File(TtUtils.getOfflineMapsDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }

            dir = new File(TtUtils.getOfflineMapsRecoveryDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }
        }

        if (_FoldersInitiated && _Report == null) {
            _Report = new TtReport();
            _Report.changeDirectory(TtUtils.getTtLogFileDir());
        }

        return _FoldersInitiated;
    }


    public void startGpsService() {
        if (gpsServiceBinder == null && AndroidUtils.App.checkLocationPermission(this)) {
            this.startService(new Intent(this, GpsService.class));
            this.bindService(new Intent(this, GpsService.class), gpsServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void startRangefinderService() {
        if (rfServiceBinder == null && AndroidUtils.App.checkBluetoothPermission(this)) {
            this.startService(new Intent(this, RangeFinderService.class));
            this.bindService(new Intent(this, RangeFinderService.class), rfServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }


    public GpsService.GpsBinder getGps() {
        return gpsServiceBinder;
    }

    public RangeFinderService.RangeFinderBinder getRF() {
        return rfServiceBinder;
    }


    public static synchronized TwoTrailsApp getInstance() {
        return _AppContext;
    }




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
}
