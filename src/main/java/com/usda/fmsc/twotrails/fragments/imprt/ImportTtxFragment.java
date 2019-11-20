package com.usda.fmsc.twotrails.fragments.imprt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SelectableHolder;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.adapters.TtxPolygonsAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.Import.TTXImportTask;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class ImportTtxFragment extends BaseImportFragment {
    private static final String FILENAME = "filename";

    private RecyclerViewEx rvImport;
    private MultiSelector selector = new MultiSelector(new MultiSelector.Listener() {
        @Override
        public void onItemSelectionChange(SelectableHolder holder, boolean isSelected) {
            if (holder instanceof TtxPolygonsAdapter.TtPolygonHolder) {
                TtxPolygonsAdapter.TtPolygonHolder ttxHolder = (TtxPolygonsAdapter.TtPolygonHolder)holder;


                if (isSelected && !polyParams.contains(ttxHolder)) {
                    polyParams.add(ttxHolder);
                } else {
                    polyParams.remove(ttxHolder);
                }
            }

            readyToImport(validate());
        }

        @Override
        public void onClearSelections() {
            polyParams.clear();
            readyToImport(validate());
        }
    });

    private TTXImportTask task;

    private String _FileName;

    private DataAccessLayer idal;

    private List<TtxPolygonsAdapter.TtPolygonHolder> polyParams;
    private List<TtPolygon> polygons;


    public static ImportTtxFragment newInstance(String fileName) {
        ImportTtxFragment fragment = new ImportTtxFragment();
        Bundle args = new Bundle();

        args.putString(FILENAME, fileName);

        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FILENAME)) {
            _FileName = bundle.getString(FILENAME);

            updateFileName(_FileName);
        }

        polyParams = new ArrayList<>();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_xml_type, container, false);

        rvImport = view.findViewById(R.id.importFragRv);
        rvImport.setLayoutManager(new LinearLayoutManager(getContext()));
        rvImport.setHasFixedSize(true);
        rvImport.setItemAnimator(new SlideInUpAnimator());
        rvImport.setViewHasFooter(true);

        setupPolygons();

        return view;
    }


    private void setupPolygons() {
        if (rvImport != null) {
            polygons = idal.getPolygons();

            selector.clearSelections();

            TtxPolygonsAdapter adapter = new TtxPolygonsAdapter(getContext(), polygons, idal, selector);
            rvImport.setAdapter(adapter);
        }
    }


    @Override
    protected void runImportTask(TwoTrailsApp app) {
        task = new TTXImportTask();

        task.setListener(result -> onTaskComplete(result.getCode()));

        selector.getSelectedPositions();

        TTXImportTask.TTXImportParams params = new TTXImportTask.TTXImportParams(
                app, _FileName, getPolygons(), idal
        );

        onTaskStart();
        task.execute(params);
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public boolean validate(boolean useMessage) {
        if (selector.getSelectionCount() < 1) {
            if (useMessage) {
                Toast.makeText(getContext(), "No Polygons selected", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    private List<TtPolygon> getPolygons() {
        return idal.getPolygons();
    }

    private void scrollToPositionAndExpand(TtxPolygonsAdapter.TtPolygonHolder holder) {
        rvImport.smoothScrollToPosition(holder.getLayoutPosition());
    }

    @Override
    public void updateFileName(String filename) {
        if (!StringEx.isEmpty(_FileName) && getActivity() != null) {

            _FileName = filename;

            try {
                idal = new DataAccessLayer(_FileName, TwoTrailsApp.getInstance(getActivity()));

                setupPolygons();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
