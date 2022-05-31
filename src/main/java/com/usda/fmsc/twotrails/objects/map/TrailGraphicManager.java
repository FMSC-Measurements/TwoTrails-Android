package com.usda.fmsc.twotrails.objects.map;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;
import java.util.HashMap;

public class TrailGraphicManager implements IPolygonGraphicManager {
    private final TtPolygon polygon;
    private final ArrayList<TtPoint> points;
    private final ArrayList<Position> positions;
    private final HashMap<String, TtMetadata> meta;
    private final boolean isAdjusted;

    private ITrailGraphic trailGraphic;
    private TrailGraphicOptions graphicOptions;


    public TrailGraphicManager(TtPolygon polygon, ArrayList<TtPoint> points, boolean adjusted, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        this(polygon, points, adjusted, meta, null, graphicOptions);
    }

    public TrailGraphicManager(TtPolygon polygon, ArrayList<TtPoint> points, boolean adjusted, HashMap<String, TtMetadata> meta, ITrailGraphic trailGraphic, TrailGraphicOptions graphicOptions) {
        this.polygon = polygon;
        this.points = points;
        this.meta = meta;
        this.graphicOptions = graphicOptions;
        this.isAdjusted = adjusted;

        positions = new ArrayList<>();

        if (trailGraphic != null) {
            setGraphic(trailGraphic, graphicOptions);
        }
    }

    public void setGraphic(ITrailGraphic trailGraphic) {
        setGraphic(trailGraphic, graphicOptions);
    }

    public void setGraphic(ITrailGraphic trailGraphic, TrailGraphicOptions graphicOptions) {
        this.trailGraphic = trailGraphic;
        this.graphicOptions = graphicOptions;

        this.trailGraphic.build(points, isAdjusted, meta, graphicOptions);
    }


    @Override
    public String getPolygonCN() {
        return polygon.getCN();
    }

    @Override
    public String getPolyName() {
        return polygon.getName();
    }

    @Override
    public String getCN() {
        return getPolygonCN();
    }

    @Override
    public Extent getExtents() {
        return trailGraphic != null ? trailGraphic.getExtents() : null;
    }

    @Override
    public Position getPosition() {
        return trailGraphic.getPosition();
    }

    public ITrailGraphic getTrailGraphic() {
        return trailGraphic;
    }

    public TrailGraphicOptions getGraphicOptions() {
        return graphicOptions;
    }


    public Position getPosition(int index) {
        return positions.get(index);
    }

    public int getPositionsCount() {
        return positions.size();
    }

    public Position addPoint(TtPoint point, boolean adjusted) {
        Position position = null;

        if (trailGraphic != null) {
            position = trailGraphic.add(point, adjusted, meta);

            positions.add(position);
        }

        return position;
    }

    public void removeLastPoint() {
        if (trailGraphic != null && positions.size() > 0) {
            trailGraphic.deleteLastPoint();
            positions.remove(points.size() - 1);
        }
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
