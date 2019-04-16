package com.usda.fmsc.twotrails.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.EnumSelectionDialog;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.android.dialogs.NumericInputDialog;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.dialogs.EditableListDialog;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.metadata.MetadataFragment;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.Datum;
import com.usda.fmsc.twotrails.units.DeclinationType;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.utilities.StringEx;

public class MetadataActivity extends CustomToolbarActivity {
    private HashMap<String, Listener> listeners;

    private GpsService.Listener listener;

    private MenuItem miLock, miReset, miDelete;

    private List<TtMetadata> _Metadata;
    private TtMetadata _CurrentMetadata, _deleteMeta;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _MetadataUpdated, adjust, _MetaLocked = true, menuCreated = false, gotNmea;
    private boolean ignoreMetaChange;
    private String addedMeta = null;

    private List<TtMetadata> getMetadata() {
        if (_Metadata == null || _Metadata.size() < 1) {
            _Metadata = getTtAppCtx().getDAL().getMetadata();
            if (mSectionsPagerAdapter != null) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mSectionsPagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        return _Metadata;
    }

    private boolean isMetadataUpdated() {
        return _MetadataUpdated;
    }

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private ComplexOnPageChangeListener onPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignoreMetaChange) {
                saveMetadata();

                _CurrentIndex = position;
                _CurrentMetadata = getMetaAtIndex(_CurrentIndex);
                updateButtons();
            }

            ignoreMetaChange = false;

            AndroidUtils.UI.hideKeyboard(MetadataActivity.this);
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

        setContentView(R.layout.activity_metadata);

        listeners = new HashMap<>();

