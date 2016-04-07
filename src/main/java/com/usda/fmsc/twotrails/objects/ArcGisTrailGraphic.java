package com.usda.fmsc.twotrails.objects;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;
import java.util.List;

public class ArcGisTrailGraphic implements ITrailGraphic {
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;

    private MapView map;
    private Extent polyBounds;
    private Extent.Builder eBuilder;

    private GraphicsLayer _TrailLayer, _PtsLayer;

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public ArcGisTrailGraphic(MapView mapView) {
        this.map = mapView;
    }


    @Override
    public void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        eBuilder = new Extent.Builder();


        for (TtPoint point : points) {
            addPoint(point, meta);
        }

        polyBounds = eBuilder.build();
    }

    @Override
    public void add(TtPoint point, HashMap<String, TtMetadata> meta) {
        addPoint(point, meta);

        polyBounds = eBuilder.build();
    }

    private void addPoint(TtPoint point, HashMap<String, TtMetadata> meta) {

    }

    @Override
    public void deleteLastPoint() {

    }



    @Override
    public HashMap<String, IMultiMapFragment.MarkerData> getMarkerData() {
        return _MarkerData;
    }

    @Override
    public Extent getExtents() {
        return polyBounds;
    }


    @Override
    public void setVisible(boolean visible) {

    }

    @Override
    public void setMarkersVisible(boolean visible) {

    }

    @Override
    public void setTrailVisible(boolean visible) {

    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isMarkersVisible() {
        return markersVisible;
    }

    @Override
    public boolean isTrailVisible() {
        return trailVisible;
    }
}
