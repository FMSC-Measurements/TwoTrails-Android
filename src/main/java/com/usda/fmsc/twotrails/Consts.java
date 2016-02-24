package com.usda.fmsc.twotrails;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class Consts {
    public static final DateTimeFormatter DateTimeFormatter = DateTimeFormat.forPattern("M/d/yyyy h:mm:ss a");

    public static final String LOG_TAG = "TT";

    public static final String EmptyGuid = "00000000-0000-0000-0000-000000000000";

    public static final double RMSEr95_Coeff = 1.7308;

    public static final double Default_Point_Accuracy = 6.0;
    public static final double Minimum_Point_Accuracy = 0.000001;
    public static final int Minimum_Point_Accuracy_Digits = 6;
    public static final int Minimum_Point_Display_Digits = 5;


    public static final float ENABLED_ALPHA = 1.0f;
    public static final float DISABLED_ALPHA = 0.5f;

    public static final int ENABLED_ICON_ALPHA = (int)(ENABLED_ALPHA * 255);
    public static final int DISABLED_ICON_ALPHA = (int)(DISABLED_ALPHA * 255);

    public static final String NewLine = "\r\n";


    public static final String ServiceTitle = "TwoTrails GPS";
    public static final String ServiceContent = "GPS Running";
    public static final String ServiceAcquiringPoint = "Acquiring Point";
    public static final String ServiceWalking = "Walking";


    public static class DeviceNames {
        public static final String[] GPS_RECEIVERS = new String[]{
                "QStar XT",
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

    public static class Activities {
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

        public static class Results {
            public static final int ERROR = 2000;
            public static final int POINT_CREATED = 2001;
            public static final int NO_POINT_DATA = 2002;
            public static final int NO_POLYGON_DATA = 2003;
            public static final int NO_METDATA_DATA = 2004;

        }

        public static class Services {
            public static final int REQUEST_GOOGLE_PLAY_SERVICES = 3000;
            public static final int REQUEST_GPS_SERVICE = 3001;
        }

        public static class Data {
            public static final String POINT_DATA = "PointData";
            public static final String POLYGON_DATA = "PolygonData";
            public static final String METADATA_DATA = "MetaDataData";
            public static final String ADDITIVE_NMEA_DATA = "AdditiveNmeaData";
            public static final String NUMBER_OF_CREATED_POINTS = "NumOfCreatedPoints";
        }

    }

    public static class GoogleMaps {
        public static final LatLngBounds USA_BOUNDS =
                new LatLngBounds(
                    new LatLng(-124.7844079, -124.7844079),
                    new LatLng(49.3457868, -66.9513812)
                );

        public static final LatLng USA_CENTER = new LatLng(39.8282, -98.5795);

        public static final int PADDING = 30;
        public static final float DEFAULT_ZOOM_CLOSE = 20f;
        public static final float DEFAULT_ZOOM_GENERAL = 10f;

        public static float ZOOM_CLOSE = 20;
        public static float ZOOM_GENERAL = 10;
    }
}