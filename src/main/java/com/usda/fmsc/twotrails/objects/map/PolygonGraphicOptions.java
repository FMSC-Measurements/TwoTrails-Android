package com.usda.fmsc.twotrails.objects.map;

import android.support.annotation.ColorInt;

import java.util.ArrayList;

public class PolygonGraphicOptions {
    public enum GraphicCode {
        ADJBND_COLOR,
        ADJNAV_COLOR,
        ADJPTS_COLOR,
        UNADJBND_COLOR,
        UNADJNAV_COLOR,
        UNADJPTS_COLOR,
        WAYPTS_COLOR
    }

    private ArrayList<Listener> listeners = new ArrayList<>();

    private String CN;
    @ColorInt private int AdjBndColor, UnAdjBndColor;
    @ColorInt private int AdjNavColor, UnAdjNavColor;
    @ColorInt private int AdjPtsColor, UnAdjPtsColor, WayPtsColor;
    private float AdjWidth, UnAdjWidth;


    public PolygonGraphicOptions(String CN, PolygonGraphicOptions options) {
        this.CN = CN;
        this.AdjBndColor = options.AdjBndColor;
        this.UnAdjBndColor = options.UnAdjBndColor;
        this.AdjNavColor = options.AdjNavColor;
        this.UnAdjNavColor = options.UnAdjNavColor;
        this.AdjPtsColor = options.AdjPtsColor;
        this.UnAdjPtsColor = options.UnAdjPtsColor;
        this.WayPtsColor = options.WayPtsColor;
        this.AdjWidth = options.AdjWidth;
        this.UnAdjWidth = options.UnAdjWidth;
    }

    public PolygonGraphicOptions(String CN, @ColorInt int AdjBndColor, @ColorInt int UnAdjBndColor, @ColorInt int AdjNavColor, @ColorInt int UnAdjNavColor,
                                 @ColorInt int AdjPtsColor, @ColorInt int UnAdjPtsColor, @ColorInt int WayPtsColor,
                                 float AdjWidth, float UnAdjWidth) {
        this.CN = CN;
        this.AdjBndColor = AdjBndColor;
        this.UnAdjBndColor = UnAdjBndColor;
        this.AdjNavColor = AdjNavColor;
        this.UnAdjNavColor = UnAdjNavColor;
        this.AdjPtsColor = AdjPtsColor;
        this.UnAdjPtsColor = UnAdjPtsColor;
        this.WayPtsColor = WayPtsColor;
        this.AdjWidth = AdjWidth;
        this.UnAdjWidth = UnAdjWidth;
    }


    public String getCN() {
        return CN;
    }

    public int getColor(GraphicCode code) {
        switch (code) {
            case ADJBND_COLOR: return AdjBndColor;
            case ADJNAV_COLOR: return AdjNavColor;
            case ADJPTS_COLOR: return AdjPtsColor;
            case UNADJBND_COLOR: return UnAdjBndColor;
            case UNADJNAV_COLOR: return UnAdjNavColor;
            case UNADJPTS_COLOR: return UnAdjPtsColor;
            case WAYPTS_COLOR: return WayPtsColor;
        }

        return 0;
    }

    public void setColor(GraphicCode color, @ColorInt int value) {
        switch (color) {
            case ADJBND_COLOR:
                setAdjBndColor(value);
                break;
            case ADJNAV_COLOR:
                setAdjNavColor(value);
                break;
            case ADJPTS_COLOR:
                setAdjPtsColor(value);
                break;
            case UNADJBND_COLOR:
                setUnAdjBndColor(value);
                break;
            case UNADJNAV_COLOR:
                setUnAdjNavColor(value);
                break;
            case UNADJPTS_COLOR:
                setUnAdjPtsColor(value);
                break;
            case WAYPTS_COLOR:
                setWayPtsColor(value);
                break;
        }
    }

    @ColorInt
    public int getAdjBndColor() {
        return AdjBndColor;
    }

    public void setAdjBndColor(@ColorInt int adjBndColor) {
        AdjBndColor = adjBndColor;
        onColorChange(GraphicCode.ADJBND_COLOR, adjBndColor);
    }

    @ColorInt
    public int getUnAdjBndColor() {
        return UnAdjBndColor;
    }

    public void setUnAdjBndColor(@ColorInt int unAdjBndColor) {
        UnAdjBndColor = unAdjBndColor;
        onColorChange(GraphicCode.UNADJBND_COLOR, unAdjBndColor);
    }

    @ColorInt
    public int getAdjNavColor() {
        return AdjNavColor;
    }

    public void setAdjNavColor(@ColorInt int adjNavColor) {
        AdjNavColor = adjNavColor;
        onColorChange(GraphicCode.ADJNAV_COLOR, adjNavColor);
    }

    @ColorInt
    public int getUnAdjNavColor() {
        return UnAdjNavColor;
    }

    public void setUnAdjNavColor(@ColorInt int unAdjNavColor) {
        UnAdjNavColor = unAdjNavColor;
        onColorChange(GraphicCode.UNADJNAV_COLOR, unAdjNavColor);
    }

    @ColorInt
    public int getAdjPtsColor() {
        return AdjPtsColor;
    }

    public void setAdjPtsColor(@ColorInt int adjPtsColor) {
        AdjPtsColor = adjPtsColor;
        onColorChange(GraphicCode.ADJPTS_COLOR, adjPtsColor);
    }

    @ColorInt
    public int getUnAdjPtsColor() {
        return UnAdjPtsColor;
    }

    public void setUnAdjPtsColor(@ColorInt int unAdjPtsColor) {
        UnAdjPtsColor = unAdjPtsColor;
        onColorChange(GraphicCode.UNADJPTS_COLOR, unAdjPtsColor);
    }

    @ColorInt
    public int getWayPtsColor() {
        return WayPtsColor;
    }

    public void setWayPtsColor(@ColorInt int wayPtsColor) {
        WayPtsColor = wayPtsColor;
        onColorChange(GraphicCode.WAYPTS_COLOR, wayPtsColor);
    }

    public float getAdjWidth() {
        return AdjWidth;
    }

    public void setAdjWidth(float adjWidth) {
        AdjWidth = adjWidth;
    }

    public float getUnAdjWidth() {
        return UnAdjWidth;
    }

    public void setUnAdjWidth(float unAdjWidth) {
        UnAdjWidth = unAdjWidth;
    }


    private void onColorChange(GraphicCode code, @ColorInt int value) {
        for (Listener listener : listeners) {
            listener.onOptionChanged(this, code, value);
        }
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    public interface Listener {
        void onOptionChanged(PolygonGraphicOptions pgo, GraphicCode code, @ColorInt int value);
    }
}
