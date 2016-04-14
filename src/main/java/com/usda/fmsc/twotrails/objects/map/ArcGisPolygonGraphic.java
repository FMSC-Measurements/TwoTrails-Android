package com.usda.fmsc.twotrails.objects.map;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;

public class ArcGisPolygonGraphic implements IPolygonGraphic, IMarkerDataGraphic {
    private TtPolygon polygon;
    private PolygonDrawOptions drawOptions;
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;

    private MapView map;
    private Extent polyBounds;

    private GraphicsLayer _AdjBndPts, _UnadjBndPts, _AdjNavPts, _UnadjNavPts, _WayPts, _AdjMiscPts, _UnadjMiscPts;
    private GraphicsLayer _AdjBndCB, _UnadjBndCB;
    private GraphicsLayer _AdjBnd, _UnadjBnd, _AdjNav, _UnadjNav;

    public ArcGisPolygonGraphic(MapView map) {
        this.map = map;
    }

    @Override
    public void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
        this.polygon = polygon;
        this.drawOptions = drawOptions;

        _MarkerData = new HashMap<>();

        _AdjBndCB = new GraphicsLayer();
        _UnadjBndCB = new GraphicsLayer();
        _AdjBnd = new GraphicsLayer();
        _UnadjBnd = new GraphicsLayer();
        _AdjNav = new GraphicsLayer();
        _UnadjNav = new GraphicsLayer();

        _AdjBndPts = new GraphicsLayer();
        _AdjNavPts = new GraphicsLayer();
        _UnadjBndPts = new GraphicsLayer();
        _UnadjNavPts = new GraphicsLayer();
        _WayPts = new GraphicsLayer();
        _AdjMiscPts = new GraphicsLayer();
        _UnadjMiscPts = new GraphicsLayer();

        Extent.Builder llBuilder = new Extent.Builder();

        Polygon adjBndPO = null;
        Polygon unadjBndPO = null;

        Polyline adjBndPLO = null;
        Polyline unadjBndPLO = null;
        Polyline adjNavPLO = null;
        Polyline unadjNavPLO = null;

        SimpleMarkerSymbol adjMkOpts = new SimpleMarkerSymbol(graphicOptions.getAdjBndColor(), 20, SimpleMarkerSymbol.STYLE.DIAMOND);
        SimpleMarkerSymbol unAdjMkOpts = new SimpleMarkerSymbol(graphicOptions.getUnAdjBndColor(), 20, SimpleMarkerSymbol.STYLE.SQUARE);

        GeoPosition adjPos, unAdjPos;
        Point adjLL, unadjLL;
        Graphic adjmk, unadjmk;

        TtMetadata metadata;

        IMultiMapFragment.MarkerData adjMd, unadjMd;

        for (TtPoint point : points) {
            metadata = meta.get(point.getMetadataCN());

            adjPos = TtUtils.getLatLonFromPoint(point, true, metadata);
            adjLL = ArcGISTools.latLngToMapSpatial(adjPos.getLatitudeSignedDecimal(), adjPos.getLongitudeSignedDecimal(), map);

            unAdjPos = TtUtils.getLatLonFromPoint(point, false, metadata);
            unadjLL = ArcGISTools.latLngToMapSpatial(unAdjPos.getLatitudeSignedDecimal(), unAdjPos.getLongitudeSignedDecimal(), map);

            adjmk = new Graphic(adjLL, adjMkOpts);
            unadjmk = new Graphic(adjLL, unAdjMkOpts);

            adjMd = new IMultiMapFragment.MarkerData(point, metadata, true);
            unadjMd = new IMultiMapFragment.MarkerData(point, metadata, false);

            if (point.isBndPoint()) {
                _MarkerData.put(Integer.toHexString(_AdjBndPts.addGraphic(adjmk)), adjMd);
                _MarkerData.put(Integer.toHexString(_UnadjBndPts.addGraphic(unadjmk)), unadjMd);


                if (adjBndPO == null) {
                    adjBndPO = new Polygon();
                    unadjBndPO = new Polygon();
                    adjBndPLO = new Polyline ();
                    unadjBndPLO = new Polyline ();

                    adjBndPO.startPath(adjLL);
                    unadjBndPO.startPath(unadjLL);

                    adjBndPLO.startPath(adjLL);
                    unadjBndPLO.startPath(unadjLL);
                } else {
                    adjBndPO.lineTo(adjLL);
                    unadjBndPO.lineTo(unadjLL);

                    adjBndPLO.lineTo(adjLL);
                    unadjBndPLO.lineTo(unadjLL);
                }

            }

            if (point.isNavPoint()) {
                _MarkerData.put(Integer.toHexString(_AdjNavPts.addGraphic(adjmk)), adjMd);
                _MarkerData.put(Integer.toHexString(_UnadjNavPts.addGraphic(unadjmk)), unadjMd);

                if (adjNavPLO == null) {
                    adjNavPLO = new Polyline();
                    unadjNavPLO = new Polyline();

                    adjNavPLO.startPath(adjLL);
                    unadjNavPLO.startPath(unadjLL);
                } else {
                    adjNavPLO.lineTo(adjLL);
                    unadjNavPLO.lineTo(unadjLL);
                }
            }

            if (point.getOp() == Units.OpType.WayPoint) {
                _MarkerData.put(Integer.toHexString(_WayPts.addGraphic(unadjmk)), unadjMd);
            }

            if (point.getOp() == Units.OpType.SideShot && !point.isOnBnd()) {
                _MarkerData.put(Integer.toHexString(_AdjMiscPts.addGraphic(adjmk)), adjMd);
                _MarkerData.put(Integer.toHexString(_UnadjMiscPts.addGraphic(unadjmk)), unadjMd);
            }
            
            llBuilder.include(adjPos);
        }

