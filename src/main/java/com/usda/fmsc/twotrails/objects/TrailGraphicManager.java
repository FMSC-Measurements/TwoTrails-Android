package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;

public class TrailGraphicManager implements IGraphicManager {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public Extent getExtents() {
        return null;
    }

    @Override
    public HashMap<String, IMultiMapFragment.MarkerData> getMarkerData() {
        return null;
    }

    public void setGraphic(ITrailGraphic graphic) {

    }
}
