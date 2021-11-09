package com.usda.fmsc.twotrails.activities;


import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import com.mukesh.MarkdownView;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;

public class PrivacyPolicyActivity extends CustomToolbarActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.activity_privacy_policy);

        MarkdownView mdView = findViewById(R.id.mdView);
        mdView.loadMarkdownFromAssets("twotrails-privacy-policy.md");
    }
}