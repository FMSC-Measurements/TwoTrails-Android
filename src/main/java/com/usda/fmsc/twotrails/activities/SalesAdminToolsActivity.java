package com.usda.fmsc.twotrails.activities;


import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.dialogs.NumericInputDialog;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.dialogs.ColorPickerDialog;
import com.usda.fmsc.twotrails.dialogs.SATPointDialogTt;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.DataActionType;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
import com.usda.fmsc.twotrails.objects.map.LineGraphicOptions;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.points.WayPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.ClosestPositionCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SalesAdminToolsActivity extends AcquireGpsMapActivity {
    private static final String SAT_POINT_DIALOG = "sat_point_diag";
    private static final String COLOR_DIALOG = "color_diag";

    private CardView cvGpsInfo;
    private FloatingActionButton fabTakePoint, fabCancel;
    private TextView tvCPDist, tvCP, tvAzTrue, tvAzMag, tvProg;
    private RelativeLayout progLay;

    private MenuItem miHideGpsInfo;
    private boolean gpsInfoHidden, _ToleranceExceeded = false, killAcquire = true;

    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private TtPoint _CurrentPoint;
    private WayPoint _ValidationPoint;

    private boolean isPointSetup, cancelVisible, pauseDistLine;

    private int increment, takeAmount, nmeaCount = 0;

    private TtPolygon _ValidationPolygon;
    private ClosestPositionCalculator _ClosestPositionCalc;

    private LineGraphicManager _ConnectionLine;
    private double _LineTolerance = 100;
    private @ColorInt int _LineColor;

    private final PostDelayHandler pdhHideProgress = new PostDelayHandler(500);

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

        ArrayList<TtPoint> polyPoints = getTtAppCtx().getDAL().getPointsInPolygon(_ValidationPolygon.getCN());
        if (polyPoints.size() > 0) {
            _CurrentPoint = polyPoints.get(polyPoints.size() - 1);
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
            actionBar.setTitle(String.format(Locale.getDefault(), "Tracking: %s", getPolygon().getName()));
            actionBar.setDisplayShowTitleEnabled(true);

            AndroidUtils.UI.createToastForToolbarTitle(SalesAdminToolsActivity.this, getToolbar());
        }

        cvGpsInfo = findViewById(R.id.satCardGpsInfo);

        fabCancel = findViewById(R.id.satFabCancel);
        fabTakePoint = findViewById(R.id.satFabTakePoint);

        _ConnectionLine = new LineGraphicManager(null, null,
                new LineGraphicOptions(
                        _LineColor,
                        getTtAppCtx().getDeviceSettings().getMapDistToPolyLineWidth(),
                        LineGraphicOptions.LineStyle.Dashed)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (_ValidationPoint != null && getTtAppCtx() != null) {
            getTtAppCtx().adjustProject(true);
        }
    }

//    @Override
//    protected int getMapRightDrawerLayoutId() {
//        return R.layout.content_drawer_media;  //TODO add other tools
//    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvCPDist = findViewById(R.id.gpsInfoForSatTvDist);
        tvCP = findViewById(R.id.gpsInfoForSatTvPoP);
        tvAzTrue = findViewById(R.id.gpsInfoForSatTvAzTrue);
        tvAzMag = findViewById(R.id.gpsInfoForSatTvAzMag);
        tvProg = findViewById(R.id.satProgressText);
        progLay = findViewById(R.id.progressLayout);
    }

    @Override
    protected void updateActivitySettings() {
        super.updateActivitySettings();

        DeviceSettings ds = getTtAppCtx().getDeviceSettings();

        options.Fix = ds.getSATFilterFix();
        options.FixType = ds.getSATFilterFixType();
        options.DopType = ds.getSATFilterDopType();
        options.DopValue = ds.getSATFilterDopValue();
        increment = ds.getSATIncrement();
        takeAmount = ds.getSATNmeaAmount();

        _LineTolerance = ds.getMapDistToPolyLineTolerance();
        _LineColor = ds.getMapDistToPolyLineColor();

        if (getPolygon() != null) {
            _ClosestPositionCalc = new ClosestPositionCalculator(Collections.singletonList(getPolygon()), getZone(), getTtAppCtx().getDAL());
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
            openSettings(SettingsActivity.SAT_SETTINGS_PAGE);
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
        } else if (itemId == R.id.satMenuValidationPoly) {
            //select poly or (all polys except _plts and _validations) for checking
        } else if (itemId == R.id.satMenuColor) {
            ColorPickerDialog.newInstance(getTtAppCtx().getDeviceSettings().getMapDistToPolyLineColor())
                    .setListener(color -> {
                        _ConnectionLine.setLineColor(color);
                        getTtAppCtx().getDeviceSettings().setMapDistToPolyLineColor(color);
                    })
                    .show(getSupportFragmentManager(), COLOR_DIALOG);
        } else if (itemId == R.id.satMenuTolerance) {
            NumericInputDialog ndiag = new NumericInputDialog(SalesAdminToolsActivity.this,
                    TtUtils.Convert.distance(_LineTolerance, getCurrentMetadata().getDistance(), Dist.Meters));

            ndiag
                .setTitle(String.format(Locale.getDefault(), "Tolerance (%s)", getCurrentMetadata().getDistance().toStringAbv()))
                .setPositiveButton(R.string.str_ok, (dialog, which) -> {
                    if (ndiag.getDouble() < 0) {
                        Toast.makeText(SalesAdminToolsActivity.this, "Tolerance must be greater than 0", Toast.LENGTH_LONG).show();
                    } else {
                        _LineTolerance = TtUtils.Convert.distance(ndiag.getDouble(), Dist.Meters, getCurrentMetadata().getDistance());
                        getTtAppCtx().getDeviceSettings().setMapDistToPolyTolerance(_LineTolerance);
                    }
                })
                .setNeutralButton(R.string.str_cancel, null);

            ndiag.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onAppSettingsUpdated() {
        updateActivitySettings();
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
        addLineGraphic(_ConnectionLine);
    }

    //endregion

    //region Map
    private void updateDirPathUI(ClosestPositionCalculator.ClosestPosition cp, UTMCoords currPos) {
        if (cp.getDistance() > _LineTolerance ^ _ToleranceExceeded) {
            _ToleranceExceeded = cp.getDistance() > _LineTolerance;

            _ConnectionLine.setLineColor(_ToleranceExceeded ?
                    AndroidUtils.UI.getColor(SalesAdminToolsActivity.this, android.R.color.holo_red_dark) :
                    _LineColor
            );

            tvCPDist.setTextColor(AndroidUtils.UI.getColor(this, _ToleranceExceeded ? android.R.color.holo_red_dark : android.R.color.black));
        }

        updateDirPathVector(cp.getCoords(), currPos);
    }

    private void updateDirPathVector(UTMCoords to, UTMCoords from) {
        if (to != null && from != null) {
            _ConnectionLine.updateGraphic(
                    UTMTools.convertUTMtoLatLonSignedDec(to),
                    UTMTools.convertUTMtoLatLonSignedDec(from)
            );

            if (!_ConnectionLine.isVisible()) {
                _ConnectionLine.setVisible(true);
            }
        }
    }

    @Override
    protected TrailGraphicManager createTrailGraphicManager(TtPolygon poly, boolean closeTrail) {
        return super.createTrailGraphicManager(_ValidationPolygon, true);
    }

    @Override
    protected Extent getTrackedPolyExtents() {
        return getTrackedPolyManager() != null ? getTrackedPolyManager().getExtents() : super.getTrackedPolyExtents();
    }

    @Override
    protected ArrayList<TtPolygon> getPolygonsToMap() {

        ArrayList<TtPolygon> polygonsToMap = new ArrayList<>();

        for (TtPolygon p : getPolygons().values()) {
            if (!p.getCN().equals(_ValidationPolygon.getCN())) {
                polygonsToMap.add(p);
            }
        }

        return polygonsToMap;
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

    private void showValidationPoint(UTMCoords currentCoords, final ClosestPositionCalculator.ClosestPosition closestPosition) {
        pauseDistLine = true;

        _ValidationPoint.setComment(String.format(Locale.getDefault(),
                "This point is %.2f (%s) from the closest position %s with an azimuth of %.2f",
                TtUtils.Convert.distance(closestPosition.getDistance(), getCurrentMetadata().getDistance(), Dist.Meters),
                getCurrentMetadata().getDistance().toStringAbv(),
                closestPosition.isPositionAtAPoint() ?
                        String.format(Locale.getDefault(), "at point %d", closestPosition.getClosestPoint().getPID()) :
                        String.format(Locale.getDefault(), "between points %d and %d", closestPosition.getPoint1().getPID(), closestPosition.getPoint2().getPID()),
                closestPosition.getAzimuthToClosestPosition(currentCoords)
        ));


        if (getTtAppCtx().getDeviceSettings().getSATVibrateOnCreate()) {
            AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_POINT_CREATED);
        }

        if (getTtAppCtx().getDeviceSettings().getSATRingOnCreate()) {
            AndroidUtils.Device.playSound(this, R.raw.ring);
        }

        updateDirPathUI(closestPosition, currentCoords);
        updateCPInfo(closestPosition, currentCoords);

        SATPointDialogTt.newInstance(_ValidationPoint, closestPosition)
                .setListener(new SATPointDialogTt.Listener() {
                    @Override
                    public void onSave(Integer pid, String cmt) {
                        if (pid != null && pid != _ValidationPoint.getPID()) {
                            _ValidationPoint.setPID(pid);
                        }

                        if (cmt != null && !cmt.equals(_ValidationPoint.getComment())) {
                            _ValidationPoint.setComment(cmt);
                        }

                        saveValidationPoint();
                        hideCancel();
                        fabTakePoint.setEnabled(true);
                        pauseDistLine = false;

                        //convert to DontAskAgainDialog
                        new AlertDialog.Builder(SalesAdminToolsActivity.this)
                                .setMessage(R.string.sat_cap_media_after_create)
                                .setPositiveButton(R.string.menu_sat_capture, (dialog, which) -> {
                                    if (AndroidUtils.Device.isFullOrientationAvailable(SalesAdminToolsActivity.this)) {
                                        if (getTtAppCtx().getDeviceSettings().getUseTtCameraAsk()) {
                                            DontAskAgainDialog dagdialog = new DontAskAgainDialog(SalesAdminToolsActivity.this,
                                                    DeviceSettings.USE_TTCAMERA_ASK,
                                                    DeviceSettings.USE_TTCAMERA,
                                                    getTtAppCtx().getDeviceSettings().getPrefs());

                                            dagdialog.setMessage(SalesAdminToolsActivity.this.getString(R.string.points_camera_diag))
                                                    .setPositiveButton("TwoTrails", (dialogInterface, i, value) -> captureImage(true, _CurrentPoint), 2)
                                                    .setNegativeButton("Android", (dialogInterface, i, value) -> captureImage(false, _CurrentPoint), 1)
                                                    .setNeutralButton(getString(R.string.str_cancel), null, 0)
                                                    .show();
                                        } else {
                                            captureImage(getTtAppCtx().getDeviceSettings().getUseTtCamera() == 2, _CurrentPoint);
                                        }
                                    } else {
                                        captureImage(false, _CurrentPoint);
                                    }
                                })
                                .setNeutralButton(R.string.str_cancel, null)
                                .create()
                                .show();
                    }

                    @Override
                    public void retake() {
                        resetCollectedNmeaData();
                        pauseDistLine = false;
                        startLogging();
                    }

                    @Override
                    public void onCancel() {
                        resetCollectedNmeaData();
                        pauseDistLine = false;
                    }
                }).show(getSupportFragmentManager(), SAT_POINT_DIALOG);
    }

    private void saveValidationPoint() {
        if (_ValidationPoint != null) {
            getTtAppCtx().getDAL().insertPoint(_ValidationPoint);
            getTtAppCtx().getDAL().insertNmeaBursts(_Bursts);

            _CurrentPoint = _ValidationPoint;
            addPosition(_ValidationPoint);

            isPointSetup = false;
        }

        pauseDistLine = false;
    }


    private void resetCollectedNmeaData() {
        _Bursts.clear();
        _UsedBursts.clear();
    }

    @Override
    protected void startLogging() {
        super.startLogging();

        nmeaCount = 0;

        progLay.setVisibility(View.VISIBLE);
        tvProg.setText("0");
    }

    @Override
    protected void stopLogging() {
        super.stopLogging();

        if (killAcquire) {
            runOnUiThread(() -> progLay.setVisibility(View.GONE));

            killAcquire = false;
        } else {
            pdhHideProgress.post(() -> runOnUiThread(() -> {
                Animation a = AnimationUtils.loadAnimation(SalesAdminToolsActivity.this, R.anim.push_down_out);

                a.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        progLay.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                progLay.startAnimation(a);
            }));
        }
    }
    //endregion

    //region Media
    @Override
    protected void onImageCaptured(TtImage image) {
        //TODO add multi image add support
        if (image != null) {
            if (getTtAppCtx().getMAL().insertImage(image)) {
                getTtAppCtx().getDAL().updateUserActivity(DataActionType.InsertedMedia);
                Toast.makeText(SalesAdminToolsActivity.this, "Image Captured", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SalesAdminToolsActivity.this, "Error saving image", Toast.LENGTH_LONG).show();
            }
        }
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

                if (closestPosition.isPositionAtAPoint()) {
                    tvCP.setText(String.format(Locale.getDefault(), "%d (%s)", closestPosition.getClosestPoint().getPID(), closestPosition.getClosestPoint().getOp()));
                } else {
                    tvCP.setText(String.format(Locale.getDefault(), "%d \u21F9 %d", closestPosition.getPoint1().getPID(), closestPosition.getPoint2().getPID()));
                }


                double azimuth = closestPosition.getAzimuthToClosestPosition(currentCoords);

                tvAzTrue.setText(String.format(Locale.getDefault(), "%.0f\u00B0", azimuth));
                tvAzMag.setText(String.format(Locale.getDefault(), "%.0f\u00B0", azimuth - getCurrentMetadata().getMagDec()));
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

                    runOnUiThread(() -> tvProg.setText(StringEx.toString(++nmeaCount)));

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
                        _ValidationPoint.adjustPoint();

                        UTMCoords calcCoords = new UTMCoords(x, y, zone);
                        showValidationPoint(calcCoords, _ClosestPositionCalc.getClosestPosition(calcCoords, true));
                    }
                }
            }

            if (!pauseDistLine && _ClosestPositionCalc != null) {
                UTMCoords currentCoords = nmeaBurst.getUTM(getCurrentMetadata().getZone());
                ClosestPositionCalculator.ClosestPosition cp = _ClosestPositionCalc.getClosestPosition(currentCoords);

                if (cp != null) {
                    updateDirPathUI(cp, currentCoords);
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
            killAcquire = true;

            stopLogging();
            _Bursts.clear();
            _UsedBursts.clear();
            pauseDistLine = false;

            fabTakePoint.setEnabled(true);
        }
    }
    //endregion
}
