package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;

import java.util.ArrayList;

import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

//todo add ability to move map around
public class AcquireGpsActivity extends AcquireGpsMapActivity {
    private TextView tvLogged, tvRecv;

    private GpsPoint _Point;
    private TtMetadata _Metadata;
    private ArrayList<TtNmeaBurst> _Bursts;

    private Button btnLog, btnCalc;

    private int loggedCount = 0, receivedCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acquire_gps);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        setResult(RESULT_CANCELED);

        if (!isCanceling()) {
            SheetLayoutEx.enterFromBottomAnimation(this);
            Intent intent = getIntent();
            if (intent != null && intent.getExtras() != null) {
                _Bursts = new ArrayList<>();

                try {
                    _Point = intent.getParcelableExtra(Consts.Codes.Data.POINT_DATA);
                    _Metadata = intent.getParcelableExtra(Consts.Codes.Data.METADATA_DATA);

                    setZone(_Metadata.getZone());

                    if (intent.getExtras().containsKey(Consts.Codes.Data.ADDITIVE_NMEA_DATA)) {
                        _Bursts = intent.getParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA);
                        setLoggedCount(_Bursts.size());
                    }

                } catch (Exception e) {
                    TtUtils.TtReport.writeError(e.getMessage(), "AcquireGpsActivity:onCreate", e.getStackTrace());
                    setResult(Consts.Codes.Results.ERROR);
                }
            } else {
                setResult(Consts.Codes.Results.NO_POINT_DATA);
                finish();
                return;
            }

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null){
                actionBar.setDisplayShowTitleEnabled(false);
            }

            tvLogged = (TextView)findViewById(R.id.acquireGpsToolbarLblLoggedValue);
            tvRecv = (TextView)findViewById(R.id.acquireGpsToolbarLblReceivedValue);

            btnLog = (Button)findViewById(R.id.aqrBtnLog);
            btnCalc = (Button)findViewById(R.id.aqrBtnCalc);

            if (_Bursts.size() > 0) {
                btnCalc.setEnabled(true);
            } else {
                btnCalc.setBackgroundColor(AndroidUtils.UI.getColor(this, R.color.primaryLighter));
            }

            //setupMap();
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
    protected void startLogging() {
        super.startLogging();
        btnLog.setText(R.string.aqr_log_pause);
    }

    protected void stopLogging() {
        super.stopLogging();
        btnLog.setText(R.string.aqr_log);
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

    public void btnCalcClick(View view) {

        if (isLogging()) {
            stopLogging();
        }

        try {
            Intent intent = new Intent(this, CalculateGpsActivity.class);

            intent.putExtra(Consts.Codes.Data.POINT_DATA, _Point);
            intent.putExtra(Consts.Codes.Data.METADATA_DATA, _Metadata);

            intent.putParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA, _Bursts);

            startActivityForResult(intent, Consts.Codes.Activites.CALCULATE);
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "AcquireGpsActivity:btnCalcClick", e.getStackTrace());
            new AlertDialog.Builder(AcquireGpsActivity.this)
                    .setMessage("Unable to start Calculation")
                    .show();
        }

    }

    @Override
    protected MapTracking getMapTracking() {
        return MapTracking.FOLLOW;
    }
}
