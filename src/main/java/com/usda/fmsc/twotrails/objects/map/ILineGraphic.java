package com.usda.fmsc.twotrails.objects.map;


import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;

public interface ILineGraphic {
    void build(Position point1, Position point2, LineGraphicOptions graphicOptions);

    void update(Position point1, Position point2);

    Extent getExtents();
    Position getPosition();

    void setVisible(boolean visible);
    boolean isVisible();
}
