package com.usda.fmsc.twotrails.activities;


import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
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
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
import com.usda.fmsc.twotrails.objects.map.LineGraphicOptions;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.points.WayPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.ClosestPositionCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class SalesAdminToolsActivity extends AcquireGpsMapActivity {

    private CardView cvGpsInfo;
    private FloatingActionButton fabTakePoint, fabCancel;
    private TextView tvCPDist, tvCP, tvAzTrue, tvAzMag;

    private MenuItem miHideGpsInfo;
    private boolean gpsInfoHidden;

    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private TtPoint _CurrentPoint;
    private WayPoint _ValidationPoint;

    private boolean isPointSetup, cancelVisible, pauseDistLine;

    private int increment, takeAmount;

    private TtPolygon _ValidationPolygon;
    private ClosestPositionCalculator _ClosestPositionCalc;

    private LineGraphicManager connectionLine;

    private final FilterOptions options = new FilterOptions();


    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_admin_tools);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        SheetLayoutEx.enterFromBottomAnimation(this);

        if (getPolygon() == null) {
            setResult(Consts.Codes.Results.NO_POLYGON_DATA);
            finish();
            return;
        }

        if (getCurrentMetadata() == null) {
            setResult(Consts.Codes.Results.NO_METADATA_DATA);
            finish();
            return;
        }

        String valPolyName = String.format(Locale.getDefault(), "%s_sat", getPolygon().getName());
        for (TtPolygon poly : getPolygons().values()) {
            if (poly.getName().equals(valPolyName)) {
                _ValidationPolygon = poly;
                break;
            }
        }

        if (_ValidationPolygon == null) {
            setResult(Consts.Codes.Results.NO_POLYGON_DATA);
            finish();
            return;
        }

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
            actionBar.setTitle(R.string.title_activity_sales_admin_tools);
            actionBar.setDisplayShowTitleEnabled(true);

            AndroidUtils.UI.createToastForToolbarTitle(SalesAdminToolsActivity.this, getToolbar());
        }

        cvGpsInfo = findViewById(R.id.satCardGpsInfo);

        fabCancel = findViewById(R.id.satFabCancel);
        fabTakePoint = findViewById(R.id.satFabTakePoint);

        connectionLine = new LineGraphicManager(null, null,
                new LineGraphicOptions(
                        AndroidUtils.UI.getColor(getTtAppCtx(), R.color.black_1000),
                        getTtAppCtx().getDeviceSettings().getMapDistToPolyLineWidth(),
                        LineGraphicOptions.LineStyle.Dashed)
        );
    }

