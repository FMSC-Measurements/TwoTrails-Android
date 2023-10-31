package com.usda.fmsc.twotrails.objects.map;

import androidx.annotation.ColorInt;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PolygonGraphicManager implements IPolygonGraphicManager {
    private final ArrayList<PolygonDrawOptions.Listener> polygonDrawListeners = new ArrayList<>();
    private final ArrayList<PolygonGraphicOptions.Listener> polygonGraphicListeners = new ArrayList<>();

    private final TtPolygon polygon;
    private final List<TtPoint> points;
    private final HashMap<String, TtMetadata> meta;

    private IPolygonGraphic polygonGraphic;
    private PolygonGraphicOptions graphicOptions;


    public PolygonGraphicManager(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions) {
        this(polygon, points, meta, null, graphicOptions, null);
    }

    public PolygonGraphicManager(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, IPolygonGraphic polygonGraphic,
                                 PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
        this.polygon = polygon;
        this.points = points;
        this.meta = meta;
        this.graphicOptions = graphicOptions;

        if (polygonGraphic != null) {
            setGraphic(polygonGraphic, drawOptions, graphicOptions);
        }
    }

    public void setGraphic(IPolygonGraphic polygonGraphic) {
        setGraphic(polygonGraphic, null);
    }

    public void setGraphic(IPolygonGraphic polygonGraphic, PolygonDrawOptions drawOptions) {
        if (this.graphicOptions != null) {
            if (this.polygonGraphic != null && drawOptions == null) {
                setGraphic(polygonGraphic, this.polygonGraphic.getDrawOptions(), this.graphicOptions);
            } else {
                setGraphic(polygonGraphic, drawOptions, this.graphicOptions);
            }
        } else {
            throw new RuntimeException("No graphic options set");
        }
    }

    public void setGraphic(IPolygonGraphic polygonGraphic, PolygonDrawOptions drawOptions, PolygonGraphicOptions graphicOptions) {
        this.polygonGraphic = polygonGraphic;
        this.graphicOptions = graphicOptions;

        if (drawOptions == null)
            drawOptions = new PolygonDrawOptions();

        this.polygonGraphic.build(polygon, points, meta, graphicOptions, drawOptions);
    }


    //region Setters
    public void setVisible(boolean visible) {
        polygonGraphic.setVisible(visible);
    }


    public void setAdjBndVisible(boolean visible) {
        polygonGraphic.setAdjBndVisible(visible);
    }

    public void setAdjBndPtsVisible(boolean visible) {
        polygonGraphic.setAdjBndPtsVisible(visible);
    }


    public void setUnadjBndVisible(boolean visible) {
        polygonGraphic.setUnadjBndVisible(visible);
    }

    public void setUnadjBndPtsVisible(boolean visible) {
        polygonGraphic.setUnadjBndPtsVisible(visible);
    }


    public void setAdjNavVisible(boolean visible) {
        polygonGraphic.setAdjNavVisible(visible);
    }

    public void setAdjNavPtsVisible(boolean visible) {
        polygonGraphic.setAdjNavPtsVisible(visible);
    }


    public void setUnadjNavVisible(boolean visible) {
        polygonGraphic.setUnadjNavVisible(visible);
    }

    public void setUnadjNavPtsVisible(boolean visible) {
        polygonGraphic.setUnadjNavPtsVisible(visible);
    }


    public void setAdjMiscPtsVisible(boolean visible) {
        polygonGraphic.setAdjMiscPtsVisible(visible);
    }

    public void setUnadjMiscPtsVisible(boolean visible) {
        polygonGraphic.setUnadjMiscPtsVisible(visible);
    }


    public void setWayPtsVisible(boolean visible) {
        polygonGraphic.setWayPtsVisible(visible);
    }


    public void setAdjBndClose(boolean close) {
        polygonGraphic.setAdjBndClose(close);
    }

    public void setUnadjBndClose(boolean close) {
        polygonGraphic.setUnadjBndClose(close);
    }


    public void setAdjBndColor(@ColorInt int adjBndColor) {
        polygonGraphic.setAdjBndColor(adjBndColor);
    }

    public void setAdjNavColor(@ColorInt int adjNavColor) {
        polygonGraphic.setAdjNavColor(adjNavColor);
    }

    public void setUnAdjBndColor(@ColorInt int unAdjBndColor) {
        polygonGraphic.setUnAdjBndColor(unAdjBndColor);
    }

    public void setUnAdjNavColor(@ColorInt int unAdjNavColor) {
        polygonGraphic.setUnAdjNavColor(unAdjNavColor);
    }

    public void setAdjPtsColor(@ColorInt int adjPtsColor) {
        polygonGraphic.setAdjPtsColor(adjPtsColor);
    }

    public void setUnAdjPtsColor(@ColorInt int unAdjPtsColor) {
        polygonGraphic.setUnAdjPtsColor(unAdjPtsColor);
    }

    public void setWayPtsColor(@ColorInt int wayPtsColor) {
        polygonGraphic.setWayPtsColor(wayPtsColor);
    }
    //endregion


    //region Getters
    @Override
    public String getPolygonCN() { return polygon.getCN();}

    @Override
    public String getCN() {
        return getPolygonCN();
    }

    public String getPolyName() {
        return polygon.getName();
    }

    public TtPolygon getPolygon() {
        return polygon;
    }

    public PolygonDrawOptions getDrawOptions() {
        return polygonGraphic.getDrawOptions();
    }

    public PolygonGraphicOptions getGraphicOptions() {
        return polygonGraphic.getGraphicOptions();
    }


    public boolean isVisible() {
        return polygonGraphic.isVisible();
    }

    public boolean isAdjBndVisible() {
        return polygonGraphic.isAdjBndVisible();
    }

    public boolean isAdjBndPtsVisible() {
        return polygonGraphic.isAdjBndPtsVisible();
    }

    public boolean isUnadjBndVisible() {
        return polygonGraphic.isUnadjBndVisible();
    }

    public boolean isUnadjBndPtsVisible() {
        return polygonGraphic.isUnadjBndPtsVisible();
    }

    public boolean isAdjNavVisible() {
        return polygonGraphic.isAdjNavVisible();
    }

    public boolean isAdjNavPtsVisible() {
        return polygonGraphic.isAdjNavPtsVisible();
    }

    public boolean isUnadjNavVisible() {
        return polygonGraphic.isUnadjNavVisible();
    }

    public boolean isUnadjNavPtsVisible() {
        return polygonGraphic.isUnadjNavPtsVisible();
    }

    public boolean isAdjMiscPtsVisible() {
        return polygonGraphic.isAdjMiscPtsVisible();
    }

    public boolean isUnadjMiscPtsVisible() {
        return polygonGraphic.isUnadjMiscPtsVisible();
    }

    public boolean isWayPtsVisible() {
        return polygonGraphic.isWayPtsVisible();
    }

    public boolean isAdjBndClose() {
        return polygonGraphic.isAdjBndClose();
    }

    public boolean isUnadjBndClose() {
        return polygonGraphic.isUnadjBndClose();
    }


    public int getAdjBndColor() {
        return graphicOptions.getAdjBndColor();
    }
    public int getUnAdjBndColor() {
        return graphicOptions.getUnAdjBndColor();
    }

    public int getAdjNavColor() {
        return graphicOptions.getAdjNavColor();
    }

    public int getUnAdjNavColor() {
        return graphicOptions.getUnAdjNavColor();
    }

    public int getAdjPtsColor() {
        return graphicOptions.getAdjPtsColor();
    }

    public int getUnAdjPtsColor() {
        return graphicOptions.getUnAdjPtsColor();
    }

    public int getWayPtsColor() {
        return graphicOptions.getWayPtsColor();
    }
    //endregion


    @Override
    public Extent getExtents() {
        return polygonGraphic != null ? polygonGraphic.getExtents() : null;
    }

    @Override
    public Position getPosition() {
        return polygonGraphic != null ? polygonGraphic.getPosition() : null;
    }

    public void addPolygonDrawListener(PolygonDrawOptions.Listener polygonDrawListener) {
        polygonDrawListeners.add(polygonDrawListener);
    }
    
    public void removePolygonDrawListener(PolygonDrawOptions.Listener polygonDrawListener) {
        polygonDrawListeners.remove(polygonDrawListener);
    }

    public void addPolygonGraphicListener(PolygonGraphicOptions.Listener polygonGraphicListener) {
        polygonGraphicListeners.add(polygonGraphicListener);
    }

    public void removePolygonGraphicListener(PolygonGraphicOptions.Listener polygonGraphicListener) {
        polygonGraphicListeners.remove(polygonGraphicListener);
    }


    public void update(PolygonDrawOptions.DrawCode code, boolean value) {
        switch (code) {
            case VISIBLE:
                setVisible(value);
                break;
            case ADJBND:
                setAdjBndVisible(value);
                break;
            case UNADJBND:
                setUnadjBndVisible(value);
                break;
            case ADJBNDPTS:
                setAdjBndPtsVisible(value);
                break;
            case UNADJBNDPTS:
                setUnadjBndPtsVisible(value);
                break;
            case ADJBNDCLOSE:
                setAdjBndClose(value);
                break;
            case UNADJBNDCLOSE:
                setUnadjBndClose(value);
                break;
            case ADJNAV:
                setAdjNavVisible(value);
                break;
            case UNADJNAV:
                setUnadjNavVisible(value);
                break;
            case ADJNAVPTS:
                setAdjNavPtsVisible(value);
                break;
            case UNADJNAVPTS:
                setUnadjNavPtsVisible(value);
                break;
            case ADJMISCPTS:
                setAdjMiscPtsVisible(value);
                break;
            case UNADJMISCPTS:
                setUnadjMiscPtsVisible(value);
                break;
            case WAYPTS:
                setWayPtsVisible(value);
                break;
        }

        for (PolygonDrawOptions.Listener listener : polygonDrawListeners) {
            listener.onOptionChanged(code, value);
        }
    }

    public void update(PolygonGraphicOptions.GraphicCode code, int value) {
        graphicOptions.setColor(code, value);

        for (PolygonGraphicOptions.Listener listener : polygonGraphicListeners) {
            listener.onOptionChanged(graphicOptions, code, value);
        }
    }
}
