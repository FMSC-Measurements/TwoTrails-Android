package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPanListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.event.OnZoomListener;
import com.esri.android.toolkit.map.MapViewHelper;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.objects.map.ArcGisPolygonGraphic;
import com.usda.fmsc.twotrails.objects.map.ArcGisTrailGraphic;
import com.usda.fmsc.twotrails.objects.map.IMarkerDataGraphic;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.ui.ArcMapCompass;
import com.usda.fmsc.twotrails.ui.MyPositionDrawable;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;


public class ArcGisMapFragment extends Fragment implements IMultiMapFragment, GpsService.Listener, OnStatusChangedListener, OnZoomListener, OnPanListener {
    private static final String START_ARC_OPTIONS = "StartupArcOptions";

    private static final int TOLERANCE = 30;

    private MapOptions startUpMapOptions;
    private MultiMapListener mmListener;

    private GpsService.GpsBinder binder;


    private ArrayList<IMarkerDataGraphic> _MarkerDataGraphics = new ArrayList<>();
    private ArrayList<ArcGisPolygonGraphic> polygonGraphics = new ArrayList<>();
    private ArrayList<ArcGisTrailGraphic> trailGraphics = new ArrayList<>();

    private MapView mMapView;
    private Callout callout;
    private ArcMapCompass compass;

    private Integer basemapId;
    private Layer mBasemapLayer;
    private GraphicsLayer locationLayer = new GraphicsLayer();

    private LayoutInflater inflater;

    private Graphic locationGraphic;

    private boolean mapReady, showPosition, centerOnLoad = false;
    private int gid;
    private SimpleMarkerSymbol sms;

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

        if (Global.Settings.DeviceSettings.isGpsConfigured()) {
            binder = Global.getGpsBinder();
            binder.addListener(this);
            binder.startGps();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arc_gis_map, container, false);

        mMapView = (MapView)view.findViewById(R.id.map);
        mMapView.enableWrapAround(true);
        mMapView.setAllowRotationByPinch(true);

        mMapView.setOnStatusChangedListener(this);
        mMapView.setOnZoomListener(this);
        mMapView.setOnPanListener(this);
        mMapView.setOnTouchListener(new TouchListener(getContext(), mMapView));

        compass = (ArcMapCompass)view.findViewById(R.id.compass);
        compass.setMapView(mMapView);

        if (startArcOpts != null) {
            changeBasemap(startArcOpts);
        } else {
            changeBasemap(startUpMapOptions.getMapId());
        }

        mMapView.addLayer(locationLayer);

        if (startUpMapOptions.hasExtents() || startUpMapOptions.hasLocation()) {
            centerOnLoad = true;
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (binder != null) {
            binder.removeListener(this);

            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
            }
        }

