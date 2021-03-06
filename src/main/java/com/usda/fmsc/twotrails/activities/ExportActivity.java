package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.util.List;

public class ExportActivity extends CustomToolbarActivity {
    private MultiStateTouchCheckBox chkAll, chkPoints, chkPolys, chkMeta, chkProj, chkNmea, chkKmz, chkGpx, chkSum, chkImgInfo, chkPc;
    private FABProgressCircleEx progCircle;
    private Export.ExportTask exportTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        //FloatingActionButton fabExport = findViewById(R.id.exportFabExport);
        progCircle = findViewById(R.id.exportFabExportProgressCircle);

        chkAll = findViewById(R.id.exportChkAll);
        chkPc = findViewById(R.id.exportChkPC);
        chkPoints = findViewById(R.id.exportChkPoints);
        chkPolys = findViewById(R.id.exportChkPolys);
        chkMeta = findViewById(R.id.exportChkMeta);
        chkImgInfo = findViewById(R.id.exportChkImgInfo);
        chkProj = findViewById(R.id.exportChkProject);
        chkNmea = findViewById(R.id.exportChkNMEA);
        chkKmz = findViewById(R.id.exportChkKMZ);
        chkGpx = findViewById(R.id.exportChkGPX);
        chkSum = findViewById(R.id.exportChkSummary);

        chkAll.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> {
            MultiStateTouchCheckBox.CheckedState chkeckedState = isChecked ?
                    MultiStateTouchCheckBox.CheckedState.Checked : MultiStateTouchCheckBox.CheckedState.NotChecked;

            chkPc.setCheckedStateNoEvent(chkeckedState);
            chkPoints.setCheckedStateNoEvent(chkeckedState);
            chkPolys.setCheckedStateNoEvent(chkeckedState);
            chkMeta.setCheckedStateNoEvent(chkeckedState);
            chkImgInfo.setCheckedStateNoEvent(chkeckedState);
            chkProj.setCheckedStateNoEvent(chkeckedState);
            chkNmea.setCheckedStateNoEvent(chkeckedState);
            chkKmz.setCheckedStateNoEvent(chkeckedState);
            chkGpx.setCheckedStateNoEvent(chkeckedState);
            chkSum.setCheckedStateNoEvent(chkeckedState);
        });

