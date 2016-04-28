package com.usda.fmsc.twotrails.activities.custom;

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
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicOptions;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;

public class AcquireGpsMapActivity extends BaseMapActivity {
    public static final int GPS_NOT_FOUND = 1910;
    public static final int GPS_NOT_CONFIGURED = 1911;

    private static final String nVal = "*";

    private TextView tvGpsStatus, tvGpsMode, tvLat, tvLon, tvUtmX, tvUtmY,
            tvZone, tvDec, tvSat, tvElev, tvPdop, tvHdop;

    private View viewGpsInfoLaySatInfo;
    private GpsStatusSkyView skyView;
    private GpsStatusSatView statusView;

    private Integer zone = null;
    private boolean canceling = false, useLostConnectionWarning = false;
    private boolean logging, gpsExtraVisable = true, animating, gpsExtraLayoutSet;

    private ArrayList<TtPolygon> polygonsToMap;


    private int currentMapIndex = -1, mapOffsetY;

    private TtPolygon polygon;

    private TrailGraphicManager trailGraphicManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            canceling = true;
            setResult(GPS_NOT_CONFIGURED);
            finish();
            return;
        }

        Intent intent = getIntent();

        if (intent != null) {
            polygon = (TtPolygon) intent.getSerializableExtra(Consts.Codes.Data.POLYGON_DATA);

            if (polygon == null) {
                setResult(Consts.Codes.Results.NO_POLYGON_DATA);
                canceling = true;
                return;
            }

            super.onCreate(savedInstanceState);

            ArrayList<TtPoint> points = Global.getDAL().getPointsInPolygon(polygon.getCN());

            trailGraphicManager = new TrailGraphicManager(polygon, points, getMetadata(),
                    new TrailGraphicOptions(
                            AndroidUtils.UI.getColor(this, R.color.indigo_800),
                            32
                    )
            );
        } else {
            canceling = true;
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        tvGpsStatus = (TextView)findViewById(R.id.gpsInfoTvGpsStatus);
        tvGpsMode = (TextView)findViewById(R.id.gpsInfoTvGpsMode);
        tvLat = (TextView)findViewById(R.id.gpsInfoTvLat);
        tvLon = (TextView)findViewById(R.id.gpsInfoTvLon);
        tvUtmX = (TextView)findViewById(R.id.gpsInfoTvUtmX);
        tvUtmY = (TextView)findViewById(R.id.gpsInfoTvUtmY);
        tvZone = (TextView)findViewById(R.id.gpsInfoTvZone);
        tvDec = (TextView)findViewById(R.id.gpsInfoTvDec);
        tvSat = (TextView)findViewById(R.id.gpsInfoTvSats);
        tvElev = (TextView)findViewById(R.id.gpsInfoTvElev);
        tvPdop = (TextView)findViewById(R.id.gpsInfoTvPdop);
        tvHdop = (TextView)findViewById(R.id.gpsInfoTvHdop);

        tvGpsMode.setText(GGASentence.GpsFixType.NoFix.toString());

        viewGpsInfoLaySatInfo = findViewById(R.id.gpsInfoLaySatInfo);

        skyView = (GpsStatusSkyView)findViewById(R.id.gpsInfoSatSky);
        statusView = (GpsStatusSatView)findViewById(R.id.gpsInfoSatStatus);

        final ViewTreeObserver observer = skyView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                skyView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                hideExtraGpsStatus(false);

                TableLayout gpsInfoLayStats = (TableLayout) findViewById(R.id.gpsInfoLayStats);

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

        addTrailGraphic(trailGraphicManager);
    }

    public void moveToMapPoint(int positionIndex) {
        if (currentMapIndex != positionIndex && positionIndex < trailGraphicManager.getPositionsCount()) {
            currentMapIndex = positionIndex;

            moveToLocation(trailGraphicManager.getPosition(currentMapIndex), true);
        }
    }


    public void addPosition(TtPoint point) {
        addPosition(point, false);
    }

    public void addPosition(TtPoint point, boolean moveToPointAfterAdd) {

        final GeoPosition position = trailGraphicManager.addPoint(point);

        if (position != null) {
            if (moveToPointAfterAdd) {
                currentMapIndex++;

                moveToLocation(position, true);
            }
        }
    }


    @Override
    protected ArrayList<TtPolygon> getPolygonsToMap() {
        if (polygonsToMap == null) {
            polygonsToMap = new ArrayList<>();

            for (TtPolygon p : getPolygons().values()) {
                if (!p.getCN().equals(getTrackedPolyCN())) {
                    polygonsToMap.add(p);
                }
            }
        }

        return polygonsToMap;
    }

    @Override
    protected String getTrackedPolyCN() {
        return polygon.getCN();
    }

    @Override
    protected Extent getTrackedPoly() {
        return trailGraphicManager.getExtents();
    }

    @Override
    protected Extent getCompleteBounds() {
        if (trailGraphicManager != null) {
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

    protected void setNmeaData(final INmeaBurst burst) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(String.format("%.4f", burst.getLatitude()));
                    tvLon.setText(String.format("%.4f", burst.getLongitude()));

                    tvUtmX.setText(String.format("%.3f", coords.getX()));
                    tvUtmY.setText(String.format("%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    tvElev.setText(String.format("%.2f", burst.getElevation()));
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

                if (burst.isValid(NmeaIDs.SentenceID.RMC)) {
                    tvDec.setText(String.format("%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
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
                    tvPdop.setText(String.format("%.2f", burst.getPDOP()));
                    tvHdop.setText(String.format("%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(nVal);
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                }

                if (burst.isValid(NmeaIDs.SentenceID.GSV)) {

                    tvSat.setText(String.format("%d/%d/%d",
                            burst.isValid(NmeaIDs.SentenceID.GSA) ? burst.getUsedSatellitesCount() : 0,
                            burst.isValid(NmeaIDs.SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()));
                } else {
                    tvSat.setText(nVal);
                }

                if (gpsExtraVisable) {
                    skyView.update(burst);
                    statusView.update(burst);
                }
            }
        });
    }


    public void setZone(Integer zone) {
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
                    dialog.setMessage("The GPS bluetooth connection has been broken. Would you like to try and reestablith the connection?");

                    dialog.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GpsService.GpsDeviceStatus status = Global.getGpsBinder().startGps();

                            if (status != GpsService.GpsDeviceStatus.ExternalGpsStarted &&
                                    status != GpsService.GpsDeviceStatus.InternalGpsStarted) {
                                Toast.makeText(AcquireGpsMapActivity.this, "Unabled to conenct to GPS.", Toast.LENGTH_SHORT).show();
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
    }

    protected boolean isCanceling() {
        return canceling;
    }


    protected void hideExtraGpsStatus() {
        hideExtraGpsStatus(true);
    }

    protected void hideExtraGpsStatus(boolean animate) {
        if (gpsExtraVisable) {
            if (!gpsExtraLayoutSet) {
                RelativeLayout lay = (RelativeLayout) findViewById(R.id.gpsInfoLaySatInfoSub);

                lay.getLayoutParams().width = lay.getWidth();
                lay.getLayoutParams().height = lay.getHeight();
                lay.requestLayout();
                gpsExtraLayoutSet = true;
            }

            if (animate) {
                if (!animating) {
                    animating = true;

                    ViewAnimator.collapseView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            super.onAnimationEnd(animator);
                            gpsExtraVisable = false;
                            animating = false;
                        }
                    });

                    animating = true;
                }
            } else {
                viewGpsInfoLaySatInfo.getLayoutParams().height = 0;
                viewGpsInfoLaySatInfo.requestLayout();
                viewGpsInfoLaySatInfo.setVisibility(View.GONE);
                gpsExtraVisable = false;
            }
        }
    }

    protected void showExtraGpsStatus() {
        if (!animating && !gpsExtraVisable) {
            ViewAnimator.expandView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    gpsExtraVisable = true;
                    animating = false;
                }
            });

            animating = true;
        }
    }

    protected boolean isGpsExtraInfoVisible() {
        return gpsExtraVisable;
    }


    protected int getPositionsCount() {
        return trailGraphicManager.getPositionsCount();
    }


    public void btnGpsInfoClick(View view) {
        if (gpsExtraVisable) {
            hideExtraGpsStatus();
        } else {
            showExtraGpsStatus();
        }
    }


    @Override
    protected boolean shouldStartGps() {
        return true;
    }

    @Override
    protected boolean shouldStopGps() {
        return false;
    }

    @Override
    protected boolean getShowMyPos() {
        return true;
    }

    @Override
    protected void onCreateGraphicManagers() {
        addTrailGraphic(trailGraphicManager);

        super.onCreateGraphicManagers();
    }
}
