package com.usda.fmsc.twotrails.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.geospatial.ins.vectornav.VNInsData;
import com.usda.fmsc.geospatial.ins.vectornav.codes.MessageID;
import com.usda.fmsc.geospatial.ins.vectornav.commands.VNCommand;
import com.usda.fmsc.geospatial.ins.vectornav.nmea.sentences.base.VNNmeaSentence;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.activities.contracts.CreateDocumentWType;
import com.usda.fmsc.twotrails.activities.contracts.CreateZipDocument;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.ins.TtInsData;
import com.usda.fmsc.twotrails.ins.VNInsService;
import com.usda.fmsc.twotrails.logic.SettingsLogic;
import com.usda.fmsc.twotrails.utilities.Export;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.MimeTypes;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.Tuple;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

public class TestActivity extends TtCustomToolbarActivity implements VNInsService.Listener {


    boolean validData = true;
    long tslm = System.currentTimeMillis();
    VNNmeaSentence lastNmeaSentence;
    VNInsData lastInsData;
    VNCommand lastCommand;
    

    FloatingActionButton fab;
    TextView tvTimeSinceLastMsg,
    tvConsecutive, tvTimespan,
    tvDistX, tvDistY, tvDistZ,
    tvVelX, tvVelY, tvVelZ,
    tvRotX, tvRotY, tvRotZ,
    tvYaw, tvPitch, tvRoll;

    String PCN;
    boolean collecting = false;

    DataAccessLayer dal;
    PostDelayHandler tareHandler = new PostDelayHandler(5000);
    private final Runnable waitForTare = () -> {
        Toast.makeText(TestActivity.this, "Zero INS Timeout", Toast.LENGTH_SHORT).show();
        collecting = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fab.setImageResource(R.drawable.ic_ttpoint_gps_white);
            }
        });
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);

        dal = getTtAppCtx().getDAL();

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

        fab = findViewById(R.id.testFab);
    }

    @Override
    public boolean requiresInsService() {
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mmMenuTestExport) {
            exportInsLauncher.launch(new Tuple<>(String.format(Locale.getDefault(), "INS_Data_%s.csv",
                    TtUtils.Date.toStringDateMillis(new DateTime(getTtAppCtx().getDAM().getDBFile().lastModified()))), MimeTypes.Text.CSV));
        }

        return super.onOptionsItemSelected(item);
    }

    private final ActivityResultLauncher<Tuple<String, String>> exportInsLauncher = registerForActivityResult(new CreateDocumentWType(),
            uri -> {
                if (uri != null) {
                    File insFile = Export.ins(getTtAppCtx(), dal, null);
                    try {
                        AndroidUtils.Files.copyFile(getTtAppCtx(), Uri.fromFile(insFile), uri);
//                        insFile.delete();
                    } catch (IOException e) {
                        Toast.makeText(TestActivity.this, "Error copying file", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(TestActivity.this, "Error selecting file for export", Toast.LENGTH_LONG).show();
                }
            });

    public void btnTestClick(View view) {
        if (collecting) {
            collecting = false;
            fab.setImageResource(R.drawable.ic_ttpoint_gps_white);
        } else {
            PCN = UUID.randomUUID().toString();
            try {
                getTtAppCtx().getVnIns().tare();
                tareHandler.post(waitForTare);
            } catch (IOException e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "TestActivity:btnTestClick", e.getStackTrace());
            }
        }
    }


    @Override
    public void insDataReceived(VNInsData data) {
        this.lastInsData = data;
        long now = System.currentTimeMillis();
        long lmt = now - tslm;
        tslm = now;

        if (collecting && dal != null) {
            try {
                dal.insertInsData(TtInsData.create(PCN, data));
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "TestActivity:insDataReceived");
            }
        }

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
//        Toast.makeText(TestActivity.this, String.format("Command %s received", command.getMessageID().toStringCode()), Toast.LENGTH_SHORT).show();

        if (command.getMessageID() == MessageID.TAR) {
            tareHandler.cancel();

            if (!collecting) {
                collecting = true;
                Toast.makeText(TestActivity.this, "Started Collecting", Toast.LENGTH_SHORT).show();

                runOnUiThread(() -> fab.setImageResource(R.drawable.ic_polygon_white_36dp));
            } else {
                Toast.makeText(TestActivity.this, "Already Collecting", Toast.LENGTH_SHORT).show();
            }
        }
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
