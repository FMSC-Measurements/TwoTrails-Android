package com.usda.fmsc.twotrails.activities;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;

import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.contracts.CreateDocumentWType;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.MimeTypes;
import com.usda.fmsc.utilities.Tuple;

import org.joda.time.DateTime;

import java.util.Locale;

public class ExportActivity extends TtCustomToolbarActivity {
    private MultiStateTouchCheckBox chkAll, chkPoints, chkPolys, chkMeta, chkProj, chkNmea, chkKmz, chkGpx, chkSum, chkImgInfo, chkPc;
    private FABProgressCircleEx progCircle;
    private Export.ExportTask exportTask;
    private final TaskRunner taskRunner = new TaskRunner();
    private int checkedCount = 0;

    private final ActivityResultLauncher<Uri> getDirForExport = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
        if (result != null) {
            DocumentFile dir = DocumentFile.fromTreeUri(getTtAppCtx(), result);
            if (dir != null) {
                startExportDir(dir);
            } else {
                getTtAppCtx().getReport().writeError("Unable to get folder", "ExportActivity:getDirForExport");
                Toast.makeText(ExportActivity.this, "Unable to get folder", Toast.LENGTH_LONG).show();
            }
        }
    });
    private final ActivityResultLauncher<Tuple<String, String>> getFilePathForExport = registerForActivityResult(new CreateDocumentWType(), result -> {
        if (result != null) {
            startExportFile(result);
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        FloatingActionButton fabExport = findViewById(R.id.exportFabExport);
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

            chkOnChange(buttonView, isChecked, state);
        });

        if (fabExport != null) {
            fabExport.setOnLongClickListener(v -> {
                if (checkedCount == 1 && !chkPoints.isChecked()) {
                    startSingleExportRequest();
                } else {
                    if (getTtAppCtx().getDeviceSettings().getExportModeAsk()) {
                        DontAskAgainDialog dialog = new DontAskAgainDialog(
                                this,
                                DeviceSettings.EXPORT_MODE_ASK,
                                DeviceSettings.EXPORT_MODE,
                                getTtAppCtx().getDeviceSettings().getPrefs());

                        dialog.setMessage("Would you like to export a single zip file for all the selected options or would you like to export them individually into a folder?");

                        dialog.setPositiveButton("Zip File", (dialogInterface, i, value) -> startZipExportRequest(), 2);

                        dialog.setNeutralButton("Individually", (dialogInterface, i, value) -> startMultiExportRequest(), 1);

                        dialog.setNegativeButton("Cancel", null);

                        dialog.show();
                    } else {
                        if (getTtAppCtx().getDeviceSettings().getExportMode() == 2) {
                            startZipExportRequest();
                        } else {
                            startMultiExportRequest();
                        }
                    }
                }

                return true;
            });
        }

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

    public void chkOnChange(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
        checkedCount = 0;

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


    private String getFileNameWPrefix(String prefix) {
        return String.format(Locale.getDefault(), "%s%s_%s%s",
                prefix != null ?
                        String.format(Locale.getDefault(), "%s_(", prefix) : "",
                TtUtils.projectToFileName(getTtAppCtx().getDAL().getProjectID()),
                TtUtils.Date.toStringDateMillis(new DateTime(getTtAppCtx().getDAM().getDBFile().lastModified())),
                prefix != null ?
                        ")" : ""
        );
    }

    private String getSingleFileName() {
        if (chkPc.isChecked())
            return getFileNameWPrefix(null);

        if (chkPoints.isChecked())
            throw new RuntimeException("Points is not single file capable");

        if (chkPolys.isChecked())
            return getFileNameWPrefix("Polygons");

        if (chkMeta.isChecked())
            return getFileNameWPrefix("Metadata");

        if (chkProj.isChecked())
            return getFileNameWPrefix("Project");

        if (chkNmea.isChecked())
            return getFileNameWPrefix("TtNmea");

        if (chkKmz.isChecked())
            return getFileNameWPrefix(null);

        if (chkGpx.isChecked())
            return getFileNameWPrefix(null);

        if (chkSum.isChecked())
            return getFileNameWPrefix(null);

        if (chkImgInfo.isChecked())
            return getFileNameWPrefix("ImageInfo");

        throw new RuntimeException("No options select");
    }

    private String getSingleFileMeme() {
        if (chkPc.isChecked() || chkPoints.isChecked())
            return MimeTypes.Application.ZIP;

        if (chkPolys.isChecked() || chkMeta.isChecked() || chkProj.isChecked() || chkNmea.isChecked() || chkImgInfo.isChecked())
            return MimeTypes.Text.CSV;

        if (chkKmz.isChecked())
            return MimeTypes.Application.GOOGLE_EARTH_KMZ;

        if (chkGpx.isChecked())
            return MimeTypes.Application.GPS;

        if (chkSum.isChecked())
            return MimeTypes.Text.PLAIN;

        throw new RuntimeException("No options select");
    }

    private String getSingleFileType() {
        if (chkPc.isChecked() || chkPoints.isChecked())
            return "zip";

        if (chkPolys.isChecked() || chkMeta.isChecked() || chkProj.isChecked() || chkNmea.isChecked() || chkImgInfo.isChecked())
            return "csv";

        if (chkKmz.isChecked())
            return "kmz";

        if (chkGpx.isChecked())
            return "gpx";

        if (chkSum.isChecked())
            return "txt";

        throw new RuntimeException("No options select");
    }


    public void btnExport(View view) {
        boolean hasExportDir = getTtAppCtx().hasExternalDirAccess();

        if (checkedCount == 1 && !chkPoints.isChecked()) {
            if (hasExportDir) startSingleExport(); else startSingleExportRequest();
        } else {
            if (getTtAppCtx().getDeviceSettings().getExportModeAsk()) {
                DontAskAgainDialog dialog = new DontAskAgainDialog(
                        this,
                        DeviceSettings.EXPORT_MODE_ASK,
                        DeviceSettings.EXPORT_MODE,
                        getTtAppCtx().getDeviceSettings().getPrefs());

                dialog.setMessage("Would you like to export a single zip file for all the selected options or would you like to export them individually into a folder?");

                dialog.setPositiveButton("Zip File", (dialogInterface, i, value) -> { if(hasExportDir) startZipExport(); else startZipExportRequest(); }, 2);

                dialog.setNegativeButton("Individually", (dialogInterface, i, value) -> { if (hasExportDir) startMultiExport(); else startMultiExportRequest(); } , 1);

                dialog.setNeutralButton("Cancel", null);

                dialog.show();
            } else {
                if (getTtAppCtx().getDeviceSettings().getExportMode() == 2) {
                    if (hasExportDir) startZipExport(); else startZipExportRequest();
                } else {
                    if (hasExportDir) startMultiExport(); else startMultiExportRequest();
                }
            }
        }
    }

    private void startSingleExport() {
        DocumentFile file = getTtAppCtx().getExportDir().createFile(getSingleFileMeme(), getSingleFileName());

        if (file != null && file.exists()) {
            startExportFile(file.getUri());
        } else {
            Toast.makeText(ExportActivity.this, "Error creating file", Toast.LENGTH_LONG).show();
        }
    }

    private void startSingleExportRequest() {
        getFilePathForExport.launch(new Tuple<>(getSingleFileName(), getSingleFileMeme()));
    }

    private void startZipExport() {
        DocumentFile file = getTtAppCtx().getExportDir().createFile(MimeTypes.Application.ZIP, getFileNameWPrefix("Export"));

        if (file != null && file.exists()) {
            startExportFile(file.getUri());
        } else {
            Toast.makeText(ExportActivity.this, "Error creating file", Toast.LENGTH_LONG).show();
        }
    }

    private void startZipExportRequest() {
        getFilePathForExport.launch(new Tuple<>(
                getFileNameWPrefix(
                        checkedCount == 1 ?
                                (chkPoints.isChecked() ?
                                    "Points" :
                                        (chkPc.isChecked() ?
                                            null :
                                            "Export")) :
                                "Export"
                ),
                MimeTypes.Application.ZIP)
        );
    }

    private void startMultiExport() {
        startExportDir(getTtAppCtx().getExportDir());
    }

    private void startMultiExportRequest() {
        getDirForExport.launch(AndroidUtils.Files.getDocumentsUri(getTtAppCtx()));
    }


    private void startExportFile(Uri file) {
        startExport(file, true,
                file.getPath().contains(".zip") ||
                    checkedCount > 1 ||
                    chkPoints.isChecked() ||
                    chkPc.isChecked());
    }

    private void startExportDir(DocumentFile dir) {
        startExport(dir.getUri(), false, false);
    }


    private void startExport(Uri path, boolean isSingleFile, boolean isZipFile) {
        if (exportTask == null || exportTask.getStatus() != TaskRunner.Status.RUNNING) {
            progCircle.show();

            exportTask = new Export.ExportTask();

            exportTask.setListener(new Export.ExportTask.Listener() {
                @Override
                public void onTaskFinish(Export.ExportTask.Result result) {
                    progCircle.beginFinalAnimation();

                    switch (result.getCode()) {
                        case Success: {
                            //MediaScannerConnection.scanFile(ExportActivity.this, new String[]{directory.getAbsolutePath()}, null, null);


                            Toast.makeText(ExportActivity.this, "Files Exported", Toast.LENGTH_LONG).show();

                            //TODO snakebar for uploading/sharing to a service (gdrive/wifi direct/bluetooth to PC)

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
                        case ExportFailure:
                        case InvalidParams:
                            progCircle.hide();
                            Toast.makeText(getBaseContext(), "Export error, See log for details", Toast.LENGTH_SHORT).show();
                            getTtAppCtx().getReport().writeError(result.getMessage(), "ExportActivity:startDirExport:invalidParams");
                            break;
                        case Cancelled:
                        default:
                            progCircle.hide();
                            break;

                    }
                }

                @Override
                public void onTaskError(Exception e) {
                    progCircle.beginFinalAnimation();

                    getTtAppCtx().getReport().writeError(e.getMessage(), "ExportActivity:startDirExport:onTaskError", e.getStackTrace());

                    Toast.makeText(ExportActivity.this, "Error exporting. See log for details", Toast.LENGTH_LONG).show();
                }
            });

            taskRunner.executeAsync(exportTask,
                    new Export.ExportTask.Params(
                            getTtAppCtx(),
                            path,
                            isSingleFile,
                            isZipFile,
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


//    private void startExport(final Uri directory, boolean checkExternalMedia) {
//        if (checkExternalMedia && chkPc.isChecked()) {
//            final MediaAccessLayer mal = getTtAppCtx().getMAL();
//            if (mal != null && mal.hasExternalImages()) {
//                if (getTtAppCtx().getDeviceSettings().getAutoInternalizeExportAsk()) {
//                    new DontAskAgainDialog(this,
//                            DeviceSettings.AUTO_INTERNALIZE_EXPORT_ASK,
//                            DeviceSettings.AUTO_INTERNALIZE_EXPORT,
//                            getTtAppCtx().getDeviceSettings().getPrefs())
//
//                    .setMessage("There are Images that are saved outside of the media database. Would you like to include them to simplify image transfer?")
//
//                    .setPositiveButton("Include", (dialogInterface, i, value) -> {
//                        progCircle.show();
//                        internalizeImages(mal, directory);
//                    }, 2)
//                    .setNegativeButton("Exclude", (dialogInterface, i, value) -> startMultiExport(directory, false), 0)
//                    .show();
//                } else {
//                    if (getTtAppCtx().getDeviceSettings().getAutoInternalizeExport() > 0) {
//                        progCircle.show();
//                        internalizeImages(mal, directory);
//                    } else {
//                        startMultiExport(directory, false);
//                    }
//                }
//
//                return;
//            }
//        }
//
//        final File fdir = new File(String.format("%s/%s/", directory, getTtAppCtx().getDAL().getProjectID()));
//        DocumentFile dir = DocumentFile.fromFile(fdir);
//
//        if (dir.exists()) {
//            if (getTtAppCtx().getDeviceSettings().getAutoOverwriteExportAsk()) {
//                new DontAskAgainDialog(this,
//                        DeviceSettings.AUTO_OVERWRITE_EXPORT_ASK,
//                        DeviceSettings.AUTO_OVERWRITE_EXPORT,
//                        getTtAppCtx().getDeviceSettings().getPrefs())
//
//                .setMessage("There is already a folder that that contains a previous export. Would you like to change the directory or overwrite it?")
//
//                .setPositiveButton("Overwrite", (dialogInterface, i, value) -> export(dir), 2)
////                .setNeutralButton("Change", new DontAskAgainDialog.OnClickListener() {
////                    @Override
////                    public void onClick(DialogInterface dialogInterface, int i, Object value) {
////                        selectDirectory(dir.getAbsolutePath());
////                    }
////                }, 1)
//                .setNegativeButton("Cancel", null, null)
//
//                .show();
//            } else {
//                if (getTtAppCtx().getDeviceSettings().getAutoOverwriteExport() == 2) {
//                    export(dir);
//                } else {
//                    //selectDirectory(directory);
//                    Toast.makeText(ExportActivity.this, "Unsupported Action", Toast.LENGTH_LONG).show();
//                }
//            }
//        } else {
//            export(dir);
//        }
//    }


//    private void internalizeImages(final MediaAccessLayer mal, final Uri directory) {
//        throw new RuntimeException("internalizeImages: Not implemented yet");
//        new Thread(() -> mal.internalizeImages(new MediaAccessLayer.SimpleMalListener(){
//            @Override
//            public void internalizeImagesCompleted(List<TtImage> imagesInternalized, final List<TtImage> failedImages) {
//                runOnUiThread(() -> {
//                    progCircle.hide();
//
//                    if (failedImages.size() > 0) {
//                        new AlertDialog.Builder(ExportActivity.this)
//                                .setMessage("Some image files were not found. Would you still like to export the database?")
//                                .setPositiveButton("Export", (dialog, which) -> runOnUiThread(() -> startExport(directory, false)))
//                                .setNeutralButton(R.string.str_cancel, null);
//                    }
//                    else {
//                        startExport(directory, false);
//                    }
//                });
//            }
//
//            @Override
//            public void internalizeImagesFailed(List<TtImage> imagesInternalized, List<TtImage> failedImages, String failedReason) {
//                runOnUiThread(() -> {
//                    progCircle.hide();
//
//                    new AlertDialog.Builder(ExportActivity.this)
//                            .setMessage("There was an issue internalizing images to the media database. Would you still like to export the database?")
//                            .setPositiveButton("Export", (dialog, which) -> startExport(directory, false))
//                            .setNeutralButton(R.string.str_cancel, null);
//                });
//            }
//        })).start();
//    }
}
