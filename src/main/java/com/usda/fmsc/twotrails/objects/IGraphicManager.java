package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;

public interface IGraphicManager {
    String getId();

    Extent getExtents();

    HashMap<String, IMultiMapFragment.MarkerData> getMarkerData();

}
