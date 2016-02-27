package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.android.widget.SheetFab;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.dialogs.LatLonDialog;
import com.usda.fmsc.twotrails.dialogs.MoveToPointDialog;
import com.usda.fmsc.twotrails.dialogs.PointEditorDialog;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.points.BasePointFragment;
import com.usda.fmsc.twotrails.fragments.points.GPSPointFragment;
import com.usda.fmsc.twotrails.fragments.points.QuondamPointFragment;
import com.usda.fmsc.twotrails.fragments.points.TraversePointFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.GpsPoint;
import com.usda.fmsc.twotrails.objects.QuondamPoint;
import com.usda.fmsc.twotrails.objects.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.Units.OpType;
import com.usda.fmsc.twotrails.ui.MSFloatingActionButton;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.utilities.StringEx;

public class PointsActivity extends CustomToolbarActivity {
    private HashMap<String, Listener> listeners;

    private MenuItem miLock, miLink, miMovePoint, miReset,
            miEnterLatLon, miNmeaRecalc, miDelete, miGoto;
    SheetLayoutEx slexAqr, slexCreate;
    android.support.design.widget.FloatingActionButton fabAqr;
    MSFloatingActionButton fabMenu;
    SheetFab fabSheet;


    private boolean ignorePointChange, adjust, menuCreated, aqrVisible = false;
    private OpType currentAqrOp = OpType.GPS, createOpType;

    private ArrayList<TtPoint> _Points;
    private HashMap<String, TtPolygon> _Polygons;
    private HashMap<String, TtMetadata> _MetaData;
    private TtPoint _CurrentPoint, _deletePoint;
    private TtPolygon _CurrentPolygon;
    private TtMetadata _CurrentMetadata;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _PointUpdated, _PointLocked;

    private String addedPoint;


    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;


