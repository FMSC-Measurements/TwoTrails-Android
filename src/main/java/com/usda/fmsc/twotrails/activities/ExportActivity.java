package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.devpaul.filepickerlibrary.FilePickerActivity;
import com.devpaul.filepickerlibrary.enums.ThemeType;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.lang.reflect.Field;

public class ExportActivity extends CustomToolbarActivity {

    private MultiStateTouchCheckBox chkAll, chkPoints, chkPolys, chkMeta, chkProj, chkNmea, chkKmz, chkGpx, chkSum;
    private FloatingActionButton fabExport;
    private FABProgressCircleEx progCircle;
    private Export.ExportTask exportTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        fabExport = (FloatingActionButton)findViewById(R.id.exportFabExport);
        progCircle = (FABProgressCircleEx)findViewById(R.id.exportFabExportProgressCircle);

        chkAll = (MultiStateTouchCheckBox)findViewById(R.id.exportChkAll);
        chkPoints = (MultiStateTouchCheckBox)findViewById(R.id.exportChkPoints);
        chkPolys = (MultiStateTouchCheckBox)findViewById(R.id.exportChkPolys);
        chkMeta = (MultiStateTouchCheckBox)findViewById(R.id.exportChkMeta);
        chkProj = (MultiStateTouchCheckBox)findViewById(R.id.exportChkProject);
        chkNmea = (MultiStateTouchCheckBox)findViewById(R.id.exportChkNMEA);
        chkKmz = (MultiStateTouchCheckBox)findViewById(R.id.exportChkKMZ);
        chkGpx = (MultiStateTouchCheckBox)findViewById(R.id.exportChkGPX);
        chkSum = (MultiStateTouchCheckBox)findViewById(R.id.exportChkSummary);

        chkAll.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
                MultiStateTouchCheckBox.CheckedState chkeckedState = isChecked ?
                        MultiStateTouchCheckBox.CheckedState.Checked : MultiStateTouchCheckBox.CheckedState.NotChecked;

