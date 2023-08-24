package com.usda.fmsc.twotrails.fragments.settings;


import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.activities.contracts.CreateZipDocument;
import com.usda.fmsc.twotrails.fragments.TtBasePrefFragment;
import com.usda.fmsc.twotrails.logic.SettingsLogic;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.Locale;

public class MiscSettingsFragment extends TtBasePrefFragment {

    private final ActivityResultLauncher<String> exportReportFileOnResult = registerForActivityResult(new CreateZipDocument(), result -> {
        if (result != null) {
            SettingsLogic.exportReport((TtActivity) getActivity(), result);
        }
    });

    private final ActivityResultLauncher<String> exportProjectsOnResult = registerForActivityResult(new CreateZipDocument(), result -> {
        if (result != null) {
            if (TtUtils.exportProjects(getTtAppCtx(), result)) {
                Toast.makeText(getActivity(), "All Projects Exported.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Error exporting all projects.", Toast.LENGTH_LONG).show();
            }
        }
    });

    private final ActivityResultLauncher<String> dataDumpOnResult = registerForActivityResult(new CreateZipDocument(), result -> {
        if (result != null) {
            Toast.makeText(getActivity(), "Dumping.. This could take a while.", Toast.LENGTH_LONG).show();

            if (TtUtils.dataDump(getTtAppCtx(), result)) {
                getTtAppCtx().runOnCurrentUIThread(() -> Toast.makeText(getActivity(), "App Data Dumped.", Toast.LENGTH_LONG).show());
            } else {
                getTtAppCtx().runOnCurrentUIThread(() -> Toast.makeText(getActivity(), "Error dumping app data.", Toast.LENGTH_LONG).show());
            }
        }
    });


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_other_settings, rootKey);

        Preference pref = findPreference(getString(R.string.set_RESET));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                SettingsLogic.reset((TtActivity) getActivity());
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_CLEAR_LOG));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                SettingsLogic.clearLog(getActivity(), getTtAppCtx());
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_EXPORT_REPORT));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                exportReportFileOnResult.launch(String.format(Locale.getDefault(), "TwoTrailsReport_%s.zip", TtUtils.Date.nowToString()));
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_CODE));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                SettingsLogic.enterCode((TtActivity) getActivity());
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_EXPORT_PROJECTS));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                exportProjectsOnResult.launch(String.format(Locale.getDefault(), "TwoTrailsProjects_%s.zip", TtUtils.Date.nowToString()));
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_APP_DATA_DUMP));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                    dataDumpOnResult.launch(String.format(Locale.getDefault(), "TwoTrailsDataDump_%s.zip", TtUtils.Date.nowToString()));
                return false;
            });
        }

        TtCustomToolbarActivity activity = (TtCustomToolbarActivity)getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();

            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle("Other Settings");
            }
        }
    }
}