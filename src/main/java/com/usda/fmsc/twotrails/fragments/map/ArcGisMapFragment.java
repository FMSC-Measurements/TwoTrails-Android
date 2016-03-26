package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.google.android.gms.maps.model.Marker;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.ArcGisPolygonGraphic;
import com.usda.fmsc.twotrails.objects.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.PolygonGraphicManager;
import com.usda.fmsc.twotrails.ui.ArcMapCompass;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;


public class ArcGisMapFragment extends Fragment implements IMultiMapFragment, GpsService.Listener, OnStatusChangedListener, OnZoomListener, OnPanListener {
    private static final int TOLERANCE = 30;

    private MapOptions startUpMapOptions;
    private MultiMapListener mmListener;

    private GpsService.GpsBinder binder;

    private MapViewHelper mapViewHelper;

    private HashMap<String, MarkerData> _MarkerData = new HashMap<>();
    private ArrayList<ArcGisPolygonGraphic> polygonGraphics = new ArrayList<>();

    private MapView mMapView;
    private Callout callout;
    private ArcMapCompass compass;

    private Integer basemapId;
    private Layer mBasemapLayer;
    private GraphicsLayer locationLayer = new GraphicsLayer();

    private LayoutInflater inflater;

    private Graphic locationCircle;
    private float locGraphicRadius;

    private boolean mapReady, showPosition;


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

    boolean centerOnLoad = false;


    public ArcGisMapFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflater = LayoutInflater.from(getContext());

        if (getArguments() != null) {
            startUpMapOptions = getArguments().getParcelable(MAP_OPTIONS_EXTRA);
        } else {
            startUpMapOptions = new MapOptions(0, Consts.LocationInfo.USA_BOUNDS);
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

        mMapView.setMaxScale(1000);
        mMapView.setMinScale(591657550.5);

        mapViewHelper = new MapViewHelper(mMapView);

        //mMapView = (MapView) view.findViewById(R.id.mMapView);
        mMapView.setOnStatusChangedListener(this);
        mMapView.setOnZoomListener(this);
        mMapView.setOnPanListener(this);
        mMapView.setOnTouchListener(new TouchListener(getContext(), mMapView));


        compass = (ArcMapCompass)view.findViewById(R.id.compass);
        compass.setMapView(mMapView);

        basemapId = startUpMapOptions.getMapId();

        mBasemapLayer = ArcGISTools.getBaseLayer(basemapId);
        mMapView.addLayer(mBasemapLayer);

        mMapView.addLayer(locationLayer);

        locGraphicRadius = AndroidUtils.Convert.dpToPx(getContext(), 15);

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

        // Must remove our layers from MapView before calling recycle(), or we won't be able to reuse them
        //mMapView.removeLayer(mBasemapLayer); //wont need them

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
        this.basemapId = basemapId;

        if (mMapView == null) {
            mBasemapLayer = null;
        } else {
            // Remove old basemap layer and add a new one as the first layer to be drawn
            mMapView.removeLayer(mBasemapLayer);
            mBasemapLayer = ArcGISTools.getBaseLayer(basemapId);
            mMapView.addLayer(mBasemapLayer, 0);

            if (mmListener != null) {
                mmListener.onMapTypeChanged(Units.MapType.ArcGIS, basemapId);
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
    public void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        mMapView.centerAt(lat, lon, animate);
        onMapLocationChanged();
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

        if (Geometry.Type.POINT.equals(geometry.getType())) {
            Point point = (Point)geometry;
            showSelectedMarkerInfo(markerData, point);
            mMapView.centerAt(point, true);
        }

        if (mmListener != null) {
            mmListener.onMarkerClick(markerData);
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


        Point ne = ArcGISTools.pointToLatLng(polygon.getPoint(1), mMapView);
        Point sw = ArcGISTools.pointToLatLng(polygon.getPoint(3), mMapView);


        return new Extent(sw.getY(), ne.getX(), ne.getY(), sw.getX());
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
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MultiMapListener");
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
    public void addGraphic(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        ArcGisPolygonGraphic polygonGraphic = new ArcGisPolygonGraphic(mMapView);
        polygonGraphics.add(polygonGraphic);

        graphicManager.setPolygonGraphic(polygonGraphic, drawOptions);

        _MarkerData.putAll(graphicManager.getMarkerData());
    }

    @Override
    public MarkerData getMarkerData(String id) {
        return _MarkerData.get(id);
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

    Point lastPoint;
    int gid;

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        if (showPosition && nmeaBurst.hasPosition()) {
            Point point = ArcGISTools.latLngToMapSpatial(nmeaBurst.getLatitude(), nmeaBurst.getLongitude(), mMapView);

            SimpleLineSymbol outline = new SimpleLineSymbol(Color.WHITE, 1, SimpleLineSymbol.STYLE.SOLID);
            SimpleFillSymbol fill = new SimpleFillSymbol(Color.BLUE, SimpleFillSymbol.STYLE.SOLID);
            fill.setOutline(outline);

            Polygon polygon = new Polygon();

            int PointCount = 40; // number of points on the circle
            double var = 2 * Math.PI / PointCount;

            for (int i = 1; i <= PointCount; i++)
            {
                double radians = var * i;

                double x = (point.getX() + locGraphicRadius * Math.cos(radians));
                double y = (point.getY() + locGraphicRadius * Math.sin(radians));

                if (i == 1) {
                    polygon.startPath(x, y);
                } else {
                    polygon.lineTo(x, y);
                }
            }

            if (locationCircle != null) {
                locationLayer.removeGraphic(gid);
            }

            locationCircle = new Graphic(polygon, fill);

            gid = locationLayer.addGraphic(locationCircle);

            lastPoint = point;
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