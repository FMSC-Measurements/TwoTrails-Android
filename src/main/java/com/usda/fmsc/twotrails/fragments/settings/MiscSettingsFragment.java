package com.usda.fmsc.twotrails.fragments.settings;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.ActionBar;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.logic.SettingsLogic;

public class MiscSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_other_settings);
        setHasOptionsMenu(true);

        findPreference(getString(R.string.set_RESET)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.reset(getActivity());
                return false;
            }
        });

        findPreference(getString(R.string.set_CLEAR_LOG)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.clearLog(getActivity());
                return false;
            }
        });

        findPreference(getString(R.string.set_EXPORT_REPORT)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.exportReport(getActivity());
                return false;
            }
        });

        findPreference(getString(R.string.set_CODE)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsLogic.enterCode(getActivity());
                return false;
            }
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