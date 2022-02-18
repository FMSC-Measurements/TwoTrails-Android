package com.usda.fmsc.twotrails.objects.map;


import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;

public interface IGraphicManager {
    Position getPosition();
    Extent getExtents();
}
