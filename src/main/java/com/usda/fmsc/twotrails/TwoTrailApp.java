package com.usda.fmsc.twotrails;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;

//android.support.multidex.MultiDexApplication pre 5.0
public class TwoTrailApp extends Application {
    private static TwoTrailApp _AppContext;




    private DataAccessLayer _DAL;
    private MediaAccessLayer _MAL;

    private TtMetadata _DefaultMeta;
    private TtGroup _MainGroup;

    private TtBluetoothManager bluetoothManager;

    private Boolean _FoldersInitiated = false;


    private GpsService.GpsBinder gpsService;
    private RangeFinderService.RangeFinderBinder rfBinder;
    private int gpsListenerCount = 0;

    private ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsService = (GpsService.GpsBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection rfServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfBinder = (RangeFinderService.RangeFinderBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    //region Get/Set
    public boolean areFoldersInitiated() {
        return _FoldersInitiated;
    }


    //endregion

    @Override
    public void onCreate() {
        super.onCreate();

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


            }
        });



    }



    public boolean isGpsExternal() {
        return false;
    }

    public boolean isGpsConfigured() {
        return false;
    }

    public void listenToGps(GpsService.Listener listener) {
        if (gpsService != null) {
            gpsService.addListener(listener);
            gpsListenerCount++;
        }
    }

    public void stopListeningToGps(GpsService.Listener listener) {
        if (gpsService != null) {
            gpsService.removeListener(listener);
            gpsListenerCount--;

            if (gpsListenerCount < 1 && !Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                gpsService.stopGps();
            }
        }
    }



//    @Override
//    public void onCreate() {
//        super.onCreate();
//
//        _AppContext = this;
//
//
//        //Global.Settings.DeviceSettings.init();
//
//        _DefaultMeta = Global.Settings.MetaDataSetting.getDefaultmetaData();
//
//        //TtUtils.TtReport.changeDirectory(getTtLogFileDir());
//
//        _MainGroup = new TtGroup();
//        _MainGroup.setCN(Consts.EmptyGuid);
//        _MainGroup.setName("Main Group");
//        _MainGroup.setDescription("Group for unassigned points.");
//        _MainGroup.setGroupType(TtGroup.GroupType.General);
//
//        bluetoothManager = new TtBluetoothManager();
//
//        //Global.TtNotifyManager.init(applicationContext);
//
//        //initFolders();
//
//        if (AndroidUtils.App.checkStoragePermission(this)) {
//            TtUtils.TtReport.writeEvent(StringEx.format("TwoTrails Started (%s)", AndroidUtils.App.getVersionName(this)));
//        }
//
//        ArcGISRuntime.setClientId(this.getString(R.string.arcgis_client_id));
//
//        //initUI();
//
//
//
//    }





    public static TwoTrailApp getTtAppContext() {
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
