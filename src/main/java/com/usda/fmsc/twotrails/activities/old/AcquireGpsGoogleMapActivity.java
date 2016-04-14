package com.usda.fmsc.twotrails.activities.old;

import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.MultiLineInfoWindowAdapter;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;

public class AcquireGpsGoogleMapActivity extends AcquireGpsInfoMapActivity implements IAcquireMapActivity, GpsService.Listener, OnMapReadyCallback {
    private GoogleMap map;
    private ArrayList<Marker> _Markers;

    private int currentMapIndex = -1, mapOffsetY;
    boolean followPosition = false;

    @Override
    public void setupMap() {
        _Markers = new ArrayList<>();

        // check google play services and setup map
        Integer code = AndroidUtils.App.checkPlayServices(this, Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES);
        if (code == null) {
            startMap();
        } else {
            String str = GoogleApiAvailability.getInstance().getErrorString(code);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void startMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES &&
                resultCode == RESULT_OK) {
            startMap();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        GpsService.GpsBinder binder = Global.getGpsBinder();
        if (Global.Settings.DeviceSettings.isGpsConfigured() &&
                binder.getGpsProvider() == GpsService.GpsProvider.External) {
            googleMap.setLocationSource(binder.getService());
        }

        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.setInfoWindowAdapter(new MultiLineInfoWindowAdapter(this));
        googleMap.setPadding(0, AndroidUtils.Convert.dpToPx(this, 260), 0, 0);

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setAllGesturesEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setZoomControlsEnabled(false);

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Consts.LocationInfo.GoogleMaps.USA_CENTER, 3));
                googleMap.setOnCameraChangeListener(null);
            }
        });

        map = googleMap;
    }

    @Override
    public void moveToMapPoint(int position) {
        if (currentMapIndex != position && position < _Markers.size()) {
            currentMapIndex = position;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LatLng llp = _Markers.get(currentMapIndex).getPosition();

                    Point point = map.getProjection().toScreenLocation(llp);
                    point.offset(0, -mapOffsetY);
                    llp = map.getProjection().fromScreenLocation(point);

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(llp.latitude, llp.longitude),
                            Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE
                    ));
                }
            });
        }
    }


    @Override
    public void addMapMarker(TtPoint point, TtMetadata metadata) {
        addMapMarker(point, metadata, false);
    }

    @Override
    public void addMapMarker(TtPoint point, TtMetadata metadata, boolean moveToPointAfterAdd) {
        Marker marker;
        if (point.isGpsType()) {
            marker = map.addMarker(TtUtils.GMap.createMarkerOptions((GpsPoint)point, false, metadata));
        } else {
            marker = map.addMarker(TtUtils.GMap.createMarkerOptions((TravPoint)point, false, metadata));
        }

        if (marker != null) {
            _Markers.add(marker);

            if (moveToPointAfterAdd) {
                if (_Markers.size() == 1) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(marker.getPosition().latitude, marker.getPosition().longitude),
                            Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE
                    ));

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            moveToMapPoint(_Markers.size() - 1);
                        }
                    }, 250);
                } else {
                    moveToMapPoint(_Markers.size() - 1);
                }
            }
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        RelativeLayout gpsInfoLay = (RelativeLayout)findViewById(R.id.gpsInfoParent);
        mapOffsetY = gpsInfoLay.getHeight();
    }


    protected ArrayList<Marker> getMarkers() {
        return _Markers;
    }

    protected GoogleMap getMap() {
        return map;
    }

    @Override
    public void nmeaBurstReceived(final INmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

        if (followPosition && nmeaBurst.hasPosition()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(nmeaBurst.getLatitude(), nmeaBurst.getLongitude()),
                            Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE
                    ));
                }
            });
        }
    }

    @Override
    public void setMapMyLocationEnabled(boolean enabled) {
        if (AndroidUtils.App.checkFineLocationPermission(this)) {
            map.setMyLocationEnabled(enabled);
        }
    }

    @Override
    public void setMapFollowMyPosition(boolean followPosition) {
        this.followPosition = followPosition;
    }

    @Override
    public void setMapGesturesEnabled(boolean enabled) {
        map.getUiSettings().setAllGesturesEnabled(enabled);
    }
}
