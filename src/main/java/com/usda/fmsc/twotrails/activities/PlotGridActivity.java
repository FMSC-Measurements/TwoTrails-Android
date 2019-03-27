package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PlotGenerator;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.usda.fmsc.utilities.EnumEx;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class PlotGridActivity extends CustomToolbarActivity {
    private Spinner spnPoints, spnPointLoc, spnSampleType;
    private EditText txtGridX, txtGridY, txtTilt, txtSubSample;
    private CheckBox chkSubSample;
    private MaterialProgressBar progressBar;
    private View layControls;

    private List<TtPolygon> polygons, allPolys;
    private HashMap<String, TtMetadata> metadata;
    private HashMap<String, List<TtPoint>> polyPoints;

    private TtPolygon selectedPoly;
    private TtPoint startPoint;
    private Dist selectedDist = Dist.FeetTenths;

    private PlotGenerator generator;
    boolean generating = false, adjust;

    private Random random = new Random(DateTime.now().getMillis());


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
                            TtAppCtx.getDAL().getPointCountInPolygon(polygon.getCN())),
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
        ArrayList<String> polyNames = new ArrayList<>();
        polyPoints = new HashMap<>();

        List<TtPoint> tmpPoints;

        for (TtPolygon poly : TtAppCtx.getDAL().getPolygons()) {
            if (TtAppCtx.getDAL().getPointCountInPolygon(poly.getCN()) > 2) {

                tmpPoints = TtAppCtx.getDAL().getPointsInPolygon(poly.getCN());
                if (TtUtils.Points.isValidPolygon(tmpPoints)) {
                    polyPoints.put(poly.getCN(), tmpPoints);
                    polygons.add(poly);
                    polyNames.add(poly.getName());
                }

            }

            allPolys.add(poly);
        }

        if (polygons.size() > 0) {
            Spinner spnPolys = findViewById(R.id.plotSpnPoly);
            Spinner spnDist = findViewById(R.id.plotSpnDistUom);
            spnPoints = findViewById(R.id.plotSpnPoint);
            spnPointLoc = findViewById(R.id.plotSpnLoc);
            spnSampleType = findViewById(R.id.plotSpnSubsample);

            txtGridX = findViewById(R.id.plotTxtGridX);
            txtGridY = findViewById(R.id.plotTxtGridY);
            txtTilt = findViewById(R.id.plotTxtTilt);
            txtSubSample = findViewById(R.id.plotTxtSubsample);
            chkSubSample = findViewById(R.id.plotChkSubSample);

            progressBar = findViewById(R.id.plotProgress);
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
                        if (position > 0) {
                            pointSelected(polyPoints.get(selectedPoly.getCN()).get(position - 1));
                        } else {
                            pointSelected(null);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            ArrayAdapter<CharSequence> distAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, EnumEx.getNames(Dist.class));

            metadata = TtAppCtx.getDAL().getMetadataMap();

            spnDist.setAdapter(distAdapter);

            spnDist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    distUomSelected(Dist.parse(position));
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
            PolygonAdjuster.adjust(TtAppCtx.getDAL(), true);
        }
    }



    private void polySelected(TtPolygon poly) {
        selectedPoly = poly;

        spnPoints.setAdapter(new PointDetailsSkip1SpinnerAdapter(polyPoints.get(selectedPoly.getCN()), this,
                AppUnits.IconColor.Primary, R.layout.simple_large_spinner_item));
    }

    private void pointSelected(TtPoint point) {
        startPoint = point;
    }

    private void distUomSelected(Dist dist) {
        selectedDist = dist;
    }



    private void generatePoints(final String polyName) {
        generating = true;

        for (final TtPolygon poly : allPolys) {
            if (poly.getName().equals(polyName)) {
                if (TtAppCtx.getDeviceSettings().getAutoOverwritePlotGridAsk()) {
                    DontAskAgainDialog dialog = new DontAskAgainDialog(
                            this,
                            DeviceSettings.AUTO_OVERWRITE_PLOTGRID_ASK,
                            DeviceSettings.AUTO_OVERWRITE_PLOTGRID,
                            TtAppCtx.getDeviceSettings().getPrefs());

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
                    if (TtAppCtx.getDeviceSettings().getAutoOverwritePlotGrid() == 2) {
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

        List<TtPoint> points = polyPoints.get(selectedPoly.getCN());

        PlotGenerator.PlotParams params = new PlotGenerator.PlotParams(
                polyName,
                startPoint != null ? startPoint : points.get(random.nextInt(points.size() - 1)),
                points,
                selectedDist,
                gridX,
                gridY,
                angle,
                metadata.get(Consts.EmptyGuid),
                inside,
                sample
        );

        if (sample) {
            params.setSamplePercent(spnSampleType.getSelectedItemPosition() > 0);
            params.setSampleValue(ParseEx.parseInteger(txtSubSample.getText().toString()));
        }

        generator = new PlotGenerator(TtAppCtx.getDAL(), metadata, plotGenListener);
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
                    TtAppCtx.getDAL().deletePolygon(polygon.getCN());
                    TtAppCtx.getDAL().deletePointsInPolygon(polygon.getCN());
                    generatePoints(polygon.getName());
                } catch (Exception ex) {
                    TtAppCtx.getReport().writeError(ex.getMessage(), "PlotGridActivity:overwritePoly");
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



    public class PointDetailsSkip1SpinnerAdapter extends BaseAdapter {
        private List<TtPoint> points;
        private LayoutInflater inflater;
        private Context context;
        private AppUnits.IconColor iconColor;
        private boolean showPolygon = false;
        private int itemView;

        public PointDetailsSkip1SpinnerAdapter(List<TtPoint> points, Context context, AppUnits.IconColor iconColor, int itemView) {
            this.points = points;
            this.context = context;
            this.itemView = itemView;
            inflater = LayoutInflater.from(this.context);
            this.iconColor = iconColor;
        }

        @Override
        public int getCount() {
            return points.size() + 1;
        }

        @Override
        public TtPoint getItem(int i) {
            return (i == 0) ? null : points.get(i - 1);
        }

        @Override
        public long getItemId(int i) {
            return i + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PointDetailsSkip1SpinnerAdapter.ViewHolder mViewHolder;

            if(convertView == null) {
                convertView = inflater.inflate(itemView, null);
                mViewHolder = new PointDetailsSkip1SpinnerAdapter.ViewHolder();
                convertView.setTag(mViewHolder);

                mViewHolder.text = convertView.findViewById(android.R.id.text1);
            } else {
                mViewHolder = (PointDetailsSkip1SpinnerAdapter.ViewHolder) convertView.getTag();
            }

            if (position > 0) {
                TtPoint point = getItem(position);

                mViewHolder.text.setText(String.format("%d", point.getPID()));
            } else {
                mViewHolder.text.setText("Random");
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            PointDetailsSkip1SpinnerAdapter.DropDownViewHolder mViewHolder;

            if(convertView == null) {
                convertView = inflater.inflate(R.layout.content_details_points_ops, parent, false);
                mViewHolder = new PointDetailsSkip1SpinnerAdapter.DropDownViewHolder();
                convertView.setTag(mViewHolder);

                mViewHolder.text = convertView.findViewById(R.id.text1);
                mViewHolder.image = convertView.findViewById(R.id.image);
            } else {
                mViewHolder = (PointDetailsSkip1SpinnerAdapter.DropDownViewHolder) convertView.getTag();
            }

            if (position > 0) {
                TtPoint point = getItem(position);

                mViewHolder.image.setImageDrawable(TtUtils.UI.getTtOpDrawable(point.getOp(), iconColor, context));
                mViewHolder.image.setVisibility(View.VISIBLE);
                mViewHolder.text.setText(StringEx.format("%d%s", point.getPID(),
                        showPolygon ? " - " + point.getPolyName() : StringEx.Empty));
            } else {
                mViewHolder.image.setVisibility(View.INVISIBLE);
                mViewHolder.text.setText("Random");
            }

            return convertView;
        }

        private class DropDownViewHolder {
            ImageView image;
            TextView text;
        }

        private class ViewHolder {
            TextView text;
        }
    }
}
