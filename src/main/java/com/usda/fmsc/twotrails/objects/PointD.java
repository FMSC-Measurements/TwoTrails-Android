package com.usda.fmsc.twotrails.objects;

public class PointD {
    public double X;
    public double Y;

    public PointD(double X, double Y){
        this.X = X;
        this.Y = Y;
    }

    public PointD(PointD point){
        this.X = point.X;
        this.Y = point.Y;
    }

    public final void set(double X, double Y){
        this.X = X;
        this.X = Y;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() == PointD.class) {
            PointD oPoint = (PointD)o;

            return this.X == oPoint.X && this.Y == oPoint.Y;
        }

        return false;
    }
}