                chkPoints.setCheckedStateNoEvent(chkeckedState);
                chkPolys.setCheckedStateNoEvent(chkeckedState);
                chkMeta.setCheckedStateNoEvent(chkeckedState);
                chkProj.setCheckedStateNoEvent(chkeckedState);
                chkNmea.setCheckedStateNoEvent(chkeckedState);
                chkKmz.setCheckedStateNoEvent(chkeckedState);
                chkGpx.setCheckedStateNoEvent(chkeckedState);
                chkSum.setCheckedStateNoEvent(chkeckedState);
            }
        });

        fabExport.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startExport(true);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_export, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FilePickerActivity.REQUEST_DIRECTORY && resultCode == RESULT_OK) {
            String directory = data.getStringExtra(FilePickerActivity.FILE_EXTRA_DATA_PATH);

            if(directory != null) {
                startExport(directory);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void btnExport(View view) {
        startExport(false);
    }

    public void chkOnChange(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
        int checkedCount = 0;

        if (chkPoints.isChecked())
            checkedCount++;

        if (chkPolys.isChecked())
            checkedCount++;

        if (chkMeta.isChecked())
            checkedCount++;

        if (chkProj.isChecked())
            checkedCount++;

        if (chkNmea.isChecked())
            checkedCount++;

        if (chkKmz.isChecked())
            checkedCount++;

        if (chkGpx.isChecked())
            checkedCount++;

        if (chkSum.isChecked())
            checkedCount++;

        if(checkedCount == 0)
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.NotChecked);
        else if (checkedCount > 7)
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.Checked);
        else
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.PartialChecked);
    }



    private void startExport(boolean selectdir) {
        if (exportTask == null || exportTask.getStatus() == AsyncTask.Status.FINISHED) {
            if (!chkAll.isChecked()) {
                Toast.makeText(this, "No options selected for export", Toast.LENGTH_SHORT).show();
            } else {
                if (selectdir) {
                    selectDirectory(TtUtils.getTtFileDir());
                } else {
                    startExport(TtUtils.getTtFileDir());
                }
            }
        }
    }

    private void startExport(String directory) {
        final File dir = new File(String.format("%s/%s/", directory, Global.DAL.getProjectID()));

        if (dir.exists()) {
            if (!Global.Settings.DeviceSettings.getAutoOverwriteExportAsk()) {
                DontAskAgainDialog dialog = new DontAskAgainDialog(this,
                        Global.Settings.DeviceSettings.AUTO_OVERWRITE_EXPORT_ASK,
                        Global.Settings.DeviceSettings.AUTO_OVERWRITE_EXPORT,
                        Global.Settings.PreferenceHelper.getPrefs());

                dialog.setMessage("There is already a folder that that contains a previous export. Would you like to change the directory or overwrite it?");

                dialog.setPositiveButton("Overwrite", new DontAskAgainDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, Object value) {
                        export(dir);
                    }
                }, 2);

                dialog.setNeutralButton("Change", new DontAskAgainDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, Object value) {
                        selectDirectory(dir.getAbsolutePath());
                    }
                }, 1);

                dialog.setNegativeButton("Cancel", null, 0);

                dialog.show();
            } else {
                switch (Global.Settings.DeviceSettings.getAutoOverwriteExport()) {
                    case 2:
                        export(dir);
                        break;
                    case 1:
                        selectDirectory(directory);
                        break;
                }
            }
        } else {
            export(dir);
        }
    }

    private void selectDirectory(String initDir) {
        Intent filePickerDialogIntent = new Intent(this, FilePickerActivity.class);
        filePickerDialogIntent.putExtra(FilePickerActivity.THEME_TYPE, ThemeType.DIALOG);
        filePickerDialogIntent.putExtra(FilePickerActivity.INTENT_EXTRA_COLOR_ID, R.color.primary);
        filePickerDialogIntent.putExtra(FilePickerActivity.INTENT_EXTRA_FAB_COLOR_ID, R.color.primaryLight);
        //filePickerDialogIntent.putExtra(FilePickerActivity.SCOPE_TYPE, FileScopeType.DIRECTORIES);
        filePickerDialogIntent.putExtra(FilePickerActivity.REQUEST_CODE, FilePickerActivity.REQUEST_DIRECTORY);
        filePickerDialogIntent.putExtra(FilePickerActivity.FILE_EXTRA_DATA_PATH, initDir);
        startActivityForResult(filePickerDialogIntent, FilePickerActivity.REQUEST_DIRECTORY);
    }

    Snackbar snackbar;

    private void export(final File directory) {
        if (exportTask == null || exportTask.getStatus() == AsyncTask.Status.FINISHED) {
            //if (!directory.canWrite()) {
            //    Toast.makeText(this, "Folder is readonly. Choose a different location.", Toast.LENGTH_SHORT).show();
            //} else {
                progCircle.show();

                exportTask = new Export.ExportTask();

                exportTask.setListener(new Export.ExportTask.Listener() {
                    @Override
                    public void onTaskFinish(Export.ExportResult result) {
                        switch (result.getCode()) {
                            case Success:
                                progCircle.beginFinalAnimation();
                                snackbar = Snackbar.make(findViewById(R.id.parent), "Files Exported", Snackbar.LENGTH_INDEFINITE).setAction("View", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setDataAndType(Uri.parse(directory.getAbsolutePath()), "resource/folder");

                                        if (snackbar != null)
                                            snackbar.dismiss();

                                        startActivity(Intent.createChooser(intent, "Open folder"));
                                    }
                                })
                                .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));

                                snackbar.show();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(5000);
                                        } catch (Exception ex) {
                                            //
                                        }

                                        if(snackbar != null)
                                            snackbar.dismiss();
                                    }
                                }).start();
                                break;
                            case Cancelled:
                                break;
                            case ExportFailure:
                            case InvalidParams:
                                progCircle.hide();
                                Toast.makeText(getBaseContext(), "Export error, See log for details", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });

                exportTask.execute(
                        new Export.ExportTask.ExportParams(
                                Global.DAL,
                                directory,
                                chkPoints.isChecked(),
                                chkPolys.isChecked(),
                                chkMeta.isChecked(),
                                chkProj.isChecked(),
                                chkNmea.isChecked(),
                                chkKmz.isChecked(),
                                chkGpx.isChecked(),
                                chkSum.isChecked()
                        )
                );
            //}
        } else {
            Toast.makeText(this, "Export in progress", Toast.LENGTH_SHORT).show();
        }
    }
}
