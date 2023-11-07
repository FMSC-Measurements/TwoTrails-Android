package com.usda.fmsc.twotrails.objects.points;


import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class InertialStartPoint extends GpsPoint implements TtPoint.IAzimuth {
    public static final Parcelable.Creator<InertialStartPoint> CREATOR = new Parcelable.Creator<InertialStartPoint>() {
        @Override
        public InertialStartPoint createFromParcel(Parcel source) {
            return new InertialStartPoint(source);
        }

        @Override
        public InertialStartPoint[] newArray(int size) {
            return new InertialStartPoint[size];
        }
    };

    private Double _FwdAz;
    private Double _BkAz;
    private Double _AzOffset;


    public InertialStartPoint() {
        super();
    }

    public InertialStartPoint(Parcel source) {
        super(source);

        _FwdAz = ParcelTools.readNDouble(source);
        _BkAz = ParcelTools.readNDouble(source);
        _AzOffset = ParcelTools.readNDouble(source);
    }


    public InertialStartPoint(TtPoint p) {
        super(p);

        if (p.getOp() == OpType.InertialStart) {
            copy((InertialStartPoint)p);
        }
    }


    public InertialStartPoint(InertialStartPoint p) {
        super(p);
        copy(p);
    }

    private void copy(InertialStartPoint p) {
        _FwdAz = p._FwdAz;
        _BkAz = p._BkAz;
        _AzOffset = p._AzOffset;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        ParcelTools.writeNDouble(dest, _FwdAz);
        ParcelTools.writeNDouble(dest, _BkAz);
        ParcelTools.writeNDouble(dest, _AzOffset);
    }

    public OpType getOp() {
        return OpType.InertialStart;
    }

    public Double getFwdAz() {
        return _FwdAz;
    }

    public void setFwdAz(Double fwdAz) {
        this._FwdAz = (fwdAz == null) ? null : TtUtils.Math.azimuthModulo(fwdAz);
    }

    public Double getBkAz() {
        return _BkAz;
    }

    public void setBkAz(Double bkAz) {
        this._BkAz = (bkAz == null) ? null : TtUtils.Math.azimuthModulo(bkAz);
    }

    public Double getAzOffset() {
        return _AzOffset;
    }

    public void setAzOffset(Double azOffset) {
        this._AzOffset = (azOffset == null) ? null : TtUtils.Math.azimuthModulo(azOffset);
    }

    public double getAzimuth() {
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
            return 0;
        }

        double aaz = (_FwdAz + adjustedBackAz) / 2;

        if (Math.abs(aaz - adjustedBackAz) > 100) {
            return TtUtils.Math.azimuthModulo(aaz + 180);
        } else {
            return TtUtils.Math.azimuthModulo(aaz);
        }
    }

    public Double getTotalAzimuth() {
        double az = getAzimuth();
        return  (_AzOffset != null) ? az + _AzOffset : az;
    }
}