        if (getMetadata().size() > 0) {
            _CurrentIndex = 0;
            _CurrentMetadata = getMetaAtIndex(_CurrentIndex);
        } else {
            getTtAppCtx().getReport().writeError("No Metadata, Invalid Project File.", "MetadataActivity");
            Toast.makeText(MetadataActivity.this, "Metadata not found, Invalid Project File.", Toast.LENGTH_LONG).show();
            finish();
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.saveFragmentStates(false);

        mViewPager = findViewById(R.id.metaViewPager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.addOnPageChangeListener(onPageChangeListener);
        }

        lockMetadata(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMetadata();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveMetadata();

        if (adjust) {
            PolygonAdjuster.adjust(getTtAppCtx().getDAL(), true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_metadata, menu);
        miLock = menu.findItem(R.id.metaMenuLock);
        miReset = menu.findItem(R.id.metaMenuReset);
        miDelete = menu.findItem(R.id.metaMenuDelete);

        menuCreated = true;
        updateButtons();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.metaMenuLock: {
                lockMetadata(!_MetaLocked);
                break;
            }
            case R.id.metaMenuDelete: {
                if (!_MetaLocked && _CurrentIndex > 0) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle(String.format("Delete Metadata %s", _CurrentMetadata.getName()));

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
                                                _deleteMeta = _CurrentMetadata;

                                                _CurrentIndex--;
                                                ignoreMetaChange = true;

                                                moveToMetadata(_CurrentIndex);
                                            } else if (getMetadata().size() > 1) {
                                                _deleteIndex = _CurrentIndex;
                                                _deleteMeta = _CurrentMetadata;

                                                _CurrentIndex++;
                                                ignoreMetaChange = true;
                                                moveToMetadata(_CurrentIndex);
                                            } else {
                                                deleteMetadata(_deleteMeta, _CurrentIndex);
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
            case R.id.metaMenuReset: {
                resetMetadata();
                break;
            }
            case R.id.metaMenuDefault: {
                getTtAppCtx().getMetadataSettings().setDefaultMetadata(_CurrentMetadata);
                Toast.makeText(this, String.format("%s saved as default.", _CurrentMetadata.getName()), Toast.LENGTH_SHORT).show();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //region Save Delete Create Reset
    private void saveMetadata() {
        if (isMetadataUpdated() && _CurrentMetadata != null) {
            getTtAppCtx().getDAL().updateMetadata(_CurrentMetadata);
            getMetadata().set(_CurrentIndex, _CurrentMetadata);
            setMetadataUpdated(false, false);
        }
    }

    private void deleteWithoutMoving() {
        if (deleteMetadata(_deleteMeta, _deleteIndex)) {
            if (_deleteIndex < _CurrentIndex)
                _CurrentIndex--;

            if (_deleteIndex > 0) {
                moveToMetadata(_CurrentIndex);
            } else {
                moveToMetadata(_CurrentIndex, false);
            }

            _deleteIndex = INVALID_INDEX;
            _deleteMeta = null;

            lockMetadata(true);
        } else {
            Toast.makeText(this, "Error deleting metadata.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteMetadata(TtMetadata metadata, int index) {
        try {
            if (metadata != null) {
                if (getTtAppCtx().getDAL().deleteMetadataSafe(metadata.getCN())) {
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    return false;
                }

                if (index > INVALID_INDEX) {
                    getMetadata().remove(index);
                    mSectionsPagerAdapter.notifyDataSetChanged();
                } else {
                    _deleteIndex = INVALID_INDEX;
                    _deleteMeta = null;
                }

                setMetadataUpdated(false, false);

                adjust = true;
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PolygonsActivity:deletePolygon");
            return false;
        }

        return true;
    }

    private void createMetadata() {
        saveMetadata();

        int metaCount = getTtAppCtx().getDAL().getItemCount(TwoTrailsSchema.MetadataSchema.TableName);

        TtMetadata newMetadata = getTtAppCtx().getMetadataSettings().getDefaultMetadata();
        newMetadata.setCN(java.util.UUID.randomUUID().toString());
        newMetadata.setName(StringEx.format("Meta %d", metaCount + 1));
        getTtAppCtx().getDAL().insertMetadata(newMetadata);

        addedMeta = newMetadata.getCN();

        getMetadata().add(newMetadata);
        mSectionsPagerAdapter.notifyDataSetChanged();

        moveToMetadata(getMetadata().size() - 1);

        lockMetadata(false);
    }

    private void resetMetadata() {
        if (isMetadataUpdated()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setTitle(String.format("Reset Metadata %s", _CurrentMetadata.getName()));
            dialog.setMessage(getString(R.string.meta_reset_diag));

            dialog.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    _CurrentMetadata = getMetaAtIndex(_CurrentIndex);
                    onMetadataUpdate();
                    updateButtons();
                    setMetadataUpdated(false, true);
                    lockMetadata(false);
                }
            });

            dialog.setNeutralButton(R.string.str_cancel, null);

            dialog.show();
        }
    }
    //endregion


    //region Get Move
    private void moveToMetadata(int index) {
        moveToMetadata(index, true);
    }

    private void moveToMetadata(int index, boolean smoothScroll) {
        if (index > INVALID_INDEX && index < getMetadata().size()) {
            mViewPager.setCurrentItem(index, smoothScroll);
            _CurrentMetadata = getMetaAtIndex(index);
            _CurrentIndex = index;
        } else {
            _CurrentMetadata = null;
            _CurrentIndex = INVALID_INDEX;
        }

        updateButtons();
    }

    private TtMetadata getMetaAtIndex(int index) {
        if (index > INVALID_INDEX && index < getMetadata().size()) {
            return new TtMetadata(getMetadata().get(index));
        }

        return null;
    }
    //endregion

    //region Update UI

    private void updateButtons() {
        lockMetadata(true);
    }


    private void lockMetadata(boolean lockMeta) {
        if (lockMeta) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed_white_36dp);

                AndroidUtils.UI.disableMenuItem(miReset);
                AndroidUtils.UI.disableMenuItem(miDelete);
            }

            _MetaLocked = true;
            onLockChange();
        } else if (getMetadata().size() > 0) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_lock);
                miLock.setIcon(R.drawable.ic_action_lock_open_white_36dp);

                if (_CurrentIndex > 0) {
                    AndroidUtils.UI.enableMenuItem(miDelete);
                } else {
                    AndroidUtils.UI.disableMenuItem(miDelete);
                }

                if (isMetadataUpdated()) {
                    AndroidUtils.UI.enableMenuItem(miReset);
                } else {
                    AndroidUtils.UI.disableMenuItem(miReset);
                }
            }

            _MetaLocked = false;
            onLockChange();
        }
    }

    private void setMetadataUpdated(boolean updated, boolean updateUI) {
        _MetadataUpdated = updated;

        if (menuCreated && updateUI) {
            if (_MetadataUpdated && !_MetaLocked) {
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
            listener.onLockChange(_MetaLocked);
        }
    }

    private void onMetadataUpdate() {
        setMetadataUpdated(true, true);

        if (listeners.containsKey(_CurrentMetadata.getCN())) {
            Listener listener = listeners.get(_CurrentMetadata.getCN());
            if (listener != null)
                listener.onMetadataUpdated(_CurrentMetadata);
            else
                getTtAppCtx().getReport().writeError("Null Listener", "MetadataActivity:onMetadataUpdate");
        }
    }

    public TtMetadata getMetadata(String cn) {
        for (TtMetadata metadata : getMetadata()) {
            if (metadata.getCN().equals(cn)) {
                return metadata;
            }
        }

        getTtAppCtx().getReport().writeError("Metadata '" + cn + "' Not Found", "MetadataActivity:getMetadata(cn)");

        return null;
    }
    //endregion


    //region Controls
    public void btnNewClick(View view) {
        createMetadata();
    }

    //region MetaFrag
    public void btnNameClick(View view) {
        if (!_MetaLocked) {
            final InputDialog inputDialog = new InputDialog(this);

            inputDialog.setTitle("Name");
            inputDialog.setInputText(_CurrentMetadata.getName());

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = inputDialog.getText();
                    _CurrentMetadata.setName(name);
                    onMetadataUpdate();
                }
            });

            inputDialog.setNeutralButton(R.string.str_cancel, null);

            inputDialog.show();
        }
    }

    public void btnZoneClick(View view) {
        if (!_MetaLocked) {
            final Activity mContext = this;

            final NumericInputDialog inputDialog = new NumericInputDialog(mContext);

            inputDialog.setTitle(R.string.str_zone);
            inputDialog.setInputText(StringEx.toString(_CurrentMetadata.getZone()));

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Integer zone = inputDialog.getInt();

                    if (zone != null && zone >= 0) {
                        final ProgressDialog progressDialog = new ProgressDialog(MetadataActivity.this);
                        progressDialog.setMessage("Recalculating Positions");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<TtPoint> points = getTtAppCtx().getDAL().getGpsTypePointsWithMeta(_CurrentMetadata.getCN());
                                progressDialog.setMax(points.size() * 2);

                                if (points.size() > 0) {
                                    for (int i = 0; i < points.size(); i++) {
                                        points.set(i, TtUtils.Points.reCalculateGps(points.get(i), zone, getTtAppCtx().getDAL(), null));

                                        progressDialog.setProgress(i);
                                    }

                                    getTtAppCtx().getDAL().updatePoints(points);

                                    progressDialog.setProgress(points.size() * 2);
                                }

                                progressDialog.dismiss();
                            }
                        }).start();

                        _CurrentMetadata.setZone(zone);
                        onMetadataUpdate();
                        adjust = true;
                    } else {
                        Toast.makeText(mContext, R.string.str_invalid, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //inputDialog.setNeutralButton("Get Zone", null);

            inputDialog.setNegativeButton(R.string.str_cancel, null);

            final AlertDialog ndialog = inputDialog.create();

            //override after onshow so the get zone btn doesn't close the dialog
            ndialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    InputMethodManager imm = (InputMethodManager) inputDialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(inputDialog.getInput(), InputMethodManager.SHOW_IMPLICIT);
                    inputDialog.getInput().setSelection(inputDialog.getInput().length());

                    ndialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                                gotNmea = false;

                                getTtAppCtx().getGps().startGps();

                                listener = new GpsService.Listener() {
                                    @Override
                                    public void nmeaBurstReceived(INmeaBurst nmeaBurst) {
                                        if (!getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                                            getTtAppCtx().getGps().stopGps();
                                        }

                                        inputDialog.getInput().setText(StringEx.toString(nmeaBurst.getTrueUTM().getZone()));

                                        gotNmea = true;

                                        if (listener != null) {
                                            getTtAppCtx().getGps().addListener(listener);
                                        }
                                    }

                                    @Override
                                    public void nmeaStringReceived(String nmeaString) {

                                    }

                                    @Override
                                    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {
                                        //
                                    }

                                    @Override
                                    public void nmeaBurstValidityChanged(boolean burstsValid) { }

                                    @Override
                                    public void gpsStarted() {

                                    }

                                    @Override
                                    public void gpsStopped() {

                                    }

                                    @Override
                                    public void gpsServiceStarted() {

                                    }

                                    @Override
                                    public void gpsServiceStopped() {

                                    }

                                    @Override
                                    public void gpsError(GpsService.GpsError error) {
                                        if (!getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                                            getTtAppCtx().getGps().stopGps();
                                        }

                                        if (listener != null) {
                                            getTtAppCtx().getGps().addListener(listener);
                                        }
                                    }
                                };

                                getTtAppCtx().getGps().addListener(listener);

                                final Handler mHandler = new Handler();

                                final Runnable notifyIfFail = new Runnable() {
                                    public void run() {
                                        try {
                                            Thread.sleep(10000);

                                            if (!getTtAppCtx().getDeviceSettings().isGpsAlwaysOn()) {
                                                getTtAppCtx().getGps().stopGps();
                                            }

                                            if (!gotNmea) {
                                                Toast.makeText(mContext, "GPS timed out.", Toast.LENGTH_SHORT).show();
                                            }

                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                new Thread() {
                                    public void run() {
                                        mHandler.post(notifyIfFail);
                                    }
                                }.start();
                            } else {
                                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

                                dialog.setTitle("GPS not Configured");
                                dialog.setMessage("Would you like to configure the GPS now?");

                                dialog.setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startActivity(new Intent(mContext, SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE));
                                    }
                                });

                                dialog.setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        ndialog.show();
                                    }
                                });
                                dialog.show();
                            }
                        }
                    });
                }
            });

            ndialog.show();
        }
    }

    public void btnDecClick(View view) {
        if (!_MetaLocked) {
            final NumericInputDialog inputDialog = new NumericInputDialog(this);

            inputDialog.setTitle(R.string.str_mag_dec);
            inputDialog.setInputText(StringEx.toString(_CurrentMetadata.getMagDec()));

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Double acc = inputDialog.getDouble();

                    if (acc != null) {
                        _CurrentMetadata.setMagDec(acc);
                        onMetadataUpdate();
                        adjust = true;
                    } else {
                        Toast.makeText(getBaseContext(), R.string.str_invalid, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            inputDialog.setNeutralButton(R.string.str_cancel, null);

            inputDialog.show();
        }
    }

    public void btnDecTypeClick(View view) {
        if (!_MetaLocked) {
            final EnumSelectionDialog<DeclinationType> edialog =
                    new EnumSelectionDialog<>(this, DeclinationType.class);

            edialog.setTitle(R.string.meta_dec_type);

            edialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setDecType(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });

            edialog.show();
        }
    }

    public void btnDatumClick(View view) {
        if (!_MetaLocked) {
            final EnumSelectionDialog<Datum> edialog =
                    new EnumSelectionDialog<>(this, Datum.class);

            edialog.setTitle(R.string.meta_datum);

            edialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setDatum(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });

            edialog.show();
        }
    }

    public void btnDistClick(View view) {
        if (!_MetaLocked) {
            final EnumSelectionDialog<Dist> edialog =
                    new EnumSelectionDialog<>(this, Dist.values());

            edialog.setTitle(R.string.str_dist);

            edialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setDistance(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });

            edialog.show();
        }
    }

    public void btnElevClick(View view) {
        if (!_MetaLocked) {
            final EnumSelectionDialog<UomElevation> edialog =
                    new EnumSelectionDialog<>(this, UomElevation.class);

            edialog.setTitle(R.string.str_elev);

            edialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setElevation(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });

            edialog.show();
        }
    }

    public void btnSlopeClick(View view) {
        if (!_MetaLocked) {
            final EnumSelectionDialog<Slope> edialog =
                    new EnumSelectionDialog<>(this, Slope.class);

            edialog.setTitle(R.string.meta_slope);

            edialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setSlope(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });

            edialog.show();
        }
    }

    public void btnGpsRecClick(View view) {
        if (!_MetaLocked) {
            final EditableListDialog edialog = new EditableListDialog();
            edialog.setItems(Consts.DeviceNames.GPS_RECEIVERS);
            edialog.setDefaultValue(_CurrentMetadata.getGpsReceiver());

            edialog.setTitle(getString(R.string.meta_gps_rec));
            edialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setGpsReceiver(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });
            edialog.setNegativeButton("Cancel", null);

            edialog.show(getSupportFragmentManager(), "GPS_RECEIVERS");
        }
    }

    public void btnRangeFinderClick(View view) {
        if (!_MetaLocked) {
            final EditableListDialog edialog = new EditableListDialog();
            edialog.setItems(Consts.DeviceNames.RANGE_FINDERS);
            edialog.setDefaultValue(_CurrentMetadata.getRangeFinder());

            edialog.setTitle(getString(R.string.meta_range_finder));
            edialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    _CurrentMetadata.setRangeFinder(edialog.getSelectedItem());
                    onMetadataUpdate();
                }
            });
            edialog.setNegativeButton("Cancel", null);

            edialog.show(getSupportFragmentManager(), "RANGE_FINDERS");
        }
    }

    public void btnCompassClick(View view) {
        if (!_MetaLocked) {
            final InputDialog inputDialog = new InputDialog(this);

            inputDialog.setTitle(R.string.meta_compass);
            inputDialog.setInputText(_CurrentMetadata.getCompass());

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String compass = inputDialog.getText();
                    _CurrentMetadata.setCompass(compass);
                    onMetadataUpdate();
                }
            });

            inputDialog.setNeutralButton(R.string.str_cancel, null);

            inputDialog.show();
        }
    }

    public void btnCrewClick(View view) {
        if (!_MetaLocked) {
            final InputDialog inputDialog = new InputDialog(this);

            inputDialog.setTitle(R.string.meta_crew);
            inputDialog.setInputText(_CurrentMetadata.getCrew());

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String crew = inputDialog.getText();
                    _CurrentMetadata.setCrew(crew);
                    onMetadataUpdate();
                }
            });

            inputDialog.setNeutralButton(R.string.str_cancel, null);

            inputDialog.show();
        }
    }

    public void btnCmtClick(View view) {
        if (!_MetaLocked) {
            final InputDialog inputDialog = new InputDialog(this);

            inputDialog.setTitle(R.string.str_cmt);
            inputDialog.setInputText(_CurrentMetadata.getComment());

            inputDialog.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String cmt = inputDialog.getText();
                    _CurrentMetadata.setComment(cmt);
                    onMetadataUpdate();
                }
            });

            inputDialog.setNeutralButton(R.string.str_cancel, null);

            inputDialog.show();
        }
    }
    //endregion
    //endregion


    public void register(String metaCN, Listener listener) {
        if (listener != null && !listeners.containsKey(metaCN)) {
            listeners.put(metaCN, listener);
        }
    }

    public void unregister(String metaCN) {
        listeners.remove(metaCN);
    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapterEx {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            String metaCN = getMetadata().get(position).getCN();

            boolean addCard = !StringEx.isEmpty(addedMeta) && addedMeta.equals(metaCN);

            if (addCard) {
                addedMeta = null;
            }

            return MetadataFragment.newInstance(metaCN, addCard);
        }

        @Override
        public int getCount() {
            return getMetadata().size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                return getMetadata().get(position).getName();
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

        void onMetadataUpdated(TtMetadata metadata);
    }
}
