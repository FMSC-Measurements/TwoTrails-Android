package com.usda.fmsc.twotrails.data;

public class TwoTrailsSchema {
    //Old Schema Version
    public static final TtDalVersion OSV_2_0_0 = new TtDalVersion(2, 0, 0);

    //Schema Version
    public static final TtDalVersion SchemaVersion = OSV_2_0_0;


    public static class SharedSchema {
        public static final String CN = "CN";
    }

    //region Point Info Table
    public static class PointSchema {
        public static final String TableName = "Points";

        public static final String Order = "PointIndex";
        public static final String ID = "PID";
        public static final String PolyName = "PolyName";
        public static final String PolyCN = "PolyCN";
        public static final String OnBoundary = "Boundary";
        public static final String Comment = "Comment";
        public static final String Operation = "Operation";
        public static final String MetadataCN = "MetaCN";
        public static final String Time = "CreationTime";
        public static final String AdjX = "AdjX";
        public static final String AdjY = "AdjY";
        public static final String AdjZ = "AdjZ";
        public static final String UnAdjX = "UnAdjX";
        public static final String UnAdjY = "UnAdjY";
        public static final String UnAdjZ = "UnAdjZ";
        public static final String Accuracy = "Accuracy";
        public static final String QuondamLinks = "QuondamLinks";
        public static final String GroupName = "GroupName";
        public static final String GroupCN = "GroupCN";


        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        TwoTrailsSchema.SharedSchema.CN + " TEXT, " +
        Order       + " INTEGER NOT NULL, " +
        ID          + " INTEGER NOT NULL, " +
        PolyName    + " TEXT, " +
        PolyCN      + " TEXT NOT NULL, " +
        OnBoundary  + " BOOLEAN NOT NULL," +
        Comment     + " TEXT, " +
        Operation   + " TEXT NOT NULL, " +
        MetadataCN  + " TEXT REFERENCES " +
        MetadataSchema.TableName + ", " +
        Time        + " TEXT, " +
        AdjX        + " REAL, " +
        AdjY        + " REAL, " +
        AdjZ        + " REAL, " +
        UnAdjX      + " REAL NOT NULL, " +
        UnAdjY      + " REAL NOT NULL, " +
        UnAdjZ      + " REAL NOT NULL, " +
        Accuracy            + " REAL, " +
        QuondamLinks    + " TEXT, " +
        GroupName   + " TEXT, " +
        GroupCN     + " TEXT NOT NULL, " +
        "PRIMARY KEY (" + TwoTrailsSchema.SharedSchema.CN + "));";
    }
    //endregion

    //region Point Info GPS
    public static class GpsPointSchema {
        public static final String TableName = "GpsPointData";

        public static final String Latitude = "UnAdjLatitude";
        public static final String Longitude = "UnAdjLongitude";
        public static final String Elevation = "Elevation";
        public static final String UserAccuracy = "UserAccuracy";
        public static final String RMSEr = "RMSEr";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT REFERENCES " +
        PointSchema.TableName + ", " +
        Latitude        + " REAL, " +
        Longitude       + " REAL, " +
        Elevation       + " REAL, " +
        UserAccuracy    + " REAL, " +
        RMSEr           + " REAL, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region Point Info Traverse/SideShot table
    public static class TravPointSchema {
        public static final String TableName = "TravPointData";

        public static final String ForwardAz = "ForwardAz";
        public static final String BackAz = "BackAz";
        public static final String SlopeDistance = "SlopeDistance";
        public static final String VerticalAngle = "VerticalAngle";
        public static final String HorizDistance = "HorizontalDistance";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT REFERENCES " +
        PointSchema.TableName + ", " +
        ForwardAz           + " REAL, " +
        BackAz              + " REAL, " +
        SlopeDistance       + " REAL NOT NULL, " +
        VerticalAngle       + " REAL NOT NULL, " +
        HorizDistance       + " REAL, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region Point Info Quondam Table
    public static class QuondamPointSchema {
        public static final String TableName = "QuondamPointData";

