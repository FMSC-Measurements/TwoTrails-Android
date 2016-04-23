package com.usda.fmsc.twotrails;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.usda.fmsc.android.AndroidUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usda.fmsc.twotrails.activities.MainActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.Units.UomElevation;
import com.usda.fmsc.utilities.StringEx;

public class Global {
    public static DataAccessLayer DAL;

    private static Context _ApplicationContext;
    private static MainActivity _MainActivity;

    private static String _LogFilePath;

    private static TtMetadata _DefaultMeta;
    private static TtGroup _MainGroup;

    protected static TtBluetoothManager bluetoothManager;


    private static GpsService.GpsBinder gpsBinder;

    private static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsBinder = (GpsService.GpsBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };



    public static void init(Context applicationContext, MainActivity mainActivity) {
        _ApplicationContext = applicationContext;
        _MainActivity = mainActivity;

        Settings.DeviceSettings.init();

        _LogFilePath = _ApplicationContext.getApplicationInfo().dataDir;// Environment.getDataDirectory().getAbsolutePath(); //TtUtils.getTtFileDir();
        _DefaultMeta = Settings.MetaDataSetting.getDefaultmetaData();

        TtUtils.TtReport.changeFilePath(_LogFilePath);

        _MainGroup = new TtGroup();
        _MainGroup.setCN(Consts.EmptyGuid);
        _MainGroup.setName("Main Group");
        _MainGroup.setDescription("Group for unassigned points.");
        _MainGroup.setGroupType(TtGroup.GroupType.General);

        bluetoothManager = new TtBluetoothManager();

        TtNotifyManager.init(applicationContext);

        File dir = new File(TtUtils.getTtFileDir());
        if(!dir.exists()) {
            dir.mkdirs();
        }

        dir = new File(TtUtils.getOfflineMapsDir());
        if(!dir.exists()) {
            dir.mkdirs();
        }

        dir = new File(TtUtils.getOfflineMapsRecoveryDir());
        if(!dir.exists()) {
            dir.mkdirs();
        }

        _ApplicationContext.startService(new Intent(_ApplicationContext, GpsService.class));
        _ApplicationContext.bindService(new Intent(_ApplicationContext, GpsService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        TtUtils.TtReport.writeEvent("TwoTrails Started");

        initUI();
    }

    private static void initUI() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AndroidUtils.UI.setOverscrollColor(_ApplicationContext.getResources(), _ApplicationContext, R.color.primary);
        }
    }

    public static void destroy() {
        gpsBinder.stopService();
        TtUtils.TtReport.writeEvent("TwoTrails Stopped");
        TtUtils.TtReport.closeReport();
        System.exit(0);
    }


    public static String getLogFilePath() {
        return _LogFilePath;
    }

    public static TtMetadata getDefaultMeta() {
        return _DefaultMeta;
    }

    public static TtGroup getMainGroup() {
        return _MainGroup;
    }

    public static String getTwoTrailsVersion() {
        try {
            PackageInfo pInfo = _ApplicationContext.getPackageManager().getPackageInfo(_ApplicationContext.getPackageName(), 0);

            return String.format("ANDROID: %s", pInfo.versionName);
        } catch (Exception ex) {
            //
        }

        return "ANDROID: ???";
    }

    public static TtBluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public static GpsService.GpsBinder getGpsBinder() {
        return gpsBinder;
    }

    public static MainActivity getMainActivity() {
        return _MainActivity;
    }

    public static Context getApplicationContext() {
        return _ApplicationContext;
    }

    public static class TtNotifyManager {
        public static int GPS_NOTIFICATION_ID = 123;

        protected static NotificationManager _NotificationManager;
        private static NotificationCompat.Builder _GpsBuilder;
        private static int _UsedDrawable;
        private static String _UsedText;
        private static HashMap<Integer, NotificationCompat.Builder> _DownloadingNotifs;

        public static void init(Context ctx) {
            _NotificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            _GpsBuilder = new NotificationCompat.Builder(ctx);
            _GpsBuilder.setOngoing(true);
            _DownloadingNotifs = new HashMap<>();
        }

        public static NotificationManager getNotificationManager() {
            return _NotificationManager;
        }

        public static void setGpsOn() {
            if(_NotificationManager != null && _GpsBuilder != null) {
                _GpsBuilder.setContentTitle(Consts.ServiceTitle);

                _UsedText = Consts.ServiceContent;
                _GpsBuilder.setContentText(_UsedText);

                _UsedDrawable = R.drawable.ic_ttgps_holo_dark_enabled;
                _GpsBuilder.setSmallIcon(_UsedDrawable);

                _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
            }
        }

        public static void setGpsOff() {
            if(_NotificationManager != null) {
                _NotificationManager.cancel(GPS_NOTIFICATION_ID);
            }
        }


        public static void startWalking() {
            if (_NotificationManager != null && _GpsBuilder != null) {
                _GpsBuilder.setContentTitle(Consts.ServiceTitle);

                _UsedText = Consts.ServiceWalking;
                _GpsBuilder.setContentText(_UsedText);

                _UsedDrawable = R.drawable.ic_ttgps_holo_dark_enabled; //switch to walking anim
                _GpsBuilder.setSmallIcon(_UsedDrawable);

                _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
            }
        }

        public static void stopWalking(){
            if (gpsBinder.isGpsRunning()) {
                setGpsOn();
            } else {
                setGpsOff();
            }
        }

        public static void showPointAquired() {
            if(_NotificationManager != null && _GpsBuilder != null) {

                _GpsBuilder.setContentTitle(Consts.ServiceTitle)
                .setContentText(Consts.ServiceAcquiringPoint)
                .setSmallIcon(R.drawable.ica_capturepoint);

                _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            sleep(1000);

                            _GpsBuilder.setContentTitle(Consts.ServiceTitle);
                            _GpsBuilder.setContentText(_UsedText);
                            _GpsBuilder.setSmallIcon(_UsedDrawable);

                            _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };

                thread.start();
            }
        }


