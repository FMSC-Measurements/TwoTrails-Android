package com.usda.fmsc.twotrails.objects.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMapsTrailGraphic implements ITrailGraphic, IMarkerDataGraphic {
    private final GoogleMap map;

    private ArrayList<Marker> _Markers;
    private Extent polyBounds;
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;
    private LatLngBounds.Builder llBuilder;
    private ArrayList<LatLng> latLngs;
    private Polyline polyline;
    private Polygon polygon;
    private TrailGraphicOptions _GraphicOptions;

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public GoogleMapsTrailGraphic(GoogleMap map) {
        this.map = map;
    }


    @Override
    public void build(List<TtPoint> points, boolean adjusted, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        _GraphicOptions = graphicOptions;
        _Markers = new ArrayList<>();
        _MarkerData = new HashMap<>();

        llBuilder = new LatLngBounds.Builder();

        latLngs = new ArrayList<>();

        for (TtPoint point : points) {
            addPoint(point, adjusted, meta);
        }

        if (points.size() > 0) {
            LatLngBounds bounds = llBuilder.build();
            polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                    bounds.southwest.latitude, bounds.southwest.longitude);
        } else {
            polyBounds = null;
        }

//        if (graphicOptions.isClosedTrail()) {
//            PolygonOptions po = new PolygonOptions();
//            po.strokeWidth(graphicOptions.getTrailWidth());
//            po.strokeColor(graphicOptions.getTrailColor());
//
//            if (latLngs.size() > 0) {
//                po.addAll(latLngs);
//                polygon = map.addPolygon(po);
//            }
//        } else {
//            PolylineOptions plo = new PolylineOptions();
//            plo.width(graphicOptions.getTrailWidth());
//            plo.color(graphicOptions.getTrailColor());
//
//            if (latLngs.size() > 0) {
//                plo.addAll(latLngs);
//                polyline = map.addPolyline(plo);
//            }
//        }
    }

    @Override
    public Position add(TtPoint point, boolean adjusted, HashMap<String, TtMetadata> meta) {
        Position pos = addPoint(point, adjusted, meta);

        LatLngBounds bounds = llBuilder.build();
        polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                bounds.southwest.latitude, bounds.southwest.longitude) ;

        return pos;
    }

    private Position addPoint(TtPoint point, boolean adjusted, HashMap<String, TtMetadata> meta) {
        MarkerOptions markerOptions = TtUtils.GMap.createMarkerOptions(point, adjusted, meta);
        Marker marker = map.addMarker(markerOptions.visible(markersVisible));

        TtMetadata metadata = meta.get(point.getMetadataCN());

        _Markers.add(marker);
        _MarkerData.put(marker.getId(), new IMultiMapFragment.MarkerData(point, metadata, adjusted));

        llBuilder.include(marker.getPosition());

        if (point.isOnBnd()) {
            latLngs.add(marker.getPosition());

            if (_GraphicOptions.isClosedTrail()) {
                if (polyline == null) {
                    PolylineOptions plo = new PolylineOptions();
                    plo.width(_GraphicOptions.getTrailWidth());
                    plo.color(_GraphicOptions.getTrailColor());

                    plo.addAll(latLngs);
                    polyline = map.addPolyline(plo);
                } else {
                    polyline.setPoints(latLngs);
                }
            } else {
                if (polygon == null) {
                    PolygonOptions po = new PolygonOptions();
                    po.strokeWidth(_GraphicOptions.getTrailWidth());
                    po.strokeColor(_GraphicOptions.getTrailColor());

                    po.addAll(latLngs);
                    polygon = map.addPolygon(po);
                }
            }

            if (polyline != null) {
                polyline.setPoints(latLngs);
            } else if (polygon != null) {
                polygon.setPoints(latLngs);
            }
        }

        return new Position(markerOptions.getPosition().latitude, markerOptions.getPosition().longitude, point.getUnAdjZ(), metadata.getElevation());
    }

    @Override
    public void deleteLastPoint() {
        if (_Markers.size() > 0) {
            Marker m = _Markers.remove(_Markers.size() - 1);

            latLngs.remove(latLngs.size() - 1);

            if (m != null) {
                _MarkerData.remove(m.getId());
                m.remove();
            }
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
    public Position getPosition() {
        return polyBounds.getCenter();
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

        for (Marker m : _Markers) {
            m.setVisible(visible);
        }
    }

    @Override
    public void setTrailVisible(boolean visible) {
        this.trailVisible = visible;

        if (polyline != null) {
            polyline.setVisible(visible);
        } else {
            polygon.setVisible(visible);
        }
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
