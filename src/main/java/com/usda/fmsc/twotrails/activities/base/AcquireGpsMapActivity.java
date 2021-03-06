package com.usda.fmsc.twotrails.activities.base;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.NmeaIDs;
import com.usda.fmsc.geospatial.nmea41.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea41.sentences.GSASentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicOptions;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public class AcquireGpsMapActivity extends BaseMapActivity {
    private static final String nVal = "*";

    private TextView tvGpsStatus, tvGpsFix, tvLat, tvLon, tvUtmX, tvUtmY,
            tvZone, tvDec, tvSat, tvElev, tvPdop, tvHdop;

    private View viewGpsInfoLaySatInfo;
    private GpsStatusSkyView skyView;
    private GpsStatusSatView statusView;

    private Integer zone = null;
    private boolean canceling = false, useLostConnectionWarning = false, trailModeEnabled = true;
    private boolean logging, gpsExtraVisible = true, animating, gpsExtraLayoutSet;

    private ArrayList<TtPolygon> polygonsToMap;


    private int currentMapIndex = -1, mapOffsetY;

    private TtPolygon _Polygon;
    private TtMetadata _Metadata;

    private TrailGraphicManager trailGraphicManager;

    private long firstPositionFixTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            canceling = true;
            setResult(Consts.Codes.Results.GPS_NOT_CONFIGURED);
            finish();
        } else {
            if (trailModeEnabled) {
                Intent intent = getIntent();

                if (intent != null) {
                    if (intent.hasExtra(Consts.Codes.Data.POINT_PACKAGE)) {
                        Bundle bundle = intent.getBundleExtra(Consts.Codes.Data.POINT_PACKAGE);

                        _Metadata = bundle.getParcelable(Consts.Codes.Data.METADATA_DATA);
                        _Polygon = bundle.getParcelable(Consts.Codes.Data.POLYGON_DATA);
                    } else if (intent.hasExtra(Consts.Codes.Data.POLYGON_DATA)) {
                        _Polygon = intent.getParcelableExtra(Consts.Codes.Data.POLYGON_DATA);
                    }

                    if (_Polygon == null) {
                        trailModeEnabled = false;
                    }

                    super.onCreate(savedInstanceState);

                    if (trailModeEnabled) {
                        setupTrailMode(_Polygon);
                    }
                } else {
                    canceling = true;
                }
            } else {
                super.onCreate(savedInstanceState);
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvGpsStatus = findViewById(R.id.gpsInfoTvGpsStatus);
        tvGpsFix = findViewById(R.id.gpsInfoTvGpsFix);
        tvLat = findViewById(R.id.gpsInfoTvLat);
        tvLon = findViewById(R.id.gpsInfoTvLon);
        tvUtmX = findViewById(R.id.gpsInfoTvUtmX);
        tvUtmY = findViewById(R.id.gpsInfoTvUtmY);
        tvZone = findViewById(R.id.gpsInfoTvZone);
        tvDec = findViewById(R.id.gpsInfoTvDec);
        tvSat = findViewById(R.id.gpsInfoTvSats);
        tvElev = findViewById(R.id.gpsInfoTvElev);
        tvPdop = findViewById(R.id.gpsInfoTvPdop);
        tvHdop = findViewById(R.id.gpsInfoTvHdop);

        tvGpsFix.setText(GGASentence.GpsFixType.NoFix.toString());

        viewGpsInfoLaySatInfo = findViewById(R.id.gpsInfoLaySatInfo);

        skyView = findViewById(R.id.gpsInfoSatSky);
        statusView = findViewById(R.id.gpsInfoSatStatus);

        final ViewTreeObserver observer = skyView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                skyView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                hideExtraGpsStatus(false);

                TableLayout gpsInfoLayStats = findViewById(R.id.gpsInfoLayStats);

                if (gpsInfoLayStats != null) {
                    mapOffsetY = AndroidUtils.Convert.dpToPx(getBaseContext(), 80) + gpsInfoLayStats.getHeight();
                }
            }
        });
    }


    @Override
    public void onMapReady() {
        super.onMapReady();
        setMapGesturesEnabled(false);

        if (mapOffsetY > 0) {
            setMapPadding(0, mapOffsetY, 0, 0);
        }

        if (trailModeEnabled) {
            addTrailGraphic(trailGraphicManager);
        }
    }

    public void moveToMapPoint(int positionIndex) {
        if (currentMapIndex != positionIndex && positionIndex < trailGraphicManager.getPositionsCount()) {
            currentMapIndex = positionIndex;

            moveToLocation(trailGraphicManager.getPosition(currentMapIndex), true);
        }
    }

    private void setupTrailMode(TtPolygon poly) {
        _Polygon = poly;

        ArrayList<TtPoint> points = getTtAppCtx().getDAL().getPointsInPolygon(poly.getCN());

        PolygonGraphicOptions pgo = getTtAppCtx().getMapSettings().getPolyGraphicOptions(poly.getCN());

        trailGraphicManager = new TrailGraphicManager(poly, points, getMetadata(),
                new TrailGraphicOptions(
                        pgo.getUnAdjNavColor(),
                        pgo.getAdjPtsColor(),
                        getTtAppCtx().getDeviceSettings().getMapUnAdjLineWidth()
                )
        );
    }

    public void addPosition(TtPoint point) {
        addPosition(point, false);
    }

    public void addPosition(TtPoint point, boolean moveToPointAfterAdd) {
        if (trailModeEnabled) {
            final Position position = trailGraphicManager.addPoint(point);

            if (position != null) {
                if (moveToPointAfterAdd) {
                    currentMapIndex++;

                    moveToLocation(position, true);
                }
            }
        } else {
            throw new RuntimeException("TrailMode is disabled");
        }
    }

    public void removeLastPosition() {
        trailGraphicManager.removeLastPoint();
    }

    @Override
    protected ArrayList<TtPolygon> getPolygonsToMap() {
        if (polygonsToMap == null) {
            polygonsToMap = new ArrayList<>();

            if (trailModeEnabled) {
                for (TtPolygon p : getPolygons().values()) {
                    if (!p.getCN().equals(getTrackedPolyCN())) {
                        polygonsToMap.add(p);
                    }
                }
            } else {
                polygonsToMap.addAll(getPolygons().values());
            }
        }

        return polygonsToMap;
    }

    @Override
    protected String getTrackedPolyCN() {
        return _Polygon != null ? _Polygon.getCN() : null;
    }

    @Override
    protected Extent getTrackedPoly() {
        if (trailModeEnabled) {
            return trailGraphicManager.getExtents();
        } else {
            throw new RuntimeException("TrailMode is disabled");
        }
    }

    @Override
    protected Extent getCompleteBounds() {
        if (trailGraphicManager != null && trailGraphicManager.getPositionsCount() > 0) {
            Extent.Builder builder = new Extent.Builder();
            builder.include(super.getCompleteBounds());
            builder.include(trailGraphicManager.getExtents());
            return builder.build();
        } else {
            return super.getCompleteBounds();
        }
    }



    @Override
    protected void onPause() {
        super.onPause();

        if (skyView != null) {
            skyView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (skyView != null) {
            skyView.resume();
        }
    }

    protected final void enabledTrailMode(TtPolygon polygon) {
        trailModeEnabled = true;
        polygonsToMap = null;

        if (trailGraphicManager != null) {
            removeTrailGraphic(trailGraphicManager);
        }

        setupTrailMode(polygon);
        addTrailGraphic(trailGraphicManager);
    }

    protected final void disableTrailMode() {
        trailModeEnabled = false;
        if (trailGraphicManager != null) {
            removeTrailGraphic(trailGraphicManager);
            trailGraphicManager = null;
        }
    }

    protected final boolean isTrailModeEnabled() {
        return trailModeEnabled;
    }

    protected void setNmeaData(final NmeaBurst burst) {
        runOnUiThread(() -> {
            try {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(String.format("%.4f", burst.getLatitudeSD()));
                    tvLon.setText(String.format("%.4f", burst.getLongitudeSD()));

                    tvUtmX.setText(String.format("%.3f", coords.getX()));
                    tvUtmY.setText(String.format("%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    if (burst.hasElevation()) {
                        tvElev.setText(String.format("%.2f", burst.getElevation()));
                    } else {
                        tvElev.setText(nVal);
                    }
                } else {
                    tvLat.setText(nVal);
                    tvLon.setText(nVal);
                    tvUtmX.setText(nVal);
                    tvUtmY.setText(nVal);
                    tvElev.setText(nVal);

                    if (zone == null) {
                        tvZone.setText(nVal);
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }
                }

                if (burst.isValid(NmeaIDs.SentenceID.RMC) && burst.getMagVar() != null) {
                    tvDec.setText(String.format("%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
                } else {
                    tvDec.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GGA)) {
                    tvGpsFix.setText(burst.getFixQuality().toStringX());
                } else {
                    tvGpsFix.setText(GGASentence.GpsFixType.NoFix.toString());
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSA)) {
                    tvGpsStatus.setText(burst.getFix().toString());
                    tvPdop.setText(burst.getPDOP() == null ? nVal : String.format("%.2f", burst.getPDOP()));
                    tvHdop.setText(burst.getHDOP() == null ? nVal : String.format("%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(GSASentence.Fix.NoFix.toString());
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSV)) {

                    tvSat.setText(String.format("%d/%d/%d",
                            burst.getUsedSatellitesCount(),
                            burst.isValid(NmeaIDs.SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()));
                } else {
                    tvSat.setText(nVal);
                }

                if (gpsExtraVisible) {
                    skyView.update(burst);
                    statusView.update(burst);
                }
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError("AcquireGpsMapActivity:setNmeaData", e.getMessage(), e.getStackTrace());
            }
        });
    }


    protected void setZone(Integer zone) {
        this.zone = zone;
        tvZone.setText(zone != null ? Integer.toString(zone) : getText(R.string.str_nullvalue));
    }

    protected void startLogging() {
        logging = true;
    }
    protected void stopLogging() {
        logging = false;
    }

    protected boolean isLogging() {
        return logging;
    }


    protected void setUseLostConnectionWarning(boolean useLostConnectionWarning) {
        this.useLostConnectionWarning = useLostConnectionWarning;
    }


    //region GPS
    @Override
    protected void onNmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);
        setNmeaData(nmeaBurst);
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        if (error == GpsService.GpsError.LostDeviceConnection) {
            if (useLostConnectionWarning) {
                AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);

                AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                final Activity activity = this;

                dialog.setTitle("GPS Connection Lost");
                dialog.setMessage("The GPS bluetooth connection has been broken. Would you like to try and reestablish the connection?");

                dialog.setPositiveButton("Connect", (dialog1, which) -> {
                    GpsService.GpsDeviceStatus status = getTtAppCtx().getGps().startGps();

                    if (status != GpsService.GpsDeviceStatus.ExternalGpsStarted &&
                            status != GpsService.GpsDeviceStatus.InternalGpsStarted) {
                        Toast.makeText(AcquireGpsMapActivity.this, "Unable to connect to GPS.", Toast.LENGTH_SHORT).show();
                        activity.setResult(RESULT_CANCELED);
                        activity.finish();
                    } else {
                        AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_CONNECTED);
                    }
                });

                dialog.setNegativeButton(R.string.str_exit, (dialog12, which) -> {
                    activity.setResult(RESULT_CANCELED);
                    activity.finish();
                });

                dialog.show();
            }
        }
    }

    @Override
    public void receivingNmeaStrings(boolean receivingNmea) {
        if (!receivingNmea) {
            Toast.makeText(AcquireGpsMapActivity.this, "Not receiving NMEA data.", Toast.LENGTH_LONG).show();
            AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);
        }
    }
    //endregion


    @Override
    protected void onFirstPositionReceived(Position position) {
        moveToLocation(position, Consts.Location.ZOOM_CLOSE, true);

        firstPositionFixTime = System.currentTimeMillis();
    }

    @Override
    protected void onPositionReceived(Position position) {
        if (firstPositionFixTime == Long.MAX_VALUE || System.currentTimeMillis() - firstPositionFixTime > 3000) {
            super.onPositionReceived(position);
            firstPositionFixTime = Long.MAX_VALUE;
        }
    }

    protected boolean isCanceling() {
        return canceling;
    }


    protected void hideExtraGpsStatus() {
        hideExtraGpsStatus(true);
    }

    protected void hideExtraGpsStatus(boolean animate) {
        if (gpsExtraVisible) {
            if (!gpsExtraLayoutSet) {
                RelativeLayout lay = findViewById(R.id.gpsInfoLaySatInfoSub);

                if (lay != null) {
                    lay.getLayoutParams().width = lay.getWidth();
                    lay.getLayoutParams().height = lay.getHeight();
                    lay.requestLayout();
                    gpsExtraLayoutSet = true;
                }
            }

            if (animate) {
                if (!animating) {
                    animating = true;

                    ViewAnimator.collapseView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            super.onAnimationEnd(animator);
                            gpsExtraVisible = false;
                            animating = false;

                            onEndHideExtraGpsStatus();
                        }
                    });

                    animating = true;
                }
            } else {
                viewGpsInfoLaySatInfo.getLayoutParams().height = 0;
                viewGpsInfoLaySatInfo.requestLayout();
                viewGpsInfoLaySatInfo.setVisibility(View.GONE);
                gpsExtraVisible = false;
            }
        }
    }

    protected void onEndHideExtraGpsStatus() {

    }

    protected void showExtraGpsStatus() {
        if (!animating && !gpsExtraVisible) {
            ViewAnimator.expandView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    gpsExtraVisible = true;
                    animating = false;

                    onEndShowExtraGpsStatus();
                }
            });

            animating = true;
        }
    }

    protected void onEndShowExtraGpsStatus() {

    }

    protected boolean isGpsExtraInfoVisible() {
        return gpsExtraVisible;
    }


    protected int getPositionsCount() {
        return trailGraphicManager.getPositionsCount();
    }


    public void btnGpsInfoClick(View view) {
        if (gpsExtraVisible) {
            hideExtraGpsStatus();
        } else {
            showExtraGpsStatus();
        }
    }


    @Override
    public boolean shouldStartGps() {
        return true;
    }

    @Override
    public boolean shouldStopGps() {
        return false;
    }

    @Override
    protected boolean getShowMyPos() {
        return true;
    }

    @Override
    protected void createPolygonGraphicManagers() {
        if (trailModeEnabled) {
            addTrailGraphic(trailGraphicManager);
        }

        super.createPolygonGraphicManagers();
    }

    protected TtPolygon getPolygon() {
        return _Polygon;
    }

    protected TtMetadata getCurrentMetadata() {
        return _Metadata;
    }
}
