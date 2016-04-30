package com.usda.fmsc.twotrails.objects.map;

public class TrailGraphicOptions {
    private int TrailColor, PointColor;
    private float TrailWidth;

    public TrailGraphicOptions(int trailColor, int pointColor, float trailWidth) {
        TrailColor = trailColor;
        PointColor = pointColor;
        TrailWidth = trailWidth;
    }

    public int getTrailColor() {
        return TrailColor;
    }

    public int getPointColor() {
        return PointColor;
    }

    public float getTrailWidth() {
        return TrailWidth;
    }
}