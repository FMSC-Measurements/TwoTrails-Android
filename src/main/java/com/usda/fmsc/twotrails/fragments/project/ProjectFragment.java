package com.usda.fmsc.twotrails.fragments.project;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.usda.fmsc.twotrails.ProjectSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.fragments.TtBasePrefFragment;


public class ProjectFragment extends TtBasePrefFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static ProjectFragment newInstance() {
        return new ProjectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.project_settings);

        EditTextPreference editTextPref = (EditTextPreference) findPreference(ProjectSettings.PROJECT_ID);
        if (editTextPref != null) {
            editTextPref.setSummary(getTtAppCtx().getDAL().getProjectID());
        }

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.DESCRIPTION);
        if (editTextPref != null) {
            editTextPref.setSummary(getTtAppCtx().getDAL().getProjectDescription());
        }

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.DISTRICT);
        if (editTextPref != null) {
            editTextPref.setSummary(getTtAppCtx().getProjectSettings().getDistrict());
        }

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.FOREST);
        if (editTextPref != null) {
            editTextPref.setSummary(getTtAppCtx().getProjectSettings().getForest());
        }

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.REGION);
        if (editTextPref != null) {
            editTextPref.setSummary(getTtAppCtx().getProjectSettings().getRegion());
        }
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
        String value = null;

        if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            value = etp.getText();
            pref.setSummary(value);
        }

        try {
            switch (key) {
                case ProjectSettings.PROJECT_ID:
                    getTtAppCtx().getDAL().setProjectID(value);
                    break;
                case ProjectSettings.DESCRIPTION:
                    getTtAppCtx().getDAL().setProjectDescription(value);
                    break;
                case ProjectSettings.DISTRICT:
                    getTtAppCtx().getDAL().setProjectDistrict(value);
                    break;
                case ProjectSettings.FOREST:
                    getTtAppCtx().getDAL().setProjectForest(value);
                    break;
                case ProjectSettings.REGION:
                    getTtAppCtx().getDAL().setProjectRegion(value);
                    break;
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "ProjectFragment:onSharedPreferenceChanged");
        }
    }
}
