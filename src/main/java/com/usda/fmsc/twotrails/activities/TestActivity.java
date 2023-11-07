package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.geospatial.ins.vectornav.VNInsData;
import com.usda.fmsc.geospatial.ins.vectornav.commands.VNCommand;
import com.usda.fmsc.geospatial.ins.vectornav.nmea.sentences.base.VNNmeaSentence;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.TtActivity;
import com.usda.fmsc.twotrails.ins.VNInsService;
import com.usda.fmsc.utilities.StringEx;

import java.io.IOException;

public class TestActivity extends TtActivity implements VNInsService.Listener {


    boolean validData = true;
    long tslm = System.currentTimeMillis();
    VNNmeaSentence lastNmeaSentence;
    VNInsData lastInsData;
    VNCommand lastCommand;
    
    
    TextView tvTimeSinceLastMsg,
    tvConsecutive, tvTimespan,
    tvDistX, tvDistY, tvDistZ,
    tvVelX, tvVelY, tvVelZ,
    tvRotX, tvRotY, tvRotZ,
    tvYaw, tvPitch, tvRoll;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);

        tvTimeSinceLastMsg = findViewById(R.id.tvTimeSinceLastMsg);
        tvConsecutive = findViewById(R.id.tvConsecutive);
        tvTimespan = findViewById(R.id.tvTimespan);
        
        tvDistX = findViewById(R.id.tvDistX);
        tvDistY = findViewById(R.id.tvDistY);
        tvDistZ = findViewById(R.id.tvDistZ);

        tvVelX = findViewById(R.id.tvVelX);
        tvVelY = findViewById(R.id.tvVelY);
        tvVelZ = findViewById(R.id.tvVelZ);

        tvRotX = findViewById(R.id.tvRotX);
        tvRotY = findViewById(R.id.tvRotY);
        tvRotZ = findViewById(R.id.tvRotZ);

        tvYaw = findViewById(R.id.tvYaw);
        tvPitch = findViewById(R.id.tvPitch);
        tvRoll = findViewById(R.id.tvRoll);
    }

    @Override
    public boolean requiresInsService() {
        return true;
    }

    public void btnTestClick(View view) {
        try {
            getTtAppCtx().getVnIns().tare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void insDataReceived(VNInsData data) {
        this.lastInsData = data;
        long now = System.currentTimeMillis();
        long lmt = now - tslm;
        tslm = now;


        runOnUiThread(() -> {
            tvTimeSinceLastMsg.setText(Long.toString(lmt));
            tvConsecutive.setText(data.isConsecutive() ? "True" : "False");
            tvTimespan.setText(StringEx.toString(data.getTimeSpan(), 2));

            tvDistX.setText(StringEx.toString(data.getDistanceX(), 6));
            tvDistY.setText(StringEx.toString(data.getDistanceY(), 6));
            tvDistZ.setText(StringEx.toString(data.getDistanceZ(), 6));

            tvVelX.setText(StringEx.toString(data.getVelocityX(), 6));
            tvVelY.setText(StringEx.toString(data.getVelocityY(), 6));
            tvVelZ.setText(StringEx.toString(data.getVelocityZ(), 6));

            tvRotX.setText(StringEx.toString(data.getRotationX(), 6));
            tvRotY.setText(StringEx.toString(data.getRotationY(), 6));
            tvRotZ.setText(StringEx.toString(data.getRotationZ(), 6));

            tvYaw.setText(StringEx.toString(data.getYaw(), 6));
            tvPitch.setText(StringEx.toString(data.getPitch(), 6));
            tvRoll.setText(StringEx.toString(data.getRoll(), 6));
        });
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(VNNmeaSentence nmeaSentence) {
        this.lastNmeaSentence = nmeaSentence;
    }

    @Override
    public void commandRespone(VNCommand command) {
        this.lastCommand = command;
        Toast.makeText(TestActivity.this, String.format("Command %s received", command.getMessageID().toStringCode()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void receivingData(boolean receiving) {
        if (receiving) {
            Toast.makeText(TestActivity.this, "Receiving Data", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(TestActivity.this, "Not receiving Data", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void receivingValidData(boolean valid) {
        this.validData = valid;
    }

    @Override
    public void insStarted() {

    }

    @Override
    public void insStopped() {

    }

    @Override
    public void insServiceStarted() {

    }

    @Override
    public void insServiceStopped() {

    }

    @Override
    public void insError(VNInsService.InsError error) {
        switch (error) {
            case LostDeviceConnection:
                break;
            case DeviceConnectionEnded:
                break;
            case FailedToConnect:
                break;
            case Unknown:
                break;
        }
    }
}
