package com.usda.fmsc.twotrails.objects.map;

public class TrailGraphicOptions {
    private final int TrailColor, PointColor;
    private final float TrailWidth;

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