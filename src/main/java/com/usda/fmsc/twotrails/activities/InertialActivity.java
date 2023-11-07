package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableAdapterEx;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.utilities.ResourceBitmapProvider;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.gnss.GeoTools;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;
import com.usda.fmsc.geospatial.ins.vectornav.VNInsData;
import com.usda.fmsc.geospatial.ins.vectornav.codes.MessageID;
import com.usda.fmsc.geospatial.ins.vectornav.commands.VNCommand;
import com.usda.fmsc.geospatial.ins.vectornav.nmea.sentences.base.VNNmeaSentence;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.activities.base.IUpdatePointActivity;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.adapters.MediaPagerAdapter;
import com.usda.fmsc.twotrails.adapters.MediaRvAdapter;
import com.usda.fmsc.twotrails.adapters.PointsEditRvAdapter;
import com.usda.fmsc.twotrails.data.TwoTrailsMediaSchema;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.ins.TtInsData;
import com.usda.fmsc.twotrails.ins.VNInsService;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.InertialPoint;
import com.usda.fmsc.twotrails.objects.points.InertialStartPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import jp.wasabeef.recyclerview.animators.BaseItemAnimator;
import jp.wasabeef.recyclerview.animators.FadeInAnimator;

public class InertialActivity extends AcquireGpsMapActivity implements IUpdatePointActivity, VNInsService.Listener, PointMediaController {
    private static final boolean enableCardFading = true;

    private final HashMap<String, PointMediaListener> listeners = new HashMap<>();

    private RecyclerViewEx<PointsEditRvAdapter<InertialActivity>.PointViewHolderEx> rvPoints;
    private PointsEditRvAdapter<InertialActivity> pointEditRvAdapter;
    private LinearLayoutManagerWithSmoothScroller linearLayoutManager;
    private FloatingActionButton fab, fabStopCancel;
    private LinearLayout layCardInfo;
    private CardView cvGpsInfo;

    private RelativeLayout progLay;
    private TextView tvProg;
    private MenuItem miMode, miHideGpsInfo, miCenterPosition;

    private List<TtPoint> _Points;
    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private ArrayList<TtInsData> _InsData;
    private TtPoint _PrevPoint, _CurrentPoint;
    private InertialStartPoint _InertialStartPoint;
    private TtGroup _Group;

    private int increment, takeAmount, nmeaCount = 0;
    private boolean saved = true, updated, onBnd = true, stopCancelVisible,
            ignoreScroll, mapViewMode, killAcquire, cameraSupported, gpsInfoHidden,
            centerPosition = false, _Locked,
            pointsCreated = false, _StartInertial = false, _InertialStarted = false;
    int numOfPointsCreated = 0;

    boolean invisible = false, handling;

    //region Media
    private TtMedia _CurrentMedia, _BackupMedia;

    private int bitmapHeight;
    private boolean mediaLoaded, ignoreMediaChange, _MediaUpdated;
    private int mediaCount, mediaSelectionIndex;

    private final Semaphore semaphore = new Semaphore(1);
    private final PostDelayHandler mediaLoaderDelayedHandler = new PostDelayHandler(500);

    private ViewPager mediaViewPager;
    private MediaPagerAdapter mediaPagerAdapter;
    private RecyclerViewEx<MediaRvAdapter.MediaViewHolder> rvMedia;
    private MediaRvAdapter rvMediaAdapter;

    private Toolbar toolbarMedia;
    private PopupMenuButton pmbMedia;

    private BitmapManager bitmapManager;
    private final BitmapManager.ScaleOptions scaleOptions = new BitmapManager.ScaleOptions();

