package com.usda.fmsc.twotrails.fragments.imprt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SelectableHolder;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.adapters.GpxTracksAdapter;
import com.usda.fmsc.twotrails.adapters.KmlPolygonsAdapter;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.utilities.Import;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.kml.Folder;
import com.usda.fmsc.utilities.kml.KmlDocument;
import com.usda.fmsc.utilities.kml.Placemark;
import com.usda.fmsc.utilities.kml.Polygon;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class ImportKmlFragment extends BaseImportFragment {
    private static final String FILENAME = "filename";

    private RecyclerViewEx rvImport;
    private MultiSelector selector = new MultiSelector(new MultiSelector.Listener() {
        @Override
        public void onItemSelectionChange(SelectableHolder holder, boolean isSelected) {
            if (holder instanceof KmlPolygonsAdapter.KmlPolygonHolder) {
                KmlPolygonsAdapter.KmlPolygonHolder kmlHolder = (KmlPolygonsAdapter.KmlPolygonHolder)holder;


                if (isSelected && !polyParams.contains(kmlHolder)) {
                    polyParams.add(kmlHolder);
                } else {
                    polyParams.remove(kmlHolder);
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

    private Import.KMLImportTask task;

    private String _FileName;

    private KmlDocument kmlDocument;

    private List<KmlPolygonsAdapter.KmlPolygonHolder> polyParams;
    private List<Polygon> polygons;



    public static ImportKmlFragment newInstance(String fileName) {
        ImportKmlFragment fragment = new ImportKmlFragment();
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

        rvImport = (RecyclerViewEx)view.findViewById(R.id.importFragRv);
        rvImport.setLayoutManager(new LinearLayoutManager(getContext()));
        rvImport.setHasFixedSize(true);
        rvImport.setItemAnimator(new SlideInUpAnimator());
        rvImport.setViewHasFooter(true);

        setupTracks();

        return view;
    }


    private void setupTracks() {
        if (rvImport != null && kmlDocument != null) {
            polygons = new ArrayList<>();

            parseFolder(kmlDocument);

            selector.clearSelections();

            KmlPolygonsAdapter adapter = new KmlPolygonsAdapter(getContext(), polygons, selector);
            rvImport.setAdapter(adapter);
        }
    }

    private void parseFolder(Folder folder) {
        for (Placemark placemark : folder.getPlacemarks()) {
            polygons.addAll(placemark.getPolygons());
        }

        for (Folder subFolder : folder.getSubFolders()) {
            parseFolder(subFolder);
        }
    }


    @Override
    protected void runImportTask(DataAccessLayer dal) {
        task = new Import.KMLImportTask();

        task.setListener(new Import.ImportTaskListener() {
            @Override
            public void onTaskFinish(Import.ImportResult result) {
                onTaskComplete(result.getCode());
            }
        });

        selector.getSelectedPositions();

        Import.KMLImportTask.KMLImportParams params = new Import.KMLImportTask.KMLImportParams(
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
    public boolean validate(boolean useMessage) {
        if (selector.getSelectionCount() < 1) {
            if (useMessage) {
                Toast.makeText(getContext(), "No Polygons selected", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        return true;
    }

    private List<Import.KMLImportTask.KMLPolyParams> getParams() {
        ArrayList<Import.KMLImportTask.KMLPolyParams> params = new ArrayList<>();

        for (KmlPolygonsAdapter.KmlPolygonHolder holder : polyParams) {
            params.add(new Import.KMLImportTask.KMLPolyParams(
                    holder.getName(), null, null, null, null, polygons.get(holder.getAdapterPosition()), Global.getDefaultMeta()));
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
                kmlDocument = KmlDocument.load(_FileName);

                setupTracks();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
