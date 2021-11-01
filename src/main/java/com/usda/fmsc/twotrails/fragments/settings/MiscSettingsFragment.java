package com.usda.fmsc.twotrails.fragments.settings;


import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.fragments.TtBasePrefFragment;
import com.usda.fmsc.twotrails.logic.SettingsLogic;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.Locale;

public class MiscSettingsFragment extends TtBasePrefFragment {

    private final ActivityResultLauncher<String> exportFileOnResult = registerForActivityResult(new ActivityResultContracts.CreateDocument(), result -> {
        if (result != null) {
            SettingsLogic.exportReport((TtActivity) getActivity(), result);
        }
    });


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_other_settings, rootKey);

        setHasOptionsMenu(true);

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
                SettingsLogic.clearLog(getTtAppCtx());
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_EXPORT_REPORT));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                exportFileOnResult.launch(String.format(Locale.getDefault(), "TwoTrailsReport_%s.zip", TtUtils.Date.nowToString()));
                return false;
            });
        }

        pref = findPreference(getString(R.string.set_CODE));
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                SettingsLogic.enterCode(getTtAppCtx());
                return false;
            });
        }

        CustomToolbarActivity activity = (CustomToolbarActivity)getActivity();
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