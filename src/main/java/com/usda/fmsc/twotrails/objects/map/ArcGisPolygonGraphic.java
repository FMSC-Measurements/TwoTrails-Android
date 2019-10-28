package com.usda.fmsc.twotrails.objects.map;

import androidx.annotation.ColorInt;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment.MarkerData;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ArcGisPolygonGraphic implements IPolygonGraphic, IMarkerDataGraphic {
    private TtPolygon polygon;
    private PolygonDrawOptions drawOptions;
    private PolygonGraphicOptions graphicOptions;
    private HashMap<String, MarkerData> _MarkerData;

    private MapView map;
    private Extent polyBounds;

    private GraphicsOverlay _AdjBndPts, _UnadjBndPts, _AdjNavPts, _UnadjNavPts, _WayPts, _AdjMiscPts, _UnadjMiscPts;
    private GraphicsOverlay _AdjBndCB, _UnadjBndCB;
    private GraphicsOverlay _AdjBnd, _UnadjBnd, _AdjNav, _UnadjNav;

    public ArcGisPolygonGraphic(MapView map) {
        this.map = map;
    }

    @Override
    public void build(TtPolygon polygon, List<TtPoint> points, HashMap<String, TtMetadata> meta, PolygonGraphicOptions graphicOptions, PolygonDrawOptions drawOptions) {
        this.polygon = polygon;
        this.drawOptions = drawOptions;
        this.graphicOptions = graphicOptions;

        _MarkerData = new HashMap<>();

        _AdjBndCB = new GraphicsOverlay();
        _UnadjBndCB = new GraphicsOverlay();
        _AdjBnd = new GraphicsOverlay();
        _UnadjBnd = new GraphicsOverlay();
        _AdjNav = new GraphicsOverlay();
        _UnadjNav = new GraphicsOverlay();

        _AdjBndPts = new GraphicsOverlay();
        _AdjNavPts = new GraphicsOverlay();
        _UnadjBndPts = new GraphicsOverlay();
        _UnadjNavPts = new GraphicsOverlay();
        _WayPts = new GraphicsOverlay();
        _AdjMiscPts = new GraphicsOverlay();
        _UnadjMiscPts = new GraphicsOverlay();

        Extent.Builder llBuilder = new Extent.Builder();

        PointCollection adjBndPC = new PointCollection(SpatialReferences.getWgs84());
        PointCollection unadjBndPC = new PointCollection(SpatialReferences.getWgs84());

        PointCollection adjBndPLC = new PointCollection(SpatialReferences.getWgs84());
        PointCollection unadjBndPLC = new PointCollection(SpatialReferences.getWgs84());
        PointCollection adjNavPLC = new PointCollection(SpatialReferences.getWgs84());
        PointCollection unadjNavPLC = new PointCollection(SpatialReferences.getWgs84());

        SimpleMarkerSymbol adjMkOpts = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.DIAMOND,
                graphicOptions.getAdjPtsColor(),
                (int)graphicOptions.getUnAdjWidth()
        );

        SimpleMarkerSymbol unAdjMkOpts = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.SQUARE,
                graphicOptions.getUnAdjPtsColor(),
                (int)graphicOptions.getUnAdjWidth()
        );

        Position adjPos, unAdjPos;
        Point adjLL, unadjLL;

        TtMetadata metadata;

        MarkerData adjMd, unadjMd;

        for (TtPoint point : points) {
            metadata = meta.get(point.getMetadataCN());

            adjPos = TtUtils.Points.getLatLonFromPoint(point, true, metadata);
            adjLL = new Point(adjPos.getLongitudeSignedDecimal(), adjPos.getLatitudeSignedDecimal(), SpatialReferences.getWgs84());

            unAdjPos = TtUtils.Points.getLatLonFromPoint(point, false, metadata);
            unadjLL = new Point(unAdjPos.getLongitudeSignedDecimal(), unAdjPos.getLatitudeSignedDecimal(), SpatialReferences.getWgs84());

            adjMd = new MarkerData(point, metadata, true);
            unadjMd = new MarkerData(point, metadata, false);

            _MarkerData.put(adjMd.getKey(), adjMd);
            _MarkerData.put(unadjMd.getKey(), unadjMd);

            if (point.isBndPoint()) {
                Graphic adjmk = new Graphic(adjLL, adjMkOpts);
                adjmk.getAttributes().put(MarkerData.ATTR_KEY, adjMd);
                _AdjBndPts.getGraphics().add(adjmk);

                Graphic unadjmk = new Graphic(adjLL, unAdjMkOpts);
                unadjmk.getAttributes().put(MarkerData.ATTR_KEY, unadjMd);
                _UnadjBndPts.getGraphics().add(unadjmk);

                adjBndPC.add(adjLL);
                unadjBndPC.add(unadjLL);

                adjBndPLC.add(adjLL);
                unadjBndPLC.add(unadjLL);
            }

            if (point.isNavPoint()) {
                Graphic adjmk = new Graphic(adjLL, adjMkOpts);
                adjmk.getAttributes().put(MarkerData.ATTR_KEY, adjMd);
                _AdjNavPts.getGraphics().add(adjmk);

                Graphic unadjmk = new Graphic(adjLL, unAdjMkOpts);
                unadjmk.getAttributes().put(MarkerData.ATTR_KEY, unadjMd);
                _UnadjNavPts.getGraphics().add(unadjmk);

                adjNavPLC.add(adjLL);
                unadjNavPLC.add(unadjLL);
            }

            if (point.getOp() == OpType.WayPoint) {
                Graphic unadjmk = new Graphic(adjLL, unAdjMkOpts);
                unadjmk.getAttributes().put(MarkerData.ATTR_KEY, unadjMd);
                _WayPts.getGraphics().add(unadjmk);
            }

            if (point.getOp() == OpType.SideShot && !point.isOnBnd()) {
                Graphic adjmk = new Graphic(adjLL, adjMkOpts);
                adjmk.getAttributes().put(MarkerData.ATTR_KEY, adjMd);
                _AdjMiscPts.getGraphics().add(adjmk);

                Graphic unadjmk = new Graphic(adjLL, unAdjMkOpts);
                unadjmk.getAttributes().put(MarkerData.ATTR_KEY, unadjMd);
                _UnadjMiscPts.getGraphics().add(unadjmk);
            }

            llBuilder.include(adjPos);
        }

        if (points.size() > 0) {
            polyBounds = llBuilder.build();
        } else {
            polyBounds = null;
        }

        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, graphicOptions.getAdjBndColor(), graphicOptions.getAdjWidth());
        _AdjBnd.getGraphics().add(new Graphic(new Polygon(adjBndPLC), outline));
        _AdjBndCB.getGraphics().add(new Graphic(new Polyline(adjBndPC), outline));

        outline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, graphicOptions.getUnAdjBndColor(), graphicOptions.getUnAdjWidth());
        _UnadjBnd.getGraphics().add(new Graphic(new Polygon(unadjBndPLC), outline));
        _UnadjBndCB.getGraphics().add(new Graphic(new Polyline(unadjBndPC), outline));

        outline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, graphicOptions.getAdjNavColor(), graphicOptions.getAdjWidth());
        _AdjNav.getGraphics().add(new Graphic(new Polyline(adjNavPLC), outline));


        outline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, graphicOptions.getUnAdjNavColor(), graphicOptions.getUnAdjWidth());
        _UnadjNav.getGraphics().add(new Graphic(new Polyline(unadjNavPLC), outline));

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

        map.getGraphicsOverlays().addAll(Arrays.asList(
                _UnadjBnd, _UnadjBndCB,
                _AdjBnd, _AdjBndCB,
                _UnadjNav, _AdjNav,
                _UnadjBndPts, _AdjBndPts,
                _UnadjNavPts, _AdjNavPts,
                _UnadjMiscPts, _AdjMiscPts,
                _WayPts));
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
    public HashMap<String, MarkerData> getMarkerData() {
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
    public GraphicsOverlay getAdjBndPtsLayer() {
        return _AdjBndPts;
    }

    public GraphicsOverlay getUnadjBndPtsLayer() {
        return _UnadjBndPts;
    }

    public GraphicsOverlay getAdjNavPtsLayer() {
        return _AdjNavPts;
    }

    public GraphicsOverlay getUnadjNavPtsLayer() {
        return _UnadjNavPts;
    }

    public GraphicsOverlay getWayPtsLayer() {
        return _WayPts;
    }

    public GraphicsOverlay getAdjMiscPtsLayer() {
        return _AdjMiscPts;
    }

    public GraphicsOverlay getUnadjMiscPtsLayer() {
        return _UnadjMiscPts;
    }

    public GraphicsOverlay getAdjBndCBLayer() {
        return _AdjBndCB;
    }

    public GraphicsOverlay getUnadjBndCBLayer() {
        return _UnadjBndCB;
    }

    public GraphicsOverlay getAdjBndLayer() {
        return _AdjBnd;
    }

    public GraphicsOverlay getUnadjBndLayer() {
        return _UnadjBnd;
    }

    public GraphicsOverlay getAdjNavLayer() {
        return _AdjNav;
    }
    //endregion

    //region Setters
    @Override
    public void setVisible(boolean visible) {
        drawOptions.setVisible(visible);

        if (drawOptions.isAdjBnd()) {
            if (drawOptions.isAdjBndClose()) {
                _AdjBndCB.setVisible(visible);
            }
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


    private void setLineColor(@ColorInt int color, GraphicsOverlay graphicOverlay) {
        if (graphicOverlay.getGraphics().size() > 0) {

            Graphic[] graphics = graphicOverlay.getGraphics().toArray(new Graphic[0]);
            graphicOverlay.getGraphics().clear();

            for (Graphic g : graphics) {
                Symbol s = g.getSymbol();

                if (s instanceof SimpleLineSymbol) {
                    SimpleLineSymbol sls = (SimpleLineSymbol)s;
                    sls.setColor(color);

                    graphicOverlay.getGraphics().remove(g);
                    graphicOverlay.getGraphics().add(new Graphic(g.getGeometry(), sls));
                }
            }
        }
    }

    private void setPtsColor(@ColorInt int color, GraphicsOverlay graphicOverlay) {
        if (graphicOverlay.getGraphics().size() > 0) {

            Graphic[] graphics = graphicOverlay.getGraphics().toArray(new Graphic[0]);
            graphicOverlay.getGraphics().clear();

            for (Graphic g : graphics) {
                Symbol s = g.getSymbol();

                if (s instanceof SimpleMarkerSymbol) {
                    SimpleMarkerSymbol sls = (SimpleMarkerSymbol)s;
                    sls.setColor(color);

                    graphicOverlay.getGraphics().remove(g);
                    graphicOverlay.getGraphics().add(new Graphic(g.getGeometry(), sls));
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
