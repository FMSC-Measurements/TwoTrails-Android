package com.usda.fmsc.twotrails;

import android.util.JsonWriter;

import androidx.annotation.ColorInt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usda.fmsc.geospatial.gnss.codes.GnssFix;
import com.usda.fmsc.geospatial.gnss.codes.GnssFixQuality;
import com.usda.fmsc.twotrails.objects.TwoTrailsProject;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"WeakerAccess", "unused"})
public class DeviceSettings extends Settings {

    //region Preference Names
    private static final String SETTINGS_CREATED = "SettingsCreated";
    private static final String DEVELOPER_OPTIONS = "DeveloperOptions";
    private static final String DEBUG_MODE = "DebugMode";
    private static final String LAST_CRASH_TIME = "LastCrashTime";

    public static final String EXTERNAL_SYNC_ENABLED = "ExternalSync";
    public static final String EXTERNAL_SYNC_ENABLED_ASK = "ExternalSyncAsk";
    public static final String EXTERNAL_SYNC_DIR = "ExternalSyncDir";

    public static final String DROP_ZERO = "DropZero";
    public static final String ROUND_POINTS = "RoundPoints";
    public static final String KEEP_SCREEN_ON = "KeepScreenOn";

    public static final String GPS_EXTERNAL = "GpsExternal";
    public static final String GPS_ALWAYS_ON = "GpsAlwaysOn";

    public static final String GPS_DEVICE_ID = "GpsDeviceID";
    public static final String GPS_DEVICE_NAME = "GpsDeviceName";
    public static final String GPS_PARSE_METHOD = "GpsParseMethod";
    public static final String GPS_PARSE_DELIMITER = "GpsParseDelimiter";
    public static final String GPS_CONFIGURED = "GPSConfigured";
    public static final String LOG_ALL_GPS = "LogAllGps";
    public static final String GPS_LOG_BURST_DETAILS = "GPSLogBurstDetails";

    public static final String RANGE_FINDER_DEVICE_ID = "RangeFinderDeviceID";
    public static final String RANGE_FINDER_DEVICE_NAME = "RangeFinderDeviceName";
    public static final String RANGE_FINDER_ALWAYS_ON = "RangeFinderAlwaysOn";
    public static final String LOG_ALL_RANGE_FINDER = "LogAllRangeFinder";
    public static final String RANGE_FINDER_CONFIGURED = "RangeFinderConfigured";
    public static final String RANGE_FINDER_LOG_BURST_DETAILS = "RangeFinderLogBurstDetails";


    public static final String VN100_DEVICE_ID = "VN100DeviceID";
    public static final String VN100_DEVICE_NAME = "VN100DeviceName";
    public static final String VN100_ALWAYS_ON = "VN100AlwaysOn";
    public static final String VN100_LOG_ALL = "VN100LogAll";
    public static final String VN100_CONFIGURED = "VN100Configured";
    

    public static final String AUTO_FILL_FROM_RANGE_FINDER = "AutoFillFromRangeFinder";
    public static final String AUTO_FILL_FROM_RANGE_FINDER_ASK = "AutoFillFromRangeFinderAsk";

    public static final String GPS_FILTER_DOP_TYPE = "GpsFilterDopType";
    public static final String GPS_FILTER_DOP_VALUE = "GpsFilterDopValue";
    public static final String GPS_FILTER_FIX_QUALITY = "GpsFilterFixType";
    public static final String GPS_FILTER_FIX = "GpsFilterFix";
    public static final String GPS_FILTER_FIX_USE = "GpsFilterFixUse";

    public static final String TAKE5_FILTER_DOP_TYPE = "Take5FilterDopType";
    public static final String TAKE5_FILTER_DOP_VALUE = "Take5FilterDopValue";
    public static final String TAKE5_FILTER_FIX_QUALITY = "Take5FilterFixType";
    public static final String TAKE5_FILTER_FIX = "Take5FilterFix";
    public static final String TAKE5_NMEA_AMOUNT = "Take5NmeaAmount";
    public static final String TAKE5_IGNORE_FIRST_NMEA = "Take5IgnoreNmea";
    public static final String TAKE5_IGNORE_FIRST_NMEA_AMOUNT = "Take5IgnoreNmeaAmount";
    public static final String TAKE5_FAIL_AMOUNT = "Take5FailAmount";
    public static final String TAKE5_INCREMENT = "Take5Increment";
    public static final String TAKE5_VIBRATE_ON_CREATE = "Take5VibrationOnCreate";
    public static final String TAKE5_RING_ON_CREATE = "Take5RingOnCreate";

    public static final String SAT_FILTER_DOP_TYPE = "SATFilterDopType";
    public static final String SAT_FILTER_DOP_VALUE = "SATFilterDopValue";
    public static final String SAT_FILTER_FIX_QUALITY = "SATFilterFixType";
    public static final String SAT_FILTER_FIX = "SATFilterFix";
    public static final String SAT_NMEA_AMOUNT = "SATNmeaAmount";
    public static final String SAT_IGNORE_FIRST_NMEA = "SATIgnoreNmea";
    public static final String SAT_IGNORE_FIRST_NMEA_AMOUNT = "SATIgnoreNmeaAmount";
    public static final String SAT_FAIL_AMOUNT = "SATFailAmount";
    public static final String SAT_INCREMENT = "SATIncrement";
    public static final String SAT_VIBRATE_ON_CREATE = "SATVibrationOnCreate";
    public static final String SAT_RING_ON_CREATE = "SATRingOnCreate";

