package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.usda.fmsc.android.adapters.MultiLineInfoWindowAdapter;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.usda.fmsc.android.widget.drawables.FadeBitmapProgressDrawable;
import com.usda.fmsc.android.widget.drawables.PolygonProgressDrawable;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.activities.custom.MultiMapTypeActivity;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.adapters.PolyMarkerMapRvAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.IPolygonGraphic;
import com.usda.fmsc.twotrails.objects.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.PolygonDrawOptions.GraphicCode;
import com.usda.fmsc.twotrails.objects.GoogleMapsPolygonGrahpic;
import com.usda.fmsc.twotrails.objects.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.ui.UnadjustedDrawable;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.utilities.StringEx;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class MapActivity extends MultiMapTypeActivity implements GpsService.Listener, SensorEventListener, PolyMarkerMapRvAdapter.Listener {

    private SensorManager mSensorManager;
    private Sensor accelerometer, magnetometer;

    private ActionBarDrawerToggle drawerToggle;
    private MenuItem miResetBounds, miShowMyPos, miTrackedPoly;
    private SlidingUpPanelLayout slidingLayout;
    private RecyclerView rvPolyOptions;
    private TextView tvNavPid, tvNavPoly, tvLocX, tvLocY, tvLocZone, tvLocXType, tvLocYType, tvZoneLbl,
                    tvNavDistMt, tvNavDistFt, tvNavAzTrue, tvNavAzMag;
    private ImageView ivArrow, ivGps;
    private FloatingActionButton fabMyPos;
    private Button btnFromPoly, btnFromPoint, btnToPoly, btnToPoint;

    TtPolygon fromPoly, toPoly;
    TtPoint fromPoint, toPoint;
    boolean fromMyLoc;

    private boolean myPosBtn, compass, dispLoc, locUtm;
    private boolean mapMoved = true, showMyPos, polysCreated;
    private Units.MapTracking mapTracking = Units.MapTracking.FOLLOW;
    private Integer zone;

    private boolean firstOpened;
    private float currentDirection;
    private float[] mGravity, mGeomagnetic;

    //private TtPoint currentPoint;
    private Location currentLocation, targetLocation;

    private HashMap<TtPolygon, ArrayList<TtPoint>> polyPoints;
    private HashMap<String, TtPolygon> polygons;
    private HashMap<String, TtMetadata> _Metadata;

    private GeoPosition lastPosition;
    private Extent completeBnds, trackedPoly;

    private PolyMarkerMapRvAdapter pmmAdapter;

    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayShowTitleEnabled(false);
        }

        rvPolyOptions = (RecyclerView)findViewById(R.id.mapRvPolyOptions);
        rvPolyOptions.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rvPolyOptions.setLayoutManager(llm);

        pmmAdapter = new PolyMarkerMapRvAdapter(this, getGraphicManagers(), this);
        rvPolyOptions.setItemAnimator(new SlideInUpAnimator());


        DrawerLayout polyDrawer = (DrawerLayout)findViewById(R.id.mapNavDrawer);

        drawerToggle = new ActionBarDrawerToggle(this, polyDrawer, getToolbar(),
                R.string.str_open, R.string.str_close){
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (!firstOpened) {
                    rvPolyOptions.setAdapter(pmmAdapter);

                    int size = getGraphicManagers().size();

                    if (size > 0) {
                        if (size < 2) {
                            pmmAdapter.notifyItemInserted(0);
                        } else {
                            pmmAdapter.notifyItemRangeInserted(0, size);
                        }
                    }

                    firstOpened = true;
                }
            }
        };

        polyDrawer.addDrawerListener(drawerToggle);

        slidingLayout = (SlidingUpPanelLayout)findViewById(R.id.mapSlidingPanelLayout);

        if (Global.DAL != null) {
            _Metadata = Global.DAL.getMetadataMap();
            zone = _Metadata.get(Consts.EmptyGuid).getZone();
        }

        tvNavPid = (TextView)findViewById(R.id.mapNavTvPid);
        tvNavPoly = (TextView)findViewById(R.id.mapNavTvPoly);
        ivArrow = (ImageView)findViewById(R.id.mapNavIbArrow);
        ivGps = (ImageView)findViewById(R.id.mapIvGps);

        tvLocX = (TextView)findViewById(R.id.mapTbTvLocX);
        tvLocXType = (TextView)findViewById(R.id.mapTbTvLocXType);
        tvLocY = (TextView)findViewById(R.id.mapTbTvLocY);
        tvLocYType = (TextView)findViewById(R.id.mapTbTvLocYType);
        tvLocZone = (TextView)findViewById(R.id.mapTbTvLocZone);
        tvZoneLbl = (TextView)findViewById(R.id.mapTbTvLocZoneLabel);

        fabMyPos = (FloatingActionButton)findViewById(R.id.mapFabMyPos);

        btnFromPoly = (Button)findViewById(R.id.mapNavBtnFromPoly);
        btnFromPoint = (Button)findViewById(R.id.mapNavBtnFromPoint);
        btnToPoly = (Button)findViewById(R.id.mapNavBtnToPoly);
        btnToPoint = (Button)findViewById(R.id.mapNavBtnToPoint);


        tvNavDistFt = (TextView)findViewById(R.id.mapNavTvDistFeet);
        tvNavDistMt = (TextView)findViewById(R.id.mapNavTvDistMeters);
        tvNavAzTrue = (TextView)findViewById(R.id.mapNavTvAzTrue);
        tvNavAzMag = (TextView)findViewById(R.id.mapNavTvAzMag);

        getSettings();



        // check google play services and setup map
