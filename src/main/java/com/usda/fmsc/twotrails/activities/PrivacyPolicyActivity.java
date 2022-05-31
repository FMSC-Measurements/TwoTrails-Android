package com.usda.fmsc.twotrails.activities;


import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.mukesh.MarkdownView;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;

public class PrivacyPolicyActivity extends TtCustomToolbarActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.activity_privacy_policy);

        MarkdownView mdView = findViewById(R.id.mdView);
        mdView.loadMarkdownFromAssets("twotrails-privacy-policy.md");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();if (itemId == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}