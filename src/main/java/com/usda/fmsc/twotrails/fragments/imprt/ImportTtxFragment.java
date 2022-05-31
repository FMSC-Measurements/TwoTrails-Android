package com.usda.fmsc.twotrails.fragments.imprt;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SelectableHolder;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.adapters.TtxPolygonsAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.DataAccessManager;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.Import.TTXImportTask;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class ImportTtxFragment extends BaseImportFragment {
    private static final String FILE_PATH = "file_path";

    private final TaskRunner taskRunner = new TaskRunner();

    private RecyclerViewEx rvImport;
    private final MultiSelector selector = new MultiSelector(new MultiSelector.Listener() {
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

    private Uri _FilePath;

    private DataAccessLayer idal;

    private List<TtxPolygonsAdapter.TtPolygonHolder> polyParams;


    public static ImportTtxFragment newInstance(Uri filePath) {
        ImportTtxFragment fragment = new ImportTtxFragment();
        Bundle args = new Bundle();

        args.putString(FILE_PATH, filePath.toString());

        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FILE_PATH)) {
            _FilePath = Uri.parse(bundle.getString(FILE_PATH));

            updateFilePath(_FilePath);
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
            selector.clearSelections();

            TtxPolygonsAdapter adapter = new TtxPolygonsAdapter(getContext(), idal.getPolygons(), idal, selector);
            rvImport.setAdapter(adapter);
        }
    }


    @Override
    protected void runImportTask(TwoTrailsApp app) {
        task = new TTXImportTask();

        task.setListener(result -> onTaskComplete(result.getCode()));

        selector.getSelectedPositions();

        TTXImportTask.TTXImportParams params = new TTXImportTask.TTXImportParams(
                app, _FilePath, getPolygons(), idal
        );

        onTaskStart();
        taskRunner.executeAsync(task, params);
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel();
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
        ArrayList<TtPolygon> polygons = new ArrayList<>();
        for (TtxPolygonsAdapter.TtPolygonHolder holder : polyParams) {
            polygons.add(holder.getPolygon());
        }

        return polygons;
    }

    private void scrollToPositionAndExpand(TtxPolygonsAdapter.TtPolygonHolder holder) {
        rvImport.smoothScrollToPosition(holder.getLayoutPosition());
    }

    public void updateFilePath(Uri filePath) {

        if (filePath != null && getActivity() != null) {

            _FilePath = filePath;

            try {
                DataAccessManager dam = DataAccessManager.importDAL(getTtAppCtx(), filePath);

                idal = dam.getDAL();

                setupPolygons();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
