package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.geospatial.nmea41.NmeaIDs;
import com.usda.fmsc.geospatial.nmea41.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.adapters.NmeaDetailsAdapter;
import com.usda.fmsc.twotrails.gps.GpsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CheckNmeaDialog extends DialogFragment implements GpsService.Listener {
    private boolean postAllStrings = false;

    private GpsService.GpsBinder binder;
    private ListView lvNmea;

    private NmeaDetailsAdapter adapter;
    private List<NmeaDetailsAdapter.NmeaDetails> nmeaDetails;
    private HashMap<String, NmeaDetailsAdapter.NmeaDetails> nmeaDetailsMap;


    public static CheckNmeaDialog newInstance() {
        return new CheckNmeaDialog();
    }

    public CheckNmeaDialog() {
        nmeaDetails = new ArrayList<>();
        nmeaDetailsMap = new HashMap<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_check_nmea, null);

        lvNmea = view.findViewById(R.id.diagCheckNmeaList);
        adapter = new NmeaDetailsAdapter(getContext(), nmeaDetails);
        lvNmea.setAdapter(adapter);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.str_ok, null)
                .create();
    }


    @Override
    public void onStart() {
        super.onStart();

        binder = TwoTrailsApp.getInstance().getGps();

        binder.addListener(this);
        binder.startGps();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (binder != null) {
            if (!TwoTrailsApp.getInstance().getDeviceSettings().isGpsAlwaysOn()) {
                binder.stopGps();
            }

            binder.removeListener(this);
        }
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {

    }

    @Override
    public void nmeaStringReceived(final String nmeaString) {
        final NmeaIDs.TalkerID tid = NmeaIDs.TalkerID.parse(nmeaString);
        final NmeaIDs.SentenceID sid = NmeaIDs.SentenceID.parse(nmeaString);

        if (lvNmea != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    String tidStr, sidStr;
                    if (tid == NmeaIDs.TalkerID.Unknown) {
                        tidStr = String.format("(%s) Unknown", nmeaString.substring(0, nmeaString.indexOf(',')));
                    } else {
                        tidStr = String.format("(%s) %s", tid.toStringCode(), tid.toString());
                    }

                    if (sid == NmeaIDs.SentenceID.Unknown) {
                        sidStr = nmeaString.substring(2, 5);
                    } else {
                        sidStr = sid.toString();
                    }

                    if (nmeaDetailsMap.containsKey(tidStr)) {
                        if (nmeaDetailsMap.get(tidStr).addId(sidStr)) {
                            lvNmea.deferNotifyDataSetChanged();
                        }
                    } else {
                        NmeaDetailsAdapter.NmeaDetails details = new NmeaDetailsAdapter.NmeaDetails(tidStr);
                        details.addId(sidStr);
                        nmeaDetails.add(details);
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