        public static void startMapDownload(int id, String name) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(_ApplicationContext);
            builder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_map_black_36dp)
                    .setContentTitle(String.format("Downloading Map %s", name))
                    .setProgress(100, 0, false);

            _DownloadingNotifs.put(id, builder);

            _NotificationManager.notify(id, builder.build());
        }

        public static void updateMapDownload(int id, int progress) {
            _NotificationManager.notify(id, _DownloadingNotifs.get(id).setProgress(100, progress, false).build());
        }

        public static void endMapDownload(int id) {
            _NotificationManager.cancel(id);
            _DownloadingNotifs.remove(id);
        }
    }



    public static class Settings {

        public static class PreferenceHelper {
            private static SharedPreferences prefs;
            private static SharedPreferences.Editor editor;

            public static void loadPrefs() {
                prefs = PreferenceManager.getDefaultSharedPreferences(_ApplicationContext);
                editor = prefs.edit();
            }

            public static SharedPreferences getPrefs() {
                return  prefs;
            }

            public static  SharedPreferences.Editor getEditor() {
                return editor;
            }


            protected static int getInt(String settingName)
            {
                return getInt(settingName, -1);
            }

            protected static int getInt(String settingName, int defaultValue) {
                if(prefs == null)
                    loadPrefs();
                return prefs.getInt(settingName, defaultValue);
            }

            protected static boolean setInt(String settingName, int value) {
                if(editor == null)
                    loadPrefs();
                return editor.putInt(settingName, value).commit();
            }


            protected static long getLong(String settingName)
            {
                return getLong(settingName, -1);
            }

            protected static long getLong(String settingName, long defaultValue) {
                if(prefs == null)
                    loadPrefs();
                return prefs.getLong(settingName, defaultValue);
            }

            protected static boolean setLong(String settingName, long value) {
                if(editor == null)
                    loadPrefs();
                return editor.putLong(settingName, value).commit();
            }


            protected static String getString(String settingName) {
                return getString(settingName, "");
            }

            protected static String getString(String settingName, String defaultValue) {
                if(prefs == null)
                    loadPrefs();
                return prefs.getString(settingName, defaultValue);
            }

            protected static boolean setString(String settingName, String value) {
                if(editor == null)
                    loadPrefs();
                return editor.putString(settingName, value).commit();
            }


            protected static double getDouble(String settingName) {
                return getDouble(settingName, 0);
            }

            protected static double getDouble(String settingName, double defaultValue) {
                if(prefs == null)
                    loadPrefs();
                return  Double.longBitsToDouble(prefs.getLong(settingName, Double.doubleToRawLongBits(defaultValue)));
            }

            protected static boolean setDouble(String settingName, double value) {
                if(editor == null)
                    loadPrefs();
                return editor.putLong(settingName, Double.doubleToRawLongBits(value)).commit();
            }


            protected static float getFloat(String settingName)
            {
                return getFloat(settingName, 0);
            }

            protected static float getFloat(String settingName, float defaultValue) {
                if(prefs == null)
                    loadPrefs();
                return prefs.getFloat(settingName, defaultValue);
            }

            protected static boolean setFlaot(String settingName, float value) {
                if(editor == null)
                    loadPrefs();
                return editor.putFloat(settingName, value).commit();
            }


            protected static boolean getBool(String settingName) {
                return getBool(settingName, false);
            }

            protected static boolean getBool(String settingName, boolean defaultValue) {
                if (prefs == null)
                    loadPrefs();
                return prefs.getBoolean(settingName, defaultValue);
            }

            protected static boolean setBool(String settingName, boolean value) {
                if(editor == null)
                    loadPrefs();
                return editor.putBoolean(settingName, value).commit();
            }
        }


        public static class DeviceSettings extends PreferenceHelper {
            //region Preference Names
            private static final String SETTINGS_CREATED = "SettingsCreated";
            private static final String DEVELOPER_OPTIONS = "DeveloperOptions";

            public static final String DROP_ZERO = "DropZero";
            public static final String ROUND_POINTS = "RoundPoints";

            public static final String GPS_EXTERNAL = "GpsExternal";
            public static final String GPS_ALWAYS_ON = "GpsAlwaysOn";
            public static final String LOG_ALL_GPS = "LogAllGps";
            public static final String GPS_CONFIGURED = "GPSConfigured";
            public static final String GPS_LOG_BURST_DETAILS = "GPSLogBurstDetails";

            public static final String GPS_DEVICE_ID = "GpsDeviceID";
            public static final String GPS_DEVICE_NAME = "GpsDeviceName";
            public static final String LASER_DEVICE_ID = "RangeFinderDeviceID";
            public static final String LASER_DEVICE_NAME = "RangeFinderDeviceName";

            public static final String GPS_FILTER_DOP_TYPE = "GpsFilterDopType";
            public static final String GPS_FILTER_DOP_VALUE = "GpsFilterDopValue";
            public static final String GPS_FILTER_FIX_TYPE = "GpsFilterFixType";

            public static final String TAKE5_FILTER_DOP_TYPE = "Take5FilterDopType";
            public static final String TAKE5_FILTER_DOP_VALUE = "Take5FilterDopValue";
            public static final String TAKE5_FILTER_FIX_TYPE = "Take5FilterFixType";
            public static final String TAKE5_NMEA_AMOUNT = "Take5NmeaAmount";
            public static final String TAKE5_IGNORE_FIRST_NMEA = "Take5IgnoreNmea";
            public static final String TAKE5_IGNORE_FIRST_NMEA_AMOUNT = "Take5IgnoreNmeaAmount";
            public static final String TAKE5_FAIL_AMOUNT = "Take5FailAmount";
            public static final String TAKE5_INCREMENT = "Take5Increment";
            public static final String TAKE5_VIBRATE_ON_CREATE = "Take5VibrationOnCreate";
            public static final String TAKE5_RING_ON_CREATE = "Take5RingOnCreate";
            
            public static final String WALK_FILTER_DOP_TYPE = "WalkFilterDopType";
            public static final String WALK_FILTER_DOP_VALUE = "WalkFilterDopValue";
            public static final String WALK_FILTER_FIX_TYPE = "WalkFilterFixType";
            public static final String WALK_FILTER_ACCURACY = "WalkFilterAccuracy";
            public static final String WALK_FILTER_FREQUENCY = "WalkFilterFrequency";
            public static final String WALK_INCREMENT = "WalkIncrement";
            public static final String WALK_VIBRATE_ON_CREATE = "WalkVibrationOnCreate";
            public static final String WALK_RING_ON_CREATE = "WalkRingOnCreate";
            public static final String WALK_SHOW_ALL_POINTS_ON_MAP = "WalkShowAllPointsOnMap";

            public static final String AUTO_UPDATE_INDEX = "AutoUpdateIndex";
            public static final String AUTO_SET_GPS_NAME_TO_META = "AutoSetGpsNameToMeta";
            public static final String AUTO_SET_GPS_NAME_TO_META_ASK = "AutoSetGpsNameToMetaAsk";
            public static final String AUTO_UPDATE_WALK_ONBND = "AutoUpdateWalkOnBnd";
            public static final String AUTO_UPDATE_WALK_ONBND_ASK = "AutoUpdateWalkOnBndAsk";
            public static final String AUTO_OVERWRITE_PLOTGRID = "AutoOverwritePlotGrid";
            public static final String AUTO_OVERWRITE_PLOTGRID_ASK = "AutoOverwritePlotGridAsk";
            public static final String AUTO_OVERWRITE_EXPORT = "AutoOverwriteExport";
            public static final String AUTO_OVERWRITE_EXPORT_ASK = "AutoOverwriteExportAsk";

            public static final String AUTO_OPEN_LAST_PROJECT = "AutoOpenLastProject";
            public static final String LAST_OPENED_PROJECT = "AutoOpenLastProject";

            public static final String MAP_TRACKING_OPTION = "MapTrackingOption";
            //public static final String MAP_ZOOM_BUTTONS = "MapZoomButtons";
            public static final String MAP_COMPASS_ENABLED = "MapCompassEnabled";
            public static final String MAP_MYPOS_BUTTON = "MapMyPosButton";
            public static final String MAP_MIN_DIST = "MapMinDist";
            public static final String MAP_SHOW_MY_POS = "MapShowMyPos";
            public static final String MAP_DISPLAY_GPS_LOCATION = "MapDisplayGpsLocation";
            public static final String MAP_USE_UTM_NAV = "MapUseUtmNav";
            public static final String MAP_TYPE = "MapType";
            public static final String MAP_ID = "MapTerrainType";
            public static final String ARC_GIS_MAPS = "ArcGISMaps";
            public static final String ARC_GIS_MAP_ID_COUNTER = "ArcGISMapIdCounter";
            //endregion

            //region Default Values
            public static final boolean DEFAULT_DROP_ZERO = true;
            public static final boolean DEFAULT_ROUND_POINTS = true;

            public static final Units.DopType DEFAULT_GPS_DOP_TYPE = Units.DopType.HDOP;
            public static final GGASentence.GpsFixType DEFAULT_GPS_FIX_TYPE = GGASentence.GpsFixType.GPS;
            public static final int DEFAULT_GPS_DOP_VALUE = 20;

            public static final Units.DopType DEFAULT_TAKE5_DOP_TYPE = Units.DopType.HDOP;
            public static final GGASentence.GpsFixType DEFAULT_TAKE5_FIX_TYPE = GGASentence.GpsFixType.GPS;
            public static final int DEFAULT_TAKE5_DOP_VALUE = 20;
            public static final int DEFAULT_TAKE5_INCREMENT = 5;
            public static final int DEFAULT_TAKE5_NMEA_AMOUNT = 5;
            public static final boolean DEFAULT_TAKE5_IGNORE = false;
            public static final int DEFAULT_TAKE5_IGNORE_AMOUNT = 2;
            public static final int DEFAULT_TAKE5_FAIL_AMOUNT = 10;
            public static final boolean DEFAULT_TAKE5_VIB_ON_CREATE = true;
            public static final boolean DEFAULT_TAKE5_RING_ON_CREATE = true;

            public static final Units.DopType DEFAULT_WALK_DOP_TYPE = Units.DopType.HDOP;
            public static final GGASentence.GpsFixType DEFAULT_WALK_FIX_TYPE = GGASentence.GpsFixType.GPS;
            public static final int DEFAULT_WALK_DOP_VALUE = 20;
            public static final int DEFAULT_WALK_INCREMENT = 2;
            public static final int DEFAULT_WALK_ACCURACY = 0;
            public static final int DEFAULT_WALK_FREQUENCY = 10;
            public static final boolean DEFAULT_WALK_VIB_ON_CREATE = true;
            public static final boolean DEFAULT_WALK_RING_ON_CREATE = true;
            public static final boolean DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP = true;

            public static final boolean DEFAULT_AUTO_OPEN_LAST_PROJECT = true;

            public static final int DEFAULT_POINT_ACCURACY = 6;
            public static final double MIN_POINT_ACCURACY = 0.0001;

            public static final boolean DEFAULT_GPS_LOG_BURST_DETAILS = false;

            public static final int DEFAULT_AUTO_SET_GPS_NAME_TO_META = 0;
            public static final boolean DEFAULT_AUTO_SET_GPS_NAME_TO_META_ASK = true;
            public static final int DEFAULT_AUTO_UPDATE_WALK_ONBND = 0;
            public static final boolean DEFAULT_AUTO_UPDATE_WALK_ONBND_ASK = true;
            public static final int DEFAULT_AUTO_OVERWRITE_PLOTGRID = 0;
            public static final boolean DEFAULT_AUTO_OVERWRITE_PLOTGRID_ASK = true;
            public static final int DEFAULT_AUTO_OVERWRITE_EXPORT = 0;
            public static final boolean DEFAULT_AUTO_OVERWRITE_EXPORT_ASK = true;

            public static final Units.MapTracking DEFAULT_MAP_TRACKING_OPTION = Units.MapTracking.POLY_BOUNDS;
            //public static final boolean DEFAULT_MAP_ZOOM_BUTTONS = false;
            public static final boolean DEFAULT_MAP_COMPASS_ENABLED = true;
            public static final boolean DEFAULT_MAP_MYPOS_BUTTON = true;
            public static final double DEFAULT_MAP_MIN_DIST = 50;
            public static final boolean DEFAULT_MAP_SHOW_MY_POS = true;
            public static final boolean DEFAULT_MAP_DISPLAY_GPS_LOCATION = true;
            public static final boolean DEFAULT_MAP_USE_UTM_NAV = true;
            public static final int DEFAULT_MAP_TYPE = 1;
            public static final int DEFAULT_MAP_ID = 1;
            public static final String DEFAULT_ARC_GIS_MAPS = StringEx.Empty;
            public static final int DEFAULT_ARC_GIS_MAP_ID_COUNTER = 0;
            //endregion

            public static void init() {
                if (!getBool(SETTINGS_CREATED, false)) {
                    reset();
                }
            }

            public static void reset() {
                if (getEditor() == null) {
                    loadPrefs();
                }

                SharedPreferences.Editor editor = getEditor();

                editor.putBoolean(DEVELOPER_OPTIONS, false);

                editor.putBoolean(DROP_ZERO, DEFAULT_DROP_ZERO);
                editor.putBoolean(ROUND_POINTS, DEFAULT_ROUND_POINTS);

                editor.putInt(GPS_FILTER_DOP_TYPE, DEFAULT_GPS_DOP_TYPE.getValue());
                editor.putInt(GPS_FILTER_DOP_VALUE, DEFAULT_GPS_DOP_VALUE);
                editor.putInt(GPS_FILTER_FIX_TYPE, DEFAULT_GPS_FIX_TYPE.getValue());

                editor.putInt(TAKE5_FILTER_DOP_TYPE, DEFAULT_TAKE5_DOP_TYPE.getValue());
                editor.putInt(TAKE5_FILTER_DOP_VALUE, DEFAULT_TAKE5_DOP_VALUE);
                editor.putInt(TAKE5_FILTER_FIX_TYPE, DEFAULT_TAKE5_FIX_TYPE.getValue());
                editor.putInt(TAKE5_NMEA_AMOUNT, DEFAULT_TAKE5_NMEA_AMOUNT);
                editor.putBoolean(TAKE5_IGNORE_FIRST_NMEA, DEFAULT_TAKE5_IGNORE);
                editor.putInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_TAKE5_IGNORE_AMOUNT);
                editor.putInt(TAKE5_FAIL_AMOUNT, DEFAULT_TAKE5_FAIL_AMOUNT);
                editor.putInt(TAKE5_INCREMENT, DEFAULT_TAKE5_INCREMENT);
                editor.putBoolean(TAKE5_RING_ON_CREATE, DEFAULT_TAKE5_RING_ON_CREATE);
                editor.putBoolean(TAKE5_VIBRATE_ON_CREATE, DEFAULT_TAKE5_VIB_ON_CREATE);

                editor.putInt(WALK_FILTER_DOP_TYPE, DEFAULT_WALK_DOP_TYPE.getValue());
                editor.putInt(WALK_FILTER_DOP_VALUE, DEFAULT_WALK_DOP_VALUE);
                editor.putInt(WALK_FILTER_FIX_TYPE, DEFAULT_WALK_FIX_TYPE.getValue());
                editor.putInt(WALK_FILTER_ACCURACY, DEFAULT_WALK_ACCURACY);
                editor.putInt(WALK_FILTER_FREQUENCY, DEFAULT_WALK_FREQUENCY);
                editor.putInt(WALK_INCREMENT, DEFAULT_WALK_INCREMENT);
                editor.putBoolean(WALK_SHOW_ALL_POINTS_ON_MAP, DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP);
                editor.putBoolean(WALK_RING_ON_CREATE, DEFAULT_WALK_RING_ON_CREATE);
                editor.putBoolean(WALK_VIBRATE_ON_CREATE, DEFAULT_WALK_VIB_ON_CREATE);

                editor.putInt(AUTO_SET_GPS_NAME_TO_META, DEFAULT_AUTO_SET_GPS_NAME_TO_META);
                editor.putBoolean(AUTO_SET_GPS_NAME_TO_META_ASK, DEFAULT_AUTO_SET_GPS_NAME_TO_META_ASK);
                editor.putInt(AUTO_UPDATE_WALK_ONBND, DEFAULT_AUTO_UPDATE_WALK_ONBND);
                editor.putBoolean(AUTO_UPDATE_WALK_ONBND_ASK, DEFAULT_AUTO_UPDATE_WALK_ONBND_ASK);
                editor.putInt(AUTO_OVERWRITE_PLOTGRID, DEFAULT_AUTO_OVERWRITE_PLOTGRID);
                editor.putBoolean(AUTO_OVERWRITE_PLOTGRID_ASK, DEFAULT_AUTO_OVERWRITE_PLOTGRID_ASK);
                editor.putInt(AUTO_OVERWRITE_EXPORT, DEFAULT_AUTO_OVERWRITE_EXPORT);
                editor.putBoolean(AUTO_OVERWRITE_EXPORT_ASK, DEFAULT_AUTO_OVERWRITE_EXPORT_ASK);


                editor.putBoolean(AUTO_OPEN_LAST_PROJECT, DEFAULT_AUTO_OPEN_LAST_PROJECT);

                editor.putInt(MAP_TRACKING_OPTION, DEFAULT_MAP_TRACKING_OPTION.getValue());
                editor.putBoolean(MAP_MYPOS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
                editor.putBoolean(MAP_COMPASS_ENABLED, DEFAULT_MAP_COMPASS_ENABLED);
                //editor.putBoolean(MAP_ZOOM_BUTTONS, DEFAULT_MAP_ZOOM_BUTTONS);
                editor.putBoolean(MAP_SHOW_MY_POS, DEFAULT_MAP_SHOW_MY_POS);
                editor.putBoolean(MAP_DISPLAY_GPS_LOCATION, DEFAULT_MAP_DISPLAY_GPS_LOCATION);
                editor.putBoolean(MAP_USE_UTM_NAV, DEFAULT_MAP_USE_UTM_NAV);
                editor.putInt(MAP_TYPE, DEFAULT_MAP_TYPE);
                editor.putInt(MAP_ID, DEFAULT_MAP_ID);
                editor.putString(ARC_GIS_MAPS, DEFAULT_ARC_GIS_MAPS);
                editor.putInt(ARC_GIS_MAP_ID_COUNTER, DEFAULT_ARC_GIS_MAP_ID_COUNTER);

                editor.putBoolean(SETTINGS_CREATED, true);

                editor.commit();
            }

            //region GPS Settings
            public static boolean getGpsExternal() {
                return getBool(GPS_EXTERNAL, false);
            }

            public static void setGpsExternal(boolean value) {
                setBool(GPS_EXTERNAL, value);
            }

            /*
            public static String getGpsPort() {
                return getString(GPS_PORT);
            }

            public static void setGpsPort(String value) {
                setString(GPS_PORT, value);
            }

            public static int getGpsBaud() {
                return getInt(GPS_BAUD);
            }

            public static void setGpsBaud(int value) {
                setInt(GPS_BAUD, value);
            }
            */


            public static String getGpsDeviceID() {
                return getString(GPS_DEVICE_ID);
            }

            public static void setGpsDeviceId(String value) {
                setString(GPS_DEVICE_ID, value);
            }


            public static String getGpsDeviceName() {
                return getString(GPS_DEVICE_NAME);
            }

            public static void setGpsDeviceName(String value) {
                setString(GPS_DEVICE_NAME, value);
            }


            public static boolean isGpsAlwaysOn() {
                return getBool(GPS_ALWAYS_ON);
            }

            public static void setGpsAlwaysOn(boolean value) {
                setBool(GPS_ALWAYS_ON, value);
            }


            public static boolean getLogAllGPS() {
                return getBool(LOG_ALL_GPS);
            }

            public static void setLogAllGps(boolean value) {
                setBool(LOG_ALL_GPS, value);
            }


            public static boolean isGpsConfigured() {
                return getBool(GPS_CONFIGURED);
            }

            public static void setGpsConfigured(boolean value) {
                setBool(GPS_CONFIGURED, value);
            }


            public static boolean getGpsLogBurstDetails() {
                return getBool(GPS_LOG_BURST_DETAILS, DEFAULT_GPS_LOG_BURST_DETAILS);
            }

            public static void setGpsLogBurstDetails(boolean value) {
                setBool(GPS_LOG_BURST_DETAILS, value);
            }
            //endregion

            //region RangeFinder Settings
            public static String getRangeFinderDeviceID() {
                return getString(LASER_DEVICE_ID);
            }

            public static void setRangeFinderDeviceId(String value) {
                setString(LASER_DEVICE_ID, value);
            }


            public static String getRangeFinderDeviceName() {
                return getString(LASER_DEVICE_NAME);
            }

            public static void setRangeFinderDeviceName(String value) {
                setString(LASER_DEVICE_NAME, value);
            }

            /*
            public static String getRangeFinderPort() {
                return getString(LASER_PORT);
            }

            public static void setRangeFinderPort(String value) {
                setString(LASER_PORT, value);
            }

            public static int getRangeFinderBaud() {
                return getInt(LASER_BAUD);
            }

            public static void setRangeFinderBaud(int value) {
                setInt(LASER_BAUD, value);
            }
            */
            //endregion

            //region Filters
            public static Units.DopType getGpsFilterDopType() {
                return Units.DopType.parse(getInt(GPS_FILTER_DOP_TYPE, DEFAULT_GPS_DOP_TYPE.getValue()));
            }

            public static void setGpsFilterDopType(Units.DopType value) {
                setInt(GPS_FILTER_DOP_TYPE, value.getValue());
            }

            public static double getGpsFilterDopValue() {
                return getInt(GPS_FILTER_DOP_VALUE, DEFAULT_GPS_DOP_VALUE);
            }

            public static void setGpsFilterDopValue(double value) {
                setDouble(GPS_FILTER_DOP_VALUE, value);
            }

            public static GGASentence.GpsFixType getGpsFilterFixType() {
                return GGASentence.GpsFixType.parse(getInt(GPS_FILTER_FIX_TYPE, DEFAULT_GPS_FIX_TYPE.getValue()));
            }

            public static void setGpsFilterFixType(GGASentence.GpsFixType value) {
                setInt(GPS_FILTER_FIX_TYPE, value.getValue());
            }


            public static Units.DopType getTake5FilterDopType() {
                return Units.DopType.parse(getInt(TAKE5_FILTER_DOP_TYPE, DEFAULT_TAKE5_DOP_TYPE.getValue()));
            }

            public static void setTake5FilterDopType(Units.DopType value) {
                setInt(TAKE5_FILTER_DOP_TYPE, value.getValue());
            }

            public static double getTake5FilterDopValue() {
                return getInt(TAKE5_FILTER_DOP_VALUE, DEFAULT_TAKE5_DOP_VALUE);
            }

            public static void setTake5FilterDopValue(double value) {
                setDouble(TAKE5_FILTER_DOP_VALUE, value);
            }

            public static GGASentence.GpsFixType getTake5FilterFixType() {
                return GGASentence.GpsFixType.parse(getInt(TAKE5_FILTER_FIX_TYPE, DEFAULT_TAKE5_FIX_TYPE.getValue()));
            }

            public static void setTake5FilterFixType(GGASentence.GpsFixType value) {
                setInt(TAKE5_FILTER_FIX_TYPE, value.getValue());
            }
            
            
            public static Units.DopType getWalkFilterDopType() {
                return Units.DopType.parse(getInt(WALK_FILTER_DOP_TYPE, DEFAULT_WALK_DOP_TYPE.getValue()));
            }

            public static void setWalkFilterDopType(Units.DopType value) {
                setInt(WALK_FILTER_DOP_TYPE, value.getValue());
            }

            public static double getWalkFilterDopValue() {
                return getInt(WALK_FILTER_DOP_VALUE, DEFAULT_WALK_DOP_VALUE);
            }

            public static void setWalkFilterDopValue(double value) {
                setDouble(WALK_FILTER_DOP_VALUE, value);
            }

            public static GGASentence.GpsFixType getWalkFilterFixType() {
                return GGASentence.GpsFixType.parse(getInt(WALK_FILTER_FIX_TYPE, DEFAULT_WALK_FIX_TYPE.getValue()));
            }

            public static void setWalkFilterFixType(GGASentence.GpsFixType value) {
                setInt(WALK_FILTER_FIX_TYPE, value.getValue());
            }


            public static int getWalkFilterAccuracy() {
                return getInt(WALK_FILTER_ACCURACY, DEFAULT_WALK_ACCURACY);
            }

            public static void setWalkFilterAccuracy(int value) {
                setInt(WALK_FILTER_ACCURACY, value);
            }

            public static int getWalkFilterFrequency() {
                return getInt(WALK_FILTER_FREQUENCY, DEFAULT_WALK_FREQUENCY);
            }

            public static void setWalkFilterFrequency(int value) {
                setInt(WALK_FILTER_FREQUENCY, value);
            }
            //endregion

            //region Take5
            public static int getTake5NmeaAmount() {
                return getInt(TAKE5_NMEA_AMOUNT, DEFAULT_TAKE5_NMEA_AMOUNT);
            }

            public static void setTake5NmeaAmount(int value) {
                setInt(TAKE5_NMEA_AMOUNT, value);
            }

            public static boolean getTake5IngoreFirstNmea() {
                return getBool(TAKE5_IGNORE_FIRST_NMEA);
            }

            public static void setTake5IgnoreFirstNmea(boolean value) {
                setBool(TAKE5_IGNORE_FIRST_NMEA, value);
            }

            public static int getTake5IngoreFirstNmeaAmount() {
                return getInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_TAKE5_IGNORE_AMOUNT);
            }

            public static void setTake5IgnoreFirstNmeaAmount(int value) {
                setInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, value);
            }

            public static int getTake5FailAmount() {
                return getInt(TAKE5_FAIL_AMOUNT, DEFAULT_TAKE5_FAIL_AMOUNT);
            }

            public static void setTake5FailAmount(int value) {
                setInt(TAKE5_FAIL_AMOUNT, value);
            }

            public static int getTake5Increment() {
                return getInt(TAKE5_INCREMENT, DEFAULT_TAKE5_INCREMENT);
            }

            public static void setTake5Increment(int value) {
                setInt(TAKE5_INCREMENT, value);
            }


            public static boolean getTake5VibrateOnCreate() {
                return getBool(TAKE5_VIBRATE_ON_CREATE, DEFAULT_TAKE5_VIB_ON_CREATE);
            }

            public static void setTake5VibrateOnCreate(boolean value) {
                setBool(TAKE5_VIBRATE_ON_CREATE, value);
            }


            public static boolean getTake5RingOnCreate() {
                return getBool(TAKE5_RING_ON_CREATE, DEFAULT_TAKE5_RING_ON_CREATE);
            }

            public static void setTake5RingOnCreate(boolean value) {
                setBool(TAKE5_RING_ON_CREATE, value);
            }
            //endregion

            //region Walk
            public static int getWalkIncrement() {
                return getInt(WALK_INCREMENT, DEFAULT_WALK_INCREMENT);
            }

            public static void setWalkIncrement(int value) {
                setInt(WALK_INCREMENT, value);
            }


            public static boolean getWalkVibrateOnCreate() {
                return getBool(WALK_VIBRATE_ON_CREATE, DEFAULT_WALK_VIB_ON_CREATE);
            }

            public static void setWalkVibrateOnCreate(boolean value) {
                setBool(WALK_VIBRATE_ON_CREATE, value);
            }


            public static boolean getWalkRingOnCreate() {
                return getBool(WALK_RING_ON_CREATE, DEFAULT_WALK_RING_ON_CREATE);
            }

            public static void setWalkRingOnCreate(boolean value) {
                setBool(WALK_RING_ON_CREATE, value);
            }


            public static boolean getWalkShowAllPointsOnMap() {
                return getBool(WALK_SHOW_ALL_POINTS_ON_MAP, DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP);
            }

            public static void setWalkShowAllPointsOnMap(boolean value) {
                setBool(WALK_SHOW_ALL_POINTS_ON_MAP, value);
            }
            //endregion

            //region Adjuster
            public static boolean getAutoUpdateIndex() {
                return getBool(AUTO_UPDATE_INDEX);
            }

            public static void setAutoUpdateIndex(boolean value) {
                setBool(AUTO_UPDATE_INDEX, value);
            }
            //endregion

            //region Map
            public static Units.MapTracking getMapTrackingOption() {
                return Units.MapTracking.parse(getInt(MAP_TRACKING_OPTION, DEFAULT_MAP_TRACKING_OPTION.getValue()));
            }

            public static void setMapTrackingOption(Units.MapTracking mapTrackingOption) {
                setInt(MAP_TRACKING_OPTION, mapTrackingOption.getValue());
            }


            public static boolean getMapMyPosBtns() {
                return getBool(MAP_MYPOS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
            }

            public static void setMapMyposButton(boolean value) {
                setBool(MAP_MYPOS_BUTTON, value);
            }

            public static boolean getMapCompassEnabled() {
                return getBool(MAP_COMPASS_ENABLED, DEFAULT_MAP_COMPASS_ENABLED);
            }

            public static void setMapCompassEnabled(boolean value) {
                setBool(MAP_COMPASS_ENABLED, value);
            }


            public static double getMapMinDist() {
                return getDouble(MAP_MIN_DIST, DEFAULT_MAP_MIN_DIST);
            }

            public static void setMapMinDist(double value) {
                setDouble(MAP_MIN_DIST, value);
            }


            public static boolean getMapShowMyPos() {
                return getBool(MAP_SHOW_MY_POS, DEFAULT_MAP_SHOW_MY_POS);
            }

            public static void setMapShowMyPos(boolean value) {
                setBool(MAP_SHOW_MY_POS, value);
            }


            public static boolean getMapDisplayGpsLocation() {
                return getBool(MAP_DISPLAY_GPS_LOCATION, DEFAULT_MAP_DISPLAY_GPS_LOCATION);
            }

            public static void setMapDisplayGpsLocation(boolean value) {
                setBool(MAP_DISPLAY_GPS_LOCATION, value);
            }


            public static boolean getMapUseUtmNav() {
                return getBool(MAP_USE_UTM_NAV, DEFAULT_MAP_USE_UTM_NAV);
            }

            public static void setMapUseUtmNav(boolean value) {
                setBool(MAP_USE_UTM_NAV, value);
            }


            public static Units.MapType getMapType() {
                return Units.MapType.parse(getInt(MAP_TYPE, DEFAULT_MAP_TYPE));
            }

            public static void setMapType(Units.MapType value) {
                setInt(MAP_TYPE, value.getValue());
            }


            public static int getMapId() {
                return getInt(MAP_ID, DEFAULT_MAP_ID);
            }

            public static void setMapId(int value) {
                setInt(MAP_ID, value);
            }



            //region ArcGIS

            public static int getArcGisIdCounter() {
                return getInt(ARC_GIS_MAP_ID_COUNTER, DEFAULT_ARC_GIS_MAP_ID_COUNTER);
            }

            public static void setArcGisMapIdCounter(int value) {
                setInt(ARC_GIS_MAP_ID_COUNTER, value);
            }

            public static ArrayList<ArcGisMapLayer> getArcGisMayLayers() {
                if(getPrefs() == null)
                    loadPrefs();

                Gson gson = new Gson();
                String json = getPrefs().getString(ARC_GIS_MAPS, null);

                if(StringEx.isEmpty(json))
                    return new ArrayList<>();

                return gson.fromJson(json, new TypeToken<ArrayList<ArcGisMapLayer>>() { }.getType());
            }

            public static boolean setArcGisMayLayers(Collection<ArcGisMapLayer> recentProjects) {
                if(getEditor() == null)
                    loadPrefs();

                return getEditor().putString(ARC_GIS_MAPS, new Gson().toJson(recentProjects)).commit();
            }
            //endregion


            //endregion

            //region Other
            public static boolean isDeveloperOptionsEnabled() {
                return getBool(DEVELOPER_OPTIONS, false);
            }

            public static void enabledDevelopterOptions(boolean value) {
                setBool(DEVELOPER_OPTIONS, value);
            }


            public static boolean getDropZeros() {
                return getBool(DROP_ZERO, true);
            }

            public static void setDropZeros(boolean value) {
                setBool(DROP_ZERO, value);
            }


            public static boolean getRoundPoints() {
                return getBool(ROUND_POINTS, true);
            }

            public static void setRoundPoints(boolean value) {
                setBool(ROUND_POINTS, value);
            }



            public static int getAutoSetGpsNameToMeta() {
                return getInt(AUTO_SET_GPS_NAME_TO_META);
            }

            public static void setAutoSetGpsNameToMeta(int value) {
                setInt(AUTO_SET_GPS_NAME_TO_META, value);
            }


            public static boolean getAutoSetGpsNameToMetaAsk() {
                return getBool(AUTO_SET_GPS_NAME_TO_META_ASK);
            }

            public static void setAutoSetGpsNameToMetaAsk(boolean value) {
                setBool(AUTO_SET_GPS_NAME_TO_META_ASK, value);
            }



            public static int getAutoUpdateWalkOnBnd() {
                return getInt(AUTO_UPDATE_WALK_ONBND);
            }

            public static void setAutoUpdateWalkOnBnd(int value) {
                setInt(AUTO_UPDATE_WALK_ONBND, value);
            }


            public static boolean getAutoUpdateWalkOnBndAsk() {
                return getBool(AUTO_UPDATE_WALK_ONBND_ASK);
            }

            public static void setAutoUpdateWalkOnBndAsk(boolean value) {
                setBool(AUTO_UPDATE_WALK_ONBND_ASK, value);
            }



            public static int getAutoOverwritePlotGrid() {
                return getInt(AUTO_OVERWRITE_PLOTGRID);
            }

            public static void setAutoOverwritePlotGrid(int value) {
                setInt(AUTO_OVERWRITE_PLOTGRID, value);
            }


            public static boolean getAutoOverwritePlotGridAsk() {
                return getBool(AUTO_OVERWRITE_PLOTGRID_ASK);
            }

            public static void setAutoOverwritePlotGridAsk(boolean value) {
                setBool(AUTO_OVERWRITE_PLOTGRID_ASK, value);
            }



            public static int getAutoOverwriteExport() {
                return getInt(AUTO_OVERWRITE_EXPORT);
            }

            public static void setAutoOverwriteExport(int value) {
                setInt(AUTO_OVERWRITE_EXPORT, value);
            }


            public static boolean getAutoOverwriteExportAsk() {
                return getBool(AUTO_OVERWRITE_EXPORT_ASK);
            }

            public static void setAutoOverwriteExportAsk(boolean value) {
                setBool(AUTO_OVERWRITE_EXPORT_ASK, value);
            }





            public static String getLastOpenedProject() {
                return getString(LAST_OPENED_PROJECT);
            }

            public static void setLastOpenedProject(String value) {
                setString(LAST_OPENED_PROJECT, value);
            }


            public static boolean getAutoOpenLastProject() {
                return getBool(AUTO_OPEN_LAST_PROJECT);
            }

            public static void setAutoOpenLastProject(boolean value) {
                setBool(AUTO_OPEN_LAST_PROJECT, value);
            }
            //endregion
        }


        public static class ProjectSettings extends PreferenceHelper {
            public static final String PROJECT_ID = "ProjectID";  //Zone
            public static final String DESCRIPTION = "Description";  //Zone
            public static final String REGION = "Region";  //Zone
            public static final String FOREST = "Forest";
            public static final String DISTRICT = "District";
            public static final String RECENT_PROJS = "Recent";
            public static final String LAST_EDITED_POLY_CN = "LastEditedPolyCN";
            public static final String TRACKED_POLY_CN = "TrackedPolyCN";

            public static void initProjectSettings(DataAccessLayer dal) {
                if(dal != null) {
                    setProjectId(dal.getProjectID());
                    setDescription(dal.getProjectDescription());
                    setRegion(dal.getProjectRegion());
                    setForest(dal.getProjectForest());
                    setDistrict(dal.getProjectDistrict());
                } else {
                    setProjectId("1");
                    setDescription(StringEx.Empty);
                }
            }

            //region Project Settings
            public static String getProjectId() {
                return getString(PROJECT_ID);
            }

            public static void setProjectId(String value) {
                setString(PROJECT_ID, value);
            }


            public static String getDescription() {
                return getString(DESCRIPTION);
            }

            public static void setDescription(String value) {
                setString(DESCRIPTION, value);
            }


            public static String getRegion() {
                return getString(REGION, "13");
            }

            public static void setRegion(String value) {
                setString(REGION, value);
            }


            public static String getForest() {
                return getString(FOREST);
            }

            public static void setForest(String value) {
                setString(FOREST, value);
            }


            public static String getDistrict() {
                return getString(DISTRICT);
            }

            public static void setDistrict(String value) {
                setString(DISTRICT, value);
            }

            public static String getLastEditedPolyCN() {
                return getString(LAST_EDITED_POLY_CN, StringEx.Empty);
            }

            public static void setLastEditedPolyCN(String value) {
                setString(LAST_EDITED_POLY_CN, value);
            }

            public static String getTrackedPolyCN() {
                return getString(TRACKED_POLY_CN, StringEx.Empty);
            }

            public static void setTrackedPolyCN(String value) {
                setString(TRACKED_POLY_CN, value);
            }
            //endregion


            //region Recent Projects
            public static ArrayList<RecentProject> getRecentProjects() {

                if(getPrefs() == null)
                    loadPrefs();

                Gson gson = new Gson();
                String json = getPrefs().getString(RECENT_PROJS, null);

                if(json == null)
                    return new ArrayList<>();

                return gson.fromJson(json, new TypeToken<ArrayList<RecentProject>>() { }.getType());
            }

            public static boolean setRecentProjects(List<RecentProject> recentProjects) {
                if(getEditor() == null)
                    loadPrefs();

                return getEditor().putString(RECENT_PROJS, new Gson().toJson(recentProjects)).commit();
            }

            public static void updateRecentProjects(RecentProject project) {
                ArrayList<RecentProject> newList = new ArrayList<>();
                newList.add(project);

                for(RecentProject p : getRecentProjects()) {
                    if(!project.File.equals(p.File) && TtUtils.fileExists(p.File)) {
                        newList.add(p);
                    }
                }

                if(newList.size() > 7)
                    setRecentProjects(newList.subList(0, 8));
                else
                    setRecentProjects(newList);
            }
            //endregion
        }


        public static class MetaDataSetting extends PreferenceHelper {
            private static final String META_NAME = "Name";
            private static final String META_ZONE = "Zone";
            private static final String META_DATUM = "Datum";
            private static final String META_DISTANCE = "Distance";
            private static final String META_ELEVATION = "Elevation";
            private static final String META_SLOPE = "Slope";
            private static final String META_DECTYPE = "Declination";
            private static final String META_MAGDEC = "MagneticDeclination";
            private static final String META_RECEIVER = "Receiver";
            private static final String META_LASER = "RangeFinder";
            private static final String META_COMPASS = "Compass";
            private static final String META_CREW = "Crew";


            //region Default Meta Settings
            public static String getName() {
                return getString(META_NAME);
            }

            public static void setName(String value) {
                setString(META_NAME, value);
            }


            public static int getZone() {
                return getInt(META_ZONE, 13);
            }

            public static void setZone(int value) {
                setInt(META_ZONE, value);
            }


            public static Units.Datum getDatum() {
                return Units.Datum.parse(getInt(META_DATUM, Units.Datum.NAD83.getValue()));
            }

            public static void setDatm(Units.Datum value) {
                setInt(META_ZONE, value.getValue());
            }


            public static Units.Dist getDistance() {
                return Units.Dist.parse(getInt(META_DISTANCE, Units.Dist.FeetTenths.getValue()));
            }

            public static void setDistance(Units.Dist value) {
                setInt(META_DISTANCE, value.getValue());
            }


            public static UomElevation getElevation() {
                return UomElevation.parse(getInt(META_ELEVATION, UomElevation.Feet.getValue()));
            }

            public static void setElevation(UomElevation value) {
                setInt(META_ELEVATION, value.getValue());
            }


            public static Units.Slope getSlope() {
                return Units.Slope.parse(getInt(META_SLOPE, Units.Slope.Percent.getValue()));
            }

            public static void setSlope(Units.Slope value) {
                setInt(META_SLOPE, value.getValue());
            }


            public static Units.DeclinationType getDeclinationType() {
                return Units.DeclinationType.parse(getInt(META_DECTYPE, Units.DeclinationType.MagDec.getValue()));
            }

            public static void setDeclinationType(Units.DeclinationType value) {
                setInt(META_DECTYPE, value.getValue());
            }


            public static double getDeclination() {
                return getDouble(META_MAGDEC, 0);
            }

            public static void setDeclination(double value) {
                setDouble(META_MAGDEC, value);
            }


            public static String getReceiver() {
                return getString(META_RECEIVER);
            }

            public static void setReceiver(String value) {
                setString(META_RECEIVER, value);
            }


            public static String getRangeFinder() {
                return getString(META_LASER);
            }

            public static void setRangeFinder(String value) {
                setString(META_LASER, value);
            }


            public static String getCompass() {
                return getString(META_COMPASS);
            }

            public static void setCompass(String value) {
                setString(META_COMPASS, value);
            }


            public static String getCrew() {
                return getString(META_CREW);
            }

            public static void setCrew(String value) {
                setString(META_CREW, value);
            }
            //endregion

            public static TtMetadata getDefaultmetaData() {
                TtMetadata meta = new TtMetadata();

                String tmp;

                meta.setCN(Consts.EmptyGuid);

                tmp = getName();
                meta.setName(StringEx.isEmpty(tmp) ? "Default MetaData" : tmp);
                meta.setZone(getZone());
                meta.setDatum(getDatum());
                meta.setDistance(getDistance());
                meta.setElevation(getElevation());
                meta.setSlope(getSlope());
                meta.setDecType(getDeclinationType());
                meta.setMagDec(0);
                meta.setGpsReceiver(getReceiver());
                meta.setRangeFinder(getRangeFinder());
                meta.setCompass(getCompass());
                meta.setCrew(getCrew());

                return meta;
            }

            public static void setDefaultMetadata(TtMetadata metadata) {
                setName(metadata.getName());
                setZone(metadata.getZone());
                setDatm(metadata.getDatum());
                setDistance(metadata.getDistance());
                setElevation(metadata.getElevation());
                setSlope(metadata.getSlope());
                setDeclinationType(metadata.getDecType());
                setDeclination(metadata.getMagDec());
            }
        }
    }


    public static class MapSettings {
        public static HashMap<String, PolygonDrawOptions> PolyOptions;
        public static PolygonDrawOptions MasterPolyOptions;

        //initial values
        public static void init(ArrayList<TtPolygon> polys) {
            HashMap<String, PolygonDrawOptions> polyOptions = new HashMap<>();

            if (MasterPolyOptions == null) {
                MasterPolyOptions = new PolygonDrawOptions();
            }

            boolean polyOptsCreated = PolyOptions != null;

            String key;
            for (TtPolygon poly : polys) {
                key = poly.getCN();

                if (polyOptsCreated && PolyOptions.containsKey(key)) {
                    polyOptions.put(key, PolyOptions.get(key));
                } else {
                    polyOptions.put(key, new PolygonDrawOptions(MasterPolyOptions));
                }
            }

            PolyOptions = polyOptions;
        }
    }
}