    private ComplexOnPageChangeListener onPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignorePointChange) {
                savePoint();

                _CurrentIndex = position;
                _CurrentPoint = getPointAtIndex(_CurrentIndex);
                _CurrentMetadata = _MetaData.get(_CurrentPoint.getMetadataCN());
                updateButtons();
            }

            ignorePointChange = false;

            AndroidUtils.UI.hideKeyboard(PointsActivity.this);
        }

        @Override
        public void onPageChanged() {
            super.onPageChanged();

            if (_deleteIndex > INVALID_INDEX) {
                boolean halfFinishedTrav = false;

                if (_deletePoint.isTravType()) {
                    TravPoint tp = (TravPoint)_deletePoint;

                    if ((tp.getFwdAz() != null || tp.getBkAz() != null) ||
                            tp.getSlopeAngle() != 0 || tp.getSlopeDistance() > 0) {
                        halfFinishedTrav = true;
                    }
                }

                if (!halfFinishedTrav && (Global.Settings.DeviceSettings.getDropZeros() || !_deletePoint.getPolyCN().equals(_CurrentPolygon.getCN()))) {
                    deleteWithoutMoving();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(PointsActivity.this);

                    dialog.setTitle("Invalid Point");

                    if (halfFinishedTrav) {
                        dialog.setMessage(String.format("The %s point %d has a partial value. Would you like to finish or delete the point.",
                                _deletePoint.getOp().toString(),
                                _deletePoint.getPID()));
                    } else {
                        dialog.setMessage(String.format("The point %d has no value. Would you like to edit or delete the point.",
                                _deletePoint.getPID()));
                    }

                    dialog.setPositiveButton(getString(R.string.str_delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteWithoutMoving();
                        }
                    });

                    dialog.setNegativeButton(getString(R.string.str_edit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ignorePointChange = true;
                            mViewPager.setCurrentItem(_deleteIndex);

                            _CurrentPoint = _deletePoint;
                            _CurrentIndex = _deleteIndex;

                            lockPoint(false);

                            _deleteIndex = INVALID_INDEX;
                            _deletePoint = null;
                        }
                    });

                    dialog.show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setUseExitWarning(true);

        listeners = new HashMap<>();

        _Polygons = Global.DAL.getPolygonsMap();
        _MetaData = Global.DAL.getMetadataMap();

        final TtPolygon[] polyArray = _Polygons.values().toArray(new TtPolygon[_Polygons.size()]);
        Arrays.sort(polyArray);

        _Points = new ArrayList<>();
        _CurrentIndex = INVALID_INDEX;

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.saveFragmentStates(false);

        mViewPager = (ViewPager)findViewById(R.id.pointsViewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(onPageChangeListener);


        //region Main Buttons
        fabAqr = (android.support.design.widget.FloatingActionButton)findViewById(R.id.pointsFabAqr);
        fabMenu = (MSFloatingActionButton)findViewById(R.id.pointsFabMenu);
        View overlay = findViewById(R.id.overlay);
        View sheetView = findViewById(R.id.fab_sheet);

        int bc = AndroidUtils.UI.getColor(this, R.color.background_card_view);
        int fc = AndroidUtils.UI.getColor(this, R.color.primaryLight);

        fabSheet = new SheetFab<>(fabMenu, sheetView, overlay, bc, fc);

        fabSheet.setListener(new SheetFab.SheetFabListener() {
            @Override
            public void onShowSheet() {

            }

            @Override
            public void onSheetShown() {

            }

            @Override
            public void onHideSheet() {

            }

            @Override
            public void onSheetHidden() {
                if (createOpType != null) {

                    switch (createOpType) {
                        case Take5:
                        case Walk:
                            slexCreate.expandFab();
                            break;
                        default:
                            createPoint(createOpType);
                            createOpType = null;
                            break;
                    }

                }
            }
        });

        AndroidUtils.UI.setContentDescToast(fabAqr, getString(R.string.str_acquire));
        //endregion

        //region ToolBar
        final ArrayAdapter<TtPolygon> polyAdapter = new ArrayAdapter<>
                (this, R.layout.control_spinner_points_polys, polyArray);

        polyAdapter.setDropDownViewResource(R.layout.list_item_fill);

        final AppCompatSpinner spinnerPoly = (AppCompatSpinner)findViewById(R.id.pointsToolBarSpinnerPoly);
        spinnerPoly.setAdapter(polyAdapter);

        String lastPolyCN = Global.Settings.ProjectSettings.getLastEditedPolyCN();
        if (_Polygons.containsKey(lastPolyCN)) {
            TtPolygon tmp;
            boolean lastSet = false;
            for (int i = 0; i < _Polygons.size(); i++) {
                tmp = polyArray[i];

                if (tmp.getCN().equals(lastPolyCN)) {
                    changePolygon(tmp);
                    spinnerPoly.setSelection(i);
                    lastSet = true;
                }
            }

            if (!lastSet) {
                changePolygon(polyArray[0]);
            }
        } else {
            changePolygon(polyArray[0]);
        }

        spinnerPoly.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                changePolygon(polyAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //endregion

        slexAqr = (SheetLayoutEx)findViewById(R.id.pointsSLExAqr);
        slexAqr.setFab(fabAqr);
        slexAqr.setFabAnimationEndListener(new SheetLayoutEx.OnFabAnimationEndListener() {
            @Override
            public void onFabAnimationEnd() {
                acquireGpsPoint(_CurrentPoint, null);
            }
        });

        slexCreate = (SheetLayoutEx)findViewById(R.id.pointsSLExCreate);
        slexCreate.setFab(fabMenu);
        slexCreate.setFabAnimationEndListener(new SheetLayoutEx.OnFabAnimationEndListener() {
            @Override
            public void onFabAnimationEnd() {
                if (createOpType == OpType.Take5) {
                    acquireT5Points(_CurrentPoint);
                } else if (createOpType == OpType.Walk) {
                    acquireWalkPoints(_CurrentPoint);
                }

                createOpType = null;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePoint();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savePoint();

        if (adjust) {
            PolygonAdjuster.adjust(Global.DAL, Global.getMainActivity(), true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_points, menu);

        miLock = menu.findItem(R.id.pointsMenuLock);
        miLink = menu.findItem(R.id.pointsMenuLink);
        miMovePoint = menu.findItem(R.id.pointsMenuMovePoint);
        miReset = menu.findItem(R.id.pointsMenuReset);
        miEnterLatLon = menu.findItem(R.id.pointsMenuEnterLatLon);
        miNmeaRecalc = menu.findItem(R.id.pointsMenuRecalcNmea);
        miDelete = menu.findItem(R.id.pointsMenuDelete);
        miGoto = menu.findItem(R.id.pointsMenuGotoPoint);

        if (_Points.size() < 1) {
            TtUtils.UI.disableMenuItem(miGoto);
            TtUtils.UI.disableMenuItem(miLock);
        }

        menuCreated = true;
        updateButtons();

        return true;
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        AndroidUtils.UI.addIconsToMenu(menu);

        return super.onPrepareOptionsPanel(view, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pointsMenuLink: {
                jumpToQuondam(_CurrentPoint);
                break;
            }
            case R.id.pointsMenuLock: {
                lockPoint(!_PointLocked);
                break;
            }
            case R.id.pointsMenuSettings: {
                startActivity(new Intent(this, PreferenceActivity.class));
                break;
            }
            case R.id.pointsMenuGotoPoint: {
                if (_Points.size() > 0) {
                    MoveToPointDialog mdialog = new MoveToPointDialog();

                    mdialog.setOnItemClick(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            moveToPoint(position);
                        }
                    });

                    mdialog.setFirstListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveToPoint(0);
                        }
                    });

                    mdialog.setLastListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveToPoint(_Points.size() - 1);
                        }
                    });

                    mdialog.setNegativeButton("Cancel", null);

                    mdialog.setItems(_Points, _CurrentIndex);
                    mdialog.setTitle("Jump To Point");

                    mdialog.show(getSupportFragmentManager(), "JUMP_POINTS");
                }
                break;
            }
            case R.id.pointsMenuMovePoint: {
                Toast.makeText(this, "Unimplemented", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.pointsMenuDelete: {
                if (!_PointLocked) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setMessage(String.format("Delete Point %d", _CurrentPoint.getPID()));

                    alert.setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AnimationCardFragment card = ((AnimationCardFragment) mSectionsPagerAdapter.getFragments().get(_CurrentIndex));

                            card.setVisibilityListener(new AnimationCardFragment.VisibilityListener() {
                                @Override
                                public void onHidden() {

                                    new Handler().post(new Runnable() {
                                        public void run() {
                                            if (_CurrentIndex == 0 && _Points.size() < 2) {
                                                deletePoint(_CurrentPoint, _CurrentIndex);

                                                if (_Points.size() < 1) {
                                                    _CurrentPoint = null;
                                                    _CurrentIndex = INVALID_INDEX;
                                                    lockPoint(true);
                                                    TtUtils.UI.disableMenuItem(miLock);
                                                    hideAqr();
                                                }
                                            } else {
                                                _deleteIndex = _CurrentIndex;
                                                _deletePoint = _CurrentPoint;

                                                if (_CurrentIndex > 0) {
                                                    _CurrentIndex--;
                                                } else {
                                                    _CurrentIndex++;
                                                }

                                                ignorePointChange = true;
                                                moveToPoint(_CurrentIndex);
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onVisible() {

                                }
                            });

                            card.hideCard();
                        }
                    });

                    alert.setNeutralButton(R.string.str_cancel, null);

                    alert.create().show();
                }
                break;
            }
            case R.id.pointsMenuReset: {
                resetPoint();
                break;
            }
            case R.id.pointsMenuEnterLatLon: {
                if (_CurrentPoint.isGpsType()) {
                    LatLonDialog dialog = LatLonDialog.newInstance((GpsPoint)_CurrentPoint);

                    dialog.setOnEditedListener(new LatLonDialog.OnEditedListener() {
                        @Override
                        public void onEdited(String cn, Double lat, Double lon) {
                            if (_CurrentPoint.getCN().equals(cn)) {
                                UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(lat, lon, _CurrentMetadata.getZone());

                                GpsPoint point = (GpsPoint)_CurrentPoint;

                                point.setLatitude(lat);
                                point.setLongitude(lon);

                                point.setUnAdjX(coords.getX());
                                point.setUnAdjY(coords.getY());

                                onPointUpdate();
                            }
                        }
                    });

                    dialog.show(getSupportFragmentManager(), "ENTER_LATLON");
                }
                break;
            }
            case R.id.pointsMenuRecalcNmea: {
                calculateGpsPoint();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (fabSheet.isSheetVisible()) {
            fabSheet.hideSheet();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Consts.Activities.ACQUIRE:
            case Consts.Activities.CALCULATE: {
                slexAqr.contractFab();
                calculateResult(resultCode, data);
                break;
            }
            case Consts.Activities.TAKE5:
            case Consts.Activities.WALK: {
                slexCreate.contractFab();
                addInsertPointsResult(resultCode, data);
                break;
            }
        }
    }

    private void calculateResult(int resultCode, Intent data) {
        if (resultCode == Consts.Activities.Results.POINT_CREATED) {
            GpsPoint point = (GpsPoint)data.getSerializableExtra(Consts.Activities.Data.POINT_DATA);

            updatePoint(point);
            onPointUpdate();
        }
    }

    private void addInsertPointsResult(int resultCode, Intent data) {
        if (resultCode == Consts.Activities.Results.POINT_CREATED) {
            Bundle bundle = data.getExtras();
            int created = 1;

            if (bundle.containsKey(Consts.Activities.Data.NUMBER_OF_CREATED_POINTS)) {
                created = bundle.getInt(Consts.Activities.Data.NUMBER_OF_CREATED_POINTS);
            }

            if (_CurrentIndex < _Points.size() - 1) {
                ArrayList<TtPoint> updatePoints = new ArrayList<>();
                TtPoint tmpPoint;

                for (int i = _CurrentIndex + 1; i < _Points.size(); i++) {
                    tmpPoint = _Points.get(i);
                    tmpPoint.setIndex(tmpPoint.getIndex() + created);
                    updatePoints.add(tmpPoint);
                }

                Global.DAL.updatePoints(updatePoints);
            }

            int goToPoint = _CurrentIndex + created;

            _Points = Global.DAL.getPointsInPolygon(_CurrentPolygon.getCN());

            mSectionsPagerAdapter.notifyDataSetChanged();

            int pointSize = _Points.size();

            if (pointSize > 0) {
                if (menuCreated) {
                    TtUtils.UI.enableMenuItem(miGoto);
                    TtUtils.UI.enableMenuItem(miLock);
                }
            }

            if (goToPoint < pointSize) {
                moveToPoint(goToPoint);
            } else {
                moveToPoint(pointSize - 1);
            }

            adjust = true;
        }
    }
    //endregion


    //region Save Delete Create Reset
    private boolean savePoint() {
        if (_CurrentPoint != null) {
            boolean phv = TtUtils.pointHasValue(_CurrentPoint);

            _deleteIndex = INVALID_INDEX;
            _deletePoint = null;

            if (_PointUpdated && phv) {
                try {
                    boolean updated = false;

                    TtPoint oldPoint = _Points.get(_CurrentIndex);

                    if (TtUtils.pointHasValue(oldPoint)) {
                        if (!TtUtils.pointHasChanges(_CurrentPoint, oldPoint)) {
                            setPointUpdated(false);
                            return true;
                        }

                        Global.DAL.updatePoint(_CurrentPoint);
                        updated = true;
                    } else {
                        Global.DAL.insertPoint(_CurrentPoint);
                    }

                    if (_CurrentPoint.getOp() == OpType.Quondam) {
                        QuondamPoint currQndm = (QuondamPoint)_CurrentPoint;
                        QuondamPoint oldQndm = (QuondamPoint)_Points.get(_CurrentIndex);
                        TtPoint tmp;

                        //points link and unlink saved when quondam is saved
                        //add link to new linked point in list
                        tmp = getPoint(currQndm.getParentCN());

                        if (tmp != null) {
                            tmp.addQuondamLink(currQndm.getCN());
                            onPointUpdate(tmp);

                            if (updated) {
                                //remove link from old linked point in list
                                tmp = getPoint(oldQndm.getParentCN());
                                tmp.removeQuondamLink(currQndm.getCN());
                                onPointUpdate(tmp);
                            }
                        }
                    }

                    _Points.set(_CurrentIndex, _CurrentPoint);

                    if (_CurrentPoint.getIndex() != _CurrentIndex) {
                        updatePointIndexes(0);
                    } else {
                        updatePointIndexes(_CurrentIndex + 1);
                    }

                    setPointUpdated(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else if (!phv) {
                _deleteIndex = _CurrentIndex;
                _deletePoint = _CurrentPoint;
            }
        }

        return true;
    }

    private void deleteWithoutMoving() {
        boolean samePoly = _deletePoint.getPolyCN().equals(_CurrentPolygon.getCN());

        if (deletePoint(_deletePoint, _deleteIndex)) {
            if (samePoly) {
                if (_deleteIndex < _CurrentIndex)
                    _CurrentIndex--;

                if (_deleteIndex > 0) {
                    moveToPoint(_CurrentIndex);
                } else {
                    moveToPoint(_CurrentIndex, false);
                }

                _deleteIndex = INVALID_INDEX;
                _deletePoint = null;

                lockPoint(true);
            }
        } else {
            Toast.makeText(this, "Error deleting point.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deletePoint(TtPoint point, int index) {
        try {
            if (point != null) {
                Global.DAL.deletePointSafe(point);

                if (point.getOp() == OpType.Quondam) {
                    QuondamPoint qp = (QuondamPoint) point;
                    if (qp.hasParent() && qp.getParentPoint().getPolyCN().equals(_CurrentPolygon.getCN())) {
                        TtPoint tmp = getPoint(qp.getParentCN());
                        tmp.removeQuondamLink(qp.getCN());
                        onPointUpdate(tmp);
                    }
                }

                if (point.hasQuondamLinks()) {
                    for (String qndmCN : point.getLinkedPoints()) {
                        TtPoint convertedPoint = Global.DAL.getPointByCN(qndmCN);

                        for (int i = 0; i < _Points.size(); i++) {
                            if (_Points.get(i).getCN().equals(qndmCN)) {
                                _Points.set(i, convertedPoint);
                            }
                        }
                    }
                }

                if (index > INVALID_INDEX) {
                    _Points.remove(index);
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    _deleteIndex = INVALID_INDEX;
                    _deletePoint = null;
                }

                setPointUpdated(false);

                adjust = true;
            }
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "PointsActivity:deletePoint");
            return false;
        }

        return true;
    }

    private void createPoint() {
        if (_CurrentPoint != null &&
                _CurrentPoint.getOp() != OpType.Take5 && _CurrentPoint.getOp() != OpType.Walk) {
            createPoint(_CurrentPoint.getOp());
        } else {
            createPoint(OpType.GPS);
        }
    }

    private void createPoint(final OpType op) {
        if (_CurrentPoint != null && !TtUtils.pointHasValue(_CurrentPoint)) {
            if (_CurrentPoint.getOp() == op) {
                return;
            }

            BasePointFragment fragment = (BasePointFragment) mSectionsPagerAdapter.getFragments().get(_CurrentIndex);

            fragment.setVisibilityListener(new BasePointFragment.VisibilityListener() {
                @Override
                public void onHidden() {

                    if (deletePoint(_deletePoint, INVALID_INDEX)) {
                        _Points.remove(_CurrentIndex);
                        _CurrentIndex--;

                        if (_CurrentIndex > INVALID_INDEX) {
                            _CurrentPoint = _Points.get(_CurrentIndex);
                        } else {
                            _CurrentPoint = null;
                        }
                    } else {
                        throw new RuntimeException("Unable to delete point.");
                    }

                    createPoint(op);

                    BasePointFragment fragment = (BasePointFragment) mSectionsPagerAdapter.getFragments().get(_CurrentIndex);
                    fragment.showCard();
                }

                @Override
                public void onVisible() {

                }
            });
            fragment.hideCard();

            return;
        } else {
            savePoint();
        }


        TtPoint newPoint =  TtUtils.getPointByOpType(op);
        newPoint.setCN(java.util.UUID.randomUUID().toString());

        if (_CurrentPoint != null) {
            newPoint.setOnBnd(_CurrentPoint.isOnBnd());
        } else {
            newPoint.setOnBnd(true);
        }

        newPoint.setPolyCN(_CurrentPolygon.getCN());
        newPoint.setPolyName(_CurrentPolygon.getName());

        newPoint.setGroupCN(Global.getMainGroup().getCN());
        newPoint.setGroupName(Global.getMainGroup().getName());

        if (_CurrentMetadata != null) {
            newPoint.setMetadataCN(_CurrentMetadata.getCN());
        } else {
            newPoint.setMetadataCN(Global.getDefaultMeta().getCN());
        }

        if (_Points.size() > 0 && _CurrentIndex < _Points.size() - 1) {
            //insert
            newPoint.setPID(PointNamer.nameInsertPoint(_Points.get(_CurrentIndex)));
            _CurrentIndex++;
            _Points.add(_CurrentIndex, newPoint);

            updatePointIndexes(_CurrentIndex);
        } else {
            //add
            if (_Points.size() > 0) {
                newPoint.setPID(PointNamer.namePoint(_Points.get(_CurrentIndex), _CurrentPolygon));
                _CurrentIndex++;
            } else {
                newPoint.setPID(PointNamer.nameFirstPoint(_CurrentPolygon));
                _CurrentIndex = 0;
            }
            _Points.add(newPoint);
        }

        newPoint.setIndex(_CurrentIndex);

        try {
            addedPoint = newPoint.getCN();
            mSectionsPagerAdapter.notifyDataSetChanged();
            moveToPoint(_CurrentIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        lockPoint(false);
        adjust = true;
    }

    private void updatePointIndexes(final int startIndex) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<TtPoint> tmpPoints = new ArrayList<>();

                for (int i = startIndex; i < _Points.size(); i++) {
                    final TtPoint tmp = _Points.get(i);

                    if (tmp.getIndex() != i) {
                        tmp.setIndex(i);
                        tmpPoints.add(tmp);

                        //update the fragments before and after the current point
                        if (i > _CurrentIndex - 2 || i < _CurrentIndex + 2) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    onPointUpdate(tmp);
                                }
                            });
                        }
                    }
                }

                if (tmpPoints.size() > 0) {
                    Global.DAL.updatePoints(tmpPoints);
                }
            }
        }).start();
    }

    private void resetPoint() {
        if (_PointUpdated) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setTitle(String.format("Reset Point %d", _CurrentPoint.getPID()));
            dialog.setMessage(getString(R.string.points_reset_diag));

            dialog.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    _CurrentPoint = getPointAtIndex(_CurrentIndex);
                    onPointUpdate(_CurrentPoint);
                    updateButtons();
                    setPointUpdated(false);
                    lockPoint(false);
                }
            });

            dialog.setNeutralButton(getString(R.string.str_cancel), null);

            dialog.show();
        }
    }
    //endregion


    //region Get Move
    private TtPoint getPointAtIndex(int index) {
        TtPoint point = null;

        if (index > INVALID_INDEX && index < _Points.size()) {
            point = TtUtils.clonePoint(_Points.get(index));
        }

        return point;
    }

    private void moveToPoint(TtPoint point) {
        if (!_CurrentPolygon.getCN().equals(point.getPolyCN())) {
            changePolygon(_Polygons.get(point.getPolyCN()));
        }

        moveToPoint(point.getCN());
    }

    private void moveToPoint(String pointCN) {
        for (int i = 0; i < _Points.size(); i++) {
            if (_Points.get(i).getCN().equals(pointCN)) {
                moveToPoint(i);
                break;
            }
        }
    }

    private void moveToPoint(int index) {
        moveToPoint(index, true);
    }

    private void moveToPoint(int index, boolean smoothScroll) {
        if (index > INVALID_INDEX && index < _Points.size()) {
            mViewPager.setCurrentItem(index, smoothScroll);
            _CurrentPoint = getPointAtIndex(index);
            _CurrentMetadata = _MetaData.get(_CurrentPoint.getMetadataCN());
            _CurrentIndex = index;
        } else {
            _CurrentPoint = null;
            _CurrentMetadata = null;
            _CurrentIndex = INVALID_INDEX;
        }

        updateButtons();
    }

    private void changePolygon(TtPolygon polygon) {
        if (_CurrentPolygon == null || !_CurrentPolygon.getCN().equals(polygon.getCN())) {
            if (savePoint()) {
                if (_deleteIndex > INVALID_INDEX) {
                    deletePoint(_deletePoint, INVALID_INDEX);
                }

                _CurrentPolygon = polygon;
                _CurrentPoint = null;
                _CurrentIndex = INVALID_INDEX;
                _Points = Global.DAL.getPointsInPolygon(_CurrentPolygon.getCN());

                if (_Points == null) {
                    Toast.makeText(this, "DATA ERROR", Toast.LENGTH_SHORT).show();
                    _Points = new ArrayList<>();
                    return;
                }

                mSectionsPagerAdapter.notifyDataSetChanged();

                int pointSize = _Points.size();
                if (pointSize > 0) {
                    moveToPoint(pointSize - 1);

                    if (menuCreated) {
                        TtUtils.UI.enableMenuItem(miGoto);
                        TtUtils.UI.enableMenuItem(miLock);
                    }
                } else {
                    _CurrentMetadata = null;
                    updateButtons();

                    if (menuCreated) {
                        TtUtils.UI.disableMenuItem(miGoto);
                        TtUtils.UI.disableMenuItem(miLock);
                    }
                }

                Global.Settings.ProjectSettings.setLastEditedPolyCN(polygon.getCN());
            }
        }
    }

    private void jumpToQuondam(TtPoint point) {

        final ArrayList<TtPoint> points = new ArrayList<>();
        for (String cn : point.getLinkedPoints()) {
            points.add(Global.DAL.getPointByCN(cn));
        }

        if (points.size() > 0) {
            if (points.size() > 1) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                dialogBuilder.setTitle("Linked Points");
                ListView listView = new ListView(this);

                final PointDetailsAdapter pda = new PointDetailsAdapter(points, this, AppUnits.IconColor.Dark);
                pda.setShowPolygonName(true);

                listView.setAdapter(pda);

                dialogBuilder.setView(listView);
                dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                final AlertDialog dialog = dialogBuilder.create();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        moveToPoint(pda.getPoint(i));
                        dialog.dismiss();
                    }
                });

                dialog.show();
            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                final TtPoint linkedPoint = points.get(0);

                dialog.setMessage(String.format("Move to Quondam %d in polygon %s.",
                        linkedPoint.getPID(), _Polygons.get(linkedPoint.getPolyCN()).getName()));

                dialog.setPositiveButton(R.string.str_move, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        moveToPoint(linkedPoint);
                    }
                });

                dialog.setNeutralButton(R.string.str_cancel, null);

                dialog.show();
            }
        } else {
            Toast.makeText(this, "No Linked Points", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    //region Update UI
    private void updateButtons() {
        boolean setLinkVisible = false;
        boolean setPolyChangeVisible = _Polygons.size() > 1;
        boolean setGpsTypeVisible = false;

        if (_CurrentPoint != null) {
            setLinkVisible = (_CurrentPoint.getLinkedPoints().size() > 0);
            setGpsTypeVisible = _CurrentPoint.isGpsType();

            OpType currop = _CurrentPoint.getOp();
            if (currop == OpType.GPS || currop == OpType.WayPoint) {
                showAqr();

                if (currop != currentAqrOp) {
                    changeAqr(currop, currentAqrOp);
                    currentAqrOp = currop;
                }
            } else if (aqrVisible) {
                hideAqr();
            }
        } else {
            if (aqrVisible) {
                hideAqr();
            }
        }

        //menu items that dont rely on a valid point
        if (menuCreated) {
            miLink.setVisible(setLinkVisible);
            miMovePoint.setVisible(setPolyChangeVisible);
            miNmeaRecalc.setVisible(setGpsTypeVisible);
            miEnterLatLon.setVisible(setGpsTypeVisible);
        }

        lockPoint(true);
    }

    private void lockPoint(boolean lockPoint) {
        if (lockPoint) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed);

                TtUtils.UI.disableMenuItem(miMovePoint);
                TtUtils.UI.disableMenuItem(miReset);
                TtUtils.UI.disableMenuItem(miDelete);
                TtUtils.UI.disableMenuItem(miNmeaRecalc);
                TtUtils.UI.disableMenuItem(miEnterLatLon);
            }

            fabAqr.setEnabled(false);

            _PointLocked = true;
            onLockChange();
        } else if (_Points.size() > 0) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_lock);
                miLock.setIcon(R.drawable.ic_action_lock_open);


                TtUtils.UI.enableMenuItem(miMovePoint);
                TtUtils.UI.enableMenuItem(miDelete);
                TtUtils.UI.enableMenuItem(miNmeaRecalc);
                TtUtils.UI.enableMenuItem(miEnterLatLon);

                if (_PointUpdated) {
                    TtUtils.UI.enableMenuItem(miReset);
                } else {
                    TtUtils.UI.disableMenuItem(miReset);
                }
            }

            fabAqr.setEnabled(true);

            _PointLocked = false;
            onLockChange();
        }
    }

    private void setPointUpdated(boolean updated) {
        _PointUpdated = updated;

        if (menuCreated) {
            if (_PointUpdated) {
                TtUtils.UI.enableMenuItem(miReset);
            } else {
                TtUtils.UI.disableMenuItem(miReset);
            }
        }

        if (_PointUpdated) {
            adjust = true;
        }
    }


    private void changeAqr(OpType to, OpType from) {
        Drawable[] draws = new Drawable[]{
                TtUtils.UI.getTtMiniOpDrawable(from, this),
                TtUtils.UI.getTtMiniOpDrawable(to, this)
        };

        TransitionDrawable trans = new TransitionDrawable(draws);
        trans.setCrossFadeEnabled(true);
        fabAqr.setImageDrawable(trans);
        trans.startTransition(250);

        CharSequence cd;

        if (to == OpType.GPS || to == OpType.WayPoint || to.isTravType()) {
            cd = getString(R.string.str_acquire);
        } else {
            cd = getString(R.string.str_convert);
        }

        AndroidUtils.UI.setContentDescToast(fabAqr, cd);
    }

    private void showAqr() {
        if (!aqrVisible) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_up_in_fast);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabAqr.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            aqrVisible = true;
            fabAqr.setAnimation(a);
            fabAqr.animate();
        }
    }

    private void hideAqr() {
        if (fabAqr.getVisibility() == View.VISIBLE) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_down_out_fast);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabAqr.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            aqrVisible = false;
            fabAqr.setAnimation(a);
            fabAqr.animate();
        }
    }
    //endregion


    //region Fragment Calls
    private void onLockChange() {
        for (Listener listener : listeners.values()) {
            listener.onLockChange(_PointLocked);
        }
    }

    private void onPointUpdate() {
        setPointUpdated(true);

        if (listeners.containsKey(_CurrentPoint.getCN())) {
            listeners.get(_CurrentPoint.getCN()).onPointUpdated(_CurrentPoint);
        }
    }

    private void onPointUpdate(TtPoint point) {
        if (listeners.containsKey(point.getCN())) {
            listeners.get(point.getCN()).onPointUpdated(point);
        }
    }


    public void updatePoint(TtPoint point) {
        //only update if current point
        if (_CurrentPoint.getCN().equals(point.getCN())) {
            _CurrentPoint = point;
            setPointUpdated(true);
        }

        adjust = true;
    }

    public TtPoint getPoint(String cn) {
        for (TtPoint point : _Points) {
            if (point.getCN().equals(cn)) {
                return TtUtils.clonePoint(point);
            }
        }

        return null;
    }

    public TtMetadata getMetadata(String cn) {
        return _MetaData.get(cn);
    }

    public HashMap<String, TtPolygon> getPolygons() {
        return _Polygons;
    }
    //endregion


    //region Misc
    private void configGps() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("The GPS is currently not configured. Would you like to configure it now?");

        dialog.setPositiveButton("Configure", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getBaseContext(), SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
            }
        });

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }
    //endregion


    //region Acquire Calculate
    private void acquireGpsPoint(TtPoint point, ArrayList<TtNmeaBurst> bursts) {
        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            configGps();
        } else {
            Intent intent = new Intent(this, AcquireGpsActivity.class);
            intent.putExtra(Consts.Activities.Data.POINT_DATA, TtUtils.clonePoint(point));
            intent.putExtra(Consts.Activities.Data.METADATA_DATA, _MetaData.get(point.getMetadataCN()));

            if (bursts != null) {
                try {
                    intent.putExtra(Consts.Activities.Data.ADDITIVE_NMEA_DATA, TtNmeaBurst.burstsToByteArray(bursts));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            startActivityForResult(intent, Consts.Activities.ACQUIRE);
        }
    }

    private void acquireT5Points(TtPoint currentPoint) {
        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            configGps();
        } else {
            Intent intent = new Intent(this, Take5Activity.class);

            if (currentPoint != null) {
                intent.putExtra(Consts.Activities.Data.POINT_DATA, TtUtils.clonePoint(currentPoint));
            }

            if (_CurrentMetadata != null) {
                intent.putExtra(Consts.Activities.Data.METADATA_DATA, _CurrentMetadata);
            } else {
                intent.putExtra(Consts.Activities.Data.METADATA_DATA, _MetaData.get(Consts.EmptyGuid));
            }

            intent.putExtra(Consts.Activities.Data.POLYGON_DATA, _CurrentPolygon);

            startActivityForResult(intent, Consts.Activities.TAKE5);
        }
    }

    private void acquireWalkPoints(TtPoint currentPoint) {
        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            configGps();
        } else {
            Intent intent = new Intent(this, WalkActivity.class);

            if (currentPoint != null) {
                intent.putExtra(Consts.Activities.Data.POINT_DATA, TtUtils.clonePoint(currentPoint));
            }

            if (_CurrentMetadata != null) {
                intent.putExtra(Consts.Activities.Data.METADATA_DATA, _CurrentMetadata);
            } else {
                intent.putExtra(Consts.Activities.Data.METADATA_DATA, _MetaData.get(Consts.EmptyGuid));
            }

            intent.putExtra(Consts.Activities.Data.POLYGON_DATA, _CurrentPolygon);

            startActivityForResult(intent, Consts.Activities.WALK);
        }
    }

    private void calculateGpsPoint() {
        if (_CurrentPoint != null && _CurrentPoint.isGpsType()) {
            ArrayList<TtNmeaBurst> bursts = Global.DAL.getNmeaBurstsByPointCN(_CurrentPoint.getCN());

            if (bursts.size() > 0) {
                Intent intent = new Intent(this, CalculateGpsActivity.class);
                intent.putExtra(Consts.Activities.Data.POINT_DATA, TtUtils.clonePoint(_CurrentPoint));
                intent.putExtra(Consts.Activities.Data.METADATA_DATA, _MetaData.get(_CurrentPoint.getMetadataCN()));

                try {
                    intent.putExtra(Consts.Activities.Data.ADDITIVE_NMEA_DATA, TtNmeaBurst.burstsToByteArray(bursts));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                startActivityForResult(intent, Consts.Activities.CALCULATE);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage("This point has no NMEA data associated with it. Would you like to acquire some data?");

                alert.setPositiveButton("Acquire NMEA", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getBaseContext(), AcquireGpsActivity.class);
                        intent.putExtra(Consts.Activities.Data.POINT_DATA, new GpsPoint(_CurrentPoint));
                        intent.putExtra(Consts.Activities.Data.METADATA_DATA, _MetaData.get(_CurrentPoint.getMetadataCN()));

                        startActivityForResult(intent, Consts.Activities.ACQUIRE);
                    }
                });

                alert.setNeutralButton(R.string.str_cancel, null);

                alert.show();
            }
        }
    }
    //endregion


    //region Controls
    public void btnAcquireClick(View view) {
        if(!_PointLocked && _CurrentPoint != null) {
            switch (_CurrentPoint.getOp()) {
                case GPS: {
                    //region GPS
                    if (Global.Settings.DeviceSettings.isGpsConfigured()) {
                        if (TtUtils.pointHasValue(_CurrentPoint)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                            dialog.setMessage(R.string.points_aqr_diag_gps_msg);

                            dialog.setPositiveButton(R.string.points_aqr_diag_add, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ArrayList<TtNmeaBurst> bursts = Global.DAL.getNmeaBurstsByPointCN(_CurrentPoint.getCN());
                                    acquireGpsPoint(_CurrentPoint, bursts);
                                }
                            });

                            dialog.setNegativeButton(R.string.points_aqr_diag_overwrite, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog.Builder dialogA = new AlertDialog.Builder(getBaseContext());

                                    dialogA.setMessage(R.string.points_aqr_diag_del_msg);

                                    dialogA.setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Global.DAL.deleteNmeaByPointCN(_CurrentPoint.getCN());
                                            acquireGpsPoint(_CurrentPoint, null);
                                        }
                                    });

                                    dialogA.setNeutralButton(R.string.str_cancel, null);

                                    dialogA.show();
                                }
                            });

                            dialog.setNeutralButton(R.string.str_cancel, null);

                            dialog.show();
                        } else {
                            slexAqr.expandFab();
                        }
                    } else {
                        configGps();
                    }
                    //endregion
                    break;
                }
                case Quondam: {
                    //convert
                    break;
                }
            }
        }
    }

    public void btnPointInfo(View view) {
        try {
            PointEditorDialog dialog = PointEditorDialog.newInstance(_CurrentPoint.getCN(), _CurrentPoint.getPID(), _CurrentPoint.getMetadataCN(), _MetaData);

            dialog.setEditPointListener(new PointEditorDialog.PointEditorListener() {
                @Override
                public void onEdited(String cn, int pid, String metacn) {
                    if (_CurrentPoint.getCN().equals(cn)) {
                        _CurrentPoint.setPID(pid);
                        _CurrentPoint.setMetadataCN(metacn);

                        onPointUpdate();
                    }
                }

                @Override
                public void onCanceled() {

                }
            });

            dialog.show(getSupportFragmentManager(), "POINT_EDITOR");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //region Add Points
    public void btnCreatePoint(View view) {
        createPoint();
    }

    public void btnPointNewGpsClick(View view) {
        createOpType = OpType.GPS;
        fabSheet.hideSheet();
    }

    public void btnPointNewTake5Click(View view) {
        createOpType = OpType.Take5;
        fabSheet.hideSheet();
    }

    public void btnPointNewWalkClick(View view) {
        createOpType = OpType.Walk;
        fabSheet.hideSheet();
    }

    public void btnPointNewWayClick(View view) {
        createOpType = OpType.WayPoint;
        fabSheet.hideSheet();
    }

    public void btnPointNewTravclick(View view) {
        createOpType = OpType.Traverse;
        fabSheet.hideSheet();
    }

    public void btnPointNewSideShotClick(View view) {
        createOpType = OpType.SideShot;
        fabSheet.hideSheet();
    }

    public void btnPointNewQuondamClick(View view) {
        createOpType = OpType.Quondam;
        fabSheet.hideSheet();
    }

    //endregion
    //endregion

    public void register(String pointCN, Listener listener) {
        if (listener != null && !listeners.containsKey(pointCN)) {
            listeners.put(pointCN, listener);
        }
    }

    public void unregister(String pointCN) {
        if (listeners.containsKey(pointCN)) {
            listeners.remove(pointCN);
        }
    }



    public class SectionsPagerAdapter extends FragmentStatePagerAdapterEx {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            TtPoint point = _Points.get(position);

            boolean hideCard = !StringEx.isEmpty(addedPoint) && point.getCN().equals(addedPoint);

            if (hideCard) {
                addedPoint = null;
            }

            switch (point.getOp()) {
                case GPS:
                case Take5:
                case Walk:
                case WayPoint: {
                    return GPSPointFragment.newInstance(point.getCN(), hideCard);
                }
                case SideShot:
                case Traverse: {
                    return TraversePointFragment.newInstance(point.getCN(), hideCard);
                }
                case Quondam: {
                    return QuondamPointFragment.newInstance(point.getCN(), hideCard);
                }
            }

            return null;
        }

        @Override
        public int getCount() {
            return _Points.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position >= _Points.size()) {
                return StringEx.Empty;
            } else {
                return StringEx.toString(_Points.get(position).getPID());
            }
        }

        @Override
        public void notifyDataSetChanged() {
            if (listeners != null) {
                listeners.clear();
            }

            super.notifyDataSetChanged();
        }
    }

    public interface Listener {
        void onLockChange(boolean locked);
        void onPointUpdated(TtPoint point);
    }
}
