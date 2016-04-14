package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.fragments.map.ArcGisMapFragment;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.DownloadOfflineArcGISMapTask;
import com.usda.fmsc.twotrails.utilities.TtUtils;
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
                new IMultiMapFragment.MapOptions(0, Consts.LocationInfo.USA_BOUNDS, Consts.LocationInfo.PADDING),
                new ArcGisMapLayer(agml.getId(), agml.getName(), agml.getDescription(), agml.getLocation(), agml.getUri(), agml.getMinScale(), agml.getMaxScale(), agml.getLevelsOfDetail(), true)
        );

        getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, fragment).commit();
    }

    public void btnCreateClick(View view) {
        String omapDir = TtUtils.getOfflineMapsDir();
        File dir = new File(omapDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileLocation = String.format("%s%s%s.tpk", omapDir, File.separator, StringEx.sanitizeForFile(agml.getName()));

        int inc = 1;

        while (TtUtils.fileExists(fileLocation)) {
            fileLocation = String.format("%s%s%s(%d).tpk", TtUtils.getOfflineMapsDir(), File.separator, StringEx.sanitizeForFile(agml.getName()), ++inc);
        }

        ArcGISTools.startOfflineMapDownload(new DownloadOfflineArcGISMapTask(
                agml,
                fragment.getArcExtents(),
                fragment.getSpatialReference(),
                null)
        );

        setResult(Consts.Activities.Results.DOWNLOADING_MAP);
        finish();
    }
}
