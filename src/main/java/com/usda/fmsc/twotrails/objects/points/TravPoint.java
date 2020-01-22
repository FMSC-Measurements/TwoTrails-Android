package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class TravPoint extends TtPoint {
    public static final Parcelable.Creator<TravPoint> CREATOR = new Parcelable.Creator<TravPoint>() {
        @Override
        public TravPoint createFromParcel(Parcel source) {
            return new TravPoint(source);
        }

        @Override
        public TravPoint[] newArray(int size) {
            return new TravPoint[size];
        }
    };

    private Double _FwdAz;
    private Double _BkAz;

    private double _SlopeDistance;
    private double _SlopeAngle;
    private double _Declination;


    //region Constructors
    public TravPoint() {
        defaultTraverseValues();
    }

    public TravPoint(Parcel source) {
        super(source);

        _FwdAz = ParcelTools.readNDouble(source);
        _BkAz = ParcelTools.readNDouble(source);

        _SlopeDistance = source.readDouble();
        _SlopeAngle = source.readDouble();
        _Declination = source.readDouble();
    }

    public TravPoint(TravPoint p) {
        super(p);
        copy(p);
    }

    public TravPoint(TtPoint p) {
        super(p);
        if (p.isTravType()) {
            copy((TravPoint)p);
        } else {
            defaultTraverseValues();
        }
    }

    private void copy(TravPoint p) {
        this._BkAz = p.getBkAz();
        this._FwdAz = p.getFwdAz();
        this._SlopeDistance = p.getSlopeDistance();
        this._SlopeAngle = p.getSlopeAngle();
    }

    //endregion


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        ParcelTools.writeNDouble(dest, _FwdAz);
        ParcelTools.writeNDouble(dest, _BkAz);

        dest.writeDouble(_SlopeDistance);
        dest.writeDouble(_SlopeAngle);
        dest.writeDouble(_Declination);
    }

    //region Get/Set
    public OpType getOp() {
        return OpType.Traverse;
    }

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

        double aaz = (_FwdAz + adjustedBackAz) / 2;

        if (Math.abs(aaz - adjustedBackAz) > 100) {
            return TtUtils.Math.azimuthModulo(aaz + 180);
        } else {
            return TtUtils.Math.azimuthModulo(aaz);
        }
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
        return _SlopeDistance * Math.cos(TtUtils.Convert.percentToRadians(_SlopeAngle));
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


    private boolean calcTravLocation(double startingX, double startingY, double startingZ, boolean isAdjusted) {
        Double azimuth = getAzimuth();

        //Must have a valid azimuth to proceed
        if (azimuth == null || azimuth < 0) {
            _calculated = false;
            throw new RuntimeException("Null Azimuth");
        }

        //Adjust by the declination
        /* Apply the magnetic declination */
        /* East declination is positve, west is negative */
        /* azimuth conversion from north to mathematic postive X-axis */
        azimuth = 90 - azimuth + getDeclination();
        double x, y, z, azAsRad = TtUtils.Convert.degreesToRadians(azimuth);

        double horizontalDist = getHorizontalDistance();

        x = startingX + (horizontalDist * Math.cos(azAsRad));
        y = startingY + (horizontalDist * Math.sin(azAsRad));
        z = startingZ + (getSlopeDistance() * Math.sin(TtUtils.Convert.percentToRadians(getSlopeAngle())));

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
