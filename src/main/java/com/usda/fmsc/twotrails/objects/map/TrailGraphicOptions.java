package com.usda.fmsc.twotrails.objects.map;

public class TrailGraphicOptions {
    private int TrailColor;
    private float TrailWidth;

    public TrailGraphicOptions(int trailColor, float trailWidth) {
        TrailColor = trailColor;
        TrailWidth = trailWidth;
    }

    public int getTrailColor() {
        return TrailColor;
    }

    public float getTrailWidth() {
        return TrailWidth;
    }
}