    public static final String WALK_FILTER_DOP_TYPE = "WalkFilterDopType";
    public static final String WALK_FILTER_DOP_VALUE = "WalkFilterDopValue";
    public static final String WALK_FILTER_FIX_QUALITY = "WalkFilterFixType";
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
    public static final String LAST_OPENED_PROJECT_NAME = "LastOpenedProjectName";
    public static final String LAST_OPENED_PROJECT_FILE_TTX = "LastOpenedProjectFileTTX";
    public static final String LAST_OPENED_PROJECT_FILE_TTMPX = "LastOpenedProjectFileTTMPX";

    public static final String MAP_TRACKING_OPTION = "MapTrackingOption";
    public static final String MAP_COMPASS_ENABLED = "MapCompassEnabled";
    public static final String MAP_MY_POS_BUTTON = "MapMyPosButton";
    public static final String MAP_MIN_DIST = "MapMinDist";
    public static final String MAP_SHOW_MY_POS = "MapShowMyPos";
    public static final String MAP_DISPLAY_GPS_LOCATION = "MapDisplayGpsLocation";
    public static final String MAP_USE_UTM_NAV = "MapUseUtmNav";
    public static final String MAP_TYPE = "MapType";
    public static final String MAP_ID = "MapTerrainType";
    public static final String ARC_GIS_MAPS = "ArcGISMaps";
    public static final String ARC_GIS_MAP_ID_COUNTER = "ArcGISMapIdCounter";
    public static final String MAP_DIST_TO_POLY_LINE_WIDTH = "MapDistToPolyLineWidth";
    public static final String MAP_DIST_TO_POLY_LINE_COLOR = "MapDistToPolyLineColor";
    public static final String MAP_DIST_TO_POLY_LINE_TOLERANCE = "MapDistToPolyLineTolerance";
    public static final String MAP_ADJ_LINE_WIDTH = "MapAdjLineWidth";
    public static final String MAP_UNADJ_LINE_WIDTH = "MapUnAdjLineWidth";
    public static final String MAP_CHOOSE_OFFLINE = "MapChooseOffline";
    public static final String MAP_CHOOSE_OFFLINE_ASK = "MapChooseOfflineAsk";

    public static final String EXPORT_MODE = "MapChooseOffline";
    public static final String EXPORT_MODE_ASK = "MapChooseOfflineAsk";

    public static final String ARC_CREDENTIALS = "ArcCredentials";

    public static final String MEDIA_COPY_TO_PROJECT = "CopyToProject";
    //endregion

    //region Default Values
    public static final boolean DEFAULT_DROP_ZERO = true;
    public static final boolean DEFAULT_ROUND_POINTS = true;

    public static final boolean DEFAULT_EXTERNAL_SYNC_ENABLED = false;
    public static final boolean DEFAULT_EXTERNAL_SYNC_ENABLED_ASK = false;
    public static final String DEFAULT_EXTERNAL_SYNC_DIR = StringEx.Empty;

    public static final boolean DEFAULT_GPS_PARSE_METHOD = true;
    public static final String DEFAULT_GPS_PARSE_DELIMITER = "$RD1";

    public static final DopType DEFAULT_GPS_DOP_TYPE = DopType.HDOP;
    public static final GnssFixQuality DEFAULT_GPS_FIX_QUALITY = GnssFixQuality.GPS;
    public static final GnssFix DEFAULT_GPS_FIX = GnssFix._3D;
    public static final int DEFAULT_GPS_DOP_VALUE = 20;
    public static final boolean DEFAULT_GPS_FIX_USE = true;

    public static final DopType DEFAULT_TAKE5_DOP_TYPE = DopType.HDOP;
    public static final GnssFixQuality DEFAULT_TAKE5_FIX_QUALITY = GnssFixQuality.GPS;
    public static final GnssFix DEFAULT_TAKE5_FIX = GnssFix._3D;
    public static final int DEFAULT_TAKE5_DOP_VALUE = 20;
    public static final int DEFAULT_TAKE5_INCREMENT = 5;
    public static final int DEFAULT_TAKE5_NMEA_AMOUNT = 5;
    public static final boolean DEFAULT_TAKE5_IGNORE = false;
    public static final int DEFAULT_TAKE5_IGNORE_AMOUNT = 2;
    public static final int DEFAULT_TAKE5_FAIL_AMOUNT = 10;
    public static final boolean DEFAULT_TAKE5_VIB_ON_CREATE = true;
    public static final boolean DEFAULT_TAKE5_RING_ON_CREATE = true;

    public static final DopType DEFAULT_SAT_DOP_TYPE = DopType.HDOP;
    public static final GnssFixQuality DEFAULT_SAT_FIX_QUALITY = GnssFixQuality.GPS;
    public static final GnssFix DEFAULT_SAT_FIX = GnssFix._3D;
    public static final int DEFAULT_SAT_DOP_VALUE = 20;
    public static final int DEFAULT_SAT_INCREMENT = 1;
    public static final int DEFAULT_SAT_NMEA_AMOUNT = 60;
    public static final boolean DEFAULT_SAT_IGNORE = false;
    public static final int DEFAULT_SAT_IGNORE_AMOUNT = 2;
    public static final int DEFAULT_SAT_FAIL_AMOUNT = 10;
    public static final boolean DEFAULT_SAT_VIB_ON_CREATE = true;
    public static final boolean DEFAULT_SAT_RING_ON_CREATE = true;
    
    public static final DopType DEFAULT_WALK_DOP_TYPE = DopType.HDOP;
    public static final GnssFixQuality DEFAULT_WALK_FIX_QUALITY = GnssFixQuality.GPS;
    public static final GnssFix DEFAULT_WALK_FIX = GnssFix._3D;
    public static final int DEFAULT_WALK_DOP_VALUE = 20;
    public static final int DEFAULT_WALK_INCREMENT = 2;
    public static final int DEFAULT_WALK_ACCURACY = 0;
    public static final int DEFAULT_WALK_FREQUENCY = 10;
    public static final boolean DEFAULT_WALK_VIB_ON_CREATE = true;
    public static final boolean DEFAULT_WALK_RING_ON_CREATE = true;
    public static final boolean DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP = true;

