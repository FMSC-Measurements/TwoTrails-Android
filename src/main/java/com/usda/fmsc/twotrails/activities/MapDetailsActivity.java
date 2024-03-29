package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.Transitions.ElevationTransition;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.utilities.StringEx;

public class MapDetailsActivity extends TtCustomToolbarActivity {
    private ArcGisMapLayer arcGisMapLayer, agmlBackup;
    private ImageView ivStatusIcon;
    private TextView tvName, tvFile, tvUrl, tvScaleMin, tvScaleMax;
    private EditText txtDesc, txtLoc;
    private PopupMenuButton ofmb;

    private boolean updated = false;

    private final boolean[] overwrites = new boolean[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_details);

        arcGisMapLayer = getIntent().getParcelableExtra(Consts.Codes.Data.MAP_DATA);
        agmlBackup = new ArcGisMapLayer(arcGisMapLayer);

        getToolbar().setTitle(getString(R.string.title_activity_edit_arc_map));

        ivStatusIcon = findViewById(R.id.amdIcon);
        tvName = findViewById(R.id.amdName);
        tvUrl = findViewById(R.id.amdTvUrl);
        tvFile = findViewById(R.id.amdTvFile);
        txtLoc = findViewById(R.id.amdTxtLoc);
        txtDesc = findViewById(R.id.amdTxtDesc);
        tvScaleMin = findViewById(R.id.amdTvScaleMin);
        tvScaleMax = findViewById(R.id.amdTvScaleMax);

        setValues(arcGisMapLayer);

