package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.twotrails.activities.custom.AcquireGpsMapActivity;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.adapters.Take5PointsEditRvAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.SideShotPoint;
import com.usda.fmsc.twotrails.objects.Take5Point;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.GeoTools;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class Take5Activity extends AcquireGpsMapActivity {
    private RecyclerViewEx rvPoints;
    private Take5PointsEditRvAdapter t5pAdapter;
    private LinearLayoutManagerWithSmoothScroller linearLayoutManager;
    private FloatingActionButton fabT5, fabSS, fabCancel;
    private LinearLayout lay;

    RelativeLayout progLay;
    MaterialProgressBar prog;
    TextView tvProg;

    private ArrayList<TtPoint> _Points;
    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private TtPoint _PrevPoint, _CurrentPoint;
    private Take5Point _AddTake5;
    private TtMetadata _Metadata;
    private TtPolygon _Polygon;
    private TtGroup _Group;

    private int increment, takeAmount, nmeaCount = 0;
    private boolean saved = true, updated, onBnd = true, cancelVisible, ignoreScroll, useRing, useVib;

    private FilterOptions options;


    private AlphaAnimation anim = new AlphaAnimation(1f, .03f);

    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        boolean invisible = false, handling;

        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (!ignoreScroll && !handling) {
                    handling = true;

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                anim.cancel();
                                lay.clearAnimation();

                                anim = new AlphaAnimation(.3f, 1f);
                                anim.setDuration(250);
                                anim.setFillEnabled(true);
                                anim.setFillAfter(true);

                                anim.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        lay.setAlpha(1f);
                                        lay.clearAnimation();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });

                                while (handling) {
                                    Thread.sleep(350);

                                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                                        lay.startAnimation(anim);
                                        invisible = false;
                                        handling = false;
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                ignoreScroll = false;
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING && !ignoreScroll && !invisible) {
                anim.cancel();

                anim = new AlphaAnimation(lay.getAlpha(), .3f);
                anim.setDuration(250);
                anim.setFillEnabled(true);
                anim.setFillAfter(true);

                lay.startAnimation(anim);

                invisible = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (invisible) {
                            try {
                                Thread.sleep(100);

                                int pos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();

                                if (pos < 0) {
                                    pos = linearLayoutManager.findFirstVisibleItemPosition();
                                }

                                if (pos > -1 && pos < _Points.size()) {
                                    moveToMapPoint(pos);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }
    };



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take5);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            //actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setUseLostConnectionWarning(true);


        if (!isCanceling()) {
            SheetLayoutEx.enterFromBottomAnimation(this);
            _CurrentPoint = null;

            _Points = new ArrayList<>();
            _Bursts = new ArrayList<>();
            _UsedBursts = new ArrayList<>();
            int cancelResult = 0;

            Intent intent = getIntent();
            if (intent != null && intent.getExtras() != null) {
                _Bursts = new ArrayList<>();

                try {
                    if (intent.getExtras().containsKey(Consts.Activities.Data.POINT_DATA)) {
                        _CurrentPoint = (TtPoint) intent.getSerializableExtra(Consts.Activities.Data.POINT_DATA);
                        onBnd = _CurrentPoint.isOnBnd();
                    }

                    _Metadata = (TtMetadata)intent.getSerializableExtra(Consts.Activities.Data.METADATA_DATA);
                    _Polygon = (TtPolygon)intent.getSerializableExtra(Consts.Activities.Data.POLYGON_DATA);

                    if (_Metadata == null) {
                        cancelResult = Consts.Activities.Results.NO_METDATA_DATA;
                    } else {
                        setZone(_Metadata.getZone());

                        if (_Polygon == null) {
                            cancelResult = Consts.Activities.Results.NO_POLYGON_DATA;
                        }
                    }


                } catch (Exception e) {
                    cancelResult = Consts.Activities.Results.ERROR;
                    e.printStackTrace();
                }
            } else {
                cancelResult = Consts.Activities.Results.NO_POINT_DATA;
            }

            if (cancelResult != 0) {
                setResult(cancelResult);
                finish();
                return;
            }

            _Group = new TtGroup(TtGroup.GroupType.Take5);
            Global.DAL.insertGroup(_Group);

            fabT5 = (FloatingActionButton)findViewById(R.id.take5FabT5);
            fabSS = (FloatingActionButton)findViewById(R.id.take5FabSideShot);
            fabCancel = (FloatingActionButton)findViewById(R.id.take5FabCancel);


            lay = (LinearLayout)findViewById(R.id.take5LayInfo);

            t5pAdapter = new Take5PointsEditRvAdapter(this, _Points, _Metadata);
            linearLayoutManager = new LinearLayoutManagerWithSmoothScroller(this);

            rvPoints = (RecyclerViewEx)findViewById(R.id.take5RvPoints);
            rvPoints.setLayoutManager(linearLayoutManager);
            rvPoints.setHasFixedSize(true);
            rvPoints.setItemAnimator(new SlideInUpAnimator());
            rvPoints.setAdapter(t5pAdapter);

            rvPoints.addOnScrollListener(scrollListener);

            progLay = (RelativeLayout)findViewById(R.id.progressLayout);
            prog = (MaterialProgressBar)findViewById(R.id.progress);
            tvProg = (TextView)findViewById(R.id.take5ProgressText);

            options = new FilterOptions();
            getSettings();

            setupMap();
        }
    }

    private void getSettings() {
        options.Fix = Global.Settings.DeviceSettings.getTake5FilterFixType();
        options.DopType = Global.Settings.DeviceSettings.getTake5FilterDopType();
        options.DopValue = Global.Settings.DeviceSettings.getTake5FilterDopValue();
        increment = Global.Settings.DeviceSettings.getTake5Increment();
        takeAmount = Global.Settings.DeviceSettings.getTake5NmeaAmount();

        useVib = Global.Settings.DeviceSettings.getTake5VibrateOnCreate();
        useRing = Global.Settings.DeviceSettings.getTake5RingOnCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_take5, menu);

        MenuItem item = menu.findItem(R.id.take5MenuToBottom);
        item.getIcon().setAlpha(178);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {

                if (isLogging()) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setMessage("The you are currently acquiring a point. Do you want to exit anyway?");

                    dialog.setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopLogging();
                            finish();
                        }
                    })
                    .setNeutralButton(R.string.str_cancel, null);

                    dialog.show();
                } else {
                    finish();
                }
                break;
            }
            case R.id.take5MenuToBottom: {
                if (_Points.size() > 0) {
                    ignoreScroll = true;
                    rvPoints.smoothScrollToPosition(_Points.size() - 1);
                    moveToMapPoint(getMarkers().size() - 1);
                }
                break;
            }
            case R.id.take5MenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Activities.SETTINGS);
                break;
            }
            case R.id.take5MenuTake5Settings: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.FILTER_TAKE5_SETTINGS_PAGE),
                        Consts.Activities.SETTINGS);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case Consts.Activities.SETTINGS: {
                Global.getGpsBinder().startGps();

                getSettings();
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

    @Override
    public void finish() {
        if (_Points != null && _Points.size() > 0) {
            setResult(Consts.Activities.Results.POINT_CREATED, new Intent().putExtra(Consts.Activities.Data.NUMBER_OF_CREATED_POINTS, _Points.size()));
        } else {
            if (_Group != null) {
                Global.DAL.deleteGroup(_Group.getCN());
            }

            setResult(RESULT_CANCELED);
        }

        super.finish();
    }


    public void updatePoint(TtPoint point) {
        if (_CurrentPoint == point) {
            updated = true;
            onBnd = point.isOnBnd();
        }
    }

    private boolean savePoint(TtPoint point) {
        if (point != null) {
            if (point == _CurrentPoint) {
                if (!saved) {
                    Global.DAL.insertPoint(point);
                    Global.DAL.insertNmeaBursts(_Bursts);

                    _Bursts = new ArrayList<>();
                    _UsedBursts = new ArrayList<>();
                } else if (updated) {
                    Global.DAL.updatePoint(point);
                }

                saved = true;
                updated = false;
            } else {
                Global.DAL.updatePoint(point);
            }
        }

        return true;
    }


    private void setupTake5() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        _PrevPoint = _CurrentPoint;
        _AddTake5 = new Take5Point();
        setupPoint(_AddTake5);


        _Bursts = new ArrayList<>();
        _UsedBursts = new ArrayList<>();


        //progressBar.setProgress(0);
        startLogging();

        fabT5.setEnabled(false);
        fabSS.setEnabled(false);
        showCancel();
    }

    private void setupSideShot() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        lockLastPoint();

        _PrevPoint = _CurrentPoint;
        _CurrentPoint = new SideShotPoint();
        setupPoint(_CurrentPoint);

        _Points.add(_CurrentPoint);

        ignoreScroll = true;

        t5pAdapter.notifyItemInserted(_Points.size() - 1);
        rvPoints.smoothScrollToPosition(_Points.size() - 1);

        AndroidUtils.UI.hideKeyboard(this);

        showCancel();
    }

    private void setupPoint(TtPoint point) {
        if (_PrevPoint != null) {
            point.setPID(PointNamer.namePoint(_PrevPoint, increment));
            point.setIndex(_PrevPoint.getIndex() + 1);
        } else {
            point.setPID(PointNamer.nameFirstPoint(_Polygon));
            point.setIndex(0);
        }

        point.setPolyCN(_Polygon.getCN());
        point.setPolyName(_Polygon.getName());
        point.setMetadataCN(_Metadata.getCN());
        point.setGroupCN(_Group.getCN());
        point.setGroupName(_Group.getName());
        point.setOnBnd(onBnd);

        saved = false;
        updated = true;
    }


    private boolean validateSideShot() {
        if (_CurrentPoint != null && _CurrentPoint.getOp() == Units.OpType.SideShot) {
            SideShotPoint ssp = (SideShotPoint)_CurrentPoint;

            if (ssp.getFwdAz() != null || ssp.getBkAz() != null) {
                if (ssp.getSlopeDistance() > 0) {

                    //temp adjust for map
                    TtPoint tmp;
                    for (int i = _Points.size() - 2; i > -1; i++) {
                        tmp =_Points.get(i);

                        if (tmp.getOp().isGpsType()) {
                            ssp.calculatePoint(_Polygon, tmp);
                            addMarker(ssp, _Metadata, true);
                            break;
                        }
                    }

                    return true;
                } else {
                    Toast.makeText(this, "SideShot requires a distance of greater than zero", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "SideShot requires a forward or back azimuth", Toast.LENGTH_SHORT).show();
            }

            return false;
        }

        return true;
    }

    private void addTake5(Take5Point point) {
        TtPoint temp = _CurrentPoint;
        _CurrentPoint = point;

        if (savePoint(point)) {
            point.adjustPoint(); //temp for map

            hideCancel();

            lockLastPoint();

            _PrevPoint = _CurrentPoint;
            _CurrentPoint = point;
            _Points.add(point);

            ignoreScroll = true;

            Global.TtNotifyManager.showPointAquired();

            t5pAdapter.notifyItemInserted(_Points.size() - 1);
            rvPoints.smoothScrollToPosition(_Points.size() - 1);

            fabT5.setEnabled(true);
            fabSS.setEnabled(true);

            addMarker(point, _Metadata, true);

            if (useVib) {
                AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_POINT_CREATED);
            }

            if (useRing) {
                AndroidUtils.Device.playSound(this, R.raw.ring);
            }
        } else {
            _CurrentPoint = temp;
            Toast.makeText(this, "Point failed to save", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void startLogging() {
        super.startLogging();

        nmeaCount = 0;

        progLay.setVisibility(View.VISIBLE);
        tvProg.setText("0");
    }

    @Override
    protected void stopLogging() {
        super.stopLogging();

        progLay.setVisibility(View.GONE);
    }


    private void lockLastPoint() {
        if (_Points.size() > 0) {
            Take5PointsEditRvAdapter.PointViewHolderEx holder = (Take5PointsEditRvAdapter.PointViewHolderEx) rvPoints.findViewHolderForAdapterPosition(_Points.size() - 1);

            if (holder != null) {
                holder.setLocked(true);
            }
        }
    }


    private void showCancel() {
        if (!cancelVisible) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_up_in_fast);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabCancel.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabCancel.startAnimation(a);
            cancelVisible = true;
        }
    }

    private void hideCancel() {
        if (cancelVisible) {
            final Animation a = AnimationUtils.loadAnimation(this, R.anim.push_down_out_fast);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabCancel.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabCancel.startAnimation(a);
            cancelVisible = false;
        }
    }


    //region GPS
    @Override
    public void nmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.nmeaBurstReceived(nmeaBurst);

        if (isLogging() && nmeaBurst.isValid()) {
            TtNmeaBurst burst = TtNmeaBurst.create(_AddTake5.getCN(), false, nmeaBurst);

            _Bursts.add(burst);

            if (TtUtils.isUsableNmeaBurst(burst, options)) {
                burst.setUsed(true);
                _UsedBursts.add(burst);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvProg.setText(StringEx.toString(++nmeaCount));
                    }
                });

                if (_UsedBursts.size() == takeAmount) {

                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopLogging();
                        }
                    });

                    ArrayList<GeoPosition> positions = new ArrayList<>();
                    int zone = _Metadata.getZone();
                    double x = 0, y = 0, count = _UsedBursts.size(), dRMSEx = 0, dRMSEy = 0, dRMSEr;

                    TtNmeaBurst tmpBurst;
                    for (int i = 0; i < count; i++) {
                        tmpBurst = _UsedBursts.get(i);
                        x += tmpBurst.getX(zone);
                        y += tmpBurst.getY(zone);
                        positions.add(tmpBurst.getPosition());
                    }

                    x /= count;
                    y /= count;

                    for (int i = 0; i < count; i++) {
                        tmpBurst = _UsedBursts.get(i);
                        dRMSEx += Math.pow(tmpBurst.getX(zone) - x, 2);
                        dRMSEy += Math.pow(tmpBurst.getY(zone) - y, 2);
                    }

                    dRMSEx = Math.sqrt(dRMSEx / count);
                    dRMSEy = Math.sqrt(dRMSEy / count);
                    dRMSEr = Math.sqrt(Math.pow(dRMSEx, 2) + Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff;

                    GeoPosition position = GeoTools.getMidPioint(positions);

                    _AddTake5.setLatitude(position.getLatitude().toSignedDecimal());
                    _AddTake5.setLongitude(position.getLongitude().toSignedDecimal());
                    _AddTake5.setElevation(position.getElevation());
                    _AddTake5.setRMSEr(dRMSEr);
                    _AddTake5.setAndCalc(x, y, position.getElevation(), _Polygon);

                    addTake5(_AddTake5);
                }
            }
        }
    }

    @Override
    public void gpsError(GpsService.GpsError error) {
        super.gpsError(error);

        switch (error) {
            case LostDeviceConnection:
                stopLogging();
                //resetPoint();
                break;
            case NoExternalGpsSocket:
                break;
            case Unkown:
                break;
        }
    }
    //endregion



    public void btnTake5Click(View view) {
        if (!validateSideShot()) {
            return;
        }

        setupTake5();
    }

    public void btnSideShotClick(View view) {
        if (!validateSideShot()) {
            return;
        }

        setupSideShot();
    }

    public void btnCancelClick(View view) {
        fabCancel.setVisibility(View.GONE);
        cancelVisible = false;

        if (isLogging()) {
            stopLogging();
            _Bursts = new ArrayList<>();
            _UsedBursts = new ArrayList<>();

            fabT5.setEnabled(true);
            fabSS.setEnabled(true);
        } else if (_Points.size() > 0 && _CurrentPoint.getOp() == Units.OpType.SideShot) {
            _Points.remove(_Points.size() - 1);

            ignoreScroll = true;

            t5pAdapter.notifyItemRemoved(_Points.size());

            if (_Points.size() > 0) {
                rvPoints.smoothScrollToPosition(_Points.size() - 1);
            }

            if (_Points.size() < 1) {
                _CurrentPoint = null;
            } else {
                _CurrentPoint = _Points.get(_Points.size() - 1);
            }
        }

        saved = true;
        updated = false;
    }


    public void btnPointInfo(View view) {

    }
}