    public static final boolean DEFAULT_AUTO_OPEN_LAST_PROJECT = true;
    public static final String DEFAULT_LAST_OPENED_PROJECT_NAME = null;
    public static final String DEFAULT_LAST_OPENED_PROJECT_FILE_TTX = null;
    public static final String DEFAULT_LAST_OPENED_PROJECT_FILE_TTMPX = null;

    public static final boolean DEFAULT_GPS_LOG_BURST_DETAILS = false;


    public static final int DEFAULT_AUTO_FILL_FROM_RANGE_FINDER = 1;
    public static final boolean DEFAULT_AUTO_FILL_FROM_RANGE_FINDER_ASK = true;

    public static final int DEFAULT_AUTO_SET_GPS_NAME_TO_META = 0;
    public static final boolean DEFAULT_AUTO_SET_GPS_NAME_TO_META_ASK = true;
    public static final int DEFAULT_AUTO_UPDATE_WALK_ONBND = 0;
    public static final boolean DEFAULT_AUTO_UPDATE_WALK_ONBND_ASK = true;
    public static final int DEFAULT_AUTO_OVERWRITE_PLOTGRID = 0;
    public static final boolean DEFAULT_AUTO_OVERWRITE_PLOTGRID_ASK = true;
    public static final int DEFAULT_AUTO_OVERWRITE_EXPORT = 0;
    public static final boolean DEFAULT_AUTO_OVERWRITE_EXPORT_ASK = true;
    public static final int DEFAULT_AUTO_INTERNALIZE_EXPORT = 0;
    public static final boolean DEFAULT_AUTO_INTERNALIZE_EXPORT_ASK = true;

    public static final MapTracking DEFAULT_MAP_TRACKING_OPTION = MapTracking.POLY_BOUNDS;
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
    public static final int DEFAULT_MAP_ADJ_LINE_WIDTH = 6;
    public static final int DEFAULT_MAP_UNADJ_LINE_WIDTH = 16;
    public static final int DEFAULT_MAP_DIST_TO_POLY_LINE_WIDTH = 10;
    public static final @ColorInt int DEFAULT_MAP_DIST_TO_POLY_LINE_COLOR = 0xFF000000;
    public static final float DEFAULT_MAP_DIST_TO_POLY_LINE_TOLERANCE = 10; //in meters
    public static final int DEFAULT_MAP_CHOOSE_OFFLINE = 0;
    public static final boolean DEFAULT_MAP_CHOOSE_OFFLINE_ASK = true;

    public static final int DEFAULT_EXPORT_MODE = 2;
    public static final boolean DEFAULT_EXPORT_MODE_ASK = true;

    public static final boolean DEFAULT_MEDIA_COPY_TO_PROJECT = true;
    //endregion


    public DeviceSettings(TwoTrailsApp context) {
        super(context);

        if (!getBool(SETTINGS_CREATED, false)) {
            reset();
        }
    }

