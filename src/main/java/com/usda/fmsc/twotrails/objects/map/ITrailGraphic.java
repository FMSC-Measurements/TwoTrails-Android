package com.usda.fmsc.twotrails.objects.map;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

import java.util.HashMap;
import java.util.List;

public interface ITrailGraphic {
    void build(List<TtPoint> points, boolean adjusted, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions);

    Position add(TtPoint point, boolean adjusted, HashMap<String, TtMetadata> meta);

    void deleteLastPoint();

    Extent getExtents();
    Position getPosition();


    void setVisible(boolean visible);
    void setMarkersVisible(boolean visible);
    void setTrailVisible(boolean visible);

    boolean isVisible();
    boolean isMarkersVisible();
    boolean isTrailVisible();
}
