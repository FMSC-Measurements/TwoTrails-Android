package com.usda.fmsc.twotrails.logic;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import android.net.Uri;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsLogic {
    public static void reset(final TtActivity context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Reset Settings");
        alert.setMessage("Do you want to reset all the settings in TwoTrails?");

        alert.setPositiveButton("Reset", (dialogInterface, i) -> {
            TwoTrailsApp app = context.getTtAppCtx();
            app.getProjectSettings().clearRecentProjects();
            app.getDeviceSettings().reset();
            app.getArcGISTools().reset();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void clearLog(final Activity activity, TwoTrailsApp app) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle("Clear Log File");
        alert.setMessage("Do you want clear the log file?");

        alert.setPositiveButton("Clear", (dialogInterface, i) -> {
            app.getReport().clearReport();
            Toast.makeText(activity, "Log Cleared", Toast.LENGTH_SHORT).show();
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void exportReport(final TtActivity activity, Uri externalReportPath) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        String fileName = FileUtils.getFileName(externalReportPath.getPath());

        try {
            if (activity.getTtAppCtx().hasDAL()) {
                dialog.setMessage("Would you like to include the current project into the report?")
                        .setPositiveButton(R.string.str_yes, (dialog1, which) -> {
                            try {
                                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(), fileName, true), externalReportPath);
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        })
                        .setNegativeButton(R.string.str_no, (dialog12, which) -> {
                            try {
                                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(), fileName,false), externalReportPath);
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        })
                        .setNeutralButton(R.string.str_cancel, null)
                        .show();
            } else {
                onExportReportComplete(activity, TtUtils.exportReport(activity.getTtAppCtx(), fileName, false), externalReportPath);
            }
        } catch (IOException e) {
            //
        }
    }

    private static void onExportReportComplete(final TtActivity activity, final File reportPath, final Uri externalReportPath) {
        if (reportPath != null) {
            if (externalReportPath != null) {
                try {
                    AndroidUtils.Files.copyFile(activity.getTtAppCtx(), Uri.fromFile(reportPath), externalReportPath);
                } catch (IOException e) {
                    activity.getTtAppCtx().getReport().writeError("Unable to copy report", "SettingsLogic:onExportReportComplete");
                    Toast.makeText(activity, "Unable to export Report", Toast.LENGTH_LONG).show();
                    return;
                } finally {
                    reportPath.delete();
                }
            }

            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Report Exported", Toast.LENGTH_LONG).show();

                if (AndroidUtils.Device.isInternetAvailable(activity)) {
                    new AlertDialog.Builder(activity)
                            .setMessage("Would you like to send the report to the developer team to help prevent future crashes?")
                            .setPositiveButton("Send", (dialog, which) -> TtUtils.SendEmailToDev(activity, reportPath, true))
                            .setNeutralButton("Don't Send", null)
                            .show();
                }
            });
        } else {
            Toast.makeText(activity, "Report failed to export", Toast.LENGTH_LONG).show();
        }
    }

    public static void enterCode(final TtActivity activity) {
        final InputDialog idialog = new InputDialog(activity);

        TwoTrailsApp context = activity.getTtAppCtx();

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
