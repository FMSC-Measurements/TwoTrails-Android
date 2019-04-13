package com.usda.fmsc.twotrails;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"WeakerAccess", "unused"})
public class DeviceSettings extends Settings {

    //region Preference Names
    private static final String SETTINGS_CREATED = "SettingsCreated";
    private static final String DEVELOPER_OPTIONS = "DeveloperOptions";
    private static final String LAST_CRASH_TIME = "LastCrashTime";

    public static final String DROP_ZERO = "DropZero";
    public static final String ROUND_POINTS = "RoundPoints";
    public static final String KEEP_SCREEN_ON = "KeepScreenOn";

    public static final String GPS_EXTERNAL = "GpsExternal";
    public static final String GPS_ALWAYS_ON = "GpsAlwaysOn";
    public static final String LOG_ALL_GPS = "LogAllGps";
    public static final String GPS_CONFIGURED = "GPSConfigured";
    public static final String GPS_LOG_BURST_DETAILS = "GPSLogBurstDetails";

    public static final String RANGE_FINDER_ALWAYS_ON = "RangeFinderAlwaysOn";
    public static final String LOG_ALL_RANGE_FINDER = "LogAllRangeFinder";
    public static final String RANGE_FINDER_CONFIGURED = "RangeFinderConfigured";
    public static final String RANGE_FINDER_LOG_BURST_DETAILS = "RangeFinderLogBurstDetails";

    public static final String GPS_DEVICE_ID = "GpsDeviceID";
    public static final String GPS_DEVICE_NAME = "GpsDeviceName";
    public static final String RANGE_FINDER_DEVICE_ID = "RangeFinderDeviceID";
    public static final String RANGE_FINDER_DEVICE_NAME = "RangeFinderDeviceName";

    public static final String AUTO_FILL_FROM_RANGE_FINDER = "AutoFillFromRangeFinder";
    public static final String AUTO_FILL_FROM_RANGE_FINDER_ASK = "AutoFillFromRangeFinderAsk";

    public static final String GPS_FILTER_DOP_TYPE = "GpsFilterDopType";
    public static final String GPS_FILTER_DOP_VALUE = "GpsFilterDopValue";
    public static final String GPS_FILTER_FIX_TYPE = "GpsFilterFixType";
    public static final String GPS_FILTER_FIX = "GpsFilterFix";
    public static final String GPS_FILTER_FIX_USE = "GpsFilterFixUse";

    public static final String TAKE5_FILTER_DOP_TYPE = "Take5FilterDopType";
    public static final String TAKE5_FILTER_DOP_VALUE = "Take5FilterDopValue";
    public static final String TAKE5_FILTER_FIX_TYPE = "Take5FilterFixType";
    public static final String TAKE5_FILTER_FIX = "Take5FilterFix";
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
    public static final String WALK_FILTER_FIX = "WalkFilterFix";
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
    public static final String AUTO_INTERNALIZE_EXPORT = "AutoInternalizeExport";
    public static final String AUTO_INTERNALIZE_EXPORT_ASK = "AutoInternalizeExportAsk";
    public static final String USE_TTCAMERA = "UseTtCamera";
    public static final String USE_TTCAMERA_ASK = "UseTtCameraAsk";

    public static final String AUTO_OPEN_LAST_PROJECT = "AutoOpenLastProject";
    public static final String LAST_OPENED_PROJECT = "AutoOpenLastProject";

    public static final String MAP_TRACKING_OPTION = "MapTrackingOption";
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
    public static final String MAP_ADJ_LINE_WIDTH = "MapAdjLineWidth";
    public static final String MAP_UNADJ_LINE_WIDTH = "MapUnAdjLineWidth";

    public static final String ARC_CREDENTIALS = "ArcCredentials";

    public static final String MEDIA_COPY_TO_PROJECT = "CopyToProject";
    //endregion

    //region Default Values
    public final boolean DEFAULT_DROP_ZERO = true;
    public final boolean DEFAULT_ROUND_POINTS = true;

    public final DopType DEFAULT_GPS_DOP_TYPE = DopType.HDOP;
    public final GGASentence.GpsFixType DEFAULT_GPS_FIX_TYPE = GGASentence.GpsFixType.GPS;
    public final GSASentence.Fix DEFAULT_GPS_FIX = GSASentence.Fix._3D;
    public final int DEFAULT_GPS_DOP_VALUE = 20;
    public final boolean DEFAULT_GPS_FIX_USE = true;

