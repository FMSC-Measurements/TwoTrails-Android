package com.usda.fmsc.twotrails.activities.custom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
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
    Fragment mapFragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(FRAGMENT) && savedInstanceState.containsKey(MAP_TYPE)) {
            mapType = Units.MapType.parse(savedInstanceState.getInt(MAP_TYPE));
            mapFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT);
        } else {
            mapType = Global.Settings.DeviceSettings.getMapType();
            mapId = Global.Settings.DeviceSettings.getMapId();

            switch (mapType) {
                case Google:
                    // check google play services and setup map
                    Integer code = AndroidUtils.App.checkPlayServices(this, Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES);
                    if (code == null) {
                        mapFragment = getMapFragment(mapType, getMapOptions(mapId));
                        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
                    } else {
                        String str = GoogleApiAvailability.getInstance().getErrorString(code);
                        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ArcGIS:
                    mapFragment = getMapFragment(mapType, getMapOptions(mapId));
                    getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
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
                    if (mapFragment == null) {
                        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
                    } else {
                        mapFragment = getMapFragment(mapType, getMapOptions(mapId));
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

        if (mapFragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT, mapFragment);
            outState.putInt(MAP_TYPE, mapType.getValue());
        }
    }


    //add poly options (lines, polygons, points) to newInstance
    protected Fragment getMapFragment(Units.MapType mapType, IMultiMapFragment.MapOptions options) {
        Fragment f = null;
        switch (mapType) {
            case Google:
                f = options != null ?
                        ManagedSupportMapFragment.newInstance(options) :
                        ManagedSupportMapFragment.newInstance();
                break;
            case ArcGIS:
                f = options != null ?
                        ArcGisMapFragment.newInstance(options) :
                        ArcGisMapFragment.newInstance();
                break;
        }

        return f;
    }

    protected IMultiMapFragment.MapOptions getMapOptions(int terrainType) {
        Extent extents = null;

        if (mapFragment != null) {
            IMultiMapFragment f = ((IMultiMapFragment) mapFragment);
            extents = f.getExtents();
        } else {
            //extents = Global.Settings.DeviceSettings.getLastViewedExtents();

            if (extents == null) {
                return new IMultiMapFragment.MapOptions(terrainType, Consts.LocationInfo.USA_BOUNDS);
            }
        }

        return new IMultiMapFragment.MapOptions(terrainType, extents);
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
            if (mapFragment != null) {
                ((IMultiMapFragment) mapFragment).setMap(mapId);
            }
        } else {
            mapFragment = getMapFragment(mapType, getMapOptions(mapId));
            getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
            onMapTypeChanged(mapType, mapId);
        }

        Global.Settings.DeviceSettings.setMapType(mapType);
        Global.Settings.DeviceSettings.setMapId(mapId);
    }


    protected void moveToLocation(float lat, float lon) {
        moveToLocation(lat, lon, false);
    }

    protected void moveToLocation(float lat, float lon, boolean animate) {
        if (mapFragment != null) {
            ((IMultiMapFragment) mapFragment).moveToLocation(lat, lon, animate);
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
        //Global.Settings.DeviceSettings.setLastViewedExtents();
    }
}
