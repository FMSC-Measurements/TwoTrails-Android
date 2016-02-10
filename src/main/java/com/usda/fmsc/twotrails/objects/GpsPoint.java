package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Units.OpType;

import java.io.Serializable;

public class GpsPoint extends TtPoint implements TtPoint.IManualAccuracy, Serializable {
    private Double _ManualAccuracy;
    private Double _RMSEr;

    private Double _Latitude;
    private Double _Longitude;
    private Double _Elevation;


    //region Constructors
    public GpsPoint() {
        super();
        _Op = OpType.GPS;
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


    //region Get/Set
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

    protected void gpsCopy(GpsPoint p) {
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
