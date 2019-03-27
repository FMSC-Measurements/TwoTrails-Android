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
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.activities.MainActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
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

public class TwoTrailApp extends Application {
    private static TwoTrailApp _AppContext;

    private DataAccessLayer _DAL;
    private MediaAccessLayer _MAL;

    private TtReport _Report;

    private Boolean _FoldersInitiated = false;


    DeviceSettings _DeviceSettings;
    ProjectSettings _ProjectSettings;
    MetadataSettings _MetadataSettings;
    MapSettings _MapSettings;

    TtNotifyManager _TtNotifyManager;

    ArcGISTools _ArcGISTools;

    private GpsService.GpsBinder gpsServiceBinder;
    private RangeFinderService.RangeFinderBinder rfServiceBinder;
    //private int gpsListenerCount = 0;

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


    public TtNotifyManager getTtNotifyManager() {
        return _TtNotifyManager;
    }

    public ArcGISTools getArcGISTools() {
        return _ArcGISTools;
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
            _MapSettings.reset();
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
            _Report.writeEvent(StringEx.format("TwoTrails Started (%s)", AndroidUtils.App.getVersionName(this)));
        }

        _DeviceSettings = new DeviceSettings(this);
        _ProjectSettings = new ProjectSettings(this);
        _MetadataSettings = new MetadataSettings(this);
        _MapSettings = new MapSettings(this);
        _TtNotifyManager = new TtNotifyManager(this);
        _ArcGISTools = new ArcGISTools(this);

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

                if (activity instanceof MainActivity) {
                    if (!AndroidUtils.App.isServiceRunning(_AppContext, GpsService.class)) {
                        startGpsService();
                    } else {
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
        });
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
