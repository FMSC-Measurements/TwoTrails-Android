package com.usda.fmsc.twotrails.objects.map;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;

import java.util.HashMap;
import java.util.List;

public interface ITrailGraphic {
    void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions);

    GeoPosition add(TtPoint point, HashMap<String, TtMetadata> meta);

    void deleteLastPoint();

    Extent getExtents();


    void setVisible(boolean visible);
    void setMarkersVisible(boolean visible);
    void setTrailVisible(boolean visible);

    boolean isVisible();
    boolean isMarkersVisible();
    boolean isTrailVisible();
}