package com.usda.fmsc.twotrails.data;

public class TwoTrailsMediaSchema {
    //Old Schema Versions
    public static final TtVersion OSV_2_0_0 = new TtVersion(2, 0, 0);

    //Schema Version
    public static final TtVersion SchemaVersion = OSV_2_0_0;


    public static class SharedSchema {
        public static final String CN = "CN";
    }


    //region Info Table
    public static class Info {
        public static final String TableName = "Info";

        public static final String TtMediaDbSchemaVersion = "TtMediaDbSchemaVersion";

        public static final String CreateTable =
                "CREATE TABLE " + TableName + " (" + TableName + " TEXT);";

        public static final String SelectItems = TtMediaDbSchemaVersion;
    }
    //endregion



    //region Media Table
    public static class Media {
        public static final String TableName = "Media";

        public static final String PointCN = "PointCN";
        public static final String MediaType = "MediaType";
        public static final String Name = "Name";
        public static final String FilePath = "FilePath";
        public static final String CreationTime = "CreationTime";
        public static final String Comment = "Comment";
        public static final String IsExternal = "IsExternal";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT NOT NULL, " +
            PointCN         + " TEXT, " +
            MediaType       + " INTEGER, " +
            Name            + " TEXT, " +
            FilePath        + " TEXT, " +
            CreationTime    + " TEXT, " +
            Comment         + " TEXT, " +
            IsExternal      + " BOOLEAN NOT NULL, " +
            "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
                SharedSchema.CN + ", " +
                PointCN + ", " +
                MediaType + ", " +
                Name + ", " +
                FilePath + ", " +
                CreationTime + ", " +
                Comment + ", " +
                IsExternal;
    }
    //endregion


    //region Media Data Table
    public static class Data {
        public static final String TableName = "Data";

        public static final String DataType = "DataType";
        public static final String BinaryData = "BinaryData";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
                    SharedSchema.CN + " TEXT NOT NULL, " +
                    DataType + " TEXT, " +
                    BinaryData + " BLOB, " +
                    "PRIMARY KEY (" + SharedSchema.CN + "));";

        public static final String SelectItems =
            SharedSchema.CN + ", " +
                    DataType + ", " +
                    BinaryData;
    }
    //endregion


    //region PictureTable
    public static class Images {
        public static final String TableName = "Images";

        public static final String PicType = "Type";
        public static final String Azimuth = "Azimuth";
        public static final String Pitch = "Pitch";
        public static final String Roll = "Roll";

        public static final String CreateTable =
            "CREATE TABLE " + TableName + " (" +
            SharedSchema.CN + " TEXT REFERENCES " +
            Media.TableName + ", " +
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
}