        public static final String ParentPointCN = "ParentPointCN";
        public static final String ManualAccuracy = "ManualAccuracy";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT REFERENCES " +
        PointSchema.TableName   + ", " +
        ParentPointCN           + " TEXT NOT NULL, " +
        ManualAccuracy          + " REAL, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region Polygon Table
    public static class PolygonSchema {
        public static final String TableName = "Polygons";

        public static final String PolyID = "PolyID";
        public static final String Accuracy = "Accuracy";
        public static final String Description = "Description";
        public static final String Area = "Area";
        public static final String Perimeter = "Perimeter";
        public static final String IncrementBy = "Increment";
        public static final String PointStartIndex = "PointStartIndex";
        public static final String TimeCreated = "TimeCreated";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT, " +
        PolyID          + " TEXT, " +
        Accuracy        + " REAL, " +
        Description     + " TEXT, " +
        Area            + " REAL, " +
        Perimeter       + " REAL, " +
        IncrementBy     + " INTEGER, " +
        PointStartIndex + " INTEGER, " +
        TimeCreated     + " TEXT, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region Group Table
    public static class GroupSchema {
        public static final String TableName = "Groups";

        public static final String Name = "Name";
        public static final String Description = "Description";
        public static final String Type = "Type";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT, " +
        Name        + " TEXT, " +
        Description + " TEXT, " +
        Type        + " TEXT, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region TTNMEA table
    public static class TtNmeaSchema {
        public static final String TableName = "TTNMEA";

        public static final String PointCN = "PointCN";
        public static final String Used = "Used";
        public static final String TimeCreated = "TimeCreated";
        public static final String FixTime = "FixTime";
        public static final String Latitude = "Latitude";
        public static final String LatDir = "LatDir";
        public static final String Longitude = "Longitude";
        public static final String LonDir = "LonDir";
        public static final String Elevation = "Elevation";
        public static final String ElevUom = "ElevUom";
        public static final String MagVar = "MagVar";
        public static final String MagDir = "MagDir";
        public static final String Fix = "Fix";
        public static final String FixQuality = "FixQuality";
        public static final String Mode = "Mode";
        public static final String PDOP = "PDOP";
        public static final String HDOP = "HDOP";
        public static final String VDOP = "VDOP";
        public static final String HorizDilution = "HorizDilution";
        public static final String GeiodHeight = "GeiodHeight";
        public static final String GeiodHeightUom = "GeiodHeightUom";
        public static final String GroundSpeed = "GroundSpeed";
        public static final String Track_Angle = "Track_Angle";
        public static final String SatellitesUsedCount = "SatUsedCount";
        public static final String SatellitesTrackedCount = "SatTrackCount";
        public static final String SatellitesInViewCount = "SatInViewCount";
        public static final String UsedSatPRNS = "PRNS";

        //region Satellite Information Fields 1-12
        public static final String Sat1ID = "Sat1ID";
        public static final String Sat1Elev = "Sat1Elev";
        public static final String Sat1Az = "Sat1Az";
        public static final String Sat1SRN = "Sat1SRN";
        public static final String Sat2ID = "Sat2ID";
        public static final String Sat2Elev = "Sat2Elev";
        public static final String Sat2Az = "Sat2Az";
        public static final String Sat2SRN = "Sat2SRN";
        public static final String Sat3ID = "Sat3ID";
        public static final String Sat3Elev = "Sat3Elev";
        public static final String Sat3Az = "Sat3Az";
        public static final String Sat3SRN = "Sat3SRN";
        public static final String Sat4ID = "Sat4ID";
        public static final String Sat4Elev = "Sat4Elev";
        public static final String Sat4Az = "Sat4Az";
        public static final String Sat4SRN = "Sat4SRN";
        public static final String Sat5ID = "Sat5ID";
        public static final String Sat5Elev = "Sat5Elev";
        public static final String Sat5Az = "Sat5Az";
        public static final String Sat5SRN = "Sat5SRN";
        public static final String Sat6ID = "Sat6ID";
        public static final String Sat6Elev = "Sat6Elev";
        public static final String Sat6Az = "Sat6Az";
        public static final String Sat6SRN = "Sat6SRN";
        public static final String Sat7ID = "Sat7ID";
        public static final String Sat7Elev = "Sat7Elev";
        public static final String Sat7Az = "Sat7Az";
        public static final String Sat7SRN = "Sat7SRN";
        public static final String Sat8ID = "Sat8ID";
        public static final String Sat8Elev = "Sat8Elev";
        public static final String Sat8Az = "Sat8Az";
        public static final String Sat8SRN = "Sat8SRN";
        public static final String Sat9ID = "Sat9ID";
        public static final String Sat9Elev = "Sat9Elev";
        public static final String Sat9Az = "Sat9Az";
        public static final String Sat9SRN = "Sat9SRN";
        public static final String Sat10ID = "Sat10ID";
        public static final String Sat10Elev = "Sat10Elev";
        public static final String Sat10Az = "Sat10Az";
        public static final String Sat10SRN = "Sat10SRN";
        public static final String Sat11ID = "Sat11ID";
        public static final String Sat11Elev = "Sat11Elev";
        public static final String Sat11Az = "Sat11Az";
        public static final String Sat11SRN = "Sat11SRN";
        public static final String Sat12ID = "Sat12ID";
        public static final String Sat12Elev = "Sat12Elev";
        public static final String Sat12Az = "Sat12Az";
        public static final String Sat12SRN = "Sat12SRN";
        //endregion