    public final DopType DEFAULT_TAKE5_DOP_TYPE = DopType.HDOP;
    public final GGASentence.GpsFixType DEFAULT_TAKE5_FIX_TYPE = GGASentence.GpsFixType.GPS;
    public final GSASentence.Fix DEFAULT_TAKE5_FIX = GSASentence.Fix._3D;
    public final int DEFAULT_TAKE5_DOP_VALUE = 20;
    public final int DEFAULT_TAKE5_INCREMENT = 5;
    public final int DEFAULT_TAKE5_NMEA_AMOUNT = 5;
    public final boolean DEFAULT_TAKE5_IGNORE = false;
    public final int DEFAULT_TAKE5_IGNORE_AMOUNT = 2;
    public final int DEFAULT_TAKE5_FAIL_AMOUNT = 10;
    public final boolean DEFAULT_TAKE5_VIB_ON_CREATE = true;
    public final boolean DEFAULT_TAKE5_RING_ON_CREATE = true;

    public final DopType DEFAULT_WALK_DOP_TYPE = DopType.HDOP;
    public final GGASentence.GpsFixType DEFAULT_WALK_FIX_TYPE = GGASentence.GpsFixType.GPS;
    public final GSASentence.Fix DEFAULT_WALK_FIX = GSASentence.Fix._3D;
    public final int DEFAULT_WALK_DOP_VALUE = 20;
    public final int DEFAULT_WALK_INCREMENT = 2;
    public final int DEFAULT_WALK_ACCURACY = 0;
    public final int DEFAULT_WALK_FREQUENCY = 10;
    public final boolean DEFAULT_WALK_VIB_ON_CREATE = true;
    public final boolean DEFAULT_WALK_RING_ON_CREATE = true;
    public final boolean DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP = true;

    public final boolean DEFAULT_AUTO_OPEN_LAST_PROJECT = true;

    public final boolean DEFAULT_GPS_LOG_BURST_DETAILS = false;


    public final int DEFAULT_AUTO_FILL_FROM_RANGE_FINDER = 1;
    public final boolean DEFAULT_AUTO_FILL_FROM_RANGE_FINDER_ASK = true;

    public final int DEFAULT_AUTO_SET_GPS_NAME_TO_META = 0;
    public final boolean DEFAULT_AUTO_SET_GPS_NAME_TO_META_ASK = true;
    public final int DEFAULT_AUTO_UPDATE_WALK_ONBND = 0;
    public final boolean DEFAULT_AUTO_UPDATE_WALK_ONBND_ASK = true;
    public final int DEFAULT_AUTO_OVERWRITE_PLOTGRID = 0;
    public final boolean DEFAULT_AUTO_OVERWRITE_PLOTGRID_ASK = true;
    public final int DEFAULT_AUTO_OVERWRITE_EXPORT = 0;
    public final boolean DEFAULT_AUTO_OVERWRITE_EXPORT_ASK = true;
    public final int DEFAULT_AUTO_INTERNALIZE_EXPORT = 0;
    public final boolean DEFAULT_AUTO_INTERNALIZE_EXPORT_ASK = true;

    public final MapTracking DEFAULT_MAP_TRACKING_OPTION = MapTracking.POLY_BOUNDS;
    public final boolean DEFAULT_MAP_COMPASS_ENABLED = true;
    public final boolean DEFAULT_MAP_MYPOS_BUTTON = true;
    public final double DEFAULT_MAP_MIN_DIST = 50;
    public final boolean DEFAULT_MAP_SHOW_MY_POS = true;
    public final boolean DEFAULT_MAP_DISPLAY_GPS_LOCATION = true;
    public final boolean DEFAULT_MAP_USE_UTM_NAV = true;
    public final int DEFAULT_MAP_TYPE = 1;
    public final int DEFAULT_MAP_ID = 1;
    public static final String DEFAULT_ARC_GIS_MAPS = StringEx.Empty;
    public final int DEFAULT_ARC_GIS_MAP_ID_COUNTER = 0;
    public final int DEFAULT_MAP_ADJ_LINE_WIDTH = 6;
    public final int DEFAULT_MAP_UNADJ_LINE_WIDTH = 16;

