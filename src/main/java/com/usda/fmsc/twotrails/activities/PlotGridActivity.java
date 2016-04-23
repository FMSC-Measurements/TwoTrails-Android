package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.MetadataDetailsSpinnerAdapter;
import com.usda.fmsc.twotrails.adapters.PointDetailsSpinnerAdapter;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PlotGenerator;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.utilities.ParseEx;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class PlotGridActivity extends CustomToolbarActivity {
    private Spinner spnPolys, spnPoints, spnPointLoc, spnSampleType, spnMeta;
    private EditText txtGridX, txtGridY, txtTilt, txtSubSample;
    private CheckBox chkSubSample;
    private MaterialProgressBar progressBar;
    private View layControls;

    private List<TtPolygon> polygons, allPolys;
    private List<String> polyNames;
    private HashMap<String, TtMetadata> metadata;
    private HashMap<String, List<TtPoint>> polyPoints;

    private TtPolygon selectedPoly;
    private TtPoint startPoint;
    private TtMetadata selectedMeta;

    private PlotGenerator generator;
    boolean generating = false, adjust;


    PlotGenerator.PlotGenListener plotGenListener = new PlotGenerator.PlotGenListener() {
        @Override
        public void onCaneled() {
            //hide progress
            generating = false;
            hideProgress();
        }

        @Override
        public void onGenerated(TtPolygon polygon) {
            //hide progress
            generating = false;
            adjust = true;
            polygons.add(polygon);
            allPolys.add(polygon);

            Toast.makeText(getBaseContext(), String.format("%d Plots created",
                            Global.DAL.getPointCountInPolygon(polygon.getCN())),
                    Toast.LENGTH_SHORT).show();
            hideProgress();
        }

        @Override
        public void onError(Exception ex) {
            //hide progress
            generating = false;
            Toast.makeText(getBaseContext(), "There was an error generating the plots", Toast.LENGTH_SHORT).show();

            hideProgress();
        }

        private void hideProgress() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layControls.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot_grid);

        polygons = new ArrayList<>();
        allPolys = new ArrayList<>();
        polyNames = new ArrayList<>();
        polyPoints = new HashMap<>();

        List<TtPoint> tmpPoints;

        for (TtPolygon poly : Global.DAL.getPolygons()) {
            if (Global.DAL.getPointCountInPolygon(poly.getCN()) > 2) {

                tmpPoints = Global.DAL.getPointsInPolygon(poly.getCN());
                if (TtUtils.isValidPolygon(tmpPoints)) {
                    polyPoints.put(poly.getCN(), tmpPoints);
                    polygons.add(poly);
                    polyNames.add(poly.getName());
                }

            }

            allPolys.add(poly);
        }

        if (polygons.size() > 0) {
            spnPolys = (Spinner)findViewById(R.id.plotSpnPoly);
            spnPoints = (Spinner)findViewById(R.id.plotSpnPoint);
            spnPointLoc = (Spinner)findViewById(R.id.plotSpnLoc);
            spnSampleType = (Spinner)findViewById(R.id.plotSpnSubsample);
            spnMeta = (Spinner)findViewById(R.id.plotSpnMeta);

            txtGridX = (EditText)findViewById(R.id.plotTxtGridX);
            txtGridY = (EditText)findViewById(R.id.plotTxtGridY);
            txtTilt = (EditText)findViewById(R.id.plotTxtTilt);
            txtSubSample = (EditText)findViewById(R.id.plotTxtSubsample);
            chkSubSample = (CheckBox)findViewById(R.id.plotChkSubSample);

            progressBar = (MaterialProgressBar)findViewById(R.id.plotProgress);
            layControls = findViewById(R.id.plotLayOverlay);


            final ArrayAdapter<String> pointLocAdapter = new ArrayAdapter<>(this, R.layout.simple_large_spinner_item,
                    getResources().getStringArray(R.array.arr_plot_point_loc));

            pointLocAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnPointLoc.setAdapter(pointLocAdapter);


            final ArrayAdapter<String> sampleTypeAdapter = new ArrayAdapter<>(this, R.layout.simple_large_spinner_item,
                    getResources().getStringArray(R.array.arr_plot_sample_type));

            sampleTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnSampleType.setAdapter(sampleTypeAdapter);


            chkSubSample.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    spnSampleType.setEnabled(isChecked);
                    txtSubSample.setEnabled(isChecked);
                }
            });

            spnSampleType.setEnabled(false);
            txtSubSample.setEnabled(false);

            final ArrayAdapter<String> polyNameArrayAdapter = new ArrayAdapter<>(
                    this, R.layout.simple_large_spinner_item, polyNames
            );

            polyNameArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnPolys.setAdapter(polyNameArrayAdapter);

            spnPolys.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    polySelected(polygons.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            spnPoints.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (selectedPoly != null) {
                        pointSelected(polyPoints.get(selectedPoly.getCN()).get(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            metadata = Global.DAL.getMetadataMap();

            final List<TtMetadata> metalist = new ArrayList<>();

            for (TtMetadata meta : metadata.values()) {
                metalist.add(meta);
            }

            MetadataDetailsSpinnerAdapter metaAdapter = new MetadataDetailsSpinnerAdapter(this, metalist,R.layout.simple_large_spinner_item);
            spnMeta.setAdapter(metaAdapter);

            spnMeta.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    metaSelected(metalist.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            Toast.makeText(this, "No valid Polygons found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (generating) {
            final Activity activity = this;

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setMessage("Plots are currently being generated. Do you want to cancel the plots?");
            dialog.setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    generator.cancel(true);
                    activity.onBackPressed();
                }
            })
            .setNegativeButton(R.string.str_no, null);

            generator.cancel(true);
            return;
        }

        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adjust) {
            PolygonAdjuster.adjust(Global.DAL, Global.getMainActivity(), true);
        }
    }



    private void polySelected(TtPolygon poly) {
        selectedPoly = poly;

        spnPoints.setAdapter(new PointDetailsSpinnerAdapter(polyPoints.get(selectedPoly.getCN()), this,
                AppUnits.IconColor.Primary, R.layout.simple_large_spinner_item));
    }

    private void pointSelected(TtPoint point) {
        startPoint = point;
    }

    private void metaSelected(TtMetadata meta) {
        selectedMeta = meta;
    }



    private void generatePoints(final String polyName) {
        generating = true;

        for (final TtPolygon poly : allPolys) {
            if (poly.getName().equals(polyName)) {
                if (Global.Settings.DeviceSettings.getAutoOverwritePlotGridAsk()) {
                    DontAskAgainDialog dialog = new DontAskAgainDialog(
                            this,
                            Global.Settings.DeviceSettings.AUTO_OVERWRITE_PLOTGRID_ASK,
                            Global.Settings.DeviceSettings.AUTO_OVERWRITE_PLOTGRID,
                            Global.Settings.PreferenceHelper.getPrefs());

                    dialog.setMessage(String.format("The polygon name '%s' already exists. Would you like to rename or overwrite it?", polyName));

                    dialog.setPositiveButton("Overwrite", new DontAskAgainDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i, Object value) {
                                overwritePoly(poly);
                        }
                    }, 2);

                    dialog.setNeutralButton("Rename", new DontAskAgainDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i, Object value) {
                                renamePlot(polyName);
                        }
                    }, 1);

                    dialog.setNegativeButton("Cancel", null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            generating = false;
                        }
                    });

                    dialog.show();
                } else {
                    if (Global.Settings.DeviceSettings.getAutoOverwritePlotGrid() == 2) {
                        overwritePoly(poly);
                    } else {
                        renamePlot(polyName);
                    }
                }
                return;
            }
        }

        int gridX = ParseEx.parseInteger(txtGridX.getText().toString()),
            gridY = ParseEx.parseInteger(txtGridY.getText().toString());

        Integer angle = txtTilt.getText().length() > 0 ? ParseEx.parseInteger(txtTilt.getText().toString()) : null;
        boolean inside = spnPointLoc.getSelectedItemPosition() < 1;
        boolean sample = chkSubSample.isChecked();

        PlotGenerator.PlotParams params = new PlotGenerator.PlotParams(
                polyName,
                startPoint,
                polyPoints.get(selectedPoly.getCN()),
                gridX,
                gridY,
                angle,
                selectedMeta,
                inside,
                sample
        );

        if (sample) {
            params.SamplePercent = spnSampleType.getSelectedItemPosition() > 0;
            params.SampleValue = ParseEx.parseInteger(txtSubSample.getText().toString());
        }

        generator = new PlotGenerator(Global.DAL, metadata, plotGenListener);
        generator.execute(params);
        layControls.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }


    private void renamePlot(final String oldName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputDialog dialog = new InputDialog(getBaseContext());

                dialog.setMessage(String.format("Rename PlotGrid %s.", oldName));

                dialog.setPositiveButton(R.string.str_rename, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        generatePoints(((InputDialog) dialog).getText());
                    }
                });

                dialog.setNeutralButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        generating = false;
                    }
                });
            }
        }).start();

    }

    private void overwritePoly(final TtPolygon polygon) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layControls.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                allPolys.remove(polygon);
                polygons.remove(polygon);
                polyPoints.remove(polygon.getCN());

                try {
                    Global.DAL.deletePolygon(polygon.getCN());
                    Global.DAL.deletePointsInPolygon(polygon.getCN());
                    generatePoints(polygon.getName());
                } catch (Exception ex) {
                    TtUtils.TtReport.writeError(ex.getMessage(), "PlotGridActivity:overwritePoly");
                    Toast.makeText(getBaseContext(), "Overwrite polygon failed.", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }


    private boolean areSettingsValid() {
        int value;

        if (txtGridX.length() < 1) {
            txtGridX.requestFocus();
            Toast.makeText(this, "Grid requires a value", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (txtGridY.length() < 1) {
            txtGridY.requestFocus();
            Toast.makeText(this, "Grid requires a value", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ParseEx.parseInteger(txtGridX.getText().toString()) < 1) {
            txtGridX.requestFocus();
            Toast.makeText(this, "Grid X requires a value greater than 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ParseEx.parseInteger(txtGridY.getText().toString()) < 1) {
            txtGridY.requestFocus();
            Toast.makeText(this, "Grid Y requires a value greater than 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (txtTilt.length() > 0) {
            value = ParseEx.parseInteger(txtTilt.getText().toString());

            if (value < -45 || value > 45) {
                Toast.makeText(this, "Tilt must be between -45 and 45 degrees", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (chkSubSample.isChecked()) {
            if (txtSubSample.length() < 1) {
                txtSubSample.requestFocus();
                Toast.makeText(this, "Sample requires a value", Toast.LENGTH_SHORT).show();
                return false;
            }

            value = ParseEx.parseInteger(txtSubSample.getText().toString());
            boolean percent = spnSampleType.getSelectedItemPosition() > 0;

            if (value < 1) {
                txtSubSample.requestFocus();

                if (percent && value > 100) {
                    Toast.makeText(this, "Sample requires a percentage between 1 and 100", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sample requires a value greater than 0", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }

        return true;
    }


    public void btnPlotCreateClick(View view) {
        if (!generating && areSettingsValid()) {
            String polyName = String.format("%s_PltSample", selectedPoly.getName());
            generatePoints(polyName);
        }
    }
}
