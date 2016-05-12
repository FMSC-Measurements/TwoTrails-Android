package com.usda.fmsc.twotrails.objects.points;

import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.Serializable;

public class TravPoint extends TtPoint implements Serializable {
    private Double _FwdAz;
    private Double _BkAz;

    private double _SlopeDistance;
    private double _SlopeAngle;
    private double _Declination;


    //region Constructors
    public TravPoint() {
        defaultTraverseValues();
        _Op = OpType.Traverse;
    }

    public TravPoint(TravPoint p) {
        copy(p);
    }

    public TravPoint(TtPoint p) {
        super(p);
        if (p.isTravType())
        {
            copy((TravPoint)p);
        }
        else
        {
            defaultTraverseValues();
        }
    }

    //endregion


    //region Get/Set
    public Double getFwdAz() {
        return _FwdAz;
    }

    public void setFwdAz(Double FwdAz) {
        if(FwdAz == null)
            this._FwdAz = null;
        else
            this._FwdAz = TtUtils.Math.azimuthModulo(FwdAz);
    }

    public Double getBkAz() {
        return _BkAz;
    }

    public void setBkAz(Double BkAz) {
        if(BkAz == null)
            this._BkAz = null;
        else
            this._BkAz = TtUtils.Math.azimuthModulo(BkAz);
    }

    public Double getAzimuth() {
        double adjustedBackAz;

        if (_FwdAz != null && _FwdAz >= 0 && _BkAz != null && _BkAz >= 0) {
            if (_BkAz > _FwdAz && _BkAz >= 180)
                adjustedBackAz = _BkAz - 180;
            else
                adjustedBackAz = _BkAz + 180;
        } else {
            if (_FwdAz != null && _FwdAz >= 0)
                return _FwdAz;
            else if (_BkAz != null && _BkAz >= 0)
                return TtUtils.Math.azimuthModulo(_BkAz + 180);
            return null;
        }

        return TtUtils.Math.azimuthModulo((_FwdAz + adjustedBackAz) / 2);
    }


    public double getSlopeDistance() {
        return _SlopeDistance;
    }

    public void setSlopeDistance(double SlopeDistance) {
        this._SlopeDistance = SlopeDistance;
    }

    public double getSlopeAngle() {
        return _SlopeAngle;
    }

    public void setSlopeAngle(double SlopeAngle) {
        this._SlopeAngle = SlopeAngle;
    }

    public double getHorizontalDistance() {
        return _SlopeDistance * Math.cos(_SlopeAngle * (Math.PI / 180.0));
    }


    public double getDeclination() {
        return _Declination;
    }

    public void setDeclination(double Declination) {
        this._Declination = Declination;
    }
    //endregion

    protected void defaultTraverseValues() {
        _FwdAz = null;
        _BkAz = null;
        _SlopeDistance = 0;
        _SlopeAngle = 0;
    }

    protected void copy(TravPoint p) {
        super.copy(p);
        this._BkAz = p.getBkAz();
        this._FwdAz = p.getFwdAz();
        this._SlopeDistance = p.getSlopeDistance();
        this._SlopeAngle = p.getSlopeAngle();
    }


    @Override
    public boolean adjustPoint() {
        if (!_calculated)
            return false;
        _AdjX = _UnAdjX;
        _AdjY = _UnAdjY;
        _AdjZ = _UnAdjZ;
        _adjusted = true;
        return true;
    }

    public boolean adjustPoint(TtPoint source) {
        return calcTravLocation(source.getAdjX(), source.getAdjY(), source.getAdjZ(), true);
    }

    @Override
    public boolean calculatePoint(TtPolygon polygon, TtPoint previousPoint) {
        return calcTravLocation(previousPoint.getUnAdjX(), previousPoint.getUnAdjY(), previousPoint.getUnAdjZ(), false);
    }


    protected boolean calcTravLocation(double startingX, double startingY, double startingZ, boolean isAdjusted) {
        Double azimuth = getAzimuth();

        //Must have a valid azimuth to proceed
        if (azimuth == null || azimuth < 0) {
            _calculated = false;
            throw new RuntimeException("Null Azimuth");
        }
        //Adjust by the declination
            /* Apply the magnetic declination */
            /* East declination is positve, west is negative */
        azimuth += getDeclination();

            /* azimuth conversion from north to mathematic postive X-axis */
        azimuth = 90 - azimuth;
        double x, y, z;

        double horizontalDist = getHorizontalDistance();

        x = startingX + (horizontalDist * Math.cos(azimuth * (Math.PI / 180)));
        y = startingY + (horizontalDist * Math.sin(azimuth * (Math.PI / 180)));
        z = startingZ + (horizontalDist * Math.tan(getSlopeAngle()));

        if (isAdjusted) {
            _AdjX = x;
            _AdjY = y;
            _AdjZ = z;
            _adjusted = true;
        } else {
            _UnAdjX = x;
            _UnAdjY = y;
            _UnAdjZ = z;
            _calculated = true;
            _adjusted = false;
        }

        return true;
    }

    public void setAdjusted() {
        _adjusted = true;
    }
}
