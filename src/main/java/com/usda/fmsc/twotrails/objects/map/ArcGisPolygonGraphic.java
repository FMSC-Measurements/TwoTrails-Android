package com.usda.fmsc.twotrails.objects.map;

import android.support.annotation.ColorInt;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;

public class ArcGisPolygonGraphic implements IPolygonGraphic, IMarkerDataGraphic {
    private TtPolygon polygon;
    private PolygonDrawOptions drawOptions;
    private PolygonGraphicOptions graphicOptions;
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
        this.graphicOptions = graphicOptions;

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

        SimpleMarkerSymbol adjMkOpts = new SimpleMarkerSymbol(
                graphicOptions.getAdjPtsColor(),
                (int)graphicOptions.getUnAdjWidth(),
                SimpleMarkerSymbol.STYLE.DIAMOND
        );

        SimpleMarkerSymbol unAdjMkOpts = new SimpleMarkerSymbol(
                graphicOptions.getUnAdjPtsColor(),
                (int)graphicOptions.getUnAdjWidth(),
                SimpleMarkerSymbol.STYLE.SQUARE
        );

        GeoPosition adjPos, unAdjPos;
        Point adjLL, unadjLL;
        Graphic adjmk, unadjmk;

        TtMetadata metadata;

        IMultiMapFragment.MarkerData adjMd, unadjMd;

