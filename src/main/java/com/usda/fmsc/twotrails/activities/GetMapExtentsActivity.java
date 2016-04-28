package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.DownloadOfflineArcGISMapTask;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;

public class GetMapExtentsActivity extends AppCompatActivity {
    public static final String MAP_LAYER = "MapLayer";

    ArcGisMapFragment fragment;
    ArcGisMapLayer agml = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        fragment = ArcGisMapFragment.newInstance(
                new IMultiMapFragment.MapOptions(0, Consts.Location.USA_BOUNDS, Consts.Location.PADDING),
                new ArcGisMapLayer(agml.getId(), agml.getName(), agml.getDescription(), agml.getLocation(),
                        agml.getUrl(), agml.getFilePath(), agml.getMinScale(), agml.getMaxScale(), agml.getLevelsOfDetail(), true)
        );

        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();
    }

    public void btnCreateClick(View view) {
        String omapDir = Global.getOfflineMapsDir();
        File dir = new File(omapDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileLocation = String.format("%s%s%s.tpk", omapDir, File.separator, StringEx.sanitizeForFile(agml.getName()));

        int inc = 1;

        while (FileUtils.fileExists(fileLocation)) {
            fileLocation = String.format("%s%s%s(%d).tpk", Global.getOfflineMapsDir(), File.separator, StringEx.sanitizeForFile(agml.getName()), ++inc);
        }

        final DownloadOfflineArcGISMapTask task = new DownloadOfflineArcGISMapTask(
                agml,
                fragment.getArcExtents(),
                fragment.getSpatialReference(),
                fileLocation,
                ArcGISTools.getCredentials());

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

    private void startDownload(final DownloadOfflineArcGISMapTask task, Long bytes) {
        String message;

        if (bytes != null && bytes > 0) {
            message = String.format("Estimated map size: %d Mb", bytes / 1000);
        } else {
            message = "Unable to estimate map size.";
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(GetMapExtentsActivity.this);
        dialog.setMessage(message)
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArcGISTools.startOfflineMapDownload(task);
                        setResult(Consts.Codes.Results.DOWNLOADING_MAP);
                        finish();
                    }
                })
                .setNeutralButton(R.string.str_cancel, null)
                .show();
    }
}
