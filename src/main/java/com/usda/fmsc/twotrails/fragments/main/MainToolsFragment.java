package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;


public class MainToolsFragment extends Fragment {
    private Button btnMap, btnGEarth, btnHAID, btnExport,
            btnPlotGrid, btnGpsLogger, btnGpsStatus, btnSAT;
    private View viewTest;

    private boolean enabled = false, viewExists = false;

    public boolean isViewCreated() {
        return viewExists;
    }

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

        btnMap = view.findViewById(R.id.mainFragToolsBtnMap);
        btnGEarth = view.findViewById(R.id.mainFragToolsBtnGoogleEarth);
        btnHAID = view.findViewById(R.id.mainFragToolsBtnHAID);
        btnExport = view.findViewById(R.id.mainFragToolsBtnExport);
        btnPlotGrid = view.findViewById(R.id.mainFragToolsBtnPlotGrid);
        btnGpsLogger = view.findViewById(R.id.mainFragToolsBtnGpsLogger);
        btnGpsStatus = view.findViewById(R.id.mainFragToolsBtnGpsStatus);
        btnSAT = view.findViewById(R.id.mainFragToolsBtnSAT);
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
            btnSAT.setEnabled(enabled);
            //btnGpsLogger.setEnabled(enable);
            //btnGpsStatus.setEnabled(enable);

            if (TwoTrailsApp.getInstance(getContext()).getDeviceSettings().isDeveloperOptionsEnabled()) {
                viewTest.setVisibility(View.VISIBLE);
            } else {
                viewTest.setVisibility(View.GONE);
            }
        }
    }
}
