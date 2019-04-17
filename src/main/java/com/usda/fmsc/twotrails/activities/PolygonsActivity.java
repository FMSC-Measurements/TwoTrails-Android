package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.activities.base.TtAjusterCustomToolbarActivity;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.polygon.PolygonFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.Tuple;

public class PolygonsActivity extends TtAjusterCustomToolbarActivity {
    private HashMap<String, Listener> listeners;

    private MenuItem miLock, miReset, miDelete, miAdjust;

    private ArrayList<TtPolygon> _Polygons;
    private TtPolygon _CurrentPolygon, _deletePolygon;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _PolygonUpdated, adjust, _PolyLocked = true, menuCreated = false;
    private boolean ignorePolyChange;
    private String addedPoly = null;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private ConcurrentHashMap<String, Tuple<Integer, ArrayList<PointD>>> drawPoints = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> hasDrawPoints = new ConcurrentHashMap<>();

    private List<TtPolygon> getPolygons() {
        if (_Polygons == null) {
            _Polygons = getTtAppCtx().getDAL().getPolygons();
            if (mSectionsPagerAdapter != null) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mSectionsPagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        return _Polygons;
    }

    private ComplexOnPageChangeListener onPageChangeArrayListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignorePolyChange) {
                savePolygon();

                _CurrentIndex = position;
                _CurrentPolygon = getPolygonAtIndex(_CurrentIndex);
                updateButtons();
            }

            ignorePolyChange = false;

