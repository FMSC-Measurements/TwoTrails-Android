package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.twotrails.units.DeclinationType;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Datum;
import com.usda.fmsc.twotrails.units.Slope;

public class TtMetadata extends TtObject implements Parcelable {
    public static final Parcelable.Creator<TtMetadata> CREATOR = new Parcelable.Creator<TtMetadata>() {
        @Override
        public TtMetadata createFromParcel(Parcel source) {
            return new TtMetadata(source);
        }

        @Override
        public TtMetadata[] newArray(int size) {
            return new TtMetadata[size];
        }
    };

    private String Name;

    private int Zone;

    private Datum _Datum;
    private Dist _Distance;
    private UomElevation _Elevation;
    private Slope _Slope;

    private DeclinationType _DecType;
    private double _MagDec;

    private String _GpsReceiver;
    private String _RangeFinder;
    private String _Compass;
    private String _Crew;
    private String _Comment;



    public TtMetadata() {
        this.Name = "Metadata";
        this.Zone = 13;
        this._Datum = Datum.NAD83;
        this._Distance = Dist.FeetInches;
        this._Elevation = UomElevation.Meters;
        this._Slope = Slope.Percent;
        this._DecType = DeclinationType.MagDec;
        this._MagDec = 0;

        this._GpsReceiver = null;
        this._RangeFinder = null;
        this._Compass = null;
        this._Crew = null;
        this._Comment = null;
    }

    public TtMetadata(Parcel source) {
        super(source);

        Name = source.readString();

        Zone = source.readInt();
        _Datum = Datum.parse(source.readInt());
        _Distance = Dist.parse(source.readInt());
        _Elevation = UomElevation.parse(source.readInt());
        _Slope = Slope.parse(source.readInt());
        _DecType = DeclinationType.parse(source.readInt());

        _MagDec = source.readDouble();

        _GpsReceiver = source.readString();
        _RangeFinder = source.readString();
        _Compass = source.readString();
        _Crew = source.readString();
        _Comment = source.readString();
    }

    public TtMetadata(TtMetadata meta) {
        setCN(meta.getCN());
        this.Name = meta.getName();
        this.Zone = meta.getZone();
        this._Datum = meta.getDatum();
        this._Distance = meta.getDistance();
        this._Elevation = meta.getElevation();
        this._Slope = meta.getSlope();
        this._DecType = meta.getDecType();
        this._MagDec = meta.getMagDec();

        this._GpsReceiver = meta.getGpsReceiver();
        this._RangeFinder = meta.getRangeFinder();
        this._Compass = meta.getCompass();
        this._Crew = meta.getCrew();
        this._Comment = meta.getComment();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(Name);

        dest.writeInt(Zone);
        dest.writeInt(_Datum.getValue());
        dest.writeInt(_Distance.getValue());
        dest.writeInt(_Elevation.getValue());
        dest.writeInt(_Slope.getValue());
        dest.writeInt(_DecType.getValue());

        dest.writeDouble(_MagDec);

        dest.writeString(_GpsReceiver);
        dest.writeString(_RangeFinder);
        dest.writeString(_Compass);
        dest.writeString(_Crew);
        dest.writeString(_Comment);
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


    public Datum getDatum() {
        return _Datum;
    }

    public void setDatum(Datum datum) {
        _Datum = datum;
    }


    public Dist getDistance() {
        return _Distance;
    }

    public void setDistance(Dist distance) {
        _Distance = distance;
    }


    public UomElevation getElevation() {
        return _Elevation;
    }

    public void setElevation(UomElevation elevation) {
        _Elevation = elevation;
    }


    public Slope getSlope() {
        return _Slope;
    }

    public void setSlope(Slope slope) {
        _Slope = slope;
    }


    public DeclinationType getDecType() {
        return _DecType;
    }

    public void setDecType(DeclinationType decType) {
        _DecType = decType;
    }


    public double getMagDec() {
        return _MagDec;
    }

    public void setMagDec(double magDec) {
        _MagDec = magDec;
    }


    public String getGpsReceiver() {
        return _GpsReceiver;
    }

    public void setGpsReceiver(String gpsReceiver) {
        _GpsReceiver = gpsReceiver;
    }


    public String getRangeFinder() {
        return _RangeFinder;
    }

    public void setRangeFinder(String rangeFinder) {
        _RangeFinder = rangeFinder;
    }


    public String getCompass() {
        return _Compass;
    }

    public void setCompass(String compass) {
        _Compass = compass;
    }


    public String getCrew() {
        return _Crew;
    }

    public void setCrew(String crew) {
        _Crew = crew;
    }


    public String getComment() {
        return _Comment;
    }

    public void setComment(String comment) {
        _Comment = comment;
    }

    @Override
    public String toString() {
        return Name != null ? Name : "[No Name]";
    }
}
