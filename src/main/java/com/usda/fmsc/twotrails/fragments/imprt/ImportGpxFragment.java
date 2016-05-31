package com.usda.fmsc.twotrails.fragments.imprt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SelectableHolder;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.utilities.gpx.GpxBaseTrack;
import com.usda.fmsc.utilities.gpx.GpxDocument;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.adapters.GpxTracksAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.Import;
import com.usda.fmsc.twotrails.utilities.Import.GPXImportTask;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class ImportGpxFragment extends BaseImportFragment {
    private static final String FILENAME = "filename";

    private RecyclerViewEx rvImport;
    private MultiSelector selector;

    Import.GPXImportTask task;

    private String _FileName;

    private GpxDocument gpxDocument;

    List<GpxTracksAdapter.GpxBaseTrackHolder> polyParams;
    List<GpxBaseTrack> tracks;


    MultiSelector.Listener mSelectionListener = new MultiSelector.Listener() {
        @Override
        public void onItemSelectionChange(SelectableHolder holder, boolean isSelected) {
            if (holder instanceof GpxTracksAdapter.GpxBaseTrackHolder) {
                GpxTracksAdapter.GpxBaseTrackHolder gh = (GpxTracksAdapter.GpxBaseTrackHolder)holder;


                if (isSelected && !polyParams.contains(gh)) {
                    polyParams.add(gh);
                } else {
                    polyParams.remove(gh);
                }
            }
        }

        @Override
        public void onClearSelections() {
            polyParams.clear();
        }
    };



    public static ImportGpxFragment newInstance(String fileName) {
        ImportGpxFragment fragment = new ImportGpxFragment();
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

        selector = new MultiSelector(mSelectionListener);
        polyParams = new ArrayList<>();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_gpx, container, false);

        rvImport = (RecyclerViewEx)view.findViewById(R.id.importFragRv);
        rvImport.setLayoutManager(new LinearLayoutManager(getContext()));
        rvImport.setHasFixedSize(true);
        rvImport.setItemAnimator(new SlideInUpAnimator());
        rvImport.setViewHasFooter(true);

        setupTracks();

        return view;
    }


    private void setupTracks() {
        if (rvImport != null && gpxDocument != null) {
            tracks = new ArrayList<>();
            tracks.addAll(gpxDocument.getTracks());
            tracks.addAll(gpxDocument.getRoutes());

            selector.clearSelections();

            GpxTracksAdapter adapter = new GpxTracksAdapter(getContext(), tracks, selector);
            rvImport.setAdapter(adapter);
        }
    }


    @Override
    protected void runImportTask(DataAccessLayer dal) {
        task = new GPXImportTask();

        task.setListener(new Import.ImportTaskListener() {
            @Override
            public void onTaskFinish(Import.ImportResult result) {
                onTaskComplete(result.getCode());
            }
        });

        selector.getSelectedPositions();

        GPXImportTask.GPXImportParams params = new GPXImportTask.GPXImportParams(
                _FileName, dal, getParams()
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
    public boolean validate() {
        if (selector.getSelectionCount() < 1) {
            Toast.makeText(getContext(), "No Polygons selected", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private List<GPXImportTask.GPXPolyParams> getParams() {
        ArrayList<GPXImportTask.GPXPolyParams> params = new ArrayList<>();

        for (GpxTracksAdapter.GpxBaseTrackHolder holder : polyParams) {
            params.add(new GPXImportTask.GPXPolyParams(
                    holder.getName(), null, null, null, null, tracks.get(holder.getAdapterPosition()), Global.getDefaultMeta()));
        }

        return params;
    }

    private void scrollToPositionAndExpand(GpxTracksAdapter.GpxBaseTrackHolder holder) {
        rvImport.smoothScrollToPosition(holder.getLayoutPosition());
    }

    @Override
    public void updateFileName(String filename) {
        if (!StringEx.isEmpty(_FileName)) {

            _FileName = filename;

            try {
                gpxDocument = GpxDocument.parseFile(_FileName);

                setupTracks();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
