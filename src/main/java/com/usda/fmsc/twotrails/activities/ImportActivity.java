package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.FABProgressCircleEx;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.TtProjectAdjusterActivity;
import com.usda.fmsc.twotrails.activities.contracts.GetContentMultiMimes;
import com.usda.fmsc.twotrails.fragments.imprt.BaseImportFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportGpxFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportKmlFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportTextFragment;
import com.usda.fmsc.twotrails.fragments.imprt.ImportTtxFragment;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.utilities.Import;
import com.usda.fmsc.utilities.MimeTypes;

import java.util.regex.Pattern;

public class ImportActivity extends TtProjectAdjusterActivity {
    private FloatingActionButton fabImport;
    private FABProgressCircleEx fabProgCircle;

    private BaseImportFragment fragment;

    private EditText txtFile;
    private final PostDelayHandler handler = new PostDelayHandler(1000);
    private final PostDelayHandler pdhShowFab = new PostDelayHandler(250);

    private boolean ignoreChange, adjust;


    private final BaseImportFragment.Listener listener = new BaseImportFragment.Listener() {
        String message = null;

        @Override
        public void onTaskComplete(Import.ImportResultCode code) {
            switch (code) {
                case Success:
                    fabProgCircle.beginFinalAnimation();
                    View view = findViewById(R.id.parent);
                    if (view != null) {
                        Snackbar snackbar = Snackbar.make(view, "File Imported", Snackbar.LENGTH_LONG)
                                .setAction("View Map", v -> getTtAppCtx().adjustProject(false))
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
            pdhShowFab.post(() -> runOnUiThread(() -> {
                if (ready)
                    fabImport.show();
                else
                    fabImport.hide();
            }));
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        fabImport = findViewById(R.id.importFabImport);
        fabProgCircle = findViewById(R.id.importFabImportProgressCircle);

        fabProgCircle.attachListener(() -> fabProgCircle.hide());

        txtFile = findViewById(R.id.importTxtFile);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtFile);


        txtFile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(final Editable s) {
                if (!ignoreChange) {
                    handler.post(() -> updateFilePath(Uri.parse(s.toString())));
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
    public boolean onCreateOptionsMenuEx(Menu menu) {
        return false;
    }


    @Override
    public void onAdjusterStopped(TwoTrailsApp.ProjectAdjusterResult result, AdjustingException.AdjustingError error) {
        super.onAdjusterStopped(result, error);

        if (result == TwoTrailsApp.ProjectAdjusterResult.SUCCESSFUL) {
            adjust = false;
            startActivity(new Intent(ImportActivity.this, MapActivity.class));
        }
    }

    private void updateFilePath(Uri filePath) {
        boolean fragUpdated = false;

        String[] parts = filePath.toString().split(Pattern.quote("."));
        String ext = parts[parts.length - 1].toLowerCase();

        if (parts.length > 1) {
            switch (ext) {
                case "txt":
                case "csv": {
                    if (fragment == null || !(fragment instanceof ImportTextFragment)) {
                        fragment = ImportTextFragment.newInstance(filePath);
                        fragUpdated = true;
                    } else {
                        fragment.updateFilePath(filePath);
                    }
                    break;
                }
                case "gpx": {
                    if (fragment == null || !(fragment instanceof ImportGpxFragment)) {
                        fragment = ImportGpxFragment.newInstance(filePath);
                        fragUpdated = true;
                    } else {
                        fragment.updateFilePath(filePath);
                    }
                    break;
                }
                case "kmz":
                case "kml": {

                    if (fragment == null || !(fragment instanceof ImportKmlFragment)) {
                        fragment = ImportKmlFragment.newInstance(filePath);
                        fragUpdated = true;
                    } else {
                        fragment.updateFilePath(filePath);
                    }
                    break;
                }
                case "ttx": {
                    if (fragment == null || !(fragment instanceof ImportTtxFragment)) {
                        fragment = ImportTtxFragment.newInstance(filePath);
                        fragUpdated = true;
                    } else {
                        fragment.updateFilePath(filePath);
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
                txtFile.setText(filePath.getPath());
                txtFile.setSelection(filePath.getPath().length() - 1);
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
            getTtAppCtx().adjustProject(true);
        }
    }

    public void btnImport(View view) {
        if (fragment != null) {
            fragment.importFile(getTtAppCtx());
        }
    }


    private final ActivityResultLauncher<String[]> onFileSelected = registerForActivityResult(new GetContentMultiMimes(), path -> {
        if (path != null) {
            updateFilePath(path);
        } else {
            Toast.makeText(ImportActivity.this, "Unable to find file.", Toast.LENGTH_LONG).show();
        }
    });


    //TODO fix import select
    public void btnImportSelect(View view) {
        //String[] extraMimes = {"text/*.txt", "file/*.csv", "file/*.gpx", "file/*.ttx"};
        String[] extraMimes = { MimeTypes.Text.PLAIN, MimeTypes.Text.CSV, MimeTypes.Application.GPS, Consts.FileMimes.TPK, MimeTypes.Application.GOOGLE_EARTH };
        onFileSelected.launch(extraMimes);
    }
}
