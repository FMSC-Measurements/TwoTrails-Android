package com.usda.fmsc.twotrails.fragments.project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.usda.fmsc.twotrails.ProjectSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;


public class ProjectFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TwoTrailsApp TtAppCtx;

    public static ProjectFragment newInstance() {
        return new ProjectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TtAppCtx = TwoTrailsApp.getInstance();

        addPreferencesFromResource(R.xml.project_settings);

        EditTextPreference editTextPref = (EditTextPreference) findPreference(ProjectSettings.PROJECT_ID);
        editTextPref.setSummary(TtAppCtx.getDAL().getProjectID());

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.DESCRIPTION);
        editTextPref.setSummary(TtAppCtx.getDAL().getProjectDescription());

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.DISTRICT);
        editTextPref.setSummary(TtAppCtx.getProjectSettings().getDistrict());

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.FOREST);
        editTextPref.setSummary(TtAppCtx.getProjectSettings().getForest());

        editTextPref = (EditTextPreference) findPreference(ProjectSettings.REGION);
        editTextPref.setSummary(TtAppCtx.getProjectSettings().getRegion());
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

        switch (key) {
            case ProjectSettings.PROJECT_ID:
                TtAppCtx.getDAL().setProjectID(value);
                break;
            case ProjectSettings.DESCRIPTION:
                TtAppCtx.getDAL().setProjectDescription(value);
                break;
            case ProjectSettings.DISTRICT:
                TtAppCtx.getDAL().setProjectDistrict(value);
                break;
            case ProjectSettings.FOREST:
                TtAppCtx.getDAL().setProjectForest(value);
                break;
            case ProjectSettings.REGION:
                TtAppCtx.getDAL().setProjectRegion(value);
                break;
        }
    }
}