//        Integer code = AndroidUtils.App.checkPlayServices(this, Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES);
//        if (code == null) {
//            startMap();
//        } else {
//            String str = GoogleApiAvailability.getInstance().getErrorString(code);
//            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
//        }

        //get Polys and init settings
        polyPoints = new HashMap<>();
        polygons = new HashMap<>();

        for (TtPolygon poly : Global.DAL.getPolygons()) {
            polyPoints.put(poly, Global.DAL.getPointsInPolygon(poly.getCN()));
            polygons.put(poly.getCN(), poly);
        }

        Global.MapSettings.init(new ArrayList<>(polyPoints.keySet()));

//        binder = Global.getGpsBinder();
//        binder.addListener(this, this);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (Global.Settings.DeviceSettings.isGpsConfigured()) {
            Global.getGpsBinder().startGps();
        }

        System.gc();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (map != null) {
//            map.setLocationSource(null);
//        }


//        if (binder != null) {
//            binder.removeListener(this);
//
//            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
//                binder.stopGps();
//            }
//        }

        System.gc();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);

        miResetBounds = menu.findItem(R.id.mapMenuResetBounds);
        miResetBounds.setVisible(mapTracking != Units.MapTracking.FOLLOW);

        miTrackedPoly = menu.findItem(R.id.mapMenuZoomToPoly);
        miTrackedPoly.setVisible(mapTracking == Units.MapTracking.POLY_BOUNDS);

        miShowMyPos = menu.findItem(R.id.mapMenuShowMyPos);
        miShowMyPos.setChecked(Global.Settings.DeviceSettings.getMapShowMyPos());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapMenuShowMyPos: {
                showMyPos = !showMyPos;
                Global.Settings.DeviceSettings.setMapShowMyPos(showMyPos);
                miShowMyPos.setChecked(showMyPos);

                if (showMyPos) {
                    Global.getGpsBinder().startGps();
                } else {
                    Global.getGpsBinder().stopGps();
                }

                setLocationEnabled(showMyPos);

//                if (map != null) {
//                    if (AndroidUtils.App.checkLocationPermission(this)) {
//                        map.setMyLocationEnabled(showMyPos);
//                    } else {
//                        Toast.makeText(this, "Unable to use location services.", Toast.LENGTH_LONG).show();
//                    }
//                }
                break;
            }
            case R.id.mmSelectMap: {
                selectMapType();
                break;
            }
            case R.id.mapMenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class).
                                putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Activities.SETTINGS);
                break;
            }
            case R.id.mapMenuMapSettings: {
                startActivityForResult(new Intent(this, SettingsActivity.class).
                                putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.MAP_SETTINGS_PAGE),
                        Consts.Activities.SETTINGS);
                break;
            }
            case R.id.mapMenuWhereIs: {
                fabMyPos.hide();
                calculate();
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                break;
            }
            case R.id.mapMenuResetBounds: {
                mapMoved = true;
                updateMapView(lastPosition);
                break;
            }
            case R.id.mapMenuZoomToPoly: {
                int gSize = getGraphicManagers().size();
                if (gSize > 0) {
                    final String[] polyStrs = new String[polyPoints.size()];

                    for (int i = 0; i < gSize; i++) {
                        polyStrs[i] = getGraphicManagers().get(i).getPolyName();
                    }

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                    dialogBuilder.setTitle("Track Polygon");

                    dialogBuilder.setItems(polyStrs, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PolygonGraphicManager pmm = getGraphicManagers().get(which);
                            trackedPoly = pmm.getExtents();
                            Global.Settings.ProjectSettings.setTrackedPolyCN(pmm.getId());
                            mapMoved = true;
                            updateMapView(null);
                        }
                    });

                    dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                    final AlertDialog dialog = dialogBuilder.create();

                    dialog.show();
                } else {
                    Toast.makeText(this, "No Polygons", Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Consts.Activities.SETTINGS: {
                getSettings();
                break;
            }
//            case Consts.Activities.Services.REQUEST_GOOGLE_PLAY_SERVICES: {
//                if (resultCode == Activity.RESULT_OK) {
//                    startMap();
//                }
//            }

        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        //call garbage collector
        System.gc();
    }

    @Override
    public void onBackPressed() {
        switch (slidingLayout.getPanelState()) {
            case EXPANDED:
            case ANCHORED: {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                break;
            }
            case COLLAPSED: {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
//                if (currentMarker != null) {
//                    currentMarker.hideInfoWindow();
//                    currentMarker = null;
//                    targetLocation = null;
//                }
                hideSelectedMarkerInfo();
                break;
            }
            case HIDDEN: {
                super.onBackPressed();
                break;
            }
        }
    }

    private void getSettings() {
        mapTracking = Global.Settings.DeviceSettings.getMapTrackingOption();
        myPosBtn = Global.Settings.DeviceSettings.getMapMyPosBtns();
        compass = Global.Settings.DeviceSettings.getMapCompassEnabled();
        dispLoc = Global.Settings.DeviceSettings.getMapDisplayGpsLocation();
        showMyPos = Global.Settings.DeviceSettings.getMapShowMyPos();
        locUtm = Global.Settings.DeviceSettings.getMapUseUtmNav();

        if (miResetBounds != null) {
            miResetBounds.setVisible(mapTracking != Units.MapTracking.FOLLOW);
        }

        if (miShowMyPos != null) {
            miShowMyPos.setChecked(showMyPos);
        }

        if (miTrackedPoly != null) {
            miTrackedPoly.setVisible(mapTracking == Units.MapTracking.POLY_BOUNDS);
        }

        setMapSettings();

//        if (binder == null) {
//            binder = Global.getGpsBinder();
//            binder.addListener(this, this);
//
//            if (showMyPos) {
//                binder.startGps();
//            }
//        }
    }
    //endregion

    @Override
    protected IMultiMapFragment.MapOptions getMapStartLocation(Units.MapType mapType, int terrainType) {
        //        try {
//            if (mapTracking == Units.MapTracking.FOLLOW && lastPosition != null) {
//                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
//                        new LatLng(lastPosition.getLatitudeSignedDecimal(), lastPosition.getLongitudeSignedDecimal()),
//                        Consts.LocationInfo.GoogleMaps.ZOOM_CLOSE
//                ));
//
//                mapMoved = true;
//            } else if (mapTracking == Units.MapTracking.POLY_BOUNDS ||
//                    mapTracking == Units.MapTracking.COMPLETE_BOUNDS) {
//                mapMoved = true;
//                updateMapView(null);
//            } else {
//                if (zone != null) {
//                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(TtUtils.GMap.getStartPosInZone(zone), 0));
//                } else {
//                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(Consts.LocationInfo.GoogleMaps.USA_BOUNDS, 0));
//                }
//
//                mapMoved = true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return super.getMapStartLocation(mapType, terrainType);
    }

    @Override
    public void onMapReady() {
        super.onMapReady();

        setCompassEnabled(compass);
        setLocationEnabled(showMyPos);

        if (!polysCreated) {
            setupPolygons();
            polysCreated = true;
        }
    }

    public void btnMyLocClick(View view) {
        //onMyLocationButtonClick();

        if (lastPosition == null) {
            lastPosition = Global.getGpsBinder().getLastPosition();
        }

        if (lastPosition != null) {
            moveToLocation(lastPosition, true);
        }
    }

    @Override
    public void onMapClick(Position position) {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        fabMyPos.show();
    }

    @Override
    public void onMarkerClick(IMultiMapFragment.MarkerData md) {
        TtPoint currentPoint = md.Point;

        targetLocation = TtUtils.getPointLocation(currentPoint, md.Adjusted, _Metadata);

        tvNavPid.setText(StringEx.toString(currentPoint.getPID()));
        tvNavPoly.setText(currentPoint.getPolyName());

        toPoint = currentPoint;
        toPoly = polygons.get(currentPoint.getPolyCN());
        btnToPoint.setText(StringEx.toString(toPoint.getPID()));
        btnToPoly.setText(toPoly.getName());
        calculate();

        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        fabMyPos.hide();
    }

    private void setMapSettings() {

        setCompassEnabled(compass);
        fabMyPos.setVisibility(myPosBtn ? View.VISIBLE : View.GONE);
        setLocationEnabled(showMyPos);

        setDisplayLocInfoVisible();
    }

    private void updateMapView(GeoPosition position) {
        if (mapTracking == Units.MapTracking.FOLLOW && position != null) {
            moveToLocation(position, true);
            mapMoved = true;
        }

        if (mapMoved) {
            if (mapTracking == Units.MapTracking.POLY_BOUNDS && trackedPoly != null) {
                moveToLocation(trackedPoly, Consts.LocationInfo.PADDING, true);
                mapMoved = false;
            } else if (mapTracking == Units.MapTracking.COMPLETE_BOUNDS && completeBnds != null) {
                moveToLocation(completeBnds, Consts.LocationInfo.PADDING, true);
                mapMoved = false;
            }
        }
    }
    //endregion


    //region GPS
    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

        if (nmeaBurst.hasPosition()) {
            updateMapView(lastPosition);

            setDisplayLocInfo();

            if (fromMyLoc && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                calculate();
            }
        }
    }
    //endregion



    @Override
    public void onMapTypeChanged(Units.MapType mapType, int mapId) {
        super.onMapTypeChanged(mapType, mapId);

        setCompassEnabled(compass);
        setLocationEnabled(showMyPos);
    }

    private void setDisplayLocInfo() {
        if (dispLoc && lastPosition != null) {
            if (locUtm) {
                UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(
                        lastPosition.getLatitudeSignedDecimal(),
                        lastPosition.getLongitudeSignedDecimal(),
                        zone);

                tvLocX.setText(String.format("%.3f", coords.getX()));
                tvLocY.setText(String.format("%.3f", coords.getY()));
            } else {
                tvLocX.setText(String.format("%.3f", lastPosition.getLatitudeSignedDecimal()));
                tvLocY.setText(String.format("%.3f", lastPosition.getLongitudeSignedDecimal()));
            }
        }
    }

    private void setDisplayLocInfoVisible() {
        int vis = dispLoc ? View.VISIBLE : View.INVISIBLE;

        if (dispLoc) {
            if (locUtm) {
                tvLocZone.setVisibility(View.VISIBLE);
                tvZoneLbl.setVisibility(View.VISIBLE);
                tvLocZone.setText(String.format("%d", zone));

                tvLocXType.setText(R.string.str_utmx);
                tvLocYType.setText(R.string.str_utmy);
            } else {
                tvLocZone.setVisibility(View.GONE);
                tvZoneLbl.setVisibility(View.GONE);

                tvLocXType.setText(R.string.str_lat);
                tvLocYType.setText(R.string.str_lon);
            }

            setDisplayLocInfo();
        }

        tvLocX.setVisibility(vis);
        tvLocY.setVisibility(vis);
        tvLocXType.setVisibility(vis);
        tvLocYType.setVisibility(vis);
        ivGps.setVisibility(vis);
    }

    private void calculate() {
        if (toPoint != null) {
            UTMCoords currPos = null;

            if (fromMyLoc && currentLocation != null) {
                currPos = UTMTools.convertLatLonSignedDecToUTM(currentLocation.getLatitude(), currentLocation.getLongitude(), zone);
            } else if (!fromMyLoc && fromPoint != null){
                currPos = new UTMCoords(fromPoint.getUnAdjX(), fromPoint.getUnAdjY(), zone);
            }

            if (currPos != null) {
                double distInMt = TtUtils.Math.distance(currPos.getX(), currPos.getY(), toPoint.getUnAdjX(), toPoint.getUnAdjY());
                double distInFt = TtUtils.Convert.toFeetTenths(distInMt, Units.Dist.Meters);

                double azimuth = TtUtils.Math.azimuthOfPoint(currPos.getX(), currPos.getY(), toPoint.getUnAdjX(), toPoint.getUnAdjY());
                double azMag = azimuth - _Metadata.get(toPoint.getMetadataCN()).getMagDec();

                tvNavDistFt.setText(StringEx.toString(distInFt, 2));
                tvNavDistMt.setText(StringEx.toString(distInMt, 2));
                tvNavAzTrue.setText(String.format("%.2f\u00B0", azimuth));
                tvNavAzMag.setText(String.format("%.2f\u00B0", azMag));
                return;
            }
        }

        tvNavDistFt.setText(StringEx.Empty);
        tvNavDistMt.setText(StringEx.Empty);
        tvNavAzTrue.setText(StringEx.Empty);
        tvNavAzMag.setText(StringEx.Empty);
    }


    private ArrayList<TtPolygon> getSortedPolys() {
        ArrayList<TtPolygon> polys = new ArrayList<>();
        for(TtPolygon poly : polygons.values()) {
            polys.add(poly);
        }

        Collections.sort(polys);

        return polys;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD: {
                mGeomagnetic = event.values;
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                mGravity = event.values;
                break;
            }
        }

        if (mGravity != null && mGeomagnetic != null && currentLocation != null && targetLocation != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float orientation[] = new float[9];
                SensorManager.getOrientation(R, orientation);

                float azimuth = Double.valueOf(Math.toDegrees(orientation[0])).floatValue();

                GeomagneticField geoField = new GeomagneticField(
                        (float) currentLocation.getLatitude(),
                        (float) currentLocation.getLongitude(),
                        (float) currentLocation.getAltitude(),
                        System.currentTimeMillis());

                azimuth += geoField.getDeclination(); // converts magnetic north into true north

                float direction = currentLocation.bearingTo(targetLocation) - azimuth;

                // create a rotation animation (reverse turn degree degrees)
                RotateAnimation ra = new RotateAnimation(
                        currentDirection,
                        direction,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);

                // how long the animation will take place
                ra.setDuration(210);

                // set the animation after the end of the reservation status
                ra.setFillAfter(true);

                // Start the animation
                ivArrow.startAnimation(ra);
                currentDirection = direction;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    //region Polygon Options
    private void setupPolygons() {
        HashMap<String, TtMetadata> meta = Global.DAL.getMetadataMap();

        PolygonGraphicManager polygonGraphicManager;
        String trackedPolyCN = Global.Settings.ProjectSettings.getTrackedPolyCN();

        IPolygonGraphic.PolygonGraphicOptions graphicOptions = new IPolygonGraphic.PolygonGraphicOptions(
                AndroidUtils.UI.getColor(this, R.color.red_500),
                AndroidUtils.UI.getColor(this, R.color.red_800),
                AndroidUtils.UI.getColor(this, R.color.indigo_500),
                AndroidUtils.UI.getColor(this, R.color.indigo_800),
                7,
                16
        );

        for (TtPolygon polygon : getSortedPolys()) {
            polygonGraphicManager =  new PolygonGraphicManager(
                    polygon,
                    polyPoints.get(polygon),
                    meta,
                    graphicOptions);

            addGraphic(polygonGraphicManager, Global.MapSettings.PolyOptions.get(polygon.getCN()));

            if (polygon.getCN().equals(trackedPolyCN)) {
                trackedPoly = polygonGraphicManager.getExtents();
            }
        }

        if (getGraphicManagers().size() > 0) {
            if (trackedPoly == null) {
                PolygonGraphicManager pgm = getGraphicManagers().get(0);
                trackedPoly = pgm.getExtents();
                Global.Settings.ProjectSettings.setTrackedPolyCN(pgm.getId());
            }

            Extent.Builder builder = new Extent.Builder();
            Extent tmp;

            int usedBounds = 0;
            for (PolygonGraphicManager pgm : getGraphicManagers()) {
                tmp = pgm.getExtents();

                if (tmp == null)
                    continue;

                builder.include(tmp.getNorthEast());
                builder.include(tmp.getSouthWest());
                
                usedBounds++;
            }

            if (usedBounds > 0) {
                completeBnds = builder.build();
            } else {
                completeBnds = null;
            }
        }

        setupMasterPolyControl();
    }


    View layHeader, layContent;
    MultiStateTouchCheckBox tcbPoly, tcbAdjBnd, tcbAdjNav, tcbUnAdjBnd, tcbUnAdjNav,
            tcbAdjBndPts, tcbAdjNavPts, tcbUnAdjBndPts, tcbUnAdjNavPts,
            tcbAdjMiscPts, tcbUnadjMiscPts, tcbWayPts;

    boolean masterCardExpanded;

    private void setupMasterPolyControl() {
        PolygonDrawOptions mopt = Global.MapSettings.MasterPolyOptions;

        layHeader = findViewById(R.id.mpcLayHeader);
        layContent = findViewById(R.id.mpcLayPolyContent);
        tcbPoly = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbPoly);

        tcbAdjBnd = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbAdjBnd);
        tcbAdjNav = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbAdjNav);
        tcbUnAdjBnd = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbUnadjBnd);
        tcbUnAdjNav = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbUnadjNav);
        tcbAdjBndPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbAdjBndPts);
        tcbAdjNavPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbAdjNavPts);
        tcbUnAdjBndPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbUnadjBndPts);
        tcbUnAdjNavPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbUnadjNavPts);
        tcbAdjMiscPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbAdjMiscPts);
        tcbUnadjMiscPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbUnadjMiscPts);
        tcbWayPts = (MultiStateTouchCheckBox)findViewById(R.id.mpcTcbWayPts);

        //ibBndMenu = (ImageButton)findViewById(R.id.mpcIbBndMenu);

        tcbAdjBnd.setCheckBoxDrawable(new PolygonProgressDrawable(5, 90));
        tcbUnAdjBnd.setCheckBoxDrawable(new UnadjustedDrawable());

        tcbAdjNav.setCheckBoxDrawable(new PolygonProgressDrawable(5, 90));
        tcbUnAdjNav.setCheckBoxDrawable(new UnadjustedDrawable());

        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_location_white_36dp);

        tcbAdjBndPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));
        tcbUnAdjBndPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));
        tcbAdjNavPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));
        tcbUnAdjNavPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));

        b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ttpoint_way_white);

        tcbWayPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));


        for (GraphicCode code : GraphicCode.values()) {
            onHolderOptionChanged(null, code);
        }