//        if (fabExport != null) {
//            fabExport.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    startExport(true);
//                    return true;
//                }
//            });
//        }

        chkPc.setCheckedState(MultiStateTouchCheckBox.CheckedState.Checked);

        chkPc.setOnCheckedStateChangeListener(this::chkOnChange);
        chkPoints.setOnCheckedStateChangeListener(this::chkOnChange);
        chkPolys.setOnCheckedStateChangeListener(this::chkOnChange);
        chkMeta.setOnCheckedStateChangeListener(this::chkOnChange);
        chkImgInfo.setOnCheckedStateChangeListener(this::chkOnChange);
        chkProj.setOnCheckedStateChangeListener(this::chkOnChange);
        chkNmea.setOnCheckedStateChangeListener(this::chkOnChange);
        chkKmz.setOnCheckedStateChangeListener(this::chkOnChange);
        chkGpx.setOnCheckedStateChangeListener(this::chkOnChange);
        chkSum.setOnCheckedStateChangeListener(this::chkOnChange);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Consts.Codes.Requests.FOLDER && resultCode == RESULT_OK) {


            Uri directoryUri = data.getData();

            boolean noDir = true;
            if (directoryUri != null) {
                DocumentFile dir = DocumentFile.fromTreeUri(this, directoryUri);

                if (dir != null && dir.isDirectory()) {
                    startExport(dir.getUri().getPath(), true);
                    noDir = false;
                }
            }

            if (noDir) {
                Toast.makeText(ExportActivity.this, "Unable to find Directory", Toast.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void chkOnChange(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
        int checkedCount = 0;

        if (chkPc.isChecked())
            checkedCount++;

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

        if (chkImgInfo.isChecked())
            checkedCount++;

        if(checkedCount == 0)
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.NotChecked);
        else if (checkedCount > 9)
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.Checked);
        else
            chkAll.setCheckedStateNoEvent(MultiStateTouchCheckBox.CheckedState.PartialChecked);
    }


    public void btnExport(View view) {
        startExport(false);
    }

    private void startExport(boolean selectdir) {
        if (exportTask == null || exportTask.getStatus() == AsyncTask.Status.FINISHED) {
            if (!chkAll.isChecked()) {
                Toast.makeText(this, "No options selected for export", Toast.LENGTH_SHORT).show();
            } else {
                if (selectdir) {
                    //selectDirectory(TtUtils.getTtFileDir());
                    Toast.makeText(ExportActivity.this, "Unsupported Action", Toast.LENGTH_LONG).show();
                } else {
                    startExport(TtUtils.getTtFileDir(), true);
                }
            }
        }
    }

    private void startExport(final String directory, boolean checkExternalMedia) {
        if (checkExternalMedia && chkPc.isChecked()) {
            final MediaAccessLayer mal = getTtAppCtx().getMAL();
            if (mal != null && mal.hasExternalImages()) {
                if (getTtAppCtx().getDeviceSettings().getAutoInternalizeExportAsk()) {
                    new DontAskAgainDialog(this,
                            DeviceSettings.AUTO_INTERNALIZE_EXPORT_ASK,
                            DeviceSettings.AUTO_INTERNALIZE_EXPORT,
                            getTtAppCtx().getDeviceSettings().getPrefs())

                    .setMessage("There are Images that are saved outside of the media database. Would you like to include them to simplify image transfer?")

                    .setPositiveButton("Include", (dialogInterface, i, value) -> {
                        progCircle.show();
                        internalizeImages(mal, directory);
                    }, 2)
                    .setNegativeButton("Exclude", (dialogInterface, i, value) -> startExport(directory, false), 0)
                    .show();
                } else {
                    if (getTtAppCtx().getDeviceSettings().getAutoInternalizeExport() > 0) {
                        progCircle.show();
                        internalizeImages(mal, directory);
                    } else {
                        startExport(directory, false);
                    }
                }

                return;
            }
        }

        final File dir = new File(String.format("%s/%s/", directory, getTtAppCtx().getDAL().getProjectID()));

        if (dir.exists()) {
            if (getTtAppCtx().getDeviceSettings().getAutoOverwriteExportAsk()) {
                new DontAskAgainDialog(this,
                        DeviceSettings.AUTO_OVERWRITE_EXPORT_ASK,
                        DeviceSettings.AUTO_OVERWRITE_EXPORT,
                        getTtAppCtx().getDeviceSettings().getPrefs())

                .setMessage("There is already a folder that that contains a previous export. Would you like to change the directory or overwrite it?")

                .setPositiveButton("Overwrite", (dialogInterface, i, value) -> export(dir), 2)
//                .setNeutralButton("Change", new DontAskAgainDialog.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i, Object value) {
//                        selectDirectory(dir.getAbsolutePath());
//                    }
//                }, 1)
                .setNegativeButton("Cancel", null, null)

                .show();
            } else {
                if (getTtAppCtx().getDeviceSettings().getAutoOverwriteExport() == 2) {
                    export(dir);
                } else {
                    //selectDirectory(directory);
                    Toast.makeText(ExportActivity.this, "Unsupported Action", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            export(dir);
        }
    }


    private void internalizeImages(final MediaAccessLayer mal, final String directory) {
        new Thread(() -> mal.internalizeImages(new MediaAccessLayer.SimpleMalListener(){
            @Override
            public void internalizeImagesCompleted(List<TtImage> imagesInternalized, final List<TtImage> failedImages) {
                runOnUiThread(() -> {
                    progCircle.hide();

                    if (failedImages.size() > 0) {
                        new AlertDialog.Builder(ExportActivity.this)
                                .setMessage("Some image files were not found. Would you still like to export the database?")
                                .setPositiveButton("Export", (dialog, which) -> runOnUiThread(() -> startExport(directory, false)))
                                .setNeutralButton(R.string.str_cancel, null);
                    }
                    else {
                        startExport(directory, false);
                    }
                });
            }

            @Override
            public void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> failedImages, String failedReason) {
                runOnUiThread(() -> {
                    progCircle.hide();

                    new AlertDialog.Builder(ExportActivity.this)
                            .setMessage("There was an issue internalizing images to the media database. Would you still like to export the database?")
                            .setPositiveButton("Export", (dialog, which) -> startExport(directory, false))
                            .setNeutralButton(R.string.str_cancel, null);
                });
            }
        })).start();
    }


//    private void selectDirectory(String initDir) {
//        new FilePicker.Builder(this, new OnSelectFileListener() {
//            @Override
//            public void onSelect(File file) {
//                startExport(file.getPath(), true);
//            }
//        })
//        .hideFiles(true)
//        .directory(initDir)
//        .show();
//    }

    private void export(final File directory) {
        if (exportTask == null || exportTask.getStatus() == AsyncTask.Status.FINISHED) {
            progCircle.show();

            exportTask = new Export.ExportTask();

            exportTask.setListener(result -> {
            switch (result.getCode()) {
                case Success: {
                    MediaScannerConnection.scanFile(ExportActivity.this, new String[]{directory.getAbsolutePath()}, null, null);

                    progCircle.beginFinalAnimation();

                    Toast.makeText(ExportActivity.this, "Files Exported", Toast.LENGTH_LONG).show();
//                        View view = findViewById(R.id.parent);
//                        if (view != null) {
//                            snackbar = Snackbar.make(view, "Files Exported", Snackbar.LENGTH_INDEFINITE).setAction("View", new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                                    intent.setType("file/*");
//                                    intent.setDataAndType(
//                                            AndroidUtils.Files.getUri(ExportActivity.this, BuildConfig.APPLICATION_ID, directory),
//                                            "resource/folder");
//
//                                    if (snackbar != null)
//                                        snackbar.dismiss();
//
//                                    if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
//                                        startActivity(Intent.createChooser(intent, "View Folder"));
//                                    } else {
//                                        Toast.makeText(ExportActivity.this, "No compatible File Explorers found", Toast.LENGTH_LONG).show();
//                                    }
//                                }
//                            })
//                                    .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));
//
//                            AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);
//
//                            snackbar.show();
//
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        Thread.sleep(5000);
//                                    } catch (Exception ex) {
//                                        //
//                                    }
//
//                                    if (snackbar != null)
//                                        snackbar.dismiss();
//                                }
//                            }).start();
//                        }
                    break;
                }
                case Cancelled:
                    break;
                case ExportFailure:
                case InvalidParams:
                    progCircle.hide();
                    Toast.makeText(getBaseContext(), "Export error, See log for details", Toast.LENGTH_SHORT).show();
                    getTtAppCtx().getReport().writeError("ExportActivity", result.getMessage());
                    break;
            }
            });

            exportTask.execute(
                    new Export.ExportTask.ExportParams(
                            getTtAppCtx(),
                            directory,
                            chkPoints.isChecked(),
                            chkPolys.isChecked(),
                            chkMeta.isChecked(),
                            chkImgInfo.isChecked(),
                            chkProj.isChecked(),
                            chkNmea.isChecked(),
                            chkKmz.isChecked(),
                            chkGpx.isChecked(),
                            chkSum.isChecked(),
                            chkPc.isChecked()
                    )
            );
        } else {
            Toast.makeText(this, "Export in progress", Toast.LENGTH_SHORT).show();
        }
    }
}
