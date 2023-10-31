package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.annotation.XmlRes;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.fragments.settings.DeviceSettingsFragment;
import com.usda.fmsc.twotrails.fragments.settings.MiscSettingsFragment;

public class SettingsActivity extends TtCustomToolbarActivity {
    public static final String SETTINGS_PAGE = "settings_page";

    public static final String MAIN_SETTINGS_PAGE = "main";
    public static final String DEVICE_SETUP_SETTINGS_PAGE = "devSetup";
    public static final String POINT_SETTINGS_PAGE = "pointSetup";
    public static final String MAP_SETTINGS_PAGE = "mapSetup";
    public static final String MEDIA_SETTINGS_PAGE = "mediaSetup";
    public static final String DIALOG_SETTINGS_PAGE = "diagSetup";
    public static final String MISC_SETTINGS_PAGE = "miscSetup";
    public static final String SAT_SETTINGS_PAGE = "satSetup";

    public static final String GPS_SETTINGS_PAGE = "gpsSetup";
    public static final String LASER_SETTINGS_PAGE = "rfSetup";
    public static final String VN_SETTINGS_PAGE = "vnSetup";

    public static final String POINT_GPS_SETTINGS_PAGE = "gpsPointSetup";
    public static final String POINT_WALK_SETTINGS_PAGE = "walkPointSetup";
    public static final String POINT_TAKE5_SETTINGS_PAGE = "take5PointSetup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();
        String page = MAIN_SETTINGS_PAGE;

        if (intent.getExtras() != null) {
            page = getIntent().getStringExtra(SETTINGS_PAGE);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content, getSettingsFragment(page)).commit();
    }

    @Override
    public boolean requiresGpsService() {
        return true;
    }

    @Override
    public boolean requiresRFService() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static Fragment getSettingsFragment(String key) {
        Fragment frag = null;

        switch (key) {
            case MISC_SETTINGS_PAGE: frag = new MiscSettingsFragment(); break;
            case GPS_SETTINGS_PAGE:
            case LASER_SETTINGS_PAGE:
            case VN_SETTINGS_PAGE: frag = DeviceSettingsFragment.newInstance(key); break;
            case MAIN_SETTINGS_PAGE:
            case DEVICE_SETUP_SETTINGS_PAGE:
            case POINT_SETTINGS_PAGE:
            case MAP_SETTINGS_PAGE:
            case MEDIA_SETTINGS_PAGE:
            case POINT_GPS_SETTINGS_PAGE:
            case POINT_TAKE5_SETTINGS_PAGE:
            case POINT_WALK_SETTINGS_PAGE:
            case DIALOG_SETTINGS_PAGE:
            case SAT_SETTINGS_PAGE: frag = SettingsFragment.newInstance(key); break;
        }

        return frag;
    }

    public static  @XmlRes int getSettingsPageRes(String key) {
        switch (key) {
            case MAIN_SETTINGS_PAGE: return R.xml.pref_main;
            case DEVICE_SETUP_SETTINGS_PAGE: return R.xml.pref_dev_settings;
            case POINT_SETTINGS_PAGE: return R.xml.pref_point_settings;
            case POINT_GPS_SETTINGS_PAGE: return R.xml.pref_point_gps_settings;
            case POINT_TAKE5_SETTINGS_PAGE: return R.xml.pref_point_take5_settings;
            case POINT_WALK_SETTINGS_PAGE: return R.xml.pref_point_walk_settings;
            case MAP_SETTINGS_PAGE: return R.xml.pref_map_settings;
            case MEDIA_SETTINGS_PAGE: return R.xml.pref_media_settings;
            case MISC_SETTINGS_PAGE: return R.xml.pref_other_settings;
            case DIALOG_SETTINGS_PAGE: return R.xml.pref_dialog_settings;
            case SAT_SETTINGS_PAGE: return R.xml.pref_sat_settings;
            default: return 0;
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        public static final String SETTINGS_PAGE = "CurrentPage";

        private String settingsPageKey;

        public static SettingsFragment newInstance(String settingsKey) {
            SettingsFragment fragment = new SettingsFragment();
            Bundle args = new Bundle();
            args.putString(SETTINGS_PAGE, settingsKey);
            fragment.setArguments(args);
            return fragment;
        }

        public SettingsFragment() { }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            @XmlRes int settingsPage = R.xml.pref_main;

            Bundle bundle = getArguments();
            if (bundle != null && bundle.containsKey(SETTINGS_PAGE) && (settingsPageKey = bundle.getString(SETTINGS_PAGE)) != null) {
                settingsPage = getSettingsPageRes(settingsPageKey);
            }

            setPreferencesFromResource(settingsPage, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();

            String settingsTitle = "Settings";

            switch (settingsPageKey) {
                case DEVICE_SETUP_SETTINGS_PAGE: settingsTitle = "Device Setup"; break;
                case POINT_SETTINGS_PAGE: settingsTitle = "Point Settings"; break;
                case POINT_GPS_SETTINGS_PAGE: settingsTitle = "GPS Point Settings"; break;
                case POINT_TAKE5_SETTINGS_PAGE: settingsTitle = "Take5 Point Settings"; break;
                case POINT_WALK_SETTINGS_PAGE:  settingsTitle = "Walk Point Settings"; break;
                case MAP_SETTINGS_PAGE: settingsTitle = "Map Settings"; break;
                case MEDIA_SETTINGS_PAGE: settingsTitle = "Media Settings"; break;
                case DIALOG_SETTINGS_PAGE: settingsTitle = "Dialog Settings"; break;
                case SAT_SETTINGS_PAGE: settingsTitle = "Sales Admin Tools"; break;
            }

            ActionBar actionBar = ((TtCustomToolbarActivity)getActivity()).getSupportActionBar();

            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(settingsTitle);
            }
        }



        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            if (preference.hasKey()) {
                Fragment frag = getSettingsFragment(preference.getKey());

                if (frag != null){
                    if (isAdded()) {
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.content, frag)
                                .addToBackStack(frag.getClass().getSimpleName())
                                .commit();
                    } else {
                        TtActivity activity = (TtActivity) getActivity();
                        if (activity != null) {
                            activity.getTtAppCtx().getReport().writeError("FragmentManager not found", "SettingsActivity:onPreferenceTreeClick");
                        }
                    }
                }
            }

            return super.onPreferenceTreeClick(preference);
        }
    }
}
