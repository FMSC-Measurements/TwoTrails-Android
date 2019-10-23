package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;


import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.AnimationCurve;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.DrawStatusChangedEvent;
import com.esri.arcgisruntime.mapping.view.DrawStatusChangedListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerViewStateChangedEvent;
import com.esri.arcgisruntime.mapping.view.LayerViewStateChangedListener;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapScaleChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapScaleChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.NavigationChangedEvent;
import com.esri.arcgisruntime.mapping.view.NavigationChangedListener;
import com.esri.arcgisruntime.mapping.view.SpatialReferenceChangedEvent;
import com.esri.arcgisruntime.mapping.view.SpatialReferenceChangedListener;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedEvent;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedListener;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.objects.map.ArcGisPolygonGraphic;
import com.usda.fmsc.twotrails.objects.map.ArcGisTrailGraphic;
import com.usda.fmsc.twotrails.objects.map.IMarkerDataGraphic;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.ui.ArcMapCompass;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;

import java.io.FileNotFoundException;
import java.util.ArrayList;


public class ArcGisMapFragment extends Fragment implements IMultiMapFragment, GpsService.Listener, MapRotationChangedListener, MapScaleChangedListener,
        ArcGISMap.BasemapChangedListener, LoadStatusChangedListener {
    private static final String START_ARC_OPTIONS = "StartupArcOptions";

    private TwoTrailsApp TtAppCtx;

    private static final int TOLERANCE = 30;

    private MapOptions startUpMapOptions;
    private MultiMapListener mmListener;

    private GpsService.GpsBinder binder;

    private MapView mapView;
    private ArcGISMap mBasemapLayer;
    private GraphicsOverlay _LocationLayer = new GraphicsOverlay();
    private SimpleMarkerSymbol sms;

    private ArrayList<IMarkerDataGraphic> _MarkerDataGraphics = new ArrayList<>();
    private ArrayList<ArcGisPolygonGraphic> polygonGraphics = new ArrayList<>();
    private ArrayList<ArcGisTrailGraphic> trailGraphics = new ArrayList<>();

    private ArcMapCompass compass;
    private Integer basemapId;

    //private LayoutInflater inflater;

    private boolean mapReady, showPosition, centerOnLoad = false;
    private int gid, padLeft, padTop, padRight, padBottom;

    private ArcGisMapLayer startArcOpts, currentGisMapLayer;


    public static ArcGisMapFragment newInstance() {
        return new ArcGisMapFragment();
    }

    public static ArcGisMapFragment newInstance(MapOptions options) {
        ArcGisMapFragment fragment = new ArcGisMapFragment();
        Bundle args = new Bundle();
        args.putParcelable(MAP_OPTIONS_EXTRA, options);
        fragment.setArguments(args);
        return fragment;
    }

    public static ArcGisMapFragment newInstance(MapOptions options, ArcGisMapLayer arcOptions) {
        ArcGisMapFragment fragment = new ArcGisMapFragment();
        Bundle args = new Bundle();
        args.putParcelable(MAP_OPTIONS_EXTRA, options);
        args.putParcelable(START_ARC_OPTIONS, arcOptions);
        fragment.setArguments(args);
        return fragment;
    }


    public ArcGisMapFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TtAppCtx = TwoTrailsApp.getInstance();

        //inflater = LayoutInflater.from(getContext());

        Bundle bundle = getArguments();

        if (bundle != null) {
            startUpMapOptions = bundle.getParcelable(MAP_OPTIONS_EXTRA);

            if (bundle.containsKey(START_ARC_OPTIONS)) {
                startArcOpts = bundle.getParcelable(START_ARC_OPTIONS);
            }
        } else {
            startUpMapOptions = new MapOptions(0, Consts.Location.USA_BOUNDS);
        }

        if (TwoTrailsApp.getInstance().getDeviceSettings().isGpsConfigured()) {
            binder = TwoTrailsApp.getInstance().getGps();
            binder.addListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arc_gis_map, container, false);

        mapView = view.findViewById(R.id.map);
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

        mapView.addMapRotationChangedListener(this);
        mapView.addMapScaleChangedListener(this);

        mapView.addDrawStatusChangedListener(new DrawStatusChangedListener() {
            @Override
            public void drawStatusChanged(DrawStatusChangedEvent drawStatusChangedEvent) {

            }
        });

//        mapView.addSpatialReferenceChangedListener(new SpatialReferenceChangedListener() {
//            @Override
//            public void spatialReferenceChanged(SpatialReferenceChangedEvent spatialReferenceChangedEvent) {
//
//            }
//        });

        mapView.addNavigationChangedListener(new NavigationChangedListener() {
            @Override
            public void navigationChanged(NavigationChangedEvent navigationChangedEvent) {

            }
        });

        mapView.addLayerViewStateChangedListener(new LayerViewStateChangedListener() {
            @Override
            public void layerViewStateChanged(LayerViewStateChangedEvent layerViewStateChangedEvent) {

            }
        });

//        mapView.addViewpointChangedListener(new ViewpointChangedListener() {
//            @Override
//            public void viewpointChanged(ViewpointChangedEvent viewpointChangedEvent) {
//
//            }
//        });


//        mapView.enableWrapAround(true);
//        mapView.setAllowRotationByPinch(true);
//
//        mapView.setOnStatusChangedListener(this);
//        mapView.setOnZoomListener(this);
//        mapView.setOnPanListener(this);

        mapView.setOnTouchListener(new MapViewOnTouchListenerEx(getContext(), mapView));
//
        compass = view.findViewById(R.id.compass);
        compass.setMapView(mapView);

        if (startArcOpts != null) {
            changeBasemap(startArcOpts);
        } else {
            changeBasemap(startUpMapOptions.getMapId());
        }

        mapView.getGraphicsOverlays().add(_LocationLayer);

        if (startUpMapOptions.hasExtents() || startUpMapOptions.hasLocation()) {
            centerOnLoad = true;
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (binder != null) {
            binder.removeListener(this);
        }

        if (mmListener != null && mmListener.shouldStopGps()) {
            binder.stopGps();
        }

        // Release MapView resources
        if (mapView != null) {
            mapView.dispose();
            mapView = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.pause();
        }
    }


    @Override
    public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {

    }

    @Override
    public void basemapChanged(ArcGISMap.BasemapChangedEvent basemapChangedEvent) {
        //onMapLocationChanged();
    }

    Runnable baseMapDoneLoading = new Runnable() {
        @Override
        public void run() {
            if (mmListener != null) {
                if (!mapReady) {
                    mmListener.onMapReady();
                    mapReady = true;

                    //if map is offline and has extents
                    moveToMapMaxExtents(false);
                }

                mmListener.onMapLoaded();
            }

//            if (centerOnLoad) {
//
//                if (startUpMapOptions.hasExtents()) {
//                    Envelope e = TtAppCtx.getArcGISTools().getEnvelopFromLatLngExtents(
//                            startUpMapOptions.getNorth(),
//                            startUpMapOptions.getEast(),
//                            startUpMapOptions.getSouth(),
//                            startUpMapOptions.getWest(),
//                            mapView);
//
//                    mapView.setExtent(e, 0, false);
//                } else {
//                    mapView.centerAt(startUpMapOptions.getLatitude(), startUpMapOptions.getLongitide(), false);
//                }
//
//                centerOnLoad = false;
//            }
        }
    };

    @Override
    public void mapRotationChanged(MapRotationChangedEvent mapRotationChangedEvent) {
        onMapLocationChanged();
    }

    @Override
    public void mapScaleChanged(MapScaleChangedEvent mapScaleChangedEvent) {
        onMapLocationChanged();
    }


    private void changeBasemap(int basemapId) {
        changeBasemap(TtAppCtx.getArcGISTools().getMapLayer(basemapId));
    }

    public void changeBasemap(final ArcGisMapLayer agml) {
        if (mapView == null) {
            mBasemapLayer = null;
        } else {
            try {
                ArcGISMap baseMap = TtAppCtx.getArcGISTools().getBaseLayer(getContext(), agml);

                baseMap.addBasemapChangedListener(this);
                baseMap.addLoadStatusChangedListener(this);
                baseMap.addDoneLoadingListener(baseMapDoneLoading);

                this.basemapId = agml.getId();
                this.currentGisMapLayer = agml;

                mBasemapLayer = baseMap;

                mapView.setMap(mBasemapLayer);

//                if (agml.hasScales()) {
//                    mapView.setMaxScale(agml.getMaxScale());
//                    mapView.setMinScale(agml.getMinScale());
//                } else {
//                    mapView.setMaxScale(50);
//                    mapView.setMinScale(591657550.5);
//                }

                if (mmListener != null) {
                    mmListener.onMapTypeChanged(MapType.ArcGIS, basemapId, agml.isOnline());
                }

                if (currentGisMapLayer.isOnline() && currentGisMapLayer.getNumberOfLevels() < 1) {
                    AndroidUtils.Device.isInternetAvailable(internetAvailable -> {
                        if (internetAvailable) {
                            TtAppCtx.getArcGISTools().getLayerFromUrl(agml.getUrl(), getActivity(), new ArcGISTools.IGetArcMapLayerListener() {
                                @Override
                                public void onComplete(ArcGisMapLayer layer) {
                                    boolean updated = false;

                                    if (layer.getLevelsOfDetail() != null) {
                                        agml.setLevelsOfDetail(layer.getLevelsOfDetail());
                                        updated = true;
                                    }

                                    if (agml.getMaxScale() < 0 && layer.getMaxScale() >= 0) {
                                        agml.setMaxScale(layer.getMaxScale());
                                        agml.setMinScale(layer.getMinScale());
                                        updated = true;
                                    }

                                    if (updated) {
                                        TtAppCtx.getArcGISTools().updateMapLayer(agml);

                                        if (currentGisMapLayer.getId() == agml.getId()) {
                                            currentGisMapLayer = agml;
                                        }
                                    }
                                }

                                @Override
                                public void onBadUrl(String error) {

                                }
                            });
                        }
                    });
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), "Unable to find offline map file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean mapHasMaxExtents() {
        return currentGisMapLayer != null && !currentGisMapLayer.isOnline() && currentGisMapLayer.hasExtent();
    }

    @Override
    public void setMap(int mapId) {
        if (mapId != basemapId) {
            changeBasemap(mapId);
        }
    }

    @Override
    public void moveToMapMaxExtents(boolean animate) {
        if (mapHasMaxExtents()) {
            moveToLocation(currentGisMapLayer.getExtent(), 0, animate);
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {
        moveToLocation(lat, lon, -1, animate);
    }

    @Override
    public void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        if (mapView != null) {
            if (animate) {
                int zLevels = currentGisMapLayer.getNumberOfLevels();

                if (currentGisMapLayer != null && zLevels > 0 && zoomLevel > zLevels) {
                    zoomLevel = currentGisMapLayer.getNumberOfLevels();
                    //zoomLevel = (float)currentGisMapLayer.getLevelsOfDetail()[(int)zoomLevel].getResolution();
                }

                if (zoomLevel > -1) {
                    mapView.setViewpointCenterAsync(new Point(lon, lat, SpatialReferences.getWgs84()), zoomLevel);
                } else {
                    mapView.setViewpointCenterAsync(new Point(lon, lat, SpatialReferences.getWgs84()), mapView.getMapScale());
                }
            } else {
                mapView.setViewpointAsync(new Viewpoint(lat, lon, mapView.getMapScale()), 0, AnimationCurve.EASE_IN_EXPO);
            }
        }
    }

    @Override
    public void moveToLocation(Extent extents, int padding, boolean animate) {
        if (mapView != null) {
            //mapView.setViewpointAsync(new Viewpoint(TtAppCtx.getArcGISTools().getEnvelopFromLatLngExtents(extents, mapView)));
            mapView.setViewpointGeometryAsync(TtAppCtx.getArcGISTools().getEnvelopFromLatLngExtents(extents, mapView), padding);
        }
    }


    @Override
    public void onMapLocationChanged() {
        if (mmListener != null) {
            mmListener.onMapLocationChanged();
        }
    }

    private void onMarkerClick(int graphicId, Geometry geometry) {
        MarkerData markerData = getMarkerData(Integer.toHexString(graphicId));

        if (markerData != null) {
            if (geometry.getGeometryType().equals(GeometryType.POINT)) {
                Point point = (Point)geometry;

                mapView.setViewpointCenterAsync(point);

                //showSelectedMarkerInfo(markerData, point);
            }

            if (mmListener != null) {
                mmListener.onMarkerClick(markerData);
            }
        }
    }

//    @Override
//    public Position getMapLatLonCenter() {
//        Point point = TtAppCtx.getArcGISTools().mapPointToLatLng(mapView.getCenter(), mapView);
//
//        return new Position(point.getY(), point.getX());
//    }

    @Override
    public Extent getExtents() {
        Viewpoint vp = mapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY);

        if (vp.getTargetGeometry() != null && vp.getTargetGeometry().getGeometryType() == GeometryType.ENVELOPE) {
            Envelope envelope = vp.getTargetGeometry().getExtent();

            if (envelope != null) {
                Point ne = TtAppCtx.getArcGISTools().mapPointToLatLng((int)envelope.getYMax(), (int)envelope.getXMin(), mapView);
                Point sw = TtAppCtx.getArcGISTools().mapPointToLatLng((int)envelope.getYMin(), (int)envelope.getXMax(), mapView);

                return new Extent(sw.getY(), ne.getX(), ne.getY(), sw.getX());
            }
        }

        return null;
    }

    public Envelope getArcExtents() {
        Viewpoint vp = mapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY);

        if (vp.getTargetGeometry() != null && vp.getTargetGeometry().getGeometryType() == GeometryType.ENVELOPE) {
            return vp.getTargetGeometry().getExtent();
        }
        return null;
    }

    public SpatialReference getSpatialReference() {
        return mapView.getSpatialReference();
    }

    public double getScale() {
        return mapView.getMapScale();
    }


    public int getMapZoomLevel() {
//        if (mBasemapLayer instanceof ArcGISTiledMapServiceLayer) {
//            double mapRes = mapView.getResolution();
//            double[] resolutions = ((ArcGISTiledMapServiceLayer) mBasemapLayer).getTileInfo().getResolutions();
//
//            for (int i = 0; i < resolutions.length - 2; i++) {
//                if (mapRes <= resolutions[i] && mapRes > resolutions[i + 1])
//                    return i;
//            }
//        }

        return  -1;
    }


//    public Point getMapPoint(int x, int y) {
//        return mapView.toMapPoint(x, y);
//    }


//    public Extent getExtentsFromScreen(int xmin, int ymin, int xmax, int ymax) {
//        Point ne = TtAppCtx.getArcGISTools().mapPointToLatLng(getMapPoint(xmin, ymin), mapView);
//        Point sw = TtAppCtx.getArcGISTools().mapPointToLatLng(getMapPoint(xmax, ymax), mapView);
//
//        return new Extent(sw.getX(), ne.getY(), ne.getX(), sw.getY());
//    }
//
//    public Envelope getArcExtentsFromScreen(int xmin, int ymin, int xmax, int ymax) {
//        Point nw = getMapPoint(xmin, ymin);
//        Point se = getMapPoint(xmax, ymax);
//
//        return new Envelope(nw.getX(), nw.getY(), se.getX(), se.getY());
//    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MultiMapListener) {
            mmListener = (MultiMapListener) context;

            if (mmListener.shouldStartGps()) {
                if (binder == null)
                    binder = TwoTrailsApp.getInstance().getGps();
                binder.startGps();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mmListener = null;
    }

    @Override
    public void setLocationEnabled(boolean enabled) {
        if (_LocationLayer != null) {
            _LocationLayer.setVisible(enabled);
            showPosition = enabled;
        }
    }

    @Override
    public void setCompassEnabled(boolean enabled) {
        compass.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {
        compass.setPadding(left, top, right, bottom);

        padLeft = left;
        padTop = top;
        padRight = right;
        padBottom = bottom;
    }

    @Override
    public void setGesturesEnabled(boolean enabled) {
        if (mapView != null) {
            mapView.setEnabled(enabled);
        }
    }

    @Override
    public void addPolygon(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        ArcGisPolygonGraphic polygonGraphic = new ArcGisPolygonGraphic(mapView);
        polygonGraphics.add(polygonGraphic);

        graphicManager.setGraphic(polygonGraphic, drawOptions);

        _MarkerDataGraphics.add(polygonGraphic);
    }

    //todo removePolygon(PolygonGraphicManager graphicManager)
    @Override
    public void removePolygon(PolygonGraphicManager graphicManager) {



    }

    @Override
    public void addTrail(TrailGraphicManager graphicManager) {
        ArcGisTrailGraphic trailGraphic = new ArcGisTrailGraphic(mapView);
        trailGraphics.add(trailGraphic);

        graphicManager.setGraphic(trailGraphic);

        _MarkerDataGraphics.add(trailGraphic);
    }

    //todo removeTrail(TrailGraphicManager graphicManager)
    @Override
    public void removeTrail(TrailGraphicManager graphicManager) {

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


    @Override
    public void hideSelectedMarkerInfo() {
//        if (callout != null) {
//            callout.hide();
//            callout = null;
//        }
    }

//    @Override
//    public Position getMapLatLonCenter() {
//        return mapView.get;
//    }

//    private void showSelectedMarkerInfo(MarkerData markerData, Point point) {
//        if (callout != null) {
//            callout.hide();
//        } else {
//            callout = mapView.getCallout();
//        }
//
//        if (markerData != null) {
//            callout.show(point, TtUtils.ArcMap.createInfoWindow(inflater, markerData));
//        }
//    }




    //region GPS
    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        if (showPosition && nmeaBurst.hasPosition() && mapView != null) {
            Point point = new Point(nmeaBurst.getLongitudeSD(), nmeaBurst.getLatitudeSD(), SpatialReferences.getWgs84());

            if (_LocationLayer != null) {
                if (mapView.getGraphicsOverlays().get(mapView.getGraphicsOverlays().size() - 1) != _LocationLayer) {
                    mapView.getGraphicsOverlays().remove(_LocationLayer);

                    _LocationLayer = new GraphicsOverlay();
                    mapView.getGraphicsOverlays().add(_LocationLayer);
                }
            } else {
                _LocationLayer = new GraphicsOverlay();
                mapView.getGraphicsOverlays().add(_LocationLayer);
            }

            if (_LocationLayer.getGraphics().size() > 0) {
                _LocationLayer.getGraphics().clear();
            }

            if (sms == null) {
                sms = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.RED, 20);
            }

            _LocationLayer.getGraphics().add(new Graphic(point, sms));
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsAreValid) {

    }

    @Override
    public void receivingNmeaStrings(boolean receiving) {

    }

    @Override
    public void gpsStarted() {

    }

    @Override
    public void gpsStopped() {

    }

    @Override
    public void gpsServiceStarted() {

    }

    @Override
    public void gpsServiceStopped() {

    }

    @Override
    public void gpsError(GpsService.GpsError error) {

    }
    //endregion

    private class MapViewOnTouchListenerEx extends DefaultMapViewOnTouchListener {

        public MapViewOnTouchListenerEx(Context context, MapView mapView) {
            super(context, mapView);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            hideSelectedMarkerInfo();

            for (GraphicsOverlay go : mapView.getGraphicsOverlays()) {
                for (Graphic g : go.getGraphics()) {
                    g.setVisible(false);
                }

                go.setVisible(false);
            }

            boolean infoDisplayed = false;

            for (ArcGisTrailGraphic tGraphic : trailGraphics) {
                if (tGraphic.isMarkersVisible() && displayInfoWindow(tGraphic.getPtsLayer(), e)) {
                    infoDisplayed = true;
                    break;
                }
            }

            if (!infoDisplayed) {
                for (ArcGisPolygonGraphic pGraphic : polygonGraphics) {
                    if (pGraphic.isVisible()) {
                        if (pGraphic.isAdjBndPtsVisible() && displayInfoWindow(pGraphic.getAdjBndPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjBndPtsVisible() && displayInfoWindow(pGraphic.getUnadjBndPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isAdjNavPtsVisible() && displayInfoWindow(pGraphic.getAdjNavPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjNavPtsVisible() && displayInfoWindow(pGraphic.getUnadjNavPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isAdjMiscPtsVisible() && displayInfoWindow(pGraphic.getAdjMiscPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjMiscPtsVisible() && displayInfoWindow(pGraphic.getUnadjMiscPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isWayPtsVisible() && displayInfoWindow(pGraphic.getWayPtsLayer(), e)) {
                            infoDisplayed = true;
                            break;
                        }
                    }
                }
            }

            if (!infoDisplayed && mmListener != null) {
                Point pointLL = TtAppCtx.getArcGISTools().mapPointToLatLng((int)e.getX(), (int)e.getY(), mapView);

                mmListener.onMapClick(new Position(pointLL.getY(), pointLL.getX()));
            }

            return super.onSingleTapConfirmed(e);
        }

        boolean displayInfoWindow(GraphicsOverlay layer, MotionEvent me) {
            try {
//                layer.getSelectedGraphics();
//
//                int[] graphicIds = layer.getGraphicIDs(me.getX(), me.getY(), TOLERANCE, 1);
//
//                Graphic graphic = null;
//
//                if (graphicIds.length > 0) {
//                    graphic = layer.getGraphic(graphicIds[0]);
//                }
//
//                if (graphic != null) {
//                    onMarkerClick(graphicIds[0], graphic.getGeometry());
//                    return true;
//                }
            } catch (Exception e) {
                //getGraphicIDs throws IllegalStateException sometimes for no reason
            }

            return false;
        }
    }
}