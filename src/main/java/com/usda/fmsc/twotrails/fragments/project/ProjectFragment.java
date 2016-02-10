package com.usda.fmsc.twotrails.fragments.project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;

public class ProjectFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static ProjectFragment newInstance() {
        return new ProjectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.project_settings);

        EditTextPreference editTextPref = (EditTextPreference) findPreference(Global.Settings.ProjectSettings.PROJECT_ID);
        editTextPref.setSummary(Global.Settings.ProjectSettings.getProjectId());

        editTextPref = (EditTextPreference) findPreference(Global.Settings.ProjectSettings.DESCRIPTION);
        editTextPref.setSummary(Global.Settings.ProjectSettings.getDescription());

        editTextPref = (EditTextPreference) findPreference(Global.Settings.ProjectSettings.DISTRICT);
        editTextPref.setSummary(Global.Settings.ProjectSettings.getDistrict());

        editTextPref = (EditTextPreference) findPreference(Global.Settings.ProjectSettings.FOREST);
        editTextPref.setSummary(Global.Settings.ProjectSettings.getForest());

        editTextPref = (EditTextPreference) findPreference(Global.Settings.ProjectSettings.REGION);
        editTextPref.setSummary(Global.Settings.ProjectSettings.getRegion());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        boolean dz = false;
        String value = null;

        if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            value = etp.getText();
            pref.setSummary(value);
        } else if (pref instanceof CheckBoxPreference) {
            CheckBoxPreference cbp = (CheckBoxPreference) pref;
            dz = cbp.isChecked();
        }

        switch (key) {
            case Global.Settings.ProjectSettings.PROJECT_ID:
                Global.DAL.setProjectID(value);
                break;
            case Global.Settings.ProjectSettings.DESCRIPTION:
                Global.DAL.setProjectDescription(value);
                break;
            case Global.Settings.ProjectSettings.DISTRICT:
                Global.DAL.setProjectDistrict(value);
                break;
            case Global.Settings.ProjectSettings.FOREST:
                Global.DAL.setProjectForest(value);
                break;
            case Global.Settings.ProjectSettings.REGION:
                Global.DAL.setProjectRegion(value);
                break;
        }
    }
}
