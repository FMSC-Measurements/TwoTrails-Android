package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;


public class MainToolsFragment extends Fragment {
    private Button btnMap, btnGEarth, btnHAID, btnExport,
            btnPlotGrid, btnGpsLogger, btnGpsStatus;
    private View viewTest;

    private boolean enabled = false, viewExists = false;

    public static MainToolsFragment newInstance() {
        return new MainToolsFragment();
    }

    public MainToolsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_tools, container, false);
        viewExists = true;

        btnMap = (Button)view.findViewById(R.id.mainFragToolsBtnMap);
        btnGEarth = (Button)view.findViewById(R.id.mainFragToolsBtnGoogleEarth);
        btnHAID = (Button)view.findViewById(R.id.mainFragToolsBtnHAID);
        btnExport = (Button)view.findViewById(R.id.mainFragToolsBtnExport);
        btnPlotGrid = (Button)view.findViewById(R.id.mainFragToolsBtnPlotGrid);
        btnGpsLogger = (Button)view.findViewById(R.id.mainFragToolsBtnGpsLogger);
        btnGpsStatus = (Button)view.findViewById(R.id.mainFragToolsBtnGpsStatus);
        viewTest = view.findViewById(R.id.mainFragToolsTest);

        enableButtons(enabled);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        viewExists = false;
    }

    public void enableButtons(boolean enable) {
        enabled = enable;

        if (viewExists) {
            btnMap.setEnabled(enable);
            btnGEarth.setEnabled(enable);
            btnHAID.setEnabled(enable);
            btnExport.setEnabled(enable);
            btnPlotGrid.setEnabled(enable);
            btnGpsLogger.setEnabled(enable);
            btnGpsStatus.setEnabled(enable);

            if (Global.Settings.DeviceSettings.isDeveloperOptionsEnabled()) {
                viewTest.setVisibility(View.VISIBLE);
            } else {
                viewTest.setVisibility(View.GONE);
            }
        }
    }
}
