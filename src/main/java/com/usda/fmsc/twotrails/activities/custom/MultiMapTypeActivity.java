package com.usda.fmsc.twotrails.activities.custom;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.dialogs.SelectMapTypeDialog;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.fragments.map.ManagedSupportMapFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.PolygonGraphicManager;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;

public class MultiMapTypeActivity extends CustomToolbarActivity implements IMultiMapFragment.MultiMapListener, GpsService.Listener {
    private static final String FRAGMENT = "fragmet";
    private static final String MAP_TYPE = "mapType";

    private static final String SELECT_MAP = "selectMap";

    GpsService.GpsBinder binder;

    Units.MapType mapType = Units.MapType.None;
    int mapId;
    Fragment mapFragment;
    IMultiMapFragment mmFrag;

    private ArrayList<PolygonGraphicManager> graphicManagers = new ArrayList<>();

    private GeoPosition lastPosition;
    private Location currentLocation;


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
                        mapFragment = getMapFragment(mapType, getMapOptions(mapType, mapId));
                        mmFrag = (IMultiMapFragment)mapFragment;
                        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
                    } else {
                        String str = GoogleApiAvailability.getInstance().getErrorString(code);
                        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ArcGIS:
                    mapFragment = getMapFragment(mapType, getMapOptions(mapType, mapId));
                    mmFrag = (IMultiMapFragment)mapFragment;
                    getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
                    break;
            }
        }

        binder = Global.getGpsBinder();
        binder.addListener(this);
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
                        mapFragment = getMapFragment(mapType, getMapOptions(mapType, mapId));
                        mmFrag = (IMultiMapFragment)mapFragment;
                    }
                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (binder != null) {
            binder.removeListener(this);

            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
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

    protected IMultiMapFragment.MapOptions getMapOptions(Units.MapType mapType, int terrainType) {
        Extent extents = null;

        if (mmFrag != null) {
            extents = mmFrag.getExtents();
        } else {
            return getMapStartLocation(mapType, terrainType);
        }

        return new IMultiMapFragment.MapOptions(terrainType, extents);
    }


    protected IMultiMapFragment.MapOptions getMapStartLocation(Units.MapType mapType, int terrainType) {
        Extent extents = null;
        //extents = Global.Settings.DeviceSettings.getLastViewedExtents();

        if (extents == null) {
            return new IMultiMapFragment.MapOptions(terrainType, Consts.LocationInfo.USA_BOUNDS, Consts.LocationInfo.PADDING);
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
            if (mmFrag != null) {
                mmFrag.setMap(mapId);
            }
        } else {
            mapFragment = getMapFragment(mapType, getMapOptions(mapType, mapId));
            mmFrag = (IMultiMapFragment)mapFragment;
            getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
            onMapTypeChanged(mapType, mapId);
        }

        Global.Settings.DeviceSettings.setMapType(mapType);
        Global.Settings.DeviceSettings.setMapId(mapId);
    }


    protected void moveToLocation(float lat, float lon) {
        moveToLocation(lat, lon, false);
    }

    protected void moveToLocation(Position position, boolean animate) {
        moveToLocation((float) position.getLatitudeSignedDecimal(), (float) position.getLongitudeSignedDecimal(), animate);
    }

    protected void moveToLocation(float lat, float lon, boolean animate) {
        moveToLocation(lat, lon, -1, animate);
    }

    protected void moveToLocation(float lat, float lon, float zoomLevel, boolean animate) {
        if (mmFrag != null) {
            mmFrag.moveToLocation(lat, lon, zoomLevel, animate);
        }
    }

    protected void moveToLocation(Extent extents, int padding, boolean animate) {
        if (mapFragment != null) {
            mmFrag.moveToLocation(extents, padding, animate);
        }
    }


    @Override
    public void onMapReady() {
        if (mmFrag != null) {
            for (PolygonGraphicManager pgm : graphicManagers) {
                mmFrag.addGraphic(pgm, null);
            }
        }
    }

    @Override
    public void onMapLoaded() {

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

    @Override
    public void onMapClick(Position position) {

    }

    @Override
    public void onMarkerClick(IMultiMapFragment.MarkerData markerData) {

    }



    protected void addGraphic(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        if (mmFrag != null) {
            mmFrag.addGraphic(graphicManager, drawOptions);
        }

        graphicManagers.add(graphicManager);
    }



    protected void setLocationEnabled(boolean enabled) {
        if (mmFrag != null) {
            mmFrag.setLocationEnabled(enabled);
        }
    }

    protected void setCompassEnabled(boolean enabled) {
        if (mmFrag != null) {
            mmFrag.setLocationEnabled(enabled);
        }
    }

    protected void hideSelectedMarkerInfo() {
        if (mmFrag != null) {
            mmFrag.hideSelectedMarkerInfo();
        }
    }

    //region GPS
    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        if (nmeaBurst.hasPosition()) {
            lastPosition = nmeaBurst.getPosition();

            if (currentLocation == null) {
                currentLocation = new Location(StringEx.Empty);
            } else {
                currentLocation.reset();
            }

            currentLocation.setLatitude(lastPosition.getLatitudeSignedDecimal());
            currentLocation.setLongitude(lastPosition.getLongitudeSignedDecimal());
            currentLocation.setAltitude(lastPosition.getElevation());
        }
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        switch (error) {
            case LostDeviceConnection:
                break;
            case NoExternalGpsSocket:
                break;
            case Unkown:
                break;
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {
        //
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
    //endregion


    public ArrayList<PolygonGraphicManager> getGraphicManagers() {
        return graphicManagers;
    }
}
