package com.usda.fmsc.twotrails.objects.map;

public class TrailGraphicOptions {
    private final int TrailColor, PointColor;
    private final float TrailWidth;
    private boolean CloseTrail;

    public TrailGraphicOptions(int trailColor, int pointColor, float trailWidth, boolean closeTrail) {
        TrailColor = trailColor;
        PointColor = pointColor;
        TrailWidth = trailWidth;
        CloseTrail = closeTrail;
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

    public boolean isClosedTrail() {
        return CloseTrail;
    }
}