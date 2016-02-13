package com.usda.fmsc.twotrails.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaIDs;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;

import java.util.ArrayList;
import java.util.List;

public class CheckNmeaDialog extends DialogFragment implements GpsService.Listener {
    boolean postAllStrings;

    GpsService.GpsBinder binder;
    ListView lvNmea;

    List<NmeaIDs.TalkerID> talkerIDs = new ArrayList<>();
    List<String> unknTid = new ArrayList<>();
    ArrayAdapter<String> tidStrings;


    public static CheckNmeaDialog newInstance() {
        return new CheckNmeaDialog();
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_check_nmea, null);

        lvNmea = (ListView)view.findViewById(R.id.diagCheckNmeaList);
        tidStrings = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        lvNmea.setAdapter(tidStrings);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.str_ok, null)
                .create();
    }


    @Override
    public void onStart() {
        super.onStart();

        binder = Global.getGpsBinder();

        postAllStrings = binder.postsAllNmeaStrings();
        binder.postAllNmeaStrings(true);

        binder.registerActiviy(getActivity(), this);
        binder.startGps();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (binder != null) {
            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
            }

            binder.postAllNmeaStrings(postAllStrings);
            binder.unregisterActivity(getActivity());
        }
    }

    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {

    }

    @Override
    public void nmeaStringReceived(final String nmeaString) {
        final NmeaIDs.TalkerID tid = NmeaIDs.TalkerID.parse(nmeaString);

        if (lvNmea != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tid != NmeaIDs.TalkerID.Unknown && !talkerIDs.contains(tid)) {
                        talkerIDs.add(tid);
                        tidStrings.add(String.format("(%s) %s", tid.toStringCode(), tid.toString()));
                        lvNmea.deferNotifyDataSetChanged();
                    } else if (tid == NmeaIDs.TalkerID.Unknown){
                        String unkn = nmeaString.substring(0, nmeaString.indexOf(','));

                        if (!unknTid.contains(unkn)) {
                            unknTid.add(unkn);
                            tidStrings.add(String.format("(%s) Unknown", unkn));
                            lvNmea.deferNotifyDataSetChanged();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {

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
