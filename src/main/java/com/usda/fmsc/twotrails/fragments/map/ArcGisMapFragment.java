package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPanListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.event.OnZoomListener;
import com.esri.android.toolkit.map.MapViewHelper;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Latlon;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.ui.ArcMapCompass;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;


public class ArcGisMapFragment extends Fragment implements IMultiMapFragment, OnStatusChangedListener, OnZoomListener, OnPanListener {
    private MapOptions startUpMapOptions;
    private MultiMapListener mmListener;

    private MapViewHelper mapViewHelper;

    private MapView mMapView;

    private Integer basemapId;
    private Layer mBasemapLayer;


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
        if (getArguments() != null) {
            startUpMapOptions = getArguments().getParcelable(MAP_OPTIONS_EXTRA);
        } else {
            startUpMapOptions = new MapOptions(0, Consts.LocationInfo.USA_BOUNDS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arc_gis_map, container, false);

        mMapView = (MapView)view.findViewById(R.id.map);
        mMapView.enableWrapAround(true);
        mMapView.setAllowRotationByPinch(true);

        mapViewHelper = new MapViewHelper(mMapView);

        //mMapView = (MapView) view.findViewById(R.id.mMapView);
        mMapView.setOnStatusChangedListener(this);
        mMapView.setOnZoomListener(this);
        mMapView.setOnPanListener(this);

        ArcMapCompass compass = (ArcMapCompass)view.findViewById(R.id.compass);
        compass.setMapView(mMapView);

        basemapId = startUpMapOptions.getMapId();

        mBasemapLayer = ArcGISTools.getMapLayer(basemapId);

        mMapView.addLayer(mBasemapLayer);

        if (startUpMapOptions.hasExtents() || startUpMapOptions.hasLocation()) {
            centerOnLoad = true;
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

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
                mmListener.onMapReady();
            }
        } else if (status == STATUS.INITIALIZED) {
            if (centerOnLoad) {

                if (startUpMapOptions.hasExtents()) {
                    Envelope e = ArcGISTools.getEnvelopFromLatLng(
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
            mBasemapLayer = ArcGISTools.getMapLayer(basemapId);
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
    public void moveToLocation(float lat, float lon, boolean animate) {
        mMapView.centerAt(lat, lon, animate);
        onMapLocationChanged();
    }

    @Override
    public void onMapLocationChanged() {
        if (mmListener != null) {
            mmListener.onMapLocationChanged();
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
}