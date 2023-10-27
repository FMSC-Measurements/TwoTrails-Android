package com.usda.fmsc.twotrails.objects.map;


import com.usda.fmsc.geospatial.gnss.Extent;
import com.usda.fmsc.geospatial.Position;

public interface IGraphicManager {
    Position getPosition();
    Extent getExtents();
    String getCN();
}
