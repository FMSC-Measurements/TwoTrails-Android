package com.usda.fmsc.twotrails.objects;


import android.support.annotation.NonNull;

import com.usda.fmsc.twotrails.Units.OpType;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import com.usda.fmsc.utilities.StringEx;


public abstract class TtPoint implements Comparable<TtPoint>, Comparator<TtPoint>, Serializable {

    //region Vars
    protected String _CN;
    protected OpType _Op;
    protected long _Index;
    protected int _PID;
    protected DateTime _Time;

    protected boolean _calculated;
    protected boolean _adjusted;

    protected String _PolyCN;
    protected String _PolyName;

    protected String _GroupCN;
    protected String _GroupName;

    protected String _Comment;

    protected String _MetadataCN;

    protected boolean _OnBnd;

    protected Double _AdjX;
    protected Double _AdjY;
    protected Double _AdjZ;

    protected double _UnAdjX;
    protected double _UnAdjY;
    protected double _UnAdjZ;

    protected Double _Accuracy;

    protected ArrayList<String> _LinkedPoints;
    //endregion


    //region Constructors
    public TtPoint() {
        _Time = DateTime.now();
    }

    public TtPoint(TtPoint toCopy) {
        copy(toCopy);
    }

    //endregion


    //region Get/Set
    //region CN Op Index PID Time
    public String getCN() {
        if (StringEx.isEmpty(_CN))
            _CN = java.util.UUID.randomUUID().toString();
        return _CN;
    }

    public void setCN(String CN) {
        _CN = CN;
    }


    public OpType getOp() {
        return _Op;
    }


    public long getIndex() {
        return _Index;
    }

    public void setIndex(long index) {
        _Index = index;
    }


    public int getPID() {
        return _PID;
    }

    public void setPID(int PID) {
        this._PID = PID;
    }


    public DateTime getTime() {
        return _Time;
    }

    public void setTime(DateTime time) {
        this._Time = time;
    }
    //endregion

    //region Adjusted Calculated
    public boolean isAdjusted() {
        return _adjusted;
    }

    public boolean isCalculated() {
        return _calculated;
    }
    //endregion

    //region Poly
    public String getPolyCN() {
        return _PolyCN;
    }

    public void setPolyCN(String PolyCN) {
        this._PolyCN = PolyCN;
    }

    public String getPolyName() {
        return _PolyName;
    }

    public void setPolyName(String PolyName) {
        this._PolyName = PolyName;
    }
    //endregion

    //region Group
    public String getGroupCN() {
        return _GroupCN;
    }

    public void setGroupCN(String GroupCN) {
        this._GroupCN = GroupCN;
    }

    public String getGroupName() {
        return _GroupName;
    }

    public void setGroupName(String GroupName) {
        this._GroupName = GroupName;
    }
    //endregion

    //region Comment Meta Bound
    public String getComment() {
        return _Comment;
    }

    public void setComment(String Comment) {
        this._Comment = Comment;
    }


    public String getMetadataCN() {
        return _MetadataCN;
    }

    public void setMetadataCN(String MetaDefCN) {
        this._MetadataCN = MetaDefCN;
    }


    public boolean isOnBnd() {
        return _OnBnd;
    }
    public void setOnBnd(boolean OnBnd) {
        this._OnBnd = OnBnd;
    }
    //endregion

    //region Adj
    public Double getAdjX() {
        return _AdjX;
    }

    public void setAdjX(Double AdjX) {
        this._AdjX = AdjX;
    }

    public Double getAdjY() {
        return _AdjY;
    }

    public void setAdjY(Double AdjY) {
        this._AdjY = AdjY;
    }

    public Double getAdjZ() {
        return _AdjZ;
    }

    public void setAdjZ(Double AdjZ) {
        this._AdjZ = AdjZ;
    }
    //endregion

    //region UnAdj
    public Double getUnAdjX() {
        return _UnAdjX;
    }

    public void setUnAdjX(double UnAdjX) {
        this._UnAdjX = UnAdjX;
    }

    public Double getUnAdjY() {
        return _UnAdjY;
    }

    public void setUnAdjY(double UnAdjY) {
        this._UnAdjY = UnAdjY;
    }

    public Double getUnAdjZ() {
        return _UnAdjZ;
    }

    public void setUnAdjZ(double UnAdjZ) {
        this._UnAdjZ = UnAdjZ;
    }

    public void setAndCalc(double x, double y, double z, TtPolygon polygon) {
        _UnAdjX = x;
        _UnAdjY = y;
        _UnAdjZ = z;
        calculatePoint(polygon);
    }
    //endregion

    //region Accuracy
    public Double getAccuracy() {
        return _Accuracy;
    }

    public void setAccuracy(Double Accuracy) {
        this._Accuracy = Accuracy;
    }
    //endregion

    //region Linked Points
    public ArrayList<String> getLinkedPoints() {
        initLinksList();
        return _LinkedPoints;
    }

