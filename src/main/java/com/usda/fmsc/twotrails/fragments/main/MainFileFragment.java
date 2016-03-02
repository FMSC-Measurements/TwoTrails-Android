package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.R;

import java.util.Date;

import com.usda.fmsc.utilities.StringEx;


public class MainFileFragment extends Fragment {
    Button btnImport, btnDup, btnCleanDb;
    TableLayout tblInfo;
    TextView tvDate, tvPolys, tvPoints, tvGroups, tvMeta;
    DataAccessLayer _dal;
    View viewCleanDb;

    boolean enabled = false, viewExists = false;

    public static MainFileFragment newInstance() {
        return new MainFileFragment();
    }

    public MainFileFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_file, container, false);
        viewExists = true;

        tblInfo = (TableLayout)view.findViewById(R.id.mainFragFileTblInfo);

        tvDate = (TextView)view.findViewById(R.id.mainFragFileTvDate);
        tvPolys = (TextView)view.findViewById(R.id.mainFragFileTvPolys);
        tvPoints = (TextView)view.findViewById(R.id.mainFragFileTvPoints);
        tvGroups = (TextView)view.findViewById(R.id.mainFragFileTvGroups);
        tvMeta = (TextView)view.findViewById(R.id.mainFragFileTvMeta);

        btnImport = (Button)view.findViewById(R.id.mainFragFileBtnImport);
        btnDup = (Button)view.findViewById(R.id.mainFragFileBtnDup);
        btnCleanDb = (Button)view.findViewById(R.id.mainFragFileBtnCleanDb);
        viewCleanDb = view.findViewById(R.id.mainFragFileCleanDb);

        enableButtons(enabled);

        if(_dal != null) {
            updateInfo(_dal);
        }

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
            btnImport.setEnabled(enable);
            btnDup.setEnabled(enable);
            btnCleanDb.setEnabled(enable);

            tblInfo.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);

            if (Global.Settings.DeviceSettings.isDeveloperOptionsEnabled()) {
                viewCleanDb.setVisibility(View.VISIBLE);
            } else {
                viewCleanDb.setVisibility(View.GONE);
            }
        }
    }

    public void updateInfo(DataAccessLayer dal) {
        if(viewExists) {
            Date date = new Date(dal.getDBFile().lastModified());
            tvDate.setText(date.toString());
            tvPolys.setText(StringEx.toString(dal.getItemCount(TwoTrailsSchema.PolygonSchema.TableName)));
            tvPoints.setText(StringEx.toString(dal.getItemCount(TwoTrailsSchema.PointSchema.TableName)));
            tvGroups.setText(StringEx.toString(dal.getItemCount(TwoTrailsSchema.GroupSchema.TableName)));
            tvMeta.setText(StringEx.toString(dal.getItemCount(TwoTrailsSchema.MetadataSchema.TableName)));
        }

        _dal = dal;
    }

}
