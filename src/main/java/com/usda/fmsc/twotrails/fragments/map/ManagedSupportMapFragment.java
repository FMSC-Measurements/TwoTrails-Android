package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.MultiLineInfoWindowAdapter;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.PositionLegacy;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.GoogleMapsPolygonGrahpic;
import com.usda.fmsc.twotrails.objects.map.GoogleMapsTrailGraphic;
import com.usda.fmsc.twotrails.objects.map.IMarkerDataGraphic;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.units.GoogleMapType;
import com.usda.fmsc.twotrails.units.MapType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class ManagedSupportMapFragment extends SupportMapFragment implements IMultiMapFragment,
        OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {

    private MultiMapListener mmlistener;

    private GoogleMap map;

    private MapOptions startUpMapOptions;

    private ArrayList<IMarkerDataGraphic> _MarkerDataGraphics = new ArrayList<>();

    private Marker currentMarker;

    private int fragWidth, fragHeight;

    private Queue<CameraUpdate> cameraQueue = new ArrayDeque<>();

    private boolean isMoving, cameraQueueEnabled = false;


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
            startUpMapOptions = new MapOptions(0, Consts.Location.USA_BOUNDS, Consts.Location.PADDING);
        }

        getMapAsync(this);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            fragWidth = view.getWidth();
            fragHeight = view.getHeight();
        });
    }


    @Override
    public void setMap(int mapId) {
        if (map != null) {
            map.setMapType(mapId);

            if (mmlistener != null) {
                mmlistener.onMapTypeChanged(MapType.Google, mapId, true);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        //use external GPS if available
        TwoTrailsApp TtAppCtx = TwoTrailsApp.getInstance();

        if (TtAppCtx.getDeviceSettings().isGpsConfigured() && TtAppCtx.getDeviceSettings().getGpsExternal()) {
            GpsService.GpsBinder binder = TtAppCtx.getGps();
            GpsService service = binder.getService();

            map.setLocationSource(service);
        }

        map.setOnMapLoadedCallback(this);
        map.setOnCameraMoveListener(this);
        map.setOnCameraMoveStartedListener(this);
        map.setOnCameraIdleListener(this);

        map.setInfoWindowAdapter(new MultiLineInfoWindowAdapter(getContext()));
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        UiSettings uiSettings = map.getUiSettings();

        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);

        setMapPadding(0, (int) (getResources().getDimension(R.dimen.toolbar_height)), 0, 0);

        if (startUpMapOptions != null && startUpMapOptions.getMapId() != GoogleMapType.MAP_TYPE_NONE.getValue()) {
            try {
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
                            startUpMapOptions.getZoomLevel() != null ? startUpMapOptions.getZoomLevel() : Consts.Location.ZOOM_GENERAL));
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                            Consts.Location.GoogleMaps.USA_BOUNDS,
                            fragWidth,
                            fragHeight,
                            Consts.Location.PADDING
                    ));
                }
            } catch (Exception e) {
                TwoTrailsApp.getInstance().getReport().writeError("ManagedSupportMapFragment:onMapReady", e.getMessage(), e.getStackTrace());
            }
        }

        if (mmlistener != null) {
            mmlistener.onMapReady();
            mmlistener.onMapTypeChanged(MapType.Google, map.getMapType(), true);
        }
    }

    @Override
    public void onMapLoaded() {
        if (mmlistener != null) {
            mmlistener.onMapLoaded();
        }
    }


    @Override
    public void onCameraIdle() {

    }

    @Override
    public void onCameraMove() {
        onMapLocationChanged();
    }

    @Override
    public void onCameraMoveStarted(int i) {

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
            mmlistener.onMapClick(new PositionLegacy(latLng.latitude, latLng.longitude));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        currentMarker = marker;

        if (mmlistener != null) {
            MarkerData md = getMarkerData(marker.getId());

            if (md != null)
                mmlistener.onMarkerClick(md);
        }
        return false;
    }


    @Override
    public void moveToMapMaxExtents(boolean animate) {
        map.moveCamera(CameraUpdateFactory.zoomTo(map.getMinZoomLevel()));
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {
        if (map != null) {
            try
            {
                moveToLocation(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)), animate);
            } catch (Exception ex) {
                TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(f,f,b)", ex.getStackTrace());
            }
        } else {
            TwoTrailsApp.getInstance().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(f,f,b)");
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        if (map != null) {
            try {
                moveToLocation(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lat, lon),
                        zoomLevel
                ), animate);
            } catch (Exception ex) {
                TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(f,f,f,b)", ex.getStackTrace());
            }
        } else {
            TwoTrailsApp.getInstance().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(f,f,f,b)");
        }
    }

    @Override
    public void moveToLocation(Extent extents, int padding, boolean animate) {
        if (map != null) {
            try {
                moveToLocation(CameraUpdateFactory.newLatLngBounds(
                        new LatLngBounds(
                                new LatLng(extents.getSouth(), extents.getWest()),
                                new LatLng(extents.getNorth(), extents.getEast())
                        ),
                        padding
                ), animate);
            } catch (Exception ex) {
                TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(e,i,b)", ex.getStackTrace());
            }
        } else {
            TwoTrailsApp.getInstance().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(e,i,b)");
        }
    }

    private void moveToLocation(CameraUpdate cu, boolean animate) {
        if (map != null && cu != null) {
            if (animate) {
                if (cameraQueueEnabled) {
                    if (isMoving) {
                        cameraQueue.add(cu);
                    } else {
                        isMoving = true;
                        map.animateCamera(cu, cancelableCallback);
                    }
                } else {
                    map.animateCamera(cu);
                }
            } else {
                if (cameraQueueEnabled) {
                    cameraQueue.clear();
                    isMoving = false;
                }

                map.moveCamera(cu);
            }
        }
    }

    GoogleMap.CancelableCallback cancelableCallback = new GoogleMap.CancelableCallback() {
        @Override
        public void onFinish() {
            if (cameraQueue.peek() != null) {
                moveToLocation(cameraQueue.poll(), true);
            } else {
                isMoving = false;
            }
        }

        @Override
        public void onCancel() {
            cameraQueue.clear();
            isMoving = false;
        }
    };

    public void setEnableCameraQueue(boolean enabled) {
        cameraQueueEnabled = enabled;

        if (!cameraQueueEnabled) {
            if (cameraQueue.peek() != null) {
                CameraUpdate cu = cameraQueue.poll();

                while (cameraQueue.peek() != null) {
                    cu = cameraQueue.poll();
                }

                map.animateCamera(cu);
            }
        }
    }


    @Override
    public boolean mapHasMaxExtents() {
        return false;
    }

    @Override
    public void hideSelectedMarkerInfo() {
        if (currentMarker != null) {
            currentMarker.hideInfoWindow();
        }
    }

    @Override
    public PositionLegacy getLatLon() {
        LatLng ll = map.getCameraPosition().target;
        return new PositionLegacy(ll.latitude, ll.longitude);
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
        for (IMarkerDataGraphic mdg : _MarkerDataGraphics) {
            if (mdg.getMarkerData().containsKey(id)) {
                return mdg.getMarkerData().get(id);
            }
        }

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
    public void setMapPadding(int left, int top, int right, int bottom) {
        if (map != null) {
            map.setPadding(left, top, right, bottom);
        }
    }

    @Override
    public void setGesturesEnabled(boolean enabled) {
        if (map != null) {
            map.getUiSettings().setAllGesturesEnabled(enabled);
        }
    }

    @Override
    public void addPolygon(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        GoogleMapsPolygonGrahpic gmpg = new GoogleMapsPolygonGrahpic(map);
        graphicManager.setGraphic(gmpg, drawOptions);
        _MarkerDataGraphics.add(gmpg);
    }

    //todo removePolygon(PolygonGraphicManager graphicManager)
    @Override
    public void removePolygon(PolygonGraphicManager graphicManager) {

    }

    @Override
    public void addTrail(TrailGraphicManager graphicManager) {
        GoogleMapsTrailGraphic gmtg = new GoogleMapsTrailGraphic(map);
        graphicManager.setGraphic(gmtg);
        _MarkerDataGraphics.add(gmtg);
    }

    //todo removeTrail(TrailGraphicManager graphicManager)
    @Override
    public void removeTrail(TrailGraphicManager graphicManager) {

    }

}
