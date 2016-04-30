package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.Units.Datum;
import com.usda.fmsc.twotrails.Units.DeclinationType;
import com.usda.fmsc.twotrails.Units.Dist;
import com.usda.fmsc.twotrails.Units.Slope;

import java.io.Serializable;

import com.usda.fmsc.geospatial.Units.UomElevation;

public class TtMetadata implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtMetadata(source);
        }

        @Override
        public TtMetadata[] newArray(int size) {
            return new TtMetadata[size];
        }
    };

    private String CN;
    private String Name;

    private int Zone;

    private Datum Datum;
    private Dist Distance;
    private UomElevation Elevation;
    private Slope Slope;

    private DeclinationType DecType;
    private double MagDec;

    private String GpsReceiver;
    private String RangeFinder;
    private String Compass;
    private String Crew;
    private String Comment;



    public TtMetadata() {
        this.CN = java.util.UUID.randomUUID().toString();
        this.Name = "Metadata";
        this.Zone = 13;
        this.Datum = Units.Datum.NAD83;
        this.Distance = Dist.FeetInches;
        this.Elevation = UomElevation.Meters;
        this.Slope = Units.Slope.Percent;
        this.DecType = DeclinationType.MagDec;
        this.MagDec = 0;

        this.GpsReceiver = null;
        this.RangeFinder = null;
        this.Compass = null;
        this.Crew = null;
        this.Comment = null;
    }

    public TtMetadata(Parcel source) {
        CN = source.readString();
        Name = source.readString();

        Zone = source.readInt();
        Datum = Units.Datum.parse(source.readInt());
        Distance = Dist.parse(source.readInt());
        Elevation = UomElevation.parse(source.readInt());
        Slope = Units.Slope.parse(source.readInt());
        DecType = DeclinationType.parse(source.readInt());

        MagDec = source.readDouble();

        GpsReceiver = source.readString();
        RangeFinder = source.readString();
        Compass = source.readString();
        Crew = source.readString();
        Comment = source.readString();
    }

    public TtMetadata(TtMetadata meta) {
        this.CN = meta.getCN();
        this.Name = meta.getName();
        this.Zone = meta.getZone();
        this.Datum = meta.getDatum();
        this.Distance = meta.getDistance();
        this.Elevation = meta.getElevation();
        this.Slope = meta.getSlope();
        this.DecType = meta.getDecType();
        this.MagDec = meta.getMagDec();

        this.GpsReceiver = meta.getGpsReceiver();
        this.RangeFinder = meta.getRangeFinder();
        this.Compass = meta.getCompass();
        this.Crew = meta.getCrew();
        this.Comment = meta.getComment();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(CN);
        dest.writeString(Name);

        dest.writeInt(Zone);
        dest.writeInt(Datum.getValue());
        dest.writeInt(Distance.getValue());
        dest.writeInt(Elevation.getValue());
        dest.writeInt(Slope.getValue());
        dest.writeInt(DecType.getValue());

        dest.writeDouble(MagDec);

        dest.writeString(GpsReceiver);
        dest.writeString(RangeFinder);
        dest.writeString(Compass);
        dest.writeString(Crew);
        dest.writeString(Comment);
    }


    public String getCN() {
        return CN;
    }

    public void setCN(String CN) {
        this.CN = CN;
    }


    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }


    public int getZone() {
        return Zone;
    }

    public void setZone(int zone) {
        Zone = zone;
    }


    public Units.Datum getDatum() {
        return Datum;
    }

    public void setDatum(Units.Datum datum) {
        Datum = datum;
    }


    public Dist getDistance() {
        return Distance;
    }

    public void setDistance(Dist distance) {
        Distance = distance;
    }


    public UomElevation getElevation() {
        return Elevation;
    }

    public void setElevation(UomElevation elevation) {
        Elevation = elevation;
    }


    public Units.Slope getSlope() {
        return Slope;
    }

    public void setSlope(Units.Slope slope) {
        Slope = slope;
    }


    public DeclinationType getDecType() {
        return DecType;
    }

    public void setDecType(DeclinationType decType) {
        DecType = decType;
    }


    public double getMagDec() {
        return MagDec;
    }

    public void setMagDec(double magDec) {
        MagDec = magDec;
    }


    public String getGpsReceiver() {
        return GpsReceiver;
    }

    public void setGpsReceiver(String gpsReceiver) {
        GpsReceiver = gpsReceiver;
    }


    public String getRangeFinder() {
        return RangeFinder;
    }

    public void setRangeFinder(String rangeFinder) {
        RangeFinder = rangeFinder;
    }


    public String getCompass() {
        return Compass;
    }

    public void setCompass(String compass) {
        Compass = compass;
    }


    public String getCrew() {
        return Crew;
    }

    public void setCrew(String crew) {
        Crew = crew;
    }


    public String getComment() {
        return Comment;
    }

    public void setComment(String comment) {
        Comment = comment;
    }

    @Override
    public String toString() {
        return Name != null ? Name : "[No Name]";
    }

}
