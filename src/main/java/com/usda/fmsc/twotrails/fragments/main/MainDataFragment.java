package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.usda.fmsc.twotrails.R;


public class MainDataFragment extends Fragment {
    Button btnPoint, btnPoly, btnMeta, btnProj, btnTable;

    boolean enabled = false, viewExists = false;

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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_data, container, false);
        viewExists = true;

        btnPoint = (Button)view.findViewById(R.id.mainFragDataBtnPoints);
        btnPoly = (Button)view.findViewById(R.id.mainFragDataBtnPolygons);
        btnMeta = (Button)view.findViewById(R.id.mainFragDataBtnMetadata);
        btnProj = (Button)view.findViewById(R.id.mainFragDataBtnProjectInfo);
        btnTable = (Button)view.findViewById(R.id.mainFragDataBtnPointTable);

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
        }
    }
}
