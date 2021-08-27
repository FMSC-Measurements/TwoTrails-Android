package com.usda.fmsc.twotrails.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.points.WayPoint;
import com.usda.fmsc.twotrails.utilities.ClosestPositionCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;

/*
-Live view of Map and show a line (dashed) from current location.
-Button takes 5 and averages, show window saying pass/fail and a button to create a waypt under poly named: (track_poly or all pts)_validation,
  includes dist from poly and two closest points in desc
*/

public class SalesAdminToolsActivity extends AcquireGpsMapActivity {

    private CardView cvGpsInfo;
    private FloatingActionButton fabTakePoint, fabCancel;

    private MenuItem miHideGpsInfo;
    private boolean gpsInfoHidden;



    private List<TtPoint> _Points;
    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private TtPoint _CurrentPoint;
    private WayPoint _ValidationPoint;
    private TtGroup _Group;

    private boolean isPointSetup, cancelVisible;

    private int increment, takeAmount, nmeaCount = 0;

    private TtPolygon _ValidationPolygon;
    private TtMetadata _DefaultMeta;
    private ClosestPositionCalculator _ClosestPositionCalc;

    private DataAccessLayer _DAL;

    private final FilterOptions options = new FilterOptions();


    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_admin_tools);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        SheetLayoutEx.enterFromBottomAnimation(this);

//        Intent intent = getIntent();
//        if (intent != null && intent.getExtras() != null) {
//
//        }

        addMapDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                if (isMapDrawerOpen(GravityCompat.END)) {
                    setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.END);
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(""); //TODO SET TITLE
            actionBar.setDisplayShowTitleEnabled(true);

            AndroidUtils.UI.createToastForToolbarTitle(SalesAdminToolsActivity.this, getToolbar());
        }

        _DAL = getTtAppCtx().getDAL();
        _DefaultMeta = _DAL.getDefaultMetadata();

        cvGpsInfo = findViewById(R.id.take5CardGpsInfo);

        fabCancel = findViewById(R.id.take5FabCancel);
        fabTakePoint = findViewById(R.id.satFabTakePoint);
    }

