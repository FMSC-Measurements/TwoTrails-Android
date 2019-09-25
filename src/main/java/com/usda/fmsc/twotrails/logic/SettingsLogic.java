package com.usda.fmsc.twotrails.logic;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;

public class SettingsLogic {
    public static void reset(final Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Reset Settings");
        alert.setMessage("Do you want to reset all the settings in TwoTrails?");

        alert.setPositiveButton("Reset", (dialogInterface, i) -> {
            TwoTrailsApp.getInstance().getDeviceSettings().reset();
            TwoTrailsApp.getInstance().getArcGISTools().reset();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void clearLog(final Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Clear Log File");
        alert.setMessage("Do you want clear the log file?");

        alert.setPositiveButton("Clear", (dialogInterface, i) -> {
            TwoTrailsApp.getInstance().getReport().clearReport();
            Toast.makeText(context, "Log Cleared", Toast.LENGTH_SHORT).show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void exportReport(final Activity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        if (TwoTrailsApp.getInstance().hasDAL()) {
            dialog.setMessage("Would you like to include the current project into the report?")
                    .setPositiveButton(R.string.str_yes, (dialog1, which) -> onExportReportComplete(activity, TtUtils.exportReport(TwoTrailsApp.getInstance().getDAL())))
                    .setNegativeButton(R.string.str_no, (dialog12, which) -> onExportReportComplete(activity, TtUtils.exportReport(null)))
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        } else {
            onExportReportComplete(activity, TtUtils.exportReport(null));
        }
    }

//    private static Snackbar snackbar;
    private static void onExportReportComplete(final Activity activity, final String filepath) {
        if (filepath != null) {
            AndroidUtils.Device.isInternetAvailable(internetAvailable -> activity.runOnUiThread(() -> {
                if (internetAvailable) {
                    new AlertDialog.Builder(activity)
                            .setMessage("Would you like to send the report to the developer team to help prevent future crashes?")
                            .setPositiveButton("Send", (dialog, which) -> TtUtils.SendEmailToDev(activity, filepath))
                            .setNeutralButton("Don't Send", null)
                            .show();
                } else {
                    Toast.makeText(activity, "Report Exported to Documents/TwoTrailsFiles/" + FileUtils.getFileName(filepath), Toast.LENGTH_LONG).show();
                }
            }));

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


    public static void enterCode(final Context context) {
        final InputDialog idialog = new InputDialog(context);

        idialog.setPositiveButton(R.string.str_ok, (dialog, which) -> {
            switch (idialog.getText().toLowerCase()) {
                case "dev":
                case "developer": {
                    TwoTrailsApp.getInstance().getDeviceSettings().enabledDevelopterOptions(true);
                    Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_LONG).show();
                    TwoTrailsApp.getInstance().getReport().writeDebug("Developer Mode: Enabled", "SettingsLogic:enterCode");
                    break;
                }
                case "disable dev":
                case "disable developer": {
                    TwoTrailsApp.getInstance().getDeviceSettings().enabledDevelopterOptions(false);
                    Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_LONG).show();
                    TwoTrailsApp.getInstance().getReport().writeDebug("Developer Mode: Disabled", "SettingsLogic:enterCode");
                    break;
                }
                case "dbg":
                case "debug" : {
                    TwoTrailsApp.getInstance().getDeviceSettings().enabledDebugMode(true);
                    Toast.makeText(context, "Debug Mode Enabled", Toast.LENGTH_LONG).show();
                    TwoTrailsApp.getInstance().getReport().writeDebug("Debug Mode: Enabled", "SettingsLogic:enterCode");
                    break;
                }
                case "disable dbg":
                case "disable debug" : {
                    TwoTrailsApp.getInstance().getDeviceSettings().enabledDebugMode(false);
                    Toast.makeText(context, "Debug Mode Disabled", Toast.LENGTH_LONG).show();
                    TwoTrailsApp.getInstance().getReport().writeDebug("Debug Mode: Disabled", "SettingsLogic:enterCode");
                    break;
                }
            }

        })
        .setNeutralButton(R.string.str_cancel, null)
        .show();
    }
}