//    @Override
//    protected int getMapRightDrawerLayoutId() {
//        return R.layout.content_drawer_media;  //TODO add other tools
//    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvCPDist = findViewById(R.id.gpsInfoForSatTvDist);
        tvCP = findViewById(R.id.gpsInfoForSatTvCP);
        tvAzTrue = findViewById(R.id.gpsInfoForSatTvAzTrue);
        tvAzMag = findViewById(R.id.gpsInfoForSatTvAzMag);
    }

    @Override
    protected void updateSettings() {
        super.updateSettings();

        options.Fix = getTtAppCtx().getDeviceSettings().getSATFilterFix();
        options.FixType = getTtAppCtx().getDeviceSettings().getSATFilterFixType();
        options.DopType = getTtAppCtx().getDeviceSettings().getSATFilterDopType();
        options.DopValue = getTtAppCtx().getDeviceSettings().getSATFilterDopValue();
        increment = getTtAppCtx().getDeviceSettings().getSATIncrement();
        takeAmount = getTtAppCtx().getDeviceSettings().getSATNmeaAmount();

        if (getPolygon() != null) {
            _ClosestPositionCalc = new ClosestPositionCalculator(Arrays.asList(getPolygon()), getZone(), getTtAppCtx().getDAL());
        }
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sales_admin_tools, menu);

        miHideGpsInfo = menu.findItem(R.id.satMenuGpsInfoToggle);

        return super.onCreateOptionsMenuEx(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.satMenuGps) {
            openSettings(SettingsActivity.GPS_SETTINGS_PAGE);
        } else if (itemId == R.id.satMenuSatSettings) {
            openSettings(SettingsActivity.POINT_SAT_SETTINGS_PAGE);
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

    @Override
    protected void onSettingsUpdated() {
        updateSettings();
    }

    @Override
    public void onBackPressed() {
        if (isMapDrawerOpen(GravityCompat.END)) {
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
            closeMapDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

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

    @Override
    protected void createGraphicManagers() {
        super.createGraphicManagers();
        addLineGraphic(connectionLine);
    }

    //endregion

    //region Map
    private void updateDirPathUI(UTMCoords to, UTMCoords from) {
        if (to != null && from != null) {
            connectionLine.updateGraphic(
                    UTMTools.convertUTMtoLatLonSignedDec(to),
                    UTMTools.convertUTMtoLatLonSignedDec(from)
            );

            if (!connectionLine.isVisible()) {
                connectionLine.setVisible(true);
            }
        }
    }

    @Override
    protected TrailGraphicManager createTrailGraphicManager(TtPolygon poly, boolean closeTrail) {
        return super.createTrailGraphicManager(poly, true);
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
            _ValidationPoint.setPID(PointNamer.nameFirstPoint(_ValidationPolygon));
            _ValidationPoint.setIndex(0);
        }

        _ValidationPoint.setPolyCN(_ValidationPolygon.getCN());
        _ValidationPoint.setPolyName(_ValidationPolygon.getName());
        _ValidationPoint.setMetadataCN(getCurrentMetadata().getCN());
        _ValidationPoint.setGroupCN(Consts.EmptyGuid);
        _ValidationPoint.setGroupName(Consts.Defaults.MainGroupName);
        _ValidationPoint.setOnBnd(false);

        _Bursts = new ArrayList<>();
        _UsedBursts = new ArrayList<>();

        showCancel();
    }

    private void showValidationPoint(UTMCoords currentCoords, ClosestPositionCalculator.ClosestPosition closestPosition) {
        hideCancel();
        fabTakePoint.setEnabled(true);
        pauseDistLine = true;


        //TODO show msgbox with point information and whether to save

        //save
        //saveValidationPoint(closestPosition);

        updateDirPathUI(closestPosition.getCoords(), currentCoords);
        updateCPInfo(closestPosition, currentCoords);

        //cancel
        _Bursts.clear();
        _UsedBursts.clear();
        pauseDistLine = false;
    }

    private void saveValidationPoint(ClosestPositionCalculator.ClosestPosition closestPosition) {
        if (_ValidationPoint != null) {

            _ValidationPoint.setComment(String.format(Locale.getDefault(), "%s", closestPosition));

            getTtAppCtx().getDAL().insertPoint(_ValidationPoint);
            getTtAppCtx().getDAL().insertNmeaBursts(_Bursts);

            _CurrentPoint = _ValidationPoint;
            addPosition(_ValidationPoint);

            isPointSetup = false;
        }

        pauseDistLine = false;
    }
    //endregion

    //region UI
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

    private void updateCPInfo(ClosestPositionCalculator.ClosestPosition closestPosition, UTMCoords currentCoords) {
        runOnUiThread(() -> {
            if (closestPosition != null) {
                tvCPDist.setText(String.format(Locale.getDefault(), "%.2f (%s)",
                        TtUtils.Convert.distance(closestPosition.getDistance(), getCurrentMetadata().getDistance(), Dist.Meters), getCurrentMetadata().getDistance().toStringAbv()));

                if (closestPosition.IsPositionPoint1()) {
                    tvCP.setText(String.format(Locale.getDefault(), "%d (%s)", closestPosition.getPoint1().getPID(), closestPosition.getPoint1().getOp()));
                } else if (closestPosition.IsPositionPoint2()) {
                    tvCP.setText(String.format(Locale.getDefault(), "%d (%s)", closestPosition.getPoint2().getPID(), closestPosition.getPoint2().getOp()));
                } else {
                    tvCP.setText(String.format(Locale.getDefault(), "%d \u21F9 %d", closestPosition.getPoint1().getPID(), closestPosition.getPoint2().getPID()));
                }


                double azimuth = TtUtils.Math.azimuthOfPoint(currentCoords.getX(), currentCoords.getY(), closestPosition.getCoords().getX(), closestPosition.getCoords().getY());
                double azMag = azimuth - getCurrentMetadata().getMagDec();

                tvAzTrue.setText(String.format(Locale.getDefault(), "%.0f\u00B0", azimuth));
                tvAzMag.setText(String.format(Locale.getDefault(), "%.0f\u00B0", azMag));
            } else {
                tvCPDist.setText(R.string.str_null_value);
                tvCP.setText(R.string.str_null_value);
                tvAzTrue.setText(R.string.str_null_value);
                tvAzMag.setText(R.string.str_null_value);
            }
        });
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
                        _ValidationPoint.setAndCalc(x, y, position.getElevation(), _ValidationPolygon);

                        UTMCoords calcCoords = new UTMCoords(x, y, zone);
                        showValidationPoint(calcCoords, _ClosestPositionCalc.getClosestPosition(calcCoords));
                    }
                }
            }

            if (!pauseDistLine && _ClosestPositionCalc != null) {
                UTMCoords currentCoords = nmeaBurst.getUTM(getCurrentMetadata().getZone());
                ClosestPositionCalculator.ClosestPosition cp = _ClosestPositionCalc.getClosestPosition(currentCoords);

                if (cp != null) {
                    updateDirPathUI(cp.getCoords(), currentCoords);
                }

                updateCPInfo(cp, currentCoords);
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
            pauseDistLine = false;

            fabTakePoint.setEnabled(true);
        }
    }
    //endregion
}