    public void reset() {
        setBool(DEVELOPER_OPTIONS, false);
        setBool(DEBUG_MODE, false);

        setBool(EXTERNAL_SYNC_ENABLED, DEFAULT_EXTERNAL_SYNC_ENABLED);
        setBool(EXTERNAL_SYNC_ENABLED_ASK, DEFAULT_EXTERNAL_SYNC_ENABLED_ASK);
        setString(EXTERNAL_SYNC_DIR, DEFAULT_EXTERNAL_SYNC_DIR);

        setBool(GPS_PARSE_METHOD, DEFAULT_GPS_PARSE_METHOD);
        setString(GPS_PARSE_DELIMITER, DEFAULT_GPS_PARSE_DELIMITER);

        setString(LAST_CRASH_TIME, StringEx.Empty);

        setBool(DROP_ZERO, DEFAULT_DROP_ZERO);
        setBool(ROUND_POINTS, DEFAULT_ROUND_POINTS);

        setBool(GPS_ALWAYS_ON, true);
        setBool(VN100_ALWAYS_ON, true);
        setBool(KEEP_SCREEN_ON, false);

        setInt(GPS_FILTER_DOP_TYPE, DEFAULT_GPS_DOP_TYPE.getValue());
        setInt(GPS_FILTER_DOP_VALUE, DEFAULT_GPS_DOP_VALUE);
        setInt(GPS_FILTER_FIX_QUALITY, DEFAULT_GPS_FIX_QUALITY.getValue());
        setInt(GPS_FILTER_FIX, DEFAULT_GPS_FIX.getValue());
        setBool(GPS_FILTER_FIX_USE, DEFAULT_GPS_FIX_USE);

        setInt(AUTO_FILL_FROM_RANGE_FINDER, DEFAULT_AUTO_FILL_FROM_RANGE_FINDER);
        setBool(AUTO_FILL_FROM_RANGE_FINDER_ASK, DEFAULT_AUTO_FILL_FROM_RANGE_FINDER_ASK);

        setInt(TAKE5_FILTER_DOP_TYPE, DEFAULT_TAKE5_DOP_TYPE.getValue());
        setInt(TAKE5_FILTER_DOP_VALUE, DEFAULT_TAKE5_DOP_VALUE);
        setInt(TAKE5_FILTER_FIX_QUALITY, DEFAULT_TAKE5_FIX_QUALITY.getValue());
        setInt(TAKE5_FILTER_FIX, DEFAULT_TAKE5_FIX.getValue());
        setInt(TAKE5_NMEA_AMOUNT, DEFAULT_TAKE5_NMEA_AMOUNT);
        setBool(TAKE5_IGNORE_FIRST_NMEA, DEFAULT_TAKE5_IGNORE);
        setInt(TAKE5_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_TAKE5_IGNORE_AMOUNT);
        setInt(TAKE5_FAIL_AMOUNT, DEFAULT_TAKE5_FAIL_AMOUNT);
        setInt(TAKE5_INCREMENT, DEFAULT_TAKE5_INCREMENT);
        setBool(TAKE5_RING_ON_CREATE, DEFAULT_TAKE5_RING_ON_CREATE);
        setBool(TAKE5_VIBRATE_ON_CREATE, DEFAULT_TAKE5_VIB_ON_CREATE);

        setInt(SAT_FILTER_DOP_TYPE, DEFAULT_SAT_DOP_TYPE.getValue());
        setInt(SAT_FILTER_DOP_VALUE, DEFAULT_SAT_DOP_VALUE);
        setInt(SAT_FILTER_FIX_QUALITY, DEFAULT_SAT_FIX_QUALITY.getValue());
        setInt(SAT_FILTER_FIX, DEFAULT_SAT_FIX.getValue());
        setInt(SAT_NMEA_AMOUNT, DEFAULT_SAT_NMEA_AMOUNT);
        setBool(SAT_IGNORE_FIRST_NMEA, DEFAULT_SAT_IGNORE);
        setInt(SAT_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_SAT_IGNORE_AMOUNT);
        setInt(SAT_FAIL_AMOUNT, DEFAULT_SAT_FAIL_AMOUNT);
        setInt(SAT_INCREMENT, DEFAULT_SAT_INCREMENT);
        setBool(SAT_RING_ON_CREATE, DEFAULT_SAT_RING_ON_CREATE);
        setBool(SAT_VIBRATE_ON_CREATE, DEFAULT_SAT_VIB_ON_CREATE);

        setInt(WALK_FILTER_DOP_TYPE, DEFAULT_WALK_DOP_TYPE.getValue());
        setInt(WALK_FILTER_DOP_VALUE, DEFAULT_WALK_DOP_VALUE);
        setInt(WALK_FILTER_FIX_QUALITY, DEFAULT_WALK_FIX_QUALITY.getValue());
        setInt(WALK_FILTER_FIX, DEFAULT_WALK_FIX.getValue());
        setInt(WALK_FILTER_ACCURACY, DEFAULT_WALK_ACCURACY);
        setInt(WALK_FILTER_FREQUENCY, DEFAULT_WALK_FREQUENCY);
        setInt(WALK_INCREMENT, DEFAULT_WALK_INCREMENT);
        setBool(WALK_SHOW_ALL_POINTS_ON_MAP, DEFAULT_WALK_SHOW_ALL_POINTS_ON_MAP);
        setBool(WALK_RING_ON_CREATE, DEFAULT_WALK_RING_ON_CREATE);
        setBool(WALK_VIBRATE_ON_CREATE, DEFAULT_WALK_VIB_ON_CREATE);

        setInt(AUTO_SET_GPS_NAME_TO_META, DEFAULT_AUTO_SET_GPS_NAME_TO_META);
        setBool(AUTO_SET_GPS_NAME_TO_META_ASK, DEFAULT_AUTO_SET_GPS_NAME_TO_META_ASK);
        setInt(AUTO_UPDATE_WALK_ONBND, DEFAULT_AUTO_UPDATE_WALK_ONBND);
        setBool(AUTO_UPDATE_WALK_ONBND_ASK, DEFAULT_AUTO_UPDATE_WALK_ONBND_ASK);
        setInt(AUTO_OVERWRITE_PLOTGRID, DEFAULT_AUTO_OVERWRITE_PLOTGRID);
        setBool(AUTO_OVERWRITE_PLOTGRID_ASK, DEFAULT_AUTO_OVERWRITE_PLOTGRID_ASK);
        setInt(AUTO_OVERWRITE_EXPORT, DEFAULT_AUTO_OVERWRITE_EXPORT);
        setBool(AUTO_OVERWRITE_EXPORT_ASK, DEFAULT_AUTO_OVERWRITE_EXPORT_ASK);
        setInt(AUTO_INTERNALIZE_EXPORT, DEFAULT_AUTO_INTERNALIZE_EXPORT);
        setBool(AUTO_INTERNALIZE_EXPORT_ASK, DEFAULT_AUTO_INTERNALIZE_EXPORT_ASK);

        setInt(USE_TTCAMERA, 2);
        setBool(USE_TTCAMERA_ASK, true);


        setBool(AUTO_OPEN_LAST_PROJECT, DEFAULT_AUTO_OPEN_LAST_PROJECT);
        setString(LAST_OPENED_PROJECT_NAME, DEFAULT_LAST_OPENED_PROJECT_NAME);
        setString(LAST_OPENED_PROJECT_FILE_TTX, DEFAULT_LAST_OPENED_PROJECT_FILE_TTX);
        setString(LAST_OPENED_PROJECT_FILE_TTMPX, DEFAULT_LAST_OPENED_PROJECT_FILE_TTX);

        setInt(MAP_TRACKING_OPTION, DEFAULT_MAP_TRACKING_OPTION.getValue());
        setBool(MAP_MY_POS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
        setBool(MAP_COMPASS_ENABLED, DEFAULT_MAP_COMPASS_ENABLED);
        setBool(MAP_SHOW_MY_POS, DEFAULT_MAP_SHOW_MY_POS);
        setBool(MAP_DISPLAY_GPS_LOCATION, DEFAULT_MAP_DISPLAY_GPS_LOCATION);
        setBool(MAP_USE_UTM_NAV, DEFAULT_MAP_USE_UTM_NAV);
        setInt(MAP_TYPE, DEFAULT_MAP_TYPE);
        setInt(MAP_ID, DEFAULT_MAP_ID);
        setString(ARC_GIS_MAPS, DEFAULT_ARC_GIS_MAPS);
        setInt(ARC_GIS_MAP_ID_COUNTER, DEFAULT_ARC_GIS_MAP_ID_COUNTER);
        setInt(MAP_DIST_TO_POLY_LINE_WIDTH, DEFAULT_MAP_DIST_TO_POLY_LINE_WIDTH);
        setInt(MAP_DIST_TO_POLY_LINE_COLOR, DEFAULT_MAP_DIST_TO_POLY_LINE_COLOR);
        setDouble(MAP_DIST_TO_POLY_LINE_TOLERANCE, DEFAULT_MAP_DIST_TO_POLY_LINE_TOLERANCE);
        setInt(MAP_ADJ_LINE_WIDTH, DEFAULT_MAP_ADJ_LINE_WIDTH);
        setInt(MAP_UNADJ_LINE_WIDTH, DEFAULT_MAP_UNADJ_LINE_WIDTH);
        setInt(MAP_CHOOSE_OFFLINE, DEFAULT_MAP_CHOOSE_OFFLINE);
        setBool(MAP_CHOOSE_OFFLINE_ASK, DEFAULT_MAP_CHOOSE_OFFLINE_ASK);

        setBool(MEDIA_COPY_TO_PROJECT, DEFAULT_MEDIA_COPY_TO_PROJECT);

        setInt(EXPORT_MODE, DEFAULT_EXPORT_MODE);
        setBool(EXPORT_MODE_ASK, DEFAULT_EXPORT_MODE_ASK);

        setString(ARC_CREDENTIALS, StringEx.Empty);

        setBool(SETTINGS_CREATED, true);
    }


    public void writeToFile(JsonWriter js) throws IOException {
        js.name(DEVELOPER_OPTIONS).value(isDeveloperOptionsEnabled());
        DateTime lastCrash = getLastCrashTime();
        js.name(LAST_CRASH_TIME).value(lastCrash != null ? lastCrash.toString() : "");

        js.name(EXTERNAL_SYNC_ENABLED).value(isExternalSyncEnabled());
        js.name(EXTERNAL_SYNC_ENABLED_ASK).value(isExternalSyncEnabledAsked());
        js.name(EXTERNAL_SYNC_DIR).value(getExternalSyncDir());

        js.name(DROP_ZERO).value(getDropZeros());
        js.name(ROUND_POINTS).value(getRoundPoints());
        js.name(KEEP_SCREEN_ON).value(getKeepScreenOn());

        js.name(GPS_EXTERNAL).value(getGpsExternal());
        js.name(GPS_ALWAYS_ON).value(isGpsAlwaysOn());
        js.name(LOG_ALL_GPS).value(getLogAllGPS());
        js.name(GPS_CONFIGURED).value(isGpsConfigured());
        js.name(GPS_LOG_BURST_DETAILS).value(getGpsLogBurstDetails());

        js.name(RANGE_FINDER_ALWAYS_ON).value(isRangeFinderAlwaysOn());
        //js.name(LOG_ALL_RANGE_FINDER).value();
        js.name(RANGE_FINDER_CONFIGURED).value(isRangeFinderConfigured());
        //js.name(RANGE_FINDER_LOG_BURST_DETAILS).value();

        js.name(GPS_DEVICE_ID).value(getGpsDeviceID());
        js.name(GPS_DEVICE_NAME).value(getGpsDeviceName());
        js.name(RANGE_FINDER_DEVICE_ID).value(getRangeFinderDeviceID());
        js.name(RANGE_FINDER_DEVICE_NAME).value(getRangeFinderDeviceName());

        js.name(AUTO_FILL_FROM_RANGE_FINDER).value(getAutoFillFromRangeFinder());
        js.name(AUTO_FILL_FROM_RANGE_FINDER_ASK).value(isAutoFillFromRangeFinderAsk());

        js.name(GPS_FILTER_DOP_TYPE).value(getGpsFilterDopType().toString());
        js.name(GPS_FILTER_DOP_VALUE).value(getGpsFilterDopValue());
        js.name(GPS_FILTER_FIX_QUALITY).value(getGpsFilterFixType().toString());
        js.name(GPS_FILTER_FIX).value(getGpsFilterFix().toString());
        js.name(GPS_FILTER_FIX_USE).value(getGpsFilterFixUse());

        js.name(TAKE5_FILTER_DOP_TYPE).value(getTake5FilterDopType().toString());
        js.name(TAKE5_FILTER_DOP_VALUE).value(getTake5FilterDopValue());
        js.name(TAKE5_FILTER_FIX_QUALITY).value(getTake5FilterFixQuality().toString());
        js.name(TAKE5_FILTER_FIX).value(getTake5FilterFix().toString());
        js.name(TAKE5_NMEA_AMOUNT).value(getTake5IngoreFirstNmeaAmount());
        js.name(TAKE5_IGNORE_FIRST_NMEA).value(getTake5IngoreFirstNmea());
        js.name(TAKE5_IGNORE_FIRST_NMEA_AMOUNT).value(getTake5IngoreFirstNmeaAmount());
        js.name(TAKE5_FAIL_AMOUNT).value(getTake5FailAmount());
        js.name(TAKE5_INCREMENT).value(getTake5Increment());
        js.name(TAKE5_VIBRATE_ON_CREATE).value(getTake5VibrateOnCreate());
        js.name(TAKE5_RING_ON_CREATE).value(getTake5RingOnCreate());

        js.name(WALK_FILTER_DOP_TYPE).value(getWalkFilterDopType().toString());
        js.name(WALK_FILTER_DOP_VALUE).value(getWalkFilterDopValue());
        js.name(WALK_FILTER_FIX_QUALITY).value(getWalkFilterFixQuality().toString());
        js.name(WALK_FILTER_FIX).value(getWalkFilterFix().toString());
        js.name(WALK_FILTER_ACCURACY).value(getWalkFilterAccuracy());
        js.name(WALK_FILTER_FREQUENCY).value(getWalkFilterFrequency());
        js.name(WALK_INCREMENT).value(getWalkIncrement());
        js.name(WALK_VIBRATE_ON_CREATE).value(getWalkVibrateOnCreate());
        js.name(WALK_RING_ON_CREATE).value(getWalkRingOnCreate());
        js.name(WALK_SHOW_ALL_POINTS_ON_MAP).value(getWalkShowAllPointsOnMap());

        js.name(AUTO_UPDATE_INDEX).value(getAutoUpdateIndex());
        js.name(AUTO_SET_GPS_NAME_TO_META).value(getAutoSetGpsNameToMeta());
        js.name(AUTO_SET_GPS_NAME_TO_META_ASK).value(getAutoSetGpsNameToMetaAsk());
        js.name(AUTO_UPDATE_WALK_ONBND).value(getAutoUpdateWalkOnBnd());
        js.name(AUTO_UPDATE_WALK_ONBND_ASK).value(getAutoUpdateWalkOnBndAsk());
        js.name(AUTO_OVERWRITE_PLOTGRID).value(getAutoOverwritePlotGrid());
        js.name(AUTO_OVERWRITE_PLOTGRID_ASK).value(getAutoOverwritePlotGridAsk());
        js.name(AUTO_OVERWRITE_EXPORT).value(getAutoOverwriteExport());
        js.name(AUTO_OVERWRITE_EXPORT_ASK).value(getAutoOverwriteExportAsk());
        js.name(AUTO_INTERNALIZE_EXPORT).value(getAutoInternalizeExport());
        js.name(AUTO_INTERNALIZE_EXPORT_ASK).value(getAutoInternalizeExportAsk());

        js.name(USE_TTCAMERA).value(getUseTtCamera());
        js.name(USE_TTCAMERA_ASK).value(getUseTtCameraAsk());

        js.name(AUTO_OPEN_LAST_PROJECT).value(getAutoOpenLastProject());

        TwoTrailsProject rp =getLastOpenedProject();
        js.name(LAST_OPENED_PROJECT_NAME).value(rp.Name);
        js.name(LAST_OPENED_PROJECT_FILE_TTX).value(rp.TTXFile);

        js.name(MAP_TRACKING_OPTION).value(getMapTrackingOption().toString());
        js.name(MAP_COMPASS_ENABLED).value(getMapCompassEnabled());
        js.name(MAP_MY_POS_BUTTON).value(getMapMyPosBtns());
        js.name(MAP_MIN_DIST).value(getMapMinDist());
        js.name(MAP_SHOW_MY_POS).value(getMapShowMyPos());
        js.name(MAP_DISPLAY_GPS_LOCATION).value(getMapDisplayGpsLocation());
        js.name(MAP_USE_UTM_NAV).value(getMapUseUtmNav());
        js.name(MAP_TYPE).value(getMapType().toString());
        js.name(MAP_ID).value(getMapId());
        js.name(MAP_DIST_TO_POLY_LINE_WIDTH).value(getMapDistToPolyLineWidth());
        js.name(MAP_ADJ_LINE_WIDTH).value(getMapAdjLineWidth());
        js.name(MAP_UNADJ_LINE_WIDTH).value(getMapUnAdjLineWidth());
        js.name(MAP_CHOOSE_OFFLINE).value(getAutoMapChooseOffline());
        js.name(MAP_CHOOSE_OFFLINE_ASK).value(getAutoMapChooseOfflineAsk());

        js.name(ARC_GIS_MAPS);
        js.beginArray();
        for (ArcGisMapLayer layer : getArcGisMayLayers()) {
            js.beginObject();
            js.name("ID").value(layer.getId());
            js.name("Name").value(layer.getName());
            js.name("Description").value(layer.getDescription());
            js.name("Location").value(layer.getLocation());
            js.name("URL").value(layer.getUrl());
            js.name("FilePath").value(layer.getFileName());
            js.name("HasValidFile").value(layer.hasValidFile());
            js.name("Online").value(layer.isOnline());
            js.name("MinScale").value(layer.getMinScale());
            js.name("MaxScale").value(layer.getMaxScale());

            js.name("DetailLevels");
            js.beginArray();
            if (layer.hasDetailLevels()) {
                for (ArcGisMapLayer.DetailLevel dl : layer.getLevelsOfDetail()) {
                    js.name("Resolution").value(dl.getResolution());
                    js.name("Level").value(dl.getLevel());
                    js.name("Scale").value(dl.getScale());
                    js.name("DescribeContents").value(dl.describeContents());
                }
            }
            js.endArray();

            js.endObject();
        }
        js.endArray();

        js.name(ARC_GIS_MAP_ID_COUNTER).value(getArcGisIdCounter());

        js.name(ARC_CREDENTIALS).value(getArcCredentials());

        js.name(MEDIA_COPY_TO_PROJECT).value(getMediaCopyToProject());
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


    public boolean isGpsParsingByTime() {
        return getBool(GPS_PARSE_METHOD, DEFAULT_GPS_PARSE_METHOD);
    }

    public void setGpsParsingByTime(boolean parseByTime) {
        setBool(GPS_PARSE_METHOD, parseByTime);
    }


    public String getGpsParseDelimiter() {
        return getString(GPS_PARSE_DELIMITER, DEFAULT_GPS_PARSE_DELIMITER);
    }

    public void setGpsParseDelimiter(String delimiter) {
        setString(GPS_PARSE_DELIMITER, delimiter);
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
        return getBool(AUTO_FILL_FROM_RANGE_FINDER_ASK, DEFAULT_AUTO_FILL_FROM_RANGE_FINDER_ASK);
    }
    //endregion

    //region VN100 Settings
    public String getVN100DeviceID() {
        return getString(VN100_DEVICE_ID);
    }

    public void setVN100DeviceId(String value) {
        setString(VN100_DEVICE_ID, value);
    }


    public String getVN100DeviceName() {
        return getString(VN100_DEVICE_NAME);
    }

    public void setVN100DeviceName(String value) {
        setString(VN100_DEVICE_NAME, value);
    }

    public boolean isVN100AlwaysOn() {
        return getBool(VN100_ALWAYS_ON);
    }

    public void setVN100AlwaysOn(boolean value) {
        setBool(VN100_ALWAYS_ON, value);
    }

    public boolean isVN100Configured() {
        return getBool(VN100_CONFIGURED);
    }

    public void setVN100Configured(boolean value) {
        setBool(VN100_CONFIGURED, value);
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

    public GnssFixQuality getGpsFilterFixType() {
        return GnssFixQuality.parse(getInt(GPS_FILTER_FIX_QUALITY, DEFAULT_GPS_FIX_QUALITY.getValue()));
    }

    public void setGpsFilterFixQuality(GnssFixQuality value) {
        setInt(GPS_FILTER_FIX_QUALITY, value.getValue());
    }

    public GnssFix getGpsFilterFix() {
        return GnssFix.parse(getInt(GPS_FILTER_FIX, DEFAULT_GPS_FIX.getValue()));
    }

    public void setGpsFilterFix(GnssFix value) {
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

    public GnssFixQuality getTake5FilterFixQuality() {
        return GnssFixQuality.parse(getInt(TAKE5_FILTER_FIX_QUALITY, DEFAULT_TAKE5_FIX_QUALITY.getValue()));
    }

    public void setTake5FilterFixQuality(GnssFixQuality value) {
        setInt(TAKE5_FILTER_FIX_QUALITY, value.getValue());
    }

    public GnssFix getTake5FilterFix() {
        return GnssFix.parse(getInt(TAKE5_FILTER_FIX, DEFAULT_TAKE5_FIX.getValue()));
    }

    public void setTake5FilterFix(GnssFix value) {
        setInt(TAKE5_FILTER_FIX, value.getValue());
    }



    public DopType getSATFilterDopType() {
        return DopType.parse(getInt(SAT_FILTER_DOP_TYPE, DEFAULT_SAT_DOP_TYPE.getValue()));
    }

    public void setSATFilterDopType(DopType value) {
        setInt(SAT_FILTER_DOP_TYPE, value.getValue());
    }

    public int getSATFilterDopValue() {
        return getInt(SAT_FILTER_DOP_VALUE, DEFAULT_SAT_DOP_VALUE);
    }

    public void setSATFilterDopValue(int value) {
        setInt(SAT_FILTER_DOP_VALUE, value);
    }

    public GnssFixQuality getSATFilterFixQuality() {
        return GnssFixQuality.parse(getInt(SAT_FILTER_FIX_QUALITY, DEFAULT_SAT_FIX_QUALITY.getValue()));
    }

    public void setSATFilterFixQuality(GnssFixQuality value) {
        setInt(SAT_FILTER_FIX_QUALITY, value.getValue());
    }

    public GnssFix getSATFilterFix() {
        return GnssFix.parse(getInt(SAT_FILTER_FIX, DEFAULT_SAT_FIX.getValue()));
    }

    public void setSATFilterFix(GnssFix value) {
        setInt(SAT_FILTER_FIX, value.getValue());
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

    public GnssFixQuality getWalkFilterFixQuality() {
        return GnssFixQuality.parse(getInt(WALK_FILTER_FIX_QUALITY, DEFAULT_WALK_FIX_QUALITY.getValue()));
    }

    public void setWalkFilterFixType(GnssFixQuality value) {
        setInt(WALK_FILTER_FIX_QUALITY, value.getValue());
    }

    public GnssFix getWalkFilterFix() {
        return GnssFix.parse(getInt(WALK_FILTER_FIX, DEFAULT_WALK_FIX.getValue()));
    }

    public void setWalkFilterFix(GnssFixQuality value) {
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

    //region SAT
    public int getSATNmeaAmount() {
        return getInt(SAT_NMEA_AMOUNT, DEFAULT_SAT_NMEA_AMOUNT);
    }

    public void setSATNmeaAmount(int value) {
        setInt(SAT_NMEA_AMOUNT, value);
    }

    public boolean getSATIngoreFirstNmea() {
        return getBool(SAT_IGNORE_FIRST_NMEA);
    }

    public void setSATIgnoreFirstNmea(boolean value) {
        setBool(SAT_IGNORE_FIRST_NMEA, value);
    }

    public int getSATIngoreFirstNmeaAmount() {
        return getInt(SAT_IGNORE_FIRST_NMEA_AMOUNT, DEFAULT_SAT_IGNORE_AMOUNT);
    }

    public void setSATIgnoreFirstNmeaAmount(int value) {
        setInt(SAT_IGNORE_FIRST_NMEA_AMOUNT, value);
    }

    public int getSATFailAmount() {
        return getInt(SAT_FAIL_AMOUNT, DEFAULT_SAT_FAIL_AMOUNT);
    }

    public void setSATFailAmount(int value) {
        setInt(SAT_FAIL_AMOUNT, value);
    }

    public int getSATIncrement() {
        return getInt(SAT_INCREMENT, DEFAULT_SAT_INCREMENT);
    }

    public void setSATIncrement(int value) {
        setInt(SAT_INCREMENT, value);
    }


    public boolean getSATVibrateOnCreate() {
        return getBool(SAT_VIBRATE_ON_CREATE, DEFAULT_SAT_VIB_ON_CREATE);
    }

    public void setSATVibrateOnCreate(boolean value) {
        setBool(SAT_VIBRATE_ON_CREATE, value);
    }


    public boolean getSATRingOnCreate() {
        return getBool(SAT_RING_ON_CREATE, DEFAULT_SAT_RING_ON_CREATE);
    }

    public void setSATRingOnCreate(boolean value) {
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
        return getBool(MAP_MY_POS_BUTTON, DEFAULT_MAP_MYPOS_BUTTON);
    }

    public void setMapMyposButton(boolean value) {
        setBool(MAP_MY_POS_BUTTON, value);
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


    public int getMapDistToPolyLineWidth() {
        return getInt(MAP_DIST_TO_POLY_LINE_WIDTH, DEFAULT_MAP_DIST_TO_POLY_LINE_WIDTH);
    }

    public void setMapDistToPolyLineWidth(int value) {
        setInt(MAP_DIST_TO_POLY_LINE_WIDTH, value);
    }

    public int getMapDistToPolyLineColor() {
        return getInt(MAP_DIST_TO_POLY_LINE_COLOR, DEFAULT_MAP_DIST_TO_POLY_LINE_COLOR);
    }

    public void setMapDistToPolyLineColor(int value) {
        setInt(MAP_DIST_TO_POLY_LINE_COLOR, value);
    }

    public double getMapDistToPolyLineTolerance() {
        return getDouble(MAP_DIST_TO_POLY_LINE_TOLERANCE, DEFAULT_MAP_DIST_TO_POLY_LINE_TOLERANCE);
    }

    public void setMapDistToPolyTolerance(double value) {
        setDouble(MAP_DIST_TO_POLY_LINE_TOLERANCE, value);
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


    public int getAutoMapChooseOffline() {
        return getInt(MAP_CHOOSE_OFFLINE, DEFAULT_MAP_CHOOSE_OFFLINE);
    }

    public void setAutoMapChooseOffline(int value) {
        setInt(MAP_CHOOSE_OFFLINE, value);
    }

    public boolean getAutoMapChooseOfflineAsk() {
        return getBool(MAP_CHOOSE_OFFLINE_ASK, DEFAULT_MAP_CHOOSE_OFFLINE_ASK);
    }

    public void setAutoMapChooseOfflineAsk(boolean value) {
        setBool(MAP_CHOOSE_OFFLINE_ASK, value);
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

    public void setArcGisMayLayers(Collection<ArcGisMapLayer> recentProjects) {
        setString(ARC_GIS_MAPS, new Gson().toJson(recentProjects));
    }
    //endregion

    //endregion

    public int getExportMode() {
        return getInt(EXPORT_MODE, DEFAULT_EXPORT_MODE);
    }

    public void setExportMode(int value) {
        setInt(EXPORT_MODE, value);
    }

    public boolean getExportModeAsk() {
        return getBool(EXPORT_MODE_ASK, DEFAULT_EXPORT_MODE_ASK);
    }

    public void setExportModeAsk(boolean value) {
        setBool(EXPORT_MODE_ASK, value);
    }

    //region Export


    //endregion

    //region Other
    public boolean isDeveloperOptionsEnabled() {
        return getBool(DEVELOPER_OPTIONS, false);
    }

    public void enabledDevelopterOptions(boolean value) {
        setBool(DEVELOPER_OPTIONS, value);

        if (value) {
            setBool(DEBUG_MODE, true);
        }
    }

    public boolean isDebugMode() {
        return getBool(DEBUG_MODE, false);
    }

    public void enabledDebugMode(boolean value) {
        setBool(DEBUG_MODE, value);
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
        return getBool(AUTO_OVERWRITE_EXPORT_ASK, DEFAULT_AUTO_OVERWRITE_EXPORT_ASK);
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
        return getBool(AUTO_INTERNALIZE_EXPORT_ASK, DEFAULT_AUTO_INTERNALIZE_EXPORT_ASK);
    }

    public void setAutoInternalizeExportAsk(boolean value) {
        setBool(AUTO_INTERNALIZE_EXPORT_ASK, value);
    }



    public TwoTrailsProject getLastOpenedProject() {
        return new TwoTrailsProject(
                getString(LAST_OPENED_PROJECT_NAME),
                getString(LAST_OPENED_PROJECT_FILE_TTX, null),
                getString(LAST_OPENED_PROJECT_FILE_TTMPX, null)
        );
    }

    public void setLastOpenedProject(TwoTrailsProject twoTrailsProject) {
        setString(LAST_OPENED_PROJECT_NAME, twoTrailsProject.Name);
        setString(LAST_OPENED_PROJECT_FILE_TTX, twoTrailsProject.TTXFile);
        setString(LAST_OPENED_PROJECT_FILE_TTX, twoTrailsProject.TTMPXFile);
    }


    public boolean getAutoOpenLastProject() {
        return getBool(AUTO_OPEN_LAST_PROJECT, DEFAULT_AUTO_OPEN_LAST_PROJECT);
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



    public boolean isExternalSyncEnabled() {
        return getBool(EXTERNAL_SYNC_ENABLED, DEFAULT_EXTERNAL_SYNC_ENABLED);
    }

    public void setExternalSyncEnabled(boolean value) {
        setBool(EXTERNAL_SYNC_ENABLED, value);
    }

    public boolean isExternalSyncEnabledAsked() {
        return getBool(EXTERNAL_SYNC_ENABLED_ASK, DEFAULT_EXTERNAL_SYNC_ENABLED_ASK);
    }

    public void setExternalSyncEnabledAsk(boolean value) {
        setBool(EXTERNAL_SYNC_ENABLED_ASK, value);
    }

    public String getExternalSyncDir() {
        return getString(EXTERNAL_SYNC_DIR, DEFAULT_EXTERNAL_SYNC_DIR);
    }

    public void setExternalSyncDir(String value) {
        setString(EXTERNAL_SYNC_DIR, value);
    }
    //endregion

}
