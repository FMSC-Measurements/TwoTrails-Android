package com.usda.fmsc.twotrails.activities.base;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MenuRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.SettingsActivity;

public abstract class TtCustomToolbarActivity extends TtActivity {
    private final String DEFAULT_EXIT_WARNING = "Press again to exit.";

    private Toolbar toolbar;
    private boolean exit, useExitWarning;
    private Toast exitToast;

    private String exitWarningText = DEFAULT_EXIT_WARNING;

    protected static final int INVALID_INDEX = -1;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupToolbar(findViewById(android.R.id.content));
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupToolbar(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupToolbar(view);
    }

    protected final void inflateMenu(@MenuRes int menuRes, Menu menu) {
        getMenuInflater().inflate(menuRes, menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    protected void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    protected Toolbar getToolbar() {
        return toolbar;
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toolbar.showOverflowMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (useExitWarning) {
            if (exit) {
                if (exitToast != null) {
                    exitToast.cancel();
                }
            } else {
                //warn before leaving
                exit = true;
                exitToast = Toast.makeText(this, exitWarningText, Toast.LENGTH_SHORT);
                exitToast.show();
                return;
            }
        }

        super.onBackPressed();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        exit = false;
        return super.dispatchTouchEvent(event);
    }


    public void setUseExitWarning(boolean useWarning) {
        useExitWarning = useWarning;
    }

    public void setExitWarningText(String warningText) {
        exitWarningText = warningText;
    }

    protected boolean isAboutToExit() {
        return exit;
    }



    private final ActivityResultLauncher<Intent> openAppSettingsForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        onAppSettingsUpdated();
    });

    protected void openSettings() {
        openAppSettingsForResult.launch(new Intent(this, SettingsActivity.class));
    }
    protected void openSettings(String settingsPage) {
        openAppSettingsForResult.launch(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, settingsPage));
    }

    protected void onAppSettingsUpdated() { }
}
