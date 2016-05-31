package com.usda.fmsc.twotrails.objects.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMapsTrailGraphic implements ITrailGraphic, IMarkerDataGraphic {
    private GoogleMap map;

    private ArrayList<Marker> _Markers;
    private Extent polyBounds;
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;
    private LatLngBounds.Builder llBuilder;
    private ArrayList<LatLng> latLngs;
    private Polyline polyline;

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public GoogleMapsTrailGraphic(GoogleMap map) {
        this.map = map;
    }


    @Override
    public void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        _Markers = new ArrayList<>();
        _MarkerData = new HashMap<>();

        llBuilder = new LatLngBounds.Builder();

        PolylineOptions plo = new PolylineOptions();
        plo.width(graphicOptions.getTrailWidth());
        plo.color(graphicOptions.getTrailColor());

        polyline = map.addPolyline(plo);
        latLngs = new ArrayList<>();

        for (TtPoint point : points) {
            addPoint(point, meta);
        }

        if (points.size() > 0) {
            LatLngBounds bounds = llBuilder.build();
            polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                    bounds.southwest.latitude, bounds.southwest.longitude);
        } else {
            polyBounds = null;
        }
    }

    @Override
    public GeoPosition add(TtPoint point, HashMap<String, TtMetadata> meta) {
        GeoPosition pos = addPoint(point, meta);

        LatLngBounds bounds = llBuilder.build();
        polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                bounds.southwest.latitude, bounds.southwest.longitude) ;

        return pos;
    }

    private GeoPosition addPoint(TtPoint point, HashMap<String, TtMetadata> meta) {
        MarkerOptions markerOptions = TtUtils.GMap.createMarkerOptions(point, false, meta);
        Marker marker = map.addMarker(markerOptions.visible(markersVisible));

        TtMetadata metadata = meta.get(point.getMetadataCN());

        _Markers.add(marker);
        _MarkerData.put(marker.getId(), new IMultiMapFragment.MarkerData(point, metadata, true));

        llBuilder.include(marker.getPosition());

        latLngs.add(marker.getPosition());
        polyline.setPoints(latLngs);

        return new GeoPosition(markerOptions.getPosition().latitude, markerOptions.getPosition().longitude, point.getUnAdjZ(), metadata.getElevation());
    }

    @Override
    public void deleteLastPoint() {
        if (_Markers.size() > 0) {
            Marker m = _Markers.remove(_Markers.size() - 1);

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

        polyline.setVisible(visible);
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
