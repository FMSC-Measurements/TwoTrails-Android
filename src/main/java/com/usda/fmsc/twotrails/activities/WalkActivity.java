package com.usda.fmsc.twotrails.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.drawables.AnimationDrawableEx;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.WalkPoint;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.List;

import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.utilities.StringEx;

public class WalkActivity extends AcquireGpsMapActivity {
    private MenuItem miRenameGroup, miWalking;
    private FloatingActionButton fabWalk;
    private View walkCardView, preFocusView;

    private TextView tvPID, tvX, tvY, tvElev;
    private EditText txtCmt;
    private ImageButton ibBnd;
    private Drawable dOnBnd, dOffBnd, dWalk, dPause;
    private AnimationDrawableEx adWalking;
    private MenuItem miMode;

    private WalkPoint _CurrentPoint;
    private TtPoint _PrevPoint;
    private TtMetadata _Metadata;
    private TtPolygon _Polygon;
    private TtGroup _Group;


    private int pointsCreated = 0, increment, frequency, minWalkDist;
    private boolean updated, onBnd = true, walking, useRing, useVib, menuCreated, mapViewMode;
    private long lastPointCreationTime = 0;

    private FilterOptions options = new FilterOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        setUseExitWarning(true);
        setUseLostConnectionWarning(true);

        if (!isCanceling()) {
            SheetLayoutEx.enterFromBottomAnimation(this);
            int cancelResult = 0;

            Intent intent = getIntent();
            if (intent != null && intent.getExtras() != null) {
                try {
                    if (intent.getExtras().containsKey(Consts.Codes.Data.POINT_DATA)) {
                        _PrevPoint = intent.getParcelableExtra(Consts.Codes.Data.POINT_DATA);
                        onBnd = _PrevPoint.isOnBnd();
                    }

                    _Metadata = intent.getParcelableExtra(Consts.Codes.Data.METADATA_DATA);
                    _Polygon = getPolygon();

                    if (_Metadata == null) {
                        cancelResult = Consts.Codes.Results.NO_METDATA_DATA;
                    } else {
                        setZone(_Metadata.getZone());

                        if (_Polygon == null) {
                            cancelResult = Consts.Codes.Results.NO_POLYGON_DATA;
                        }
                    }
                } catch (Exception e) {
                    cancelResult = Consts.Codes.Results.ERROR;
                    e.printStackTrace();
                }
            } else {
                cancelResult = Consts.Codes.Results.NO_POINT_DATA;
            }

            if (cancelResult != 0) {
                setResult(cancelResult);
                finish();
                return;
            }

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(_Polygon.getName());
                actionBar.setDisplayShowTitleEnabled(true);
            }

            _Group = new TtGroup(TtGroup.GroupType.Walk);
            Global.getDAL().insertGroup(_Group);

            fabWalk = (FloatingActionButton)findViewById(R.id.walkFabWalk);
            walkCardView = findViewById(R.id.walkCardWalk);
            preFocusView = findViewById(R.id.preFocusView);

            tvPID = (TextView)findViewById(R.id.pointHeaderTvPid);
            tvX = (TextView)findViewById(R.id.pointCardTvX);
            tvY = (TextView)findViewById(R.id.pointCardTvY);
            tvElev = (TextView)findViewById(R.id.pointCardTvElev);
            txtCmt = (EditText)findViewById(R.id.pointTxtCmt);
            ibBnd = (ImageButton)findViewById(R.id.pointHeaderIbBnd);

            ImageView ivOp = (ImageView)findViewById(R.id.pointHeaderIvOp);
            if (ivOp != null) {
                ivOp.setImageDrawable(TtUtils.UI.getTtOpDrawable(OpType.Walk, AppUnits.IconColor.Dark, this));
            }

            TextView tvElevType = (TextView)findViewById(R.id.pointCardTvElevType);
            if (tvElevType != null) {
                tvElevType.setText(_Metadata.getElevation().toString());
            }

            dOnBnd = AndroidUtils.UI.getDrawable(this, R.drawable.ic_onbnd_dark);
            dOffBnd = AndroidUtils.UI.getDrawable(this, R.drawable.ic_offbnd_dark);

            dWalk = AndroidUtils.UI.getDrawable(this, R.drawable.ic_ttpoint_walk_white);
            dPause = AndroidUtils.UI.getDrawable(this, R.drawable.ic_media_pause);

            ibBnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (_CurrentPoint != null) {
                        onBnd = !_CurrentPoint.isOnBnd();

                        ibBnd.setImageDrawable(onBnd ? dOnBnd : dOffBnd);
                        _CurrentPoint.setOnBnd(onBnd);
                        updated = true;
                    }
                }
            });

            lockPoint(true);
        }
    }

    @Override
    protected void getSettings() {
        super.getSettings();

        options.FixType = Global.Settings.DeviceSettings.getWalkFilterFixType();
        options.DopType = Global.Settings.DeviceSettings.getWalkFilterDopType();
        options.DopValue = Global.Settings.DeviceSettings.getWalkFilterDopValue();
        increment = Global.Settings.DeviceSettings.getWalkIncrement();
        frequency = Global.Settings.DeviceSettings.getWalkFilterFrequency() * 1000;  //to milliseconds
        minWalkDist = Global.Settings.DeviceSettings.getWalkFilterAccuracy();

        useVib = Global.Settings.DeviceSettings.getWalkVibrateOnCreate();
        useRing = Global.Settings.DeviceSettings.getWalkRingOnCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_walk, menu);

        miMode = menu.findItem(R.id.walkMenuMode);
        miRenameGroup = menu.findItem(R.id.walkMenuRenameGroup);
        miWalking = menu.findItem(R.id.walkMenuWalking);
        adWalking = new AnimationDrawableEx((AnimationDrawable)miWalking.getIcon());

        miWalking.setIcon(adWalking);

        menuCreated = true;

        return super.onCreateOptionsMenu(menu);
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
            case R.id.walkMenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.walkMenuWalkSettings: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.FILTER_WALK_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.walkMenuRenameGroup: {
                updateGroupName();
                break;
            }
            case R.id.walkMenuMode: {
                mapViewMode = !mapViewMode;
                setMapGesturesEnabled(mapViewMode);
                setMapFollowMyPosition(!mapViewMode);
                txtCmt.setEnabled(!mapViewMode);
                walkCardView.setEnabled(!mapViewMode && _CurrentPoint != null);
                walkCardView.setVisibility(mapViewMode || _CurrentPoint == null ? View.GONE : View.VISIBLE);
                walkCardView.setAlpha(mapViewMode || _CurrentPoint == null ? 0f : 1f);
                miMode.setIcon(mapViewMode ? R.drawable.ic_add_location_white_36dp : R.drawable.ic_map_white_36dp);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case Consts.Codes.Activites.SETTINGS: {
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
        if (updated) {
            Global.getDAL().updatePoint(_CurrentPoint);
        }

        if (pointsCreated > 0) {
            setResult(Consts.Codes.Results.POINT_CREATED,
                    new Intent().putExtra(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS, pointsCreated));
        } else {
            if (_Group != null) {
                Global.getDAL().deleteGroup(_Group.getCN());
            }

            setResult(RESULT_CANCELED);
        }

        super.finish();
    }

    private void updateGroupName() {
        final InputDialog dialog = new InputDialog(this);

        dialog.setTitle(String.format("Rename Group (%s)", _Group.getName()));
        dialog.setInputText(_Group.getName());

        dialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = dialog.getText();

                _Group.setName(name);
                Global.getDAL().updateGroup(_Group);

                if (_CurrentPoint != null) {
                    _CurrentPoint.setGroupName(name);
                    updated = true;

                    List<TtPoint> points = Global.getDAL().getPointsInGroup(_Group.getCN());

                    if (points.size() > 0) {
                        for (TtPoint p : points) {
                            p.setGroupName(name);
                        }

                        Global.getDAL().updatePoints(points);
                    }
                }
            }
        })
        .setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }

    private void lockPoint(boolean lock) {
        ibBnd.setEnabled(!lock);
        txtCmt.setEnabled(!lock);

        if (lock) {
            preFocusView.requestFocus();
        }
    }


    private void createPoint(INmeaBurst nmeaBurst, UTMCoords utmCoords) {
        if (updated) {
            Global.getDAL().updatePoint(_CurrentPoint);
            updated = false;
        }

        if (_CurrentPoint != null)
            _PrevPoint = _CurrentPoint;

        _CurrentPoint = new WalkPoint();

        if (_PrevPoint == null) {
            _CurrentPoint.setPID(PointNamer.nameFirstPoint(_Polygon));
            _CurrentPoint.setIndex(0);
        } else {
            _CurrentPoint.setPID(PointNamer.namePoint(_PrevPoint, increment));
            _CurrentPoint.setIndex(_PrevPoint.getIndex() + 1);
        }

        TtNmeaBurst burst = TtNmeaBurst.create(_CurrentPoint.getCN(), true, nmeaBurst);

        _CurrentPoint.setOnBnd(onBnd);
        _CurrentPoint.setPolyCN(_Polygon.getCN());
        _CurrentPoint.setPolyName(_Polygon.getName());
        _CurrentPoint.setGroupCN(_Group.getCN());
        _CurrentPoint.setGroupName(_Group.getName());
        _CurrentPoint.setMetadataCN(_Metadata.getCN());

        _CurrentPoint.setLatitude(nmeaBurst.getPosition().getLatitudeSignedDecimal());
        _CurrentPoint.setLongitude(nmeaBurst.getPosition().getLongitudeSignedDecimal());
        _CurrentPoint.setElevation(nmeaBurst.getElevation());

        //saves the need to recalculate utm
        _CurrentPoint.setAndCalc(utmCoords.getX(), utmCoords.getY(), burst.getElevation(), _Polygon);

        try {
            Global.getDAL().insertPoint(_CurrentPoint);
            Global.getDAL().insertNmeaBurst(burst);
            pointsCreated++;

            lastPointCreationTime = System.currentTimeMillis();
            onPointCreated();
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "WalkActivity:createPoint");
            AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_ERROR);
            stopLogging();
        }
    }

    private void onPointCreated() {
        if (!mapViewMode) {
            Animation animOut = AnimationUtils.loadAnimation(this, R.anim.push_down_out_fast);
            final Animation animIn = AnimationUtils.loadAnimation(this, R.anim.push_up_in_fast);

            final Context ctx = this;

            animOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    preFocusView.requestFocus();

                    tvPID.setText(StringEx.toString(_CurrentPoint.getPID()));
                    ibBnd.setImageDrawable(_CurrentPoint.isOnBnd() ? dOnBnd : dOffBnd);

                    tvX.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjX(), Consts.Minimum_Point_Display_Digits));
                    tvY.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjY(), Consts.Minimum_Point_Display_Digits));
                    tvElev.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjZ(), Consts.Minimum_Point_Display_Digits));

                    txtCmt.setText(_CurrentPoint.getComment());

                    walkCardView.setVisibility(View.VISIBLE);

                    walkCardView.clearAnimation();
                    walkCardView.startAnimation(animIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            animIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (useVib) {
                        AndroidUtils.Device.vibrate(ctx, Consts.Notifications.VIB_POINT_CREATED);
                    }

                    if (useRing) {
                        AndroidUtils.Device.playSound(ctx, R.raw.ring);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            walkCardView.startAnimation(animOut);
        } else {
            preFocusView.requestFocus();

            tvPID.setText(StringEx.toString(_CurrentPoint.getPID()));
            ibBnd.setImageDrawable(_CurrentPoint.isOnBnd() ? dOnBnd : dOffBnd);

            tvX.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjX(), Consts.Minimum_Point_Display_Digits));
            tvY.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjY(), Consts.Minimum_Point_Display_Digits));
            tvElev.setText(StringEx.toStringRound(_CurrentPoint.getUnAdjZ(), Consts.Minimum_Point_Display_Digits));

            txtCmt.setText(_CurrentPoint.getComment());

            if (useVib) {
                AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_POINT_CREATED);
            }

            if (useRing) {
                AndroidUtils.Device.playSound(this, R.raw.ring);
            }
        }

        Global.TtNotifyManager.showPointAquired();

        addPosition(_CurrentPoint, getLastPosition() != null);
    }

    private void setStartWalkingDrawable(boolean startAquring) {
        Drawable[] draws;

        if (startAquring) {
            draws = new Drawable[]{ dWalk, dPause };
            AndroidUtils.UI.setContentDescToast(fabWalk, "Pause");
        } else {
            draws = new Drawable[]{ dPause, dWalk };
            AndroidUtils.UI.setContentDescToast(fabWalk, "Start Walking");
        }

        TransitionDrawable trans = new TransitionDrawable(draws);
        trans.setCrossFadeEnabled(true);
        fabWalk.setImageDrawable(trans);
        trans.startTransition(250);
    }


    @Override
    protected void startLogging() {
        super.startLogging();

        walking = true;
        lastPointCreationTime = 0;

        if (menuCreated) {
            AndroidUtils.UI.disableMenuItem(miRenameGroup);
            miWalking.setVisible(true);
            adWalking.start();
        }

        lockPoint(true);

        setStartWalkingDrawable(true);

        Global.TtNotifyManager.startWalking();
    }

    @Override
    protected void stopLogging() {
        super.stopLogging();

        walking = false;

        if (menuCreated) {
            AndroidUtils.UI.enableMenuItem(miRenameGroup);
            miWalking.setVisible(false);
            adWalking.stop();
        }

        lockPoint(false);

        setStartWalkingDrawable(false);

        Global.TtNotifyManager.stopWalking();
    }

    @Override
    protected void onNmeaBurstReceived(INmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (walking) {
            //if valid and after frequency
            if (System.currentTimeMillis() - lastPointCreationTime >= frequency && nmeaBurst.isValid()) {

                //if point is up good enough to use
                if (TtUtils.isUsableNmeaBurst(nmeaBurst, options)) {
                    UTMCoords utmCoords = nmeaBurst.getUTM(_Metadata.getZone());

                    //if more than minimum distance
                    if (_PrevPoint == null || minWalkDist <= 0 || TtUtils.Math.distance(utmCoords.getX(), utmCoords.getY(),
                            _PrevPoint.getUnAdjX(), _PrevPoint.getUnAdjY()) > minWalkDist) {
                        createPoint(nmeaBurst, utmCoords);
                    }
                }
            }
        }
    }

    public void btnWalkClick(View view) {
        if (walking) {
            stopLogging();
        } else {
            startLogging();
        }
    }

    public void btnPointInfo(View view) { }

    @Override
    protected MapTracking getMapTracking() {
        return mapViewMode ? MapTracking.NONE : MapTracking.FOLLOW;
    }
}
