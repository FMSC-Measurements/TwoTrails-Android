package com.usda.fmsc.twotrails.activities.base;

import android.animation.Animator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaIDs;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailApp;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicOptions;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;

public class AcquireGpsMapActivity extends BaseMapActivity {
    private static final String nVal = "*";

    private TextView tvGpsStatus, tvGpsMode, tvLat, tvLon, tvUtmX, tvUtmY,
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

    private TrailGraphicManager trailGraphicManager;

    private long firstPositionFixTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!TwoTrailApp.getContext().getDeviceSettings().isGpsConfigured()) {
            canceling = true;
            setResult(Consts.Codes.Results.GPS_NOT_CONFIGURED);
            finish();
            return;
        }

        if (trailModeEnabled) {
            Intent intent = getIntent();

            if (intent != null) {
                if (intent.hasExtra(Consts.Codes.Data.POLYGON_DATA))
                    _Polygon = intent.getParcelableExtra(Consts.Codes.Data.POLYGON_DATA);

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

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvGpsStatus = findViewById(R.id.gpsInfoTvGpsStatus);
        tvGpsMode = findViewById(R.id.gpsInfoTvGpsMode);
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

        tvGpsMode.setText(GGASentence.GpsFixType.NoFix.toString());

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

        ArrayList<TtPoint> points = TtAppCtx.getDAL().getPointsInPolygon(poly.getCN());

        PolygonGraphicOptions pgo = TtAppCtx.getMapSettings().getPolyGraphicOptions(poly.getCN());

        trailGraphicManager = new TrailGraphicManager(poly, points, getMetadata(),
                new TrailGraphicOptions(
                        pgo.getUnAdjNavColor(),
                        pgo.getAdjPtsColor(),
                        TtAppCtx.getDeviceSettings().getMapUnAdjLineWidth()
                )
        );
    }

    public void addPosition(TtPoint point) {
        addPosition(point, false);
    }

    public void addPosition(TtPoint point, boolean moveToPointAfterAdd) {
        if (trailModeEnabled) {
            final GeoPosition position = trailGraphicManager.addPoint(point);

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

    protected void setNmeaData(final INmeaBurst burst) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(StringEx.format("%.4f", burst.getLatitude()));
                    tvLon.setText(StringEx.format("%.4f", burst.getLongitude()));

                    tvUtmX.setText(StringEx.format("%.3f", coords.getX()));
                    tvUtmY.setText(StringEx.format("%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    if (burst.hasElevation()) {
                        tvElev.setText(StringEx.format("%.2f", burst.getElevation()));
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

                if (burst.isValid(NmeaIDs.SentenceID.RMC) && burst.getMagVarDir() != null) {
                    tvDec.setText(StringEx.format("%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
                } else {
                    tvDec.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GGA)) {
                    tvGpsMode.setText(burst.getFixQuality().toStringX());
                } else {
                    tvGpsMode.setText(GGASentence.GpsFixType.NoFix.toString());
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSA)) {
                    tvGpsStatus.setText(burst.getFix().toString());
                    tvPdop.setText(StringEx.format("%.2f", burst.getPDOP()));
                    tvHdop.setText(StringEx.format("%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(nVal);
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSV)) {

                    tvSat.setText(StringEx.format("%d/%d/%d",
                            burst.isValid(NmeaIDs.SentenceID.GSA) ? burst.getUsedSatellitesCount() : 0,
                            burst.isValid(NmeaIDs.SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()));
                } else {
                    tvSat.setText(nVal);
                }

                if (gpsExtraVisible) {
                    skyView.update(burst);
                    statusView.update(burst);
                }
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
    protected void onNmeaBurstReceived(INmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);
        setNmeaData(nmeaBurst);
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        switch (error) {
            case LostDeviceConnection: {
                if (useLostConnectionWarning) {
                    AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                    final Activity activity = this;

                    dialog.setTitle("GPS Connection Lost");
                    dialog.setMessage("The GPS bluetooth connection has been broken. Would you like to try and reestablish the connection?");

                    dialog.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GpsService.GpsDeviceStatus status = getTtAppContext().getGps().startGps();

                            if (status != GpsService.GpsDeviceStatus.ExternalGpsStarted &&
                                    status != GpsService.GpsDeviceStatus.InternalGpsStarted) {
                                Toast.makeText(AcquireGpsMapActivity.this, "Unable to connect to GPS.", Toast.LENGTH_SHORT).show();
                                activity.setResult(RESULT_CANCELED);
                                activity.finish();
                            } else {
                                AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_CONNECTED);
                            }
                        }
                    });

                    dialog.setNegativeButton(R.string.str_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.setResult(RESULT_CANCELED);
                            activity.finish();
                        }
                    });

                    dialog.show();
                }
                break;
            }
        }
    }
    //endregion


    @Override
    protected void onFirstPositionReceived(GeoPosition position) {
        moveToLocation(position, Consts.Location.ZOOM_CLOSE, true);

        firstPositionFixTime = System.currentTimeMillis();
    }

    @Override
    protected void onPositionReceived(GeoPosition position) {
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

    protected void showExtraGpsStatus() {
        if (!animating && !gpsExtraVisible) {
            ViewAnimator.expandView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    gpsExtraVisible = true;
                    animating = false;
                }
            });

            animating = true;
        }
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
    protected void onCreateGraphicManagers() {
        if (trailModeEnabled) {
            addTrailGraphic(trailGraphicManager);
        }

        super.onCreateGraphicManagers();
    }

    protected TtPolygon getPolygon() {
        return _Polygon;
    }
}
