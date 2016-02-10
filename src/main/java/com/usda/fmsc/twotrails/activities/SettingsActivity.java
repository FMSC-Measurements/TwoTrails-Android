package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.usda.fmsc.twotrails.fragments.settings.SettingsFragment;
import com.usda.fmsc.twotrails.R;

public class SettingsActivity extends AppCompatActivity {
    public static final String SETTINGS_PAGE = "settings_page";

    public static final String MAIN_SETTINGS_PAGE = "main";
    public static final String GPS_SETTINGS_PAGE = "gpsSetup";
    public static final String LASER_SETTINGS_PAGE = "rfSetup";
    public static final String FILTER_GPS_SETTINGS_PAGE = "gpsPointSetup";
    public static final String FILTER_WALK_SETTINGS_PAGE = "walkPointSetup";
    public static final String FILTER_TAKE5_SETTINGS_PAGE = "take5PointSetup";
    public static final String MAP_SETTINGS_PAGE = "mapSetup";
    public static final String DIALOG_SETTINGS_PAGE = "diagSetup";
    public static final String MISC_SETTINGS_PAGE = "miscSetup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();
        String page = MAIN_SETTINGS_PAGE;

        if (intent.getExtras() != null) {
            page = getIntent().getStringExtra(SETTINGS_PAGE);
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.content2, SettingsFragment.newInstance(page)).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
