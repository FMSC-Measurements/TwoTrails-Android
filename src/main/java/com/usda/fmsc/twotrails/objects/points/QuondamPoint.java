package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.utilities.StringEx;

public class QuondamPoint extends TtPoint implements TtPoint.IManualAccuracy {
    public static final Parcelable.Creator<QuondamPoint> CREATOR = new Parcelable.Creator<QuondamPoint>() {
        @Override
        public QuondamPoint createFromParcel(Parcel source) {
            return new QuondamPoint(source);
        }

        @Override
        public QuondamPoint[] newArray(int size) {
            return new QuondamPoint[size];
        }
    };

    //vars
    private TtPoint _ParentPoint;
    private Double _ManualAccuracy;

    //region Constructors
    public QuondamPoint() {
        _ParentPoint = null;
    }

    public QuondamPoint(Parcel source) {
        super(source);

        if (ParcelTools.readBool(source)) {
            switch (OpType.parse(source.readInt())) {
                case GPS:
                    _ParentPoint = source.readParcelable(GpsPoint.class.getClassLoader());
                    break;
                case Take5:
                    _ParentPoint = source.readParcelable(Take5Point.class.getClassLoader());
                    break;
                case Traverse:
                    _ParentPoint = source.readParcelable(TravPoint.class.getClassLoader());
                    break;
                case SideShot:
                    _ParentPoint = source.readParcelable(SideShotPoint.class.getClassLoader());
                    break;
                case Quondam:
                    _ParentPoint = source.readParcelable(QuondamPoint.class.getClassLoader());
                    break;
                case Walk:
                    _ParentPoint = source.readParcelable(WalkPoint.class.getClassLoader());
                    break;
                case WayPoint:
                    _ParentPoint = source.readParcelable(WayPoint.class.getClassLoader());
                    break;
            }
        }

        _ManualAccuracy = ParcelTools.readNDouble(source);
    }

    public QuondamPoint(QuondamPoint p) {
        super(p);
        _ParentPoint = p.getParentPoint();
    }

    public QuondamPoint(TtPoint p) {
        super(p);

        if (p.getOp() == OpType.Quondam) {
            this._ParentPoint = ((QuondamPoint)p).getParentPoint();
        } else {
            this._ParentPoint = null;
        }
    }

    //endregion


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        if (_ParentPoint != null) {
            ParcelTools.writeBool(dest, true);
            dest.writeInt(_ParentPoint.getOp().getValue());
            dest.writeParcelable(_ParentPoint, flags);
        }

        ParcelTools.writeNDouble(dest, _ManualAccuracy);
    }

    //region Get/Set
    public OpType getOp() {
        return OpType.Quondam;
    }

    public TtPoint getParentPoint() {
        return _ParentPoint;
    }

    public void setParentPoint(TtPoint ParentPoint) {
        this._ParentPoint = ParentPoint;
    }

    public String getParentCN() {
        if(_ParentPoint == null || StringEx.isEmpty(_ParentPoint.getCN()))
            return StringEx.Empty;
        return _ParentPoint.getCN();
    }

    public int getParentPID() {
        return _ParentPoint.getPID();
    }

    public String getParentPolyName() {
        return _ParentPoint.getPolyName();
    }

    public OpType getParentOp() {
        return _ParentPoint.getOp();
    }

    @Override
    public Double getManualAccuracy() {
        return _ManualAccuracy;
    }

    @Override
    public void setManualAccuracy(Double ManualAccuracy) {
        this._ManualAccuracy = ManualAccuracy;
    }

    @Override
    public Double getAccuracy() {
        _Accuracy = (_ManualAccuracy != null) ? _ManualAccuracy : (_ParentPoint != null ? _ParentPoint.getAccuracy() : null);
        return _Accuracy;
    }

    //endregion


    public boolean hasParent() {
        return _ParentPoint != null;
    }

    @Override
    public boolean isCalculated() {
        return _ParentPoint != null && _calculated && _ParentPoint.isCalculated();
    }

    @Override
    public boolean calculatePoint(TtPolygon polygon, TtPoint previousPoint) {
        if (_ParentPoint == null || !_ParentPoint.isCalculated())
            return false;

        _UnAdjX = _ParentPoint.getUnAdjX();
        _UnAdjY = _ParentPoint.getUnAdjY();
        _UnAdjZ = _ParentPoint.getUnAdjZ();

        _MetadataCN = _ParentPoint.getMetadataCN();
        _calculated = true;

        return true;
    }

    @Override
    public boolean adjustPoint() {
        if (_ParentPoint.isAdjusted()) {
            _AdjX = _ParentPoint.getAdjX();
            _AdjY = _ParentPoint.getAdjY();
            _AdjZ = _ParentPoint.getAdjZ();
            _adjusted = true;
        }

        return _adjusted;
    }


    @Override
    public String toString() {
        return String.format("%d: %s- %s", _PID, getOp(),
            (_ParentPoint != null) ? _ParentPoint.getPID() : StringEx.Empty);
    }
}