        public static final String[] SatIDs = new String[] {
                Sat1ID, Sat2ID, Sat3ID, Sat4ID, Sat5ID, Sat6ID,
                Sat7ID, Sat8ID, Sat9ID, Sat10ID, Sat11ID, Sat12ID
        };

        public static final String[] SatElevs = new String[] {
                Sat1Elev, Sat2Elev, Sat3Elev, Sat4Elev, Sat5Elev, Sat6Elev,
                Sat7Elev, Sat8Elev, Sat9Elev, Sat10Elev, Sat11Elev, Sat12Elev
        };

        public static final String[] SatAzs = new String[] {
                Sat1Az, Sat2Az, Sat3Az, Sat4Az, Sat5Az, Sat6Az,
                Sat7Az, Sat8Az, Sat9Az, Sat10Az, Sat11Az, Sat12Az
        };

        public static final String[] SatSRNs = new String[] {
                Sat1SRN, Sat2SRN, Sat3SRN, Sat4SRN, Sat5SRN, Sat6SRN,
                Sat7SRN, Sat8SRN, Sat9SRN, Sat10SRN, Sat11SRN, Sat12SRN
        };


        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN   + " TEXT, " +
        PointCN     + " TEXT, " +
        Used        + " BOOLEAN, " +
        TimeCreated     + " TEXT, " +
        FixTime         + " TEXT, " +
        Latitude        + " REAL, " +
        LatDir          + " TEXT, " +
        Longitude       + " REAL, " +
        LonDir          + " TEXT, " +
        Elevation       + " REAL, " +
        ElevUom         + " TEXT, " +
        MagVar          + " REAL, " +
        MagDir          + " TEXT, " +
        Fix             + " INTEGER, " +
        FixQuality      + " INTEGER, " +
        Mode            + " INTEGER, " +
        PDOP            + " REAL, " +
        HDOP            + " REAL, " +
        VDOP            + " REAL, " +
        HorizDilution   + " REAL, " +
        GeiodHeight     + " REAL, " +
        GeiodHeightUom  + " TEXT, " +
        GroundSpeed     + " REAL, " +
        Track_Angle     + " REAL, " +
        SatellitesUsedCount     + " INTEGER, " +
        SatellitesTrackedCount  + " INTEGER, " +
        SatellitesInViewCount   + " INTEGER, " +

        //region PRNS
        UsedSatPRNS + " TEXT, " +
        
        Sat1ID + " TEXT, " +
        Sat1Elev + " REAL, " +
        Sat1Az + " REAL, " +
        Sat1SRN + " REAL, " +

        Sat2ID + " TEXT, " +
        Sat2Elev + " REAL, " +
        Sat2Az + " REAL, " +
        Sat2SRN + " REAL, " +

