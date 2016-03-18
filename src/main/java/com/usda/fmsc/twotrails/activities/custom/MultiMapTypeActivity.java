package com.usda.fmsc.twotrails.activities.custom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.dialogs.SelectMapTypeDialog;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.fragments.map.ManagedSupportMapFragment;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;

import java.util.ArrayList;

public class MultiMapTypeActivity extends CustomToolbarActivity implements IMultiMapFragment.MultiMapListener {
    private static final String FRAGMENT = "fragmet";
    private static final String MAP_TYPE = "mapType";

    private static final String SELECT_MAP = "selectMap";

    Units.MapType mapType = Units.MapType.None;
    int mapId;
    Fragment fragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(FRAGMENT) && savedInstanceState.containsKey(MAP_TYPE)) {
            mapType = Units.MapType.parse(savedInstanceState.getInt(MAP_TYPE));
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT);
        } else {
            mapType = Global.Settings.DeviceSettings.getMapType();
            mapId = Global.Settings.DeviceSettings.getMapId();

            switch (mapType) {
                case Google:
                    // check google play services and setup map
                    Integer code = AndroidUtils.App.checkPlayServices(this, Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES);
                    if (code == null) {
                        fragment = getMapFragment(mapType, getMapFragmentOptions(mapType, mapId));
                        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();
                    } else {
                        String str = GoogleApiAvailability.getInstance().getErrorString(code);
                        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ArcGIS:
                    fragment = getMapFragment(mapType, getMapFragmentOptions(mapType, mapId));
                    getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES: {
                if (resultCode == Activity.RESULT_OK) {
                    if (fragment == null) {
                        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();
                    } else {
                        fragment = getMapFragment(mapType, getMapFragmentOptions(mapType, mapId));
                    }
                }
            }

        }
    }



    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT, fragment);
            outState.putInt(MAP_TYPE, mapType.getValue());
        }
    }


    //add poly options (lines, polygons, points) to newInstance
    protected Fragment getMapFragment(Units.MapType mapType, Object options) {
        Fragment f = null;
        switch (mapType) {
            case Google:
                f = options != null ?
                        ManagedSupportMapFragment.newInstance((GoogleMapOptions)options) :
                        ManagedSupportMapFragment.newInstance();
                break;
            case ArcGIS:
                f = options != null ?
                        ArcGisMapFragment.newInstance((ArcGisMapFragment.ArcGisMapOptions)options) :
                        ArcGisMapFragment.newInstance();
                break;
        }

        return f;
    }

    protected Object getMapFragmentOptions(Units.MapType mapType, int terrainType) {
        Position pos = null;

        if (fragment != null) {
            pos = ((IMultiMapFragment)fragment).getLatLon();
        } else {
            if (Global.getGpsBinder().getLastPosition() != null) {
                pos = Global.getGpsBinder().getLastPosition();
            }
        }

        if (mapType == Units.MapType.Google) {
            GoogleMapOptions gmo = new GoogleMapOptions()
                    .mapType(terrainType);

            if (pos != null) {
                return gmo.camera(new CameraPosition(
                                new LatLng(pos.getLatitudeSignedDecimal(), pos.getLongitudeSignedDecimal()),
                                Consts.GoogleMaps.DEFAULT_ZOOM_CLOSE, 0, 0)
                );
            } else {
                return gmo;
            }
        } else if (mapType == Units.MapType.ArcGIS) {
            if (pos == null) {
                return new ArcGisMapFragment.ArcGisMapOptions(terrainType, null, null);
            } else {
                return new ArcGisMapFragment.ArcGisMapOptions(
                        terrainType,
                        pos.getLatitudeSignedDecimal(),
                        pos.getLongitudeSignedDecimal()
                );
            }
        }

        return null;
    }

    protected void selectMapType() {
        SelectMapTypeDialog dialog = SelectMapTypeDialog.newInstance(new ArrayList<>(ArcGISTools.getLayers()));

        dialog.setOnMapSelectedListener(new SelectMapTypeDialog.OnMapSelectedListener() {
            @Override
            public void mapSelected(Units.MapType mapType, int mapId) {
                setMapType(mapType, mapId);
            }
        });

        dialog.show(getSupportFragmentManager(), SELECT_MAP);
    }

    protected void setMapType(Units.MapType mapType, int mapId) {
        if (this.mapType == mapType) {
            if (fragment != null) {
                ((IMultiMapFragment)fragment).setMap(mapId);
            }
        } else {
            Fragment fragment = getMapFragment(mapType, getMapFragmentOptions(mapType, mapId));
            getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, fragment).commit();
            onMapTypeChanged(mapType, mapId);
        }

        Global.Settings.DeviceSettings.setMapType(mapType);
        Global.Settings.DeviceSettings.setMapId(mapId);
    }


    protected void moveToLocation(float lat, float lon) {
        moveToLocation(lat, lon, false);
    }

    protected void moveToLocation(float lat, float lon, boolean animate) {
        if (fragment != null) {
            ((IMultiMapFragment)fragment).moveToLocation(lat, lon, animate);
        }
    }



    @Override
    public void onMapReady() {

    }

    @Override
    public void onMapTypeChanged(Units.MapType mapType, int mapId) {
        this.mapType = mapType;
        this.mapId = mapId;
    }

    @Override
    public void onMapLocationChanged() {

    }
}
