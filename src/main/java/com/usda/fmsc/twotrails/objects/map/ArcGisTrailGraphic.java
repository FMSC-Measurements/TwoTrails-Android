package com.usda.fmsc.twotrails.objects.map;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class ArcGisTrailGraphic implements ITrailGraphic, IMarkerDataGraphic {
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;

    private MapView map;
    private Extent polyBounds;
    private Extent.Builder eBuilder;

    private GraphicsLayer _TrailLayer, _PtsLayer;
    private Stack<String> keys;

    private Polyline polyline;
    private SimpleMarkerSymbol markerOpts;

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public ArcGisTrailGraphic(MapView mapView) {
        this.map = mapView;
    }


    @Override
    public void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        _MarkerData = new HashMap<>();

        keys = new Stack<>();
        eBuilder = new Extent.Builder();

        _TrailLayer = new GraphicsLayer();
        _PtsLayer = new GraphicsLayer();

        markerOpts = new SimpleMarkerSymbol(graphicOptions.getTrailColor(), 20, SimpleMarkerSymbol.STYLE.SQUARE);

        for (TtPoint point : points) {
            addPoint(point, meta);
        }

        SimpleLineSymbol outline = new SimpleLineSymbol(graphicOptions.getTrailColor(), graphicOptions.getTrailWidth(), SimpleLineSymbol.STYLE.SOLID);

        _TrailLayer.addGraphic(new Graphic(polyline, outline));

        map.addLayer(_TrailLayer);
        map.addLayer(_PtsLayer);

        if (points.size() > 0)
            polyBounds = eBuilder.build();
        else
            polyBounds = null;
    }

    @Override
    public GeoPosition add(TtPoint point, HashMap<String, TtMetadata> meta) {
        GeoPosition position = addPoint(point, meta);

        polyBounds = eBuilder.build();

        return position;
    }

    private GeoPosition addPoint(TtPoint point, HashMap<String, TtMetadata> meta) {
        TtMetadata metadata = meta.get(point.getMetadataCN());

        GeoPosition pos = TtUtils.getLatLonFromPoint(point, false, metadata);
        Point posLL = ArcGISTools.latLngToMapSpatial(pos.getLatitudeSignedDecimal(), pos.getLongitudeSignedDecimal(), map);
        Graphic mk = new Graphic(posLL, markerOpts);

        String key = Integer.toHexString(_PtsLayer.addGraphic(mk));
        _MarkerData.put(key, new IMultiMapFragment.MarkerData(point, metadata, true));
        keys.add(key);

        if (polyline == null) {
            polyline = new Polyline();
            polyline.startPath(posLL);
        } else {
            polyline.lineTo(posLL);
        }

        eBuilder.include(pos);

        return pos;
    }

    @Override
    public void deleteLastPoint() {
        polyline.removePoint(polyline.getPointCount() - 1);
        _MarkerData.remove(keys.pop());
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
}
