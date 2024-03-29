package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.usda.fmsc.twotrails.R;


public class MainDataFragment extends Fragment {
    private Button btnPoint, btnPoly, btnMeta, btnProj, btnTable, btnSAT;

    private boolean enabled = false, viewExists = false;

    public boolean isViewCreated() {
        return viewExists;
    }

    public static MainDataFragment newInstance() {
        return new MainDataFragment();
    }

    public MainDataFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_data, container, false);
        viewExists = true;

        btnPoint = view.findViewById(R.id.mainFragDataBtnPoints);
        btnPoly = view.findViewById(R.id.mainFragDataBtnPolygons);
        btnMeta = view.findViewById(R.id.mainFragDataBtnMetadata);
        btnProj = view.findViewById(R.id.mainFragDataBtnProjectInfo);
        btnTable = view.findViewById(R.id.mainFragDataBtnPointTable);
        btnSAT = view.findViewById(R.id.mainFragDataBtnSAT);

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

        if(viewExists) {
            btnPoint.setEnabled(enable);
            btnPoly.setEnabled(enable);
            btnMeta.setEnabled(enable);
            btnProj.setEnabled(enable);
            btnTable.setEnabled(enable);
            btnSAT.setEnabled(enabled);
        }
    }
}
