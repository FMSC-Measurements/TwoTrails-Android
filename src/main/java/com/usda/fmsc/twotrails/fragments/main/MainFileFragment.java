package com.usda.fmsc.twotrails.fragments.main;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.R;

import java.util.Date;

import com.usda.fmsc.utilities.StringEx;


public class MainFileFragment extends Fragment {
    private Button btnImport, btnDup, btnCleanDb;
    private TableLayout tblInfo;
    private TextView tvDate, tvPolys, tvPoints, tvGroups, tvMeta;
    private DataAccessLayer _dal;
    private View viewCleanDb;

    boolean enabled = false, viewExists = false;

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

        tvDate = view.findViewById(R.id.mainFragFileTvDate);
        tvPolys = view.findViewById(R.id.mainFragFileTvPolys);
        tvPoints = view.findViewById(R.id.mainFragFileTvPoints);
        tvGroups = view.findViewById(R.id.mainFragFileTvGroups);
        tvMeta = view.findViewById(R.id.mainFragFileTvMeta);

        btnImport = view.findViewById(R.id.mainFragFileBtnImport);
        btnDup = view.findViewById(R.id.mainFragFileBtnDup);
        btnCleanDb = view.findViewById(R.id.mainFragFileBtnCleanDb);
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

            if (TwoTrailsApp.getInstance().getDeviceSettings().isDeveloperOptionsEnabled()) {
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
