package com.usda.fmsc.twotrails.fragments.project;

import android.content.SharedPreferences;
import android.os.Bundle;
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

        EditTextPreference editTextPref = (EditTextPreference) findPreference(TtAppCtx.getProjectSettings().PROJECT_ID);
        editTextPref.setSummary(TtAppCtx.getDAL().getProjectID());

        editTextPref = (EditTextPreference) findPreference(TtAppCtx.getProjectSettings().DESCRIPTION);
        editTextPref.setSummary(TtAppCtx.getDAL().getProjectDescription());

        editTextPref = (EditTextPreference) findPreference(TtAppCtx.getProjectSettings().DISTRICT);
        editTextPref.setSummary(TtAppCtx.getProjectSettings().getDistrict());

        editTextPref = (EditTextPreference) findPreference(TtAppCtx.getProjectSettings().FOREST);
        editTextPref.setSummary(TtAppCtx.getProjectSettings().getForest());

        editTextPref = (EditTextPreference) findPreference(TtAppCtx.getProjectSettings().REGION);
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
            case TtAppCtx.getProjectSettings().PROJECT_ID:
                TtAppCtx.getDAL().setProjectID(value);
                break;
            case TtAppCtx.getProjectSettings().DESCRIPTION:
                TtAppCtx.getDAL().setProjectDescription(value);
                break;
            case TtAppCtx.getProjectSettings().DISTRICT:
                TtAppCtx.getDAL().setProjectDistrict(value);
                break;
            case TtAppCtx.getProjectSettings().FOREST:
                TtAppCtx.getDAL().setProjectForest(value);
                break;
            case TtAppCtx.getProjectSettings().REGION:
                TtAppCtx.getDAL().setProjectRegion(value);
                break;
        }
    }
}
