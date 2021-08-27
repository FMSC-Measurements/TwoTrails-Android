package com.usda.fmsc.twotrails.logic;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;

import java.io.File;
import java.io.IOException;

public class SettingsLogic {
    public static void reset(final TwoTrailsApp context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Reset Settings");
        alert.setMessage("Do you want to reset all the settings in TwoTrails?");

        alert.setPositiveButton("Reset", (dialogInterface, i) -> {
            context.getDeviceSettings().reset();
            context.getArcGISTools().reset();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void clearLog(final TwoTrailsApp context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Clear Log File");
        alert.setMessage("Do you want clear the log file?");

        alert.setPositiveButton("Clear", (dialogInterface, i) -> {
            context.getReport().clearReport();
            Toast.makeText(context, "Log Cleared", Toast.LENGTH_SHORT).show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void exportReport(final TtActivity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        try {
            if (activity.getTtAppCtx().hasDAL()) {
                dialog.setMessage("Would you like to include the current project into the report?")
                        .setPositiveButton(R.string.str_yes, (dialog1, which) -> {
                            try {
                                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(),true));
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        })
                        .setNegativeButton(R.string.str_no, (dialog12, which) -> {
                            try {
                                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(),false));
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        })
                        .setNeutralButton(R.string.str_cancel, null)
                        .show();
            } else {
                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(), false));
            }
        } catch (IOException e) {
            //
        }
    }

//    private static Snackbar snackbar;
    private static void onExportReportComplete(final Activity activity, final File filepath) {
        if (filepath != null) {
            activity.runOnUiThread(() -> {
                if (AndroidUtils.Device.isInternetAvailable(activity)) {
                    new AlertDialog.Builder(activity)
                            .setMessage("Would you like to send the report to the developer team to help prevent future crashes?")
                            .setPositiveButton("Send", (dialog, which) -> TtUtils.SendEmailToDev(activity, filepath, true))
                            .setNeutralButton("Don't Send", null)
                            .show();
                } else {
                    Toast.makeText(activity, "Report Exported to Documents/TwoTrailsFiles/" + FileUtils.getFileName(filepath.getName()), Toast.LENGTH_LONG).show();
                }
            });

//            snackbar = Snackbar.make(activity.findViewById(android.R.id.content), "Report Exported", Snackbar.LENGTH_LONG)
//                    .setAction("View", new View.OnClickListener() {
//
//                        @Override
//                        public void onClick(View v) {
//                            Intent intent = new Intent(Intent.ACTION_VIEW);
//                            intent.setDataAndType(Uri.parseNmea(TtUtils.getTtFileDir()), "resource/folder");
//
//                            if (snackbar != null)
//                                snackbar.dismiss();
//
//                            activity.startActivity(Intent.createChooser(intent, "Open folder"));
//                        }
//                    })
//                    .setActionTextColor(AndroidUtils.UI.getColor(activity, R.color.primaryLighter));
//
//            AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);
//
//            snackbar.show();
        } else {
            Toast.makeText(activity, "Report failed to export", Toast.LENGTH_LONG).show();
        }
    }


    public static void enterCode(final TwoTrailsApp context) {
        final InputDialog idialog = new InputDialog(context);

        idialog.setPositiveButton(R.string.str_ok, (dialog, which) -> {
            switch (idialog.getText().toLowerCase()) {
                case "dev":
                case "developer": {
                    context.getDeviceSettings().enabledDevelopterOptions(true);
                    Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_LONG).show();
                    context.getReport().writeDebug("Developer Mode: Enabled", "SettingsLogic:enterCode");
                    break;
                }
                case "disable dev":
                case "disable developer": {
                    context.getDeviceSettings().enabledDevelopterOptions(false);
                    Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_LONG).show();
                    context.getReport().writeDebug("Developer Mode: Disabled", "SettingsLogic:enterCode");
                    break;
                }
                case "dbg":
                case "debug" : {
                    context.getDeviceSettings().enabledDebugMode(true);
                    Toast.makeText(context, "Debug Mode Enabled", Toast.LENGTH_LONG).show();
                    context.getReport().writeDebug("Debug Mode: Enabled", "SettingsLogic:enterCode");
                    break;
                }
                case "disable dbg":
                case "disable debug" : {
                    context.getDeviceSettings().enabledDebugMode(false);
                    Toast.makeText(context, "Debug Mode Disabled", Toast.LENGTH_LONG).show();
                    context.getReport().writeDebug("Debug Mode: Disabled", "SettingsLogic:enterCode");
                    break;
                }
            }

        })
        .setNeutralButton(R.string.str_cancel, null)
        .show();
    }
}
