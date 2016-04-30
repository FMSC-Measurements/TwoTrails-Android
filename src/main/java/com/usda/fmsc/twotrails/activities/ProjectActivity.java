package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.view.MenuItem;

import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.fragments.project.ProjectFragment;

public class ProjectActivity extends CustomToolbarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_project);

        getFragmentManager().beginTransaction()
                .replace(R.id.content2, ProjectFragment.newInstance()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