        ofmb = findViewById(R.id.amdMenu);
        if (ofmb != null) {
            ofmb.setItemVisible(R.id.amdMenuUpdatePath, !arcGisMapLayer.isOnline());
            setUpdated(false);

            ofmb.setListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.amdMenuRename) {
                    final InputDialog id = new InputDialog(MapDetailsActivity.this);

                    id.setInputText(arcGisMapLayer.getName())
                            .setPositiveButton(R.string.str_rename, (dialog, which) -> {
                                String value = id.getText();

                                arcGisMapLayer.setName(value);
                                tvName.setText(value);
                                setUpdated(true);
                            })
                            .setNeutralButton(R.string.str_cancel, null)
                            .show();
                } else if (itemId == R.id.amdMenuUpdatePath) {
                    updatePath();
                } else if (itemId == R.id.amdMenuUpdateDetails) {
                    checkUpdateDetails(0);
                } else if (itemId == R.id.amdMenuReset) {
                    if (updated) {
                        arcGisMapLayer = new ArcGisMapLayer(agmlBackup);
                        setValues(arcGisMapLayer);
                        setUpdated(false);
                    }
                } else if (itemId == R.id.amdMenuDelete) {
                    new AlertDialog.Builder(MapDetailsActivity.this)
                            .setMessage("Arc you sure you want to delete this map?")
                            .setPositiveButton(R.string.str_delete, (dialog, which) ->
                                    getTtAppCtx().getArcGISTools().deleteMapLayer(MapDetailsActivity.this, arcGisMapLayer.getId(), true, o -> {
                                        setUpdated(false);

                                        Intent i = new Intent();
                                        i.putExtra(Consts.Codes.Data.MAP_DATA, arcGisMapLayer);

                                        MapDetailsActivity.this.setResult(Consts.Codes.Results.MAP_DELETED, i);
                                        MapDetailsActivity.this.finish();
                                    }))
                            .setNeutralButton(R.string.str_cancel, null)
                            .show();
                }
                return false;
                });
        }

        ElevationTransition.finishTransition(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Consts.Codes.Dialogs.IMPORT_PROJECT_FILE && data != null && data.getData() != null) {
            arcGisMapLayer.setFileName(data.getData().toString());

            tvFile.setText(arcGisMapLayer.getFileName());

            tvFile.setTextColor(AndroidUtils.UI.getColor(this,
                    arcGisMapLayer.hasValidFile() ?
                            android.R.color.black :
                            R.color.red_800));

            updated = true;
        }
    }

    private void setValues(ArcGisMapLayer agml) {
        if (ivStatusIcon != null)
            ivStatusIcon.setImageDrawable(AndroidUtils.UI.getDrawable(this,
                    agml.isOnline() ? R.drawable.ic_online_primary_36 :
                            agml.hasValidFile() ?
                                    R.drawable.ic_offline_primary_36 : R.drawable.ic_offline_red_36));

        tvName.setText(agml.getName());

        tvUrl.setText(agml.getUrl());

        tvFile.setText(agml.getFileName());

        tvFile.setTextColor(AndroidUtils.UI.getColor(this,
                agml.hasValidFile() ?
                    android.R.color.black :
                    R.color.red_800));

        txtLoc.setText(agml.getLocation());

        txtDesc.setText(agml.getDescription());

        tvScaleMin.setText(agml.getMinScale() < 0 ? StringEx.Empty : StringEx.toString(agml.getMinScale(), 4));

        tvScaleMax.setText(agml.getMaxScale() < 0 ? StringEx.Empty : StringEx.toString(agml.getMaxScale(), 4));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //supportFinishAfterTransition();
    }

    @Override
    public void finish() {
        if (updated) {
            Intent i = new Intent();
            i.putExtra(Consts.Codes.Data.MAP_DATA, arcGisMapLayer);
            setResult(Consts.Codes.Results.MAP_UPDATED, i);

            getTtAppCtx().getArcGISTools().updateMapLayer(arcGisMapLayer);
        }

        super.finish();
    }


    private void setUpdated(boolean updated) {
        this.updated = updated;
        ofmb.setItemVisible(R.id.amdMenuReset, updated);
    }

    private void updatePath() {
        AndroidUtils.App.openFileIntent(this, Consts.FileMimes.TPK, Consts.Codes.Dialogs.IMPORT_PROJECT_FILE);
    }


    private final String[] messages = new String[] {
            "A description already exists. Would you like to overwrite it?",
            "Scale Levels already exists. Would you like to overwrite them?",
            "Detail Levels already exists. Would you like to overwrite them?"
    };

    private void checkUpdateDetails(final int c) {
        if (c < 3) {
            boolean invalid = false;

            if (c == 0) {
                invalid = StringEx.isEmpty(arcGisMapLayer.getDescription());
            } else if (c == 1) {
                invalid = arcGisMapLayer.getMinScale() < 0 || arcGisMapLayer.getMaxScale() < 0;
            } else if (c == 2) {
                invalid = arcGisMapLayer.getNumberOfLevels() < 1;
            }

            if (invalid) {
                overwrites[c] = true;
                checkUpdateDetails(c + 1);
            } else {
                new AlertDialog.Builder(MapDetailsActivity.this)
                        .setMessage(messages[c])
                        .setPositiveButton(R.string.str_yes, (dialog, which) -> {
                            overwrites[c] = true;
                            checkUpdateDetails(c + 1);
                        })
                        .setNegativeButton(R.string.str_no, (dialog, which) -> {
                            overwrites[c] = false;
                            checkUpdateDetails(c + 1);
                        })
                        .setNeutralButton(R.string.str_cancel, null)
                        .show();
            }
        } else {
            if (overwrites[0] || overwrites[1] || overwrites[2]) {
                getTtAppCtx().getArcGISTools().getLayerFromUrl(arcGisMapLayer.getUrl(), MapDetailsActivity.this, new ArcGISTools.IGetArcMapLayerListener() {
                    @Override
                    public void onComplete(ArcGisMapLayer layer) {
                        if (overwrites[0]) {
                            arcGisMapLayer.setDescription(layer.getDescription());
                        }

                        if (overwrites[1]) {
                            arcGisMapLayer.setMaxScale(layer.getMaxScale());
                            arcGisMapLayer.setMinScale(layer.getMinScale());
                        }

                        if (overwrites[2] && layer.hasDetailLevels()) {
                            arcGisMapLayer.setLevelsOfDetail(layer.getLevelsOfDetail());
                        }

                        updated = true;

                        setValues(arcGisMapLayer);

                        Toast.makeText(MapDetailsActivity.this, "Map Details Updated", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onBadUrl(String error) {
                        Toast.makeText(MapDetailsActivity.this, "Failed to update map details", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(MapDetailsActivity.this, "No map options set to update", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void amdTvFileClick(View view) {
        if (!arcGisMapLayer.isOnline() && !arcGisMapLayer.hasValidFile()) {
            updatePath();
        }
    }
}
