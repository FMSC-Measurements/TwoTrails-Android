package com.usda.fmsc.twotrails.objects.map;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class ArcGisTrailGraphic implements ITrailGraphic, IMarkerDataGraphic {
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;

    private MapView map;
    private Extent polyBounds;
    private Extent.Builder eBuilder;

    private GraphicsOverlay _TrailLayer, _PtsLayer;
    private Graphic _TrailGraphic;
    private PointCollection _TrailPoints;
    private SimpleLineSymbol _TrailOutline;
    private Stack<String> keys;

    private SimpleMarkerSymbol markerOpts;

    private TtPoint _LastPoint;

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public ArcGisTrailGraphic(MapView mapView) {
        this.map = mapView;
    }


    @Override
    public void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        _MarkerData = new HashMap<>();

        keys = new Stack<>();
        eBuilder = new Extent.Builder();

        _TrailLayer = new GraphicsOverlay();
        _PtsLayer = new GraphicsOverlay();

        int drawSize = (int)(graphicOptions.getTrailWidth() / 2);

        markerOpts = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, graphicOptions.getPointColor(), drawSize);

        _TrailOutline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, graphicOptions.getTrailColor(), drawSize);

        for (TtPoint point : points) {
            addPoint(point, meta);
        }

        map.getGraphicsOverlays().add(_TrailLayer);
        map.getGraphicsOverlays().add(_PtsLayer);

        if (points.size() > 0)
            polyBounds = eBuilder.build();
        else
            polyBounds = null;
    }

    @Override
    public Position add(TtPoint point, HashMap<String, TtMetadata> meta) {
        Position position = addPoint(point, meta);

        polyBounds = eBuilder.build();

        return position;
    }

    private Position addPoint(TtPoint point, HashMap<String, TtMetadata> meta) {
        TtMetadata metadata = meta.get(point.getMetadataCN());

        Position pos = TtUtils.Points.getLatLonFromPoint(point, false, metadata);
        Point posLL = TwoTrailsApp.getInstance().getArcGISTools().latLngToMapSpatial(pos.getLatitudeSignedDecimal(), pos.getLongitudeSignedDecimal(), map);
        Graphic mk = new Graphic(posLL, markerOpts);
        _PtsLayer.getGraphics().add(mk);

        String key = mk.hashCode() + "_trail";
        _MarkerData.put(key, new IMultiMapFragment.MarkerData(point, metadata, true));
        keys.add(key);

        if (point.isOnBnd()) {
            if (_TrailPoints == null) {
                _TrailPoints = new PointCollection(map.getSpatialReference());
            }

            _TrailPoints.add(posLL);

            if (_TrailGraphic != null) {
                _TrailLayer.getGraphics().remove(_TrailGraphic);
            }

            _TrailGraphic = new Graphic(new Polyline(_TrailPoints), _TrailOutline);
            _TrailLayer.getGraphics().add(_TrailGraphic);

            eBuilder.include(pos);
        }

        _LastPoint = point;

        return pos;
    }

    @Override
    public void deleteLastPoint() {
        if (_LastPoint != null) {
            if (_TrailPoints.size() > 0 && _LastPoint.isOnBnd()) {
                _TrailPoints.remove(_TrailPoints.size() - 1);

                if (_TrailGraphic != null) {
                    _TrailLayer.getGraphics().remove(_TrailGraphic);
                }

                _TrailGraphic = new Graphic(new Polyline(_TrailPoints), _TrailOutline);
                _TrailLayer.getGraphics().add(_TrailGraphic);
            }

            _PtsLayer.getGraphics().remove(_PtsLayer.getGraphics().size() - 1);

            _MarkerData.remove(keys.pop());
        }
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
    public void setVisible(boolean visible) {
        this.visible = visible;

        setMarkersVisible(visible);
        setTrailVisible(visible);
    }

    @Override
    public void setMarkersVisible(boolean visible) {
        this.markersVisible = visible;
        _PtsLayer.setVisible(visible);
    }

    @Override
    public void setTrailVisible(boolean visible) {
        this.trailVisible = visible;
        _TrailLayer.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isMarkersVisible() {
        return markersVisible;
    }

    @Override
    public boolean isTrailVisible() {
        return trailVisible;
    }


    public GraphicsOverlay getTrailLayer() {
        return _TrailLayer;
    }

    public GraphicsOverlay getPtsLayer() {
        return _PtsLayer;
    }
}
