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
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.utilities.EnumEx;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class CalculateGpsActivityOld extends CustomToolbarActivity {
    private static final Pattern pattern = Pattern.compile("[a-zA-Z]");

    private GpsPoint _Point;
    private TtMetadata _Metadata;
    private List<TtNmeaBurst> _Bursts, _FilteredBursts;
    private boolean calculated;

    private int _Zone, rangeStart = -1, rangeEnd = Integer.MAX_VALUE;

    private FilterOptions options = new FilterOptions();

    private Button btnCreate;

    private TextView tvUtmX1, tvUtmX2,tvUtmX3,tvUtmXF, tvUtmY1, tvUtmY2,tvUtmY3,tvUtmYF,
            tvNssda1, tvNssda2, tvNssda3, tvNssdaF;

    private CheckBox chkG1, chkG2, chkG3;

    private EditText txtDop;

    private Integer manualGroupSize = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_gps);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            _Bursts = new ArrayList<>();

            try {
                _Metadata = intent.getParcelableExtra(Consts.Codes.Data.METADATA_DATA);
                _Point = intent.getParcelableExtra(Consts.Codes.Data.POINT_DATA);

                _Zone = _Metadata.getZone();

                _Bursts = intent.getParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA);
                _FilteredBursts = new ArrayList<>();
            } catch (Exception e) {
                TtUtils.TtReport.writeError(e.getMessage(), "CalculateGpsActivityOld:onCreate", e.getStackTrace());
                setResult(Consts.Codes.Results.ERROR);
                finish();
                return;
            }
        } else {
            setResult(Consts.Codes.Results.NO_POINT_DATA);
            finish();
            return;
        }

        options.Fix = TtAppCtx.getDeviceSettings().getGpsFilterFix();
        options.DopType = TtAppCtx.getDeviceSettings().getGpsFilterDopType();
        options.DopValue = TtAppCtx.getDeviceSettings().getGpsFilterDopValue();
        options.FixType = TtAppCtx.getDeviceSettings().getGpsFilterFixType();

        //region Control Assign
        btnCreate = findViewById(R.id.calcBtnCreate);

        chkG1 = findViewById(R.id.calcChkGroup1);
        chkG2 = findViewById(R.id.calcChkGroup2);
        chkG3 = findViewById(R.id.calcChkGroup3);

        tvUtmX1 = findViewById(R.id.calcTvUtmXG1);
        tvUtmX2 = findViewById(R.id.calcTvUtmXG2);
        tvUtmX3 = findViewById(R.id.calcTvUtmXG3);
        tvUtmXF = findViewById(R.id.calcTvUtmXF);

        tvUtmY1 = findViewById(R.id.calcTvUtmYG1);
        tvUtmY2 = findViewById(R.id.calcTvUtmYG2);
        tvUtmY3 = findViewById(R.id.calcTvUtmYG3);
        tvUtmYF = findViewById(R.id.calcTvUtmYF);

        tvNssda1 = findViewById(R.id.calcTvNssdaG1);
        tvNssda2 = findViewById(R.id.calcTvNssdaG2);
        tvNssda3 = findViewById(R.id.calcTvNssdaG3);
        tvNssdaF = findViewById(R.id.calcTvNssdaF);

        Spinner spDop = findViewById(R.id.calcSpinnerDopType);
        Spinner spFix = findViewById(R.id.calcSpinnerFix);

        txtDop = findViewById(R.id.calcTxtDopValue);
        EditText txtGroup = findViewById(R.id.calcTxtGroup);
        EditText txtRange = findViewById(R.id.calcTxtRange);
        //endregion



        if (txtRange != null && _Bursts.size() > 0) {
            txtRange.setText(String.format("1-%d", _Bursts.size()));
        }

        //region Control Init
        ArrayAdapter<CharSequence> dopAdapter = ArrayAdapter.createFromResource(this, R.array.arr_dops, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> fixAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, EnumEx.getNames(GSASentence.Fix.class));

        dopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spDop != null && spFix != null) {
            spDop.setAdapter(dopAdapter);
            spDop.setSelection(options.DopType.getValue());

            spFix.setAdapter(fixAdapter);
            spFix.setSelection(options.Fix.getValue());
        }

        spDop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                options.DopType = DopType.parse(i);
                TtAppCtx.getDeviceSettings().setGpsFilterDopType(options.DopType);
                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spFix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                options.Fix = GSASentence.Fix.parse(i);
                TtAppCtx.getDeviceSettings().setGpsFilterFix(options.Fix);
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
                    TtAppCtx.getDeviceSettings().setGpsFilterDopValue(value);
                    txtDop.setTextColor(AndroidUtils.UI.getColor(getBaseContext(), R.color.black_1000));
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
        try {
            int groupSize;

            _FilteredBursts.clear();

            TtNmeaBurst tmpBurst;
            for (int i = 0; i < _Bursts.size(); i++) {
                tmpBurst = _Bursts.get(i);
                tmpBurst.setUsed(false);

                if (i > rangeStart && i < rangeEnd && TtUtils.NMEA.isBurstUsable(tmpBurst, options)) {
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

            String nVal = "*";
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


                tvUtmX1.setText(StringEx.toString(x, 2));
                tvUtmY1.setText(StringEx.toString(y, 2));
                tvNssda1.setText(StringEx.toString(dRMSEr, 2));
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

                    tvUtmX2.setText(StringEx.toString(x, 2));
                    tvUtmY2.setText(StringEx.toString(y, 2));
                    tvNssda2.setText(StringEx.toString(dRMSEr, 2));


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

                    tvUtmX3.setText(StringEx.toString(x, 2));
                    tvUtmY3.setText(StringEx.toString(y, 2));
                    tvNssda3.setText(StringEx.toString(dRMSEr, 2));

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
                    _Point.setElevation(TtUtils.Convert.distance(zF, UomElevation.Meters, _Metadata.getElevation()));
                    _Point.setUnAdjX(xF);
                    _Point.setUnAdjY(yF);
                    _Point.setUnAdjZ(_Point.getElevation());
                    _Point.setRMSEr(dRMSErf);

                    tvUtmXF.setText(StringEx.toString(xF, 3));
                    tvUtmYF.setText(StringEx.toString(yF, 3));
                    tvNssdaF.setText(StringEx.toString(dRMSErf, 2));

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
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "CalculateGpsActivityOld:calculate", e.getStackTrace());
        }
    }

    //region Controls
    public void btnCreateClick(View view) {
        if (calculated) {

            TtAppCtx.getDAL().insertNmeaBursts(_Bursts);

            setResult(Consts.Codes.Results.POINT_CREATED, new Intent().putExtra(Consts.Codes.Data.POINT_DATA, _Point));
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
