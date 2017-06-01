package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcquireAndCalculateGpsActivity extends AcquireGpsMapActivity {
    public static final String CALCULATE_ONLY_MODE = "CalculateOnlyMode";

    private static final Pattern pattern = Pattern.compile("[a-zA-Z]");

    private Button btnCreate;

    private TextView tvLogged, tvRecv, tvUtmX1, tvUtmX2,tvUtmX3,tvUtmXF, tvUtmY1, tvUtmY2, tvUtmY3, tvUtmYF,
            tvNssda1, tvNssda2, tvNssda3, tvNssdaF;

    private CheckBox chkG1, chkG2, chkG3;

    private Spinner spFix;

    private EditText txtDop;

    private GpsPoint _Point;
    private TtMetadata _Metadata;
    private ArrayList<TtNmeaBurst> _Bursts, _FilteredBursts;

    private Button btnLog, btnCalc;

    private int loggedCount = 0, receivedCount = 0;

    private boolean calculated, calcOnlyMode;

    private int rangeStart = -1, rangeEnd = Integer.MAX_VALUE;

    private FilterOptions options = new FilterOptions();

    private Integer manualGroupSize = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            try {
                if (intent.getExtras().containsKey(CALCULATE_ONLY_MODE)) {
                    calcOnlyMode = intent.getBooleanExtra(CALCULATE_ONLY_MODE, false);

                    if (calcOnlyMode) {
                        disableTrailMode();
                    }
                }

                super.onCreate(savedInstanceState);

                setResult(RESULT_CANCELED);

                if (isCanceling()) {
                    finish();
                    return;
                }

                setContentView(R.layout.activity_acquire_and_calculate_gps);

                SheetLayoutEx.enterFromBottomAnimation(this);

                setUseExitWarning(true);
                setUseLostConnectionWarning(true);

                _Point = intent.getParcelableExtra(Consts.Codes.Data.POINT_DATA);
                _Metadata = intent.getParcelableExtra(Consts.Codes.Data.METADATA_DATA);

                setZone(_Metadata.getZone());

                if (intent.getExtras().containsKey(Consts.Codes.Data.ADDITIVE_NMEA_DATA)) {
                    _Bursts = intent.getParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA);
                    setLoggedCount(_Bursts.size());
                } else {
                    _Bursts = new ArrayList<>();
                }

                _FilteredBursts = new ArrayList<>();
            } catch (Exception e) {
                TtUtils.TtReport.writeError(e.getMessage(), "AcquireAndCalculateGpsActivity:onCreate", e.getStackTrace());
                setResult(Consts.Codes.Results.ERROR);
            }
        } else {
            setResult(Consts.Codes.Results.NO_POINT_DATA);
            finish();
            return;
        }

        //region Control Assign
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayShowTitleEnabled(false);
        }

        setMapDrawerLockMode(calcOnlyMode ?
                    DrawerLayout.LOCK_MODE_LOCKED_OPEN : DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END);

        Toolbar tbCalc = (Toolbar)findViewById(R.id.toolbarCalc);
        if (tbCalc != null)
            tbCalc.setTitle("Average NMEA");

        tvLogged = (TextView)findViewById(R.id.acquireGpsToolbarLblLoggedValue);
        tvRecv = (TextView)findViewById(R.id.acquireGpsToolbarLblReceivedValue);

        btnLog = (Button)findViewById(R.id.aqrBtnLog);
        btnCalc = (Button)findViewById(R.id.aqrBtnCalc);

        if (_Bursts.size() > 0) {
            btnCalc.setEnabled(true);
        } else {
            btnCalc.setBackgroundColor(AndroidUtils.UI.getColor(this, R.color.primaryLighter));
        }

        options.Fix = Global.Settings.DeviceSettings.getGpsFilterFix();
        options.DopType = Global.Settings.DeviceSettings.getGpsFilterDopType();
        options.DopValue = Global.Settings.DeviceSettings.getGpsFilterDopValue();
        options.FixType = Global.Settings.DeviceSettings.getGpsFilterFixType();

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

        Spinner spDop = (Spinner)findViewById(R.id.calcSpinnerDopType);
        spFix = (Spinner)findViewById(R.id.calcSpinnerFix);

        txtDop = (EditText)findViewById(R.id.calcTxtDopValue);
        EditText txtGroup = (EditText)findViewById(R.id.calcTxtGroup);
        EditText txtRange = (EditText)findViewById(R.id.calcTxtRange);
        //endregion

        if (txtRange != null) {
            if (_Bursts.size() > 0) {
                txtRange.setText(String.format("1-%d", _Bursts.size()));
            }

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
        }

        //region Control Init
        ArrayAdapter<CharSequence> dopAdapter = ArrayAdapter.createFromResource(this, R.array.arr_dops, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> fixAdapter =  ArrayAdapter.createFromResource(this, R.array.arr_fix_types, android.R.layout.simple_spinner_item);

        dopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spDop != null && spFix != null) {
            spDop.setAdapter(dopAdapter);
            spDop.setSelection(options.DopType.getValue());

            spFix.setAdapter(fixAdapter);
            setSpinnerFixOption(options);

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
                    setSettingsFixOptions(i);
                    calculate();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        txtDop.setText(StringEx.toString(options.DopValue));
        txtDop.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                Integer value = ParseEx.parseInteger(text);

                if (value != null) {
                    options.DopValue = value;
                    Global.Settings.DeviceSettings.setGpsFilterDopValue(value);
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
        //endregion
    }

    @Override
    protected int getMapRightDrawerLayoutId() {
        return R.layout.content_drawer_calc;
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
            int zone = getZone();

            String nVal = "*";
            if (_FilteredBursts.size() > 0) {
                //region Group 1
                for (int i = 0; i < _FilteredBursts.size() && i < groupSize; i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    x += tmpBurst.getX(zone);
                    y += tmpBurst.getY(zone);
                    count++;

                    if (chkG1.isChecked()) {
                        xF += tmpBurst.getX(zone);
                        yF += tmpBurst.getY(zone);
                        zF += tmpBurst.getElevation();
                        usedBursts.add(tmpBurst);
                    }
                }

                x /= count;
                y /= count;

                for (int i = 0; i < _FilteredBursts.size() && i < groupSize; i++) {
                    tmpBurst = _FilteredBursts.get(i);
                    dRMSEx += Math.pow(tmpBurst.getX(zone) - x, 2);
                    dRMSEy += Math.pow(tmpBurst.getY(zone) - y, 2);
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
                        x += tmpBurst.getX(zone);
                        y += tmpBurst.getY(zone);
                        count++;

                        if (chkG2.isChecked()) {
                            xF += tmpBurst.getX(zone);
                            yF += tmpBurst.getY(zone);
                            zF += tmpBurst.getElevation();
                            usedBursts.add(tmpBurst);
                        }
                    }

                    x /= count;
                    y /= count;

                    for (int i = groupSize; i < _FilteredBursts.size() && i < groupSize * 2; i++) {
                        tmpBurst = _FilteredBursts.get(i);
                        dRMSEx += Math.pow(tmpBurst.getX(zone) - x, 2);
                        dRMSEy += Math.pow(tmpBurst.getY(zone) - y, 2);
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
                        x += tmpBurst.getX(zone);
                        y += tmpBurst.getY(zone);
                        count++;

                        if (chkG3.isChecked()) {
                            xF += tmpBurst.getX(zone);
                            yF += tmpBurst.getY(zone);
                            zF += tmpBurst.getElevation();
                            usedBursts.add(tmpBurst);
                        }
                    }

                    x /= count;
                    y /= count;

                    for (int i = groupSize * 2; i < _FilteredBursts.size() && i < groupSize * 3; i++) {
                        tmpBurst = _FilteredBursts.get(i);
                        dRMSEx += Math.pow(tmpBurst.getX(zone) - x, 2);
                        dRMSEy += Math.pow(tmpBurst.getY(zone) - y, 2);
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

                        dRMSExf += Math.pow(tmpBurst.getX(zone) - xF, 2);
                        dRMSEyf += Math.pow(tmpBurst.getY(zone) - yF, 2);

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
            TtUtils.TtReport.writeError(e.getMessage(), "CalculateGpsActivity:calculate", e.getStackTrace());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Consts.Codes.Activites.CALCULATE) {
            switch (resultCode) {
                case Consts.Codes.Results.POINT_CREATED: {
                    setResult(Consts.Codes.Results.POINT_CREATED, data);
                    finish();
                    break;
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (!calcOnlyMode && isMapDrawerOpen(GravityCompat.END)) {
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isLogging()) {
            stopLogging();
        }
    }

    @Override
    public void onMapReady() {
        super.onMapReady();
        setMapGesturesEnabled(true);
    }

    @Override
    protected void startLogging() {
        super.startLogging();
        btnLog.setText(R.string.aqr_log_pause);

        setMapGesturesEnabled(false);
    }

    protected void stopLogging() {
        super.stopLogging();
        btnLog.setText(R.string.aqr_log);

        setMapGesturesEnabled(true);
    }

    protected void setLoggedCount(int count) {
        loggedCount = count;
    }

    protected int getLoggedCount() {
        return loggedCount;
    }

    protected int getReceivedCount() {
        return receivedCount;
    }


    protected void onLoggedNmeaBurst(INmeaBurst burst) {
        _Bursts.add(TtNmeaBurst.create(_Point.getCN(), false, burst));

        if (!btnCalc.isEnabled() && getLoggedCount() > 0) {
            btnCalc.setEnabled(true);
            btnCalc.setBackgroundColor(AndroidUtils.UI.getColor(this, R.color.primary));
        }
    }

    @Override
    public void onNmeaBurstReceived(final INmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (isLogging() && nmeaBurst.hasPosition()) {
            onLoggedNmeaBurst(nmeaBurst);
            loggedCount++;
        }

        receivedCount++;

        tvRecv.setText(StringEx.toString(getReceivedCount()));
        tvLogged.setText(StringEx.toString(getLoggedCount()));
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        switch (error) {
            case LostDeviceConnection:
                stopLogging();
                break;
            case NoExternalGpsSocket:
                break;
            case Unkown:
                break;
        }

        super.gpsError(error);
    }


    private void setSpinnerFixOption(FilterOptions opts) {
        int val = opts.Fix.getValue();
        if (val > 1) {
            val = opts.FixType.getValue();

            if (val == 1) {
                val = 2;
            } else if (val > 1 && val < 6) {
                if (val == 4)
                    val = 5;
                else if (val == 5)
                    val = 4;
                val++;
            } else {
                val = 0;
            }
        }

        spFix.setSelection(val);
    }

    private void setSettingsFixOptions(int spinnerIndex) {
        if (spinnerIndex < 3) {
            options.Fix = GSASentence.Fix.parse(spinnerIndex);

            options.FixType = spinnerIndex == 2 ? GGASentence.GpsFixType.GPS : GGASentence.GpsFixType.NoFix;
        } else {
            options.Fix = GSASentence.Fix._3D;

            switch (spinnerIndex) {
                case 3: options.FixType = GGASentence.GpsFixType.DGPS; break;
                case 4: options.FixType = GGASentence.GpsFixType.PPS; break;
                case 5: options.FixType = GGASentence.GpsFixType.FloatRTK; break;
                case 6: options.FixType = GGASentence.GpsFixType.RTK; break;
                default: options.FixType = GGASentence.GpsFixType.NoFix; break;
            }
        }

        Global.Settings.DeviceSettings.setGpsFilterFix(options.Fix);
        Global.Settings.DeviceSettings.setGpsFilterFixType(options.FixType);
    }

    //region Controls
    public void btnCancelClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void btnLogClick(View view) {
        if (isLogging()) {
            stopLogging();
        } else {
            startLogging();
        }
    }

    public void btnStartCalcClick(View view) {
        if (isLogging()) {
            stopLogging();
        }

        setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.END);
        setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);

        calculate();
    }

    public void btnCreateClick(View view) {
        if (calculated) {

            Global.getDAL().insertNmeaBursts(_Bursts);

            setResult(Consts.Codes.Results.POINT_CREATED, new Intent().putExtra(Consts.Codes.Data.POINT_DATA, _Point));
            finish();
        }
    }

    public void btnCalculateClick(View view) {
        calculate();
    }

    public void chkGroupClick(View view) {
        calculate();
    }
    //endregion


    @Override
    public boolean shouldStartGps() {
        return !calcOnlyMode && super.shouldStartGps();
    }

    @Override
    protected MapTracking getMapTracking() {
        return isLogging() ? MapTracking.FOLLOW : MapTracking.NONE;
    }
}
