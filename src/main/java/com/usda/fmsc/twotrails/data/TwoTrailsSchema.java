package com.usda.fmsc.twotrails.data;

public class TwoTrailsSchema {
    //Old Schema Versions
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
        public static final String CreationTime = "CreationTime";
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
            Operation   + " INTEGER NOT NULL, " +
            MetadataCN  + " TEXT REFERENCES " +
            MetadataSchema.TableName + ", " +
            CreationTime+ " TEXT, " +
            AdjX        + " REAL, " +
            AdjY        + " REAL, " +
            AdjZ        + " REAL, " +
            UnAdjX      + " REAL NOT NULL, " +
            UnAdjY      + " REAL NOT NULL, " +
            UnAdjZ      + " REAL NOT NULL, " +
            Accuracy    + " REAL, " +
            QuondamLinks+ " TEXT, " +
            GroupName   + " TEXT, " +
            GroupCN     + " TEXT NOT NULL, " +
            "PRIMARY KEY (" + TwoTrailsSchema.SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            Order + ", " +
            ID + ", " +
            PolyName + ", " +
            PolyCN + ", " +
            OnBoundary + ", " +
            Comment + ", " +
            Operation + ", " +
            MetadataCN + ", " +
            CreationTime + ", " +
            AdjX + ", " +
            AdjY + ", " +
            AdjZ + ", " +
            UnAdjX + ", " +
            UnAdjY + ", " +
            UnAdjZ + ", " +
            Accuracy + ", " +
            QuondamLinks + ", " +
            GroupName + ", " +
            GroupCN;
    }
    //endregion

    //region Point Info GPS
    public static class GpsPointSchema {
        public static final String TableName = "GpsPointData";

        public static final String Latitude = "UnAdjLatitude";
        public static final String Longitude = "UnAdjLongitude";
        public static final String Elevation = "Elevation";
        public static final String ManualAccuracy = "ManualAccuracy";
        public static final String RMSEr = "RMSEr";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT REFERENCES " +
            PointSchema.TableName + ", " +
            Latitude        + " REAL, " +
            Longitude       + " REAL, " +
            Elevation       + " REAL, " +
            ManualAccuracy  + " REAL, " +
            RMSEr           + " REAL, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            Latitude + ", " +
            Longitude + ", " +
            Elevation + ", " +
            ManualAccuracy + ", " +
            RMSEr;
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
            SharedSchema.CN     + " TEXT REFERENCES " +
            PointSchema.TableName + ", " +
            ForwardAz           + " REAL, " +
            BackAz              + " REAL, " +
            SlopeDistance       + " REAL NOT NULL, " +
            VerticalAngle       + " REAL NOT NULL, " +
            HorizDistance       + " REAL, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            ForwardAz + ", " +
            BackAz + ", " +
            SlopeDistance + ", " +
            VerticalAngle;
    }
    //endregion

    //region Point Info Quondam Table
    public static class QuondamPointSchema {
        public static final String TableName = "QuondamPointData";

        public static final String ParentPointCN = "ParentPointCN";
        public static final String ManualAccuracy = "ManualAccuracy";

        public static final String CreateTable =
            "CREATE TABLE "     + TableName + " (" +
            SharedSchema.CN         + " TEXT REFERENCES " +
            PointSchema.TableName   + ", " +
            ParentPointCN           + " TEXT NOT NULL, " +
            ManualAccuracy          + " REAL, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            ParentPointCN + ", " +
            ManualAccuracy;
    }
    //endregion

    //region Polygon Table
    public static class PolygonSchema {
        public static final String TableName = "Polygons";

        public static final String Name = "Name";
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
            Name            + " TEXT, " +
            Accuracy        + " REAL, " +
            Description     + " TEXT, " +
            Area            + " REAL, " +
            Perimeter       + " REAL, " +
            IncrementBy     + " INTEGER, " +
            PointStartIndex + " INTEGER, " +
            TimeCreated     + " TEXT, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            Name + ", " +
            Accuracy + ", " +
            Description + ", " +
            Area + ", " +
            Perimeter + ", " +
            IncrementBy + ", " +
            PointStartIndex + ", " +
            TimeCreated;
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
            Name            + " TEXT, " +
            Description     + " TEXT, " +
            Type            + " TEXT, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";


        public static final String SelectItems =
            SharedSchema.CN + ", " +
            Name + ", " +
            Description + ", " +
            Type;
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
        public static final String TrackAngle = "TrackAngle";
        public static final String SatellitesUsedCount = "SatUsedCount";
        public static final String SatellitesTrackedCount = "SatTrackCount";
        public static final String SatellitesInViewCount = "SatInViewCount";
        public static final String UsedSatPRNS = "PRNS";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT, " +
            PointCN         + " TEXT, " +
            Used            + " BOOLEAN, " +
            TimeCreated     + " TEXT, " +
            FixTime         + " TEXT, " +
            Latitude        + " REAL, " +
            LatDir          + " INTEGER, " +
            Longitude       + " REAL, " +
            LonDir          + " INTEGER, " +
            Elevation       + " REAL, " +
            ElevUom         + " INTEGER, " +
            MagVar          + " REAL, " +
            MagDir          + " INTEGER, " +
            Fix             + " INTEGER, " +
            FixQuality      + " INTEGER, " +
            Mode            + " INTEGER, " +
            PDOP            + " REAL, " +
            HDOP            + " REAL, " +
            VDOP            + " REAL, " +
            HorizDilution   + " REAL, " +
            GeiodHeight     + " REAL, " +
            GeiodHeightUom  + " INTEGER, " +
            GroundSpeed     + " REAL, " +
            TrackAngle      + " REAL, " +
            SatellitesUsedCount     + " INTEGER, " +
            SatellitesTrackedCount  + " INTEGER, " +
            SatellitesInViewCount   + " INTEGER, " +
            UsedSatPRNS + " TEXT, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
            SharedSchema.CN + ", " +
            PointCN + ", " +
            Used + ", " +
            TimeCreated + ", " +
            FixTime + ", " +
            Latitude + ", " +
            LatDir + ", " +
            Longitude + ", " +
            LonDir + ", " +
            Elevation + ", " +
            ElevUom + ", " +
            MagVar + ", " +
            MagDir + ", " +
            Fix + ", " +
            FixQuality + ", " +
            Mode + ", " +
            PDOP + ", " +
            HDOP + ", " +
            VDOP + ", " +
            HorizDilution + ", " +
            GeiodHeight + ", " +
            GeiodHeightUom + ", " +
            GroundSpeed + ", " +
            TrackAngle + ", " +
            SatellitesUsedCount + ", " +
            SatellitesTrackedCount + ", " +
            SatellitesInViewCount + ", " +
            UsedSatPRNS;
    }
    //endregion

    //region MetaData Table
    public static class MetadataSchema {
        public static final String TableName = "MetaData";

        public static final String Name = "Name";
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
            Name            + " TEXT, " +
            Distance        + " TEXT, " +
            Slope           + " TEXT, " +
            MagDec          + " REAL, " +
            DeclinationType + " TEXT, " +
            Elevation       + " TEXT, " +
            Comment         + " TEXT, " +
            Datum           + " TEXT, " +
            GpsReceiver     + " TEXT, " +
            RangeFinder     + " TEXT, " +
            Compass         + " TEXT, " +
            Crew            + " TEXT, " +
            UtmZone         + " INTEGER, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
            SharedSchema.CN + ", " +
            Name + ", " +
            Distance + ", " +
            Slope + ", " +
            MagDec + ", " +
            DeclinationType + ", " +
            Elevation + ", " +
            Comment + ", " +
            Datum + ", " +
            GpsReceiver + ", " +
            RangeFinder + ", " +
            Compass + ", " +
            Crew + ", " +
            UtmZone;
    }
    //endregion

    //region Project Info Table
    public static class ProjectInfoSchema {
        public static final String TableName = "ProjectInfo";

        public static final String DeviceID = "Device";
        public static final String ID = "Name";
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
            ID                  + " TEXT, " +
            District            + " TEXT, " +
            Forest              + " TEXT, " +
            Region              + " TEXT, " +
            DeviceID            + " TEXT, " +
            Created             + " TEXT, " +
            Description         + " TEXT, " +
            TtDbSchemaVersion   + " TEXT, " +
            TtVersion           + " TEXT, " +
            CreatedTtVersion    + " TEXT" +
            ");";
    }
    //endregion

    //region Polygon Attr Table
    public static class PolygonAttrSchema {
        public static final String TableName = "PolygonAttr";

        public static final String AdjBndColor = "AdjBndColor";
        public static final String UnAdjBndColor = "UnAdjBndColor";
        public static final String AdjNavColor = "AdjNavColor";
        public static final String UnAdjNavColor = "UnAdjNavColor";
        public static final String AdjPtsColor = "AdjPtsColor";
        public static final String UnAdjPtsColor = "UnAdjPtsColor";
        public static final String WayPtsColor = "WayPtsColor";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT, " +
            AdjBndColor     + " INTEGER, " +
            UnAdjBndColor   + " INTEGER, " +
            AdjNavColor     + " INTEGER, " +
            UnAdjNavColor   + " INTEGER, " +
            AdjPtsColor     + " INTEGER, " +
            UnAdjPtsColor   + " INTEGER, " +
            WayPtsColor     + " INTEGER, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
            SharedSchema.CN + ", " +
            AdjBndColor + ", " +
            UnAdjBndColor + ", " +
            AdjNavColor + ", " +
            UnAdjNavColor + ", " +
            AdjPtsColor + ", " +
            UnAdjPtsColor + ", " +
            WayPtsColor;
    }
    //endregion

    //region Media

    //region Media Table
    public static class MediaSchema {
        public static final String TableName = "Media";

        public static final String PointCN = "PointCN";
        public static final String MediaType = "MediaType";
        public static final String Name = "Name";
        public static final String FilePath = "FilePath";
        public static final String CreationTime = "CreationTime";
        public static final String Comment = "Comment";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT NOT NULL, " +
            PointCN         + " TEXT, " +
            MediaType       + " INTEGER, " +
            Name            + " TEXT, " +
            FilePath        + " TEXT, " +
            CreationTime    + " TEXT, " +
            Comment         + " TEXT, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
                SharedSchema.CN + ", " +
                PointCN + ", " +
                MediaType + ", " +
                Name + ", " +
                FilePath + ", " +
                CreationTime + ", " +
                Comment;
    }
    //endregion


    //region PictureTable
    public static class PictureSchema {
        public static final String TableName = "PictureData";

        public static final String PicType = "Type";
        public static final String Azimuth = "Azimuth";
        public static final String Pitch = "Pitch";
        public static final String Roll = "Roll";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT REFERENCES " +
            MediaSchema.TableName + ", " +
            PicType     + " INTEGER, " +
            Azimuth     + " REAL, " +
            Pitch       + " REAL, " +
            Roll        + " REAL, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItemsNoCN =
            PicType + ", " +
            Azimuth + ", " +
            Pitch + ", " +
            Roll;
    }
    //endregion

    //endregion
}