        for (TtPoint point : points) {
            metadata = meta.get(point.getMetadataCN());

            adjPos = TtUtils.Points.getLatLonFromPoint(point, true, metadata);
            adjLL = ArcGISTools.latLngToMapSpatial(adjPos.getLatitudeSignedDecimal(), adjPos.getLongitudeSignedDecimal(), map);

            unAdjPos = TtUtils.Points.getLatLonFromPoint(point, false, metadata);
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

            if (point.getOp() == OpType.WayPoint) {
                _MarkerData.put(Integer.toHexString(_WayPts.addGraphic(unadjmk)), unadjMd);
            }

            if (point.getOp() == OpType.SideShot && !point.isOnBnd()) {
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
        _AdjBnd.addGraphic(new Graphic(adjBndPLO, outline));
        _AdjBndCB.addGraphic(new Graphic(adjBndPO, outline));

        outline = new SimpleLineSymbol(graphicOptions.getUnAdjBndColor(), graphicOptions.getUnAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _UnadjBnd.addGraphic(new Graphic(unadjBndPLO, outline));
        _UnadjBndCB.addGraphic(new Graphic(unadjBndPO, outline));

        outline = new SimpleLineSymbol(graphicOptions.getAdjNavColor(), graphicOptions.getAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _AdjNav.addGraphic(new Graphic(adjNavPLO, outline));


        outline = new SimpleLineSymbol(graphicOptions.getUnAdjNavColor(), graphicOptions.getUnAdjWidth(), SimpleLineSymbol.STYLE.SOLID);
        _UnadjNav.addGraphic(new Graphic(unadjNavPLO, outline));

        if (drawOptions.isVisible()) {
            if (drawOptions.isAdjBnd()) {
                if (drawOptions.isAdjBndClose()) {
                    _AdjBnd.setVisible(false);
                } else {
                    _AdjBndCB.setVisible(false);
                }
            } else {
                _AdjBnd.setVisible(false);
                _AdjBndCB.setVisible(false);
            }

            if (drawOptions.isUnadjBnd()) {
                if (drawOptions.isUnadjBndClose()) {
                    _UnadjBnd.setVisible(false);
                } else {
                    _UnadjBndCB.setVisible(false);
                }
            } else {
                _UnadjBnd.setVisible(false);
                _UnadjBndCB.setVisible(false);
            }

            if (!drawOptions.isAdjNav()) {
                _AdjNav.setVisible(false);
            }

            if (!drawOptions.isUnadjNav()) {
                _UnadjNav.setVisible(false);
            }

            if (!drawOptions.isAdjBndPts()) {
                _AdjBndPts.setVisible(false);
            }

            if (!drawOptions.isUnadjBndPts()) {
                _UnadjBndPts.setVisible(false);
            }

            if (!drawOptions.isAdjNavPts()) {
                _AdjNavPts.setVisible(false);
            }

            if (!drawOptions.isUnadjNavPts()) {
                _UnadjNavPts.setVisible(false);
            }
            
            if (!drawOptions.isAdjMiscPts()) {
                _AdjMiscPts.setVisible(false);
            }

            if (!drawOptions.isUnadjMiscPts()) {
                _UnadjMiscPts.setVisible(false);
            }
            
            if (!drawOptions.isWayPts()) {
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
            
            _AdjNavPts.setVisible(false);
            _UnadjNavPts.setVisible(false);

            _AdjMiscPts.setVisible(false);
            _UnadjMiscPts.setVisible(false);

            _WayPts.setVisible(false);
        }

        map.addLayer(_UnadjBnd);
        map.addLayer(_UnadjBndCB);

        map.addLayer(_AdjBnd);
        map.addLayer(_AdjBndCB);

        map.addLayer(_UnadjNav);
        map.addLayer(_AdjNav);

        map.addLayer(_UnadjBndPts);
        map.addLayer(_AdjBndPts);

        map.addLayer(_UnadjNavPts);
        map.addLayer(_AdjNavPts);

        map.addLayer(_UnadjMiscPts);
        map.addLayer(_AdjMiscPts);

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

    @Override
    public PolygonGraphicOptions getGraphicOptions() {
        return graphicOptions;
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
        drawOptions.setVisible(visible);

        if (drawOptions.isAdjBnd()) {
            if (drawOptions.isAdjBndClose())
                _AdjBndCB.setVisible(visible);
            else
                _AdjBnd.setVisible(visible);
        }

        if (drawOptions.isAdjBndPts())
            _AdjBndPts.setVisible(visible);

        if (drawOptions.isUnadjBnd()) {
            if (drawOptions.isUnadjBndClose())
                _UnadjBndCB.setVisible(visible);
            else
                _UnadjBnd.setVisible(visible);
        }

        if (drawOptions.isUnadjBndPts())
            _UnadjBndPts.setVisible(visible);

        if (drawOptions.isAdjNav())
            _AdjNav.setVisible(visible);

        if (drawOptions.isAdjNavPts())
            _AdjNavPts.setVisible(visible);

        if (drawOptions.isUnadjNav())
            _UnadjNav.setVisible(visible);

        if (drawOptions.isUnadjNavPts())
            _UnadjNavPts.setVisible(visible);

        if (drawOptions.isAdjMiscPts())
            _AdjMiscPts.setVisible(visible);

        if (drawOptions.isUnadjMiscPts())
            _UnadjMiscPts.setVisible(visible);

        if (drawOptions.isWayPts())
            _WayPts.setVisible(visible);
    }

    @Override
    public void setAdjBndVisible(boolean visible) {
        drawOptions.setAdjBnd(visible);
        visible &= drawOptions.isVisible();
        
        if (drawOptions.isAdjBndClose())
            _AdjBndCB.setVisible(visible);
        else
            _AdjBnd.setVisible(visible);
    }

    @Override
    public void setAdjBndPtsVisible(boolean visible) {
        drawOptions.setAdjBndPts(visible);
        visible &= drawOptions.isVisible();

        _AdjBndPts.setVisible(visible);
    }

    @Override
    public void setUnadjBndVisible(boolean visible) {
        drawOptions.setUnadjBnd(visible);
        visible &= drawOptions.isVisible();

        if (drawOptions.isUnadjBndClose())
            _UnadjBndCB.setVisible(visible);
        else
            _UnadjBnd.setVisible(visible);
    }

    @Override
    public void setUnadjBndPtsVisible(boolean visible) {
        drawOptions.setUnadjBndPts(visible);
        visible &= drawOptions.isVisible();

        _UnadjBndPts.setVisible(visible);
    }

    @Override
    public void setAdjNavVisible(boolean visible) {
        drawOptions.setAdjNav(visible);
        visible &= drawOptions.isVisible();

        _AdjNav.setVisible(visible);
    }

    @Override
    public void setAdjNavPtsVisible(boolean visible) {
        drawOptions.setAdjNavPts(visible);
        visible &= drawOptions.isVisible();

        _AdjNavPts.setVisible(visible);
    }

    @Override
    public void setUnadjNavVisible(boolean visible) {
        drawOptions.setUnadjNav(visible);
        visible &= drawOptions.isVisible();

        _UnadjNav.setVisible(visible);
    }

    @Override
    public void setUnadjNavPtsVisible(boolean visible) {
        drawOptions.setUnadjNavPts(visible);
        visible &= drawOptions.isVisible();

        _UnadjNavPts.setVisible(visible);
    }

    @Override
    public void setAdjMiscPtsVisible(boolean visible) {
        drawOptions.setAdjMiscPts(visible);
        visible &= drawOptions.isVisible();

        _AdjMiscPts.setVisible(visible);
    }

    @Override
    public void setUnadjMiscPtsVisible(boolean visible) {
        drawOptions.setUnadjMiscPts(visible);
        visible &= drawOptions.isVisible();

        _UnadjMiscPts.setVisible(visible);
    }

    @Override
    public void setWayPtsVisible(boolean visible) {
        drawOptions.setWayPts(visible);
        visible &= drawOptions.isVisible();

        _WayPts.setVisible(visible);
    }

    @Override
    public void setAdjBndClose(boolean close) {
        drawOptions.setAdjBndClose(close);
        
        if (drawOptions.isVisible()) {
            if (drawOptions.isAdjBndClose()) {
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
        drawOptions.setUnadjBndClose(close);

        if (drawOptions.isVisible()) {
            if (drawOptions.isUnadjBndClose()) {
                _UnadjBndCB.setVisible(true);
                _UnadjBnd.setVisible(false);
            } else {
                _UnadjBndCB.setVisible(false);
                _UnadjBnd.setVisible(true);
            }
        }
    }


    @Override
    public void setAdjBndColor(@ColorInt int adjBndColor) {
        graphicOptions.setAdjBndColor(adjBndColor);

        setLineColor(adjBndColor, _AdjBnd);
        setLineColor(adjBndColor, _AdjBndCB);
    }

    @Override
    public void setUnAdjBndColor(@ColorInt int unAdjBndColor) {
        graphicOptions.setUnAdjBndColor(unAdjBndColor);

        setLineColor(unAdjBndColor, _UnadjBnd);
        setLineColor(unAdjBndColor, _UnadjBndCB);
    }

    @Override
    public void setAdjNavColor(@ColorInt int adjNavColor) {
        graphicOptions.setAdjNavColor(adjNavColor);

        setLineColor(adjNavColor, _AdjNav);
    }

    @Override
    public void setUnAdjNavColor(@ColorInt int unAdjNavColor) {
        graphicOptions.setUnAdjNavColor(unAdjNavColor);

        setLineColor(unAdjNavColor, _UnadjNav);
    }

    @Override
    public void setAdjPtsColor(@ColorInt int adjPtsColor) {
        graphicOptions.setAdjPtsColor(adjPtsColor);

        setPtsColor(adjPtsColor, _AdjBndPts);
        setPtsColor(adjPtsColor, _AdjNavPts);
    }

    @Override
    public void setUnAdjPtsColor(@ColorInt int unAdjPtsColor) {
        graphicOptions.setUnAdjPtsColor(unAdjPtsColor);

        setPtsColor(unAdjPtsColor, _UnadjBndPts);
        setPtsColor(unAdjPtsColor, _UnadjNavPts);
    }

    @Override
    public void setWayPtsColor(@ColorInt int wayPtsColor) {
        graphicOptions.setWayPtsColor(wayPtsColor);

        setPtsColor(wayPtsColor, _WayPts);
    }


    private void setLineColor(@ColorInt int color, GraphicsLayer graphicLayer) {
        if (graphicLayer.getNumberOfGraphics() > 0) {
            int[] gids = graphicLayer.getGraphicIDs();

            for (int id : gids) {
                Graphic g = graphicLayer.getGraphic(id);
                Symbol s = g.getSymbol();

                if (s instanceof SimpleLineSymbol) {
                    SimpleLineSymbol sls = (SimpleLineSymbol)s;
                    sls.setColor(color);

                    graphicLayer.updateGraphic(id, sls);
                }
            }
        }
    }

    private void setPtsColor(@ColorInt int color, GraphicsLayer graphicLayer) {
        if (graphicLayer.getNumberOfGraphics() > 0) {
            int[] gids = graphicLayer.getGraphicIDs();

            for (int id : gids) {
                Graphic g = graphicLayer.getGraphic(id);
                Symbol s = g.getSymbol();

                if (s instanceof SimpleMarkerSymbol) {
                    SimpleMarkerSymbol sms = (SimpleMarkerSymbol)s;
                    sms.setColor(color);

                    graphicLayer.updateGraphic(id, sms);
                }
            }
        }
    }
    //endregion

    //region Getters
    @Override
    public boolean isVisible() {
        return drawOptions.isVisible();
    }

    @Override
    public boolean isAdjBndVisible() {
        return drawOptions.isAdjBnd();
    }

    @Override
    public boolean isAdjBndPtsVisible() {
        return drawOptions.isAdjBndPts();
    }

    @Override
    public boolean isUnadjBndVisible() {
        return drawOptions.isUnadjBnd();
    }

    @Override
    public boolean isUnadjBndPtsVisible() {
        return drawOptions.isUnadjBndPts();
    }

    @Override
    public boolean isAdjNavVisible() {
        return drawOptions.isAdjNav();
    }

    @Override
    public boolean isAdjNavPtsVisible() {
        return drawOptions.isAdjNavPts();
    }

    @Override
    public boolean isUnadjNavVisible() {
        return drawOptions.isUnadjNav();
    }

    @Override
    public boolean isUnadjNavPtsVisible() {
        return drawOptions.isUnadjNavPts();
    }

    @Override
    public boolean isAdjMiscPtsVisible() {
        return drawOptions.isAdjMiscPts();
    }

    @Override
    public boolean isUnadjMiscPtsVisible() {
        return drawOptions.isUnadjMiscPts();
    }

    @Override
    public boolean isWayPtsVisible() {
        return drawOptions.isWayPts();
    }

    @Override
    public boolean isAdjBndClose() {
        return drawOptions.isAdjBndClose();
    }

    @Override
    public boolean isUnadjBndClose() {
        return drawOptions.isUnadjBndClose();
    }



    @Override
    public int getAdjBndColor() {
        return graphicOptions.getAdjBndColor();
    }

    @Override
    public int getUnAdjBndColor() {
        return graphicOptions.getUnAdjBndColor();
    }

    @Override
    public int getAdjNavColor() {
        return graphicOptions.getAdjNavColor();
    }

    @Override
    public int getUnAdjNavColor() {
        return graphicOptions.getUnAdjNavColor();
    }

    @Override
    public int getAdjPtsColor() {
        return graphicOptions.getAdjPtsColor();
    }

    @Override
    public int getUnAdjPtsColor() {
        return graphicOptions.getUnAdjPtsColor();
    }

    @Override
    public int getWayPtsColor() {
        return graphicOptions.getWayPtsColor();
    }
    //endregion
}