        if (points.size() > 0) {
            polyBounds = llBuilder.build();
        } else {
            polyBounds = null;
        }

        SimpleLineSymbol outline = new SimpleLineSymbol(graphicOptions.getAdjBndColor(), graphicOptions.getAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        //SimpleFillSymbol fill = new SimpleFillSymbol(Color.TRANSPARENT, SimpleFillSymbol.STYLE.NULL);
        //fill.setOutline(outline);
        _AdjBnd.addGraphic(new Graphic(adjBndPLO, outline));
        _AdjBndCB.addGraphic(new Graphic(adjBndPO, outline));

        
        outline = new SimpleLineSymbol(graphicOptions.getUnAdjBndColor(), graphicOptions.getUnAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _UnadjBnd.addGraphic(new Graphic(unadjBndPLO, outline));
        _UnadjBndCB.addGraphic(new Graphic(unadjBndPO, outline));

        
        outline = new SimpleLineSymbol(graphicOptions.getAdjNavColor(), graphicOptions.getAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _AdjNav.addGraphic(new Graphic(adjNavPLO, outline));


        outline = new SimpleLineSymbol(graphicOptions.getUnAdjNavColor(), graphicOptions.getUnAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _UnadjNav.addGraphic(new Graphic(unadjNavPLO, outline));

        if (drawOptions.Visible) {
            if (drawOptions.AdjBnd) {
                if (drawOptions.AdjBndClose) {
                    _AdjBnd.setVisible(false);
                } else {
                    _AdjBndCB.setVisible(false);
                }
            } else {
                _AdjBnd.setVisible(false);
                _AdjBndCB.setVisible(false);
            }

            if (drawOptions.UnadjBnd) {
                if (drawOptions.UnadjBndClose) {
                    _UnadjBnd.setVisible(false);
                } else {
                    _UnadjBndCB.setVisible(false);
                }
            } else {
                _UnadjBnd.setVisible(false);
                _UnadjBndCB.setVisible(false);
            }

            if (!drawOptions.AdjNav) {
                _AdjNav.setVisible(false);
            }

            if (!drawOptions.UnadjNav) {
                _UnadjNav.setVisible(false);
            }

            if (!drawOptions.AdjBndPts) {
                _AdjBndPts.setVisible(false);
            }

            if (!drawOptions.UnadjBndPts) {
                _UnadjBndPts.setVisible(false);
            }
            
            if (!drawOptions.AdjMiscPts) {
                _AdjMiscPts.setVisible(false);
            }

            if (!drawOptions.UnadjMiscPts) {
                _UnadjMiscPts.setVisible(false);
            }
            
            if (!drawOptions.WayPts) {
                _WayPts.setVisible(false);
            }
        } else {
            _AdjBnd.setVisible(false);
            _AdjBndCB.setVisible(false);
            
            _UnadjBnd.setVisible(false);
            _UnadjBndCB.setVisible(false);

            _AdjNav.setVisible(false);
            _UnadjNav.setVisible(false);

            _AdjBndPts.setVisible(false);
            _UnadjBndPts.setVisible(false);

            _AdjMiscPts.setVisible(false);
            _UnadjMiscPts.setVisible(false);

            _WayPts.setVisible(false);
        }

        map.addLayer(_AdjBnd);
        map.addLayer(_AdjBndCB);

        map.addLayer(_UnadjBnd);
        map.addLayer(_UnadjBndCB);

        map.addLayer(_AdjNav);
        map.addLayer(_UnadjNav);

        map.addLayer(_AdjBndPts);
        map.addLayer(_UnadjBndPts);

        map.addLayer(_AdjMiscPts);
        map.addLayer(_UnadjMiscPts);

        map.addLayer(_WayPts);
    }

    @Override
    public TtPolygon getPolygon() {
        return polygon;
    }

    @Override
    public PolygonDrawOptions getDrawOptions() {
        return drawOptions;
    }

    @Override
    public HashMap<String, IMultiMapFragment.MarkerData> getMarkerData() {
        return _MarkerData;
    }

    @Override
    public Extent getExtents() {
        return polyBounds;
    }


    //region Get Layers
    public GraphicsLayer getAdjBndPtsLayer() {
        return _AdjBndPts;
    }

    public GraphicsLayer getUnadjBndPtsLayer() {
        return _UnadjBndPts;
    }

    public GraphicsLayer getAdjNavPtsLayer() {
        return _AdjNavPts;
    }

    public GraphicsLayer getUnadjNavPtsLayer() {
        return _UnadjNavPts;
    }

    public GraphicsLayer getWayPtsLayer() {
        return _WayPts;
    }

    public GraphicsLayer getAdjMiscPtsLayer() {
        return _AdjMiscPts;
    }

    public GraphicsLayer getUnadjMiscPtsLayer() {
        return _UnadjMiscPts;
    }

    public GraphicsLayer getAdjBndCBLayer() {
        return _AdjBndCB;
    }

    public GraphicsLayer getUnadjBndCBLayer() {
        return _UnadjBndCB;
    }

    public GraphicsLayer getAdjBndLayer() {
        return _AdjBnd;
    }

    public GraphicsLayer getUnadjBndLayer() {
        return _UnadjBnd;
    }

    public GraphicsLayer getAdjNavLayer() {
        return _AdjNav;
    }
    //endregion

    //region Setters
    @Override
    public void setVisible(boolean visible) {
        drawOptions.Visible = visible;

        if (drawOptions.AdjBnd) {
            if (drawOptions.AdjBndClose)
                _AdjBndCB.setVisible(visible);
            else
                _AdjBnd.setVisible(visible);
        }

        if (drawOptions.AdjBndPts)
            _AdjBndPts.setVisible(visible);

        if (drawOptions.UnadjBnd) {
            if (drawOptions.UnadjBndClose)
                _UnadjBndCB.setVisible(visible);
            else
                _UnadjBnd.setVisible(visible);
        }

        if (drawOptions.UnadjBndPts)
            _UnadjBndPts.setVisible(visible);

        if (drawOptions.AdjNav)
            _AdjNav.setVisible(visible);

        if (drawOptions.AdjNavPts)
            _AdjNavPts.setVisible(visible);

        if (drawOptions.UnadjNav)
            _UnadjNav.setVisible(visible);

        if (drawOptions.UnadjNavPts)
            _UnadjNavPts.setVisible(visible);

        if (drawOptions.AdjMiscPts)
            _AdjMiscPts.setVisible(visible);

        if (drawOptions.UnadjMiscPts)
            _UnadjMiscPts.setVisible(visible);

        if (drawOptions.WayPts)
            _WayPts.setVisible(visible);
    }

    @Override
    public void setAdjBndVisible(boolean visible) {
        drawOptions.AdjBnd = visible;
        visible &= drawOptions.Visible;
        
        if (drawOptions.AdjBndClose)
            _AdjBndCB.setVisible(visible);
        else
            _AdjBnd.setVisible(visible);
    }

    @Override
    public void setAdjBndPtsVisible(boolean visible) {
        drawOptions.AdjBndPts = visible;
        visible &= drawOptions.Visible;

        _AdjBndPts.setVisible(visible);
    }

    @Override
    public void setUnadjBndVisible(boolean visible) {
        drawOptions.UnadjBnd = visible;
        visible &= drawOptions.Visible;

        if (drawOptions.UnadjBndClose)
            _UnadjBndCB.setVisible(visible);
        else
            _UnadjBnd.setVisible(visible);
    }

    @Override
    public void setUnadjBndPtsVisible(boolean visible) {
        drawOptions.UnadjBndPts = visible;
        visible &= drawOptions.Visible;

        _UnadjBndPts.setVisible(visible);
    }

    @Override
    public void setAdjNavVisible(boolean visible) {
        drawOptions.AdjNav = visible;
        visible &= drawOptions.Visible;

        _AdjNav.setVisible(visible);
    }

    @Override
    public void setAdjNavPtsVisible(boolean visible) {
        drawOptions.AdjNavPts = visible;
        visible &= drawOptions.Visible;

        _AdjNavPts.setVisible(visible);
    }

    @Override
    public void setUnadjNavVisible(boolean visible) {
        drawOptions.UnadjNav = visible;
        visible &= drawOptions.Visible;

        _UnadjNav.setVisible(visible);
    }

    @Override
    public void setUnadjNavPtsVisible(boolean visible) {
        drawOptions.UnadjNavPts = visible;
        visible &= drawOptions.Visible;

        _UnadjNavPts.setVisible(visible);
    }

    @Override
    public void setAdjMiscPtsVisible(boolean visible) {
        drawOptions.AdjMiscPts = visible;
        visible &= drawOptions.Visible;

        _AdjMiscPts.setVisible(visible);
    }

    @Override
    public void setUnadjMiscPtsVisible(boolean visible) {
        drawOptions.UnadjMiscPts = visible;
        visible &= drawOptions.Visible;

        _UnadjMiscPts.setVisible(visible);
    }

    @Override
    public void setWayPtsVisible(boolean visible) {
        drawOptions.WayPts = visible;
        visible &= drawOptions.Visible;

        _WayPts.setVisible(visible);
    }

    @Override
    public void setAdjBndClose(boolean close) {
        drawOptions.AdjBndClose = close;
        
        if (drawOptions.Visible) {
            if (drawOptions.AdjBndClose) {
                _AdjBndCB.setVisible(true);
                _AdjBnd.setVisible(false);
            } else {
                _AdjBndCB.setVisible(false);
                _AdjBnd.setVisible(true);
            }
        }
    }

    @Override
    public void setUnadjBndClose(boolean close) {
        drawOptions.UnadjBndClose = close;

        if (drawOptions.Visible) {
            if (drawOptions.UnadjBndClose) {
                _UnadjBndCB.setVisible(true);
                _UnadjBnd.setVisible(false);
            } else {
                _UnadjBndCB.setVisible(false);
                _UnadjBnd.setVisible(true);
            }
        }
    }
    //endregion

    //region Getters
    @Override
    public boolean isVisible() {
        return drawOptions.Visible;
    }

    @Override
    public boolean isAdjBndVisible() {
        return drawOptions.AdjBnd;
    }

    @Override
    public boolean isAdjBndPtsVisible() {
        return drawOptions.AdjBndPts;
    }

    @Override
    public boolean isUnadjBndVisible() {
        return drawOptions.UnadjBnd;
    }

    @Override
    public boolean isUnadjBndPtsVisible() {
        return drawOptions.UnadjBndPts;
    }

    @Override
    public boolean isAdjNavVisible() {
        return drawOptions.AdjNav;
    }

    @Override
    public boolean isAdjNavPtsVisible() {
        return drawOptions.AdjNavPts;
    }

    @Override
    public boolean isUnadjNavVisible() {
        return drawOptions.UnadjNav;
    }

    @Override
    public boolean isUnadjNavPtsVisible() {
        return drawOptions.UnadjNavPts;
    }

    @Override
    public boolean isAdjMiscPtsVisible() {
        return drawOptions.AdjMiscPts;
    }

    @Override
    public boolean isUnadjMiscPtsVisible() {
        return drawOptions.UnadjMiscPts;
    }

    @Override
    public boolean isWayPtsVisible() {
        return drawOptions.WayPts;
    }

    @Override
    public boolean isAdjBndClose() {
        return drawOptions.AdjBndClose;
    }

    @Override
    public boolean isUnadjBndClose() {
        return drawOptions.UnadjBndClose;
    }
    //endregion
}