    private final ComplexOnPageChangeListener onMediaPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignoreMediaChange && rvMediaAdapter.getItemCountEx() > 0) {
                saveMedia();

                setCurrentMedia(rvMediaAdapter.getItem(position));
                rvMediaAdapter.selectItem(position);
                rvMedia.smoothScrollToPosition(position);
                //onLockChange();
            }
            ignoreMediaChange = false;
        }
    };

    private final SelectableAdapterEx.Listener<TtMedia> mediaListener = new SelectableAdapterEx.Listener<TtMedia>() {
        @Override
        public void onItemSelected(TtMedia media, int adapterPosition, int layoutPosition) {
            if (mediaLoaded) {
                if (!_CurrentMedia.getCN().equals(media.getCN())) {
                    ignoreMediaChange = true;
                }

                saveMedia();

                setCurrentMedia(media);

                mediaViewPager.setCurrentItem(adapterPosition, true);
            }
        }
    };

    private final PopupMenu.OnMenuItemClickListener menuPopupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.ctx_menu_add) {
                pickImages();
            } else if (itemId == R.id.ctx_menu_capture) {
                if (AndroidUtils.Device.isFullOrientationAvailable(InertialActivity.this)) {
                    if (getTtAppCtx().getDeviceSettings().getUseTtCameraAsk()) {
                        DontAskAgainDialog dialog = new DontAskAgainDialog(InertialActivity.this,
                                DeviceSettings.USE_TTCAMERA_ASK,
                                DeviceSettings.USE_TTCAMERA,
                                getTtAppCtx().getDeviceSettings().getPrefs());

                        dialog.setMessage(InertialActivity.this.getString(R.string.points_camera_diag))
                                .setPositiveButton("TwoTrails", (dialogInterface, i, value) -> captureImage(true, _CurrentPoint), 2)
                                .setNegativeButton("Android", (dialogInterface, i, value) -> captureImage(false, _CurrentPoint), 1)
                                .setNeutralButton(getString(R.string.str_cancel), null, 0)
                                .show();
                    } else {
                        captureImage(getTtAppCtx().getDeviceSettings().getUseTtCamera() == 2, _CurrentPoint);
                    }
                } else {
                    captureImage(false, _CurrentPoint);
                }
            } else if (itemId == R.id.ctx_menu_update_orientation) {
                if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture) {
                    updateImageOrientation((TtImage) _CurrentMedia);
                }
            } else if (itemId == R.id.ctx_menu_reset) {
                resetMedia();
            } else if (itemId == R.id.ctx_menu_delete) {
                if (_CurrentMedia != null) {
                    new AlertDialog.Builder(InertialActivity.this)
                            .setMessage(String.format(
                                    "Would you like to delete %s '%s' from storage or only remove its association with the point?",
                                    _CurrentMedia.getMediaType().toString().toLowerCase(),
                                    _CurrentMedia.getName()))
                            .setPositiveButton(R.string.str_remove, (dialog, which) -> removeMedia(_CurrentMedia, false))
                            .setNegativeButton(R.string.str_delete, (dialog, which) -> new AlertDialog.Builder(InertialActivity.this)
                                    .setMessage(String.format("You are about to delete file '%s'.", _CurrentMedia.getFileName()))
                                    .setPositiveButton(R.string.str_delete, (dialog1, which1) -> removeMedia(_CurrentMedia, true))
                                    .setNeutralButton(R.string.str_cancel, null)
                                    .show())
                            .setNeutralButton(R.string.str_cancel, null)
                            .show();
                }
            }

            return false;
        }
    };
    //endregion

    private final PostDelayHandler pdhHideProgress = new PostDelayHandler(500);

    private final FilterOptions options = new FilterOptions();


    private AlphaAnimation animFadePartial = new AlphaAnimation(1f, .03f);

    //region Scroller
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (!ignoreScroll && !handling) {
                    if (enableCardFading) {

                        handling = true;

                        new Handler().post(() -> {
                            try {
                                animFadePartial.cancel();
                                layCardInfo.clearAnimation();

                                animFadePartial = new AlphaAnimation(.3f, 1f);
                                animFadePartial.setDuration(250);
                                animFadePartial.setFillEnabled(true);
                                animFadePartial.setFillAfter(true);

                                animFadePartial.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        layCardInfo.setAlpha(1f);
                                        layCardInfo.clearAnimation();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });

                                while (handling) {
                                    Thread.sleep(350);

                                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                                        layCardInfo.startAnimation(animFadePartial);
                                        invisible = false;
                                        handling = false;
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    }
                }

                onStopCardMovement();

                ignoreScroll = false;
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING && !ignoreScroll && !invisible) {
                onStartCardMovement(enableCardFading);

                new Thread(() -> {
                    while (invisible) {
                        try {
                            Thread.sleep(100);

                            int pos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();

                            if (pos < 0) {
                                pos = linearLayoutManager.findFirstVisibleItemPosition();
                            }

                            if (pos > -1 && pos < _Points.size()) {
                                final int fpos = pos;
                                runOnUiThread(() -> moveToMapPoint(fpos));
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    };
    //endregion


    //region Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inertial);

        setUseExitWarning(true);
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
                    if (intent.hasExtra(Consts.Codes.Data.POINT_PACKAGE)) {
                        Bundle bundle = intent.getBundleExtra(Consts.Codes.Data.POINT_PACKAGE);

                        if (bundle.containsKey(Consts.Codes.Data.POINT_DATA)) {
                            _CurrentPoint = bundle.getParcelable(Consts.Codes.Data.POINT_DATA);

                            if (_CurrentPoint != null) {
                                onBnd = _CurrentPoint.isOnBnd();
                            } else {
                                onBnd = true;
                            }
                        }
                    }

                    if (getCurrentMetadata() == null) {
                        cancelResult = Consts.Codes.Results.NO_METADATA_DATA;
                    } else {
                        setZone(getCurrentMetadata().getZone());

                        if (getPolygon() == null) {
                            cancelResult = Consts.Codes.Results.NO_POLYGON_DATA;
                        } else {
                            if (!isTrailModeEnabled()) {
                                enabledTrailMode(getPolygon());
                            }

                            _Points = getTtAppCtx().getDAL().getPointsInPolygon(getPolygon().getCN());

                            if (_Points.size() > 0 && _CurrentPoint != null) {
                                _Points = _Points.subList(0, _CurrentPoint.getIndex() + 1);

                                if (_CurrentPoint.getIndex() > 0) {
                                    _PrevPoint = _Points.get(_CurrentPoint.getIndex() - 1);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    cancelResult = Consts.Codes.Results.ERROR;
                    getTtAppCtx().getReport().writeError("InertialActivity:onCreate", e.getMessage(), e.getStackTrace());
                }
            } else {
                cancelResult = Consts.Codes.Results.NO_POINT_DATA;
            }

            if (cancelResult != 0) {
                setResult(cancelResult);
                finish();
                return;
            }

            addMapDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerOpened(View drawerView) {
                    if (isMapDrawerOpen(GravityCompat.END)) {
                        setMapDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.END);
                    }
                }
            });

            bitmapManager = new BitmapManager(new ResourceBitmapProvider(getTtAppCtx()), getTtAppCtx().getMAL());

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getPolygon().getName());
                actionBar.setDisplayShowTitleEnabled(true);

                AndroidUtils.UI.createToastForToolbarTitle(InertialActivity.this, getToolbar());
            }

            _Group = new TtGroup(TtGroup.GroupType.Inertial);
            getTtAppCtx().getDAL().insertGroup(_Group);

            fab = findViewById(R.id.inertialFab);
            fabStopCancel = findViewById(R.id.inertialFabStopCancel);

            layCardInfo = findViewById(R.id.inertialLayInfo);

            pointEditRvAdapter = new PointsEditRvAdapter<>(this, _Points, getCurrentMetadata());
            linearLayoutManager = new LinearLayoutManagerWithSmoothScroller(this);

            cvGpsInfo = findViewById(R.id.inertialCardGpsInfo);

            rvPoints = findViewById(R.id.inertialRvPoints);
            if (rvPoints != null) {
                rvPoints.setViewHasFooter(true);
                rvPoints.setLayoutManager(linearLayoutManager);
                rvPoints.setHasFixedSize(true);

                rvPoints.setItemAnimator(new BaseItemAnimator() {
                    @Override
                    protected void animateRemoveImpl(@NonNull final RecyclerView.ViewHolder holder) {
                        holder.itemView.animate()
                                .translationY(holder.itemView.getHeight())
                                .alpha(0)
                                .setDuration(getRemoveDuration())
                                .setInterpolator(getInterpolator())
                                .setListener(new DefaultRemoveAnimatorListener(holder))
                                .setStartDelay(getRemoveDelay(holder))
                                .start();
                    }

                    @Override
                    protected void preAnimateAddImpl(@NonNull RecyclerView.ViewHolder holder) {
                        holder.itemView.setTranslationY(holder.itemView.getHeight());
                        holder.itemView.setAlpha(0);
                    }

                    @Override
                    protected void animateAddImpl(@NonNull RecyclerView.ViewHolder holder) {
                        holder.itemView.animate()
                                .translationY(0)
                                .alpha(1)
                                .setDuration(getAddDuration())
                                .setInterpolator(getInterpolator())
                                .setListener(new DefaultAddAnimatorListener(holder))
                                .setStartDelay(getCardDelayTime())
                                .start();
                    }
                });
                rvPoints.setAdapter(pointEditRvAdapter);

                rvPoints.addOnScrollListener(scrollListener);
            }

            progLay = findViewById(R.id.progressLayout);
            tvProg = findViewById(R.id.inertialProgressText);


            //region Media Layout
            toolbarMedia = findViewById(R.id.toolbarMedia);
            toolbarMedia.setNavigationIcon(AndroidUtils.UI.getDrawable(InertialActivity.this, R.drawable.ic_arrow_back_white_24dp));
            toolbarMedia.setNavigationOnClickListener(v -> {
                setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                closeMapDrawer(GravityCompat.END);
            });

            pmbMedia = findViewById(R.id.pmdMenu);
            if (pmbMedia != null) {
                cameraSupported = AndroidUtils.Device.isCameraAvailable(InertialActivity.this);

                pmbMedia.setListener(menuPopupListener);

                pmbMedia.setItemEnabled(R.id.ctx_menu_reset, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_delete, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_capture, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_add, false);
            }

            rvMedia = findViewById(R.id.pmdRvMedia);
            if (rvMedia != null) {
                rvMedia.setViewHasFooter(true);
                rvMedia.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this, LinearLayoutManager.HORIZONTAL, false));
                rvMedia.setHasFixedSize(true);
                rvMedia.setItemAnimator(new FadeInAnimator());
            }

            mediaViewPager = findViewById(R.id.pmdViewPager);
            if (mediaViewPager != null) {
                rvMediaAdapter = new MediaRvAdapter(InertialActivity.this, Collections.synchronizedList(new ArrayList<>()), mediaListener,
                        AndroidUtils.Convert.dpToPx(InertialActivity.this, 90), bitmapManager);

                rvMedia.setAdapter(rvMediaAdapter);

                mediaPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager(), rvMediaAdapter);
                mediaViewPager.setAdapter(mediaPagerAdapter);
                mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);

                rvMediaAdapter.setListener(() -> mediaPagerAdapter.notifyDataSetChanged());

                loadMedia(_CurrentPoint, false);
            }

            ImageView ivFullscreen = findViewById(R.id.pmdIvFullscreen);
            if (ivFullscreen != null) {
                ivFullscreen.setOnClickListener(v -> {
                    if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture) {
                        TtUtils.Media.openInImageViewer(getTtAppCtx(), _CurrentMedia);
                    }
                });

                AndroidUtils.UI.setContentDescToast(ivFullscreen, "View in Fullscreen");
            }
            //endregion
        }
    }

    private int getBitmapHeight() {
        if (bitmapHeight == 0) {
            bitmapHeight = mediaViewPager.getHeight();
            bitmapManager.setImageLimitSize(bitmapHeight);
            scaleOptions.setScaleMode(BitmapManager.ScaleMode.Max);
            scaleOptions.setSize(bitmapHeight);
        }
        return bitmapHeight;
    }

    @Override
    protected int getMapRightDrawerLayoutId() {
        return R.layout.content_drawer_media;
    }

    @Override
    protected void updateActivitySettings() {
        super.updateActivitySettings();

        //TODO update for inertial
        options.Fix = getTtAppCtx().getDeviceSettings().getTake5FilterFix();
        options.FixType = getTtAppCtx().getDeviceSettings().getTake5FilterFixQuality();
        options.DopType = getTtAppCtx().getDeviceSettings().getTake5FilterDopType();
        options.DopValue = getTtAppCtx().getDeviceSettings().getTake5FilterDopValue();
        increment = getTtAppCtx().getDeviceSettings().getTake5Increment();
        takeAmount = getTtAppCtx().getDeviceSettings().getTake5NmeaAmount();
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inertial, menu);

        miMode = menu.findItem(R.id.inertialMenuMode);
        miHideGpsInfo = menu.findItem(R.id.inertialMenuGpsInfoToggle);
        miCenterPosition = menu.findItem(R.id.inertialMenuCenterPositionToggle);

        return super.onCreateOptionsMenuEx(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (isLogging()) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage("The you are currently acquiring a point. Do you want to exit anyway?");

                dialog.setPositiveButton(R.string.str_yes, (dialog1, which) -> {
                            stopLogging();
                            finish();
                        })
                        .setNeutralButton(R.string.str_cancel, null);

                dialog.show();
            } else {
                finish();
            }
        } else if (itemId == R.id.inertialMenuAddMedia) {
            openMapDrawer(GravityCompat.END);
        } else if (itemId == R.id.inertialMenuGps) {
            openSettings(SettingsActivity.GPS_SETTINGS_PAGE);
//        } else if (itemId == R.id.inertialMenuInertialSettings) {
//            openSettings(SettingsActivity.POINT_INERTIAL_SETTINGS_PAGE);
        } else if (itemId == R.id.inertialMenuMode) {
            mapViewMode = !mapViewMode;
            setMapGesturesEnabled(mapViewMode);
            layCardInfo.setEnabled(!mapViewMode);
            layCardInfo.setVisibility(mapViewMode ? View.GONE : View.VISIBLE);
            miCenterPosition.setVisible(mapViewMode);

            miMode.setIcon(mapViewMode ? R.drawable.ic_add_location_white_36dp : R.drawable.ic_map_white_36dp);
        } else if (itemId == R.id.inertialMenuGpsInfoToggle) {
            if (gpsInfoHidden) {
                gpsInfoHidden = false;
                cvGpsInfo.setVisibility(View.VISIBLE);
                miHideGpsInfo.setTitle(R.string.menu_x_hide_gps_info);
            } else {
                gpsInfoHidden = true;
                cvGpsInfo.setVisibility(View.GONE);
                miHideGpsInfo.setTitle(R.string.menu_x_show_gps_info);
            }
        } else if (itemId == R.id.inertialMenuCenterPositionToggle) {
            centerPosition = !centerPosition;
            miCenterPosition.setChecked(centerPosition);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onAppSettingsUpdated() {
        updateActivitySettings();
    }


    @Override
    public void onBackPressed() {
        if (isMapDrawerOpen(GravityCompat.END)) {
            setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
            closeMapDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_Points != null && _Points.size() > 1) {
            new Handler().postDelayed(() -> {
                linearLayoutManager.scrollToPositionWithOffset(_Points.size() - 1, 0);
                //rvPoints.scrollToPosition(_Points.size() - 1);
                rvPoints.setVisibility(View.VISIBLE);
                onStopCardMovement();
            }, 250);
        } else {
            rvPoints.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

    @Override
    public void finish() {
        if (pointsCreated) {
            if (!saved || updated) {
                savePoint(_CurrentPoint);
            }

            setResult(Consts.Codes.Results.POINT_CREATED, new Intent().putExtra(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS, numOfPointsCreated));
        } else {
            if (_Group != null) {
                getTtAppCtx().getDAL().deleteGroup(_Group.getCN());
            }

            setResult(RESULT_CANCELED);
        }

        super.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (_CurrentPoint == null || (_CurrentPoint.getOp() != OpType.InertialStart || _CurrentPoint.getOp() != OpType.Inertial)) {
                setupInertialStartPoint();
            } else if (_CurrentPoint.getOp() == OpType.Inertial) {
                addInertialPoint();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (hasPosition()) {
                moveToLocation(getLastPosition(), Consts.Location.ZOOM_CLOSE, true);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onMapReady() {
        super.onMapReady();

        setMapGesturesEnabled(mapViewMode);
    }

    private long getCardDelayTime() {
        return _CurrentPoint != null ? (_CurrentPoint.getOp() == OpType.Take5 ? 250 : 0) : 0;
    }

    @Override
    public boolean requiresInsService() {
        return true;
    }

    //endregion


    //region Update/Save/Validate/Setup Points
    public void updatePoint(TtPoint point) {
        if (_CurrentPoint.getCN().equals(point.getCN())) {
            _CurrentPoint = point;
            updated = true;

            if (onBnd != point.isOnBnd()) {
                onBnd = point.isOnBnd();

                removeLastPosition();
                addPosition(_CurrentPoint);
            }
        }
    }

    private boolean savePoint(TtPoint sPoint) {
        if (sPoint != null) {
            if (sPoint == _CurrentPoint) {
                if (!saved) {
                    getTtAppCtx().getDAL().insertPoint(sPoint);
                    if (sPoint.getOp() == OpType.Inertial) {
                        getTtAppCtx().getDAL().insertInsData(_InsData);
                    } else if (sPoint.getOp() == OpType.InertialStart) {
                        getTtAppCtx().getDAL().insertNmeaBursts(_Bursts);
                    }

                    pointsCreated = true;
                    numOfPointsCreated++;

                    if (sPoint.getIndex() < _Points.size()) { //update all the points after inserted point
                        for (int i = sPoint.getIndex() + 1; i < _Points.size(); i++) {
                            TtPoint point = TtUtils.Points.clonePoint(_Points.get(i));
                            point.setIndex(i);
                            getTtAppCtx().getDAL().updatePoint(point, _Points.get(i));
                        }
                    }

                    _Bursts = new ArrayList<>();
                    _UsedBursts = new ArrayList<>();
                } else if (updated) {
                    getTtAppCtx().getDAL().updatePoint(sPoint, sPoint);
                }

                saved = true;
                updated = false;
            } else {
                getTtAppCtx().getDAL().updatePoint(sPoint, sPoint);
            }
        }

        updateMediaContextMenuLocked();

        return true;
    }


    private void setupInertialStartPoint() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        _PrevPoint = _CurrentPoint;
        _InertialStartPoint = new InertialStartPoint();
        setupPoint(_InertialStartPoint);

        _Bursts = new ArrayList<>();
        _UsedBursts = new ArrayList<>();

        AndroidUtils.UI.hideKeyboard(this);

        startLogging();

        fab.setEnabled(false);

        showStopCancel();
    }

    private void setupPoint(TtPoint point) {
        if (_PrevPoint != null) {
            point.setPID(PointNamer.namePoint(_PrevPoint, increment));
            point.setIndex(_PrevPoint.getIndex() + 1);
        } else {
            point.setPID(PointNamer.nameFirstPoint(getPolygon()));
            point.setIndex(0);
        }

        point.setPolyCN(getPolygon().getCN());
        point.setPolyName(getPolygon().getName());
        point.setMetadataCN(getCurrentMetadata().getCN());
        point.setGroupCN(_Group.getCN());
        point.setGroupName(_Group.getName());
        point.setOnBnd(onBnd);

        saved = false;
        updated = true;
    }

    private void addInertialStartPoint(InertialStartPoint point) {
        TtPoint prevPoint = _CurrentPoint;
        _CurrentPoint = point;

        if (savePoint(point)) {
            point.adjustPoint(); //temp for map

            hideStopCancel();

            lockLastPoint(true);

            _PrevPoint = prevPoint;
            _Points.add(point.getIndex(), point);

            ignoreScroll = true;

            pointEditRvAdapter.notifyItemInserted(_Points.size() - 1);
            pointEditRvAdapter.notifyDataSetChanged();

            onStartCardMovement(false);
            rvPoints.smoothScrollToPosition(_Points.size() - 1);


            if (isTrailModeEnabled()) {
                addPosition(point);
            } else {
                getTtAppCtx().getReport().writeWarn("TrailMode is disabled.", "InertialActivity:addInertialStartPoint");
            }

            if (getTtAppCtx().getDeviceSettings().getTake5VibrateOnCreate()) {
                AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_POINT_CREATED);
            }

            if (getTtAppCtx().getDeviceSettings().getTake5RingOnCreate()) {
                AndroidUtils.Device.playSound(this, R.raw.ring);
            }

            fab.setEnabled(true);
            //TODO update to start inertial system
            fab.setImageResource(R.drawable.ic_ttpoint_sideshot_white);
        } else {
            _CurrentPoint = prevPoint;
            Toast.makeText(this, "Point failed to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupInertialPoint() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        lockLastPoint(true);

        _PrevPoint = _CurrentPoint;
        _CurrentPoint = new InertialPoint();
        setupPoint(_CurrentPoint);

        _Points.add(_CurrentPoint);

        ignoreScroll = true;

        onStartCardMovement(false);
        pointEditRvAdapter.notifyItemInserted(_Points.size() - 1);
        rvPoints.smoothScrollToPosition(_Points.size() - 1);

        AndroidUtils.UI.hideKeyboard(this);
    }

    private void addInertialPoint() {
        if (_InertialStarted) {
            if (_PrevPoint == null || !_PrevPoint.getOp().isInertialType()) {
                getTtAppCtx().getReport().writeWarn("Previous point is not an Inertial type.", "InertialActivity:addInertialPoint");
            }

            double azimuth = _PrevPoint.getOp() == OpType.InertialStart ?
                    ((InertialStartPoint)_PrevPoint).getTotalAzimuth() :
                    ((InertialPoint)_PrevPoint).getAzimuth();

            double x = 0, y = 0, z = 0, timespan = 0;
            boolean isConsecutive = true;

            for (TtInsData data : _InsData) {
                x += data.getDistanceX();
                y += data.getDistanceY();
                z += data.getDistanceZ();

                timespan += data.getTimeSpan();
                isConsecutive &= data.isConsecutive();
            }

            ((InertialPoint)_CurrentPoint).setInertialValues(azimuth, isConsecutive, timespan, x, y, z);

            setupInertialPoint();
        } else {
            getTtAppCtx().getReport().writeWarn("Inertial not started.", "InertialActivity:addInertialPoint");
        }
    }
    //endregion

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

        if (killAcquire) {
            runOnUiThread(() -> progLay.setVisibility(View.GONE));

            killAcquire = false;
        } else {
            pdhHideProgress.post(() -> runOnUiThread(() -> {
                Animation a = AnimationUtils.loadAnimation(InertialActivity.this, R.anim.push_down_out);

                a.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        progLay.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                progLay.startAnimation(a);
            }));
        }
    }

    private void lockLastPoint(boolean lock) {
        if (_Points.size() > 0) {
            RecyclerView.ViewHolder holder = rvPoints.findViewHolderForAdapterPosition(_Points.size() - 1);

            if (holder instanceof PointsEditRvAdapter.PointViewHolderEx) {
                ((PointsEditRvAdapter.PointViewHolderEx) holder).setLocked(lock);
            }
        }
    }

    //region UI
    private void showStopCancel() {
        if (!stopCancelVisible) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_right_in);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabStopCancel.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabStopCancel.startAnimation(a);
            stopCancelVisible = true;
        }
    }

    private void hideStopCancel() {
        AndroidUtils.UI.hideKeyboard(this);

        if (stopCancelVisible) {
            final Animation a = AnimationUtils.loadAnimation(this, R.anim.push_left_out);

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabStopCancel.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabStopCancel.startAnimation(a);
            stopCancelVisible = false;
        }
    }

    private void onStartCardMovement(boolean animateOpacity) {
        linearLayoutManager.setScrollingEnabled(true);

        if (animateOpacity) {
            animFadePartial.cancel();

            animFadePartial = new AlphaAnimation(layCardInfo.getAlpha(), .3f);
            animFadePartial.setDuration(250);
            animFadePartial.setFillEnabled(true);
            animFadePartial.setFillAfter(true);

            layCardInfo.startAnimation(animFadePartial);

            invisible = true;
        }
    }

    private void onStopCardMovement() {
        linearLayoutManager.setScrollingEnabled(false);
    }

    @Override
    protected void onEndHideExtraGpsStatus() {
        rvPoints.setVisibility(View.VISIBLE);
        linearLayoutManager.scrollToPositionWithOffset(_Points.size() - 1, 0);
        onStopCardMovement();
    }

    @Override
    protected void onEndShowExtraGpsStatus() {
        rvPoints.setVisibility(View.INVISIBLE);
    }

    //endregion

    //region GPS
    @Override
    protected void onNmeaBurstReceived(GnssNmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (isLogging() && nmeaBurst.isValid()) {
            TtNmeaBurst burst = TtNmeaBurst.create(_InertialStartPoint.getCN(), false, nmeaBurst);

            _Bursts.add(burst);

            if (TtUtils.NMEA.isBurstUsable(burst, options)) {
                burst.setUsed(true);
                _UsedBursts.add(burst);

                runOnUiThread(() -> tvProg.setText(StringEx.toString(++nmeaCount)));

                if (_UsedBursts.size() == takeAmount) {
                    stopLogging();

                    ArrayList<Position> positions = new ArrayList<>();
                    int zone = getCurrentMetadata().getZone();
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

                    Position position = GeoTools.getMidPoint(positions);

                    _InertialStartPoint.setLatitude(position.getLatitude());
                    _InertialStartPoint.setLongitude(position.getLongitude());
                    _InertialStartPoint.setElevation(position.getElevation());
                    _InertialStartPoint.setRMSEr(dRMSEr);
                    _InertialStartPoint.setAndCalc(x, y, position.getElevation(), getPolygon());

                    addInertialStartPoint(_InertialStartPoint);
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
            case Unknown:
                break;
        }
    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsValid) {
        super.nmeaBurstValidityChanged(burstsValid);

        String message = isReceivingNmea() ? "Now receiving NMEA data." : "No longer receiving NMEA data.";
        Toast.makeText(InertialActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region INS
    private final PostDelayHandler waitForTareHandler = new PostDelayHandler(10000);
    private final Runnable waitForTare = () -> {
        if (!_InertialStarted) {
            _StartInertial = false;
            Toast.makeText(InertialActivity.this, "Zero INS Timeout", Toast.LENGTH_SHORT).show();
        }
    };

    private void startInertial() {
        if (!getTtAppCtx().isVNInsServiceStarted() || !getTtAppCtx().getVnIns().isInsRunning()) {
            Toast.makeText(InertialActivity.this, "INS is not running. Please configure the device.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                _StartInertial = true;
                waitForTareHandler.post(waitForTare);
                getTtAppCtx().getVnIns().tare();
            } catch (IOException e) {
                _StartInertial = false;
                Toast.makeText(InertialActivity.this, "Unable to Zero INS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopInertial() {
        _InertialStarted = false;
        _InsData.clear();

        //TODO set Inertial Icon to Create Inertial Start and Stop Icon to Cancel
        fab.setImageResource(R.drawable.ic_ttpoint_gps_white); //to inertial start point
        fabStopCancel.setImageResource(R.drawable.ic_clear_white_36dp);
    }

    @Override
    public void insDataReceived(VNInsData data) {
        if (_InertialStarted) {
            _InsData.add(TtInsData.create(_CurrentPoint.getCN(), data));
        }
    }

    @Override
    public void nmeaSentenceReceived(VNNmeaSentence nmeaSentence) {
        //
    }

    @Override
    public void commandRespone(VNCommand command) {
        if (command.getMessageID() == MessageID.TAR) {
            waitForTareHandler.cancel();

            if (_StartInertial) {
                _InsData = new ArrayList<>();
                _InertialStarted = true;
                _StartInertial = false;

                //TODO set Inertial Icon and Stop Icon
                showStopCancel();
                fab.setImageResource(R.drawable.ic_ttpoint_traverse_white); //to ins point
                fabStopCancel.setImageResource(R.drawable.ic_clear_white_36dp); //to stop
                setupInertialPoint();

                Toast.makeText(InertialActivity.this, "Start Traversing", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(InertialActivity.this, "INS Zeroed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void receivingData(boolean receiving) {
        Toast.makeText(InertialActivity.this, receiving ? "Now receiving INS data" : "No longer receiving INS data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void receivingValidData(boolean valid) {
        if (!valid) {
            Toast.makeText(InertialActivity.this, "Received Invalid Data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void insStarted() {
        //
    }

    @Override
    public void insStopped() {
        //
    }

    @Override
    public void insServiceStarted() {
        //
    }

    @Override
    public void insServiceStopped() {
        //
    }

    @Override
    public void insError(VNInsService.InsError error) {
        switch (error) {
            case LostDeviceConnection:
            case DeviceConnectionEnded:
                stopInertial();
                Toast.makeText(InertialActivity.this, "INS connected stopped", Toast.LENGTH_LONG).show();
                break;
            case FailedToConnect:
                break;
            case Unknown:
            default:
                Toast.makeText(InertialActivity.this, "An unknown INS has occurred", Toast.LENGTH_LONG).show();
                break;
        }
    }
    //endregion

    //region Media
    private void saveMedia() {
        if (_MediaUpdated && _CurrentMedia != null) {
            if (!getTtAppCtx().getMAL().updateMedia(_CurrentMedia)) {
                Toast.makeText(InertialActivity.this,
                        String.format("Unable to save %s", _CurrentMedia.getMediaType().toString()),
                        Toast.LENGTH_LONG
                ).show();
            } else {
                setMediaUpdated(false);
            }
        }
    }

    private void removeMedia(TtMedia media, boolean delete) {
        List<TtMedia> mediaList = rvMediaAdapter.getItems();
        int index = mediaList.indexOf(media);

        getTtAppCtx().getMAL().deleteMedia(media);

//        if (delete) {
//            File file = new File(media.getPath());
//            file.deleteOnExit();
//        }

        mediaCount--;
        TtMedia changeTo = null;

        if (index > INVALID_INDEX) {
            if (index > 0) {
                if (index < mediaList.size() - 1) {
                    changeTo = mediaList.get(index + 1);
                } else {
                    changeTo = mediaList.get(--index);
                }
            } else if (mediaList.size() > 1) {
                changeTo = mediaList.get(1);
            } else {
                setMediaTitle(null);
            }
        }

        rvMediaAdapter.remove(media);
        mediaViewPager.setCurrentItem(index);
        setCurrentMedia(changeTo);
    }

    private void addImage(final TtImage picture) {
        if (picture != null) {
            if (getTtAppCtx().getMAL().insertImage(picture)) {
                mediaSelectionIndex = TtUtils.Media.getMediaIndex(picture, rvMediaAdapter.getItems());
                loadImageToList(picture);

//                new Thread(() -> {
//                    getTtAppCtx().getMAL().internalizeImages(null);
//                }).start();
            } else {
                Toast.makeText(InertialActivity.this, "Error saving picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addImages(final List<TtImage> pictures) {
        if (pictures.size() > 0) {
            int error = 0;

            pictures.sort(TtUtils.Media.PictureTimeComparator);

            for (int i = 0; i < pictures.size(); i++) {
                if (!getTtAppCtx().getMAL().insertImage(pictures.get(i))) {
                    pictures.remove(i--);
                    error++;
                }
            }

            mediaSelectionIndex = TtUtils.Media.getMediaIndex(pictures.get(0), rvMediaAdapter.getItems());

            for (TtImage p : pictures) {
                loadImageToList(p);
            }

            if (error > 0) {
                Toast.makeText(InertialActivity.this, String.format(Locale.getDefault(), "Error saving %d pictures", pictures.size()), Toast.LENGTH_LONG).show();
            }
        }
    }


    private void resetMedia() {
        if (_MediaUpdated) {
            new AlertDialog.Builder(this)
                    .setTitle(String.format("Reset Media %s", _CurrentMedia.getName()))
                    .setMessage(String.format("This will reset this %s back to its original values.",
                            _CurrentMedia.getMediaType().toString().toLowerCase()))
                    .setPositiveButton("Reset", (dialogInterface, i) -> {
                        _CurrentMedia = TtUtils.Media.cloneMedia(_BackupMedia);
                        setMediaUpdated(false);
                    })
                    .setNeutralButton(getString(R.string.str_cancel), null)
                    .show();
        }
    }

    private void loadMedia(final TtPoint point, final boolean loadPoints) {
        if (rvMediaAdapter != null) {
            mediaViewPager.removeOnPageChangeListener(onMediaPageChangeListener);

            rvMediaAdapter.clear();

            mediaLoaded = false;

            if (point != null) {
                if (getTtAppCtx().hasMAL()) {
                    if (loadPoints) {
                        mediaCount = 0;
                        mediaSelectionIndex = INVALID_INDEX;

                        ArrayList<TtImage> pictures = getTtAppCtx().getMAL().getImagesInPoint(point.getCN());

                        pictures.sort(TtUtils.Media.PictureTimeComparator);
                        for (final TtImage p : pictures) {
                            loadImageToList(p);
                        }

                        if (mediaCount > 0)
                            mediaSelectionIndex = 0;
                    } else {
                        mediaCount = getTtAppCtx().getMAL().getItemsCount(
                                TwoTrailsMediaSchema.Media.TableName,
                                TwoTrailsMediaSchema.Media.PointCN,
                                point.getCN());
                    }
                } else {
                    mediaCount = 0;
                }
            }

            setCurrentMedia(null);

            mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);
        }
    }

    private void loadImageToList(final TtImage picture) {
        mediaCount++;

        try {
            Bitmap bmp = bitmapManager.get(getTtAppCtx().getMAL().getProviderId(), picture.getCN());

            addImageToList(picture, true, bmp);
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "InertialActivity:loadImageToList", e.getStackTrace());
            addInvalidImagesToList(picture);
        }

//        getTtAppCtx().getMAL().loadImage(picture, new MediaAccessLayer.SimpleMalListener() {
//            @Override
//            public void imageLoaded(TtImage image, View view, Bitmap bitmap) {
//                addImageToList(picture, true, bitmap);
//            }
//
//            @Override
//            public void loadingFailed(TtImage image, View view, String reason) {
//                addInvalidImagesToList(picture);
//            }
//        });
    }

    private void addInvalidImagesToList(final TtImage picture) {
        Bitmap bitmap = BitmapFactory.decodeResource(InertialActivity.this.getResources(), R.drawable.ic_error_outline_black_48dp);
        if (bitmap != null) {
            addImageToList(picture, false, bitmap);
        }
    }

    private void addImageToList(final TtImage picture, boolean isValid, final Bitmap loadedImage) {
        if (picture.getPointCN().equals(_CurrentPoint.getCN())) {
//            if (isValid) {
//                bitmapManager.put(picture.getCN(), picture.getPath(), AndroidUtils.UI.scaleMinBitmap(loadedImage, getBitmapHeight(), false), scaleOptions);
//            } else {
//                bitmapManager.put(picture.getCN(), Integer.toString(R.drawable.ic_error_outline_black_48dp), AndroidUtils.UI.scaleMinBitmap(loadedImage, getBitmapHeight(), false), scaleOptions, true);
//            }

            try {
                semaphore.acquire();

                final int order = TtUtils.Media.getMediaIndex(picture, rvMediaAdapter.getItems());

                InertialActivity.this.runOnUiThread(() -> {
                    rvMediaAdapter.add(order, picture);

                    //don't bombard the adapter with lots of changes
                    mediaLoaderDelayedHandler.post(onMediaChanged);

                    semaphore.release();
                });
            } catch (InterruptedException e) {
                //
            }
        }
    }

    private void updateMediaContextMenuLocked() {
        boolean unlocked = _CurrentMedia != null;
        pmbMedia.setItemEnabled(R.id.ctx_menu_reset, unlocked);
        pmbMedia.setItemEnabled(R.id.ctx_menu_delete, unlocked);

        unlocked = _CurrentPoint != null;
        pmbMedia.setItemEnabled(R.id.ctx_menu_capture, unlocked && cameraSupported);
        pmbMedia.setItemEnabled(R.id.ctx_menu_add, unlocked);
    }


    private final Runnable onMediaChanged = new Runnable() {
        @Override
        public void run() {
            InertialActivity.this.runOnUiThread(() -> {
                if (mediaSelectionIndex > INVALID_INDEX && mediaSelectionIndex < rvMediaAdapter.getItemCountEx()) {
                    setCurrentMedia(rvMediaAdapter.getItem(mediaSelectionIndex));
                    rvMediaAdapter.selectItem(mediaSelectionIndex);
                    mediaViewPager.setCurrentItem(mediaSelectionIndex);
                    mediaSelectionIndex = INVALID_INDEX;
                }

                mediaLoaded = true;

                setMediaTitle(isMapDrawerOpen(GravityCompat.END) ? _CurrentMedia.getName() : null);
                _Locked = false;
                onLockChange();
            });
        }
    };


    private void setCurrentMedia(TtMedia media) {
        if (media != null) {
            if (isMapDrawerOpen(GravityCompat.END)) {
                setMediaTitle(media.getName());
            } else {
                setMediaTitle(null);
            }

            if (_CurrentMedia == null || !media.getCN().equals(_CurrentMedia.getCN())) {
                _BackupMedia = TtUtils.Media.cloneMedia(media);

                setMediaUpdated(false);
            }
        } else {
            setMediaTitle(null);
        }

        _CurrentMedia = media;

        if (_CurrentMedia == null)
            _BackupMedia = null;
    }

    private void setMediaTitle(String title) {
        if (title != null) {
            toolbarMedia.setTitle(title);
        } else {
            toolbarMedia.setTitle(String.format(Locale.getDefault(), "Media (%d)", mediaCount));
        }
    }

    private void setMediaUpdated(boolean updated) {
        _MediaUpdated = updated;

        pmbMedia.setItemEnabled(R.id.ctx_menu_reset, _MediaUpdated);
    }


    public void updateMedia(TtMedia media) {
        //only update if current media
        if (_CurrentMedia.getCN().equals(media.getCN())) {
            setCurrentMedia(media);
            setMediaUpdated(true);
        }
    }

    @Override
    public TtMetadata getMetadata(String cn) {
        return getCurrentMetadata();
    }

    @Override
    public BitmapManager getBitmapManager() {
        return bitmapManager;
    }


    @Override
    protected void onImageCaptured(TtImage image) {
        addImage(image);
    }

    @Override
    protected void onImagesSelected(List<TtImage> images) {
        addImages(images);
    }

    //endregion

    //region Controls
    public void btnInertialClick(View view) {
        if (_CurrentPoint == null || (!_CurrentPoint.getOp().isInertialType() || (_CurrentPoint.getOp() == OpType.Inertial && !_InertialStarted))) {
            if (isReceivingNmea()) {
                setupInertialStartPoint();
            } else {
                Toast.makeText(InertialActivity.this, "Currently not receiving NMEA data.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (_InertialStarted) {
                setupInertialPoint();
            } else if (!_StartInertial && _CurrentPoint.getOp() == OpType.InertialStart) {
                startInertial();
            }
        }
    }

    public void btnStopCancelClick(View view) {
        if (isLogging()) {
            killAcquire = true;
            stopLogging();
            _Bursts = new ArrayList<>();
            _UsedBursts = new ArrayList<>();

            fab.setEnabled(true);
        } else if (_InertialStarted) {
            stopInertial();
        }

        saved = true;
        updated = false;
    }

    public void btnPointInfo(View view) {

    }

    public void btnInertialInfoClick(View view) {
        //inertial info control
    }
    //endregion

    @Override
    protected MapTracking getMapTracking() {
        return !mapViewMode || centerPosition ? MapTracking.FOLLOW : MapTracking.NONE;
    }

    //region Fragment Interaction
    private void onLockChange() {
        if (_CurrentPoint != null && listeners.containsKey(_CurrentPoint.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentPoint.getCN());
            if (listener != null) {
                listener.onLockChange(_Locked);
            }
        }

        if (_CurrentMedia != null && listeners.containsKey(_CurrentMedia.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentMedia.getCN());
            if (listener != null) {
                listener.onLockChange(_Locked);
            }
        }
    }

    private void onPointUpdate() {
        if (_CurrentPoint != null && listeners.containsKey(_CurrentPoint.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentPoint.getCN());
            if (listener != null) {
                listener.onPointUpdated(_CurrentPoint);
            }
        }
    }

    private void onPointUpdate(TtPoint point) {
        if (listeners.containsKey(point.getCN())) {
            PointMediaListener listener = listeners.get(point.getCN());
            if (listener != null) {
                listener.onPointUpdated(point);
            }
        }
    }


    private void onMediaUpdated() {
        setMediaUpdated(true);

        if (listeners.containsKey(_CurrentMedia.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentMedia.getCN());
            if (listener != null) {
                listener.onMediaUpdated(_CurrentMedia);
            }
        }
    }

    protected void onMediaUpdated(TtMedia media) {
        if (listeners.containsKey(media.getCN())) {
            PointMediaListener listener = listeners.get(media.getCN());
            if (listener != null) {
                listener.onMediaUpdated(media);
            }
        }
    }


    @Override
    protected void onImageOrientationUpdated(TtImage image) {
        onMediaUpdated(image);
    }


    public void register(String pointCN, PointMediaListener listener) {
        if (listener != null && !listeners.containsKey(pointCN)) {
            listeners.put(pointCN, listener);
        }
    }

    public void unregister(String pmlCN) {
        listeners.remove(pmlCN);
    }
    //endregion
}