            AndroidUtils.UI.hideKeyboard(PolygonsActivity.this);
        }

        @Override
        public void onPageChanged() {
            super.onPageChanged();

            if (_deleteIndex > INVALID_INDEX) {
                deleteWithoutMoving();
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polygons);

        listeners = new HashMap<>();

        if (getPolygons().size() > 0) {
            _CurrentIndex = 0;
            _CurrentPolygon = getPolyAtIndex(_CurrentIndex);
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.saveFragmentStates(false);

        mViewPager = findViewById(R.id.polysViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);

            mViewPager.addOnPageChangeListener(onPageChangeArrayListener);
        }

        updateButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePolygon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savePolygon();

        if (adjust) {
            PolygonAdjuster.adjust(getTtAppCtx().getDAL(), true);
        }
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        inflateMenu(R.menu.menu_polygons, menu);

        miLock = menu.findItem(R.id.polyMenuLock);
        miReset = menu.findItem(R.id.polyMenuReset);
        miDelete = menu.findItem(R.id.polyMenuDelete);
        miAdjust = menu.findItem(R.id.polyMenuAdjust);

        menuCreated = true;
        updateButtons();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.polyMenuLock: {
                lockPolygon(!_PolyLocked);
                break;
            }
            case R.id.polyMenuDelete: {
                if (!_PolyLocked) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setMessage(String.format("Delete Polygon %s", _CurrentPolygon.getName()));

                    alert.setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AnimationCardFragment card = ((AnimationCardFragment) mSectionsPagerAdapter.getFragments().get(_CurrentIndex));

                            card.setVisibilityListener(new AnimationCardFragment.VisibilityListener() {
                                @Override
                                public void onHidden() {
                                    new Handler().post(new Runnable() {
                                        public void run() {
                                            if (_CurrentIndex > 0) {
                                                _deleteIndex = _CurrentIndex;
                                                _deletePolygon = _CurrentPolygon;

                                                ignorePolyChange = true;

                                                moveToPolygon(_CurrentIndex - 1);
                                            } else if (getPolygons().size() > 0) {
                                                _deleteIndex = _CurrentIndex;
                                                _deletePolygon = _CurrentPolygon;

                                                ignorePolyChange = true;
                                                moveToPolygon(_CurrentIndex + 1);
                                            } else {
                                                deletePolygon(_CurrentPolygon, _CurrentIndex);
                                                lockPolygon(true);
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
            case R.id.polyMenuReset: {
                resetPolygon();
                break;
            }
            case R.id.polyMenuAdjust: {
                PolygonAdjuster.adjust(getTtAppCtx().getDAL());
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private TtPolygon getPolyAtIndex(int index) {
        if (index > -1 && index < getPolygons().size()) {
            return new TtPolygon(getPolygons().get(index));
        }

        return null;
    }

    @Override
    protected void onAdjusterStopped(PolygonAdjuster.AdjustResult result) {
        super.onAdjusterStopped(result);

        if (result == PolygonAdjuster.AdjustResult.SUCCESSFUL) {
            final int index = _CurrentIndex;

            _Polygons = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSectionsPagerAdapter.notifyDataSetChanged();
                    moveToPolygon(index);
                }
            });

            for (TtPolygon poly : getPolygons())
                onPolygonPointsUpdated(poly);
        }
    }

    //region Save Delete Create Reset
    private void savePolygon() {
        if (_PolygonUpdated && _CurrentPolygon != null) {
            getTtAppCtx().getDAL().updatePolygon(_CurrentPolygon);
            getPolygons().set(_CurrentIndex, _CurrentPolygon);
            _PolygonUpdated = false;
        }
    }

    private void deleteWithoutMoving() {
        if (deletePolygon(_deletePolygon, _deleteIndex)) {
            if (_deleteIndex < _CurrentIndex)
                _CurrentIndex--;

            if (_deleteIndex > 0) {
                moveToPolygon(_CurrentIndex);
            } else {
                moveToPolygon(_CurrentIndex, false);
            }

            _deleteIndex = INVALID_INDEX;
            _deletePolygon = null;

            lockPolygon(true);
        } else {
            Toast.makeText(this, "Error deleting polygon.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deletePolygon(TtPolygon polygon, int index) {
        try {
            if (polygon != null) {
                if (getTtAppCtx().getDAL().deletePointsInPolygon(polygon.getCN())) {
                    getTtAppCtx().getDAL().deletePolygon(polygon.getCN());
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    return false;
                }

                if (index > INVALID_INDEX) {
                    getPolygons().remove(index);
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    _deleteIndex = INVALID_INDEX;
                    _deletePolygon = null;
                }

                updateButtons();

                setPolygonUpdated(false);

                adjust = true;
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PolygonsActivity:deletePolygon");
            return false;
        }

        return true;
    }

    private void createPolygon() {
        savePolygon();

        int polyCount = getTtAppCtx().getDAL().getItemCount(TwoTrailsSchema.PolygonSchema.TableName);

        TtPolygon newPolygon = new TtPolygon(polyCount * 1000 + 1010);
        newPolygon.setName(String.format("Poly %d", polyCount + 1));
        newPolygon.setAccuracy(Consts.Default_Point_Accuracy);
        getTtAppCtx().getDAL().insertPolygon(newPolygon);

        addedPoly = newPolygon.getCN();

        getPolygons().add(newPolygon);
        mSectionsPagerAdapter.notifyDataSetChanged();

        moveToPolygon(getPolygons().size() - 1);

        updateButtons();

        lockPolygon(false);
    }

    private void resetPolygon() {
        if (_PolygonUpdated) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setTitle(String.format("Reset Polygon %s", _CurrentPolygon.getName()));
            dialog.setMessage(getString(R.string.poly_reset_diag));

            dialog.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    _CurrentPolygon = getPolyAtIndex(_CurrentIndex);
                    onPolygonUpdated();
                    updateButtons();
                    setPolygonUpdated(false);
                    lockPolygon(false);
                }
            });

            dialog.setNeutralButton(R.string.str_cancel, null);

            dialog.show();
        }
    }
    //endregion

    //region Get Move
    private void moveToPolygon(int index) {
        moveToPolygon(index, true);
    }

    private void moveToPolygon(int index, boolean smoothScroll) {
        if (index != _CurrentIndex) {
            if (index > INVALID_INDEX && index < getPolygons().size()) {
                mViewPager.setCurrentItem(index, smoothScroll);
                _CurrentPolygon = getPolygonAtIndex(index);
                _CurrentIndex = index;
            } else {
                _CurrentPolygon = null;
                _CurrentIndex = INVALID_INDEX;
            }
        }

        updateButtons();
    }

    public TtPolygon getPolygonAtIndex(int index) {
        TtPolygon poly = null;

        if (index > INVALID_INDEX && index < getPolygons().size()) {
            poly = new TtPolygon(getPolygons().get(index));
        }

        return poly;
    }
    //endregion

    //region Update UI
    private void updateButtons() {
        if (menuCreated) {
            if (getPolygons().size() > 0)
                AndroidUtils.UI.enableMenuItem(miAdjust);
            else
                AndroidUtils.UI.disableMenuItem(miAdjust);
        }

        lockPolygon(true);
    }

    private void lockPolygon(boolean lockPoly) {
        if (lockPoly) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed_white_36dp);

                AndroidUtils.UI.disableMenuItem(miReset);
                AndroidUtils.UI.disableMenuItem(miDelete);
            }

            _PolyLocked = true;
            onLockChange();
        } else if (getPolygons().size() > 0) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_lock);
                miLock.setIcon(R.drawable.ic_action_lock_open_white_36dp);

                AndroidUtils.UI.enableMenuItem(miDelete);

                if (_PolygonUpdated) {
                    AndroidUtils.UI.enableMenuItem(miReset);
                } else {
                    AndroidUtils.UI.disableMenuItem(miReset);
                }
            }

            _PolyLocked = false;
            onLockChange();
        }
    }

    private void setPolygonUpdated(boolean updated) {
        _PolygonUpdated = updated;

        if (menuCreated) {
            if (_PolygonUpdated) {
                AndroidUtils.UI.enableMenuItem(miReset);
            } else {
                AndroidUtils.UI.disableMenuItem(miReset);
            }
        }
    }

    private void onPolygonsUpdated() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSectionsPagerAdapter.notifyDataSetChanged();
                }
            });
        } else {
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }
    //endregion

    //Fragment Calls
    private void onLockChange() {
        for (Listener listener : listeners.values()) {
            listener.onLockChange(_PolyLocked);
        }
    }

    private void onPolygonUpdated() {
        _PolygonUpdated = true;

        if (!_PolyLocked) {
            miReset.setEnabled(true);
        }

        if (listeners.containsKey(_CurrentPolygon.getCN())) {
            Listener listener = listeners.get(_CurrentPolygon.getCN());
            if (listener != null)
                listener.onPolygonPointsUpdated(_CurrentPolygon);
            else
                getTtAppCtx().getReport().writeError("Null Listener", "PolygonsActivity:onPolygonUpdate");
        }
    }

    private void onPolygonUpdated(TtPolygon poly) {
        if (listeners.containsKey(poly.getCN())) {
            Listener listener = listeners.get(poly.getCN());
            if (listener != null)
                listener.onPolygonPointsUpdated(poly);
            else
                getTtAppCtx().getReport().writeError("Null Listener", "PolygonsActivity:onPolygonUpdate(cn)");
        }
    }

    private void onPolygonPointsUpdated(TtPolygon poly) {
        if (listeners.containsKey(poly.getCN())) {
            Listener listener = listeners.get(poly.getCN());
            if (listener != null)
                listener.onPolygonPointsUpdated(poly);
            else
                getTtAppCtx().getReport().writeError("Null Listener", "PolygonsActivity:onPolygonPointsUpdated");
        }
    }

    public void updatePolygon(TtPolygon polygon) {
        //only update if current poly
        if (_CurrentPolygon.getCN().equals(polygon.getCN())) {
            if (_CurrentPolygon.getAccuracy() != polygon.getAccuracy()) {
                adjust = true;
                AndroidUtils.UI.enableMenuItem(miAdjust);
            }

            _CurrentPolygon = polygon;

            setPolygonUpdated(true);
        }
    }

    public TtPolygon getPolygon(String cn) {
        for (TtPolygon polygon : getPolygons()) {
            if (polygon.getCN().equals(cn)) {
                return new TtPolygon(polygon);
            }
        }

        getTtAppCtx().getReport().writeError("Polygon '" + cn + "' Not Found", "PolygonActivity:getPolygon(cn)");

        return null;
    }
    
    public ArrayList<PointD> getDrawPoints(final TtPolygon poly, final int width, boolean reset) {
        boolean hdpk = hasDrawPoints.containsKey(poly.getCN());
        Tuple<Integer, ArrayList<PointD>> dpk = hdpk ? drawPoints.get(poly.getCN()) : null;

        if (!reset && hdpk && dpk != null && (dpk.Item1 == width || width == 0)) {
            return dpk.Item2;
        } else if (!hdpk || reset) {
            if (width > 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (getTtAppCtx().getDAL().getBoundaryPointsCountInPoly(poly.getCN()) > 2) {
                            final List<TtPoint> points = getTtAppCtx().getDAL().getBoundaryPointsInPoly(poly.getCN());

                            if (points != null && points.size() > 2) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        HashMap<String, TtMetadata> metadata = getTtAppCtx().getDAL().getMetadataMap();
                                        TtMetadata defMeta = metadata.get(Consts.EmptyGuid);
                                        if (defMeta != null) {
                                            drawPoints.put(poly.getCN(), new Tuple<>(width, TtUtils.UI.generateStaticPolyPoints(points, metadata, defMeta.getZone(), (int)(width * 0.9))));
                                            hasDrawPoints.put(poly.getCN(), true);

                                            onPolygonPointsUpdated(poly);
                                        } else {
                                            getTtAppCtx().getReport().writeError("No default Metadata", "PolygonsActivity:getDrawPoints");
                                        }
                                    }
                                }).start();
                            }
                        } else {
                            hasDrawPoints.put(poly.getCN(), false);
                        }
                    }
                }).start();
            }
        }

        return null;
    }
    //endregion


    //region Controls
    public void btnNewClick(View view) {
        createPolygon();
    }
    //endregion

    public void register(String polyCN, Listener listener) {
        if (listener != null && !listeners.containsKey(polyCN)) {
            listeners.put(polyCN, listener);
        }
    }

    public void unregister(String polyCN) {
        listeners.remove(polyCN);
    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapterEx {

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            String polyCN = getPolygons().get(position).getCN();

            boolean addCard = !StringEx.isEmpty(addedPoly) && addedPoly.equals(polyCN);

            if (addCard) {
                addedPoly = null;
            }

            return PolygonFragment.newInstance(polyCN, addCard);
        }

        @Override
        public int getCount() {
            return getPolygons().size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                return getPolygons().get(position).getName();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
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
        void onPolygonUpdated(TtPolygon polygon);
        void onPolygonPointsUpdated(TtPolygon polygon);
    }
}
