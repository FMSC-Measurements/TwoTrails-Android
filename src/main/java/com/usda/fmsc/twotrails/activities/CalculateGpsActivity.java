package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.Units.DopType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.Units;
import com.usda.fmsc.utilities.EnumEx;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class CalculateGpsActivity extends CustomToolbarActivity {
    private static final Pattern pattern = Pattern.compile("[a-zA-Z]");

    private GpsPoint _Point;
    private TtMetadata _Metadata;
    private List<TtNmeaBurst> _Bursts, _FilteredBursts;
    private boolean calculated;

    private int _Zone, rangeStart = -1, rangeEnd = Integer.MAX_VALUE;

    private FilterOptions options = new FilterOptions();

    Button btnCalc, btnCreate;

    TextView tvUtmX1, tvUtmX2,tvUtmX3,tvUtmXF, tvUtmY1, tvUtmY2,tvUtmY3,tvUtmYF,
            tvNssda1, tvNssda2, tvNssda3, tvNssdaF;
    
    CheckBox chkG1, chkG2, chkG3;

    Spinner spDop, spFix;
    EditText txtRange, txtGroup, txtDop;

    String nVal = "-";
    Integer manualGroupSize = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_gps);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            _Bursts = new ArrayList<>();

            try {
                _Point = (GpsPoint)intent.getSerializableExtra(Consts.Activities.Data.POINT_DATA);
                _Metadata = (TtMetadata)intent.getSerializableExtra(Consts.Activities.Data.METADATA_DATA);
                _Zone = _Metadata.getZone();

                _Bursts = TtNmeaBurst.bytesToBursts(intent.getByteArrayExtra(Consts.Activities.Data.ADDITIVE_NMEA_DATA));
                _FilteredBursts = new ArrayList<>();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            setResult(Consts.Activities.Results.NO_POINT_DATA);
            finish();
            return;
        }

        options.DopType = Global.Settings.DeviceSettings.getGpsFilterDopType();
        options.DopValue = Global.Settings.DeviceSettings.getGpsFilterDopValue();
        options.Fix = Global.Settings.DeviceSettings.getGpsFilterFixType();

        //region Control Assign
        btnCalc = (Button)findViewById(R.id.calcBtnCalc);
        btnCreate = (Button)findViewById(R.id.calcBtnCreate);

        chkG1 = (CheckBox)findViewById(R.id.calcChkGroup1);
        chkG2 = (CheckBox)findViewById(R.id.calcChkGroup2);
        chkG3 = (CheckBox)findViewById(R.id.calcChkGroup3);

        tvUtmX1 = (TextView)findViewById(R.id.calcTvUtmXG1);
        tvUtmX2 = (TextView)findViewById(R.id.calcTvUtmXG2);
        tvUtmX3 = (TextView)findViewById(R.id.calcTvUtmXG3);
        tvUtmXF = (TextView)findViewById(R.id.calcTvUtmXF);

        tvUtmY1 = (TextView)findViewById(R.id.calcTvUtmYG1);
        tvUtmY2 = (TextView)findViewById(R.id.calcTvUtmYG2);
        tvUtmY3 = (TextView)findViewById(R.id.calcTvUtmYG3);
        tvUtmYF = (TextView)findViewById(R.id.calcTvUtmYF);

        tvNssda1 = (TextView)findViewById(R.id.calcTvNssdaG1);
        tvNssda2 = (TextView)findViewById(R.id.calcTvNssdaG2);
        tvNssda3 = (TextView)findViewById(R.id.calcTvNssdaG3);
        tvNssdaF = (TextView)findViewById(R.id.calcTvNssdaF);

        spDop = (Spinner)findViewById(R.id.calcSpinnerDopType);
        spFix = (Spinner)findViewById(R.id.calcSpinnerFix);

        txtDop = (EditText)findViewById(R.id.calcTxtDopValue);
        txtGroup = (EditText)findViewById(R.id.calcTxtGroup);
        txtRange = (EditText)findViewById(R.id.calcTxtRange);
        //endregion

        //region Control Init
        ArrayAdapter<CharSequence> dopAdapter = ArrayAdapter.createFromResource(this, R.array.arr_dops, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> fixAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, EnumEx.getNames(GGASentence.GpsFixType.class));

        dopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spDop.setAdapter(dopAdapter);
        spDop.setSelection(options.DopType.getValue());

        spFix.setAdapter(fixAdapter);
        spFix.setSelection(options.Fix.getValue());

        spDop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                options.DopType = DopType.parse(i);
                Global.Settings.DeviceSettings.setGpsFilterDopType(options.DopType);
                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spFix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                options.Fix = GGASentence.GpsFixType.parse(i);
                Global.Settings.DeviceSettings.setGpsFilterFixType(options.Fix);
                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        txtDop.setText(StringEx.toString(options.DopValue));
        txtDop.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                Integer value = ParseEx.parseInteger(text);

                if (value != null) {
                    options.DopValue = value;
                    Global.Settings.DeviceSettings.setGpsFilterDopValue(value);
                    txtDop.setTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.abc_primary_text_material_light));
                    calculate();
                } else {
                    txtDop.setTextColor(AndroidUtils.UI.getColor(getBaseContext(), android.R.color.holo_red_dark));
                }
            }
        });

        txtGroup.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                manualGroupSize = ParseEx.parseInteger(editable.toString());
                calculate();
            }
        });

        txtRange.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                boolean valid = false;

                rangeStart = -1;
                rangeEnd = Integer.MAX_VALUE;

                Matcher m = pattern.matcher(text);
                if (!m.find()) {
                    String[] tokens = text.split("-");

                    if (tokens.length > 0) {
                        Integer value = ParseEx.parseInteger(tokens[0]);

                        if (value != null) {
                            rangeStart = value;
                            valid = true;

                            if (tokens.length > 1) {
                                value = ParseEx.parseInteger(tokens[1]);

                                if (value != null && value > rangeStart) {
                                    rangeEnd = value;
                                } else {
                                    valid = false;
                                    rangeStart = -1;
                                }
                            }

                            rangeStart -= 2;
                        }
                    }
                }

                if (valid) {
                    txtDop.setTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.abc_primary_text_material_light));
                    calculate();
                } else {
                    txtDop.setTextColor(AndroidUtils.UI.getColor(getBaseContext(), android.R.color.holo_red_dark));
                }

            }
        });
        //endregion
    }

    @Override
    protected void onResume() {
        super.onResume();

        calculate();
    }

    private void setCalculated(boolean calculated) {
        this.calculated = calculated;

        btnCreate.setEnabled(calculated);
        btnCreate.setAlpha(calculated ? Consts.ENABLED_ALPHA : Consts.DISABLED_ALPHA);
    }

    private void calculate() {
        int groupSize;

        _FilteredBursts.clear();

        TtNmeaBurst tmpBurst;
        for (int i = 0; i < _Bursts.size(); i++) {
            tmpBurst = _Bursts.get(i);
            tmpBurst.setUsed(false);

            if (i > rangeStart && i < rangeEnd && TtUtils.isUsableNmeaBurst(tmpBurst, options)) {
                _FilteredBursts.add(tmpBurst);
            }
        }

        if (manualGroupSize == null) {
            double d = _FilteredBursts.size() / 3.0;
            int i = (int)d;

            if ((d-i) > 0.666) {
                groupSize = i + 1;
            } else {
                groupSize = i;
            }

            if (groupSize < 5) {
                groupSize = 5;
            }
        } else {
            groupSize = manualGroupSize;
        }


        ArrayList<TtNmeaBurst> usedBursts = new ArrayList<>();

        double x = 0, y = 0, xF = 0, yF = 0, zF = 0;
        double dRMSEx = 0, dRMSEy = 0, dRMSEr, dRMSExf = 0, dRMSEyf = 0, dRMSErf;

        int count = 0, countF;

        if (_FilteredBursts.size() > 0) {
            //region Group 1
            for (int i = 0; i < _FilteredBursts.size() && i < groupSize; i++) {
                tmpBurst = _FilteredBursts.get(i);
                x += tmpBurst.getX(_Zone);
                y += tmpBurst.getY(_Zone);
                count++;

                if (chkG1.isChecked()) {
                    xF += tmpBurst.getX(_Zone);
                    yF += tmpBurst.getY(_Zone);
                    zF += tmpBurst.getElevation();
                    usedBursts.add(tmpBurst);
                }
            }

            x /= count;
            y /= count;

            for (int i = 0; i < _FilteredBursts.size() && i < groupSize; i++) {
                tmpBurst = _FilteredBursts.get(i);
                dRMSEx += Math.pow(tmpBurst.getX(_Zone) - x, 2);
                dRMSEy += Math.pow(tmpBurst.getY(_Zone) - y, 2);
            }

            dRMSEx = Math.sqrt(dRMSEx / count);
            dRMSEy = Math.sqrt(dRMSEy / count);
            dRMSEr = Math.sqrt(Math.pow(dRMSEx, 2) + Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff;


            tvUtmX1.setText(String.format("%.2f", x));
            tvUtmY1.setText(String.format("%.2f", y));
            tvNssda1.setText(String.format("%.2f", dRMSEr));
            chkG1.setEnabled(true);
            chkG1.setText(String.format("(%d)", count));
            //endregion

            //region Group 2
            if (_FilteredBursts.size() > groupSize) {
                x = y = count = 0;
                dRMSEx = dRMSEy = 0;

                for (int i = groupSize; i < _FilteredBursts.size() && i < groupSize * 2; i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    x += tmpBurst.getX(_Zone);
                    y += tmpBurst.getY(_Zone);
                    count++;

                    if (chkG2.isChecked()) {
                        xF += tmpBurst.getX(_Zone);
                        yF += tmpBurst.getY(_Zone);
                        zF += tmpBurst.getElevation();
                        usedBursts.add(tmpBurst);
                    }
                }

                x /= count;
                y /= count;

                for (int i = groupSize; i < _FilteredBursts.size() && i < groupSize * 2; i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    dRMSEx += Math.pow(tmpBurst.getX(_Zone) - x, 2);
                    dRMSEy += Math.pow(tmpBurst.getY(_Zone) - y, 2);
                }

                dRMSEx = Math.sqrt(dRMSEx / count);
                dRMSEy = Math.sqrt(dRMSEy / count);
                dRMSEr = Math.sqrt(Math.pow(dRMSEx, 2) + Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff;

                tvUtmX2.setText(String.format("%.2f", x));
                tvUtmY2.setText(String.format("%.2f", y));
                tvNssda2.setText(String.format("%.2f", dRMSEr));


                if (!chkG2.isEnabled()) {
                    chkG2.setChecked(true);
                }

                chkG2.setEnabled(true);
                chkG2.setText(String.format("(%d)", count));
            } else {
                tvUtmX2.setText(nVal);
                tvUtmY2.setText(nVal);
                tvNssda2.setText(nVal);
                chkG2.setChecked(false);
                chkG2.setEnabled(false);
                chkG2.setText("(0)");
            }
            //endregion

            //region Group 3
            if (_FilteredBursts.size() > groupSize * 2) {
                x = y = count = 0;
                dRMSEx = dRMSEy = 0;

                for (int i = groupSize * 2; i < _FilteredBursts.size(); i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    x += tmpBurst.getX(_Zone);
                    y += tmpBurst.getY(_Zone);
                    count++;

                    if (chkG3.isChecked()) {
                        xF += tmpBurst.getX(_Zone);
                        yF += tmpBurst.getY(_Zone);
                        zF += tmpBurst.getElevation();
                        usedBursts.add(tmpBurst);
                    }
                }

                x /= count;
                y /= count;

                for (int i = groupSize * 2; i < _FilteredBursts.size() && i < groupSize * 3; i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    dRMSEx += Math.pow(tmpBurst.getX(_Zone) - x, 2);
                    dRMSEy += Math.pow(tmpBurst.getY(_Zone) - y, 2);
                }

                dRMSEx = Math.sqrt(dRMSEx / count);
                dRMSEy = Math.sqrt(dRMSEy / count);
                dRMSEr = Math.sqrt(Math.pow(dRMSEx, 2) + Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff;

                tvUtmX3.setText(String.format("%.2f", x));
                tvUtmY3.setText(String.format("%.2f", y));
                tvNssda3.setText(String.format("%.2f", dRMSEr));

                if (!chkG3.isEnabled()) {
                    chkG3.setChecked(true);
                }

                chkG3.setEnabled(true);
                chkG3.setText(String.format("(%d)", count));
            } else {
                tvUtmX3.setText(nVal);
                tvUtmY3.setText(nVal);
                tvNssda3.setText(nVal);
                chkG3.setChecked(false);
                chkG3.setEnabled(false);
                chkG3.setText("(0)");
            }
            //endregion

            //region Real Point
            countF = usedBursts.size();

            if (countF > 0) {
                xF /= countF;
                yF /= countF;
                zF /= countF;

                ArrayList<GeoPosition> positions = new ArrayList<>(countF);

                for (int i = 0; i < countF; i++) {
                    tmpBurst = usedBursts.get(i);
                    tmpBurst.setUsed(true);

                    dRMSExf += Math.pow(tmpBurst.getX(_Zone) - xF, 2);
                    dRMSEyf += Math.pow(tmpBurst.getY(_Zone) - yF, 2);

                    positions.add(tmpBurst.getPosition());
                }

                dRMSExf = Math.sqrt(dRMSExf / countF);
                dRMSEyf = Math.sqrt(dRMSEyf / countF);
                dRMSErf = Math.sqrt(Math.pow(dRMSExf, 2) + Math.pow(dRMSEyf, 2)) * Consts.RMSEr95_Coeff;

                GeoPosition position = GeoTools.getMidPioint(positions);

                _Point.setLatitude(position.getLatitudeSignedDecimal());
                _Point.setLongitude(position.getLongitudeSignedDecimal());
                _Point.setElevation(TtUtils.Convert.distance(zF, Units.UomElevation.Meters, _Metadata.getElevation()));
                _Point.setUnAdjX(xF);
                _Point.setUnAdjY(yF);
                _Point.setUnAdjZ(_Point.getElevation());
                _Point.setRMSEr(dRMSErf);

                tvUtmXF.setText(String.format("%.3f", xF));
                tvUtmYF.setText(String.format("%.3f", yF));
                tvNssdaF.setText(String.format("%.2f", dRMSErf));

                setCalculated(true);
            } else {
                tvUtmXF.setText(nVal);
                tvUtmYF.setText(nVal);
                tvNssdaF.setText(nVal);
                setCalculated(false);
            }
            //endregion
        } else {
            //region reset
            tvUtmX1.setText(nVal);
            tvUtmY1.setText(nVal);
            tvNssda1.setText(nVal);
            chkG1.setEnabled(false);
            chkG2.setText("(0)");

            tvUtmX2.setText(nVal);
            tvUtmY2.setText(nVal);
            tvNssda2.setText(nVal);
            chkG2.setEnabled(false);
            chkG1.setText("(0)");

            tvUtmX3.setText(nVal);
            tvUtmY3.setText(nVal);
            tvNssda3.setText(nVal);
            chkG3.setEnabled(false);
            chkG3.setText("(0)");

            tvUtmXF.setText(nVal);
            tvUtmYF.setText(nVal);
            tvNssdaF.setText(nVal);

            setCalculated(false);
            //endregion
        }
    }

    //region Controls
    public void btnCreateClick(View view) {
        if (calculated) {

            Global.DAL.insertNmeaBursts(_Bursts);

            setResult(Consts.Activities.Results.POINT_CREATED, new Intent().putExtra(Consts.Activities.Data.POINT_DATA, _Point));
            finish();
        }
    }

    public void btnCalculateClick(View view) {
        calculate();
    }

    public void btnCancelClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void chkGroupClick(View view) {
        calculate();
    }
    //endregion

}