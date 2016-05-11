package com.usda.fmsc.twotrails.logic;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class SettingsLogic {

    public static void reset(final Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Reset Settings");
        alert.setMessage("Do you want to reset all the settings in TwoTrails?");

        alert.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Global.Settings.DeviceSettings.reset();
                ArcGISTools.reset();
            }
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void clearLog(final Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Clear Log File");
        alert.setMessage("Do you want clear the log file?");

        alert.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                TtUtils.TtReport.clearReport();
                Toast.makeText(context, "Log Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNeutralButton(R.string.str_cancel, null);

        alert.show();
    }

    public static void exportReport(final Activity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        if (Global.getDAL() != null) {
            dialog.setMessage("Would you like to include the current project into the report?")
                    .setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onExportReportComplete(TtUtils.exportReport(Global.getDAL()), activity);
                        }
                    })
                    .setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onExportReportComplete(TtUtils.exportReport(null), activity);
                        }
                    })
                    .setNeutralButton(R.string.str_cancel, null)
                    .show();
        } else {
            onExportReportComplete(TtUtils.exportReport(null), activity);
        }
    }

    static Snackbar snackbar;
    private static void onExportReportComplete(String filepath, final Activity activity) {
        if (filepath != null) {
            snackbar = Snackbar.make(activity.findViewById(android.R.id.content), "Report Exported", Snackbar.LENGTH_LONG)
                    .setAction("View", new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(Global.getTtFileDir()), "resource/folder");

                            if (snackbar != null)
                                snackbar.dismiss();

                            activity.startActivity(Intent.createChooser(intent, "Open folder"));
                        }
                    })
                    .setActionTextColor(AndroidUtils.UI.getColor(activity, R.color.primaryLighter));

            AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);

            snackbar.show();
        } else {
            Toast.makeText(activity, "Report failed to export", Toast.LENGTH_LONG).show();
        }
    }


    public static void enterCode(final Context context) {
        final InputDialog idialog = new InputDialog(context);

        idialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (idialog.getText().toLowerCase()) {
                    case "dev": {
                        Global.Settings.DeviceSettings.enabledDevelopterOptions(true);
                        Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_LONG).show();
                        break;
                    }
                    case "disable dev": {
                        Global.Settings.DeviceSettings.enabledDevelopterOptions(false);
                        Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_LONG).show();
                        break;
                    }
                }

            }
        })
        .setNeutralButton(R.string.str_cancel, null)
        .show();
    }
}
