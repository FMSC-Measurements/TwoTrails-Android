package com.usda.fmsc.twotrails;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.runtime.ArcGISRuntime;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.activities.MainActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.utilities.TtNotifyManager;
import com.usda.fmsc.twotrails.utilities.TtReport;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;

public class TwoTrailApp extends Application {
    private static TwoTrailApp _AppContext;

    private DataAccessLayer _DAL;
    private MediaAccessLayer _MAL;

    private TtBluetoothManager bluetoothManager;

    private TtReport Report;

    private Boolean _FoldersInitiated = false;


    DeviceSettings _DeviceSettings;
    ProjectSettings _ProjectSettings;
    MetadataSettings _MetadataSettings;
    MapSettings _MapSettings;

    TtNotifyManager _TtNotifyManager;


    private GpsService.GpsBinder gpsServiceBinder;
    private RangeFinderService.RangeFinderBinder rfServiceBinder;
    private int gpsListenerCount = 0;

    private ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsServiceBinder = (GpsService.GpsBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection rfServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfServiceBinder = (RangeFinderService.RangeFinderBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    //region Get/Set
    public boolean areFoldersInitiated() {
        return _FoldersInitiated;
    }

    public DeviceSettings getDeviceSettings() {
        return _DeviceSettings;
        //return _DeviceSettings != null ? _DeviceSettings : (_DeviceSettings = new DeviceSettings(this));
    }

    public ProjectSettings getProjectSettings() {
        return _ProjectSettings;
        //return _ProjectSettings != null ? _ProjectSettings : (_ProjectSettings = new ProjectSettings(this));
    }

    public MetadataSettings getMetadataSettings() {
        return _MetadataSettings;
        //return _MetadataSettings != null ? _MetadataSettings : (_MetadataSettings = new MetadataSettings(this));
    }

    public MapSettings getMapSettings() {
        return _MapSettings;
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
            _MapSettings.reset();
        }
    }


    public MediaAccessLayer getMAL() {
        return _MAL;
    }

    public MediaAccessLayer getOrCreateMAL() {
        if (_MAL == null && _DAL != null)
        {
            _MAL = new MediaAccessLayer(getMALFilename());
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
                    TtUtils.TtReport.changeDirectory(Global.getTtFileDir());
                }

                TtUtils.TtReport.writeError(exception.getMessage(), errorThread.getName(), exception.getStackTrace());


                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(_AppContext,"Fatal Error. Check Log for details.", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }.start();
                try
                {
                    Thread.sleep(4000); // Let the Toast display before app will get shutdown
                }
                catch (InterruptedException e) {
                    //
                }
                System.exit(2);
            }
        });

        if (initFolders()) {
            TtUtils.TtReport.writeEvent(StringEx.format("TwoTrails Started (%s)", AndroidUtils.App.getVersionName(this)));
        }

        _DeviceSettings = new DeviceSettings(this);
        _ProjectSettings = new ProjectSettings(this);
        _MetadataSettings = new MetadataSettings(this);
        _MapSettings = new MapSettings(this);
        _TtNotifyManager = new TtNotifyManager(this);

        bluetoothManager = new TtBluetoothManager();

        ArcGISRuntime.setClientId(this.getString(R.string.arcgis_client_id));

        ImageLoader.getInstance().init(new ImageLoaderConfiguration.Builder(this).build());

        AppLifecycle.get(this).addListener(new AppLifecycle.Listener() {
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

            }

            @Override
            public void onDestroyed(Activity activity) {
                Log.d(Consts.LOG_TAG, "Destroyed: (" + activity.getClass().getSimpleName() + ")" + activity.getClass().getName());

                if (activity.getClass().isInstance(MainActivity.class)) {
                    if (gpsServiceBinder != null) {
                        gpsServiceBinder.stopService();
                        gpsServiceBinder = null;
                    }

                    if (rfServiceBinder != null) {
                        rfServiceBinder.stopService();
                        rfServiceBinder = null;
                    }

                    if (Report != null) {
                        Report.writeEvent("TwoTrails Stopped");
                        Report.closeReport();
                    }
                }
            }
        });
    }

    public boolean initFolders() {
        _FoldersInitiated = false;

        if (AndroidUtils.App.checkStoragePermission(_AppContext)) {
            _FoldersInitiated = true;

            File dir = new File(Global.getTtFileDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }

            dir = new File(Global.getOfflineMapsDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }

            dir = new File(Global.getOfflineMapsRecoveryDir());
            if (!dir.exists()) {
                _FoldersInitiated &= dir.mkdirs();
            }
        }

        if (_FoldersInitiated && Report == null) {
            Report = new TtReport();
            Report.changeDirectory(Global.getTtLogFileDir());
        }

        return _FoldersInitiated;
    }

    public void startGpsService() {
        if (AndroidUtils.App.checkLocationPermission(this)) {
            this.startService(new Intent(this, GpsService.class));
            this.bindService(new Intent(this, GpsService.class), gpsServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void startRangefinderService() {
        this.startService(new Intent(this, RangeFinderService.class));
        this.bindService(new Intent(this, RangeFinderService.class), rfServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void listenToGps(GpsService.Listener listener) {
        if (gpsServiceBinder != null) {
            gpsServiceBinder.addListener(listener);
            gpsListenerCount++;

            if (!gpsServiceBinder.isGpsRunning() && getDeviceSettings().isGpsConfigured()) {
                gpsServiceBinder.startGps();
            }
        }
    }

    public void stopListeningToGps(GpsService.Listener listener) {
        if (gpsServiceBinder != null) {
            gpsServiceBinder.removeListener(listener);
            gpsListenerCount--;

            if (gpsListenerCount < 1 && !getDeviceSettings().isGpsAlwaysOn()) {
                gpsServiceBinder.stopGps();
            }
        }
    }

    public boolean isGpsRunning() {
        return gpsServiceBinder != null && gpsServiceBinder.isGpsRunning();
    }


    public static TwoTrailApp getContext() {
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
