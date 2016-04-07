package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;
import java.util.List;

public interface ITrailGraphic {
    void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions);

    void add(TtPoint point, HashMap<String, TtMetadata> meta);

    void deleteLastPoint();

    HashMap<String, IMultiMapFragment.MarkerData> getMarkerData();

    Extent getExtents();


    void setVisible(boolean visible);
    void setMarkersVisible(boolean visible);
    void setTrailVisible(boolean visible);

    boolean isVisible();
    boolean isMarkersVisible();
    boolean isTrailVisible();


    class TrailGraphicOptions {
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
}
