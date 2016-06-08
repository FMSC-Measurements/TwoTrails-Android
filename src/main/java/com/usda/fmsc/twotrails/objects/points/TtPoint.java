package com.usda.fmsc.twotrails.objects.points;

import android.os.Parcel;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.objects.TtObject;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.utilities.StringEx;


public abstract class TtPoint extends TtObject implements Comparable<TtPoint>, Comparator<TtPoint> {
    protected int _Index;
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

    protected List<String> _LinkedPoints = new ArrayList<>();


    //region Constructors
    public TtPoint() {
        _Time = DateTime.now();
    }

    public TtPoint(Parcel source) {
        super(source);

        _Index = source.readInt();
        _PID = source.readInt();
        _Time = (DateTime) source.readSerializable();

        _PolyCN = source.readString();
        _PolyName = source.readString();
        _GroupCN = source.readString();
        _GroupName = source.readString();

        _Comment = source.readString();
        _MetadataCN = source.readString();

        _OnBnd = ParcelTools.readBool(source);

        _AdjX = ParcelTools.readNDouble(source);
        _AdjY = ParcelTools.readNDouble(source);
        _AdjZ = ParcelTools.readNDouble(source);

        _UnAdjX = source.readDouble();
        _UnAdjY = source.readDouble();
        _UnAdjZ = source.readDouble();

        _Accuracy = ParcelTools.readNDouble(source);

        source.readStringList(_LinkedPoints);
    }

    public TtPoint(TtPoint toCopy) {
        copy(toCopy);
    }

    //endregion


    //region Get/Set
    //region _CN Op Index PID Time
    public abstract OpType getOp();


    public int getIndex() {
        return _Index;
    }

    public void setIndex(int index) {
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
    public List<String> getLinkedPoints() {
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
        if(_LinkedPoints.indexOf(cn) < 0)
            _LinkedPoints.add(cn);
    }

    public void removeQuondamLink(String cn) {
        _LinkedPoints.remove(cn);
    }
    //endregion
    //endregion


    //region copy Point
    public void copy(TtPoint toCopy) {
        setCN(toCopy.getCN());
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
        setCN(toCopy.getCN());

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
        return (getOp() == OpType.GPS || getOp() == OpType.Take5 ||
                getOp() == OpType.Walk || getOp() == OpType.WayPoint);
    }

    public boolean isTravType() {
        return (getOp() == OpType.Traverse || getOp() == OpType.SideShot);
    }

    public boolean isBndPoint() {
        return _OnBnd && getOp() != OpType.WayPoint;
    }

    public boolean isNavPoint() {
        return  (getOp() == OpType.GPS || getOp() == OpType.Take5 ||
                getOp() == OpType.Walk || getOp() == OpType.Traverse);
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
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeLong(_Index);
        dest.writeInt(_PID);
        dest.writeSerializable(_Time);

        dest.writeString(StringEx.getValueOrEmpty(_PolyCN));
        dest.writeString(StringEx.getValueOrEmpty(_PolyName));
        dest.writeString(StringEx.getValueOrEmpty(_GroupCN));
        dest.writeString(StringEx.getValueOrEmpty(_GroupName));

        dest.writeString(StringEx.getValueOrEmpty(_Comment));
        dest.writeString(StringEx.getValueOrEmpty(_MetadataCN));

        ParcelTools.writeBool(dest, _OnBnd);

        ParcelTools.writeNDouble(dest, _AdjX);
        ParcelTools.writeNDouble(dest, _AdjY);
        ParcelTools.writeNDouble(dest, _AdjZ);

        dest.writeDouble(_UnAdjX);
        dest.writeDouble(_UnAdjY);
        dest.writeDouble(_UnAdjZ);

        ParcelTools.writeNDouble(dest, _Accuracy);

        dest.writeStringList(getLinkedPoints());
    }

    @Override
    public String toString() {
        return String.format("%d: %s", _PID, getOp());
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
            val = Integer.valueOf(p1.getIndex()).compareTo(p2.getIndex());

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
