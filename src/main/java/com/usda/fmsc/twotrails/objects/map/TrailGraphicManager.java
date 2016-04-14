package com.usda.fmsc.twotrails.objects.map;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrailGraphicManager implements IGraphicManager {
    private TtPolygon polygon;
    private ArrayList<TtPoint> points;
    private ArrayList<GeoPosition> positions;
    private HashMap<String, TtMetadata> meta;

    private ITrailGraphic trailGraphic;
    private ITrailGraphic.TrailGraphicOptions graphicOptions;


    public TrailGraphicManager(TtPolygon polygon, ArrayList<TtPoint> points, HashMap<String, TtMetadata> meta, ITrailGraphic.TrailGraphicOptions graphicOptions) {
        this(polygon, points, meta, null, graphicOptions);
    }

    public TrailGraphicManager(TtPolygon polygon, ArrayList<TtPoint> points, HashMap<String, TtMetadata> meta, ITrailGraphic trailGraphic, ITrailGraphic.TrailGraphicOptions graphicOptions) {
        this.polygon = polygon;
        this.points = points;
        this.meta = meta;
        this.graphicOptions = graphicOptions;

        positions = new ArrayList<>();

        if (trailGraphic != null) {
            setGraphic(trailGraphic, graphicOptions);
        }
    }

    public void setGraphic(ITrailGraphic trailGraphic) {
        setGraphic(trailGraphic, graphicOptions);
    }

    public void setGraphic(ITrailGraphic trailGraphic, ITrailGraphic.TrailGraphicOptions graphicOptions) {
        this.trailGraphic = trailGraphic;
        this.graphicOptions = graphicOptions;

        this.trailGraphic.build(points, meta, graphicOptions);
    }


    @Override
    public String getPolygonCN() {
        return polygon.getCN();
    }

    @Override
    public Extent getExtents() {
        return trailGraphic.getExtents();
    }


    public ITrailGraphic getTrailGraphic() {
        return trailGraphic;
    }

    public ITrailGraphic.TrailGraphicOptions getGraphicOptions() {
        return graphicOptions;
    }


    public GeoPosition getPosition(int index) {
        return positions.get(index);
    }

    public int getPositionsCount() {
        return positions.size();
    }

    public GeoPosition addPoint(TtPoint point) {
        GeoPosition position = null;

        if (trailGraphic != null) {
            position = trailGraphic.add(point, meta);

            positions.add(position);
        }

        return position;
    }


    public void setVisible(boolean visible) {
        trailGraphic.setVisible(visible);
    }

    public void setMarkersVisible(boolean visible) {
        trailGraphic.setMarkersVisible(visible);
    }

    public void setTrailVisible(boolean visible) {
        trailGraphic.setTrailVisible(visible);
    }

    public boolean isVisible() {
        return trailGraphic.isVisible();
    }

    public boolean isMarkersVisible() {
        return trailGraphic.isMarkersVisible();
    }

    public boolean isTrailVisible() {
        return trailGraphic.isTrailVisible();
    }
}
