package com.usda.fmsc.twotrails.activities.custom;

import android.app.Activity;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
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

public class AcquireGpsMapActivity extends AcquireGpsCustomToolbarActivity implements GpsService.Listener, OnMapReadyCallback {
    private GoogleMap map;
    private ArrayList<Marker> _Markers;

    private int currentMapIndex = -1, mapOffsetY;
    boolean followPosition = false;


    protected void setupMap() {
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

    private void startMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES &&
                resultCode == Activity.RESULT_OK) {
            startMap();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        GpsService.GpsBinder binder = Global.getGpsBinder();
        if (Global.Settings.DeviceSettings.isGpsConfigured() &&
                binder.getGpsProvider() == GpsService.GpsProvider.External) {
            googleMap.setLocationSource(binder.getService());
        }

        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.getUiSettings().setAllGesturesEnabled(false);

        map = googleMap;

        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(Consts.GoogleMaps.USA_CENTER, 3));
                map.setOnCameraChangeListener(null);
            }
        });
    }

    protected void moveToMapPoint(int position) {
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
                            Consts.GoogleMaps.ZOOM_CLOSE
                    ));
                }
            });
        }
    }


    protected void addMarker(TtPoint point, TtMetadata metadata) {
        addMarker(point, metadata, false);
    }

    protected void addMarker(TtPoint point, TtMetadata metadata, boolean moveToPointAfterAdd) {
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
                            Consts.GoogleMaps.ZOOM_CLOSE
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

    protected void setMyLocationEnabled(boolean enabled) {
        if (AndroidUtils.App.checkFineLocationPermission(this)) {
            map.setMyLocationEnabled(enabled);
        }
    }

    protected void setFollowMyPosition(boolean followPosition) {
        this.followPosition = followPosition;
    }

    @Override
    public void nmeaBurstReceived(final NmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

        if (followPosition && nmeaBurst.hasPosition()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(nmeaBurst.getLatitude(), nmeaBurst.getLongitude()),
                            Consts.GoogleMaps.ZOOM_CLOSE
                    ));
                }
            });
        }
    }
}
