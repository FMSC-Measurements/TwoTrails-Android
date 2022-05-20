package com.usda.fmsc.twotrails.activities.base;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.drawables.FadeBitmapProgressDrawable;
import com.usda.fmsc.android.widget.drawables.PolygonProgressDrawable;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.SettingsActivity;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.adapters.PolyMarkerMapRvAdapter;
import com.usda.fmsc.twotrails.dialogs.SelectMapTypeDialog;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment.MarkerData;
import com.usda.fmsc.twotrails.fragments.map.ManagedSupportMapFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.map.IPolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
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
import com.usda.fmsc.twotrails.utilities.ClosestPositionCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.Tuple;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_OPEN;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNDEFINED;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED;

public abstract class BaseMapActivity extends ProjectAdjusterActivity implements IMultiMapFragment.MultiMapListener, GpsService.Listener,
        SensorEventListener, PolyMarkerMapRvAdapter.Listener, OnMapsSdkInitializedCallback {

    //region Lock and Gravity Defs
    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED_OPEN,
            LOCK_MODE_UNDEFINED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LockMode {}

    @IntDef({GravityCompat.START, GravityCompat.END})
    @Retention(RetentionPolicy.SOURCE)
    private @interface EdgeGravity {}
    //endregion

    private static final String FRAGMENT = "fragment";
    private static final String MAP_TYPE = "mapType";
    private static final String SELECT_MAP = "selectMap";

    //region Vars
    private MapType mapType = MapType.None;
    private int mapId;
    private Fragment mapFragment;
    private IMultiMapFragment mmFrag;

    private final ArrayList<PolygonGraphicManager> polyGraphicManagers = new ArrayList<>();
    private final ArrayList<TrailGraphicManager> trailGraphicManagers = new ArrayList<>();
    private final ArrayList<LineGraphicManager> lineGraphicManagers = new ArrayList<>();

    private Position lastPosition;
    private android.location.Location targetLocation;

    private DrawerLayout baseMapDrawer;

    private SensorManager mSensorManager;
    private Sensor accelerometer, magnetometer;

    private ActionBarDrawerToggle drawerToggle;
    private MenuItem miResetBounds, miShowMyPos, miTrackedPoly, miMapMaxBounds;
    private SlidingUpPanelLayout slidingLayout;
    private TextView tvNavPid, tvNavPoly, tvNavDistMt, tvNavDistFt, tvNavAzTrue, tvNavAzMag;
    private ImageView ivArrow;
    private Button btnFromPoly, btnFromPoint, btnToPoly, btnToPoint, btnToPoly2, btnToPoint2;
    private View layToLine;

    private TtPolygon _FromPoly, _ToPoly, _ToPoly2;
    private TtPoint _FromPoint, _ToPoint, _ToPoint2;
    private boolean _NavFromMyLoc = true, _NavToPoint = true, _ReceivingNmea;

    private boolean showCompass, mapMoved = true, showMyPos, polysCreated, mapHasMaxExtents, mapReady;
    private MapTracking mapTracking = MapTracking.FOLLOW;
    private Integer zone = null;

    private float currentDirection;
    private float[] mGravity, mGeomagnetic;

    private final HashMap<String, ArrayList<TtPoint>> polyPoints = new HashMap<>();
    private HashMap<String, TtPolygon> _Polygons = new HashMap<>();
    private HashMap<String, TtMetadata> _Metadata;
    private TtMetadata _DefaultMetadata;

    private Extent completeBnds;//, trackedPolyExtents;
    private IPolygonGraphicManager trackedPolyManager;

    private final boolean[] visd = new boolean[12];
    private final boolean[] invisd = new boolean[12];
    private final int[] dpc = new int[12];
    //endregion

    @Override
    public boolean requiresGpsService() {
        return true;
    }


    //region get/set
    
    protected HashMap<String, TtMetadata> getMetadata() {
        if (_Metadata == null || _Metadata.size() == 0) {
            _Metadata = getTtAppCtx().getDAL().getMetadataMap();
        }

        return _Metadata;
    }

    protected HashMap<String, TtPolygon> getPolygons() {
        if (_Polygons == null || _Polygons.size() == 0) {
            _Polygons = getTtAppCtx().getDAL().getPolygonsMap();
        }

        return _Polygons;
    }
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup new renderer
        MapsInitializer.initialize(getTtAppCtx(), MapsInitializer.Renderer.LATEST, this);

        if (savedInstanceState != null && savedInstanceState.containsKey(FRAGMENT) && savedInstanceState.containsKey(MAP_TYPE)) {
            mapType = MapType.parse(savedInstanceState.getInt(MAP_TYPE));
            mapFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT);
        } else {
            mapType = getTtAppCtx().getDeviceSettings().getMapType();
            mapId = getTtAppCtx().getDeviceSettings().getMapId();

            final boolean internetAvailable = AndroidUtils.Device.isInternetAvailable(getTtAppCtx());
            switch (mapType) {
                case Google:
                    if (internetAvailable || mapId == GoogleMapType.MAP_TYPE_NONE.getValue()) {
                        // check google play services and setup map
                        int code = AndroidUtils.App.checkPlayServices(BaseMapActivity.this, Consts.Codes.Services.REQUEST_GOOGLE_PLAY_SERVICES);
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
                    ArcGisMapLayer agml = getTtAppCtx().getArcGISTools().getMapLayer(mapId);
                    if (agml == null) {
                        mapId = 0;
                        agml = getTtAppCtx().getArcGISTools().getMapLayer(mapId);
                    }

                    if (agml.isOnline() && !internetAvailable) {
                        requestOfflineMap();
                    } else {
                        startArcMap();
                    }
                    break;
            }
        }

        if (getTtAppCtx().getDeviceSettings().getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        super.setContentView(R.layout.activity_map_base);

        setupUI();

        setCompassEnabled(getShowCompass());
        setLocationEnabled(getShowMyPos());

        if (AndroidUtils.Device.isFullOrientationAvailable(this)) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            if (mSensorManager != null) {
                accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            } else {
                getTtAppCtx().getReport().writeDebug("Sensors Not Found", "BaseMapActivity:onCreate");
            }
        }

        getMapSettings();
    }

    private void setupUI() {
        tvNavPid = findViewById(R.id.mapNavTvPid);
        tvNavPoly = findViewById(R.id.mapNavTvPoly);
        ivArrow = findViewById(R.id.mapNavIbArrow);

        btnFromPoly = findViewById(R.id.mapNavBtnFromPoly);
        btnFromPoint = findViewById(R.id.mapNavBtnFromPoint);
        btnToPoly = findViewById(R.id.mapNavBtnToPoly);
        btnToPoint = findViewById(R.id.mapNavBtnToPoint);
        btnToPoly2 = findViewById(R.id.mapNavBtnToPoly2);
        btnToPoint2 = findViewById(R.id.mapNavBtnToPoint2);
        layToLine = findViewById(R.id.mapNavLayToLine);

        tvNavDistFt = findViewById(R.id.mapNavTvDistFeet);
        tvNavDistMt = findViewById(R.id.mapNavTvDistMeters);
        tvNavAzTrue = findViewById(R.id.mapNavTvAzTrue);
        tvNavAzMag = findViewById(R.id.mapNavTvAzMag);

        baseMapDrawer = findViewById(R.id.mapDrawer);
    }

    private void setupPolygonOptionsUI() {
        RecyclerView rvPolyOptions = findViewById(R.id.mapRvPolyOptions);

        if (rvPolyOptions != null) {
            rvPolyOptions.setHasFixedSize(true);

            LinearLayoutManager llm = new LinearLayoutManager(this);
            rvPolyOptions.setLayoutManager(llm);

            PolyMarkerMapRvAdapter pmmAdapter = new PolyMarkerMapRvAdapter(this, getPolygonGraphicManagers(), this);
            rvPolyOptions.setItemAnimator(new SlideInUpAnimator());
            rvPolyOptions.setAdapter(pmmAdapter);

            slidingLayout = findViewById(R.id.mapSlidingPanelLayout);
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
    }

    private void startGMap() {
        mapFragment = createMapFragment(mapType, getMapOptions(mapType, mapId));
        mmFrag = (IMultiMapFragment)mapFragment;
        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
    }

    private void startArcMap() {
        try {
            mapFragment = createMapFragment(mapType, getMapOptions(mapType, mapId));
            mmFrag = (IMultiMapFragment)mapFragment;
            getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "BaseMapActivity:startArcMap", e.getStackTrace());
            setMapType(MapType.Google, GoogleMapType.MAP_TYPE_NONE.getValue());
            Toast.makeText(BaseMapActivity.this, "Error displaying ArcMap. See log for details.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestOfflineMap() {
        //use empty google map while user decides
        mapFragment = createMapFragment(MapType.Google, getMapOptions(MapType.Google, GoogleMapType.MAP_TYPE_NONE.getValue()));
        mmFrag = (IMultiMapFragment)mapFragment;
        if (mapFragment != null) {
            getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();

            runOnUiThread(() -> {
                final Runnable selectMap = () -> SelectMapTypeDialog.newInstance(new ArrayList<>(getTtAppCtx().getArcGISTools().getMapLayers()),
                        SelectMapTypeDialog.SelectMapMode.ARC_OFFLINE)
                        .setOnMapSelectedListener(this::setMapType)
                        .show(getSupportFragmentManager(), SELECT_MAP);

                if (getTtAppCtx().getArcGISTools().offlineMapsAvailable()) {
                    if (getTtAppCtx().getDeviceSettings().getAutoMapChooseOfflineAsk()) {
                        DontAskAgainDialog dialog = new DontAskAgainDialog(BaseMapActivity.this,
                                DeviceSettings.MAP_CHOOSE_OFFLINE_ASK,
                                DeviceSettings.MAP_CHOOSE_OFFLINE,
                                getTtAppCtx().getDeviceSettings().getPrefs());

                        dialog.setMessage("There is no internet connection. Would you like to use an offline map?")
                                .setPositiveButton(getString(R.string.str_yes), (dialogInterface, i, value) -> selectMap.run(), 2)
                                .setNegativeButton(getString(R.string.str_no), null, 1)
                                .show();
                    } else if (getTtAppCtx().getDeviceSettings().getAutoMapChooseOffline() == 0) {
                        selectMap.run();
                    }
                } else if (getTtAppCtx().getDeviceSettings().getAutoMapChooseOfflineAsk()) {
                    Toast.makeText(BaseMapActivity.this, "There is no internet connection and there are no Offline maps available. No Map can be displayed.",  Toast.LENGTH_LONG).show();
                }
            });
        } else {
            getTtAppCtx().getReport().writeWarn("Unable to create mapFragment", "BaseMapActivity:requestOfflineMap");
            Toast.makeText(BaseMapActivity.this, "Unable to create Map", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout container = findViewById(R.id.contentContainer);

        View view = inflater.inflate(layoutResID, container, false);

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

                if (baseMapDrawer != null) {
                    drawerToggle = new ActionBarDrawerToggle(this, baseMapDrawer, toolbar, R.string.str_open, R.string.str_close);

                    baseMapDrawer.addDrawerListener(drawerToggle);
                }
            }
        } else {
            getTtAppCtx().getReport().writeDebug("View not created", "BaseMapActivity:setContentView");
        }

        if (getMapRightDrawerLayoutId() != 0) {
            FrameLayout rightDrawer = findViewById(R.id.mapRightDrawer);
            view = inflater.inflate(getMapRightDrawerLayoutId(), container, false);
            if (view != null) {
                rightDrawer.addView(view);
            } else {
                setMapDrawerLockMode(LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            }
        } else {
            setMapDrawerLockMode(LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        }
    }

    protected int getMapRightDrawerLayoutId() {
        return 0;
    }

    protected void setMapDrawerLockMode(@LockMode int lockMode, @EdgeGravity int edgeGravity) {
        baseMapDrawer.setDrawerLockMode(lockMode, edgeGravity);
    }

    protected boolean isMapDrawerOpen(@EdgeGravity int edgeGravity) {
        return baseMapDrawer.isDrawerOpen(edgeGravity);
    }

    protected void openMapDrawer(@EdgeGravity int edgeGravity) {
        baseMapDrawer.openDrawer(edgeGravity);
    }

    protected void closeMapDrawer(@EdgeGravity int edgeGravity) {
        baseMapDrawer.closeDrawer(edgeGravity);
    }

    protected void addMapDrawerListener(DrawerLayout.DrawerListener listener) {
        baseMapDrawer.addDrawerListener(listener);
    }

    protected void removeMapDrawerListener(DrawerLayout.DrawerListener listener) {
        baseMapDrawer.removeDrawerListener(listener);
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
            case Consts.Codes.Activities.SETTINGS: {
                getMapSettings();
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        miResetBounds = menu.findItem(R.id.mapMenuResetBounds);
        if (miResetBounds != null) {
            miResetBounds.setVisible(getMapTracking() != MapTracking.FOLLOW);
        }

        miTrackedPoly = menu.findItem(R.id.mapMenuZoomToPoly);
        if (miTrackedPoly != null) {
            miTrackedPoly.setVisible(true);//getMapTracking() == MapTracking.POLY_BOUNDS);
        }

        miShowMyPos = menu.findItem(R.id.mapMenuShowMyPos);
        if (miShowMyPos != null) {
            miShowMyPos.setChecked(getTtAppCtx().getDeviceSettings().getMapShowMyPos());
        }

        MenuItem miZoomToPoly = menu.findItem(R.id.mapMenuZoomToPoly);
        if (miZoomToPoly != null) {
            miZoomToPoly.setVisible(getPolygons().size() > 0);
        }

        miMapMaxBounds = menu.findItem(R.id.mmMenuMoveToMaxBounds);
        if (miMapMaxBounds != null) {
            miMapMaxBounds.setVisible(false);

            if (mmFrag != null) {
                mapHasMaxExtents = mmFrag.mapHasMaxExtents();
            }

            miMapMaxBounds.setVisible(mapHasMaxExtents);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.mapMenuShowMyPos) {
            showMyPos = !showMyPos;
            getTtAppCtx().getDeviceSettings().setMapShowMyPos(showMyPos);
            miShowMyPos.setChecked(showMyPos);

            setLocationEnabled(getShowMyPos());
        } else if (itemId == R.id.mmMenuSelectMap) {
            selectMapType();
        } else if (itemId == R.id.mapMenuGps) {
            openSettings(SettingsActivity.GPS_SETTINGS_PAGE);
        } else if (itemId == R.id.mmMenuMapSettings) {
            openSettings(SettingsActivity.MAP_SETTINGS_PAGE);
        } else if (itemId == R.id.mapMenuWhereIs) {
            calculateDir();
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        } else if (itemId == R.id.mmMenuMoveToMaxBounds) {
            if (mmFrag != null) {
                mmFrag.moveToMapMaxExtents(true);
            }
        } else if (itemId == R.id.mapMenuResetBounds) {
            resetMapBounds(true);
        } else if (itemId == R.id.mapMenuZoomToPoly) {
            ArrayList<IPolygonGraphicManager> managers = getIPolygonGraphicManagers();
            int gSize = managers.size();
            if (gSize > 0) {
                if (gSize > 1) {
                    final String[] polyStrs = new String[managers.size()];

                    for (int i = 0; i < gSize; i++) {
                        polyStrs[i] = managers.get(i).getPolyName();
                    }

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                    dialogBuilder.setTitle("Track Polygon");

                    dialogBuilder.setItems(polyStrs, (dialog, which) -> {
                        trackedPolyManager = managers.get(which);
                        getTtAppCtx().getProjectSettings().setTrackedPolyCN(trackedPolyManager.getPolygonCN());
                        mapMoved = true;
                        moveToLocation(trackedPolyManager.getExtents(), Consts.Location.PADDING, true);
                    });

                    dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                    final AlertDialog dialog = dialogBuilder.create();

                    dialog.show();
                } else {
                    trackedPolyManager = getIPolygonGraphicManagers().get(0);
                    getTtAppCtx().getProjectSettings().setTrackedPolyCN(trackedPolyManager.getPolygonCN());
                    mapMoved = true;
                    moveToLocation(trackedPolyManager.getExtents(), Consts.Location.PADDING, true);
                }
            } else {
                Toast.makeText(this, "No Polygons", Toast.LENGTH_SHORT).show();
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
    protected void onPause() {
        super.onPause();

        if (mSensorManager != null) {
            // to stop the listener and save battery
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSensorManager != null) {
            try {
                mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getTtAppCtx().getDeviceSettings().getGpsExternal()) {
            if (getTtAppCtx().getDeviceSettings().isGpsConfigured())
            {
                if (!getTtAppCtx().getGps().isGpsRunning()) {
                    Toast.makeText(BaseMapActivity.this, "GPS is not Receiving", Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                Toast.makeText(BaseMapActivity.this, "GPS is not Configured", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (baseMapDrawer.isDrawerOpen(GravityCompat.START)) {
            baseMapDrawer.closeDrawer(GravityCompat.START);
        } else if (slidingLayout != null) {
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
        } else {
            if (getTtAppCtx().hasReport()) {
                getTtAppCtx().getReport().writeDebug("SlidingLayout not initialized", "BaseMapActivity:onBackPressed");
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        return new IMultiMapFragment.MapOptions(terrainType, mmFrag != null ? mmFrag.getExtents() : Consts.Location.USA_BOUNDS, Consts.Location.PADDING);
    }


    //region Map Type and Move
    protected void selectMapType() {
        selectMapType(SelectMapTypeDialog.SelectMapMode.ALL);
    }

    protected void selectMapType(SelectMapTypeDialog.SelectMapMode mode) {
        SelectMapTypeDialog dialog = SelectMapTypeDialog.newInstance(new ArrayList<>(getTtAppCtx().getArcGISTools().getMapLayers()), mode);

        dialog.setOnMapSelectedListener(this::setMapType);

        dialog.show(getSupportFragmentManager(), SELECT_MAP);
    }

    protected void setMapType(MapType mapType, int mapId) {
        if (this.mapType == mapType) {
            if (mmFrag != null) {
                mmFrag.setMap(mapId);

                getTtAppCtx().getDeviceSettings().setMapId(mapId);
            } else {
                getTtAppCtx().getReport().writeError("MapFragment is null", "BaseMapActivity:setMapType");
                Toast.makeText(getTtAppCtx(), "Error setting map type.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            if (mapType == MapType.Google && AndroidUtils.App.checkPlayServices(this, Consts.Codes.Services.REQUEST_GOOGLE_PLAY_SERVICES) != ConnectionResult.SUCCESS) {
                getTtAppCtx().getReport().writeError("Google Play Services not available", "BaseMapActivity:setMapType");
                Toast.makeText(getTtAppCtx(), "Google Play Services not available.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                mapReady = false;
                mapTypeChanging = true;
                startOptions = getMapStartLocation(mapType, mapId);
                mapFragment = createMapFragment(mapType, startOptions);
                mmFrag = (IMultiMapFragment) mapFragment;
                getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();

                getTtAppCtx().getDeviceSettings().setMapType(mapType);
                getTtAppCtx().getDeviceSettings().setMapId(mapId);
            }
        }
    }


    protected void moveToLocation(Position position, boolean animate) {
        if (position != null) {
            moveToLocation((float) position.getLatitudeSignedDecimal(), (float) position.getLongitudeSignedDecimal(), animate);
        } else {
            getTtAppCtx().getReport().writeWarn("Null Position", "BaseMapActivity:moveToLocation(p,b)");
        }
    }

    protected void moveToLocation(Position position, float zoomLevel, boolean animate) {
        if (position != null) {
            moveToLocation((float) position.getLatitudeSignedDecimal(), (float) position.getLongitudeSignedDecimal(), zoomLevel, animate);
        } else {
            getTtAppCtx().getReport().writeWarn("Null Position", "BaseMapActivity:moveToLocation(p,f,b)", Thread.currentThread().getStackTrace());
        }
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
        if (mmFrag != null) {
            if (extents != null) {
                mmFrag.moveToLocation(extents, padding, animate);
            } else {
                getTtAppCtx().getReport().writeWarn("Null Extents", "BaseMapActivity:moveToLocation(e,i,b)", Thread.currentThread().getStackTrace());
            }
        } else {
            getTtAppCtx().getReport().writeWarn("Null MapFragment", "BaseMapActivity:moveToLocation(e,i,b)", Thread.currentThread().getStackTrace());
        }
    }
    //endregion


    //region Map Events
    @Override
    public void onMapReady() {
        if (!mapReady) {
            mapReady = true;

            setCompassEnabled(getShowCompass());
            setLocationEnabled(getShowMyPos());

            if (!polysCreated) {
                setupGraphicManagers();
                polysCreated = true;
            }

            if (mmFrag != null) {
                for (PolygonGraphicManager pgm : getPolygonGraphicManagers()) {
                    mmFrag.addPolygon(pgm, null);
                }
            }

            setupPolygonOptionsUI();
        }
    }

    protected void resetMapBounds(boolean animate) {
        mapMoved = true;
        if (getMapTracking() == MapTracking.POLY_BOUNDS && getTrackedPolyExtents() != null) {
            moveToLocation(getTrackedPolyExtents(), Consts.Location.PADDING, animate);
        } else if (getMapTracking() == MapTracking.FOLLOW && hasPosition()) {
            moveToLocation(getLastPosition(), Consts.Location.ZOOM_CLOSE, animate);
        } else if (getCompleteBounds() != null) {
            moveToLocation(getCompleteBounds(), Consts.Location.PADDING, animate);
        } else {
            moveToLocation(Consts.Location.USA_BOUNDS, Consts.Location.PADDING, animate);
        }
        mapMoved = false;
    }

    boolean mapTypeChanging;
    IMultiMapFragment.MapOptions startOptions;

    @Override
    public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LEGACY:
                break;
            case LATEST:
                break;
        }
    }

    @Override
    public void onMapLoaded() {
        if (mapTypeChanging) {
            moveToLocation(startOptions.getExtents(), startOptions.getPadding(), false);
            mapTypeChanging = false;
        } else {
            resetMapBounds(true);
        }
    }

    @Override
    public void onMapClick(Position position) {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        hideSelectedMarkerInfo();
    }

    @Override
    public void onMarkerClick(MarkerData markerData) {
        TtPoint currentPoint = markerData.getPoint();

        targetLocation = TtUtils.Points.getPointLocation(currentPoint, markerData.isAdjusted(), getMetadata());

        tvNavPid.setText(StringEx.toString(currentPoint.getPID()));
        tvNavPoly.setText(currentPoint.getPolyName());

        _ToPoint = currentPoint;
        _ToPoly = getPolygons().get(currentPoint.getPolyCN());
        btnToPoint.setText(StringEx.toString(_ToPoint.getPID()));
        btnToPoly.setText(_ToPoly.getName());
        calculateDir();

        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    protected void addPolygonGraphic(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions) {
        if (!polyGraphicManagers.contains(graphicManager)) {
            if (mmFrag != null) {
                try {
                    mmFrag.addPolygon(graphicManager, drawOptions);
                } catch (NullPointerException e) {
                    new AlertDialog.Builder(this)
                            .setMessage("An error occurred trying to add a polygon. Please try readjusting your Polygons.")
                            .setPositiveButton("Adjust Polygons", (dialog, which) -> {
                                getTtAppCtx().adjustProject();
                                finish();
                            })
                            .setNeutralButton(R.string.str_cancel, (dialog, which) -> finish())
                            .show();
                }
            }

            polyGraphicManagers.add(graphicManager);
        }
    }

    protected void addTrailGraphic(TrailGraphicManager graphicManager) {
        if (!trailGraphicManagers.contains(graphicManager)) {
            if (mmFrag != null) {
                mmFrag.addTrail(graphicManager);
            }

            trailGraphicManagers.add(graphicManager);
        }
    }

    protected void removePolygonGraphic(PolygonGraphicManager graphicManager) {
        if (graphicManager != null && polyGraphicManagers.contains(graphicManager)) {
            if (mmFrag != null) {
                mmFrag.removePolygon(graphicManager);
            }

            polyGraphicManagers.remove(graphicManager);
        }
    }

    protected void removeTrailGraphic(TrailGraphicManager graphicManager) {
        if (graphicManager != null && trailGraphicManagers.contains(graphicManager)) {
            if (mmFrag != null) {
                mmFrag.removeTrail(graphicManager);
            }

            trailGraphicManagers.remove(graphicManager);
        }
    }

    protected void addLineGraphic(LineGraphicManager graphicManager) {
        if (mmFrag != null) {
            mmFrag.addLine(graphicManager);
        }

        lineGraphicManagers.add(graphicManager);
    }

    private void removeLineGraphic(LineGraphicManager graphicManager) {
        if (graphicManager != null && lineGraphicManagers.contains(graphicManager)) {
            if (mmFrag != null) {
                mmFrag.removeLine(graphicManager);
            }

            lineGraphicManagers.remove(graphicManager);
        }
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
        mapTracking = getTtAppCtx().getDeviceSettings().getMapTrackingOption();
        showCompass = getTtAppCtx().getDeviceSettings().getMapCompassEnabled();
        showMyPos = getTtAppCtx().getDeviceSettings().getMapShowMyPos();

        if (miResetBounds != null) {
            miResetBounds.setVisible(getMapTracking() != MapTracking.FOLLOW);
        }

        if (miShowMyPos != null) {
            miShowMyPos.setChecked(getShowMyPos());
        }

        if (miTrackedPoly != null) {
            miTrackedPoly.setVisible(true);
        }

        updateActivitySettings();
    }

    protected void updateActivitySettings() {

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
            mapTracking = getTtAppCtx().getDeviceSettings().getMapTrackingOption();
        }
    }

    //endregion


    protected void hideSelectedMarkerInfo() {
        if (mmFrag != null) {
            mmFrag.hideSelectedMarkerInfo();
        }
    }

    protected void onPositionReceived(Position position) {
        updateMapView(position);
    }

    protected void onFirstPositionReceived(Position position) {
        if (mapReady) {
            if (getMapTracking() == MapTracking.FOLLOW) {
                moveToLocation(position, Consts.Location.ZOOM_CLOSE, true);
                mapMoved = false;
            }
        }
    }

    private void updateMapView(Position position) {
        if (getMapTracking() == MapTracking.FOLLOW) {
            moveToLocation(position, true);
        } else if (mapMoved) {
            if (getMapTracking() == MapTracking.POLY_BOUNDS && getTrackedPolyExtents() != null) {
                moveToLocation(getTrackedPolyExtents(), Consts.Location.PADDING, true);
            } else if (getMapTracking() == MapTracking.COMPLETE_BOUNDS && getCompleteBounds() != null) {
                moveToLocation(getCompleteBounds(), Consts.Location.PADDING, true);
            }
        }

        mapMoved = false;
    }


    //region GPS
    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        if (nmeaBurst.hasPosition()) {
            Position position = nmeaBurst.getPosition();

            if (lastPosition == null) {
                onFirstPositionReceived(position);
            } else {
                onPositionReceived(position);
            }

            lastPosition = position;

            if (_NavFromMyLoc && slidingLayout != null && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                calculateDir();
            }

            if (mmFrag != null) {
                mmFrag.updateLocation(nmeaBurst.getPosition());
            }
        }

        onNmeaBurstReceived(nmeaBurst);

        _ReceivingNmea = true;
    }

    protected void onNmeaBurstReceived(NmeaBurst nmeaBurst) {
        //
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        switch (error) {
            case LostDeviceConnection:
                Toast.makeText(BaseMapActivity.this, "Lost GPS Connection", Toast.LENGTH_LONG).show();
                _ReceivingNmea = false;
                break;
            case NoExternalGpsSocket:
                break;
//            case Unknown:
//                break;
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
    public void nmeaBurstValidityChanged(boolean burstsValid) {
        _ReceivingNmea = burstsValid;
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

    @Override
    public void receivingNmeaStrings(boolean receiving) {
        if (!receiving) {
            Toast.makeText(BaseMapActivity.this, "Not receiving NMEA data.", Toast.LENGTH_LONG).show();
        }
    }

    protected boolean isReceivingNmea() {
        return _ReceivingNmea;
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

        if (mGravity != null && mGeomagnetic != null && lastPosition != null && targetLocation != null) {
            float[] R = new float[9];
            float[] I = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float[] orientation = new float[9];
                SensorManager.getOrientation(R, orientation);

                float azimuth = Double.valueOf(Math.toDegrees(orientation[0])).floatValue();


                android.location.Location position = new android.location.Location(StringEx.Empty);
                position.setLatitude(lastPosition.getLatitudeSignedDecimal());
                position.setLongitude(lastPosition.getLongitudeSignedDecimal());
                position.setAltitude(lastPosition.hasElevation() ? lastPosition.getElevation() : 0);

                GeomagneticField geoField = new GeomagneticField(
                        (float) position.getLatitude(),
                        (float) position.getLongitude(),
                        (float) position.getAltitude(),
                        System.currentTimeMillis());

                azimuth += geoField.getDeclination(); // converts magnetic north into true north

                float direction = position.bearingTo(targetLocation) - azimuth;

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


    //region Polygon Options
    private void setupGraphicManagers() {
        createGraphicManagers();

        if (getPolygonGraphicManagers().size() > 0) {
            Extent.Builder builder = new Extent.Builder();
            Extent tmp;

            int usedBounds = 0;
            for (IPolygonGraphicManager pgm : getIPolygonGraphicManagers()) {
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

    protected void createGraphicManagers() {
        PolygonGraphicManager polygonGraphicManager;
        String trackedPolyCN = getTrackedPolyCN();

        ArrayList<TtPoint> points;
        for (TtPolygon polygon : getSortedPolys()) {
            if (!polyPoints.containsKey(polygon.getCN())) {
                points = getTtAppCtx().getDAL().getPointsInPolygon(polygon.getCN());
                polyPoints.put(polygon.getCN(), points);
            } else {
                points = polyPoints.get(polygon.getCN());
            }

            polygonGraphicManager = new PolygonGraphicManager(
                    polygon,
                    points,
                    getMetadata(),
                    getTtAppCtx().getMapSettings().getPolyGraphicOptions(polygon.getCN()));

            addPolygonGraphic(polygonGraphicManager, getTtAppCtx().getMapSettings().getPolyDrawOptions(polygon.getCN()));

            if (trackedPolyCN != null && polygon.getCN().equals(trackedPolyCN)) {
                trackedPolyManager = polygonGraphicManager;
            }
        }
    }

    private View layContent;
    private MultiStateTouchCheckBox tcbPoly, tcbAdjBnd, tcbAdjNav, tcbUnAdjBnd, tcbUnAdjNav,
            tcbAdjBndPts, tcbAdjNavPts, tcbUnAdjBndPts, tcbUnAdjNavPts,
            tcbAdjMiscPts, tcbUnAdjMiscPts, tcbWayPts;

    private boolean masterCardExpanded;

    private final PostDelayHandler[] postDelayHandlers = new PostDelayHandler[12];

    private void setupMasterPolyControl() {
        for (int i = 0; i < 12; i++) {
            postDelayHandlers[i] = new PostDelayHandler(250);
        }

        View layHeader = findViewById(R.id.mpcLayHeader);
        layContent = findViewById(R.id.mpcLayPolyContent);
        tcbPoly = findViewById(R.id.mpcTcbPoly);

        tcbAdjBnd = findViewById(R.id.mpcTcbAdjBnd);
        tcbAdjNav = findViewById(R.id.mpcTcbAdjNav);
        tcbUnAdjBnd = findViewById(R.id.mpcTcbUnadjBnd);
        tcbUnAdjNav = findViewById(R.id.mpcTcbUnadjNav);
        tcbAdjBndPts = findViewById(R.id.mpcTcbAdjBndPts);
        tcbAdjNavPts = findViewById(R.id.mpcTcbAdjNavPts);
        tcbUnAdjBndPts = findViewById(R.id.mpcTcbUnadjBndPts);
        tcbUnAdjNavPts = findViewById(R.id.mpcTcbUnadjNavPts);
        tcbAdjMiscPts = findViewById(R.id.mpcTcbAdjMiscPts);
        tcbUnAdjMiscPts = findViewById(R.id.mpcTcbUnadjMiscPts);
        tcbWayPts = findViewById(R.id.mpcTcbWayPts);

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

        layHeader.setOnClickListener(view -> {
            if (masterCardExpanded) {
                ViewAnimator.collapseView(layContent);
                masterCardExpanded = false;
            } else {
                ViewAnimator.expandView(layContent);
                masterCardExpanded = true;
            }
        });

        tcbPoly.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.VISIBLE, isChecked));

        tcbAdjBnd.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.ADJBND, isChecked));

        tcbAdjNav.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.ADJNAV, isChecked));

        tcbUnAdjBnd.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.UNADJBND, isChecked));

        tcbUnAdjNav.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.UNADJNAV, isChecked));

        tcbAdjBndPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.ADJBNDPTS, isChecked));

        tcbAdjNavPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.ADJNAVPTS, isChecked));

        tcbUnAdjBndPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.UNADJBNDPTS, isChecked));

        tcbUnAdjNavPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.UNADJNAVPTS, isChecked));

        tcbAdjMiscPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.ADJMISCPTS, isChecked));

        tcbUnAdjMiscPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.UNADJMISCPTS, isChecked));

        tcbWayPts.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> updatePolygonDrawOptions(PolygonDrawOptions.DrawCode.WAYPTS, isChecked));
    }

    //change master
    @Override
    public void onHolderOptionChanged(PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder holder, PolygonDrawOptions.DrawCode code) {
        boolean vis = false, invis = false;
        MultiStateTouchCheckBox tcb = null;
        int x = -1;

        try {
            for (PolygonGraphicManager map : getPolygonGraphicManagers()) {

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
                    tcb = tcbUnAdjMiscPts;
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

                        postDelayHandlers[xf].post(() -> {
                            setMasterPolyCheckBox(mscb, visd[xf], invisd[xf], codef);
                            dpc[xf] = 0;
                            visd[xf] = false;
                            invisd[xf] = false;
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
                getTtAppCtx().getMapSettings().getMasterPolyDrawOptions().setValue(code, true);
            } else {
                if (vis) {
                    tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.Checked);
                    getTtAppCtx().getMapSettings().getMasterPolyDrawOptions().setValue(code, true);
                } else {
                    tcb.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.NotChecked);
                    getTtAppCtx().getMapSettings().getMasterPolyDrawOptions().setValue(code, false);
                }
            }
        }
    }

    //change options for all drawoptions and display
    private void updatePolygonDrawOptions(PolygonDrawOptions.DrawCode code, boolean value) {
        for (PolygonGraphicManager gm : getPolygonGraphicManagers()) {
            gm.update(code, value);
        }
    }
    //endregion


    //region Controls
    public void radFromClick(View view) {
        if (((RadioButton) view).isChecked()) {
            int id = view.getId();
            if (id == R.id.mapNavRadPoint) {
                _NavFromMyLoc = false;

                btnFromPoint.setEnabled(true);
                btnFromPoly.setEnabled(true);
            } else if (id == R.id.mapNavRadMyLoc) {
                _NavFromMyLoc = true;

                btnFromPoint.setEnabled(false);
                btnFromPoly.setEnabled(false);
            }

            calculateDir();
        }
    }

    public void radToClick(View view) {
        if (((RadioButton) view).isChecked()) {
            int id = view.getId();
            if (id == R.id.mapNavRadToPoint) {
                _NavToPoint = true;
                layToLine.setVisibility(View.GONE);
            } else if (id == R.id.mapNavRadToPoly) {
                _NavToPoint = false;
                layToLine.setVisibility(View.VISIBLE);
            }

            calculateDir();
        }
    }

    public void btnFromPolyClick(View view) {
        showPolySelectDialog("From Polygon", polygon -> {
            _FromPoly = polygon;
            _FromPoint = null;
            btnFromPoly.setText(_FromPoly.getName());
            btnFromPoint.setText(R.string.str_point);
        });
    }

    public void btnFromPointClick(View view) {
        if (_FromPoly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        showPointSelectDialog(_FromPoly, new onPointSelectClickListener() {
            @Override
            public void onClick(TtPoint point) {
                _FromPoint = point;
                if (_FromPoint != null) {
                    btnFromPoint.setText(StringEx.toString(_FromPoint.getPID()));
                }
                calculateDir();
            }

            @Override
            public void onNoPoints() {
                _FromPoint = null;
                calculateDir();
            }
        });
    }

    public void btnToPolyClick(View view) {
        showPolySelectDialog("To Polygon", polygon -> {
            _ToPoly = polygon;
            _ToPoint = null;
            btnToPoly.setText(_ToPoly.getName());
            btnToPoint.setText(R.string.str_point);
        });
    }

    public void btnToPointClick(View view) {
        if (_ToPoly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        showPointSelectDialog(_ToPoly, new onPointSelectClickListener() {
            @Override
            public void onClick(TtPoint point) {
                _ToPoint = point;
                if (_ToPoint != null) {
                    onTargetPointSelected();
                    btnToPoint.setText(String.format(Locale.getDefault(), "%s",  _ToPoint.getPID()));
                }
            }

            @Override
            public void onNoPoints() {
                _ToPoint = null;
                calculateDir();
            }
        });
    }

    public void btnToPoly2Click(View view) {
        showPolySelectDialog("To Polygon 2", polygon -> {
            _ToPoly2 = polygon;
            _ToPoint2 = null;
            btnToPoly2.setText(_ToPoly2.getName());
            btnToPoint2.setText(R.string.str_point);
        });
    }

    public void btnToPoint2Click(View view) {
        if (_ToPoly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        showPointSelectDialog(_ToPoly2, new onPointSelectClickListener() {
            @Override
            public void onClick(TtPoint point) {
                _ToPoint2 = point;
                if (_ToPoint2 != null) {
                    onTargetPointSelected();
                    btnToPoint2.setText(String.format(Locale.getDefault(), "%s",  _ToPoint2.getPID()));
                }
            }

            @Override
            public void onNoPoints() {
                _ToPoint2 = null;
                calculateDir();
            }
        });
    }

    private void onTargetPointSelected() {
        if (_ToPoly != null && _ToPoint != null && _ToPoly2 != null && _ToPoint2 != null) {
            tvNavPid.setText(String.format(Locale.getDefault(), "%s \u21F9 %s", _ToPoint.getPID(), _ToPoint2.getPID()));
            tvNavPoly.setText((_ToPoly.getCN().equals(_ToPoly2.getCN())) ?
                    _ToPoly.getName() :
                    String.format(Locale.getDefault(), "%s - %s", _ToPoly.getName(), _ToPoly2.getName()));


        } else if (_ToPoint != null) {
            tvNavPid.setText(StringEx.toString(_ToPoint.getPID()));
            tvNavPoly.setText(_ToPoint.getPolyName());
        } else {
            tvNavPid.setText(R.string.str_point);
            tvNavPoly.setText(R.string.str_polygon);
        }

        calculateDir();

        hideSelectedMarkerInfo();
    }

    private void showPolySelectDialog(String title, onPolySelectClickListener opscl) {
        final ArrayList<TtPolygon> polygons = new ArrayList<>(getPolygons().values());
        Collections.sort(polygons);


        if (polygons.size() > 0) {
            final String[] polyStrs = new String[polygons.size()];

            int i = 0;
            for(TtPolygon poly : polygons) {
                polyStrs[i] = poly.getName();
                i++;
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(title);

            dialogBuilder.setItems(polyStrs, (dialog, which) -> {
                opscl.onClick(polygons.get(which));
            });

            dialogBuilder.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog dialog = dialogBuilder.create();

            dialog.show();
        } else {
            Toast.makeText(this, "No Polygons", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPointSelectDialog(TtPolygon poly, onPointSelectClickListener opscl) {
        if (poly == null) {
            Toast.makeText(this, "Select polygon first", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<TtPoint> points = null;
        if (polyPoints.containsKey(poly.getCN())) {
            points = polyPoints.get(poly.getCN());
        } else {
            points = getTtAppCtx().getDAL().getPointsInPolygon(poly.getCN());
        }

        if (points != null && points.size() > 0) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(String.format("To Point in %s", poly.getName()));
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

            listView.setOnItemClickListener((adapterView, view1, i, l) -> {
                opscl.onClick(pda.getItem(i));
                dialog.dismiss();
            });

            dialog.show();
        } else {
            opscl.onNoPoints();
            Toast.makeText(this, "No Points in Polygon", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    private void calculateDir() {
        if (_ToPoint != null) {
            UTMCoords currPos = null;

            if (_NavFromMyLoc && lastPosition != null) {
                currPos = UTMTools.convertLatLonSignedDecToUTM(lastPosition.getLatitudeSignedDecimal(), lastPosition.getLongitudeSignedDecimal(), zone);
            } else if (!_NavFromMyLoc && _FromPoint != null){
                currPos = new UTMCoords(_FromPoint.getUnAdjX(), _FromPoint.getUnAdjY(), zone);
            }

            if (currPos != null) {
                UTMCoords toCoords = null;
                double magDec;

                TtMetadata meta = getMetadata().get(_ToPoint.getMetadataCN());
                if (meta == null)
                    throw new RuntimeException("Metadata not found");
                else
                    magDec = meta.getMagDec();


                if (_NavToPoint) {
                    if (_ToPoint != null) {
                        toCoords = new UTMCoords(_ToPoint.getAdjX(), _ToPoint.getAdjY(), meta.getZone());
                        targetLocation = TtUtils.Points.getPointLocation(_ToPoint, true, getMetadata());
                    }
                } else {
                    if (_ToPoint != null && _ToPoint2 != null && !_ToPoint.sameAdjLocation(_ToPoint2)) {
                        Tuple<UTMCoords, Double> closestPosition = ClosestPositionCalculator.getClosestPointAndDistance(
                                currPos,
                                new UTMCoords(_ToPoint.getAdjX(), _ToPoint.getAdjY(), meta.getZone()),
                                new UTMCoords(_ToPoint2.getAdjX(), _ToPoint2.getAdjY(), meta.getZone()));

                        //error calculating position
                        if (closestPosition == null) {
                            return;
                        }

                        toCoords = closestPosition.Item1;
                        targetLocation = new Location("");

                        Position position = UTMTools.convertUTMtoLatLonSignedDec(toCoords);
                        targetLocation.setLatitude(position.getLatitudeSignedDecimal());
                        targetLocation.setLongitude(position.getLongitudeSignedDecimal());
                        targetLocation.setAltitude(_ToPoint.getAdjZ());
                    }
                }

                if (toCoords != null) {
                    double distInMt = TtUtils.Math.distance(currPos, toCoords);
                    double distInFt = TtUtils.Convert.toFeetTenths(distInMt, Dist.Meters);

                    double azimuth = TtUtils.Math.azimuthOfPoint(currPos.getX(), currPos.getY(), toCoords.getX(), toCoords.getY());
                    double azMag = azimuth - magDec;

                    tvNavDistFt.setText(StringEx.toString(distInFt, 2));
                    tvNavDistMt.setText(StringEx.toString(distInMt, 2));
                    tvNavAzTrue.setText(String.format(Locale.getDefault(), "%.2f\u00B0", azimuth));
                    tvNavAzMag.setText(String.format(Locale.getDefault(), "%.2f\u00B0", azMag));

                    return;
                }
            }
        }

        tvNavDistFt.setText(StringEx.Empty);
        tvNavDistMt.setText(StringEx.Empty);
        tvNavAzTrue.setText(StringEx.Empty);
        tvNavAzMag.setText(StringEx.Empty);
    }


    private ArrayList<TtPolygon> getSortedPolys() {
        ArrayList<TtPolygon> polys = new ArrayList<>(getPolygonsToMap());

        Collections.sort(polys);

        return polys;
    }

    protected Position getLastPosition() {
        return lastPosition;
    }
    
    protected boolean hasPosition() {
        return lastPosition != null;
    }

    protected final int getZone() {
        if (zone == null) {
            TtMetadata defMeta = getMetadata().get(Consts.EmptyGuid);
            if (defMeta != null) {
                zone = defMeta.getZone();
            } else {
                throw new RuntimeException("No default Metadata");
            }
        }

        return zone;
    }

    protected ArrayList<PolygonGraphicManager> getPolygonGraphicManagers() {
        return polyGraphicManagers;
    }

    protected ArrayList<TrailGraphicManager> getTrailGraphicManagers() {
        return trailGraphicManagers;
    }

    protected ArrayList<IPolygonGraphicManager> getIPolygonGraphicManagers() {
        ArrayList<IPolygonGraphicManager> managers = new ArrayList<>();

        managers.addAll(getTrailGraphicManagers());
        managers.addAll(getPolygonGraphicManagers());

        return managers;
    }

    protected Collection<TtPolygon> getPolygonsToMap() {
        return getPolygons().values();
    }

    protected String getTrackedPolyCN() {
        return getTtAppCtx().getProjectSettings().getTrackedPolyCN();
    }

    protected IPolygonGraphicManager getTrackedPolyManager() {
        return trackedPolyManager;
    }

    protected Extent getTrackedPolyExtents() {
        return trackedPolyManager.getExtents();
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

    protected final TtMetadata getDefaultMetadata() {
        return _DefaultMetadata != null ? _DefaultMetadata : (_DefaultMetadata = getMetadata().get(Consts.EmptyGuid));
    }



    private interface onPolySelectClickListener {
        void onClick(TtPolygon polygon);
    }

    private interface onPointSelectClickListener {
        void onClick(TtPoint point);
        void onNoPoints();
    }
}
