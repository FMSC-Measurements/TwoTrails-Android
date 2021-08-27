package com.usda.fmsc.twotrails.objects.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.Comparator;

public class ArcGisMapLayer implements Parcelable, Comparable<ArcGisMapLayer> {
    public static final Parcelable.Creator<ArcGisMapLayer> CREATOR = new Parcelable.Creator<ArcGisMapLayer>() {
        @Override
        public ArcGisMapLayer createFromParcel(Parcel source) {
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
    private String url;
    private String filePath;
    private boolean online;
    private double minScale;
    private double maxScale;
    private DetailLevel[] levelsOfDetail;
    private double north, south, east, west;

    private boolean hasValidFile;


    public ArcGisMapLayer(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        location = in.readString();
        url = in.readString();
        setFilePath(in.readString());
        online = in.readByte() == 1;
        minScale = in.readDouble();
        maxScale = in.readDouble();
        levelsOfDetail = in.createTypedArray(DetailLevel.CREATOR);
        north = in.readDouble();
        south = in.readDouble();
        east = in.readDouble();
        west = in.readDouble();
    }

    public ArcGisMapLayer(ArcGisMapLayer agml) {
        this.id = agml.getId();
        this.name = agml.getName();
        this.description = agml.getDescription();
        this.location = agml.getLocation();
        this.url = agml.getUrl();
        this.filePath = agml.getFilePath();
        this.online = agml.isOnline();
        this.minScale = agml.getMinScale();
        this.maxScale = agml.getMaxScale();
        this.levelsOfDetail = agml.getLevelsOfDetail();
        this.hasValidFile = agml.hasValidFile();
        setExtent(agml.getExtent());
    }

    public ArcGisMapLayer(int id, String name, String description, String location, String url, String filePath, boolean online) {
        this(id, name, description, location, url, filePath, -1, -1, null, null, online);
    }

    public ArcGisMapLayer(int id, String name, String description, String location, String url, String filePath, double minScale,
                          double maxScale, DetailLevel[] levelsOfDetail, Extent extent, boolean online) {
        if (name == null)
            description = StringEx.Empty;

        if (description == null)
            description = StringEx.Empty;

        if (location == null)
            location = StringEx.Empty;

        if (url == null)
            url = StringEx.Empty;

        if (filePath == null)
            filePath = StringEx.Empty;

        if (levelsOfDetail == null)
            levelsOfDetail = new DetailLevel[0];

        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.url = url;
        setFilePath(filePath);
        this.online = online;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.levelsOfDetail = levelsOfDetail;
        setExtent(extent);
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
        dest.writeString(url);
        dest.writeString(filePath);
        dest.writeByte((byte) (online ? 1 : 0));
        dest.writeDouble(minScale);
        dest.writeDouble(maxScale);
        dest.writeTypedArray(levelsOfDetail, flags);
        dest.writeDouble(north);
        dest.writeDouble(south);
        dest.writeDouble(east);
        dest.writeDouble(west);
    }


    @Override
    public int compareTo(ArcGisMapLayer another) {
        return this.getName().compareTo((another).getName());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;

        hasValidFile = !StringEx.isEmpty(this.filePath) && filePath.endsWith(".tpk") && FileUtils.fileOrFolderExists(filePath);
    }

    public boolean hasValidFile() {
        return hasValidFile;
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
        return levelsOfDetail.length;
    }

    public boolean hasDetailLevels() {
        return levelsOfDetail != null && levelsOfDetail.length > 0;
    }

    public DetailLevel[] getLevelsOfDetail() {
        return levelsOfDetail;
    }

    public void setLevelsOfDetail(DetailLevel[] levelsOfDetail) {
        this.levelsOfDetail = levelsOfDetail;
    }


    public Extent getExtent() {
        return new Extent(north, east, south, west);
    }

    public boolean hasExtent() {
        return north != 0 || south != 0 || east != 0 || west != 0;
    }

    public void setExtent(Extent extent) {
        if (extent == null) {
            this.north = south = east = west = 0;
        } else {
            this.north = extent.getNorth();
            this.south = extent.getSouth();
            this.east = extent.getEast();
            this.west = extent.getWest();
        }
    }

    public void update(ArcGisMapLayer layer) {
        this.name = layer.getName();
        this.description = layer.getDescription();
        this.location = layer.getLocation();
        this.url = layer.getUrl();
        this.filePath = layer.getFilePath();
        this.online = layer.isOnline();
        this.minScale = layer.getMinScale();
        this.maxScale = layer.getMaxScale();
        this.levelsOfDetail = layer.getLevelsOfDetail();
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof ArcGisMapLayer) {
            ArcGisMapLayer agml = (ArcGisMapLayer)o;

            return
                this.name.equals(agml.getName()) &&
                this.description.equals(agml.getDescription()) &&
                this.location.equals(agml.getLocation()) &&
                this.url.equals(agml.getUrl()) &&
                this.filePath.equals(agml.getFilePath()) &&
                this.online == agml.isOnline() &&
                this.minScale == agml.getMinScale() &&
                this.maxScale == agml.getMaxScale() &&
                this.levelsOfDetail.length == agml.getNumberOfLevels();
        }

        return false;
    }


    public static class DetailLevel implements Parcelable {
        public static final Parcelable.Creator<DetailLevel> CREATOR = new Parcelable.Creator<DetailLevel>() {
            @Override
            public DetailLevel createFromParcel(Parcel source) {
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


        @Override
        public boolean equals(Object o) {
            if (o instanceof  DetailLevel) {
                DetailLevel dl = (DetailLevel)o;

                return
                    level == dl.level &&
                    resolution == dl.resolution &&
                    scale == dl.scale;
            }

            return false;
        }
    }

    public static Comparator<ArcGisMapLayer> Comparator =
            (lhs, rhs) -> lhs.isOnline() ^ rhs.isOnline() ?
                    (rhs.isOnline() ? -1 : 1) :
                    lhs.getName().compareTo(rhs.getName());
}
