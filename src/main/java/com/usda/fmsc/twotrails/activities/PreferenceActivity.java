package com.usda.fmsc.twotrails.activities;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.preferences.AppCompatPreferenceActivity;
import com.usda.fmsc.twotrails.fragments.settings.PreferenceFragmentEx;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.List;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class PreferenceActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (Build.VERSION.SDK_INT  < Build.VERSION_CODES.LOLLIPOP) {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);

        setContentView(R.layout.activity_preferences);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        //bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        bar.setTitle(R.string.title_activity_settings);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || DeviceSetupPreferenceFragment.class.getName().equals(fragmentName)
                || MapPreferenceFragment.class.getName().equals(fragmentName)
                || PointSettingsPreferenceFragment.class.getName().equals(fragmentName)
                || DialogPreferenceFragment.class.getName().equals(fragmentName)
                || MiscPreferenceFragment.class.getName().equals(fragmentName);
    }



    public static class DeviceSetupPreferenceFragment extends PreferenceFragmentEx {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_dev_setup);
            setHasOptionsMenu(true);
        }
    }

    public static class PointSettingsPreferenceFragment extends PreferenceFragmentEx {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_point_settings);
            setHasOptionsMenu(true);
        }
    }

    public static class MapPreferenceFragment extends PreferenceFragmentEx {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_map_options);
            setHasOptionsMenu(true);
        }
    }

    public static class DialogPreferenceFragment extends PreferenceFragmentEx {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_diag_options);
            setHasOptionsMenu(true);
        }
    }

    public static class MiscPreferenceFragment extends PreferenceFragmentEx {

        Preference prefExportReport, prefClearLog, prefResetDevice;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_misc_settings);
            setHasOptionsMenu(true);

            prefClearLog = findPreference(getString(R.string.set_CLEAR_LOG));
            prefExportReport = findPreference(getString(R.string.set_EXPORT_REPORT));
            prefResetDevice = findPreference(getString(R.string.set_RESET));

            prefResetDevice.setOnPreferenceClickListener(resetDeviceListener);

            prefClearLog.setOnPreferenceClickListener(clearLogListener);

            prefExportReport.setOnPreferenceClickListener(exportReportListener);
        }

        Preference.OnPreferenceClickListener resetDeviceListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle("Reset Settings");
                alert.setMessage("Do you want to reset all the settings in TwoTrails?");

                alert.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Global.Settings.DeviceSettings.reset();
                    }
                });

                alert.setNeutralButton(R.string.str_cancel, null);

                alert.show();

                return false;
            }
        };

        Preference.OnPreferenceClickListener clearLogListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle("Clear Log File");
                alert.setMessage("Do you want clear the log file?");

                alert.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TtUtils.TtReport.clearReport();
                    }
                });

                alert.setNeutralButton(R.string.str_cancel, null);

                alert.show();

                return false;
            }
        };


        Preference.OnPreferenceClickListener exportReportListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

                if (Global.DAL != null) {
                    dialog.setMessage("Would you like to include the current project into the report?")
                            .setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onExportReportComplete(TtUtils.exportReport(Global.DAL));
                                }
                            })
                            .setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onExportReportComplete(TtUtils.exportReport(null));
                                }
                            })
                            .setNeutralButton(R.string.str_cancel, null)
                            .show();
                } else {
                    onExportReportComplete(TtUtils.exportReport(null));
                }

                return false;
            }
        };

        Snackbar snackbar;
        private void onExportReportComplete(String filepath) {
            if (filepath != null) {
                snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), "Report Exported", Snackbar.LENGTH_LONG)
                        .setAction("View", new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(TtUtils.getTtFileDir()), "resource/folder");

                                if (snackbar != null)
                                    snackbar.dismiss();

                                startActivity(Intent.createChooser(intent, "Open folder"));
                            }
                        })
                        .setActionTextColor(AndroidUtils.UI.getColor(getActivity(), R.color.primaryLighter));

                snackbar.show();
            } else {
                Toast.makeText(getActivity(), "Report failed to export", Toast.LENGTH_LONG).show();
            }
        }
    }
}
