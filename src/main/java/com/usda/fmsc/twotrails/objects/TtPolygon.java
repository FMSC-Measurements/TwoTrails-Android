package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.usda.fmsc.twotrails.Consts;
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

    private String Name;
    private DateTime Time;

    private String Description;

    private int IncrementBy;
    private int PointStartIndex;

    private double Accuracy;

    private double Area;
    private double Perimeter;
    private double PerimeterLine;


    //region Constructors

    public TtPolygon() {
        this(1010);
    }

    public TtPolygon(Parcel source) {
        super(source);

        Name = source.readString();
        Time = (DateTime) source.readSerializable();
        Description = source.readString();
        IncrementBy = source.readInt();
        PointStartIndex = source.readInt();
        Accuracy = source.readDouble();
        Area = source.readDouble();
        Perimeter = source.readDouble();
        PerimeterLine = source.readDouble();
    }

    public TtPolygon(TtPolygon p) {
        super(p);

        Name = p.getName();
        Description = p.getDescription();
        Accuracy = p.getAccuracy();
        Area = p.getArea();
        Perimeter = p.getPerimeter();
        PerimeterLine = p.getPerimeterLine();
        IncrementBy = p.getIncrementBy();
        PointStartIndex = p.getPointStartIndex();
        Time = p.getTime();
    }

    public TtPolygon(int pointStartIndex) {
        this.Description = StringEx.Empty;
        this.IncrementBy = 10;
        this.PointStartIndex = pointStartIndex;
        this.Accuracy = Consts.Default_Point_Accuracy;
        this.Time = DateTime.now();
    }
    //endregion


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(Name));
        dest.writeSerializable(Time);
        dest.writeString(StringEx.getValueOrEmpty(Description));
        dest.writeInt(IncrementBy);
        dest.writeInt(PointStartIndex);
        dest.writeDouble(Accuracy);
        dest.writeDouble(Area);
        dest.writeDouble(Perimeter);
        dest.writeDouble(PerimeterLine);
    }

    //region Get/Set
    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }


    public String getDescription() {
        return Description;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public int getIncrementBy() {
        return IncrementBy;
    }

    public void setIncrementBy(int IncrementBy) {
        if(IncrementBy < 1)
            throw new IllegalArgumentException("Increment must be greater than 0.");

        this.IncrementBy = IncrementBy;
    }

    public int getPointStartIndex() {
        return PointStartIndex;
    }

    public void setPointStartIndex(int PointStartIndex) {
        if(PointStartIndex < 1)
            throw new IllegalArgumentException("Start Index must be greater than 0.");

        this.PointStartIndex = PointStartIndex;
    }


    public double getAccuracy() {
        return Accuracy;
    }

    public void setAccuracy(double PolyAcc) {
        this.Accuracy = PolyAcc;
    }


    public double getArea() {
        return Area;
    }

    public void setArea(double Area) {
        this.Area = Area;
    }

    public double getPerimeter() {
        return Perimeter;
    }

    public void setPerimeter(double Perimeter) {
        this.Perimeter = Perimeter;
    }

    public void setPerimeterLine(double PerimeterLine) {
        this.PerimeterLine = PerimeterLine;
    }

    public double getPerimeterLine() {
        return PerimeterLine;
    }

    public DateTime getTime() {
        return Time;
    }

    public void setTime(DateTime time) {
        this.Time = time;
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
    public String toString() {
        return Name;
    }
}
