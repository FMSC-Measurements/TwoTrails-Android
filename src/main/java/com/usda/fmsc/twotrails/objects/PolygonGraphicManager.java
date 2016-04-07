package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;
import java.util.List;

public class PolygonGraphicManager implements IGraphicManager, PolygonDrawOptions.Listener {

    TtPolygon polygon;
    List<TtPoint> points;
    HashMap<String, TtMetadata> meta;

    IPolygonGraphic polygonGraphic;
    IPolygonGraphic.PolygonGraphicOptions graphicOptions;

    Extent extents;


    public PolygonGraphicManager(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, IPolygonGraphic.PolygonGraphicOptions graphicOptions) {
        this(polygon, points, meta, null, graphicOptions, null);
    }

    public PolygonGraphicManager(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, IPolygonGraphic polygonGraphic,
                                 IPolygonGraphic.PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
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

    public void setGraphic(IPolygonGraphic polygonGraphic, PolygonDrawOptions drawOptions, IPolygonGraphic.PolygonGraphicOptions graphicOptions) {
        this.polygonGraphic = polygonGraphic;
        this.graphicOptions = graphicOptions;

        if (drawOptions == null)
            drawOptions = new PolygonDrawOptions();

        this.polygonGraphic.build(polygon, points, meta, graphicOptions, drawOptions);

        this.extents = this.polygonGraphic.getExtents();
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
        polygonGraphic.setAdjBndPtsVisible(visible);
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
    //endregion


    //region Getters
    @Override
    public String getId() { return polygon.getCN();}

    public String getPolyName() {
        return polygon.getName();
    }

    public TtPolygon getPolygon() {
        return polygon;
    }

    public PolygonDrawOptions getDrawOptions() {
        return polygonGraphic.getDrawOptions();
    }

    @Override
    public HashMap<String, IMultiMapFragment.MarkerData> getMarkerData() {
        return polygonGraphic.getMarkerData();
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
    //endregion


    @Override
    public Extent getExtents() {
        return extents;
    }

    @Override
    public void onOptionChanged(PolygonDrawOptions.GraphicCode code, boolean value) {

    }
}
