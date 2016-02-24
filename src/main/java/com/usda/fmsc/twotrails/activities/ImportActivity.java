package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.twotrails.activities.custom.TtAjusterCustomToolbarActivity;
import com.usda.fmsc.twotrails.fragments.imprt.BaseImportFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportGpxFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportTextFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.utilities.Import;

import java.util.regex.Pattern;

public class ImportActivity extends TtAjusterCustomToolbarActivity {
    private static final int OPEN_FILE = 101;

    FloatingActionButton fabImport;
    FABProgressCircleEx fabProgCircle;

    BaseImportFragment fragment;


    PostDelayHandler handler;

    String _FileName;

    boolean ignoreChange, adjust;

    Activity activity;


    BaseImportFragment.Listener listener = new BaseImportFragment.Listener() {
        String message = null;

        @Override
        public void onTaskComplete(Import.ImportResultCode code) {
            switch (code) {
                case Success:
                    fabProgCircle.beginFinalAnimation();
                    Snackbar.make(findViewById(R.id.parent), "File Imported", Snackbar.LENGTH_LONG)
                            .setAction("View Map", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    PolygonAdjuster.adjust(Global.DAL, activity);
                                }
                            })
                            .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter))
                            .show();

                    adjust = true;
                    return;
                case Cancelled:
                    message = "Import Canceled";
                    break;
                case ImportFailure:
                    message = "Import Failed";
                    break;
                case InvalidParams:
                    message = "Invalid Import Params";
                    break;
            }

            fabProgCircle.hide();
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onTaskStart() {
            fabProgCircle.show();
        }
    };

    EditText txtFile;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        activity = this;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        fabImport = (FloatingActionButton)findViewById(R.id.importFabImport);
        fabProgCircle = (FABProgressCircleEx)findViewById(R.id.importFabImportProgressCircle);

        fabProgCircle.attachListener(new FABProgressCircleEx.FABProgressListener() {
            @Override
            public void onFABProgressAnimationEnd() {
                fabProgCircle.hide();
            }
        });

        //View view = findViewById(R.id.importSv);
        txtFile = (EditText)findViewById(R.id.importTxtFile);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtFile);
        //AndroidUtils.UI.hideKeyboardOnTouch(findViewById(R.id.fragmentTouchContent), txtFile);


        handler = new PostDelayHandler(1000);

        txtFile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (!ignoreChange) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateFileName(s.toString());
                        }
                    });
                }

                ignoreChange = false;

                if (s.length() < 1) {
                    fabImport.hide();
                } else {
                    fabImport.show();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OPEN_FILE: {
                if (data != null) {
                    updateFileName(data.getData().getPath());
                }
                break;
            }
        }
    }

    private void updateFileName(String filename) {
        boolean fragUpdated = false;

        _FileName = filename;
        String[] parts = _FileName.split(Pattern.quote("."));

        if (parts.length > 1) {
            switch (parts[parts.length - 1].toLowerCase()) {
                case "txt":
                case "csv": {
                    if (fragment == null || !(fragment instanceof ImportTextFragment)) {
                        fragment = ImportTextFragment.newInstance(_FileName);
                        fragUpdated = true;
                    } else {
                        fragment.updateFileName(_FileName);
                    }
                    break;
                }
                case "gpx": {
                    if (fragment == null || !(fragment instanceof ImportGpxFragment)) {
                        fragment = ImportGpxFragment.newInstance(_FileName);
                        fragUpdated = true;
                    } else {
                        fragment.updateFileName(_FileName);
                    }
                    break;
                }
                default: {
                    Toast.makeText(this, "Invalid File Type", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (fragUpdated) {
                ignoreChange = true;
                txtFile.setText(_FileName);
                txtFile.setSelection(_FileName.length() - 1);
                fragment.setListener(listener);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContent, fragment).commit();
            }
        } else if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                super.onBackPressed();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adjust) {
            PolygonAdjuster.adjust(Global.DAL, Global.getMainActivity(), true);
        }
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        return true;
    }


    @Override
    protected void onAdjusterStopped(PolygonAdjuster.AdjustResult result) {
        super.onAdjusterStopped(result);

        if (result == PolygonAdjuster.AdjustResult.SUCCESSFUL) {
            adjust = false;
            startActivity(new Intent(this, MapActivity.class));
        } else {
            Toast.makeText(this, "Polygon(s) failed to adjust.", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnImport(View view) {
        if (fragment != null) {
            fragment.importFile(Global.DAL);
        }
    }

    public void btnImportSelect(View view) {
        String[] extraMimes = {"file/*.csv", "file/*.gpx"};
        AndroidUtils.App.openFileIntent(this, "file/*.txt", extraMimes, OPEN_FILE);
    }
}