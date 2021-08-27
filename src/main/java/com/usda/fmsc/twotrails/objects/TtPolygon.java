package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.units.UnitType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.util.Comparator;

public class TtPolygon extends TtObject implements Comparable<TtPolygon>, Comparator<TtPolygon>, Parcelable {
    public static final Parcelable.Creator<TtPolygon> CREATOR = new Parcelable.Creator<TtPolygon>() {
        @Override
        public TtPolygon createFromParcel(Parcel source) {
            return new TtPolygon(source);
        }

        @Override
        public TtPolygon[] newArray(int size) {
            return new TtPolygon[size];
        }
    };

    private String _Name;
    private DateTime _Time;

    private String _Description;

    private int _IncrementBy;
    private int _PointStartIndex;

    private double _Accuracy;

    private double _Area;
    private double _Perimeter;
    private double _PerimeterLine;


    //region Constructors

    public TtPolygon() {
        this(1010);
    }

    public TtPolygon(Parcel source) {
        super(source);

        _Name = source.readString();
        _Time = (DateTime) source.readSerializable();
        _Description = source.readString();
        _IncrementBy = source.readInt();
        _PointStartIndex = source.readInt();
        _Accuracy = source.readDouble();
        _Area = source.readDouble();
        _Perimeter = source.readDouble();
        _PerimeterLine = source.readDouble();
    }

    public TtPolygon(TtPolygon p) {
        super(p);

        _Name = p.getName();
        _Description = p.getDescription();
        _Accuracy = p.getAccuracy();
        _Area = p.getArea();
        _Perimeter = p.getPerimeter();
        _PerimeterLine = p.getPerimeterLine();
        _IncrementBy = p.getIncrementBy();
        _PointStartIndex = p.getPointStartIndex();
        _Time = p.getTime();
    }

    public TtPolygon(int pointStartIndex) {
        this._Description = StringEx.Empty;
        this._IncrementBy = 10;
        this._PointStartIndex = pointStartIndex;
        this._Accuracy = Consts.Default_Point_Accuracy;
        this._Time = DateTime.now();
    }
    //endregion


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(_Name));
        dest.writeSerializable(_Time);
        dest.writeString(StringEx.getValueOrEmpty(_Description));
        dest.writeInt(_IncrementBy);
        dest.writeInt(_PointStartIndex);
        dest.writeDouble(_Accuracy);
        dest.writeDouble(_Area);
        dest.writeDouble(_Perimeter);
        dest.writeDouble(_PerimeterLine);
    }

    //region Get/Set
    public String getName() {
        return _Name;
    }

    public void setName(String Name) {
        this._Name = Name;
    }


    public UnitType getUnitType() {
        return UnitType.General;
    }


    public String getDescription() {
        return _Description;
    }

    public void setDescription(String Description) {
        this._Description = Description;
    }

    public int getIncrementBy() {
        return _IncrementBy;
    }

    public void setIncrementBy(int IncrementBy) {
        if(IncrementBy < 1)
            throw new IllegalArgumentException("Increment must be greater than 0.");

        this._IncrementBy = IncrementBy;
    }

    public int getPointStartIndex() {
        return _PointStartIndex;
    }

    public void setPointStartIndex(int PointStartIndex) {
        if(PointStartIndex < 1)
            throw new IllegalArgumentException("Start Index must be greater than 0.");

        this._PointStartIndex = PointStartIndex;
    }


    public double getAccuracy() {
        return _Accuracy;
    }

    public void setAccuracy(double PolyAcc) {
        this._Accuracy = PolyAcc;
    }


    public double getArea() {
        return _Area;
    }

    public void setArea(double Area) {
        this._Area = Area;
    }

    public double getPerimeter() {
        return _Perimeter;
    }

    public void setPerimeter(double Perimeter) {
        this._Perimeter = Perimeter;
    }

    public void setPerimeterLine(double PerimeterLine) {
        this._PerimeterLine = PerimeterLine;
    }

    public double getPerimeterLine() {
        return _PerimeterLine;
    }

    public DateTime getTime() {
        return _Time;
    }

    public void setTime(DateTime time) {
        this._Time = time;
    }


    //endregion


    @Override
    public int compareTo(@NonNull TtPolygon another) {
        return compare(this, another);
    }

    @Override
    public int compare(TtPolygon p1, TtPolygon p2) {
        if (p1 == null && p2 == null)
            return 0;

        if (p1 == null)
            return -1;

        if (p2 == null)
            return 1;

        return p1.getTime().compareTo(p2.getTime());
    }

    @Override
    @NonNull
    public String toString() {
        return _Name;
    }
}
