package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;
import com.usda.fmsc.geospatial.nmea.codes.SentenceID;
import com.usda.fmsc.geospatial.nmea.codes.TalkerID;
import com.usda.fmsc.geospatial.nmea.sentences.NmeaSentence;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.adapters.NmeaDetailsAdapter;
import com.usda.fmsc.twotrails.gps.GpsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CheckNmeaDialogTt extends TtBaseDialogFragment implements GpsService.Listener {
    private GpsService.GpsBinder binder;
    private ListView lvNmea;

    private NmeaDetailsAdapter adapter;
    private final List<NmeaDetailsAdapter.GnssNmeaDetails> gnssNmeaDetails;
    private final HashMap<String, NmeaDetailsAdapter.GnssNmeaDetails> nmeaDetailsMap;


    public static CheckNmeaDialogTt newInstance() {
        return new CheckNmeaDialogTt();
    }

    public CheckNmeaDialogTt() {
        gnssNmeaDetails = new ArrayList<>();
        nmeaDetailsMap = new HashMap<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_check_nmea, null);

        lvNmea = view.findViewById(R.id.diagCheckNmeaList);
        adapter = new NmeaDetailsAdapter(getContext(), gnssNmeaDetails);
        lvNmea.setAdapter(adapter);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.str_ok, null)
                .create();
    }


    @Override
    public void onStart() {
        super.onStart();

        if (getTtAppCtx().isGpsServiceStarted()) {
            binder = getTtAppCtx().getGps();

            binder.addListener(this);
        } else {
            getTtAppCtx().startGpsService();
            Toast.makeText(getTtAppCtx(), "Gps Service Not Started. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (binder != null) {
            if (!(getTtAppCtx().getDeviceSettings().isGpsAlwaysOn() || binder.isLogging())) {
                binder.stopGps();
            }

            binder.removeListener(this);
        }
    }

    @Override
    public void nmeaBurstReceived(GnssNmeaBurst nmeaBurst) {

    }

    @Override
    public void nmeaStringReceived(final String nmeaString) {
        if (nmeaString.indexOf("$", 1) > -1) {
            String[] nmeaStrings = nmeaString.split("\\$");

            for (String ns : nmeaStrings) {
                if (!ns.isEmpty()) {
                    parseString("$" + ns);
                }
            }
        } else {
            parseString(nmeaString);
        }
    }

    private void parseString(String nmeaString) {
        final TalkerID tid = TalkerID.parse(nmeaString);
        final SentenceID sid = SentenceID.parse(nmeaString);

        if (lvNmea != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    String tidStr, sidStr;
                    if (tid == TalkerID.UNKNOWN) {
                        tidStr = String.format("(%s) Unknown", nmeaString.substring(0, nmeaString.indexOf(',')));
                    } else {
                        tidStr = String.format("(%s) %s", tid.toStringCode(), tid.toString());
                    }

                    if (sid == SentenceID.Unknown) {
                        sidStr = nmeaString.substring(2, 5);
                    } else {
                        sidStr = sid.toString();
                    }

                    if (nmeaDetailsMap.containsKey(tidStr)) {
                        if (nmeaDetailsMap.get(tidStr).addSentenceId(sidStr)) {
                            lvNmea.deferNotifyDataSetChanged();
                        }
                    } else {
                        NmeaDetailsAdapter.GnssNmeaDetails details = new NmeaDetailsAdapter.GnssNmeaDetails(tidStr);
                        details.addSentenceId(sidStr);
                        gnssNmeaDetails.add(details);
                        nmeaDetailsMap.put(tidStr, details);
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsAreValid) {

    }

    @Override
    public void receivingNmeaStrings(boolean receiving) {

    }

    @Override
    public void gpsStarted() {

    }

    @Override
    public void gpsStopped() {

    }

    @Override
    public void gpsServiceStarted() {

    }

    @Override
    public void gpsServiceStopped() {

    }

    @Override
    public void gpsError(GpsService.GpsError error) {

    }
}
