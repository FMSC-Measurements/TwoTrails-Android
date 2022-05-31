package com.usda.fmsc.twotrails.fragments.imprt;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SelectableHolder;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.adapters.GpxTracksAdapter;
import com.usda.fmsc.twotrails.adapters.KmlPolygonsAdapter;
import com.usda.fmsc.twotrails.utilities.Import;
import com.usda.fmsc.utilities.kml.Folder;
import com.usda.fmsc.utilities.kml.KmlDocument;
import com.usda.fmsc.utilities.kml.Placemark;
import com.usda.fmsc.utilities.kml.Polygon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class ImportKmlFragment extends BaseImportFragment {
    private static final String FILE_PATH = "file_path";

    private final TaskRunner taskRunner = new TaskRunner();

    private RecyclerViewEx rvImport;
    private final MultiSelector selector = new MultiSelector(new MultiSelector.Listener() {
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

    private Uri _FilePath;

    private KmlDocument kmlDocument;

    private List<KmlPolygonsAdapter.KmlPolygonHolder> polyParams;
    private List<Polygon> polygons;



    public static ImportKmlFragment newInstance(Uri filePath) {
        ImportKmlFragment fragment = new ImportKmlFragment();
        Bundle args = new Bundle();

        args.putString(FILE_PATH, filePath.getPath());

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
    protected void runImportTask(TwoTrailsApp app) {
        task = new Import.KMLImportTask();

        task.setListener(result -> onTaskComplete(result.getCode()));

        selector.getSelectedPositions();

        Import.KMLImportTask.KMLImportParams params = new Import.KMLImportTask.KMLImportParams(
                app, _FilePath, getParams()
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

    private List<Import.KMLImportTask.KMLPolyParams> getParams() {
        ArrayList<Import.KMLImportTask.KMLPolyParams> params = new ArrayList<>();

        for (KmlPolygonsAdapter.KmlPolygonHolder holder : polyParams) {
            params.add(new Import.KMLImportTask.KMLPolyParams(
                    holder.getName(), null, null, null, null, polygons.get(holder.getBindingAdapterPosition()),
                    getTtAppCtx().getMetadataSettings().getDefaultMetadata()));
        }

        return params;
    }

    private void scrollToPositionAndExpand(GpxTracksAdapter.GpxBaseTrackHolder holder) {
        rvImport.smoothScrollToPosition(holder.getLayoutPosition());
    }

    @Override
    public void updateFilePath(Uri filePath) {
        if (_FilePath != null) {

            _FilePath = filePath;

            try {
                kmlDocument = KmlDocument.load(new File(_FilePath.getPath()));

                setupTracks();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
