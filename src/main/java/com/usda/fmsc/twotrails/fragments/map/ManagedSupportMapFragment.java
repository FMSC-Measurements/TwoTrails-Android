package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.MultiLineInfoWindowAdapter;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.activities.custom.MultiMapTypeActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.GoogleMapsPolygonGrahpic;
import com.usda.fmsc.twotrails.objects.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.PolygonGraphicManager;

import java.util.HashMap;

public class ManagedSupportMapFragment extends SupportMapFragment implements IMultiMapFragment,
        OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    MultiMapListener mmlistener;

    GoogleMap map;

    MapOptions startUpMapOptions;

    private HashMap<String, MarkerData> _MarkerData = new HashMap<>();

    private Marker currentMarker;


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
            startUpMapOptions = new MapOptions(0, Consts.LocationInfo.USA_BOUNDS, Consts.LocationInfo.PADDING);
        }

        getMapAsync(this);
    }


    int fragWidth, fragHeight;
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fragWidth = view.getWidth();
                fragHeight = view.getHeight();
            }
        });
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
        //use external GPS if available
        if (Global.Settings.DeviceSettings.isGpsConfigured() && Global.Settings.DeviceSettings.getGpsExternal()) {
            GpsService.GpsBinder binder = Global.getGpsBinder();
            GpsService service = binder.getService();

            map.setLocationSource(service);
        }

        map = googleMap;

        map.setOnMapLoadedCallback(this);
        map.setOnCameraChangeListener(this);

        map.setInfoWindowAdapter(new MultiLineInfoWindowAdapter(getContext()));
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        UiSettings uiSettings = map.getUiSettings();

        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);

        map.setPadding(0, (int) (getResources().getDimension(R.dimen.toolbar_height)), 0, 0);

        if (startUpMapOptions != null) {
            if (startUpMapOptions.hasExtents()) {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                        new LatLngBounds(
                                new LatLng(startUpMapOptions.getSouth(), startUpMapOptions.getWest()),
                                new LatLng(startUpMapOptions.getNorth(), startUpMapOptions.getEast())
                        ),
                        fragWidth - startUpMapOptions.getPadding() / 2,
                        fragHeight - startUpMapOptions.getPadding() / 2,
                        startUpMapOptions.getPadding()
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
                        Consts.LocationInfo.GoogleMaps.USA_BOUNDS,
                        fragWidth,
                        fragHeight,
                        Consts.LocationInfo.PADDING
                ));
            }
        }

        if (mmlistener != null) {
            mmlistener.onMapReady();
        }
    }

    @Override
    public void onMapLoaded() {
        if (mmlistener != null) {
            mmlistener.onMapLoaded();
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
    public void onMapClick(LatLng latLng) {
        if (mmlistener != null) {
            mmlistener.onMapClick(new Position(latLng.latitude, latLng.longitude));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        currentMarker = marker;

        if (mmlistener != null) {
            mmlistener.onMarkerClick(_MarkerData.get(marker.getId()));
        }
        return false;
    }

    @Override
    public void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lon),
                zoomLevel
        );

        if (animate) {
            map.animateCamera(cu);
        } else {
            map.moveCamera(cu);
        }
    }

    @Override
    public void moveToLocation(Extent extents, int padding, boolean animate) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                new LatLng(extents.getSouth(), extents.getWest()),
                new LatLng(extents.getNorth(), extents.getEast())
            ),
            padding
        );

        if (animate) {
            map.animateCamera(cu);
        } else {
            map.moveCamera(cu);
        }
    }

    @Override
    public void hideSelectedMarkerInfo() {
        if (currentMarker != null) {
            currentMarker.hideInfoWindow();
        }
    }

    @Override
    public Position getLatLon() {
        LatLng ll = map.getCameraPosition().target;
        return new Position(ll.latitude, ll.longitude);
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

    @Override
    public MarkerData getMarkerData(String id) {
        return null;
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



    @Override
    public void setLocationEnabled(boolean enabled) {
        if (map != null && AndroidUtils.App.checkLocationPermission(getContext())) {
            map.setMyLocationEnabled(enabled);
        }
    }

    @Override
    public void setCompassEnabled(boolean enabled) {
        if (map != null) {
            map.getUiSettings().setCompassEnabled(enabled);
        }
    }

    @Override
    public void addGraphic(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        graphicManager.setPolygonGraphic(new GoogleMapsPolygonGrahpic(map), drawOptions);
        _MarkerData.putAll(graphicManager.getMarkerData());
    }
}
