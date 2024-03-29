package com.usda.fmsc.twotrails.fragments.map;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

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
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.objects.map.GoogleMapsLineGraphic;
import com.usda.fmsc.twotrails.objects.map.GoogleMapsPolygonGraphic;
import com.usda.fmsc.twotrails.objects.map.GoogleMapsTrailGraphic;
import com.usda.fmsc.twotrails.objects.map.IMarkerDataGraphic;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.units.MapType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class ManagedSupportMapFragment extends SupportMapFragment implements IMultiMapFragment,
        OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {

    private TwoTrailsApp _TtAppCtx;

    private MultiMapListener mmlistener;

    private GoogleMap map;

    private MapOptions startUpMapOptions;

    private final ArrayList<IMarkerDataGraphic> _MarkerDataGraphics = new ArrayList<>();
    private final HashMap<String, GoogleMapsPolygonGraphic> polygonGraphics = new HashMap<>();
    private final HashMap<String, GoogleMapsTrailGraphic> trailGraphics = new HashMap<>();
    private final HashMap<String, GoogleMapsLineGraphic> lineGraphics = new HashMap<>();

    private Marker currentMarker;

    private final Queue<CameraUpdate> cameraQueue = new ArrayDeque<>();

    private boolean isMoving, cameraQueueEnabled = false, attachGpsServiceOnActivityAttached = false;

    private Runnable attachGpsService = new Runnable() {
        @Override
        public void run() {
            if (getTtAppCtx().getDeviceSettings().isGpsConfigured() && getTtAppCtx().getDeviceSettings().getGpsExternal()) {
                if (getTtAppCtx().isGpsServiceStarted()) {
                    map.setLocationSource(getTtAppCtx().getGps().getService());
                } else {
                    getTtAppCtx().startGpsService();

                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);

                            if (getTtAppCtx().isGpsServiceStarted()) {
                                map.setLocationSource(getTtAppCtx().getGps().getService());
                            }
                        } catch (InterruptedException e) {
                            //
                        }
                    }).start();
                }
            }
        }
    };


    @NonNull
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (_TtAppCtx == null) {
            Context ctx = context.getApplicationContext();
            if (ctx != null) {
                _TtAppCtx = (TwoTrailsApp)ctx;
            } else {
                throw new RuntimeException("Null app context");
            }
        }

        if (context instanceof MultiMapListener) {
            mmlistener = (MultiMapListener) context;
        } else {
            throw new RuntimeException(context + " must implement MultiMapListener");
        }

        if (attachGpsServiceOnActivityAttached) {
            attachGpsService.run();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mmlistener = null;
    }

    protected TwoTrailsApp getTtAppCtx() {
        if (_TtAppCtx == null) {
            Activity act =  getActivity();

            if (act != null) {
                Context ctx = act.getApplicationContext();
                if (ctx != null) {
                    _TtAppCtx = (TwoTrailsApp)ctx;
                } else {
                    throw new RuntimeException("Null app context");
                }
            }
        }

        return _TtAppCtx;
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        //use external GPS if available

        if (getTtAppCtx() != null) {
            attachGpsService.run();
        } else {
            attachGpsServiceOnActivityAttached = true;
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
        //
    }

    @Override
    public void onCameraMoveStarted(int i) {

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
    public void moveToLocation(double lat, double lon, boolean animate) {
        if (map != null) {
            try
            {
                moveToLocation(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)), animate);
            } catch (Exception ex) {
                if (getTtAppCtx() != null) {
                    getTtAppCtx().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(f,f,b)", ex.getStackTrace());
                }
            }
        } else if (getTtAppCtx() != null) {
            getTtAppCtx().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(f,f,b)");
        }
    }

    @Override
    public void moveToLocation(double lat, double lon, float zoomLevel, boolean animate) {
        if (map != null) {
            try {
                moveToLocation(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lat, lon),
                        zoomLevel
                ), animate);
            } catch (Exception ex) {
                if (getTtAppCtx() != null) {
                    getTtAppCtx().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(f,f,f,b)", ex.getStackTrace());
                }
            }
        } else if (getTtAppCtx() != null) {
            getTtAppCtx().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(f,f,f,b)");
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
                if (getTtAppCtx() != null) {
                    getTtAppCtx().getReport().writeError(ex.getMessage(), "ManagedSupportMapFragment:moveToLocation(e,i,b)", ex.getStackTrace());
                }
            }
        } else if (getTtAppCtx() != null) {
            getTtAppCtx().getReport().writeWarn("Map not ready", "ManagedSupportMapFragment:moveToLocation(e,i,b)");
        }
    }

    @Override
    public void updateLocation(Position position) {
        //
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

    private final GoogleMap.CancelableCallback cancelableCallback = new GoogleMap.CancelableCallback() {
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

                if (cu != null) {
                    map.animateCamera(cu);
                }
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

//    @Override
////    public Position getMapLatLonCenter() {
////        LatLng ll = map.getCameraPosition().target;
////        return new Position(ll.latitude, ll.longitude);
////    }

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
        if (!polygonGraphics.containsKey(graphicManager.getPolygonCN())) {
            GoogleMapsPolygonGraphic gmpg = new GoogleMapsPolygonGraphic(map);
            polygonGraphics.put(graphicManager.getPolygonCN(), gmpg);

            graphicManager.setGraphic(gmpg, drawOptions);

            _MarkerDataGraphics.add(gmpg);
        }
    }

    //todo removePolygon(PolygonGraphicManager graphicManager)
    @Override
    public void removePolygon(PolygonGraphicManager graphicManager) {

    }

    @Override
    public void addTrail(TrailGraphicManager graphicManager) {
        if (!trailGraphics.containsKey(graphicManager.getPolygonCN())) {
            GoogleMapsTrailGraphic gmtg = new GoogleMapsTrailGraphic(map);
            trailGraphics.put(graphicManager.getPolygonCN(), gmtg);

            graphicManager.setGraphic(gmtg);

            _MarkerDataGraphics.add(gmtg);
        }
    }

    //todo removeTrail(TrailGraphicManager graphicManager)
    @Override
    public void removeTrail(TrailGraphicManager graphicManager) {

    }

    @Override
    public void addLine(LineGraphicManager graphicManager) {
        if (!lineGraphics.containsKey(graphicManager.getCN())) {
            GoogleMapsLineGraphic gmlg = new GoogleMapsLineGraphic(map);
            lineGraphics.put(graphicManager.getCN(), gmlg);

            graphicManager.setGraphic(gmlg);
        }
    }

    //todo removeLine(LineGraphicManager graphicManager)
    @Override
    public void removeLine(LineGraphicManager graphicManager) {

    }
}
