package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.adapters.BaseTableAdapter;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.widget.SpreadsheetView;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.R;

import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class TableViewActivity extends CustomToolbarActivity {
    private static final DateTimeFormatter DateTimeFormatter = DateTimeFormat.forPattern("h:mm:ss a M/d/yyyy");

    private static String[] Headers = new String[] {
        "OpType",
        "Index",
        "Polygon",
        "Created",
        "OnBnd",
        "Metadata",
        "AdjX",
        "AdjY",
        "AdjZ",
        "UnAdjX",
        "UnAdjY",
        "UnAdjZ",
        "Accuracy",
        "Man Acc",
        "Latitude",
        "Longitude",
        "Elevation",
        "Fwd Az",
        "Bk Az",
        "Horiz Dist",
        "Slope Dist",
        "Slope Angle",
        "Parent",
        "Comment",
        "Linked Points"
    };


    private MaterialProgressBar progressBar;
    private SpreadsheetView ssvPoints;
    private View filterView;


    private ViewOptions viewOptions;

    private List<TtPolygon> _Polygons;
    private HashMap<String, TtMetadata> _Metadata;

    private HashMap<String, Boolean> _PolygonFilter;
    private HashMap<OpType, Boolean> _OpFilter;

    private HashMap<String, String> _MetaNames;
    private ArrayList<TtPoint> _DisplayedPoints, _Points;

    private List<String> headers = new ArrayList<>(24);
    private List<Integer> columnKeys = new ArrayList<>(24);
    private List<Integer> columnSizes = new ArrayList<>(24);


    boolean filterGps, filterT5, filterTrav, filterSS, filterQdnm, filterWalk, filterWay;
    boolean filterDisplayed;

    int colorPressed, colorNotPressed;


    View.OnClickListener cellClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView tv = (TextView)v;

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle(String.format("Point: %s", tv.getTag(R.id.tag1)))
                    .setMessage(String.format("%s: %s", tv.getTag(R.id.tag2), tv.getTag(R.id.tag3)))
                    .show();
        }
    };


    Activity activity;



    private PointsTableAdapter pointsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_edit);

        activity = this;

        ssvPoints = findViewById(R.id.teSsvPoints);
        ssvPoints.setFinalLayout();
        progressBar = findViewById(R.id.teProgress);
        filterView = findViewById(R.id.teLayFilters);

        colorNotPressed = AndroidUtils.UI.getColor(this, R.color.primaryLight);
        colorPressed = AndroidUtils.UI.getColor(this, R.color.primaryLighter);

        filterGps = filterT5 = filterTrav = filterSS = filterQdnm = filterWalk = filterWay = true;

        viewOptions = new ViewOptions();
    }


    @Override
    protected void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
                setupTable();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        inflateMenu(R.menu.menu_table_edit, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.teMenuFilter: {
                if (filterDisplayed) {
                    ViewAnimator.collapseView(filterView);
                } else {
                    ViewAnimator.expandView(filterView);
                }

                filterDisplayed = !filterDisplayed;
                break;
            }
            case R.id.teMenuColumns: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Select Columns");

                builder.setMultiChoiceItems(Headers, viewOptions.getOptions(), new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        viewOptions.setOption(which, isChecked);
                    }
                });

                builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setupHeaders();
                        pointsAdapter.notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton(R.string.menu_table_edit_reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewOptions = new ViewOptions(true);
                        setupHeaders();
                        pointsAdapter.notifyDataSetChanged();
                    }
                });

                builder.show();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    private void setupTable() {
        _Polygons = new ArrayList<>();
        _Points = new ArrayList<>();
        _Metadata = getTtAppCtx().getDAL().getMetadataMap();
        _MetaNames = new HashMap<>();

        _PolygonFilter = new HashMap<>();
        _OpFilter = new HashMap<>();

        for (OpType op : OpType.values()) {
            _OpFilter.put(op, true);
        }

        for (TtMetadata meta : _Metadata.values()) {
            _MetaNames.put(meta.getCN(), meta.getName());
        }

        for (TtPolygon poly : getTtAppCtx().getDAL().getPolygons()) {
            _Polygons.add(poly);
            _PolygonFilter.put(poly.getCN(), true);

            _Points.addAll(getTtAppCtx().getDAL().getPointsInPolygon(poly.getCN()));
        }

        _DisplayedPoints = new ArrayList<>();

        setupHeaders();

        filterPoints();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pointsAdapter = new PointsTableAdapter(getBaseContext());
                ssvPoints.setAdapter(pointsAdapter);

                ssvPoints.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }


    private void setupHeaders() {
        headers.clear();
        columnKeys.clear();
        columnSizes.clear();

        if (viewOptions.OpType) {
            headers.add(Headers[0]);
            columnKeys.add(0);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 70));
        }

        if (viewOptions.Index) {
            headers.add(Headers[1]);
            columnKeys.add(1);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 50));
        }

        if (viewOptions.Polygon) {
            headers.add(Headers[2]);
            columnKeys.add(2);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.DateTime) {
            headers.add(Headers[3]);
            columnKeys.add(3);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.OnBound) {
            headers.add(Headers[4]);
            columnKeys.add(4);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 60));
        }

        if (viewOptions.Metadata) {
            headers.add(Headers[5]);
            columnKeys.add(5);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.AdjX) {
            headers.add(Headers[6]);
            columnKeys.add(6);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.AdjY) {
            headers.add(Headers[7]);
            columnKeys.add(7);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 105));
        }

        if (viewOptions.AdjZ) {
            headers.add(Headers[8]);
            columnKeys.add(8);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 85));
        }

        if (viewOptions.UnAdjX) {
            headers.add(Headers[9]);
            columnKeys.add(9);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.UnAdjY) {
            headers.add(Headers[10]);
            columnKeys.add(10);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 105));
        }

        if (viewOptions.UnAdjZ) {
            headers.add(Headers[11]);
            columnKeys.add(11);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 85));
        }

        if (viewOptions.Accuracy ) {
            headers.add(Headers[12]);
            columnKeys.add(12);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 75));
        }

        if (viewOptions.ManAcc ) {
            headers.add(Headers[13]);
            columnKeys.add(13);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 70));
        }

        if (viewOptions.Latitude) {
            headers.add(Headers[14]);
            columnKeys.add(14);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 70));
        }

        if (viewOptions.Longitude) {
            headers.add(Headers[15]);
            columnKeys.add(15);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 75));
        }

        if (viewOptions.Elevation) {
            headers.add(Headers[16]);
            columnKeys.add(16);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 70));
        }

        if (viewOptions.FwdAz) {
            headers.add(Headers[17]);
            columnKeys.add(17);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 60));
        }

        if (viewOptions.BkAz) {
            headers.add(Headers[18]);
            columnKeys.add(18);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 65));
        }

        if (viewOptions.HorizDist) {
            headers.add(Headers[19]);
            columnKeys.add(19);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 80));
        }
        if (viewOptions.SlopeDist) {
            headers.add(Headers[20]);
            columnKeys.add(20);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 80));
        }

        if (viewOptions.SlopeAngle) {
            headers.add(Headers[21]);
            columnKeys.add(21);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 90));
        }

        if (viewOptions.Parent) {
            headers.add(Headers[22]);
            columnKeys.add(22);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 60));
        }

        if (viewOptions.Comment) {
            headers.add(Headers[23]);
            columnKeys.add(23);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }

        if (viewOptions.LinkedPoints) {
            headers.add(Headers[24]);
            columnKeys.add(24);
            columnSizes.add(AndroidUtils.Convert.dpToPx(this, 100));
        }
    }



    private void filterPoints() {
        _DisplayedPoints.clear();

        for (TtPoint point : _Points) {
            if (!_PolygonFilter.get(point.getPolyCN()))
                continue;

            if (!_OpFilter.get(point.getOp()))
                continue;

            _DisplayedPoints.add(point);
        }
    }


    public void btnTePolys(View view) {
        final CharSequence[] items = new CharSequence[_Polygons.size()];
        final boolean[] selections = new boolean[_Polygons.size()];

        for (int i = 0; i < _Polygons.size(); i++) {
            TtPolygon poly = _Polygons.get(i);

            items[i] = poly.getName();

            if (_PolygonFilter.containsKey(poly.getCN())) {
                selections[i] = _PolygonFilter.get(_Polygons.get(i).getCN());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.str_polygons)
                .setMultiChoiceItems(items, selections, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        _PolygonFilter.put(_Polygons.get(indexSelected).getCN(), isChecked);
                        filterPoints();
                        pointsAdapter.notifyDataSetChanged();
                    }
                })
                .setPositiveButton(R.string.str_ok, null)
                .show();
    }

    public void btnTeGps(View view) {
        filterGps = !filterGps;
        setButtonPressed(view, filterGps);

        _OpFilter.put(OpType.GPS, filterGps);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeTake5(View view) {
        filterT5 = !filterT5;
        setButtonPressed(view, filterT5);

        _OpFilter.put(OpType.Take5, filterT5);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeTrav(View view) {
        filterTrav = !filterTrav;
        setButtonPressed(view, filterTrav);

        _OpFilter.put(OpType.Traverse, filterTrav);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeSS(View view) {
        filterSS = !filterSS;
        setButtonPressed(view, filterSS);

        _OpFilter.put(OpType.SideShot, filterSS);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeQndm(View view) {
        filterQdnm = !filterQdnm;
        setButtonPressed(view, filterQdnm);

        _OpFilter.put(OpType.Quondam, filterQdnm);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeWalk(View view) {
        filterWalk = !filterWalk;
        setButtonPressed(view, filterWalk);

        _OpFilter.put(OpType.Walk, filterWalk);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }

    public void btnTeWay(View view) {
        filterWay = !filterWay;
        setButtonPressed(view, filterWay);

        _OpFilter.put(OpType.WayPoint, filterWay);

        filterPoints();
        pointsAdapter.notifyDataSetChanged();
    }


    private void setButtonPressed(View view, boolean pressed) {
        if (pressed) {
            ViewCompat.setElevation(view, 3);
            view.setBackgroundColor(colorPressed);
        } else {
            ViewCompat.setElevation(view, 0);
            view.setBackgroundColor(colorNotPressed);
        }
    }



    private class PointsTableAdapter extends BaseTableAdapter {
        Context context;
        LayoutInflater inflater;

        int pidHeaderSize, cellHeight;

        private PointsTableAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            pidHeaderSize = AndroidUtils.Convert.dpToPx(context, 50);
            cellHeight = AndroidUtils.Convert.dpToPx(context, 20);
        }

        @Override
        public int getRowCount() {
            return _DisplayedPoints.size();
        }

        @Override
        public int getColumnCount() {
            return columnKeys.size();
        }

        @Override
        public View getView(int row, int column, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(getLayout(row, column), parent, false);
            }

            if (column < 0) {
                setText(getPIDHeader(row), convertView);
            } else if (row < 0) {
                setText(getColumnHeader(column), convertView);
            } else {
                setText(row, column, convertView);
            }

            return convertView;
        }


        private  int getLayout(int row, int column) {
            if (row < 0) {
                //pid header
                return R.layout.spreadsheet_item_header;
            } else  {
                if (column < 0){
                    //column header
                    return R.layout.spreadsheet_item_header;
                } else {
                    //content
                    return R.layout.spreadsheet_item;
                }
            }
        }

        private void setText(String text, View view) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(text);
        }

        private void setText(int row, int columnIndex, View view) {
            TextView tv = view.findViewById(android.R.id.text1);

            String value = getValue(row, columnIndex);

            tv.setText(value);

            if (!StringEx.isEmpty(value)) {
                tv.setTag(R.id.tag1, Integer.toString(_DisplayedPoints.get(row).getPID()));
                tv.setTag(R.id.tag2, headers.get(columnKeys.get(columnIndex)));
                tv.setTag(R.id.tag3, getFullValue(row, columnIndex));
                tv.setOnClickListener(cellClickedListener);
            } else {
                tv.setOnClickListener(null);
            }
        }

        private String getColumnHeader(int column) {
            return (column > -1 && column < headers.size()) ? headers.get(column) : StringEx.Empty;
        }

        private String getPIDHeader(int row) {
            return (row > -1 && row < _DisplayedPoints.size()) ? Integer.toString(_DisplayedPoints.get(row).getPID()) : "PID";
        }

        private TtMetadata getMetadata(String cn) {
            if (_Metadata.containsKey(cn)) {
                return _Metadata.get(cn);
            }

            throw new RuntimeException("Metadata NotFound");
        }

        private String getValue(int row, int columnKey) {
            TtPoint point = _DisplayedPoints.get(row);

            switch (columnKey)
            {
                case 0:
                    return point.getOp().toString();
                case 1:
                    return StringEx.toString(point.getIndex());
                case 2:
                    return point.getPolyName();
                case 3:
                    return DateTimeFormatter.print(point.getTime());
                case 4:
                    return StringEx.toString(point.isOnBnd());
                case 5:
                    return _MetaNames.get(point.getMetadataCN());
                case 6:
                    return StringEx.toString(point.getAdjX(), 4);
                case 7:
                    return StringEx.toString(point.getAdjY(), 4);
                case 8:
                    return (point.getAdjZ() != null) ? StringEx.toString(
                            TtUtils.Convert.distance(point.getAdjZ(), TtUtils.Convert.elevationToDistance(getMetadata(point.getMetadataCN()).getElevation()), Dist.Meters),
                            4) : StringEx.Empty;
                case 9:
                    return StringEx.toString(point.getUnAdjX(), 4);
                case 10:
                    return StringEx.toString(point.getUnAdjY(), 4);
                case 11:
                    return StringEx.toString(
                            TtUtils.Convert.distance(point.getUnAdjZ(), TtUtils.Convert.elevationToDistance(getMetadata(point.getMetadataCN()).getElevation()), Dist.Meters),
                            4);
                case 12:
                    return StringEx.toString(point.getAccuracy(), 2);
                case 13:
                    return (point instanceof TtPoint.IManualAccuracy) ? StringEx.toString(((TtPoint.IManualAccuracy)point).getManualAccuracy(), 2) : StringEx.Empty;
                case 14:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getLatitude(), 4) : StringEx.Empty;
                case 15:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getLongitude(), 4) : StringEx.Empty;
                case 16:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getElevation(), 2) : StringEx.Empty;
                case 17:
                    return (point instanceof TravPoint) ? StringEx.toString(((TravPoint)point).getFwdAz(), 2) : StringEx.Empty;
                case 18:
                    return (point instanceof TravPoint) ? StringEx.toString(((TravPoint)point).getBkAz(), 2) : StringEx.Empty;
                case 19:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.distance(((TravPoint)point).getHorizontalDistance(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters),
                            3) : StringEx.Empty;
                case 20:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.distance(((TravPoint)point).getSlopeDistance(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters),
                            3) : StringEx.Empty;
                case 21:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.angle(((TravPoint)point).getSlopeAngle(), getMetadata(point.getMetadataCN()).getSlope(), Slope.Percent),
                            3) : StringEx.Empty;
                case 22:
                    if (point instanceof QuondamPoint) {
                        QuondamPoint qp = ((QuondamPoint)point);
                        if (qp.hasParent())
                            return Integer.toString(qp.getParentPID());
                    }
                    return StringEx.Empty;
                case 23:
                    return point.getComment();
                case 24:
                    return point.hasQuondamLinks() ? "Yes" : "No";
            }

            return null;
        }

        private String getFullValue(int row, int columnKey) {
            TtPoint point = _DisplayedPoints.get(row);

            switch (columnKey)
            {
                case 0:
                    return point.getOp().toString();
                case 1:
                    return StringEx.toString(point.getIndex());
                case 2:
                    return point.getPolyName();
                case 3:
                    return DateTimeFormatter.print(point.getTime());
                case 4:
                    return StringEx.toString(point.isOnBnd());
                case 5:
                    return _MetaNames.get(point.getMetadataCN());
                case 6:
                    return StringEx.toString(point.getAdjX());
                case 7:
                    return StringEx.toString(point.getAdjY());
                case 8:
                    return (point.getAdjZ() != null) ? StringEx.toString(
                            TtUtils.Convert.distance(point.getAdjZ(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters)) : StringEx.Empty;
                case 9:
                    return StringEx.toString(point.getUnAdjX());
                case 10:
                    return StringEx.toString(point.getUnAdjY());
                case 11:
                    return StringEx.toString(
                            TtUtils.Convert.distance(point.getUnAdjZ(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters));
                case 12:
                    return StringEx.toString(point.getAccuracy());
                case 13:
                    return (point instanceof TtPoint.IManualAccuracy) ? StringEx.toString(((TtPoint.IManualAccuracy)point).getManualAccuracy()) : StringEx.Empty;
                case 14:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getLatitude()) : StringEx.Empty;
                case 15:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getLongitude()) : StringEx.Empty;
                case 16:
                    return (point instanceof GpsPoint) ? StringEx.toString(((GpsPoint)point).getElevation()) : StringEx.Empty;
                case 17:
                    return (point instanceof TravPoint) ? StringEx.toString(((TravPoint)point).getFwdAz()) : StringEx.Empty;
                case 18:
                    return (point instanceof TravPoint) ? StringEx.toString(((TravPoint)point).getBkAz()) : StringEx.Empty;
                case 19:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.distance(((TravPoint)point).getHorizontalDistance(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters)) : StringEx.Empty;
                case 20:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.distance(((TravPoint)point).getSlopeDistance(), getMetadata(point.getMetadataCN()).getDistance(), Dist.Meters)) : StringEx.Empty;
                case 21:
                    return (point instanceof TravPoint) ? StringEx.toString(
                            TtUtils.Convert.angle(((TravPoint)point).getSlopeAngle(), getMetadata(point.getMetadataCN()).getSlope(), Slope.Percent)) : StringEx.Empty;
                case 22:
                    if (point instanceof QuondamPoint) {
                        QuondamPoint qp = ((QuondamPoint)point);
                        if (qp.hasParent())
                            return StringEx.toString(qp.getParentPID());
                    }
                    return StringEx.Empty;
                case 23:
                    return point.getComment();
                case 24:
                    return point.hasQuondamLinks() ? "Yes" : "No";
            }

            return null;
        }

        @Override
        public int getWidth(int column) {
            return (column < 0) ? pidHeaderSize : columnSizes.get(column);
        }

        @Override
        public int getHeight(int row) {
            return cellHeight;
        }

        @Override
        public int getItemViewType(int row, int column) {
            if (row < 0) {
                return 0;
            } else  {
                if (column < 0){
                    //column header
                    return 0;
                } else {
                    //content
                    return 1;
                }
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    public static class ViewOptions {
        private boolean OpType, Index, Polygon,
                DateTime, OnBound, Metadata, AdjX, AdjY, AdjZ,
                UnAdjX, UnAdjY, UnAdjZ, Accuracy, ManAcc, Latitude, Longitude,
                Elevation, FwdAz, BkAz, HorizDist, SlopeDist,
                SlopeAngle, Parent, Comment, LinkedPoints;

        public ViewOptions() {
            this(true);
        }

        public ViewOptions(boolean allOptions) {
            OpType = Index = Polygon = DateTime = Metadata =
            OnBound = AdjX = AdjY = AdjZ = UnAdjX = UnAdjY =
            UnAdjZ = Accuracy = ManAcc = Latitude = Longitude = Elevation =
            FwdAz = BkAz = HorizDist = SlopeDist =
            SlopeAngle = Parent = Comment = LinkedPoints = allOptions;
        }

        public ViewOptions(boolean opType, boolean index, boolean polygon, boolean dateTime,
                           boolean metadata, boolean onBound, boolean adjX, boolean adjY,boolean  adjZ,
                           boolean unAdjX, boolean unAdjY, boolean unAdjZ, boolean acc, boolean manAcc,
                           boolean latitude, boolean longitude, boolean elevation, boolean fwdAz,
                           boolean bkAz, boolean horizDist, boolean slopeDist,
                           boolean slopeAngle, boolean parent,
                           boolean comment, boolean linkedPoints) {
            OpType = opType;
            Index = index;
            Polygon = polygon;
            DateTime = dateTime;
            OnBound = onBound;
            Metadata = metadata;
            AdjX = adjX;
            AdjY = adjY;
            AdjZ = adjZ;
            UnAdjX = unAdjX;
            UnAdjY = unAdjY;
            UnAdjZ = unAdjZ;
            Accuracy = acc;
            ManAcc = manAcc;
            Latitude = latitude;
            Longitude = longitude;
            Elevation = elevation;
            FwdAz = fwdAz;
            BkAz = bkAz;
            HorizDist = horizDist;
            SlopeDist = slopeDist;
            SlopeAngle = slopeAngle;
            Parent = parent;
            Comment = comment;
            LinkedPoints = linkedPoints;
        }

        public boolean[] getOptions() {
            return new boolean[] {
                    OpType, Index, Polygon,
                    DateTime, OnBound, Metadata, AdjX, AdjY, AdjZ,
                    UnAdjX, UnAdjY, UnAdjZ, Accuracy, ManAcc, Latitude, Longitude,
                    Elevation, FwdAz, BkAz, HorizDist, SlopeDist,
                    SlopeAngle, Parent, Comment, LinkedPoints
            };
        }

        private void setOption(int position, boolean value) {
            switch (position) {
                case 0:
                    OpType = value;
                    break;
                case 1:
                    Index = value;
                    break;
                case 2:
                    Polygon = value;
                    break;
                case 3:
                    DateTime = value;
                    break;
                case 4:
                    OnBound = value;
                    break;
                case 5:
                    Metadata = value;
                    break;
                case 6:
                    AdjX = value;
                    break;
                case 7:
                    AdjY = value;
                    break;
                case 8:
                    AdjZ = value;
                    break;
                case 9:
                    UnAdjX = value;
                    break;
                case 10:
                    UnAdjY = value;
                    break;
                case 11:
                    UnAdjZ = value;
                    break;
                case 12:
                    Accuracy = value;
                    break;
                case 13:
                    ManAcc = value;
                    break;
                case 14:
                    Latitude = value;
                    break;
                case 15:
                    Longitude = value;
                    break;
                case 16:
                    Elevation = value;
                    break;
                case 17:
                    FwdAz = value;
                    break;
                case 18:
                    BkAz = value;
                    break;
                case 19:
                    HorizDist = value;
                    break;
                case 20:
                    SlopeDist = value;
                    break;
                case 21:
                    SlopeAngle = value;
                    break;
                case 22:
                    Parent = value;
                    break;
                case 23:
                    Comment = value;
                    break;
                case 24:
                    LinkedPoints = value;
                    break;
            }
        }
    }
}
