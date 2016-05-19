package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
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
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.polygon.PolygonFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.utilities.StringEx;

public class PolygonsActivity extends CustomToolbarActivity {
    private HashMap<String, Listener> listeners;

    private MenuItem miLock, miReset, miDelete, miAdjust;

    private List<TtPolygon> _Polygons;
    private TtPolygon _CurrentPolygon, _deletePolygon;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _PolygonUpdated, adjust, _PolyLocked = true, menuCreated = false;
    private boolean ignorePolyChange;
    private String addedPoly = null;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private ComplexOnPageChangeListener onPageChangeListener = new ComplexOnPageChangeListener() {
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

        _Polygons = Global.getDAL().getPolygons();
        if (_Polygons.size() > 0) {
            _CurrentIndex = 0;
            _CurrentPolygon = getPolyAtIndex(_CurrentIndex);
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.saveFragmentStates(false);

        mViewPager = (ViewPager) findViewById(R.id.polysViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);

            mViewPager.addOnPageChangeListener(onPageChangeListener);
        }

        lockPolygon(true);
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
            PolygonAdjuster.adjust(Global.getDAL(), Global.getMainActivity(), true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_polygons, menu);
        miLock = menu.findItem(R.id.polyMenuLock);
        miReset = menu.findItem(R.id.polyMenuReset);
        miDelete = menu.findItem(R.id.polyMenuDelete);
        miAdjust = menu.findItem(R.id.polyMenuAdjust);

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

                                                _CurrentIndex--;
                                                ignorePolyChange = true;

                                                moveToPolygon(_CurrentIndex);
                                            } else if (_Polygons.size() > 1) {
                                                _deleteIndex = _CurrentIndex;
                                                _deletePolygon = _CurrentPolygon;

                                                _CurrentIndex++;
                                                ignorePolyChange = true;
                                                moveToPolygon(_CurrentIndex);
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
                Toast.makeText(this, "Unimplemented", Toast.LENGTH_SHORT).show();
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
        if (index > -1 && index < _Polygons.size()) {
            return new TtPolygon(_Polygons.get(index));
        }

        return null;
    }


    //region Save Delete Create Reset
    private void savePolygon() {
        if (_PolygonUpdated && _CurrentPolygon != null) {
            Global.getDAL().updatePolygon(_CurrentPolygon);
            _Polygons.set(_CurrentIndex, _CurrentPolygon);
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
                if (Global.getDAL().deletePointsInPolygon(polygon.getCN())) {
                    Global.getDAL().deletePolygon(polygon.getCN());
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    return false;
                }

                if (index > INVALID_INDEX) {
                    _Polygons.remove(index);
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    _deleteIndex = INVALID_INDEX;
                    _deletePolygon = null;
                }

                setPolygonUpdated(false);

                adjust = true;
            }
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "PolygonsActivity:deletePolygon");
            return false;
        }

        return true;
    }

    private void createPolygon() {
        savePolygon();

        int polyCount = Global.getDAL().getItemCount(TwoTrailsSchema.PolygonSchema.TableName);

        TtPolygon newPolygon = new TtPolygon(polyCount * 1000 + 1010);
        newPolygon.setName(String.format("Poly %d", polyCount + 1));
        newPolygon.setAccuracy(Consts.Default_Point_Accuracy);
        Global.getDAL().insertPolygon(newPolygon);

        addedPoly = newPolygon.getCN();

        _Polygons.add(newPolygon);
        mSectionsPagerAdapter.notifyDataSetChanged();

        moveToPolygon(_Polygons.size() - 1);

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
                    onPolygonUpdate();
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
        if (index > INVALID_INDEX && index < _Polygons.size()) {
            mViewPager.setCurrentItem(index, smoothScroll);
            _CurrentPolygon = getPolygonAtIndex(index);
            _CurrentIndex = index;
        } else {
            _CurrentPolygon = null;
            _CurrentIndex = INVALID_INDEX;
        }

        updateButtons();
    }

    public TtPolygon getPolygonAtIndex(int index) {
        TtPolygon poly = null;

        if (index > INVALID_INDEX && index < _Polygons.size()) {
            poly = new TtPolygon(_Polygons.get(index));
        }

        return poly;
    }
    //endregion

    //region Update UI
    private void updateButtons() {
        /*
        if (_CurrentPolygon != null) {

        }
        */

        lockPolygon(true);
    }

    private void lockPolygon(boolean lockPoly) {
        if (lockPoly) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed);

                AndroidUtils.UI.disableMenuItem(miReset);
                AndroidUtils.UI.disableMenuItem(miDelete);
            }

            _PolyLocked = true;
            onLockChange();
        } else if (_Polygons.size() > 0) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_lock);
                miLock.setIcon(R.drawable.ic_action_lock_open);

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
    //endregion

    //Fragment Calls
    private void onLockChange() {
        for (Listener listener : listeners.values()) {
            listener.onLockChange(_PolyLocked);
        }
    }

    private void onPolygonUpdate() {
        _PolygonUpdated = true;

        if (!_PolyLocked) {
            miReset.setEnabled(true);
        }

        if (listeners.containsKey(_CurrentPolygon.getCN())) {
            listeners.get(_CurrentPolygon.getCN()).onPolygonUpdated(_CurrentPolygon);
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
        for (TtPolygon polygon : _Polygons) {
            if (polygon.getCN().equals(cn)) {
                return new TtPolygon(polygon);
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
        if (listeners.containsKey(polyCN)) {
            listeners.remove(polyCN);
        }
    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapterEx {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            String polyCN = _Polygons.get(position).getCN();

            boolean addCard = !StringEx.isEmpty(addedPoly) && addedPoly.equals(polyCN);

            if (addCard) {
                addedPoly = null;
            }

            return PolygonFragment.newInstance(polyCN, addCard);
        }

        @Override
        public int getCount() {
            return _Polygons.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                return _Polygons.get(position).getName();
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
    }
}
