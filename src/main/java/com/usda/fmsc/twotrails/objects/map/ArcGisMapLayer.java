package com.usda.fmsc.twotrails.objects.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.utilities.StringEx;

import java.io.Serializable;

public class ArcGisMapLayer implements Serializable, Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new ArcGisMapLayer(source);
        }

        @Override
        public ArcGisMapLayer[] newArray(int size) {
            return new ArcGisMapLayer[size];
        }
    };

    private int id;
    private String name;
    private String description;
    private String location;
    private String uri;
    private boolean online;
    private double minScale;
    private double maxScale;
    private int numberOfLevels;
    private DetailLevel[] levelsOfDetail;


    public ArcGisMapLayer(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        location = in.readString();
        uri = in.readString();
        online = in.readByte() == 1;
        minScale = in.readDouble();
        maxScale = in.readDouble();

        numberOfLevels = in.readInt();
        levelsOfDetail = (DetailLevel[])in.createTypedArray(DetailLevel.CREATOR);
    }

    public ArcGisMapLayer(int id, String name, String description, String location, String uri, boolean online) {
        this(id, name, description, location, uri, 0, 0, null, online);
    }

    public ArcGisMapLayer(int id, String name, String description, String location, String uri, double minScale,
                          double maxScale, DetailLevel[] levelsOfDetail, boolean online) {
        if (name == null)
            description = StringEx.Empty;

        if (uri == null)
            description = StringEx.Empty;

        if (description == null)
            description = StringEx.Empty;

        if (location == null)
            location = StringEx.Empty;

        if (levelsOfDetail == null)
            levelsOfDetail = new DetailLevel[0];

        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.uri = uri;
        this.online = online;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.numberOfLevels = levelsOfDetail.length;
        this.levelsOfDetail = levelsOfDetail;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(location);
        dest.writeString(uri);
        dest.writeByte((byte) (online ? 1 : 0));
        dest.writeDouble(minScale);
        dest.writeDouble(maxScale);
        dest.writeInt(numberOfLevels);
        dest.writeTypedArray(levelsOfDetail, flags);
    }


    public int getId() {
        return id;
    }

    public boolean isOnline() {
        return online;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public double getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
    }

    public double getMinScale() {
        return minScale;
    }

    public void setMinScale(double minScale) {
        this.minScale = minScale;
    }

    public boolean hasScales() {
        return minScale != 0 && maxScale != 0;
    }

    public int getNumberOfLevels() {
        return numberOfLevels;
    }

    public DetailLevel[] getLevelsOfDetail() {
        return levelsOfDetail;
    }


    public static class DetailLevel implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            @Override
            public Object createFromParcel(Parcel source) {
                return new DetailLevel(source);
            }

            @Override
            public DetailLevel[] newArray(int size) {
                return new DetailLevel[size];
            }
        };

        private int level;
        private double resolution;
        private double scale;

        public DetailLevel(Parcel in) {
            level = in.readInt();
            resolution = in.readDouble();
            scale = in.readDouble();
        }

        public DetailLevel(int level, double resolution, double scale) {
            this.level = level;
            this.resolution = resolution;
            this.scale = scale;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(level);
            dest.writeDouble(resolution);
            dest.writeDouble(scale);
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public double getResolution() {
            return resolution;
        }

        public void setResolution(double resolution) {
            this.resolution = resolution;
        }
    }
}