//    @Override
//    protected int getMapRightDrawerLayoutId() {
//        return R.layout.content_drawer_media;  //TODO add other tools
//    }

    @Override
    protected void getSettings() {
        super.getSettings();

        options.Fix = getTtAppCtx().getDeviceSettings().getSATFilterFix();
        options.FixType = getTtAppCtx().getDeviceSettings().getSATFilterFixType();
        options.DopType = getTtAppCtx().getDeviceSettings().getSATFilterDopType();
        options.DopValue = getTtAppCtx().getDeviceSettings().getSATFilterDopValue();
        increment = getTtAppCtx().getDeviceSettings().getSATIncrement();
        takeAmount = getTtAppCtx().getDeviceSettings().getSATNmeaAmount();
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_take5, menu);

        miHideGpsInfo = menu.findItem(R.id.satMenuGpsInfoToggle);

        return super.onCreateOptionsMenuEx(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.satMenuGps) {
            startActivityForResult(new Intent(this, SettingsActivity.class)
                            .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                    Consts.Codes.Activities.SETTINGS);
        } else if (itemId == R.id.satMenuSatSettings) {
            startActivityForResult(new Intent(this, SettingsActivity.class)
                            .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.POINT_TAKE5_SETTINGS_PAGE),
                    Consts.Codes.Activities.SETTINGS);
        } else if (itemId == R.id.satMenuGpsInfoToggle) {
            if (gpsInfoHidden) {
                gpsInfoHidden = false;
                cvGpsInfo.setVisibility(View.VISIBLE);
                miHideGpsInfo.setTitle(R.string.menu_x_hide_gps_info);
            } else {
                gpsInfoHidden = true;
                cvGpsInfo.setVisibility(View.GONE);
                miHideGpsInfo.setTitle(R.string.menu_x_show_gps_info);
            }
        } else if (itemId == R.id.satMenuValidationPoly) {//select poly or (all polys except _plts and _validations) for checking
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == Consts.Codes.Activities.SETTINGS) {
//            getTtAppCtx().getGps().startGps();
//
//            getSettings();
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    protected void onSettingsUpdated() {
        getTtAppCtx().getGps().startGps();

        getSettings();
    }

    //    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }

    @Override
    public void onBackPressed() {
        if (isMapDrawerOpen(GravityCompat.END)) {
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
            closeMapDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

//    @Override
//    public void finish() {
//        super.finish();
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            if (fabTakePoint.isEnabled()) {
                btnTakePointClick(null);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (hasPosition()) {
                moveToLocation(getLastPosition(), Consts.Location.ZOOM_CLOSE, true);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onMapReady() {
        super.onMapReady();
        setMapGesturesEnabled(true);
    }
    //endregion


    //region Setup | Validate | Save

    private void setupValidation() {
        TtPoint prevPoint = _CurrentPoint;
        _CurrentPoint = null;
        _ValidationPoint = new WayPoint();

        if (prevPoint != null) {
            _ValidationPoint.setPID(PointNamer.namePoint(prevPoint, increment));
            _ValidationPoint.setIndex(prevPoint.getIndex() + 1);
        } else {
            _ValidationPoint.setPID(PointNamer.nameFirstPoint(getPolygon()));
            _ValidationPoint.setIndex(0);
        }

        _ValidationPoint.setPolyCN(getPolygon().getCN());
        _ValidationPoint.setPolyName(getPolygon().getName());
        _ValidationPoint.setMetadataCN(_DefaultMeta.getCN());
        _ValidationPoint.setGroupCN(_Group.getCN());
        _ValidationPoint.setGroupName(_Group.getName());
        _ValidationPoint.setOnBnd(false);

        _Bursts = new ArrayList<>();
        _UsedBursts = new ArrayList<>();

        showCancel();
    }

    private void showValidationPoint() {
        hideCancel();
        fabTakePoint.setEnabled(true);

        ClosestPositionCalculator.ClosestPosition cp = _ClosestPositionCalc.getClosestPosition(_ValidationPoint.getAdjX(), _ValidationPoint.getAdjY());
        updateDirPathUI(cp.getClosestPoint(), new PointD(_ValidationPoint.getAdjX(), _ValidationPoint.getAdjY()));

        //TODO show msgbox with point information and whether to save
    }

    private void saveValidationPoint(ClosestPositionCalculator.ClosestPosition closestPosition) {
        if (_ValidationPoint != null) {

            _ValidationPoint.setComment("");

            getTtAppCtx().getDAL().insertPoint(_ValidationPoint);
            getTtAppCtx().getDAL().insertNmeaBursts(_Bursts);


            _CurrentPoint = _ValidationPoint;

            isPointSetup = false;
        }
    }
    //endregion

    //region UI
    private void updateDirPathUI(PointD to, PointD from) {


        setDirPath(to, from);
    }

    private void setDirPath(PointD to, PointD from) {
        //TODO at MapBase
    }


    private void showCancel() {
        if (!cancelVisible) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_right_in);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabCancel.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabCancel.startAnimation(a);
            cancelVisible = true;
        }
    }

    private void hideCancel() {
        AndroidUtils.UI.hideKeyboard(this);

        if (cancelVisible) {
            final Animation a = AnimationUtils.loadAnimation(this, R.anim.push_left_out);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabCancel.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            final Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

            fabCancel.startAnimation(a);
            cancelVisible = false;
        }
    }
    //endregion

    //region GPS

    @Override
    protected void onNmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (nmeaBurst.hasPosition()) {
            if (isLogging() && nmeaBurst.isValid()) {
                TtNmeaBurst burst = TtNmeaBurst.create(_ValidationPoint.getCN(), false, nmeaBurst);
                _Bursts.add(burst);

                if (TtUtils.NMEA.isBurstUsable(burst, options)) {
                    burst.setUsed(true);
                    _UsedBursts.add(burst);

                    if (_UsedBursts.size() == takeAmount) {
                        stopLogging();

                        ArrayList<Position> positions = new ArrayList<>();
                        int zone = getCurrentMetadata().getZone();
                        double x = 0, y = 0, count = _UsedBursts.size(), dRMSEx = 0, dRMSEy = 0, dRMSEr;

                        TtNmeaBurst tmpBurst;
                        for (int i = 0; i < count; i++) {
                            tmpBurst = _UsedBursts.get(i);
                            x += tmpBurst.getX(zone);
                            y += tmpBurst.getY(zone);
                            positions.add(tmpBurst.getPosition());
                        }

                        x /= count;
                        y /= count;

                        for (int i = 0; i < count; i++) {
                            tmpBurst = _UsedBursts.get(i);
                            dRMSEx += Math.pow(tmpBurst.getX(zone) - x, 2);
                            dRMSEy += Math.pow(tmpBurst.getY(zone) - y, 2);
                        }

                        dRMSEx = Math.sqrt(dRMSEx / count);
                        dRMSEy = Math.sqrt(dRMSEy / count);
                        dRMSEr = Math.sqrt(Math.pow(dRMSEx, 2) + Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff;

                        Position position = GeoTools.getMidPioint(positions);

                        _ValidationPoint.setLatitude(position.getLatitudeSignedDecimal());
                        _ValidationPoint.setLongitude(position.getLongitudeSignedDecimal());
                        _ValidationPoint.setElevation(position.getElevation());
                        _ValidationPoint.setRMSEr(dRMSEr);
                        _ValidationPoint.setAndCalc(x, y, position.getElevation(), getPolygon());

                        showValidationPoint();
                    }
                }
            } else {
                UTMCoords coords = nmeaBurst.getUTM(_DefaultMeta.getZone());
                ClosestPositionCalculator.ClosestPosition cp = _ClosestPositionCalc.getClosestPosition(coords.getX(), coords.getY());
                updateDirPathUI(cp.getClosestPoint(), new PointD(coords.getX(), coords.getY()));
            }
        }
    }

    //endregion

    //region Controls
    public void btnTakePointClick(View view) {
        if (isReceivingNmea()) {
            if (!isPointSetup) {
                setupValidation();
                startLogging();
            }
        } else {
            Toast.makeText(SalesAdminToolsActivity.this, "Currently not receiving NMEA data.", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnCancelClick(View view) {
        hideCancel();

        if (isLogging()) {
            stopLogging();
            _Bursts.clear();
            _UsedBursts.clear();

            fabTakePoint.setEnabled(true);
        }
    }
    //endregion
}
