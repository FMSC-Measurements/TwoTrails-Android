package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;
import java.util.List;

public interface IPolygonGraphic {

    void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions);

    TtPolygon getPolygon();

    PolygonDrawOptions getDrawOptions();

    HashMap<String, IMultiMapFragment.MarkerData> getMarkerData();

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


    class PolygonGraphicOptions {
        private int AdjBndColor, UnAdjBndColor;
        private int AdjNavColor, UnAdjNavColor;
        private float AdjWidth, UnAdjWidth;

        public PolygonGraphicOptions(int AdjBndColor, int UnAdjBndColor, int AdjNavColor, int UnAdjNavColor, float AdjWidth, float UnAdjWidth) {
            this.AdjBndColor = AdjBndColor;
            this.UnAdjBndColor = UnAdjBndColor;
            this.AdjNavColor = AdjNavColor;
            this.UnAdjNavColor = UnAdjNavColor;
            this.AdjWidth = AdjWidth;
            this.UnAdjWidth = UnAdjWidth;
        }

        public int getAdjBndColor() {
            return AdjBndColor;
        }

        public int getUnAdjBndColor() {
            return UnAdjBndColor;
        }

        public int getAdjNavColor() {
            return AdjNavColor;
        }

        public int getUnAdjNavColor() {
            return UnAdjNavColor;
        }

        public float getAdjWidth() {
            return AdjWidth;
        }

        public float getUnAdjWidth() {
            return UnAdjWidth;
        }
    }
}
