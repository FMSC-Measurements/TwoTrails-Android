package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.graphics.Color;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.fragments.imprt.BaseImportFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportGpxFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportKmlFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportTextFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.utilities.Import;

import java.util.regex.Pattern;

public class ImportActivity extends CustomToolbarActivity {
    private FloatingActionButton fabImport;
    private FABProgressCircleEx fabProgCircle;

    private BaseImportFragment fragment;

    private EditText txtFile;
    private PostDelayHandler handler = new PostDelayHandler(1000), pdhShowFab = new PostDelayHandler(250);

    private boolean ignoreChange, adjust;


    private BaseImportFragment.Listener listener = new BaseImportFragment.Listener() {
        String message = null;

        @Override
        public void onTaskComplete(Import.ImportResultCode code) {
            switch (code) {
                case Success:
                    fabProgCircle.beginFinalAnimation();
                    View view = findViewById(R.id.parent);
                    if (view != null) {
                        Snackbar snackbar = Snackbar.make(view, "File Imported", Snackbar.LENGTH_LONG)
                                .setAction("View Map", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        PolygonAdjuster.adjust(getTtAppCtx().getDAL(), false, new PolygonAdjuster.Listener() {
                                            @Override
                                            public void adjusterStarted() {

                                            }

                                            @Override
                                            public void adjusterStopped(PolygonAdjuster.AdjustResult result, AdjustingException.AdjustingError error) {
                                                if (result == PolygonAdjuster.AdjustResult.SUCCESSFUL) {
                                                    adjust = false;
                                                    startActivity(new Intent(ImportActivity.this, MapActivity.class));
                                                } else {
                                                    Toast.makeText(ImportActivity.this, "Polygon(s) failed to adjust.", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void adjusterRunningSlow() {

                                            }
                                        });
                                    }
                                })
                                .setActionTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.primaryLighter));

                        AndroidUtils.UI.setSnackbarTextColor(snackbar, Color.WHITE);

                        snackbar.show();
                    }

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

        @Override
        public void onReadyToImport(final boolean ready) {
            pdhShowFab.post(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ready)
                                fabImport.show();
                            else
                                fabImport.hide();
                        }
                    });
                }
            });
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        fabImport = findViewById(R.id.importFabImport);
        fabProgCircle = findViewById(R.id.importFabImportProgressCircle);

        fabProgCircle.attachListener(new FABProgressCircleEx.FABProgressListener() {
            @Override
            public void onFABProgressAnimationEnd() {
                fabProgCircle.hide();
            }
        });

        txtFile = findViewById(R.id.importTxtFile);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtFile);


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

        if (requestCode == Consts.Codes.Requests.OPEN_FILE) {
            if (data != null && data.getData() != null && data.getData().getPath() != null) {
                updateFileName(data.getData().getPath());
            } else {
                Toast.makeText(ImportActivity.this, "Unable to update file name.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateFileName(String filename) {
        boolean fragUpdated = false;

        String[] parts = filename.split(Pattern.quote("."));
        String ext = parts[parts.length - 1].toLowerCase();

        if (parts.length > 1) {
            switch (ext) {
                case "txt":
                case "csv": {
                    if (fragment == null || !(fragment instanceof ImportTextFragment)) {
                        fragment = ImportTextFragment.newInstance(filename);
                        fragUpdated = true;
                    } else {
                        fragment.updateFileName(filename);
                    }
                    break;
                }
                case "gpx": {
                    if (fragment == null || !(fragment instanceof ImportGpxFragment)) {
                        fragment = ImportGpxFragment.newInstance(filename);
                        fragUpdated = true;
                    } else {
                        fragment.updateFileName(filename);
                    }
                    break;
                }
                case "kmz":
                case "kml": {

                    if (fragment == null || !(fragment instanceof ImportKmlFragment)) {
                        fragment = ImportKmlFragment.newInstance(filename);
                        fragUpdated = true;
                    } else {
                        fragment.updateFileName(filename);
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
                txtFile.setText(filename);
                txtFile.setSelection(filename.length() - 1);
                fragment.setListener(listener);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContent, fragment).commit();
            }
        } else if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adjust) {
            PolygonAdjuster.adjust(getTtAppCtx().getDAL(), true);
        }
    }

    public void btnImport(View view) {
        if (fragment != null) {
            fragment.importFile(getTtAppCtx().getDAL());
        }
    }

    public void btnImportSelect(View view) {
        String[] extraMimes = {"file/*.csv", "file/*.gpx"};
        AndroidUtils.App.openFileIntent(this, "file/*.txt", extraMimes, Consts.Codes.Requests.OPEN_FILE);
    }
}
