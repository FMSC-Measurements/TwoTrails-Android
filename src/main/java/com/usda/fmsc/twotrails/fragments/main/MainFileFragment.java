package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.TtBaseFragment;
import com.usda.fmsc.utilities.StringEx;

import java.io.File;
import java.util.Date;


public class MainFileFragment extends TtBaseFragment {
    private Button btnImport, btnCleanDb;//, btnDup;
    private TableLayout tblInfo;
    private TextView tvDate, tvPolys, tvPoints, tvGroups, tvMeta;
    private DataAccessLayer _dal;
    private View viewCleanDb;

    private boolean enabled = false, viewExists = false;

    public boolean isViewCreated() {
        return viewExists;
    }

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
        View view = inflater.inflate(R.layout.fragment_main_file, container, false);
        viewExists = true;

        tblInfo = view.findViewById(R.id.mainFragFileTblInfo);

        //TODO add version info
        tvDate = view.findViewById(R.id.mainFragFileTvDate);
        tvPolys = view.findViewById(R.id.mainFragFileTvPolys);
        tvPoints = view.findViewById(R.id.mainFragFileTvPoints);
        tvGroups = view.findViewById(R.id.mainFragFileTvGroups);
        tvMeta = view.findViewById(R.id.mainFragFileTvMeta);

        btnImport = view.findViewById(R.id.mainFragFileBtnImport);
        //btnDup = view.findViewById(R.id.mainFragFileBtnDup);
        btnCleanDb = view.findViewById(R.id.mainFragFileBtnCleanDb);
        viewCleanDb = view.findViewById(R.id.mainFragFileCleanDb);

        enableButtons(enabled);

        if(_dal != null) {
            updateInfo();
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

        if (isViewCreated()) {
            btnImport.setEnabled(enable);
            //btnDup.setEnabled(enable);
            btnCleanDb.setEnabled(enable);

            tblInfo.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);

            if (getTtAppCtx().getDeviceSettings().isDeveloperOptionsEnabled()) {
                viewCleanDb.setVisibility(View.VISIBLE);
            } else {
                viewCleanDb.setVisibility(View.GONE);
            }
        }
    }

    public void updateInfo() {
        DataAccessManager dam = getTtAppCtx().getDAM();
        DataAccessLayer dal = dam.getDAL();

        if (isViewCreated()) {
            File dbFile = dam.getDBFile();

            tvDate.setText(new Date(dbFile.lastModified()).toString());
            tvPolys.setText(StringEx.toString(dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName)));
            tvPoints.setText(StringEx.toString(dal.getItemsCount(TwoTrailsSchema.PointSchema.TableName)));
            tvGroups.setText(StringEx.toString(dal.getItemsCount(TwoTrailsSchema.GroupSchema.TableName)));
            tvMeta.setText(StringEx.toString(dal.getItemsCount(TwoTrailsSchema.MetadataSchema.TableName)));
        }

        _dal = dal;
    }

}