//        try {
//            Class c = PolygonDrawOptions.class;
//            for (Field f : c.getFields()) {
//                if (f.getType().equals(GraphicCode.class)) {
//                    onHolderOptionChanged(null, (GraphicCode)f.get(mopt));
//                }
//            }
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }

        layHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (masterCardExpanded) {
                    ViewAnimator.collapseView(layContent);
                    masterCardExpanded = false;
                } else {
                    ViewAnimator.expandView(layContent);
                    masterCardExpanded = true;
                }
            }
        });

        tcbPoly.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.VISIBLE, isChecked);
            }
        });

        tcbAdjBnd.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.ADJBND, isChecked);
            }
        });

        tcbAdjNav.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.ADJNAV, isChecked);
            }
        });

        tcbUnAdjBnd.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.UNADJBND, isChecked);
            }
        });

        tcbUnAdjNav.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.UNADJNAV, isChecked);
            }
        });

        tcbAdjBndPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.ADJBNDPTS, isChecked);
            }
        });

        tcbAdjNavPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.ADJNAVPTS, isChecked);
            }
        });

        tcbUnAdjBndPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.UNADJBNDPTS, isChecked);
            }
        });

        tcbUnAdjNavPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.UNADJNAVPTS, isChecked);
            }
        });

        tcbAdjMiscPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.ADJMISCPTS, isChecked);
            }
        });

        tcbUnadjMiscPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.UNADJMISCPTS, isChecked);
            }
        });

        tcbWayPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(GraphicCode.WAYPTS, isChecked);
            }
        });
    }

    //change master
    @Override
    public void onHolderOptionChanged(PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder holder, PolygonDrawOptions.GraphicCode code) {
        boolean vis = false, invis = false;

        MultiStateTouchCheckBox tcb = null;

        try {
            for (PolygonGraphicManager map : getGraphicManagers()) {

                boolean value = map.getDrawOptions().getValue(code);

                if (value) {
                    vis = true;
                } else {
                    invis = true;
                }

                if (vis && invis) {
                    break;
                }
            }

            switch (code) {
                case VISIBLE:
                    tcb = tcbPoly;
                    break;
                case ADJBND:
                    tcb = tcbAdjBnd;
                    break;
                case UNADJBND:
                    tcb = tcbUnAdjBnd;
                    break;
                case ADJBNDPTS:
                    tcb = tcbAdjBndPts;
                    break;
                case UNADJBNDPTS:
                    tcb = tcbUnAdjBndPts;
                    break;
                case ADJNAV:
                    tcb = tcbAdjNav;
                    break;
                case UNADJNAV:
                    tcb = tcbUnAdjNav;
                    break;
                case ADJNAVPTS:
                    tcb = tcbAdjNavPts;
                    break;
                case UNADJNAVPTS:
                    tcb = tcbUnAdjNavPts;
                    break;
                case ADJMISCPTS:
                    tcb = tcbAdjMiscPts;
                    break;
                case UNADJMISCPTS:
                    tcb = tcbUnadjMiscPts;
                    break;
                case WAYPTS:
                    tcb = tcbWayPts;
                    break;
            }


            if (tcb != null) {
                if (vis && invis) {
                    tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.PartialChecked);
                    Global.MapSettings.MasterPolyOptions.setValue(code, true);
                } else {
                    if (vis) {
                        tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.Checked);
                        Global.MapSettings.MasterPolyOptions.setValue(code, true);
                    } else {
                        tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.NotChecked);
                        Global.MapSettings.MasterPolyOptions.setValue(code, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //change options for all drawoptions and display
    private void updatePolyOptions(GraphicCode code, boolean value) {
        for (PolygonGraphicManager gm : getGraphicManagers()) {
            switch (code) {
                case VISIBLE:
                    gm.setVisible(value);
                    break;
                case ADJBND:
                    gm.setAdjBndVisible(value);
                    break;
                case UNADJBND:
                    gm.setUnadjBndVisible(value);
                    break;
                case ADJBNDPTS:
                    gm.setAdjBndPtsVisible(value);
                    break;
                case UNADJBNDPTS:
                    gm.setUnadjBndPtsVisible(value);
                    break;
                case ADJBNDCLOSE:
                    gm.setAdjBndClose(value);
                    break;
                case UNADJBNDCLOSE:
                    gm.setUnadjBndClose(value);
                    break;
                case ADJNAV:
                    gm.setAdjNavVisible(value);
                    break;
                case UNADJNAV:
                    gm.setUnadjNavVisible(value);
                    break;
                case ADJNAVPTS:
                    gm.setAdjNavPtsVisible(value);
                    break;
                case UNADJNAVPTS:
                    gm.setUnadjNavPtsVisible(value);
                    break;
                case ADJMISCPTS:
                    gm.setAdjMiscPtsVisible(value);
                    break;
                case UNADJMISCPTS:
                    gm.setUnadjMiscPtsVisible(value);
                    break;
                case WAYPTS:
                    gm.setWayPtsVisible(value);
                    break;
            }

            gm.onOptionChanged(code, value);
        }
    }
    //endregion


    //region Controls
    public void radFromClick(View view) {
        if (((RadioButton) view).isChecked()) {
            switch (view.getId()) {
                case R.id.mapNavRadPoint: {
                    fromMyLoc = false;

                    btnFromPoint.setEnabled(true);
                    btnFromPoly.setEnabled(true);
                    break;
                }
                case R.id.mapNavRadMyLoc: {
                    fromMyLoc = true;

                    btnFromPoint.setEnabled(false);
                    btnFromPoly.setEnabled(false);
                    break;
                }
            }

            calculate();
        }
    }

    public void btnFromPolyClick(View view) {
        if (polyPoints.size() > 0) {
            final String[] polyStrs = new String[polyPoints.size()];
            final ArrayList<TtPolygon> polys = getSortedPolys();

            int i = 0;
            for(TtPolygon poly : polys) {
                polyStrs[i] = poly.getName();
                i++;
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle("From Polygon");


            dialogBuilder.setItems(polyStrs, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fromPoly = polys.get(which);
                    fromPoint = null;
                    btnFromPoly.setText(fromPoly.getName());
                    btnFromPoint.setText(R.string.str_point);
                }
            });

            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            dialog.show();
        } else {
            Toast.makeText(this, "No Polygons", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnFromPointClick(View view) {
        if (fromPoly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<TtPoint> points = polyPoints.get(fromPoly);

        if (points.size() > 0) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(String.format("From Point in %s", fromPoly.getName()));
            ListView listView = new ListView(this);

            final PointDetailsAdapter pda = new PointDetailsAdapter(points, this, AppUnits.IconColor.Dark);
            pda.setShowPolygonName(true);

            listView.setAdapter(pda);

            dialogBuilder.setView(listView);
            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    fromPoint = pda.getPoint(i);
                    btnFromPoint.setText(StringEx.toString(fromPoint.getPID()));
                    calculate();
                    dialog.dismiss();
                }
            });

            dialog.show();
        } else {
            fromPoint = null;
            calculate();

            Toast.makeText(this, "No Points in Polygon", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnToPolyClick(View view) {
        if (polyPoints.size() > 0) {
            final String[] polyStrs = new String[polyPoints.size()];
            final ArrayList<TtPolygon> polys = getSortedPolys();

            int i = 0;
            for(TtPolygon poly : polys) {
                polyStrs[i] = poly.getName();
                i++;
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle("To Polygon");

            dialogBuilder.setItems(polyStrs, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    toPoly = polys.get(which);
                    toPoint = null;
                    btnToPoly.setText(toPoly.getName());
                    btnToPoint.setText(R.string.str_point);
                }
            });

            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            dialog.show();
        } else {
            Toast.makeText(this, "No Polygons", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnToPointClick(View view) {
        if (toPoly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<TtPoint> points = polyPoints.get(toPoly);

        if (points.size() > 0) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(String.format("To Point in %s", toPoly.getName()));
            ListView listView = new ListView(this);

            final PointDetailsAdapter pda = new PointDetailsAdapter(points, this, AppUnits.IconColor.Dark);
            pda.setShowPolygonName(true);

            listView.setAdapter(pda);

            dialogBuilder.setView(listView);
            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    toPoint = pda.getPoint(i);
                    btnToPoint.setText(StringEx.toString(toPoint.getPID()));
                    tvNavPid.setText(StringEx.toString(toPoint.getPID()));
                    tvNavPoly.setText(toPoint.getPolyName());

                    calculate();

//                    if (currentMarker != null) {
//                        currentMarker.hideInfoWindow();
//                    }

                    hideSelectedMarkerInfo();

                    targetLocation = TtUtils.getPointLocation(toPoint, false, _Metadata);

                    dialog.dismiss();
                }
            });

            dialog.show();
        } else {
            toPoint = null;
            calculate();

            Toast.makeText(this, "No Points in Polygon", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion
}
