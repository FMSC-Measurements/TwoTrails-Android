package com.usda.fmsc.twotrails.objects;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMapsTrailGraphic implements ITrailGraphic {
    private GoogleMap map;

    private ArrayList<Marker> _Markers;
    private Extent polyBounds;
    private HashMap<String, IMultiMapFragment.MarkerData> _MarkerData;
    private LatLngBounds.Builder llBuilder;

    private PolylineOptions plo = new PolylineOptions();

    private boolean visible = true, trailVisible = true, markersVisible = true;


    public GoogleMapsTrailGraphic(GoogleMap map) {
        this.map = map;
    }


    @Override
    public void build(List<TtPoint> points, HashMap<String, TtMetadata> meta, TrailGraphicOptions graphicOptions) {
        _Markers = new ArrayList<>();
        _MarkerData = new HashMap<>();

        llBuilder = new LatLngBounds.Builder();

        plo.width(graphicOptions.getTrailWidth());
        plo.color(graphicOptions.getTrailColor());

        for (TtPoint point : points) {
            addPoint(point, meta);
        }

        LatLngBounds bounds = llBuilder.build();
        polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                bounds.southwest.latitude, bounds.southwest.longitude) ;
    }

    @Override
    public void add(TtPoint point, HashMap<String, TtMetadata> meta) {
        addPoint(point, meta);

        LatLngBounds bounds = llBuilder.build();
        polyBounds = new Extent(bounds.northeast.latitude, bounds.northeast.longitude,
                bounds.southwest.latitude, bounds.southwest.longitude) ;
    }

    private void addPoint(TtPoint point, HashMap<String, TtMetadata> meta) {
        MarkerOptions markerOptions = TtUtils.GMap.createMarkerOptions(point, false, meta);
        Marker marker = map.addMarker(markerOptions.visible(markersVisible));

        _Markers.add(marker);
        _MarkerData.put(marker.getId(), new IMultiMapFragment.MarkerData(point, meta.get(point.getMetadataCN()), true));

        llBuilder.include(marker.getPosition());

        plo.add(marker.getPosition());
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

        plo.visible(visible);
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
