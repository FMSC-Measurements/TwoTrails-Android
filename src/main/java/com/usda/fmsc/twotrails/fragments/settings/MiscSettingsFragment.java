package com.usda.fmsc.twotrails.fragments.settings;


import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import androidx.appcompat.app.ActionBar;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.logic.SettingsLogic;

public class MiscSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_other_settings, rootKey);

        setHasOptionsMenu(true);

        findPreference(getString(R.string.set_RESET)).setOnPreferenceClickListener(preference -> {
            SettingsLogic.reset(getActivity());
            return false;
        });

        findPreference(getString(R.string.set_CLEAR_LOG)).setOnPreferenceClickListener(preference -> {
            SettingsLogic.clearLog(getActivity());
            return false;
        });

        findPreference(getString(R.string.set_EXPORT_REPORT)).setOnPreferenceClickListener(preference -> {
            SettingsLogic.exportReport(getActivity());
            return false;
        });

        findPreference(getString(R.string.set_CODE)).setOnPreferenceClickListener(preference -> {
            SettingsLogic.enterCode(getActivity());
            return false;
        });

        ActionBar actionBar = ((CustomToolbarActivity)getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("Other Settings");
        }
    }
}