    public final boolean DEFAULT_MEDIA_COPY_TO_PROJECT = true;
    //endregion


    public DeviceSettings(Context context) {
        super(context);

        init();
    }


    public void init() {
        if (!getBool(SETTINGS_CREATED, false)) {
            reset();
        }
    }

    public void reset() {
        SharedPreferences.Editor editor = getEditor();

        editor.putBoolean(DEVELOPER_OPTIONS, false);

        editor.putString(LAST_CRASH_TIME, StringEx.Empty);

        editor.putBoolean(DROP_ZERO, DEFAULT_DROP_ZERO);
        editor.putBoolean(ROUND_POINTS, DEFAULT_ROUND_POINTS);

        editor.putBoolean(GPS_ALWAYS_ON, true);
        editor.putBoolean(KEEP_SCREEN_ON, false);

        editor.putInt(GPS_FILTER_DOP_TYPE, DEFAULT_GPS_DOP_TYPE.getValue());
        editor.putInt(GPS_FILTER_DOP_VALUE, DEFAULT_GPS_DOP_VALUE);
        editor.putInt(GPS_FILTER_FIX_TYPE, DEFAULT_GPS_FIX_TYPE.getValue());
        editor.putInt(GPS_FILTER_FIX, DEFAULT_GPS_FIX.getValue());
        editor.putBoolean(GPS_FILTER_FIX_USE, DEFAULT_GPS_FIX_USE);

        editor.putInt(AUTO_FILL_FROM_RANGE_FINDER, DEFAULT_AUTO_FILL_FROM_RANGE_FINDER);
        editor.putBoolean(AUTO_FILL_FROM_RANGE_FINDER_ASK, DEFAULT_AUTO_FILL_FROM_RANGE_FINDER_ASK);

        editor.putInt(TAKE5_FILTER_DOP_TYPE, DEFAULT_TAKE5_DOP_TYPE.getValue());
        editor.putInt(TAKE5_FILTER_DOP_VALUE, DEFAULT_TAKE5_DOP_VALUE);
        editor.putInt(TAKE5_FILTER_FIX_TYPE, DEFAULT_TAKE5_FIX_TYPE.getValue());
        editor.putInt(TAKE5_FILTER_FIX, DEFAULT_TAKE5_FIX.getValue());
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
        editor.putInt(WALK_FILTER_FIX, DEFAULT_WALK_FIX.getValue());
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
        editor.putInt(AUTO_INTERNALIZE_EXPORT, DEFAULT_AUTO_INTERNALIZE_EXPORT);
        editor.putBoolean(AUTO_INTERNALIZE_EXPORT_ASK, DEFAULT_AUTO_INTERNALIZE_EXPORT_ASK);

        editor.putInt(USE_TTCAMERA, 2);
        editor.putBoolean(USE_TTCAMERA_ASK, true);


        editor.putBoolean(AUTO_OPEN_LAST_PROJECT, DEFAULT_AUTO_OPEN_LAST_PROJECT);

        editor.putInt(MAP_TRACKING_OPTION, DEFAULT_MAP_TRACKING_OPTION.getValue());
        editor.putBoolean(MAP_MYPOS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
        editor.putBoolean(MAP_COMPASS_ENABLED, DEFAULT_MAP_COMPASS_ENABLED);
        editor.putBoolean(MAP_SHOW_MY_POS, DEFAULT_MAP_SHOW_MY_POS);
        editor.putBoolean(MAP_DISPLAY_GPS_LOCATION, DEFAULT_MAP_DISPLAY_GPS_LOCATION);
        editor.putBoolean(MAP_USE_UTM_NAV, DEFAULT_MAP_USE_UTM_NAV);
        editor.putInt(MAP_TYPE, DEFAULT_MAP_TYPE);
        editor.putInt(MAP_ID, DEFAULT_MAP_ID);
        editor.putString(ARC_GIS_MAPS, DEFAULT_ARC_GIS_MAPS);
        editor.putInt(ARC_GIS_MAP_ID_COUNTER, DEFAULT_ARC_GIS_MAP_ID_COUNTER);
        editor.putInt(MAP_ADJ_LINE_WIDTH, DEFAULT_MAP_ADJ_LINE_WIDTH);
        editor.putInt(MAP_UNADJ_LINE_WIDTH, DEFAULT_MAP_UNADJ_LINE_WIDTH);

        editor.putBoolean(MEDIA_COPY_TO_PROJECT, DEFAULT_MEDIA_COPY_TO_PROJECT);

        editor.putString(ARC_CREDENTIALS, StringEx.Empty);

        editor.putBoolean(SETTINGS_CREATED, true);

        editor.commit();
    }


    //region GPS Settings
    public boolean getGpsExternal() {
        return getBool(GPS_EXTERNAL, false);
    }

    public void setGpsExternal(boolean value) {
        setBool(GPS_EXTERNAL, value);
    }


    public String getGpsDeviceID() {
        return getString(GPS_DEVICE_ID);
    }

    public void setGpsDeviceId(String value) {
        setString(GPS_DEVICE_ID, value);
    }


    public String getGpsDeviceName() {
        return getString(GPS_DEVICE_NAME);
    }

    public void setGpsDeviceName(String value) {
        setString(GPS_DEVICE_NAME, value);
    }


    public boolean isGpsAlwaysOn() {
        return getBool(GPS_ALWAYS_ON);
    }

    public void setGpsAlwaysOn(boolean value) {
        setBool(GPS_ALWAYS_ON, value);
    }


    public boolean getLogAllGPS() {
        return getBool(LOG_ALL_GPS);
    }

    public void setLogAllGps(boolean value) {
        setBool(LOG_ALL_GPS, value);
    }


    public boolean isGpsConfigured() {
        return getBool(GPS_CONFIGURED) || !getBool(GPS_EXTERNAL);
    }

    public void setGpsConfigured(boolean value) {
        setBool(GPS_CONFIGURED, value);
    }


    public boolean getGpsLogBurstDetails() {
        return getBool(GPS_LOG_BURST_DETAILS, DEFAULT_GPS_LOG_BURST_DETAILS);
    }

    public void setGpsLogBurstDetails(boolean value) {
        setBool(GPS_LOG_BURST_DETAILS, value);
    }
    //endregion

    //region RangeFinder Settings
    public String getRangeFinderDeviceID() {
        return getString(RANGE_FINDER_DEVICE_ID);
    }

    public void setRangeFinderDeviceId(String value) {
        setString(RANGE_FINDER_DEVICE_ID, value);
    }


    public String getRangeFinderDeviceName() {
        return getString(RANGE_FINDER_DEVICE_NAME);
    }

    public void setRangeFinderDeviceName(String value) {
        setString(RANGE_FINDER_DEVICE_NAME, value);
    }

    public boolean isRangeFinderAlwaysOn() {
        return getBool(RANGE_FINDER_ALWAYS_ON);
    }

    public void setRangeFinderAlwaysOn(boolean value) {
        setBool(RANGE_FINDER_ALWAYS_ON, value);
    }

    public boolean isRangeFinderConfigured() {
        return getBool(RANGE_FINDER_CONFIGURED);
    }

    public void setRangeFinderConfigured(boolean value) {
        setBool(RANGE_FINDER_CONFIGURED, value);
    }

    public int getAutoFillFromRangeFinder() {
        return getInt(AUTO_FILL_FROM_RANGE_FINDER);
    }

    public boolean isAutoFillFromRangeFinderAsk() {
        return getBool(AUTO_FILL_FROM_RANGE_FINDER_ASK);
    }
    //endregion

    //region Filters
    public DopType getGpsFilterDopType() {
        return DopType.parse(getInt(GPS_FILTER_DOP_TYPE, DEFAULT_GPS_DOP_TYPE.getValue()));
    }

    public void setGpsFilterDopType(DopType value) {
        setInt(GPS_FILTER_DOP_TYPE, value.getValue());
    }

    public int getGpsFilterDopValue() {
        return getInt(GPS_FILTER_DOP_VALUE, DEFAULT_GPS_DOP_VALUE);
    }

    public void setGpsFilterDopValue(int value) {
        setInt(GPS_FILTER_DOP_VALUE, value);
    }

    public GGASentence.GpsFixType getGpsFilterFixType() {
        return GGASentence.GpsFixType.parse(getInt(GPS_FILTER_FIX_TYPE, DEFAULT_GPS_FIX_TYPE.getValue()));
    }

    public void setGpsFilterFixType(GGASentence.GpsFixType value) {
        setInt(GPS_FILTER_FIX_TYPE, value.getValue());
    }

    public GSASentence.Fix getGpsFilterFix() {
        return GSASentence.Fix.parse(getInt(GPS_FILTER_FIX, DEFAULT_GPS_FIX.getValue()));
    }

    public void setGpsFilterFix(GSASentence.Fix value) {
        setInt(GPS_FILTER_FIX, value.getValue());
    }


    public boolean getGpsFilterFixUse() {
        return getBool(GPS_FILTER_FIX_USE, true);
    }

    public void setGpsFilterFixUse(boolean value) {
        setBool(GPS_FILTER_FIX_USE, value);
    }

    public DopType getTake5FilterDopType() {
        return DopType.parse(getInt(TAKE5_FILTER_DOP_TYPE, DEFAULT_TAKE5_DOP_TYPE.getValue()));
    }

    public void setTake5FilterDopType(DopType value) {
        setInt(TAKE5_FILTER_DOP_TYPE, value.getValue());
    }

    public int getTake5FilterDopValue() {
        return getInt(TAKE5_FILTER_DOP_VALUE, DEFAULT_TAKE5_DOP_VALUE);
    }

    public void setTake5FilterDopValue(int value) {
        setInt(TAKE5_FILTER_DOP_VALUE, value);
    }

    public GGASentence.GpsFixType getTake5FilterFixType() {
        return GGASentence.GpsFixType.parse(getInt(TAKE5_FILTER_FIX_TYPE, DEFAULT_TAKE5_FIX_TYPE.getValue()));
    }

    public void setTake5FilterFixType(GGASentence.GpsFixType value) {
        setInt(TAKE5_FILTER_FIX_TYPE, value.getValue());
    }

    public GSASentence.Fix getTake5FilterFix() {
        return GSASentence.Fix.parse(getInt(TAKE5_FILTER_FIX, DEFAULT_TAKE5_FIX.getValue()));
    }

    public void setTake5FilterFix(GSASentence.Fix value) {
        setInt(TAKE5_FILTER_FIX, value.getValue());
    }


    public DopType getWalkFilterDopType() {
        return DopType.parse(getInt(WALK_FILTER_DOP_TYPE, DEFAULT_WALK_DOP_TYPE.getValue()));
    }

    public void setWalkFilterDopType(DopType value) {
        setInt(WALK_FILTER_DOP_TYPE, value.getValue());
    }

    public int getWalkFilterDopValue() {
        return getInt(WALK_FILTER_DOP_VALUE, DEFAULT_WALK_DOP_VALUE);
    }

    public void setWalkFilterDopValue(int value) {
        setInt(WALK_FILTER_DOP_VALUE, value);
    }

    public GGASentence.GpsFixType getWalkFilterFixType() {
        return GGASentence.GpsFixType.parse(getInt(WALK_FILTER_FIX_TYPE, DEFAULT_WALK_FIX_TYPE.getValue()));
    }

    public void setWalkFilterFixType(GGASentence.GpsFixType value) {
        setInt(WALK_FILTER_FIX_TYPE, value.getValue());
    }

    public GSASentence.Fix getWalkFilterFix() {
        return GSASentence.Fix.parse(getInt(WALK_FILTER_FIX, DEFAULT_WALK_FIX.getValue()));
    }

    public void setWalkFilterFix(GSASentence.Fix value) {
        setInt(WALK_FILTER_FIX, value.getValue());
    }


    public int getWalkFilterAccuracy() {
        return getInt(WALK_FILTER_ACCURACY, DEFAULT_WALK_ACCURACY);
    }

    public void setWalkFilterAccuracy(int value) {
        setInt(WALK_FILTER_ACCURACY, value);
    }

    public int getWalkFilterFrequency() {
        return getInt(WALK_FILTER_FREQUENCY, DEFAULT_WALK_FREQUENCY);
    }

    public void setWalkFilterFrequency(int value) {
        setInt(WALK_FILTER_FREQUENCY, value);
    }
    //endregion

    //region Take5
    public int getTake5NmeaAmount() {
        return getInt(TAKE5_NMEA_AMOUNT, DEFAULT_TAKE5_NMEA_AMOUNT);
    }

    public void setTake5NmeaAmount(int value) {
        setInt(TAKE5_NMEA_AMOUNT, value);
    }

    public boolean getTake5IngoreFirstNmea() {
        return getBool(TAKE5_IGNORE_FIRST_NMEA);
    }

    public void setTake5IgnoreFirstNmea(boolean value) {
        setBool(TAKE5_IGNORE_FIRST_NMEA, value);
    }

    public int getTake5IngoreFirstNmeaAmount() {
        return getInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_TAKE5_IGNORE_AMOUNT);
    }

