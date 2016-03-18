package com.usda.fmsc.twotrails.fragments.map;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.esri.android.map.Layer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.toolkit.map.MapViewHelper;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.ui.ArcMapCompass;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;


public class ArcGisMapFragment extends Fragment implements IMultiMapFragment, OnStatusChangedListener {
    private static final String ARC_GIS_MAP_OPTIONS = "param1";

    private ArcGisMapOptions mapOptions;
    private MultiMapListener mmListener;

    private MapViewHelper mapViewHelper;

    private MapView mMapView;

    private Integer basemapId;
    private Layer mBasemapLayer;


    public static ArcGisMapFragment newInstance() {
        return new ArcGisMapFragment();
    }

    public static ArcGisMapFragment newInstance(ArcGisMapOptions options) {
        ArcGisMapFragment fragment = new ArcGisMapFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARC_GIS_MAP_OPTIONS, options);
        fragment.setArguments(args);
        return fragment;
    }

    public ArcGisMapFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mapOptions = getArguments().getParcelable(ARC_GIS_MAP_OPTIONS);
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


        ArcMapCompass compass = (ArcMapCompass)view.findViewById(R.id.compass);
        compass.setMapView(mMapView);


        if (mapOptions != null) {
            basemapId = mapOptions.BaseMap;

            //mMapView.centerAt(40.56, -105.08, false);
        }

        if (mBasemapLayer == null) {
            if (basemapId == null) {
                basemapId = 0;
            }

            mBasemapLayer = ArcGISTools.getMapLayer(basemapId);
        }

        mMapView.addLayer(mBasemapLayer);


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
            //mMapView.centerAt(40.56, -105.08, false);

            if (mmListener != null) {
                mmListener.onMapReady();
            }
        }
    }




    public void changeBasemap(int basemapId) {
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



    private void zoomToLevel(double lat, double lon, int level) {
        mMapView.setMapOptions(new MapOptions(MapOptions.MapType.SATELLITE,
                lat,
                lon,
                level)
        );
    }



    @Override
    public void setMap(int mapId) {
        if (mapId != basemapId) {
            changeBasemap(mapId);
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {

    }


    @Override
    public Position getLatLon() {
        return null;
    }


    @Override
    public int getZoomLevel() {
        return 0;
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


    public static class ArcGisMapOptions implements SafeParcelable {
        private int BaseMap;
        private Double Latitude, Longitude;

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            @Override
            public Object createFromParcel(Parcel source) {
                return new ArcGisMapOptions(source);
            }

            @Override
            public ArcGisMapOptions[] newArray(int size) {
                return new ArcGisMapOptions[size];
            }
        };


        public ArcGisMapOptions(Parcel in) {
            BaseMap = in.readInt();
            Latitude = in.readDouble();
            Longitude = in.readDouble();
        }

        public ArcGisMapOptions(int baseMap, Double latitude, Double longitude) {
            this.BaseMap = baseMap;
            this.Latitude = latitude;
            this.Longitude = longitude;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(BaseMap);
            dest.writeDouble(Latitude);
            dest.writeDouble(Longitude);
        }
    }
}