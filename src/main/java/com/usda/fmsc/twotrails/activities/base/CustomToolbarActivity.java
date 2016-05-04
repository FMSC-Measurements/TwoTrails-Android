package com.usda.fmsc.twotrails.activities.base;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
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

    protected void setupToolbar(View view) {
        toolbar = (Toolbar)view.findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (Build.VERSION.SDK_INT  < Build.VERSION_CODES.LOLLIPOP) {
                AndroidUtils.UI.setHomeIndicatorIcon(this, R.drawable.ic_arrow_back_white_24dp);
            }

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
}
