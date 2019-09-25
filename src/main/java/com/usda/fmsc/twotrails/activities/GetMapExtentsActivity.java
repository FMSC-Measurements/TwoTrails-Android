package com.usda.fmsc.twotrails.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.esri.core.ags.MapServiceInfo;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.utilities.DownloadOfflineArcGISMapTask;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;

@SuppressLint("RestrictedApi")
public class GetMapExtentsActivity extends AppCompatActivity implements GpsService.Listener {
    public static final String MAP_LAYER = "MapLayer";

    private TwoTrailsApp TtAppCtx;

    private ArcGisMapFragment fragment;
    private ArcGisMapLayer agml = null;

    private FloatingActionButton btnMap, btnGps;

    private ProgressDialog progressDialog;
    private AlertDialog estimateDialog;

    private boolean estimating, estimateReceived;

    private Position position;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TtAppCtx = TwoTrailsApp.getInstance();

        setContentView(R.layout.activity_get_map_extents);

        setResult(RESULT_CANCELED);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.containsKey(MAP_LAYER)) {
            agml = bundle.getParcelable(MAP_LAYER);
        }

        if (agml == null) {
            finish();
            return;
        }

        btnMap = findViewById(R.id.getMapFabMap);
        btnGps = findViewById(R.id.getMapFabPos);

        btnMap.setOnLongClickListener(v -> {
            final DownloadOfflineArcGISMapTask task = new DownloadOfflineArcGISMapTask(
                    agml,
                    fragment.getArcExtents(),
                    fragment.getSpatialReference(),
                    null,
                    TtAppCtx.getArcGISTools().getCredentials(GetMapExtentsActivity.this));

            task.getMapServiceInfo(new DownloadOfflineArcGISMapTask.ServiceInfoListener() {
                @Override
                public void onInfoReceived(final MapServiceInfo msi) {
                    GetMapExtentsActivity.this.runOnUiThread(() -> new AlertDialog.Builder(GetMapExtentsActivity.this)
                            .setTitle("Map Service Info")
                            .setMessage(StringEx.format("Map Name: %s\nMin Scale: 1:%.4f\nMax Scale: 1:%.4f\nMax Export Tiles: %d\nMax Record Count: %d\n\nURL: %s\n\nDescription: %s",
                                msi.getMapName(),
                                msi.getMinScale(), msi.getMaxScale(),
                                msi.getMaxExportTilesCount(),
                                msi.getMaximumRecordCount(),
                                msi.getUrl(),
                                msi.getDescription()
                            ))
                            .setPositiveButton(R.string.str_ok, null)
                            .show());
                }

                @Override
                public void onError(String error) {
                    GetMapExtentsActivity.this.runOnUiThread(() -> new AlertDialog.Builder(GetMapExtentsActivity.this)
                            .setMessage("Error receiving map info.")
                            .setPositiveButton(R.string.str_ok, null)
                            .show());
                }
            });

            return true;
        });

        //squareOverlay = (SquareOverlay)findViewById(R.id.getMapOverlay);

        fragment = ArcGisMapFragment.newInstance(
                new IMultiMapFragment.MapOptions(0, Consts.Location.USA_BOUNDS, Consts.Location.PADDING),
                new ArcGisMapLayer(agml.getId(), agml.getName(), agml.getDescription(), agml.getLocation(),
                        agml.getUrl(), agml.getFilePath(), agml.getMinScale(), agml.getMaxScale(), agml.getLevelsOfDetail(), agml.getExtent(), true)
        );

        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();

        if (TtAppCtx.getDeviceSettings().isGpsConfigured()) {
            TtAppCtx.getGps().startGps();
            TtAppCtx.getGps().addListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (estimateDialog != null && estimateDialog.isShowing()) {
            estimateDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (TtAppCtx.getGps() != null) {
            if (TtAppCtx.getGps().isGpsRunning() && !TtAppCtx.getDeviceSettings().isGpsAlwaysOn()) {
                TtAppCtx.getGps().stopGps();
            }

            TtAppCtx.getGps().removeListener(this);
        }
    }

    public void btnLocClick(View view) {
        if (position != null) {
            fragment.moveToLocation((float)position.getLatitudeSignedDecimal(), (float)position.getLongitudeSignedDecimal(),
                    Consts.Location.ZOOM_CLOSE, true);
        }
    }

    public void btnCreateClick(View view) {
        if (!estimating) {
            estimating = true;

            btnMap.setEnabled(false);
            progressDialog = new ProgressDialog(this);

            progressDialog.setMessage("Estimating Map Size");
            progressDialog.show();

            String omapDir = TtUtils.getOfflineMapsDir();
            File dir = new File(omapDir);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileLocation = String.format("%s%s%s.tpk", omapDir, File.separator, StringEx.sanitizeForFile(agml.getName()));

            int inc = 1;

            while (FileUtils.fileExists(fileLocation)) {
                fileLocation = String.format("%s%s%s(%d).tpk", TtUtils.getOfflineMapsDir(), File.separator, StringEx.sanitizeForFile(agml.getName()), ++inc);
            }

            int level = fragment.getMapZoomLevel();

            agml.setExtent(fragment.getExtents());

            //int[] location = squareOverlay.getScreenExtents();
            //agml.setExtent(fragment.getExtentsFromScreen(location[0], location[1], location[2], location[3]));

            final DownloadOfflineArcGISMapTask task = new DownloadOfflineArcGISMapTask(
                    agml,
                    fragment.getArcExtents(),
                    //fragment.getArcExtentsFromScreen(location[0], location[1], location[2], location[3]),
                    fragment.getSpatialReference(),
                    fileLocation,
                    TtAppCtx.getArcGISTools().getCredentials(GetMapExtentsActivity.this), level, 5);

            task.estimateMapSize(new DownloadOfflineArcGISMapTask.EstimateListener() {
                @Override
                public void onSizeEstimated(Long bytes) {
                    startDownload(task, bytes);
                }

                @Override
                public void onEstimateError(String message) {
                    startDownload(task, null);
                }
            });
        }
    }

    private void startDownload(final DownloadOfflineArcGISMapTask task, final Long bytes) {
        if (!estimateReceived) {
            estimateReceived = true;

            if (progressDialog != null) {
                GetMapExtentsActivity.this.runOnUiThread(() -> progressDialog.hide());
            }

            GetMapExtentsActivity.this.runOnUiThread(() -> {
                if (bytes != null && bytes > 0) {
                    estimateDialog = new AlertDialog.Builder(GetMapExtentsActivity.this)
                            .setMessage(StringEx.format("Estimated map size: %d Mb", bytes / 1000000))
                            .setPositiveButton("Download", (dialog, which) -> {
                                TtAppCtx.getArcGISTools().startOfflineMapDownload(task);
                                GetMapExtentsActivity.this.setResult(Consts.Codes.Results.DOWNLOADING_MAP);
                                GetMapExtentsActivity.this.finish();
                            })
                            .setNeutralButton(R.string.str_cancel, null)
                            .setOnDismissListener(dialog -> estimateReceived = false)
                            .create();

                    estimateDialog.show();
                } else {
                    new AlertDialog.Builder(GetMapExtentsActivity.this)
                            .setMessage("This Map is too large to download from the selected map service.")
                            .setPositiveButton(R.string.str_ok, null)
                            .setOnDismissListener(dialog -> estimateReceived = false).show();
                }

                btnMap.setEnabled(true);
            });

            estimating = false;
        }
    }


    @Override
    public void nmeaBurstReceived(NmeaBurst NmeaBurst) {
        if (NmeaBurst.hasPosition()) {
            position = NmeaBurst.getPosition();

            if (btnGps.getVisibility() != View.VISIBLE) {
                btnGps.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsAreValid) {

    }

    @Override
    public void receivingNmeaStrings(boolean receiving) {

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
    public void gpsError(GpsService.GpsError error) {

    }
}
