package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.usda.fmsc.twotrails.activities.custom.AcquireGpsCustomToolbarActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;

import java.util.ArrayList;
import java.util.List;

import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.utilities.StringEx;

public class AcquireGpsActivity extends AcquireGpsCustomToolbarActivity {
    private TextView tvLogged, tvRecv;

    private GpsPoint _Point;
    private TtMetadata _Metadata;
    private List<TtNmeaBurst> _Bursts;

    Button btnLog, btnCalc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acquire_gps);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        setResult(RESULT_CANCELED);

        tvLogged = (TextView)findViewById(R.id.acquireGpsToolbarLblLoggedValue);
        tvRecv = (TextView)findViewById(R.id.acquireGpsToolbarLblReceivedValue);

        if (!isCanceling()) {
            Intent intent = getIntent();
            if (intent != null && intent.getExtras() != null) {
                _Bursts = new ArrayList<>();

                try {
                    _Point = (GpsPoint)intent.getSerializableExtra(Consts.Activities.Data.POINT_DATA);
                    _Metadata = (TtMetadata)intent.getSerializableExtra(Consts.Activities.Data.METADATA_DATA);

                    setZone(_Metadata.getZone());

                    if (intent.getExtras().containsKey(Consts.Activities.Data.ADDITIVE_NMEA_DATA)) {
                        _Bursts = TtNmeaBurst.bytesToBursts(intent.getByteArrayExtra(Consts.Activities.Data.ADDITIVE_NMEA_DATA));
                        setLoggedCount(_Bursts.size());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                setResult(Consts.Activities.Results.NO_POINT_DATA);
                finish();
                return;
            }

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null){
                actionBar.setDisplayShowTitleEnabled(false);
            }

            btnLog = (Button)findViewById(R.id.aqrBtnLog);
            btnCalc = (Button)findViewById(R.id.aqrBtnCalc);

            if (_Bursts.size() > 0) {
                btnCalc.setEnabled(true);
            } else {
                btnCalc.setAlpha(Consts.DISABLED_ALPHA);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Consts.Activities.CALCULATE) {
            switch (resultCode) {
                case Consts.Activities.Results.POINT_CREATED: {
                    setResult(Consts.Activities.Results.POINT_CREATED, data);
                    finish();
                    break;
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isLogging()) {
            stopLogging();
        }
    }

    @Override
    protected void onLoggedNmeaBurst(NmeaBurst burst) {
        super.onLoggedNmeaBurst(burst);

        _Bursts.add(TtNmeaBurst.create(_Point.getCN(), false, burst));

        if (!btnCalc.isEnabled() && getLoggedCount() > 0) {
            btnCalc.setEnabled(true);
            btnCalc.setAlpha(Consts.ENABLED_ALPHA);
        }
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

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


    @Override
    protected void startLogging() {
        super.startLogging();
        btnLog.setText(R.string.aqr_log_pause);
    }

    @Override
    protected void stopLogging() {
        super.stopLogging();
        btnLog.setText(R.string.aqr_log);
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
            intent.putExtra(Consts.Activities.Data.POINT_DATA, _Point);
            intent.putExtra(Consts.Activities.Data.METADATA_DATA, _Metadata);

            intent.putExtra(Consts.Activities.Data.ADDITIVE_NMEA_DATA, TtNmeaBurst.burstsToByteArray(_Bursts));

            startActivityForResult(intent, Consts.Activities.CALCULATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