        Sat3ID + " TEXT, " +
        Sat3Elev + " REAL, " +
        Sat3Az + " REAL, " +
        Sat3SRN + " REAL, " +

        Sat4ID + " TEXT, " +
        Sat4Elev + " REAL, " +
        Sat4Az + " REAL, " +
        Sat4SRN + " REAL, " +

        Sat5ID + " TEXT, " +
        Sat5Elev + " REAL, " +
        Sat5Az + " REAL, " +
        Sat5SRN + " REAL, " +

        Sat6ID + " TEXT, " +
        Sat6Elev + " REAL, " +
        Sat6Az + " REAL, " +
        Sat6SRN + " REAL, " +

        Sat7ID + " TEXT, " +
        Sat7Elev + " REAL, " +
        Sat7Az + " REAL, " +
        Sat7SRN + " REAL, " +

        Sat8ID + " TEXT, " +
        Sat8Elev + " REAL, " +
        Sat8Az + " REAL, " +
        Sat8SRN + " REAL, " +

        Sat9ID + " TEXT, " +
        Sat9Elev + " REAL, " +
        Sat9Az + " REAL, " +
        Sat9SRN + " REAL, " +

        Sat10ID + " TEXT, " +
        Sat10Elev + " REAL, " +
        Sat10Az + " REAL, " +
        Sat10SRN + " REAL, " +

        Sat11ID + " TEXT, " +
        Sat11Elev + " REAL, " +
        Sat11Az + " REAL, " +
        Sat11SRN + " REAL, " +

        Sat12ID + " TEXT, " +
        Sat12Elev + " REAL, " +
        Sat12Az + " REAL, " +
        Sat12SRN + " REAL, " +
        //endregion

        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region MetaData Table
    public static class MetadataSchema {
        public static final String TableName = "MetaData";

        public static final String ID = "MetaID";
        public static final String Distance = "Distance";
        public static final String Slope = "Slope";
        public static final String MagDec = "MagDec";
        public static final String DeclinationType = "DecType";
        public static final String Elevation = "Elevation";
        public static final String Comment = "Comment";
        public static final String Datum = "Datum";
        public static final String GpsReceiver = "GpsReceiver";
        public static final String RangeFinder = "RangeFinder";
        public static final String Compass = "Compass";
        public static final String Crew = "Crew";
        public static final String UtmZone = "UtmZone";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN + " TEXT NOT NULL, " +
        ID              + " TEXT, " +
        Distance        + " TEXT, " +
        Slope           + " TEXT, " +
        MagDec          + " REAL, " +
        DeclinationType + " TEXT, " +
        Elevation       + " TEXT, " +
        Comment         + " TEXT, " +
        Datum           + " TEXT, " +
        GpsReceiver        + " TEXT, " +
        RangeFinder           + " TEXT, " +
        Compass         + " TEXT, " +
        Crew            + " TEXT, " +
        UtmZone         + " INTEGER, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion

    //region Project Info Table
    public static class ProjectInfoSchema {
        public static final String TableName = "ProjectInfo";

        public static final String DeviceID = "Device";
        public static final String ID = "ID";
        public static final String Region = "Region";
        public static final String Forest = "Forest";
        public static final String District = "District";
        public static final String Created = "DateCreated";
        public static final String Description = "Description";
        public static final String TtDbSchemaVersion = "TtDbSchemaVersion";
        public static final String TtVersion = "TtVersion";
        public static final String CreatedTtVersion = "CreatedTtVersion";

        public static final String CreateTable =
        "CREATE TABLE " + TableName + " (" +
        SharedSchema.CN   + " TEXT, " +
        ID          + " TEXT, " +
        District    + " TEXT, " +
        Forest      + " TEXT, " +
        Region      + " TEXT, " +
        DeviceID    + " TEXT, " +
        Created     + " TEXT, " +
        Description         + " TEXT, " +
        TtDbSchemaVersion   + " TEXT, " +
        TtVersion           + " TEXT, " +
        CreatedTtVersion    + " TEXT, " +
        "PRIMARY KEY (" + SharedSchema.CN + "));";
    }
    //endregion
}
