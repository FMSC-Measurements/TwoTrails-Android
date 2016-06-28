package com.usda.fmsc.twotrails.activities.base;

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
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.drawables.FadeBitmapProgressDrawable;
import com.usda.fmsc.android.widget.drawables.PolygonProgressDrawable;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.SettingsActivity;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.adapters.PolyMarkerMapRvAdapter;
import com.usda.fmsc.twotrails.dialogs.SelectMapTypeDialog;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.fragments.map.ManagedSupportMapFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.ui.UnadjustedDrawable;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.GoogleMapType;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class BaseMapActivity extends CustomToolbarActivity implements IMultiMapFragment.MultiMapListener, GpsService.Listener,
        SensorEventListener, PolyMarkerMapRvAdapter.Listener {

    private static final String FRAGMENT = "fragmet";
    private static final String MAP_TYPE = "mapType";
    private static final String SELECT_MAP = "selectMap";

    //region Vars
    private GpsService.GpsBinder binder;

    private MapType mapType = MapType.None;
    private int mapId;
    private Fragment mapFragment;
    private IMultiMapFragment mmFrag;

    private ArrayList<PolygonGraphicManager> polyGraphicManagers = new ArrayList<>();
    private ArrayList<TrailGraphicManager> trailGraphicManagers = new ArrayList<>();

    private GeoPosition lastPosition;
    private android.location.Location currentLocation, targetLocation;

    private DrawerLayout polyDrawer;

    private SensorManager mSensorManager;
    private Sensor accelerometer, magnetometer;

    private ActionBarDrawerToggle drawerToggle;
    private MenuItem miResetBounds, miShowMyPos, miTrackedPoly, miMapMaxBounds;
    private SlidingUpPanelLayout slidingLayout;
    private TextView tvNavPid, tvNavPoly, tvNavDistMt, tvNavDistFt, tvNavAzTrue, tvNavAzMag;
    private ImageView ivArrow;
    private Button btnFromPoly, btnFromPoint, btnToPoly, btnToPoint;

    private TtPolygon fromPoly, toPoly;
    private TtPoint fromPoint, toPoint;
    private boolean fromMyLoc = true;

    private boolean showCompass, mapMoved = true, showMyPos, polysCreated, mapHasMaxExtents;
    private MapTracking mapTracking = MapTracking.FOLLOW;
    private Integer zone;

    private float currentDirection;
    private float[] mGravity, mGeomagnetic;

    private HashMap<String, ArrayList<TtPoint>> polyPoints = new HashMap<>();
    private HashMap<String, TtPolygon> _Polygons = new HashMap<>();
    private HashMap<String, TtMetadata> _Metadata;

    private Extent completeBnds, trackedPoly;

    private boolean[] visd = new boolean[12];
    private boolean[] invisd = new boolean[12];
    private int[] dpc = new int[12];
    //endregion


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(FRAGMENT) && savedInstanceState.containsKey(MAP_TYPE)) {
            mapType = MapType.parse(savedInstanceState.getInt(MAP_TYPE));
            mapFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT);
        } else {
            mapType = Global.Settings.DeviceSettings.getMapType();
            mapId = Global.Settings.DeviceSettings.getMapId();

            AndroidUtils.Device.isInternetAvailable(new AndroidUtils.Device.InternetAvailableCallback() {
                @Override
                public void onCheckInternet(final boolean internetAvailable) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (mapType) {
                                case Google:
                                    if (internetAvailable || mapId == GoogleMapType.MAP_TYPE_NONE.getValue()) {
                                        // check google play services and setup map
                                        Integer code = AndroidUtils.App.checkPlayServices(BaseMapActivity.this, Consts.Codes.Services.REQUEST_GOOGLE_PLAY_SERVICES);
                                        if (code == 0) {
                                            startGMap();
                                        } else {
                                            String str = GoogleApiAvailability.getInstance().getErrorString(code);
                                            Toast.makeText(BaseMapActivity.this, str, Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        requestOfflineMap();
                                    }
                                    break;
                                case ArcGIS:
                                    ArcGisMapLayer agml = ArcGISTools.getMapLayer(mapId);
                                    if (agml == null) {
                                        mapId = 0;
                                        agml = ArcGISTools.getMapLayer(mapId);
                                    }

                                    if (agml.isOnline() && !internetAvailable) {
                                        requestOfflineMap();
                                    } else {
                                        startArcMap();
                                    }
                                    break;
                            }
                        }
                    });
                }
            });
        }

        super.setContentView(R.layout.activity_map_base);

        setupUI();

        setCompassEnabled(getShowCompass());
        setLocationEnabled(getShowMyPos());

        binder = Global.getGpsBinder();
        binder.addListener(this);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        getMapSettings();

        _Polygons = Global.getDAL().getPolygonsMap();

        for (TtPolygon poly : getPolygonsToMap()) {
            polyPoints.put(poly.getCN(), Global.getDAL().getPointsInPolygon(poly.getCN()));
            _Polygons.put(poly.getCN(), poly);
        }
    }

    private void setupUI() {
        RecyclerView rvPolyOptions = (RecyclerView)findViewById(R.id.mapRvPolyOptions);

        if (rvPolyOptions != null) {
            rvPolyOptions.setHasFixedSize(true);

            LinearLayoutManager llm = new LinearLayoutManager(this);
            rvPolyOptions.setLayoutManager(llm);

            PolyMarkerMapRvAdapter pmmAdapter = new PolyMarkerMapRvAdapter(this, getPolyGraphicManagers(), this);
            rvPolyOptions.setItemAnimator(new SlideInUpAnimator());
            rvPolyOptions.setAdapter(pmmAdapter);

            slidingLayout = (SlidingUpPanelLayout)findViewById(R.id.mapSlidingPanelLayout);
            slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View panel, float slideOffset) {

                }

                @Override
                public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                    if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED && targetLocation == null)
                        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                }
            });
        }

        if (Global.getDAL() != null) {
            _Metadata = Global.getDAL().getMetadataMap();
            zone = _Metadata.get(Consts.EmptyGuid).getZone();
        }

        tvNavPid = (TextView)findViewById(R.id.mapNavTvPid);
        tvNavPoly = (TextView)findViewById(R.id.mapNavTvPoly);
        ivArrow = (ImageView)findViewById(R.id.mapNavIbArrow);

        btnFromPoly = (Button)findViewById(R.id.mapNavBtnFromPoly);
        btnFromPoint = (Button)findViewById(R.id.mapNavBtnFromPoint);
        btnToPoly = (Button)findViewById(R.id.mapNavBtnToPoly);
        btnToPoint = (Button)findViewById(R.id.mapNavBtnToPoint);


        tvNavDistFt = (TextView)findViewById(R.id.mapNavTvDistFeet);
        tvNavDistMt = (TextView)findViewById(R.id.mapNavTvDistMeters);
        tvNavAzTrue = (TextView)findViewById(R.id.mapNavTvAzTrue);
        tvNavAzMag = (TextView)findViewById(R.id.mapNavTvAzMag);

        polyDrawer = (DrawerLayout)findViewById(R.id.mapNavDrawer);
    }


    private void startGMap() {
        mapFragment = createMapFragment(mapType, getMapOptions(mapType, mapId));
        mmFrag = (IMultiMapFragment)mapFragment;
        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
    }

    private void startArcMap() {
        mapFragment = createMapFragment(mapType, getMapOptions(mapType, mapId));
        mmFrag = (IMultiMapFragment)mapFragment;
        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
    }

    private void requestOfflineMap() {
        //use empty google map while user decides
        mapFragment = createMapFragment(MapType.Google, getMapOptions(MapType.Google, GoogleMapType.MAP_TYPE_NONE.getValue()));
        mmFrag = (IMultiMapFragment)mapFragment;
        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ArcGISTools.offlineMapsAvailable()) {
                    new AlertDialog.Builder(BaseMapActivity.this)
                            .setMessage("There is no internet connection. Would you like to use an offline map?")
                            .setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SelectMapTypeDialog.newInstance(new ArrayList<>(ArcGISTools.getMapLayers()),
                                            SelectMapTypeDialog.SelectMapMode.ARC_OFFLINE)
                                            .setOnMapSelectedListener(new SelectMapTypeDialog.OnMapSelectedListener() {
                                                @Override
                                                public void mapSelected(MapType mapType, int mapId) {
                                                    setMapType(mapType, mapId);
                                                }
                                            })
                                            .show(getSupportFragmentManager(), SELECT_MAP);
                                }
                            })
                            .setNegativeButton(R.string.str_no, null)
                            .show();
                } else {
                    new AlertDialog.Builder(BaseMapActivity.this)
                            .setMessage("There is no internet connection and there are no Offline maps are available. No Map can be displayed.")
                            .setPositiveButton(R.string.str_ok, null)
                            .show();
                }
            }
        });
    }

    @Override
    public void setContentView(int layoutResID) {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view = inflater.inflate(layoutResID, null);
        FrameLayout container = (FrameLayout)findViewById(R.id.contentContainer);

        if (view != null) {
            if (container != null) {
                container.addView(view);
            }

            setupToolbar(view);

            Toolbar toolbar = getToolbar();

            if (toolbar != null) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayShowTitleEnabled(false);
                }

                if (polyDrawer != null) {
                    drawerToggle = new ActionBarDrawerToggle(this, polyDrawer, toolbar, R.string.str_open, R.string.str_close);

                    polyDrawer.addDrawerListener(drawerToggle);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Consts.Codes.Services.REQUEST_GOOGLE_PLAY_SERVICES: {
                if (resultCode == Activity.RESULT_OK) {
                    startGMap();
                }
            }
            case Consts.Codes.Activites.SETTINGS: {
                getMapSettings();
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        miResetBounds = menu.findItem(R.id.mapMenuResetBounds);
        if (miResetBounds != null) {
            miResetBounds.setVisible(getMapTracking() != MapTracking.FOLLOW);
        }

        miTrackedPoly = menu.findItem(R.id.mapMenuZoomToPoly);
        if (miTrackedPoly != null) {
            miTrackedPoly.setVisible(getMapTracking() == MapTracking.POLY_BOUNDS);
        }

        miShowMyPos = menu.findItem(R.id.mapMenuShowMyPos);
        if (miShowMyPos != null) {
            miShowMyPos.setChecked(Global.Settings.DeviceSettings.getMapShowMyPos());
        }

        miMapMaxBounds = menu.findItem(R.id.mmMenuMoveToMaxBounds);
        if (miMapMaxBounds != null) {
            miMapMaxBounds.setVisible(false);

            if (mmFrag != null) {
                mapHasMaxExtents = mmFrag.mapHasMaxExtents();
            }

            miMapMaxBounds.setVisible(mapHasMaxExtents);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapMenuShowMyPos: {
                showMyPos = !showMyPos;
                Global.Settings.DeviceSettings.setMapShowMyPos(showMyPos);
                miShowMyPos.setChecked(showMyPos);

                if (getShowMyPos()) {
                    startGps();
                } else {
                    stopGps();
                }

                setLocationEnabled(getShowMyPos());
                break;
            }
            case R.id.mmMenuSelectMap: {
                selectMapType();
                break;
            }
            case R.id.mapMenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class).
                                putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.mmMenuMapSettings: {
                startActivityForResult(new Intent(this, SettingsActivity.class).
                                putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.MAP_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.mapMenuWhereIs: {
                calculateDir();
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                break;
            }
            case R.id.mmMenuMoveToMaxBounds: {
                if (mmFrag != null) {
                    mmFrag.moveToMapMaxExtents(true);
                }
                break;
            }
            case R.id.mapMenuResetBounds: {
                mapMoved = true;
                updateMapView(lastPosition);
                break;
            }
            case R.id.mapMenuZoomToPoly: {
                int gSize = getPolyGraphicManagers().size();
                if (gSize > 0) {
                    final String[] polyStrs = new String[polyPoints.size()];

                    for (int i = 0; i < gSize; i++) {
                        polyStrs[i] = getPolyGraphicManagers().get(i).getPolyName();
                    }

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                    dialogBuilder.setTitle("Track Polygon");

                    dialogBuilder.setItems(polyStrs, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PolygonGraphicManager pmm = getPolyGraphicManagers().get(which);
                            trackedPoly = pmm.getExtents();
                            Global.Settings.ProjectSettings.setTrackedPolyCN(pmm.getPolygonCN());
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
    public void onBackPressed() {
        if (polyDrawer.isDrawerOpen(GravityCompat.START)) {
            polyDrawer.closeDrawer(GravityCompat.START);
        } else {
            switch (slidingLayout.getPanelState()) {
                case EXPANDED:
                case ANCHORED: {
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    break;
                }
                case COLLAPSED: {
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    hideSelectedMarkerInfo();
                    break;
                }
                case HIDDEN: {
                    super.onBackPressed();
                    break;
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


    protected Fragment createMapFragment(MapType mapType, IMultiMapFragment.MapOptions options) {
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

    protected IMultiMapFragment.MapOptions getMapOptions(MapType mapType, int terrainType) {
        Extent extents;

        if (mmFrag != null) {
            extents = mmFrag.getExtents();
        } else {
            return getMapStartLocation(mapType, terrainType);
        }

        return new IMultiMapFragment.MapOptions(terrainType, extents != null ? extents : Consts.Location.USA_BOUNDS, Consts.Location.PADDING);
    }

    protected IMultiMapFragment.MapOptions getMapStartLocation(MapType mapType, int terrainType) {
        return new IMultiMapFragment.MapOptions(terrainType, Consts.Location.USA_BOUNDS, Consts.Location.PADDING);
    }


    //region Map Type and Move
    protected void selectMapType() {
        selectMapType(SelectMapTypeDialog.SelectMapMode.ALL);
    }

    protected void selectMapType(SelectMapTypeDialog.SelectMapMode mode) {
        SelectMapTypeDialog dialog = SelectMapTypeDialog.newInstance(new ArrayList<>(ArcGISTools.getMapLayers()), mode);

        dialog.setOnMapSelectedListener(new SelectMapTypeDialog.OnMapSelectedListener() {
            @Override
            public void mapSelected(MapType mapType, int mapId) {
                setMapType(mapType, mapId);
            }
        });

        dialog.show(getSupportFragmentManager(), SELECT_MAP);
    }

    protected void setMapType(MapType mapType, int mapId) {
        if (this.mapType == mapType) {
            if (mmFrag != null) {
                mmFrag.setMap(mapId);
            } else {
                throw new NullPointerException("MapFragment is null");
            }
        } else {
            if (mapType == MapType.Google && AndroidUtils.App.checkPlayServices(this, Consts.Codes.Services.REQUEST_GOOGLE_PLAY_SERVICES) != 0) {
                throw new RuntimeException("Play Services not available");
            }

            mapFragment = createMapFragment(mapType, getMapOptions(mapType, mapId));
            mmFrag = (IMultiMapFragment)mapFragment;
            getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
        }

        Global.Settings.DeviceSettings.setMapType(mapType);
        Global.Settings.DeviceSettings.setMapId(mapId);
    }


    protected void moveToLocation(Position position, boolean animate) {
        moveToLocation((float) position.getLatitudeSignedDecimal(), (float) position.getLongitudeSignedDecimal(), animate);
    }

    protected void moveToLocation(Position position, float zoomLevel, boolean animate) {
        moveToLocation((float) position.getLatitudeSignedDecimal(), (float) position.getLongitudeSignedDecimal(), zoomLevel, animate);
    }

    protected void moveToLocation(float lat, float lon) {
        moveToLocation(lat, lon, false);
    }

    protected void moveToLocation(float lat, float lon, boolean animate) {
        if (mmFrag != null) {
            mmFrag.moveToLocation(lat, lon, animate);
        }
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
    //endregion


    //region Map Events
    @Override
    public void onMapReady() {
        if (mmFrag != null) {
            for (PolygonGraphicManager pgm : polyGraphicManagers) {
                mmFrag.addPolygon(pgm, null);
            }
        }

        setCompassEnabled(getShowCompass());
        setLocationEnabled(getShowMyPos());

        if (!polysCreated) {
            setupPolygons();
            polysCreated = true;
        }

        setupUI();
    }

    @Override
    public void onMapLoaded() {

    }

    @Override
    public void onMapLocationChanged() {
        //Global.Settings.DeviceSettings.setLastViewedExtents();
    }

    @Override
    public void onMapClick(Position position) {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        hideSelectedMarkerInfo();
    }

    @Override
    public void onMarkerClick(IMultiMapFragment.MarkerData markerData) {
        TtPoint currentPoint = markerData.Point;

        targetLocation = TtUtils.getPointLocation(currentPoint, markerData.Adjusted, _Metadata);

        tvNavPid.setText(StringEx.toString(currentPoint.getPID()));
        tvNavPoly.setText(currentPoint.getPolyName());

        toPoint = currentPoint;
        toPoly = _Polygons.get(currentPoint.getPolyCN());
        btnToPoint.setText(StringEx.toString(toPoint.getPID()));
        btnToPoly.setText(toPoly.getName());
        calculateDir();

        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    protected void addPolygonGraphic(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        if (mmFrag != null) {
            mmFrag.addPolygon(graphicManager, drawOptions);
        }

        polyGraphicManagers.add(graphicManager);
    }

    protected void addTrailGraphic(TrailGraphicManager graphicManager) {
        if (mmFrag != null) {
            mmFrag.addTrail(graphicManager);
        }

        trailGraphicManagers.add(graphicManager);
    }

    @Override
    public void onMapTypeChanged(MapType mapType, int mapId, boolean isOnline) {
        this.mapType = mapType;
        this.mapId = mapId;

        setCompassEnabled(showCompass);
        setLocationEnabled(showMyPos);

        if (miMapMaxBounds != null && mmFrag != null) {
            mapHasMaxExtents = mmFrag.mapHasMaxExtents();
            miMapMaxBounds.setVisible(mapHasMaxExtents);
        }
    }
    //endregion


    //region Settings
    private void getMapSettings() {
        mapTracking = Global.Settings.DeviceSettings.getMapTrackingOption();
        showCompass = Global.Settings.DeviceSettings.getMapCompassEnabled();
        showMyPos = Global.Settings.DeviceSettings.getMapShowMyPos();

        if (miResetBounds != null) {
            miResetBounds.setVisible(getMapTracking() != MapTracking.FOLLOW);
        }

        if (miShowMyPos != null) {
            miShowMyPos.setChecked(getShowMyPos());
        }

        if (miTrackedPoly != null) {
            miTrackedPoly.setVisible(getMapTracking() == MapTracking.POLY_BOUNDS);
        }

        if (getShowMyPos()) {
            startGps();
        }

        getSettings();
    }

    protected void getSettings() {

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

    protected void setMapPadding(int left, int top, int right, int bottom) {
        if (mmFrag != null) {
            mmFrag.setMapPadding(left, top, right, bottom);
        }
    }

    protected void setMapGesturesEnabled(boolean enabled) {
        if (mmFrag != null) {
            mmFrag.setGesturesEnabled(enabled);
        }
    }

    public void setMapFollowMyPosition(boolean followPosition) {
        if (followPosition) {
            mapTracking = MapTracking.FOLLOW;
        } else {
            mapTracking = Global.Settings.DeviceSettings.getMapTrackingOption();
        }
    }

    //endregion


    protected void hideSelectedMarkerInfo() {
        if (mmFrag != null) {
            mmFrag.hideSelectedMarkerInfo();
        }
    }

    protected void onFirstPositionReceived(GeoPosition position) {
        updateMapView(position);
    }

    protected void onPositionReceived(GeoPosition position) {
        updateMapView(position);
    }

    private void updateMapView(GeoPosition position) {
        if (getMapTracking() == MapTracking.FOLLOW) {
            moveToLocation(position, true);
            mapMoved = true;
        }

        if (mapMoved) {
            if (getMapTracking() == MapTracking.POLY_BOUNDS && getTrackedPoly() != null) {
                moveToLocation(getTrackedPoly(), Consts.Location.PADDING, true);
                mapMoved = false;
            } else if (getMapTracking() == MapTracking.COMPLETE_BOUNDS && getCompleteBounds() != null) {
                moveToLocation(getCompleteBounds(), Consts.Location.PADDING, true);
                mapMoved = false;
            }
        }
    }


    //region GPS
    @Override
    public void nmeaBurstReceived(INmeaBurst nmeaBurst) {
        if (nmeaBurst.hasPosition()) {
            GeoPosition position = nmeaBurst.getPosition();

            if (currentLocation == null) {
                currentLocation = new android.location.Location(StringEx.Empty);
            } else {
                currentLocation.reset();
            }

            currentLocation.setLatitude(position.getLatitudeSignedDecimal());
            currentLocation.setLongitude(position.getLongitudeSignedDecimal());
            currentLocation.setAltitude(position.getElevation());

            if (lastPosition == null) {
                onFirstPositionReceived(position);
            } else {
                onPositionReceived(position);
            }

            if (fromMyLoc && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                calculateDir();
            }

            onNmeaBurstReceived(nmeaBurst);

            lastPosition = position;
        } else {
            onNmeaBurstReceived(nmeaBurst);
        }
    }

    protected void onNmeaBurstReceived(INmeaBurst nmeaBurst) {

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


    //region Sensors
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
    //endregion


    protected void onCreateGraphicManagers() {
        PolygonGraphicManager polygonGraphicManager;
        String trackedPolyCN = getTrackedPolyCN();

        for (TtPolygon polygon : getSortedPolys()) {
            polygonGraphicManager =  new PolygonGraphicManager(
                    polygon,
                    polyPoints.get(polygon.getCN()),
                    _Metadata,
                    Global.MapSettings.getPolyGraphicOptions(polygon.getCN()));

            addPolygonGraphic(polygonGraphicManager, Global.MapSettings.getPolyDrawOptions(polygon.getCN()));

            if (polygon.getCN().equals(trackedPolyCN)) {
                trackedPoly = polygonGraphicManager.getExtents();
            }
        }
    }


    //region Polygon Options
    private void setupPolygons() {
        onCreateGraphicManagers();

        if (getPolyGraphicManagers().size() > 0) {
            Extent.Builder builder = new Extent.Builder();
            Extent tmp;

            int usedBounds = 0;
            for (PolygonGraphicManager pgm : getPolyGraphicManagers()) {
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

    PostDelayHandler[] postDelayHandlers = new PostDelayHandler[12];

    private void setupMasterPolyControl() {
        for (int i = 0; i < 12; i++) {
            postDelayHandlers[i] = new PostDelayHandler(250);
        }

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

        tcbAdjBnd.setCheckBoxDrawable(new PolygonProgressDrawable(5, 90));
        tcbUnAdjBnd.setCheckBoxDrawable(new UnadjustedDrawable());

        tcbAdjNav.setCheckBoxDrawable(new PolygonProgressDrawable(5, 90));
        tcbUnAdjNav.setCheckBoxDrawable(new UnadjustedDrawable());

        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_polygon_pts_white_36dp);

        tcbAdjBndPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));
        tcbAdjNavPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));

        b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_unadjusted_polygon_pts_white_36dp);

        tcbUnAdjBndPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));
        tcbUnAdjNavPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));

        b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ttpoint_way_white);

        tcbWayPts.setCheckBoxDrawable(new FadeBitmapProgressDrawable(b));


        for (PolygonDrawOptions.DrawCode code : PolygonDrawOptions.DrawCode.values()) {
            onHolderOptionChanged(null, code);
        }

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
            public void onCheckedStateChanged(View buttonView, final boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.VISIBLE, isChecked);
            }
        });

        tcbAdjBnd.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.ADJBND, isChecked);
            }
        });

        tcbAdjNav.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.ADJNAV, isChecked);
            }
        });

        tcbUnAdjBnd.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.UNADJBND, isChecked);
            }
        });

        tcbUnAdjNav.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.UNADJNAV, isChecked);
            }
        });

        tcbAdjBndPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.ADJBNDPTS, isChecked);
            }
        });

        tcbAdjNavPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.ADJNAVPTS, isChecked);
            }
        });

        tcbUnAdjBndPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.UNADJBNDPTS, isChecked);
            }
        });

        tcbUnAdjNavPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.UNADJNAVPTS, isChecked);
            }
        });

        tcbAdjMiscPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.ADJMISCPTS, isChecked);
            }
        });

        tcbUnadjMiscPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.UNADJMISCPTS, isChecked);
            }
        });

        tcbWayPts.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                updatePolyOptions(PolygonDrawOptions.DrawCode.WAYPTS, isChecked);
            }
        });
    }

    //change master
    @Override
    public void onHolderOptionChanged(PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder holder, PolygonDrawOptions.DrawCode code) {
        boolean vis = false, invis = false;
        MultiStateTouchCheckBox tcb = null;
        int x = -1;

        try {
            for (PolygonGraphicManager map : getPolyGraphicManagers()) {

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
                    x = 0;
                    break;
                case ADJBND:
                    tcb = tcbAdjBnd;
                    x = 1;
                    break;
                case UNADJBND:
                    tcb = tcbUnAdjBnd;
                    x = 2;
                    break;
                case ADJBNDPTS:
                    tcb = tcbAdjBndPts;
                    x = 3;
                    break;
                case UNADJBNDPTS:
                    tcb = tcbUnAdjBndPts;
                    x = 4;
                    break;
                case ADJNAV:
                    tcb = tcbAdjNav;
                    x = 5;
                    break;
                case UNADJNAV:
                    tcb = tcbUnAdjNav;
                    x = 6;
                    break;
                case ADJNAVPTS:
                    tcb = tcbAdjNavPts;
                    x = 7;
                    break;
                case UNADJNAVPTS:
                    tcb = tcbUnAdjNavPts;
                    x = 8;
                    break;
                case ADJMISCPTS:
                    tcb = tcbAdjMiscPts;
                    x = 9;
                    break;
                case UNADJMISCPTS:
                    tcb = tcbUnadjMiscPts;
                    x = 10;
                    break;
                case WAYPTS:
                    tcb = tcbWayPts;
                    x = 11;
                    break;
            }

            if (x > -1) {
                final int xf = x;
                final PolygonDrawOptions.DrawCode codef = code;
                final MultiStateTouchCheckBox mscb = tcb;

                if (dpc[xf] == 0) {
                    if (postDelayHandlers[xf] != null) {
                        dpc[xf]++;
                        visd[xf] = vis;
                        invisd[xf] = invis;

                        postDelayHandlers[xf].post(new Runnable() {
                            @Override
                            public void run() {
                                setMasterPolyCheckBox(mscb, visd[xf], invisd[xf], codef);
                                dpc[xf] = 0;
                                visd[xf] = false;
                                invisd[xf] = false;
                            }
                        });
                    } else {
                        setMasterPolyCheckBox(mscb, visd[xf], invisd[xf], codef);
                    }
                } else {
                    dpc[xf]++;
                    visd[xf] &= vis;
                    invisd[xf] &= invis;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMasterPolyCheckBox(MultiStateTouchCheckBox tcb, boolean vis, boolean invis, PolygonDrawOptions.DrawCode code) {
        if (tcb != null) {
            if (vis && invis) {
                tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.PartialChecked);
                Global.MapSettings.getMasterPolyDrawOptions().setValue(code, true);
            } else {
                if (vis) {
                    tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.Checked);
                    Global.MapSettings.getMasterPolyDrawOptions().setValue(code, true);
                } else {
                    tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.NotChecked);
                    Global.MapSettings.getMasterPolyDrawOptions().setValue(code, false);
                }
            }
        }
    }

    //change options for all drawoptions and display
    private void updatePolyOptions(PolygonDrawOptions.DrawCode code, boolean value) {
        for (PolygonGraphicManager gm : getPolyGraphicManagers()) {
            gm.update(code, value);
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

            calculateDir();
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
        if (fromPoly == null || !polyPoints.containsKey(fromPoly.getCN())) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<TtPoint> points = polyPoints.get(fromPoly.getCN());

            if (points.size() > 0) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                dialogBuilder.setTitle(String.format("From Point in %s", fromPoly.getName()));
                ListView listView = new ListView(this);
                listView.setBackgroundColor(AndroidUtils.UI.getColor(BaseMapActivity.this, android.R.color.white));

                final PointDetailsAdapter pda = new PointDetailsAdapter(this, points, AppUnits.IconColor.Primary);
                pda.setShowPolygonName(true);
                @ColorInt int transparent = AndroidUtils.UI.getColor(BaseMapActivity.this, android.R.color.transparent);
                pda.setSelectedColor(transparent);
                pda.setNonSelectedColor(transparent);

                listView.setAdapter(pda);

                dialogBuilder.setView(listView);
                dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                final AlertDialog dialog = dialogBuilder.create();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        fromPoint = pda.getItem(i);
                        btnFromPoint.setText(StringEx.toString(fromPoint.getPID()));
                        calculateDir();
                        dialog.dismiss();
                    }
                });

                dialog.show();
            } else {
                fromPoint = null;
                calculateDir();

                Toast.makeText(this, "No Points in Polygon", Toast.LENGTH_SHORT).show();
            }
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
        if (toPoly == null || !polyPoints.containsKey(toPoly.getCN())) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<TtPoint> points = polyPoints.get(toPoly.getCN());

        if (points.size() > 0) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(String.format("To Point in %s", toPoly.getName()));
            ListView listView = new ListView(this);
            listView.setBackgroundColor(AndroidUtils.UI.getColor(BaseMapActivity.this, android.R.color.white));

            final PointDetailsAdapter pda = new PointDetailsAdapter(this, points, AppUnits.IconColor.Primary);
            pda.setShowPolygonName(true);
            @ColorInt int transparent = AndroidUtils.UI.getColor(BaseMapActivity.this, android.R.color.transparent);
            pda.setSelectedColor(transparent);
            pda.setNonSelectedColor(transparent);

            listView.setAdapter(pda);

            dialogBuilder.setView(listView);
            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    toPoint = pda.getItem(i);
                    btnToPoint.setText(StringEx.toString(toPoint.getPID()));
                    tvNavPid.setText(StringEx.toString(toPoint.getPID()));
                    tvNavPoly.setText(toPoint.getPolyName());

                    calculateDir();

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
            calculateDir();

            Toast.makeText(this, "No Points in Polygon", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    private void calculateDir() {
        if (toPoint != null) {
            UTMCoords currPos = null;

            if (fromMyLoc && currentLocation != null) {
                currPos = UTMTools.convertLatLonSignedDecToUTM(currentLocation.getLatitude(), currentLocation.getLongitude(), zone);
            } else if (!fromMyLoc && fromPoint != null){
                currPos = new UTMCoords(fromPoint.getUnAdjX(), fromPoint.getUnAdjY(), zone);
            }

            if (currPos != null) {
                double distInMt = TtUtils.Math.distance(currPos.getX(), currPos.getY(), toPoint.getUnAdjX(), toPoint.getUnAdjY());
                double distInFt = TtUtils.Convert.toFeetTenths(distInMt, Dist.Meters);

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
        for (TtPolygon poly : getPolygonsToMap()) {
            polys.add(poly);
        }

        Collections.sort(polys);

        return polys;
    }

    protected GeoPosition getLastPosition() {
        return lastPosition;
    }

    protected final Integer getZone() {
        return zone;
    }

    protected ArrayList<PolygonGraphicManager> getPolyGraphicManagers() {
        return polyGraphicManagers;
    }

    protected ArrayList<TrailGraphicManager> getTrailGraphicManagers() {
        return trailGraphicManagers;
    }


    protected final void startGps() {
        if (shouldStartGps()) {
            binder.startGps();
        }
    }

    public boolean shouldStartGps() {
        return getShowMyPos();
    }

    protected final void stopGps() {
        if (shouldStopGps()) {
            binder.startGps();
        }
    }

    public boolean shouldStopGps() {
        return !getShowMyPos();
    }

    protected final HashMap<String, TtPolygon> getPolygons() {
        return _Polygons;
    }

    protected Collection<TtPolygon> getPolygonsToMap() {
        return _Polygons.values();
    }

    protected String getTrackedPolyCN() {
        return Global.Settings.ProjectSettings.getTrackedPolyCN();
    }

    protected Extent getTrackedPoly() {
        return trackedPoly;
    }

    protected final HashMap<String, TtMetadata> getMetadata() {
        return _Metadata;
    }

    protected Extent getCompleteBounds() {
        return completeBnds;
    }

    protected boolean getShowCompass() {
        return showCompass;
    }

    protected boolean getShowMyPos() {
        return showMyPos;
    }

    protected MapTracking getMapTracking() {
        return mapTracking;
    }
}