        // Release MapView resources
        mMapView.recycle();
        mMapView = null;
    }


    @Override
    public void onStatusChanged(Object o, STATUS status) {
        if (status == STATUS.LAYER_LOADED) {
            if (mmListener != null) {
                if (!mapReady) {
                    mmListener.onMapReady();
                    mapReady = true;
                }

                mmListener.onMapLoaded();
            }
        } else if (status == STATUS.INITIALIZED) {
            if (centerOnLoad) {

                if (startUpMapOptions.hasExtents()) {
                    Envelope e = ArcGISTools.getEnvelopFromLatLngExtents(
                            startUpMapOptions.getNorth(),
                            startUpMapOptions.getEast(),
                            startUpMapOptions.getSouth(),
                            startUpMapOptions.getWest(),
                            mMapView);

                    mMapView.setExtent(e, 0, false);
                } else {
                    mMapView.centerAt(startUpMapOptions.getLatitude(), startUpMapOptions.getLongitide(), false);
                }

                centerOnLoad = false;
            }
        }
    }

    @Override
    public void postPointerUp(float v, float v1, float v2, float v3) {
        onMapLocationChanged();
    }

    @Override
    public void postAction(float v, float v1, double v2) {
        onMapLocationChanged();
    }

    @Override
    public void postPointerMove(float v, float v1, float v2, float v3) { }
    @Override
    public void prePointerMove(float v, float v1, float v2, float v3) { }
    @Override
    public void prePointerUp(float v, float v1, float v2, float v3) { }
    @Override
    public void preAction(float v, float v1, double v2) { }


    private void changeBasemap(int basemapId) {
        changeBasemap(ArcGISTools.getMapLayer(basemapId));
    }

    public void changeBasemap(final ArcGisMapLayer agml) {

        if (mMapView == null) {
            mBasemapLayer = null;
        } else {
            try {
                Layer newLayer = ArcGISTools.getBaseLayer(agml);

                if (mBasemapLayer != null)
                    mMapView.removeLayer(mBasemapLayer);

                this.basemapId = agml.getId();
                this.currentGisMapLayer = agml;

                mBasemapLayer = newLayer;

                mMapView.addLayer(mBasemapLayer, 0);

                if (agml.hasScales()) {
                    mMapView.setMaxScale(agml.getMaxScale());
                    mMapView.setMinScale(agml.getMinScale());
                } else {
                    mMapView.setMaxScale(50);
                    mMapView.setMinScale(591657550.5);
                }

                if (mmListener != null) {
                    mmListener.onMapTypeChanged(Units.MapType.ArcGIS, basemapId);
                }

                if (currentGisMapLayer.isOnline() && currentGisMapLayer.getNumberOfLevels() < 1) {
                    AndroidUtils.Device.isInternetAvailable(new AndroidUtils.Device.InternetAvailableCallback() {
                        @Override
                        public void onCheckInternet(boolean internetAvailable) {
                            if (internetAvailable) {
                                ArcGISTools.getLayerFromUrl(agml.getUrl(), getActivity(), new ArcGISTools.IGetArcMapLayerListener() {
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
                                            ArcGISTools.updateMapLayer(agml);

                                            if (currentGisMapLayer.getId() == agml.getId()) {
                                                currentGisMapLayer = agml;
                                            }
                                        }
                                    }

                                    @Override
                                    public void onBadUrl() {

                                    }
                                });
                            }
                        }
                    });
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), "Unable to find offline map file.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void setMap(int mapId) {
        if (mapId != basemapId) {
            changeBasemap(mapId);
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {
        moveToLocation(lat, lon, -1, animate);
    }

    @Override
    public void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        if (animate) {
            float zLevel = -1;

            if (currentGisMapLayer != null && zoomLevel > -1 && currentGisMapLayer.getNumberOfLevels() > zoomLevel) {
                zLevel = (float)currentGisMapLayer.getLevelsOfDetail()[(int)zoomLevel].getResolution();
            }

            if (zLevel > -1) {
                mMapView.zoomToResolution(ArcGISTools.latLngToMapSpatial(lat, lon, mMapView), zLevel);
            } else {
                mMapView.centerAt(lat, lon, true);
            }
        } else {
            mMapView.centerAt(lat, lon, false);
        }
    }

    @Override
    public void moveToLocation(Extent extents, int padding, boolean animate) {
        mMapView.setExtent(ArcGISTools.getEnvelopFromLatLngExtents(extents, mMapView), padding, animate);
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
            if (Geometry.Type.POINT.equals(geometry.getType())) {
                Point point = (Point)geometry;
                showSelectedMarkerInfo(markerData, point);
                mMapView.centerAt(point, true);
            }

            if (mmListener != null) {
                mmListener.onMarkerClick(markerData);
            }
        }
    }

    @Override
    public Position getLatLon() {
        Point point = ArcGISTools.pointToLatLng(mMapView.getCenter(), mMapView);

        return new Position(point.getY(), point.getX());
    }

    @Override
    public Extent getExtents() {
        Polygon polygon = mMapView.getExtent();

        if (polygon != null) {
            Point ne = ArcGISTools.pointToLatLng(polygon.getPoint(1), mMapView);
            Point sw = ArcGISTools.pointToLatLng(polygon.getPoint(3), mMapView);

            return new Extent(sw.getY(), ne.getX(), ne.getY(), sw.getX());
        }

        return null;
    }

    public Envelope getArcExtents() {
        Envelope extents = new Envelope();
        mMapView.getExtent().queryEnvelope(extents);
        return extents;
    }

    public SpatialReference getSpatialReference() {
        return mMapView.getSpatialReference();
    }

    public double getScale() {
        return mMapView.getScale();
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mMapView != null) {
            mMapView.unpause();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMapView != null) {
            mMapView.pause();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MultiMapListener) {
            mmListener = (MultiMapListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mmListener = null;
    }

    @Override
    public void setLocationEnabled(boolean enabled) {
        if (locationLayer != null) {
            locationLayer.setVisible(enabled);
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
    }

    @Override
    public void setGesturesEnabled(boolean enabled) {
        mMapView.setEnabled(enabled);
    }

    @Override
    public void addPolygon(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        ArcGisPolygonGraphic polygonGraphic = new ArcGisPolygonGraphic(mMapView);
        polygonGraphics.add(polygonGraphic);

        graphicManager.setGraphic(polygonGraphic, drawOptions);

        _MarkerDataGraphics.add(polygonGraphic);
    }

    @Override
    public void addTrail(TrailGraphicManager graphicManager) {
        ArcGisTrailGraphic trailGraphic = new ArcGisTrailGraphic(mMapView);
        trailGraphics.add(trailGraphic);

        graphicManager.setGraphic(trailGraphic);

        _MarkerDataGraphics.add(trailGraphic);
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
        if(callout != null) {
            callout.hide();
            callout = null;
        }
    }

    private void showSelectedMarkerInfo(MarkerData markerData, Point point) {
        if (callout != null) {
            callout.hide();
        } else {
            callout = mMapView.getCallout();
        }

        if (markerData != null) {
            callout.show(point, TtUtils.ArcMap.createInfoWindow(inflater, markerData));
        }
    }


    //TODO update my position drawable
    @Override
    public void nmeaBurstReceived(INmeaBurst nmeaBurst) {
        if (showPosition && nmeaBurst.hasPosition()) {
            Point point = ArcGISTools.latLngToMapSpatial(nmeaBurst.getLatitude(), nmeaBurst.getLongitude(), mMapView);

            if (sms == null) {
                sms = new SimpleMarkerSymbol(Color.RED, 20, SimpleMarkerSymbol.STYLE.DIAMOND);
            }

            if (locationGraphic != null) {
                locationLayer.removeGraphic(gid);
            }

            locationGraphic = new Graphic(point, sms);

            gid = locationLayer.addGraphic(locationGraphic);
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

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




    private class TouchListener extends MapOnTouchListener {
        public TouchListener(Context context, MapView view) {
            super(context, view);
        }

        @Override
        public boolean onSingleTap(MotionEvent point) {
            hideSelectedMarkerInfo();

            boolean infoDisplayed = false;

            for (ArcGisTrailGraphic tGraphic : trailGraphics) {
                if (tGraphic.isMarkersVisible() && displayInfoWindow(tGraphic.getPtsLayer(), point)) {
                    infoDisplayed = true;
                    break;
                }
            }

            if (!infoDisplayed) {
                for (ArcGisPolygonGraphic pGraphic : polygonGraphics) {
                    if (pGraphic.isVisible()) {
                        if (pGraphic.isAdjBndPtsVisible() && displayInfoWindow(pGraphic.getAdjBndPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjBndPtsVisible() && displayInfoWindow(pGraphic.getUnadjBndPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isAdjNavPtsVisible() && displayInfoWindow(pGraphic.getAdjNavPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjNavPtsVisible() && displayInfoWindow(pGraphic.getUnadjNavPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isAdjMiscPtsVisible() && displayInfoWindow(pGraphic.getAdjMiscPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isUnadjMiscPtsVisible() && displayInfoWindow(pGraphic.getUnadjMiscPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }

                        if (pGraphic.isWayPtsVisible() && displayInfoWindow(pGraphic.getWayPtsLayer(), point)) {
                            infoDisplayed = true;
                            break;
                        }
                    }
                }
            }

            if (!infoDisplayed && mmListener != null) {
                Point pointLL = ArcGISTools.pointToLatLng(point.getX(), point.getY(), mMapView);

                mmListener.onMapClick(new Position(pointLL.getY(), pointLL.getX()));
            }

            return super.onSingleTap(point);
        }

        boolean displayInfoWindow(GraphicsLayer layer, MotionEvent point) {
            int[] graphicIds = layer.getGraphicIDs(point.getX(), point.getY(), TOLERANCE, 1);

            Graphic graphic = null;

            if (graphicIds.length > 0) {
                graphic = layer.getGraphic(graphicIds[0]);
            }

            if (graphic != null) {
                onMarkerClick(graphicIds[0],graphic.getGeometry());
                return true;
            }

            return false;
        }
    }
}