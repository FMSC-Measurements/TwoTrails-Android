package com.usda.fmsc.twotrails;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.TtGroup;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


@SuppressWarnings("unused")
public class Consts {
    public static final DateTimeFormatter DateTimeFormatter = DateTimeFormat.forPattern("M/d/yyyy h:mm:ss a");

    public static final String LOG_TAG = "TT";

    public static final String FILE_EXTENSION = ".ttx";
    public static final String MEDIA_PACKAGE_EXTENSION = ".ttmpx";

    public static final String EmptyGuid = "00000000-0000-0000-0000-000000000000";

    public static final double RMSEr95_Coeff = 1.7308;

    public static final double Default_Point_Accuracy = 6.01;
    public static final double Minimum_Point_Accuracy = 0.000001;
    public static final int Minimum_Point_Display_Digits = 5;


    public static final float ENABLED_ALPHA = 1.0f;
    public static final float DISABLED_ALPHA = 0.5f;

    public static final String NewLine = "\r\n";


    public static final String ServiceTitle = "TwoTrails GPS";
    public static final String ServiceContent = "GPS Running";
    public static final String ServiceAcquiringPoint = "Acquiring Point";
    public static final String ServiceWalking = "Walking";


    public static class DeviceNames {
        public static final String[] GPS_RECEIVERS = new String[]{
                "EOS Arrow 100",
                "EOS Arrow 200",
                "EOS Arrow Lite",
                "EOS Arrow Gold",
                "Juniper Geode",
                "QStarz 1000XT",
                "QStarz 1000ST",
                "Trimble R1",
                "Other"
        };

        public static final String[] RANGE_FINDERS = new String[] {
                "TruPulse 360",
                "TruPulse 200",
                "TruPulse Other",
                "Impulse 200",
                "Impulse 100",
                "Impulse Other",
                "Logger Tape",
                "Nylon Tape",
                "Chains",
                "Other"
        };
    }

    public static class Notifications {
        public static final long[] VIB_PATTERN_GPS_LOST_CONNECTED = new long[] { 0, 300, 50, 300, 50, 500 };
        public static final long[] VIB_PATTERN_GPS_CONNECTED = new long[] { 0, 250, 50, 250 };
        public static final long[] VIB_POINT_CREATED = new long[] { 0, 150, 25, 150 };
        public static final long[] VIB_ERROR = new long[] { 0, 1000 };
    }

    public static class FileExtensions {
        public static final String TWO_TRAILS = "file/*" + FILE_EXTENSION;
        public static final String TPK = "file/*.tpk";
    }

    public static class Codes {
        public static class Activites {
            public static final int MAIN = 1000;
            public static final int ACQUIRE = 1001;
            public static final int CALCULATE = 1002;
            public static final int LOGGER = 1003;
            public static final int HAID = 1004;
            public static final int MAP = 1005;
            public static final int METADATA = 1006;
            public static final int POINTS = 1007;
            public static final int POLYGONS = 1008;
            public static final int PROJECT = 1009;
            public static final int SETTINGS = 1010;
            public static final int TABLE_EDIT = 1011;
            public static final int TAKE5 = 1012;
            public static final int WALK = 1013;
            public static final int EXPORT = 1014;
            public static final int MULTI_POINT = 1015;
            public static final int PLOT_GRID = 1016;
            public static final int GET_MAP_EXTENTS = 1017;
            public static final int MAP_MANAGER = 1018;
            public static final int MAP_DETAILS = 1019;
            public static final int ARC_GIS_LOGIN = 1020;
            public static final int TTCAMERA = 1021;
            public static final int SEND_EMAIL_TO_DEV = 1022;
        }

        public static class Dialogs {
            public static final int REQUEST_FILE = 4001;
            public static final int NEW_ARC_MAP = 4002;
        }

        public static class Results {
            public static final int ERROR = 2000;
            public static final int POINT_CREATED = 2001;
            public static final int POINT_UPDATED = 2002;
            public static final int MEDIA_CREATED = 2003;
            public static final int MEDIA_UPDATED = 2004;
            public static final int NO_POINT_DATA = 2005;
            public static final int NO_POLYGON_DATA = 2006;
            public static final int NO_METDATA_DATA = 2007;
            public static final int MAP_CREATED = 2008;
            public static final int MAP_UPDATED = 2009;
            public static final int MAP_DELETED = 2010;
            public static final int DOWNLOADING_MAP = 2011;
            public static final int IMAGE_CAPTURED = 2012;
            public static final int GPS_NOT_FOUND = 2013;
            public static final int GPS_NOT_CONFIGURED = 2014;
            public static final int ORIENTATION_UPDATED = 2015;
            public static final int NO_DAL = 2016;

        }

        public static class Services {
            public static final int REQUEST_GOOGLE_PLAY_SERVICES = 3000;
            public static final int REQUEST_GPS_SERVICE = 3001;
        }

        public static class Data {
            public static final String POINT_DATA = "PointData";
            public static final String POINT_CN = "PointCN";
            public static final String POLYGON_DATA = "PolygonData";
            public static final String METADATA_DATA = "MetaDataData";
            public static final String ADDITIVE_NMEA_DATA = "AdditiveNmeaData";
            public static final String NUMBER_OF_CREATED_POINTS = "NumOfCreatedPoints";
            public static final String MAP_DATA = "MapData";
            public static final String TTIMAGE = "TtImage";
            public static final String ORIENTATION = "Orientation";
            public static final String CRASH = "AppCrash";
            public static final String POINT_PACKAGE = "PointPackage";
        }

        public static class Requests {
            public static final int LOCATION = 5000;
            public static final int PHONE = 5001;
            public static final int STORAGE = 5002;
            public static final int BLUETOOH = 5003;
            public static final int INTERNET = 5004;
            public static final int VIBRATE = 5005;
            public static final int CAPTURE_IMAGE = 5006;
            public static final int ADD_IMAGES = 5007;
            public static final int OPEN_FILE = 5008;
            public static final int CREATE_FILE = 5009;
            public static final int FOLDER = 5010;
            public static final int CAMERA = 5011;
            public static final int UPDATE_ORIENTATION = 5012;
        }
    }

    public static class Location {
        private static final double north = 49.384359;
        private static final double east = -66.885666;
        private static final double south = 25.837377;
        private static final double west = -124.327629;

        private static final double center_lat = 39.8282;
        private static final double center_lon = -98.5795;

        public static final Position USA_CENTER = new Position(center_lat, center_lon);
        public static final Extent USA_BOUNDS = new Extent(north, east, south, west);

        public static final int PADDING = 30;

        public static final float ZOOM_GENERAL = 10f;
        public static final float ZOOM_CLOSE = 23;

        public static class GoogleMaps {
            public static final LatLng USA_CENTER = new LatLng(center_lat, center_lon);

            public static final LatLngBounds USA_BOUNDS =
                    new LatLngBounds(
                            new LatLng(south, west),
                            new LatLng(north, east)
                    );
        }
    }

    public static class Defaults {
        public static final String MainGroupName = "Main Group";

        public static TtGroup createDefaultGroup() {
            TtGroup group = new TtGroup();
            group.setCN(EmptyGuid);
            group.setName(MainGroupName);
            group.setDescription("Group for unassigned points.");
            group.setGroupType(TtGroup.GroupType.General);

            return group;
        }
    }
}