    public String getLinkedPointsString() {
        StringBuilder sb = new StringBuilder();

        for (String ql : getLinkedPoints()) {
            sb.append(ql);
            sb.append('_');
        }

        return sb.toString();
    }

    public boolean hasQuondamLinks()
    {
        return _LinkedPoints != null && _LinkedPoints.size() > 0;
    }

    public void addQuondamLink(String cn) {
        initLinksList();
        if(_LinkedPoints.indexOf(cn) < 0)
            _LinkedPoints.add(cn);
    }

    public void removeQuondamLink(String cn) {
        initLinksList();
        _LinkedPoints.remove(cn);
    }

    private void initLinksList() {
        if (_LinkedPoints == null) {
            _LinkedPoints = new ArrayList<>();
        }
    }
    //endregion
    //endregion


    //region copy Point
    public void copy(TtPoint toCopy) {
        this._CN = toCopy.getCN();
        this._Op = toCopy.getOp();
        this._Comment = toCopy.getComment();
        this._Index = toCopy.getIndex();
        this._PolyCN = toCopy.getPolyCN();
        this._PolyName = toCopy.getPolyName();
        this._PID = toCopy.getPID();
        this._Time = toCopy.getTime();
        this._AdjX = toCopy.getAdjX();
        this._AdjY = toCopy.getAdjY();
        this._AdjZ = toCopy.getAdjZ();
        this._UnAdjX = toCopy.getUnAdjX();
        this._UnAdjY = toCopy.getUnAdjY();
        this._UnAdjZ = toCopy.getUnAdjZ();
        this._Accuracy = toCopy.getAccuracy();
        this._LinkedPoints = toCopy.getLinkedPoints();
        this._MetadataCN = toCopy.getMetadataCN();
        this._OnBnd = toCopy.isOnBnd();
        this._GroupName = toCopy.getGroupName();
        this._GroupCN = toCopy.getGroupCN();
    }

    public void copyInfo(TtPoint toCopy) {
        this._CN = toCopy.getCN();

        if (StringEx.isEmpty(_Comment)) {
            _Comment = toCopy.getComment();
        }

        this._Index = toCopy.getIndex();
        this._PolyCN = toCopy.getPolyCN();
        this._PolyName = toCopy.getPolyName();
        this._PID = toCopy.getPID();
        this._LinkedPoints = toCopy.getLinkedPoints();
        this._MetadataCN = toCopy.getMetadataCN();
        this._GroupName = toCopy.getGroupName();
        this._GroupCN = toCopy.getGroupCN();
    }
    //endregion


    //region Point Types
    public boolean isGpsType() {
        return (_Op == OpType.GPS || _Op == OpType.Take5 ||
                _Op == OpType.Walk || _Op == OpType.WayPoint);
    }

    public boolean isTravType() {
        return (_Op == OpType.Traverse || _Op == OpType.SideShot);
    }

    public boolean isBndPoint() {
        return _OnBnd && _Op != OpType.WayPoint;
    }

    public boolean isNavPoint() {
        return  (_Op == OpType.GPS || _Op == OpType.Take5 ||
                _Op == OpType.Walk || _Op == OpType.Traverse);
    }
    //endregion


    //region Point Locations
    public boolean sameAdjLocation(TtPoint point) {
        return (this._AdjX.equals(point.getAdjX()) &&
                this._AdjY.equals(point.getAdjY()) &&
                this._AdjZ.equals(point.getAdjZ()));
    }

    public boolean sameUnAdjLocation(TtPoint point) {
        return  (this._UnAdjX == point.getUnAdjX() && this._UnAdjY == point.getUnAdjY() &&
                this._UnAdjZ == point.getUnAdjZ());
    }
    //endregion


    //region Adjusting / Calculating
    public abstract boolean adjustPoint();

    public boolean calculatePoint(TtPolygon polygon) {
        return calculatePoint(polygon, null);
    }

    public abstract boolean calculatePoint(TtPolygon polygon, TtPoint previousPoint);
    //endregion


    @Override
    public String toString() {
        return String.format("%d: %s", _PID, _Op);
    }

    @Override
    public int compareTo(@NonNull TtPoint another) {
        return compare(this, another);
    }

    @Override
    public int compare(TtPoint p1, TtPoint p2) {
        if (p1 == null && p2 == null)
            return 0;

        if (p1 == null)
            return -1;

        if (p2 == null)
            return 1;

        int val = p1.getPolyName().compareTo(p2.getPolyName());

        if (val != 0)
            return val;
        else
        {
            val = Long.valueOf(p1.getIndex()).compareTo(p2.getIndex());

            if (val != 0)
                return val;
            else
            {
                val = Integer.valueOf(p1.getPID()).compareTo(p2.getPID());

                if (val != 0)
                    return val;
                else
                    return p1.getCN().compareTo(p2.getCN());
            }
        }
    }


    public interface IManualAccuracy {
        Double getManualAccuracy();
        void setManualAccuracy(Double value);
    }
}
