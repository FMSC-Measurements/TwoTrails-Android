package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Units;

public class ManagedSupportMapFragment extends SupportMapFragment implements IMultiMapFragment, OnMapReadyCallback, GoogleMap.OnCameraChangeListener {

    MultiMapListener mmlistener;

    GoogleMap map;

    MapOptions startUpMapOptions;




    public static ManagedSupportMapFragment newInstance() {
        return new ManagedSupportMapFragment();
    }

    public static ManagedSupportMapFragment newInstance(MapOptions options) {
        ManagedSupportMapFragment mapFragment = new ManagedSupportMapFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable("MapOptions", new GoogleMapOptions().mapType(options.getMapId()));
        bundle.putParcelable(MAP_OPTIONS_EXTRA, options);
        mapFragment.setArguments(bundle);

        return mapFragment;
    }

    public ManagedSupportMapFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(MAP_OPTIONS_EXTRA)) {
            startUpMapOptions = bundle.getParcelable(MAP_OPTIONS_EXTRA);
        } else {
            startUpMapOptions = new MapOptions(0, Consts.LocationInfo.USA_BOUNDS);
        }

        getMapAsync(this);
    }




    @Override
    public void setMap(int mapId) {
        if (map != null) {
            map.setMapType(mapId);

            if (mmlistener != null) {
                mmlistener.onMapTypeChanged(Units.MapType.Google, mapId);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setOnCameraChangeListener(this);

        if (startUpMapOptions != null) {
            if (startUpMapOptions.hasExtents()) {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                        new LatLngBounds(
                                new LatLng(startUpMapOptions.getSouth(), startUpMapOptions.getWest()),
                                new LatLng(startUpMapOptions.getNorth(), startUpMapOptions.getEast())
                        ), 0
                ));
            } else if (startUpMapOptions.hasLocation()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(
                                startUpMapOptions.getLatitude(),
                                startUpMapOptions.getLongitide()
                        ),
                        startUpMapOptions.getZoomLevel() != null ? startUpMapOptions.getZoomLevel() : Consts.LocationInfo.GoogleMaps.ZOOM_GENERAL));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                        Consts.LocationInfo.GoogleMaps.USA_BOUNDS, Consts.LocationInfo.GoogleMaps.PADDING
                ));
            }
        }

        if (mmlistener != null) {
            mmlistener.onMapReady();
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        onMapLocationChanged();
    }

    @Override
    public void onMapLocationChanged() {
        if (mmlistener != null) {
            mmlistener.onMapLocationChanged();
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lon),
                Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE
        );

        if (animate) {
            map.animateCamera(cu);
        } else {
            map.moveCamera(cu);
        }
    }


    @Override
    public Position getLatLon() {
        LatLng ll = map.getCameraPosition().target;
        return new Position(ll.latitude, ll.longitude);
    }

    public LatLngBounds getBounds() {
        return map.getProjection().getVisibleRegion().latLngBounds;

    }

    @Override
    public Extent getExtents() {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;

        return new Extent(
                bounds.northeast.latitude,
                bounds.northeast.longitude,
                bounds.southwest.latitude,
                bounds.southwest.longitude);
    }

    public float getZoomLevel() {
        return map.getCameraPosition().zoom;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MultiMapListener) {
            mmlistener = (MultiMapListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MultiMapListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mmlistener = null;
    }
}
