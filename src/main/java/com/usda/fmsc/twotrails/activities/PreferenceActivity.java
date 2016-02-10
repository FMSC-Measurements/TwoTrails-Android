package com.usda.fmsc.twotrails.activities;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.usda.fmsc.twotrails.activities.custom.AppCompatPreferenceActivity;
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

            prefClearLog = findPreference("prefClearLog");
            prefExportReport = findPreference("prefExportReport");
            prefResetDevice = findPreference("prefResetDevice");


            prefResetDevice.setOnPreferenceClickListener(resetDeviceListener);

            prefClearLog.setOnPreferenceClickListener(clearLogListener);
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

                alert.setNegativeButton(R.string.str_cancel, null);

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

                alert.setNegativeButton(R.string.str_cancel, null);

                alert.show();

                return false;
            }
        };

    }
}
