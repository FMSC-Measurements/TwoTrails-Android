package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units.OpType;
import com.usda.fmsc.utilities.StringEx;

import java.io.Serializable;

public class QuondamPoint extends TtPoint implements TtPoint.IManualAccuracy, Serializable {
    //vars
    private TtPoint _ParentPoint;
    private Double _ManualAccuracy;

    //region Constructors
    public QuondamPoint() {
        _ParentPoint = null;
        _Op = OpType.Quondam;
    }

    public QuondamPoint(QuondamPoint p) {
        super(p);
        _ParentPoint = p.getParentPoint();
        _Op = OpType.Quondam;
    }

    public QuondamPoint(TtPoint p) {
        super(p);

        if (p.getOp() == OpType.Quondam) {
            this._ParentPoint = ((QuondamPoint)p).getParentPoint();
        } else {
            this._ParentPoint = null;
        }

        _Op = OpType.Quondam;
    }

    //endregion


    //region Get/Set
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
        if(_ParentPoint == null)
            return false;
        return _calculated && _ParentPoint.isCalculated();
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
        return String.format("%d: %s- %s", _PID, _Op,
            (_ParentPoint != null) ? _ParentPoint.getPID() : StringEx.Empty);
    }
}
