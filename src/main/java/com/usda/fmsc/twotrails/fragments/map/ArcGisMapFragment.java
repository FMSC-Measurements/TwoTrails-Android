package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
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
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapScaleChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapScaleChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.toolkit.compass.Compass;
import com.esri.arcgisruntime.toolkit.scalebar.Scalebar;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.fragments.TtBaseFragment;
import com.usda.fmsc.twotrails.objects.map.ArcGisLineGraphic;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.objects.map.ArcGisPolygonGraphic;
import com.usda.fmsc.twotrails.objects.map.ArcGisTrailGraphic;
import com.usda.fmsc.twotrails.objects.map.IMarkerDataGraphic;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class ArcGisMapFragment extends TtBaseFragment implements IMultiMapFragment, MapRotationChangedListener, MapScaleChangedListener,
        ArcGISMap.BasemapChangedListener, LoadStatusChangedListener {
    private static final String START_ARC_OPTIONS = "StartupArcOptions";

    private static final int TOLERANCE = 30;

    private MapOptions startUpMapOptions;
    private MultiMapListener mmListener;

    private MapView mapView;
    private GraphicsOverlay _LocationLayer = new GraphicsOverlay();
    private Callout callout;

    private LayoutInflater inflater;

    private final ArrayList<IMarkerDataGraphic> _MarkerDataGraphics = new ArrayList<>();
    private final HashMap<String, ArcGisPolygonGraphic> polygonGraphics = new HashMap<>();
    private final HashMap<String, ArcGisTrailGraphic> trailGraphics = new HashMap<>();
    private final HashMap<String, ArcGisLineGraphic> lineGraphics = new HashMap<>();

    private Compass compass;
    private Integer basemapId;

    private boolean mapReady;
    private boolean showPosition;
    private int padLeft, padTop, padRight, padBottom;

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

        inflater = LayoutInflater.from(getContext());

        Bundle bundle = getArguments();

        if (bundle != null) {
            startUpMapOptions = bundle.getParcelable(MAP_OPTIONS_EXTRA);

            if (bundle.containsKey(START_ARC_OPTIONS)) {
                startArcOpts = bundle.getParcelable(START_ARC_OPTIONS);
            }
        } else {
            startUpMapOptions = new MapOptions(0, Consts.Location.USA_BOUNDS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arc_gis_map, container, false);

        mapView = view.findViewById(R.id.map);
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

        mapView.addMapRotationChangedListener(this);
        mapView.addMapScaleChangedListener(this);

        mapView.setOnTouchListener(new MapViewOnTouchListenerEx(getContext(), mapView));

        compass = view.findViewById(R.id.compass);
        compass.bindTo(mapView);

        Scalebar scalebar = view.findViewById(R.id.scalebar);
        scalebar.bindTo(mapView);

        if (startArcOpts != null) {
            changeBasemap(startArcOpts);
        } else {
            changeBasemap(startUpMapOptions.getMapId());
        }

        mapView.getGraphicsOverlays().add(_LocationLayer);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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

    private final Runnable baseMapDoneLoading = new Runnable() {
        @Override
        public void run() {
            if (mmListener != null) {
                if (!mapReady) {
                    mmListener.onMapReady();
                    mapReady = true;
                }

                mmListener.onMapLoaded();
            }
        }
    };

    @Override
    public void mapRotationChanged(MapRotationChangedEvent mapRotationChangedEvent) {
        //
    }

    @Override
    public void mapScaleChanged(MapScaleChangedEvent mapScaleChangedEvent) {
        //
    }


    private void changeBasemap(int basemapId) {
        changeBasemap(getTtAppCtx().getArcGISTools().getMapLayer(basemapId));
    }

    public void changeBasemap(final ArcGisMapLayer agml) {
        ArcGISMap mBasemapLayer;
        if (mapView != null) {
            try {
                ArcGISMap baseMap = getTtAppCtx().getArcGISTools().getBaseLayer(agml);

                baseMap.addBasemapChangedListener(this);
                baseMap.addLoadStatusChangedListener(this);
                baseMap.addDoneLoadingListener(baseMapDoneLoading);

                this.basemapId = agml.getId();
                this.currentGisMapLayer = agml;

                if (agml.getMinScale() < 0) {
                    baseMap.setMinScale(4622324);
                }

                if (agml.getMaxScale() > 1000 || agml.getMaxScale() < 0) {
                    baseMap.setMaxScale(1000);
                }

                mBasemapLayer = baseMap;

                mapView.setMap(mBasemapLayer);
                mapView.setPadding(padLeft, padTop, padRight, padBottom);

                if (mmListener != null) {
                    mmListener.onMapTypeChanged(MapType.ArcGIS, basemapId, agml.isOnline());
                }

                if (currentGisMapLayer.isOnline() && currentGisMapLayer.getNumberOfLevels() < 1) {
                    if (AndroidUtils.Device.isInternetAvailable(getContext())) {
                        getTtAppCtx().getArcGISTools().getLayerFromUrl(agml.getUrl(), getActivity(), new ArcGISTools.IGetArcMapLayerListener() {
                            @Override
                            public void onComplete(ArcGisMapLayer layer) {
                                boolean updated = false;

                                if (layer.hasDetailLevels()) {
                                    agml.setLevelsOfDetail(layer.getLevelsOfDetail());
                                    updated = true;
                                }

                                if (agml.getMaxScale() < 0 && layer.getMaxScale() >= 0) {
                                    agml.setMaxScale(layer.getMaxScale());
                                    agml.setMinScale(layer.getMinScale());
                                    updated = true;
                                }

                                if (updated) {
                                    getTtAppCtx().getArcGISTools().updateMapLayer(agml);

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
                }
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "ArcGisMapFragment:changeBasemap", e.getStackTrace());
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
    public void moveToLocation(double lat, double lon, boolean animate) {
        moveToLocation(lat, lon, -1, animate);
    }

    @Override
    public void moveToLocation(double lat, double lon, float zoomLevel, boolean animate) {
        if (mapView != null) {
            if (animate) {
                int zLevels = currentGisMapLayer.getNumberOfLevels();

                if (currentGisMapLayer != null && zLevels > 0 && zoomLevel > zLevels) {
                    zoomLevel = currentGisMapLayer.getNumberOfLevels();
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
        if (mapView != null && getTtAppCtx() != null) {
            mapView.setViewpointGeometryAsync(getTtAppCtx().getArcGISTools().getEnvelopFromLatLngExtents(extents, mapView), padding);
        }
    }


    private void onMarkerClick(String mdKey, Geometry geometry) {
        MarkerData markerData = getMarkerData(mdKey);

        if (markerData != null) {
            if (geometry.getGeometryType().equals(GeometryType.POINT)) {
                Point point = (Point)geometry;

                mapView.setViewpointCenterAsync(point);

                showSelectedMarkerInfo(markerData, point);
            }

            if (mmListener != null) {
                mmListener.onMarkerClick(markerData);
            }
        }
    }

    @Override
    public Extent getExtents() {
        Envelope envelope = getArcExtents();

        if (envelope != null) {
            envelope = (Envelope) GeometryEngine.project(envelope, SpatialReferences.getWgs84());

            return new Extent(envelope.getYMax(), envelope.getXMax(), envelope.getYMin(), envelope.getXMin());
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


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MultiMapListener) {
            mmListener = (MultiMapListener) context;
            if (currentGisMapLayer != null && mapReady) {
                mmListener.onMapTypeChanged(MapType.ArcGIS, this.basemapId, currentGisMapLayer.isOnline());
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
        padLeft = left;
        padTop = top;
        padRight = right;
        padBottom = bottom;

        if (mapView != null) {
            mapView.setPadding(padLeft, padTop, padRight, padBottom);
        }
    }

    @Override
    public void setGesturesEnabled(boolean enabled) {
        if (mapView != null) {
            mapView.setEnabled(enabled);
        }
    }

    @Override
    public void addPolygon(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        if (!polygonGraphics.containsKey(graphicManager.getPolygonCN())) {
            ArcGisPolygonGraphic polygonGraphic = new ArcGisPolygonGraphic(mapView);
            polygonGraphics.put(graphicManager.getPolygonCN(), polygonGraphic);

            graphicManager.setGraphic(polygonGraphic, drawOptions);

            _MarkerDataGraphics.add(polygonGraphic);
        }
    }

    //todo removePolygon(PolygonGraphicManager graphicManager)
    @Override
    public void removePolygon(PolygonGraphicManager graphicManager) {



    }

    @Override
    public void addTrail(TrailGraphicManager graphicManager) {
        if (!trailGraphics.containsKey(graphicManager.getPolygonCN())) {
            ArcGisTrailGraphic trailGraphic = new ArcGisTrailGraphic(mapView);
            trailGraphics.put(graphicManager.getPolygonCN(), trailGraphic);

            graphicManager.setGraphic(trailGraphic);

            _MarkerDataGraphics.add(trailGraphic);
        }
    }

    //todo removeTrail(TrailGraphicManager graphicManager)
    @Override
    public void removeTrail(TrailGraphicManager graphicManager) {

    }

    @Override
    public void addLine(LineGraphicManager graphicManager) {
        if (!lineGraphics.containsKey(graphicManager.getCN())) {
            ArcGisLineGraphic lineGraphic = new ArcGisLineGraphic(mapView);
            lineGraphics.put(graphicManager.getCN(), lineGraphic);

            graphicManager.setGraphic(lineGraphic);
        }
    }

    @Override
    public void removeLine(LineGraphicManager graphicManager) {

    }

    @Override
    public MarkerData getMarkerData(String key) {
        for (IMarkerDataGraphic mdg : _MarkerDataGraphics) {
            if (mdg.getMarkerData().containsKey(key)) {
                return mdg.getMarkerData().get(key);
            }
        }

        return null;
    }


    @Override
    public void hideSelectedMarkerInfo() {
        if (callout != null) {
            callout.dismiss();
            callout = null;
        }
    }

    private void showSelectedMarkerInfo(MarkerData markerData, Point point) {
        if (callout != null) {
            callout.dismiss();
        } else {
            callout = mapView.getCallout();
        }

        if (markerData != null) {
            callout.show(TtUtils.ArcMap.createInfoWindow(inflater, markerData), point);
        }
    }




    //region GPS
    public void updateLocation(Position position) {
        if (showPosition && mapView != null) {
            Point point = new Point(position.getLongitudeSignedDecimal(), position.getLatitudeSignedDecimal(), SpatialReferences.getWgs84());

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
                Graphic graphic = _LocationLayer.getGraphics().get(0);
                graphic.setGeometry(point);
            } else {
                _LocationLayer.getGraphics().add(new Graphic(point, new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.RED, 20)));
            }
        }
    }
    //endregion

    private class MapViewOnTouchListenerEx extends DefaultMapViewOnTouchListener {

        public MapViewOnTouchListenerEx(Context context, MapView mapView) {
            super(context, mapView);
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            hideSelectedMarkerInfo();

            boolean infoDisplayed = false;

            for (ArcGisTrailGraphic tGraphic : trailGraphics.values()) {
                if (tGraphic.isMarkersVisible() && displayInfoWindow(tGraphic.getPtsLayer(), e)) {
                    infoDisplayed = true;
                    break;
                }
            }

            if (!infoDisplayed) {
                for (ArcGisPolygonGraphic pGraphic : polygonGraphics.values()) {
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
                Point pointLL = getTtAppCtx().getArcGISTools().mapPointToLatLng((int)e.getX(), (int)e.getY(), mapView);

                mmListener.onMapClick(new Position(pointLL.getY(), pointLL.getX()));
            }

            return super.onSingleTapConfirmed(e);
        }

        boolean displayInfoWindow(GraphicsOverlay overlay, MotionEvent me) {
            try {
                final ListenableFuture<IdentifyGraphicsOverlayResult> identifyFuture = mMapView.identifyGraphicsOverlayAsync(overlay,
                        new android.graphics.Point((int)me.getX(), (int)me.getY()), TOLERANCE, false, 5);

                identifyFuture.addDoneListener(() -> {
                    try {
                        for (Graphic graphic : identifyFuture.get().getGraphics()) {
                            if (graphic.getGeometry().getGeometryType() == GeometryType.POINT && graphic.getAttributes().containsKey(MarkerData.ATTR_KEY)) {
                                onMarkerClick((String)graphic.getAttributes().get(MarkerData.ATTR_KEY), graphic.getGeometry());
                                break;
                            }
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        //
                    }

                });
            } catch (Exception e) {
                //
            }

            return false;
        }


    }
}