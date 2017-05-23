package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;

public class GpsPoint extends TtPoint implements TtPoint.IManualAccuracy {
    public static final Parcelable.Creator<GpsPoint> CREATOR = new Parcelable.Creator<GpsPoint>() {
        @Override
        public GpsPoint createFromParcel(Parcel source) {
            return new GpsPoint(source);
        }

        @Override
        public GpsPoint[] newArray(int size) {
            return new GpsPoint[size];
        }
    };

    private Double _ManualAccuracy;
    private Double _RMSEr;

    private Double _Latitude;
    private Double _Longitude;
    private Double _Elevation;


    //region Constructors
    public GpsPoint() {
        super();
    }

    public GpsPoint(Parcel source) {
        super(source);

        _ManualAccuracy = ParcelTools.readNDouble(source);
        _RMSEr = ParcelTools.readNDouble(source);
        _Latitude = ParcelTools.readNDouble(source);
        _Longitude = ParcelTools.readNDouble(source);
        _Elevation = ParcelTools.readNDouble(source);
    }

    public GpsPoint(GpsPoint p) {
        super(p);
        gpsCopy(p);
    }

    public GpsPoint(TtPoint p) {
        super(p);

        if(p.isGpsType()) {
            gpsCopy((GpsPoint)p);
        }
    }
    //endregion


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        ParcelTools.writeNDouble(dest, _ManualAccuracy);
        ParcelTools.writeNDouble(dest, _RMSEr);
        ParcelTools.writeNDouble(dest, _Latitude);
        ParcelTools.writeNDouble(dest, _Longitude);
        ParcelTools.writeNDouble(dest, _Elevation);
    }

    //region Get/Set


    @Override
    public OpType getOp() {
        return OpType.GPS;
    }

    @Override
    public Double getManualAccuracy() {
        return _ManualAccuracy;
    }

    @Override
    public void setManualAccuracy(Double ManualAccuracy) {
        this._ManualAccuracy = ManualAccuracy;
    }

    public Double getRMSEr() {
        return _RMSEr;
    }

    public void setRMSEr(Double RMSEr) {
        this._RMSEr = RMSEr;
    }

    public Double getNSSDA_RMSEr() {
        if (_RMSEr == null)
            return null;
        return _RMSEr * Consts.RMSEr95_Coeff;
    }

    public Double getLatitude() {
        return _Latitude;
    }

    public void setLatitude(Double latitude) {
        this._Latitude = latitude;
    }

    public Double getLongitude() {
        return _Longitude;
    }

    public void setLongitude(Double longitude) {
        this._Longitude = longitude;
    }

    public Double getElevation() {
        return _Elevation;
    }

    public void setElevation(Double elevation) {
        _Elevation = elevation;
    }

    //endregion

    private void gpsCopy(GpsPoint p) {
        this._ManualAccuracy = p.getManualAccuracy();
        this._RMSEr = p.getRMSEr();
        this._Latitude = p.getLatitude();
        this._Longitude = p.getLongitude();
        this._Elevation = p.getElevation();
    }

    public boolean hasLatLon() {
        return _Latitude != null && _Longitude != null;
    }

    public void clearLatLon() {
        this._Latitude = this._Longitude = this._Elevation = null;
    }

    @Override
    public boolean calculatePoint(TtPolygon polygon, TtPoint previousPoint) {
        _Accuracy = (_ManualAccuracy != null) ? _ManualAccuracy : polygon.getAccuracy();
        _calculated = true;
        return true;
    }

    @Override
    public boolean adjustPoint() {
        _AdjX = _UnAdjX;
        _AdjY = _UnAdjY;
        _AdjZ = _UnAdjZ;
        _adjusted = true;
        return true;
    }
}
