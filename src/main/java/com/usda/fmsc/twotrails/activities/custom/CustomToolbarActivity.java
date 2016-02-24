package com.usda.fmsc.twotrails.activities.custom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.usda.fmsc.twotrails.R;

public class CustomToolbarActivity extends AppCompatActivity {
    private final String DEFAULT_EXIT_WARNING = "Press again to exit.";

    private Toolbar toolbar;
    private boolean exit, useExitWarning;
    private Toast exitToast;

    private String exitWarningText = DEFAULT_EXIT_WARNING;

    protected static final int INVALID_INDEX = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupToolbar();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupToolbar();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupToolbar();
    }

    private void setupToolbar() {
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
    public boolean onTouchEvent(MotionEvent event) {
        exit = false;
        return super.onTouchEvent(event);
    }

    public void setUseExitWarning(boolean useWarning) {
        useExitWarning = useWarning;
    }

    public void setExitWarningText(String warningText) {
        exitWarningText = warningText;
    }
}