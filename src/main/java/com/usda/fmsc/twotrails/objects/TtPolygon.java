package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Consts;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Comparator;

public class TtPolygon implements Comparable<TtPolygon>, Comparator<TtPolygon>, Serializable {

    private String CN;
    private String Name;
    private DateTime Time;

    private String Description;

    private int IncrementBy;
    private int PointStartIndex;

    private double Accuracy;

    private double Area;
    private double Perimeter;


    //region Constructors
    public TtPolygon(TtPolygon p) {
        CN = p.getCN();
        Name = p.getName();
        Description = p.getDescription();
        Accuracy = p.getAccuracy();
        Area = p.getArea();
        Perimeter = p.getPerimeter();
        IncrementBy = p.getIncrementBy();
        PointStartIndex = p.getPointStartIndex();
        Time = p.getTime();
    }

    public TtPolygon() {
        this(1010);
    }

    public TtPolygon(int pointStartIndex) {
        this.CN = java.util.UUID.randomUUID().toString();
        this.IncrementBy = 10;
        this.PointStartIndex = pointStartIndex;
        this.Accuracy = Consts.Default_Point_Accuracy;
        this.Time = DateTime.now();
    }
    //endregion

    //region Get/Set
    public String getCN() {
        return CN;
    }

    public void setCN(String CN) {
        this.CN = CN;
    }

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

    public DateTime getTime() {
        return Time;
    }

    public void setTime(DateTime time) {
        this.Time = time;
    }


    //endregion


    @Override
    public int compareTo(TtPolygon another) {
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