    public void setTake5IgnoreFirstNmeaAmount(int value) {
        setInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, value);
    }

    public int getTake5FailAmount() {
        return getInt(TAKE5_FAIL_AMOUNT, DEFAULT_TAKE5_FAIL_AMOUNT);
    }

    public void setTake5FailAmount(int value) {
        setInt(TAKE5_FAIL_AMOUNT, value);
    }

    public int getTake5Increment() {
        return getInt(TAKE5_INCREMENT, DEFAULT_TAKE5_INCREMENT);
    }

    public void setTake5Increment(int value) {
        setInt(TAKE5_INCREMENT, value);
    }


    public boolean getTake5VibrateOnCreate() {
        return getBool(TAKE5_VIBRATE_ON_CREATE, DEFAULT_TAKE5_VIB_ON_CREATE);
    }

    public void setTake5VibrateOnCreate(boolean value) {
        setBool(TAKE5_VIBRATE_ON_CREATE, value);
    }


    public boolean getTake5RingOnCreate() {
        return getBool(TAKE5_RING_ON_CREATE, DEFAULT_TAKE5_RING_ON_CREATE);
    }

    public void setTake5RingOnCreate(boolean value) {
        setBool(TAKE5_RING_ON_CREATE, value);
    }
    //endregion

    //region Walk
    public int getWalkIncrement() {
        return getInt(WALK_INCREMENT, DEFAULT_WALK_INCREMENT);
    }

    public void setWalkIncrement(int value) {
        setInt(WALK_INCREMENT, value);
    }


    public boolean getWalkVibrateOnCreate() {
        return getBool(WALK_VIBRATE_ON_CREATE, DEFAULT_WALK_VIB_ON_CREATE);
    }

    public void setWalkVibrateOnCreate(boolean value) {
        setBool(WALK_VIBRATE_ON_CREATE, value);
    }


    public boolean getWalkRingOnCreate() {
        return getBool(WALK_RING_ON_CREATE, DEFAULT_WALK_RING_ON_CREATE);
    }

    public void setWalkRingOnCreate(boolean value) {
        setBool(WALK_RING_ON_CREATE, value);
    }


    public boolean getWalkShowAllPointsOnMap() {
        return getBool(WALK_SHOW_ALL_POINTS_ON_MAP, DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP);
    }

    public void setWalkShowAllPointsOnMap(boolean value) {
        setBool(WALK_SHOW_ALL_POINTS_ON_MAP, value);
    }
    //endregion

    //region Adjuster
    public boolean getAutoUpdateIndex() {
        return getBool(AUTO_UPDATE_INDEX);
    }

    public void setAutoUpdateIndex(boolean value) {
        setBool(AUTO_UPDATE_INDEX, value);
    }
    //endregion

    //region Map
    public MapTracking getMapTrackingOption() {
        return MapTracking.parse(getInt(MAP_TRACKING_OPTION, DEFAULT_MAP_TRACKING_OPTION.getValue()));
    }

    public void setMapTrackingOption(MapTracking mapTrackingOption) {
        setInt(MAP_TRACKING_OPTION, mapTrackingOption.getValue());
    }


    public boolean getMapMyPosBtns() {
        return getBool(MAP_MYPOS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
    }

    public void setMapMyposButton(boolean value) {
        setBool(MAP_MYPOS_BUTTON, value);
    }


    public boolean getMapCompassEnabled() {
        return getBool(MAP_COMPASS_ENABLED, DEFAULT_MAP_COMPASS_ENABLED);
    }

    public void setMapCompassEnabled(boolean value) {
        setBool(MAP_COMPASS_ENABLED, value);
    }


    public double getMapMinDist() {
        return getDouble(MAP_MIN_DIST, DEFAULT_MAP_MIN_DIST);
    }

    public void setMapMinDist(double value) {
        setDouble(MAP_MIN_DIST, value);
    }


    public boolean getMapShowMyPos() {
        return getBool(MAP_SHOW_MY_POS, DEFAULT_MAP_SHOW_MY_POS);
    }

    public void setMapShowMyPos(boolean value) {
        setBool(MAP_SHOW_MY_POS, value);
    }


    public boolean getMapDisplayGpsLocation() {
        return getBool(MAP_DISPLAY_GPS_LOCATION, DEFAULT_MAP_DISPLAY_GPS_LOCATION);
    }

    public void setMapDisplayGpsLocation(boolean value) {
        setBool(MAP_DISPLAY_GPS_LOCATION, value);
    }


    public boolean getMapUseUtmNav() {
        return getBool(MAP_USE_UTM_NAV, DEFAULT_MAP_USE_UTM_NAV);
    }

    public void setMapUseUtmNav(boolean value) {
        setBool(MAP_USE_UTM_NAV, value);
    }



    public MapType getMapType() {
        return MapType.parse(getInt(MAP_TYPE, DEFAULT_MAP_TYPE));
    }

    public void setMapType(MapType value) {
        setInt(MAP_TYPE, value.getValue());
    }


    public int getMapId() {
        return getInt(MAP_ID, DEFAULT_MAP_ID);
    }

    public void setMapId(int value) {
        setInt(MAP_ID, value);
    }



    public int getMapAdjLineWidth() {
        return getInt(MAP_ADJ_LINE_WIDTH, DEFAULT_MAP_ADJ_LINE_WIDTH);
    }

    public void setMapAdjLineWidth(int value) {
        setInt(MAP_ADJ_LINE_WIDTH, value);
    }


    public int getMapUnAdjLineWidth() {
        return getInt(MAP_UNADJ_LINE_WIDTH, DEFAULT_MAP_UNADJ_LINE_WIDTH);
    }

    public void setMapUnAdjLineWidth(int value) {
        setInt(MAP_UNADJ_LINE_WIDTH, value);
    }


    //region ArcGIS

    public int getArcGisIdCounter() {
        return getInt(ARC_GIS_MAP_ID_COUNTER, DEFAULT_ARC_GIS_MAP_ID_COUNTER);
    }

    public void setArcGisMapIdCounter(int value) {
        setInt(ARC_GIS_MAP_ID_COUNTER, value);
    }


    public ArrayList<ArcGisMapLayer> getArcGisMayLayers() {
        Gson gson = new Gson();
        String json = getPrefs().getString(ARC_GIS_MAPS, null);

        if(StringEx.isEmpty(json))
            return new ArrayList<>();

        return gson.fromJson(json, new TypeToken<ArrayList<ArcGisMapLayer>>() { }.getType());
    }

    public boolean setArcGisMayLayers(Collection<ArcGisMapLayer> recentProjects) {
        return getEditor().putString(ARC_GIS_MAPS, new Gson().toJson(recentProjects)).commit();
    }
    //endregion

    //endregion

    //region Other
    public boolean isDeveloperOptionsEnabled() {
        return getBool(DEVELOPER_OPTIONS, false);
    }

    public void enabledDevelopterOptions(boolean value) {
        setBool(DEVELOPER_OPTIONS, value);
    }


    public DateTime getLastCrashTime() {
        String time = getString(LAST_CRASH_TIME);
        if (!StringEx.isEmpty(time))
            return DateTime.parse(time);
        return null;
    }

    public void setLastCrashTime(DateTime crashTime) {
        setString(LAST_CRASH_TIME, crashTime.toString());
    }


    public boolean getDropZeros() {
        return getBool(DROP_ZERO, true);
    }

    public void setDropZeros(boolean value) {
        setBool(DROP_ZERO, value);
    }


    public boolean getRoundPoints() {
        return getBool(ROUND_POINTS, true);
    }

    public void setRoundPoints(boolean value) {
        setBool(ROUND_POINTS, value);
    }



    public int getAutoSetGpsNameToMeta() {
        return getInt(AUTO_SET_GPS_NAME_TO_META);
    }

    public void setAutoSetGpsNameToMeta(int value) {
        setInt(AUTO_SET_GPS_NAME_TO_META, value);
    }


    public boolean getAutoSetGpsNameToMetaAsk() {
        return getBool(AUTO_SET_GPS_NAME_TO_META_ASK);
    }

    public void setAutoSetGpsNameToMetaAsk(boolean value) {
        setBool(AUTO_SET_GPS_NAME_TO_META_ASK, value);
    }



    public int getAutoUpdateWalkOnBnd() {
        return getInt(AUTO_UPDATE_WALK_ONBND);
    }

    public void setAutoUpdateWalkOnBnd(int value) {
        setInt(AUTO_UPDATE_WALK_ONBND, value);
    }


    public boolean getAutoUpdateWalkOnBndAsk() {
        return getBool(AUTO_UPDATE_WALK_ONBND_ASK);
    }

    public void setAutoUpdateWalkOnBndAsk(boolean value) {
        setBool(AUTO_UPDATE_WALK_ONBND_ASK, value);
    }



    public int getAutoOverwritePlotGrid() {
        return getInt(AUTO_OVERWRITE_PLOTGRID);
    }

    public void setAutoOverwritePlotGrid(int value) {
        setInt(AUTO_OVERWRITE_PLOTGRID, value);
    }


    public boolean getAutoOverwritePlotGridAsk() {
        return getBool(AUTO_OVERWRITE_PLOTGRID_ASK);
    }

    public void setAutoOverwritePlotGridAsk(boolean value) {
        setBool(AUTO_OVERWRITE_PLOTGRID_ASK, value);
    }



    public int getAutoOverwriteExport() {
        return getInt(AUTO_OVERWRITE_EXPORT);
    }

    public void setAutoOverwriteExport(int value) {
        setInt(AUTO_OVERWRITE_EXPORT, value);
    }


    public boolean getAutoOverwriteExportAsk() {
        return getBool(AUTO_OVERWRITE_EXPORT_ASK);
    }

    public void setAutoOverwriteExportAsk(boolean value) {
        setBool(AUTO_OVERWRITE_EXPORT_ASK, value);
    }


    public int getAutoInternalizeExport() {
        return getInt(AUTO_INTERNALIZE_EXPORT);
    }

    public void setAutoInternalizeExport(int value) {
        setInt(AUTO_INTERNALIZE_EXPORT, value);
    }


    public boolean getAutoInternalizeExportAsk() {
        return getBool(AUTO_INTERNALIZE_EXPORT_ASK);
    }

    public void setAutoInternalizeExportAsk(boolean value) {
        setBool(AUTO_INTERNALIZE_EXPORT_ASK, value);
    }



    public String getLastOpenedProject() {
        return getString(LAST_OPENED_PROJECT);
    }

    public void setLastOpenedProject(String value) {
        setString(LAST_OPENED_PROJECT, value);
    }


    public boolean getAutoOpenLastProject() {
        return getBool(AUTO_OPEN_LAST_PROJECT);
    }

    public void setAutoOpenLastProject(boolean value) {
        setBool(AUTO_OPEN_LAST_PROJECT, value);
    }



    public String getArcCredentials() {
        return getString(ARC_CREDENTIALS, StringEx.Empty);
    }

    public void setArcCredentials(String value) {
        setString(ARC_CREDENTIALS, value);
    }


    public boolean getMediaCopyToProject() {
        return getBool(MEDIA_COPY_TO_PROJECT, DEFAULT_MEDIA_COPY_TO_PROJECT);
    }

    public void setMediaCopyToProject(boolean value) {
        setBool(MEDIA_COPY_TO_PROJECT, value);
    }



    public boolean getUseTtCameraAsk() {
        return getBool(USE_TTCAMERA_ASK, true);
    }

    public void setUseTtCameraAsk(boolean value) {
        setBool(USE_TTCAMERA_ASK, value);
    }

    public int getUseTtCamera() {
        return getInt(USE_TTCAMERA, 2);
        //return getInt(USE_TTCAMERA, Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH ? 2 : 1);
    }

    public void setUseTtCamera(int value) {
        setInt(USE_TTCAMERA, value);
    }


    public boolean getKeepScreenOn() {
        return getBool(KEEP_SCREEN_ON, false);
    }

    public void setKeepScreenOn(boolean value) {
        setBool(KEEP_SCREEN_ON, value);
    }

    //endregion

}
