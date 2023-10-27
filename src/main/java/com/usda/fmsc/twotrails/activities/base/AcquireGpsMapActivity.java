package com.usda.fmsc.twotrails.activities.base;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.geospatial.gnss.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.gnss.codes.GnssFixQuality;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;
import com.usda.fmsc.geospatial.nmea.codes.SentenceID;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.GetDirectionActivity;
import com.usda.fmsc.twotrails.activities.contracts.CaptureTtImage;
import com.usda.fmsc.twotrails.activities.contracts.GetImages;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicOptions;
import com.usda.fmsc.twotrails.ui.GpsStatusSatView;
import com.usda.fmsc.twotrails.ui.GpsStatusSkyView;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.Tuple;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AcquireGpsMapActivity extends BaseMapActivity {
    private static final String nVal = "*";

    private TextView tvGpsStatus, tvGpsFix, tvLat, tvLon, tvUtmX, tvUtmY,
            tvZone, tvDec, tvSat, tvElev, tvPdop, tvHdop, tvNmeaStats;

    private View viewGpsInfoLaySatInfo;
    private GpsStatusSkyView skyView;
    private GpsStatusSatView statusView;

    private Integer zone = null;
    private boolean canceling = false, useLostConnectionWarning = false, trailModeEnabled = true, trailCreated = false;
    private boolean logging, gpsExtraVisible = true, animating, gpsExtraLayoutSet, nmeaInvalid = false;

    private ArrayList<TtPolygon> polygonsToMap;

    private int currentMapIndex = -1, mapOffsetY;

    private TtPolygon _Polygon;
    private TtMetadata _Metadata;

    private TrailGraphicManager trailGraphicManager;

    private long firstPositionFixTime = 0;

    //region Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            canceling = true;
            setResult(Consts.Codes.Results.GPS_NOT_CONFIGURED);
            finish();
        } else {
            Intent intent = getIntent();

            if (intent != null) {
                if (intent.hasExtra(Consts.Codes.Data.POINT_PACKAGE)) {
                    Bundle bundle = intent.getBundleExtra(Consts.Codes.Data.POINT_PACKAGE);

                    if (bundle != null) {
                        _Metadata = bundle.getParcelable(Consts.Codes.Data.METADATA_DATA);
                        _Polygon = bundle.getParcelable(Consts.Codes.Data.POLYGON_DATA);
                    }
                } else {
                    if (intent.hasExtra(Consts.Codes.Data.POLYGON_DATA)) {
                        _Polygon = intent.getParcelableExtra(Consts.Codes.Data.POLYGON_DATA);
                    }

                    if (intent.hasExtra(Consts.Codes.Data.METADATA_DATA)) {
                        _Metadata = intent.getParcelableExtra(Consts.Codes.Data.METADATA_DATA);
                    }
                }

                if (_Metadata == null) {
                    _Metadata = getDefaultMetadata();
                }

                if (_Polygon == null) {
                    trailModeEnabled = false;
                }

                super.onCreate(savedInstanceState);

//                if (isTrailModeEnabled()) {
//                    setupTrailMode(_Polygon);
//                }
            } else if (isTrailModeEnabled()) {
                canceling = true;
            }
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

        if (isTrailModeEnabled() && !trailCreated) {
            setupTrailMode(_Polygon);
        }

        if (skyView != null) {
            skyView.resume();
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
        tvNmeaStats = findViewById(R.id.gpsInfoNmeaTvStats);

        tvGpsFix.setText(GnssFixQuality.NoFix.toString());

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


    protected boolean isCanceling() {
        return canceling;
    }
    //endregion


    //region Map
    @Override
    public void onMapReady() {
        super.onMapReady();
        setMapGesturesEnabled(false);

        if (mapOffsetY > 0) {
            setMapPadding(0, mapOffsetY, 0, 0);
        }
    }

    @Override
    protected void createGraphicManagers() {
        super.createGraphicManagers();

        if (isTrailModeEnabled()) {
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
        trailGraphicManager = createTrailGraphicManager(poly, false);
        trailCreated = true;
    }

    protected TrailGraphicManager createTrailGraphicManager(TtPolygon poly, boolean closeTrail) {
        ArrayList<TtPoint> points = getTtAppCtx().getDAL().getPointsInPolygon(poly.getCN());
        PolygonGraphicOptions pgo = getTtAppCtx().getMapSettings().getPolyGraphicOptions(poly.getCN());

        return new TrailGraphicManager(poly, points, false, getMetadata(),
                new TrailGraphicOptions(
                        pgo.getUnAdjNavColor(),
                        pgo.getAdjPtsColor(),
                        getTtAppCtx().getDeviceSettings().getMapUnAdjLineWidth(),
                        closeTrail
                )
        );
    }

    public void addPosition(TtPoint point) {
        addPosition(point, false, false);
    }

    public void addPosition(TtPoint point, boolean adjusted, boolean moveToPointAfterAdd) {
        if (isTrailModeEnabled()) {
            final Position position = trailGraphicManager.addPoint(point, adjusted);

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

            if (isTrailModeEnabled()) {
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
    protected Extent getTrackedPolyExtents() {
        return isTrailModeEnabled() ? trailGraphicManager.getExtents() : super.getTrackedPolyExtents();
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
        trailCreated = false;
    }

    protected final boolean isTrailModeEnabled() {
        return trailModeEnabled;
    }
    //endregion


    //region GPS
    @Override
    protected void onNmeaBurstReceived(GnssNmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);
        setNmeaData(nmeaBurst);
    }


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
                        Toast.makeText(this, "Unable to connect to GPS.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Not receiving NMEA data.", Toast.LENGTH_LONG).show();
            AndroidUtils.Device.vibrate(getApplicationContext(), Consts.Notifications.VIB_PATTERN_GPS_LOST_CONNECTED);
            tvNmeaStats.setVisibility(View.VISIBLE);
            tvNmeaStats.setText("Not receiving NMEA");
            nmeaInvalid = true;
        }
    }


    protected void setUseLostConnectionWarning(boolean useLostConnectionWarning) {
        this.useLostConnectionWarning = useLostConnectionWarning;
    }
    //endregion


    //region UI
    protected void setNmeaData(final GnssNmeaBurst burst) {
        runOnUiThread(() -> {
            try {
                if (burst.hasPosition()) {
                    UTMCoords coords = zone != null ? burst.getUTM(zone) : burst.getTrueUTM();

                    tvLat.setText(String.format(Locale.getDefault(), "%.4f", burst.getLatitude()));
                    tvLon.setText(String.format(Locale.getDefault(), "%.4f", burst.getLongitude()));

                    tvUtmX.setText(String.format(Locale.getDefault(), "%.3f", coords.getX()));
                    tvUtmY.setText(String.format(Locale.getDefault(), "%.3f", coords.getY()));

                    if (zone == null) {
                        tvZone.setText(StringEx.toString(coords.getZone()));
                    } else {
                        tvZone.setText(StringEx.toString(zone));
                    }

                    if (burst.hasElevation()) {
                        tvElev.setText(String.format(Locale.getDefault(), "%.2f", burst.getElevation()));
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

                boolean iivRMC = !burst.isValid(SentenceID.RMC), iivGGA = false, iivGSA = false, iivGSV = false;

                if (!iivRMC && burst.getMagVar() != null) {
                    tvDec.setText(String.format(Locale.getDefault(), "%.2f %s", burst.getMagVar(), burst.getMagVarDir().toStringAbv()));
                } else {
                    tvDec.setText(nVal);
                }

                if (burst.isValid(SentenceID.GGA)) {
                    tvGpsFix.setText(burst.getFixQuality().toString());
                } else {
                    tvGpsFix.setText(GnssFixQuality.NoFix.toString());
                    iivGGA = true;
                }

                if (burst.areAnyValid(SentenceID.GSA)) {
                    tvGpsStatus.setText(burst.getFix().toString());
                    tvPdop.setText(burst.getPDOP() == null ? nVal : String.format(Locale.getDefault(), "%.2f", burst.getPDOP()));
                    tvHdop.setText(burst.getHDOP() == null ? nVal : String.format(Locale.getDefault(), "%.2f", burst.getHDOP()));
                } else {
                    tvGpsStatus.setText(GnssFixQuality.NoFix.toString());
                    tvHdop.setText(nVal);
                    tvPdop.setText(nVal);
                    iivGSA = true;
                }

                if (burst.isValid(SentenceID.GSV)) {
                    tvSat.setText(String.format(Locale.getDefault(), "%d/%d/%d",
                            burst.getSatellitesInViewCount(),
                            burst.isValid(SentenceID.GGA) ? burst.getTrackedSatellitesCount() : 0,
                            burst.getUsedSatellitesCount()));
                } else {
                    tvSat.setText(nVal);
                    iivGSV = true;
                }

                if ((iivRMC || iivGGA || iivGSA || iivGSV) ^ nmeaInvalid) {
                    nmeaInvalid = (iivRMC || iivGGA || iivGSA || iivGSV);
                    tvNmeaStats.setVisibility(nmeaInvalid ? View.VISIBLE : View.GONE);
                }

                if (nmeaInvalid) {
                    tvNmeaStats.setText(String.format(Locale.getDefault(),
                            "Missing: %s %s %s %s",
                            iivRMC ? "RMC" : StringEx.Empty,
                            iivGGA ? "GGA" : StringEx.Empty,
                            iivGSA ? "GSA" : StringEx.Empty,
                            iivGSV ? "GSV" : StringEx.Empty));
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
        tvZone.setText(zone != null ? Integer.toString(zone) : getText(R.string.str_null_value));
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


    public void btnGpsInfoClick(View view) {
        if (gpsExtraVisible) {
            hideExtraGpsStatus();
        } else {
            showExtraGpsStatus();
        }
    }
    //endregion


    //region Logging and Settings
    protected void startLogging() {
        logging = true;
    }
    protected void stopLogging() {
        logging = false;
    }

    protected boolean isLogging() {
        return logging;
    }


    protected int getPositionsCount() {
        return trailGraphicManager.getPositionsCount();
    }

    @Override
    protected boolean getShowMyPos() {
        return true;
    }
    //endregion

    private Uri _CapturedImageUri;
    private String _CapturedImagePointCN;

    //region Requests
    private final ActivityResultLauncher<String> requestImagesForResult = registerForActivityResult(new GetImages(), results -> {
        try {
            List<TtImage> images = getImagesFromUris(results, _CapturedImagePointCN);

            onImagesSelected(images);

            _CapturedImageUri = null;
            _CapturedImagePointCN = null;

            if (images.size() == 1) {
                askAndUpdateImageOrientation(images.get(0));
            }
        } catch (IOException e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PointsActivity:requestImagesForResult");
            Toast.makeText(this, "Error adding Images, check log for details.", Toast.LENGTH_LONG).show();
        }
    });
    protected void pickImages() {
        requestImagesForResult.launch(null);
    }

    private final ActivityResultLauncher<Intent> updateImageOrientationForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent intent = result.getData();

        if (result.getResultCode() != RESULT_CANCELED) {
            if (intent != null && intent.hasExtra(Consts.Codes.Data.ORIENTATION)) {
                DeviceOrientationEx.Orientation orientation = intent.getParcelableExtra(Consts.Codes.Data.ORIENTATION);

                TtImage image = (TtImage)getCurrentMedia();

                if (image != null) {
                    image.setAzimuth(orientation.getRationalAzimuth());
                    image.setPitch(orientation.getPitch());
                    image.setRoll(orientation.getRoll());

                    onImageOrientationUpdated(image);
                }
            }
        }
    });
    protected void updateImageOrientation(TtImage image) {
        Intent intent = new Intent(this, GetDirectionActivity.class);

        if (image != null) {
            intent.putExtra(Consts.Codes.Data.ORIENTATION, new DeviceOrientationEx.Orientation(image.getAzimuth(), image.getPitch(), image.getRoll()));
            intent.putExtra(Consts.Codes.Data.TTIMAGE_CN, image.getCN());
        }

        updateImageOrientationForResult.launch(intent);
    }

    protected void askAndUpdateImageOrientation(TtImage image) {
        new AlertDialog.Builder(this)
                .setMessage("Would you like to update the orientation (Azimuth) to this image?")
                .setPositiveButton(R.string.str_yes, (dialog, which) -> updateImageOrientation(image))
                .setNegativeButton(R.string.str_no, null)
                .show();
    }



    private final ActivityResultLauncher<Uri> captureImageForResult = registerForActivityResult(new ActivityResultContracts.TakePicture(), picTaken -> {
        if (picTaken) {
            if (AndroidUtils.Files.fileExists(getTtAppCtx(), _CapturedImageUri)) {
                try {
                    onImageCaptured(createImageFromFile(_CapturedImageUri, _CapturedImagePointCN));
                } catch (IOException e) {
                    getTtAppCtx().getReport().writeError(e.getMessage(), "TtPointCollectionActivity:captureImageForResult");
                    Toast.makeText(this, "Failed to create Image.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Image file not found.", Toast.LENGTH_LONG).show();
            }
        }

        _CapturedImageUri = null;
        _CapturedImagePointCN = null;
    });
    private final ActivityResultLauncher<Tuple<String, Uri>> captureTtImageForResult = registerForActivityResult(new CaptureTtImage(), result -> {
        if (result != null) {
            onImageCaptured(result.getImage());
        }

        _CapturedImageUri = null;
        _CapturedImagePointCN = null;
    });
    private final ActivityResultLauncher<String> requestTtCameraPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissionGranted -> {
        if (permissionGranted) {
            captureTtImageForResult.launch(new Tuple<>(_CapturedImagePointCN, _CapturedImageUri));
        } else {
            Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_LONG).show();
        }
    });

    public void captureImage(boolean useTtCamera, TtPoint currentPoint) {
        DateTime dateTime = DateTime.now();

        _CapturedImageUri = Uri.parse(Paths.get(
                getTtAppCtx().getProjectMediaDir().toString(),
                String.format(Locale.getDefault(), "IMG_%s.jpg", TtUtils.Date.toStringDateMillis(dateTime))).toString());
        _CapturedImagePointCN = currentPoint.getCN();

        if (useTtCamera) {
            if (AndroidUtils.App.checkCameraPermission(this)) {
                captureTtImageForResult.launch(new Tuple<>(_CapturedImagePointCN, _CapturedImageUri));
            } else {
                requestTtCameraPermission.launch(Manifest.permission.CAMERA);
            }
        } else {
            captureImageForResult.launch(_CapturedImageUri);
        }
    }
    //endregion


    //region Get
    protected TtPolygon getPolygon() {
        return _Polygon;
    }

    protected TtMetadata getCurrentMetadata() {
        return _Metadata;
    }

    protected TtMedia getCurrentMedia() { return null; }
    //endregion


    //region Updates

    protected void onImageCaptured(TtImage image) { }

    protected void onImagesSelected(List<TtImage> images) { }

    protected void onImageOrientationUpdated(TtImage image) { }


    protected void onMediaUpdated(TtMedia media) { }
    //endregion


    //region Image Tools
    protected TtImage createImageFromFile(Uri uri, String pointCN) throws IOException {
        return TtUtils.Media.createImageFromFile(getTtAppCtx(), uri, pointCN);
    }

    public ArrayList<TtImage> getImagesFromUris(List<Uri> imageUris, String pointCN) throws IOException {
        return TtUtils.Media.getImagesFromUris(getTtAppCtx(), imageUris, pointCN);
    }
    //endregion
}
