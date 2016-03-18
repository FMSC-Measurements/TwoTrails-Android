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
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.objects.PointD;

public class ManagedSupportMapFragment extends SupportMapFragment implements IMultiMapFragment, OnMapReadyCallback {

    MultiMapListener mmlistener;

    GoogleMap map;


    public static ManagedSupportMapFragment newInstance() {
        return new ManagedSupportMapFragment();
    }

    public static ManagedSupportMapFragment newInstance(GoogleMapOptions options) {
        ManagedSupportMapFragment mapFragment = new ManagedSupportMapFragment();

        Bundle var2 = new Bundle();
        var2.putParcelable("MapOptions", options);
        mapFragment.setArguments(var2);

        return mapFragment;
    }

    public ManagedSupportMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        if (mmlistener != null) {
            mmlistener.onMapReady();
        }
    }

    @Override
    public void moveToLocation(float lat, float lon, boolean animate) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lon),
                Consts.GoogleMaps.ZOOM_CLOSE
        );

        if (animate) {
            map.animateCamera(cu);
        } else {
            map.moveCamera(cu);
        }

        if (mmlistener != null) {
            mmlistener.onMapLocationChanged();
        }
    }


    @Override
    public Position getLatLon() {
        LatLng ll = map.getCameraPosition().target;
        return new Position(ll.latitude, ll.longitude);
    }

    @Override
    public int getZoomLevel() {
        return 0;
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
