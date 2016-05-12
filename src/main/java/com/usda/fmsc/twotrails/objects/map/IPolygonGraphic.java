package com.usda.fmsc.twotrails.objects.map;

import android.support.annotation.ColorInt;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.HashMap;
import java.util.List;

public interface IPolygonGraphic {

    void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions);

    TtPolygon getPolygon();

    PolygonDrawOptions getDrawOptions();

    PolygonGraphicOptions getGraphicOptions();

    Extent getExtents();

    void setVisible(boolean visible);

    void setAdjBndVisible(boolean visible);
    void setAdjBndPtsVisible(boolean visible);

    void setUnadjBndVisible(boolean visible);
    void setUnadjBndPtsVisible(boolean visible);

    void setAdjNavVisible(boolean visible);
    void setAdjNavPtsVisible(boolean visible);

    void setUnadjNavVisible(boolean visible);
    void setUnadjNavPtsVisible(boolean visible);

    void setAdjMiscPtsVisible(boolean visible);
    void setUnadjMiscPtsVisible(boolean visible);

    void setWayPtsVisible(boolean visible);

    void setAdjBndClose(boolean close);
    void setUnadjBndClose(boolean close);


    boolean isVisible();

    boolean isAdjBndVisible();
    boolean isAdjBndPtsVisible();

    boolean isUnadjBndVisible();
    boolean isUnadjBndPtsVisible();

    boolean isAdjNavVisible();
    boolean isAdjNavPtsVisible();

    boolean isUnadjNavVisible();
    boolean isUnadjNavPtsVisible();

    boolean isAdjMiscPtsVisible();
    boolean isUnadjMiscPtsVisible();

    boolean isWayPtsVisible();

    boolean isAdjBndClose();
    boolean isUnadjBndClose();



    void setAdjBndColor(@ColorInt int adjBndColor);
    void setUnAdjBndColor(@ColorInt int unAdjBndColor);
    void setAdjNavColor(@ColorInt int adjNavColor);
    void setUnAdjNavColor(@ColorInt int unAdjNavColor);
    void setAdjPtsColor(@ColorInt int adjPtsColor);
    void setUnAdjPtsColor(@ColorInt int unAdjPtsColor);
    void setWayPtsColor(@ColorInt int wayPtsColor);

    int getAdjBndColor();
    int getUnAdjBndColor();
    int getAdjNavColor();
    int getUnAdjNavColor();
    int getAdjPtsColor();
    int getUnAdjPtsColor();
    int getWayPtsColor();
}
