package com.usda.fmsc.twotrails.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
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

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableAdapterEx;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.android.utilities.DeviceOrientationEx;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.geospatial.nmea41.NmeaBurst;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.activities.base.AcquireGpsMapActivity;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.adapters.MediaPagerAdapter;
import com.usda.fmsc.twotrails.adapters.MediaRvAdapter;
import com.usda.fmsc.twotrails.data.MediaAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsMediaSchema;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.adapters.Take5PointsEditRvAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.points.SideShotPoint;
import com.usda.fmsc.twotrails.objects.points.Take5Point;
import com.usda.fmsc.twotrails.objects.TtGroup;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.GeoTools;

import jp.wasabeef.recyclerview.animators.BaseItemAnimator;
import jp.wasabeef.recyclerview.animators.FadeInAnimator;

@SuppressLint({"RestrictedApi", "unused"})
public class Take5Activity extends AcquireGpsMapActivity implements PointMediaController {
    private static final boolean enableCardFading = true;

    private HashMap<String, PointMediaListener> listeners = new HashMap<>();

    private RecyclerViewEx rvPoints;
    private Take5PointsEditRvAdapter t5pAdapter;
    private LinearLayoutManagerWithSmoothScroller linearLayoutManager;
    private FloatingActionButton fabT5, fabSS, fabCancel, fabSSCommit;
    private LinearLayout layCardInfo;
    private CardView cvGpsInfo;

    private RelativeLayout progLay;
    private TextView tvProg;
    private MenuItem miMode, miMoveToEnd, miHideGpsInfo, miCenterPosition;

    private List<TtPoint> _Points;
    private ArrayList<TtNmeaBurst> _Bursts, _UsedBursts;
    private TtPoint _PrevPoint, _CurrentPoint;
    private Take5Point _AddTake5;
    private TtGroup _Group;

    private int increment, takeAmount, nmeaCount = 0;
    private boolean saved = true, updated, onBnd = true, createSSVisible, cancelVisible, commitSSVisible,
            ignoreScroll, useRing, useVib, mapViewMode, killAcquire, cameraSupported, gpsInfoHidden,
            centerPosition = false, _Locked;

    boolean invisible = false, handling;

    //region Media
    private TtMedia _CurrentMedia, _BackupMedia;

    private int bitmapHeight;
    private boolean mediaLoaded, ignoreMediaChange, _MediaUpdated;
    private int mediaCount, mediaSelectionIndex;

    private Uri captureImageUri;

    private Semaphore semaphore = new Semaphore(1);
    private PostDelayHandler mediaLoaderDelayedHandler = new PostDelayHandler(500);

    private ViewPager mediaViewPager;
    private MediaPagerAdapter mediaPagerAdapter;
    private RecyclerViewEx rvMedia;
    private MediaRvAdapter rvMediaAdapter;

    private Toolbar toolbarMedia;
    private PopupMenuButton pmbMedia;

    private BitmapManager bitmapManager;
    private BitmapManager.ScaleOptions scaleOptions = new BitmapManager.ScaleOptions();

    private ComplexOnPageChangeListener onMediaPageChangeListener = new ComplexOnPageChangeListener() {
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
    
    private SelectableAdapterEx.Listener<TtMedia> mediaListener = new SelectableAdapterEx.Listener<TtMedia>() {
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

    private PopupMenu.OnMenuItemClickListener menuPopupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.ctx_menu_add: {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("image/*");
                    startActivityForResult(intent, Consts.Codes.Requests.ADD_IMAGES);
                    break;
                }
                case R.id.ctx_menu_capture: {
                    if (AndroidUtils.Device.isFullOrientationAvailable(Take5Activity.this)) {
                        if (getTtAppCtx().getDeviceSettings().getUseTtCameraAsk()) {
                            DontAskAgainDialog dialog = new DontAskAgainDialog(Take5Activity.this,
                                    DeviceSettings.USE_TTCAMERA_ASK,
                                    DeviceSettings.USE_TTCAMERA,
                                    getTtAppCtx().getDeviceSettings().getPrefs());

                            dialog.setMessage(Take5Activity.this.getString(R.string.points_camera_diag))
                                    .setPositiveButton("TwoTrails", (dialogInterface, i, value) -> captureImageUri = TtUtils.Media.captureImage(Take5Activity.this, true, _CurrentPoint), 2)
                                    .setNegativeButton("Android", (dialogInterface, i, value) -> captureImageUri = TtUtils.Media.captureImage(Take5Activity.this, false, _CurrentPoint), 1)
                                    .setNeutralButton(getString(R.string.str_cancel), null, 0)
                                    .show();
                        } else {
                            captureImageUri = TtUtils.Media.captureImage(Take5Activity.this, getTtAppCtx().getDeviceSettings().getUseTtCamera() == 2, _CurrentPoint);
                        }
                    } else {
                        captureImageUri = TtUtils.Media.captureImage(Take5Activity.this, false, _CurrentPoint);
                    }
                    break;
                }
                case R.id.ctx_menu_update_orientation: {
                    if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture) {
                        TtUtils.Media.updateImageOrientation(Take5Activity.this, (TtImage)_CurrentMedia);
                    }
                    break;
                }
                case R.id.ctx_menu_reset: {
                    resetMedia();
                    break;
                }
                case R.id.ctx_menu_delete: {
                    if (_CurrentMedia != null) {
                        new AlertDialog.Builder(Take5Activity.this)
                                .setMessage(String.format(
                                        "Would you like to delete %s '%s' from storage or only remove its association with the point?",
                                        _CurrentMedia.getMediaType().toString().toLowerCase(),
                                        _CurrentMedia.getName()))
                                .setPositiveButton(R.string.str_remove, (dialog, which) -> removeMedia(_CurrentMedia, false))
                                .setNegativeButton(R.string.str_delete, (dialog, which) -> new AlertDialog.Builder(Take5Activity.this)
                                        .setMessage(String.format("You are about to delete file '%s'.", _CurrentMedia.getFilePath()))
                                        .setPositiveButton(R.string.str_delete, (dialog1, which1) -> removeMedia(_CurrentMedia, true))
                                        .setNeutralButton(R.string.str_cancel, null)
                                        .show())
                                .setNeutralButton(R.string.str_cancel, null)
                                .show();
                    }
                    break;
                }
            }

            return false;
        }
    };
    //endregion


    private PostDelayHandler pdhHideProgress = new PostDelayHandler(500);

    private FilterOptions options = new FilterOptions();


    private AlphaAnimation animFadePartial = new AlphaAnimation(1f, .03f);

    //region Scroller
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
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
        setContentView(R.layout.activity_take5);

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
                    getTtAppCtx().getReport().writeError("Take5Activity:onCreate", e.getMessage(), e.getStackTrace());
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

            bitmapManager = new BitmapManager(getResources());

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getPolygon().getName());
                actionBar.setDisplayShowTitleEnabled(true);

                AndroidUtils.UI.createToastForToolbarTitle(Take5Activity.this, getToolbar());
            }

            _Group = new TtGroup(TtGroup.GroupType.Take5);
            getTtAppCtx().getDAL().insertGroup(_Group);

            fabT5 = findViewById(R.id.take5FabT5);
            fabSS = findViewById(R.id.take5FabSideShot);
            fabCancel = findViewById(R.id.take5FabCancel);
            fabSSCommit = findViewById(R.id.take5FabSideShotCommit);

            layCardInfo = findViewById(R.id.take5LayInfo);

            t5pAdapter = new Take5PointsEditRvAdapter(this, _Points, getCurrentMetadata());
            linearLayoutManager = new LinearLayoutManagerWithSmoothScroller(this);

            cvGpsInfo = findViewById(R.id.take5CardGpsInfo);

            rvPoints = findViewById(R.id.take5RvPoints);
            if (rvPoints != null) {
                rvPoints.setViewHasFooter(true);
                rvPoints.setLayoutManager(linearLayoutManager);
                rvPoints.setHasFixedSize(true);
                //rvPoints.setItemAnimator(new SlideInUpAnimator()); //too long of a delay
                rvPoints.setItemAnimator(new BaseItemAnimator() {
                    @Override protected void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
                        ViewCompat.animate(holder.itemView)
                                .translationY(holder.itemView.getHeight())
                                .alpha(0)
                                .setDuration(getRemoveDuration())
                                .setInterpolator(mInterpolator)
                                .setListener(new DefaultRemoveVpaListener(holder))
                                .setStartDelay(getRemoveDelay(holder))
                                .start();
                    }

                    @Override protected void preAnimateAddImpl(RecyclerView.ViewHolder holder) {
                        ViewCompat.setTranslationY(holder.itemView, holder.itemView.getHeight());
                        ViewCompat.setAlpha(holder.itemView, 0);
                    }

                    @Override
                    protected void animateAddImpl(RecyclerView.ViewHolder holder) {
                        ViewCompat.animate(holder.itemView)
                                .translationY(0)
                                .alpha(1)
                                .setDuration(getAddDuration())
                                .setInterpolator(mInterpolator)
                                .setListener(new DefaultAddVpaListener(holder))
                                .setStartDelay(getCardDelayTime())
                                .start();
                    }
                });
                rvPoints.setAdapter(t5pAdapter);

                rvPoints.addOnScrollListener(scrollListener);
            }

            progLay = findViewById(R.id.progressLayout);
            tvProg = findViewById(R.id.take5ProgressText);


            //region Media Layout
            toolbarMedia = findViewById(R.id.toolbarMedia);
            toolbarMedia.setNavigationIcon(AndroidUtils.UI.getDrawable(Take5Activity.this, R.drawable.ic_arrow_back_white_24dp));
            toolbarMedia.setNavigationOnClickListener(v -> {
                setMapDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                closeMapDrawer(GravityCompat.END);
            });

            pmbMedia = findViewById(R.id.pmdMenu);
            if (pmbMedia != null) {
                cameraSupported = AndroidUtils.Device.isCameraAvailable(Take5Activity.this);

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
                rvMediaAdapter = new MediaRvAdapter(Take5Activity.this, Collections.synchronizedList(new ArrayList<>()), mediaListener,
                        AndroidUtils.Convert.dpToPx(Take5Activity.this, 90), bitmapManager);

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
                        TtUtils.Media.openInImageViewer(Take5Activity.this, _CurrentMedia.getFilePath());
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
    protected void getSettings() {
        super.getSettings();

        options.Fix = getTtAppCtx().getDeviceSettings().getTake5FilterFix();
        options.FixType = getTtAppCtx().getDeviceSettings().getTake5FilterFixType();
        options.DopType = getTtAppCtx().getDeviceSettings().getTake5FilterDopType();
        options.DopValue = getTtAppCtx().getDeviceSettings().getTake5FilterDopValue();
        increment = getTtAppCtx().getDeviceSettings().getTake5Increment();
        takeAmount = getTtAppCtx().getDeviceSettings().getTake5NmeaAmount();

        useVib = getTtAppCtx().getDeviceSettings().getTake5VibrateOnCreate();
        useRing = getTtAppCtx().getDeviceSettings().getTake5RingOnCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_take5, menu);

        miMoveToEnd = menu.findItem(R.id.take5MenuToBottom);
        miMode = menu.findItem(R.id.take5MenuMode);
        miHideGpsInfo = menu.findItem(R.id.take5MenuGpsInfoToggle);
        miCenterPosition = menu.findItem(R.id.take5MenuCenterPositionToggle);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {

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
                break;
            }
            case R.id.take5MenuToBottom: {
                if (_Points.size() > 0) {
                    //ignoreScroll = true;
                    onStartCardMovement(enableCardFading);

                    rvPoints.smoothScrollToPosition(_Points.size() - 1);

                    moveToMapPoint(getPositionsCount() - 1);
                }
                break;
            }
            case R.id.take5MenuAddMedia: {
                openMapDrawer(GravityCompat.END);
                break;
            }
            case R.id.take5MenuGps: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.take5MenuTake5Settings: {
                startActivityForResult(new Intent(this, SettingsActivity.class)
                                .putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.POINT_TAKE5_SETTINGS_PAGE),
                        Consts.Codes.Activites.SETTINGS);
                break;
            }
            case R.id.take5MenuMode: {
                if (!mapViewMode && _Points.size() > 0 && _CurrentPoint.getOp() == OpType.SideShot && !saved) {
                    btnCancelClick(null);
                }

                mapViewMode = !mapViewMode;
                miMoveToEnd.setVisible(!mapViewMode);
                setMapGesturesEnabled(mapViewMode);
                layCardInfo.setEnabled(!mapViewMode);
                layCardInfo.setVisibility(mapViewMode ? View.GONE : View.VISIBLE);
                miCenterPosition.setVisible(mapViewMode);

                if (mapViewMode) {
                    hideCreateSS();
                } else {
                    showCreateSS();
                }

                miMode.setIcon(mapViewMode ? R.drawable.ic_add_location_white_36dp : R.drawable.ic_map_white_36dp);
                break;
            }
            case R.id.take5MenuGpsInfoToggle: {
                if (gpsInfoHidden) {
                    gpsInfoHidden = false;
                    cvGpsInfo.setVisibility(View.VISIBLE);
                    miHideGpsInfo.setTitle(R.string.menu_x_hide_gps_info);
                } else {
                    gpsInfoHidden = true;
                    cvGpsInfo.setVisibility(View.GONE);
                    miHideGpsInfo.setTitle(R.string.menu_x_show_gps_info);
                }
                break;
            }
            case R.id.take5MenuCenterPositionToggle: {
                centerPosition = !centerPosition;
                miCenterPosition.setChecked(centerPosition);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Consts.Codes.Activites.SETTINGS: {
                getTtAppCtx().getGps().startGps();

                getSettings();
                break;
            }
            case Consts.Codes.Requests.ADD_IMAGES: {
                if (data != null) {
                    List<TtImage> images = TtUtils.Media.getPicturesFromImageIntent(getTtAppCtx(), data, _CurrentPoint.getCN());

                    if (images.size() == 1) {
                        TtUtils.Media.askAndUpdateImageOrientation(Take5Activity.this, null);
                    }

                    addImages(images);
                }
                break;
            }
            case Consts.Codes.Activites.TTCAMERA: {
                if (data != null) {
                    TtImage image = TtUtils.Media.getPictureFromTtCameraIntent(data);

                    if (image == null) {
                        Toast.makeText(Take5Activity.this, "Unable to add Image", Toast.LENGTH_LONG).show();
                    } else {
                        addImage(image);
                    }
                }
                break;
            }
            case Consts.Codes.Requests.CAPTURE_IMAGE: {
                if (resultCode != RESULT_CANCELED) {
                    TtImage image = TtUtils.Media.createPictureFromUri(captureImageUri, _CurrentPoint.getCN());

                    if (image == null) {
                        Toast.makeText(Take5Activity.this, "Unable to add Image", Toast.LENGTH_LONG).show();
                    } else {
                        TtUtils.Media.askAndUpdateImageOrientation(Take5Activity.this, null);
                        addImage(image);
                    }
                }
                break;
            }
            case Consts.Codes.Requests.UPDATE_ORIENTATION: {
                if (resultCode != RESULT_CANCELED) {
                    if (data != null && data.hasExtra(Consts.Codes.Data.ORIENTATION)) {
                        DeviceOrientationEx.Orientation orientation = data.getParcelableExtra(Consts.Codes.Data.ORIENTATION);

                        if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture) {
                            TtImage image = (TtImage)_CurrentMedia;
                            image.setAzimuth(orientation.getRationalAzimuth());
                            image.setPitch(orientation.getPitch());
                            image.setRoll(orientation.getRoll());
                            onMediaUpdate();
                        }
                    }

                }
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Consts.Codes.Requests.CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Take5Activity.this, TtCameraActivity.class);

            if (_CurrentPoint != null) {
                intent.putExtra(Consts.Codes.Data.POINT_CN, _CurrentPoint.getCN());
            }

            startActivityForResult(intent, Consts.Codes.Activites.TTCAMERA);
        }
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

        if (_PrevPoint != null && !createSSVisible) {
            fabSS.setVisibility(View.VISIBLE);
            createSSVisible = true;
        }
    }

    @Override
    protected void onPause() {
        SheetLayoutEx.exitToBottomAnimation(this);
        super.onPause();
    }

    @Override
    public void finish() {
        if (validateSideShot()) {
            if (_Points != null && _Points.size() > 0) {
                if (!saved || updated) {
                    savePoint(_CurrentPoint);
                }

                setResult(Consts.Codes.Results.POINT_CREATED, new Intent().putExtra(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS, _Points.size()));
            } else {
                if (_Group != null) {
                    getTtAppCtx().getDAL().deleteGroup(_Group.getCN());
                }

                setResult(RESULT_CANCELED);
            }

            super.finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            if (validateSideShot()) {
                setupTake5();
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
                    getTtAppCtx().getDAL().insertNmeaBursts(_Bursts);

                    if (sPoint.getIndex() < _Points.size()) { //update all the points after inserted point
                        for (int i = sPoint.getIndex() + 1; i <_Points.size(); i++) {
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


    private void setupTake5() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        _PrevPoint = _CurrentPoint;
        _AddTake5 = new Take5Point();
        setupPoint(_AddTake5);

        _Bursts = new ArrayList<>();
        _UsedBursts = new ArrayList<>();

        AndroidUtils.UI.hideKeyboard(this);

        startLogging();

        fabT5.setEnabled(false);
        fabSS.setEnabled(false);

        showCancel();
        hideCommitSS();
    }

    private void setupSideShot() {
        if (!saved || updated) {
            savePoint(_CurrentPoint);
        }

        lockLastPoint(true);

        _PrevPoint = _CurrentPoint;
        _CurrentPoint = new SideShotPoint();
        setupPoint(_CurrentPoint);

        _Points.add(_CurrentPoint);

        ignoreScroll = true;

        onStartCardMovement(false);
        t5pAdapter.notifyItemInserted(_Points.size() - 1);
        rvPoints.smoothScrollToPosition(_Points.size() - 1);

        AndroidUtils.UI.hideKeyboard(this);

        showCancel();
        showCommitSS();
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


    private boolean validateSideShot() {
        if (_CurrentPoint != null && _CurrentPoint.getOp() == OpType.SideShot) {
            SideShotPoint ssp = (SideShotPoint)_CurrentPoint;

            if (ssp.getFwdAz() != null || ssp.getBkAz() != null) {
                if (ssp.getSlopeDistance() > 0) {

                    //temp adjust for map
                    TtPoint tmp;
                    for (int i = _Points.size() - 2; i > -1; i--) {
                        tmp =_Points.get(i);

                        if (tmp.getOp().isGpsType()) {
                            ssp.calculatePoint(getPolygon(), tmp);
                            addPosition(ssp);
                            break;
                        }
                    }

                    return true;
                } else {
                    Toast.makeText(Take5Activity.this, "SideShot requires a distance of greater than zero", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Take5Activity.this, "SideShot requires a forward or back azimuth", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        return true;
    }

    private void addTake5(Take5Point t5point) {
        TtPoint prevPoint = _CurrentPoint;
        _CurrentPoint = t5point;

        if (savePoint(t5point)) {
            t5point.adjustPoint(); //temp for map

            hideCancel();
            hideCommitSS();

            lockLastPoint(true);

            _PrevPoint = prevPoint;
            _Points.add(t5point.getIndex(), t5point);

            ignoreScroll = true;

            //getTtAppCtx().getTtNotifyManager().showPointAquired();

            t5pAdapter.notifyItemInserted(_Points.size() - 1);
            t5pAdapter.notifyDataSetChanged();

            onStartCardMovement(false);
            rvPoints.smoothScrollToPosition(_Points.size() - 1);

            fabT5.setEnabled(true);
            fabSS.setEnabled(true);

            if (isTrailModeEnabled()) {
                addPosition(t5point, true);
            } else {
                getTtAppCtx().getReport().writeWarn("TrailMode is disabled.", "Take5Activity:addTake5");
            }

            if (useVib) {
                AndroidUtils.Device.vibrate(this, Consts.Notifications.VIB_POINT_CREATED);
            }

            if (useRing) {
                AndroidUtils.Device.playSound(this, R.raw.ring);
            }

            if (!createSSVisible) {
                showCreateSS();
            }
        } else {
            _CurrentPoint = prevPoint;
            Toast.makeText(this, "Point failed to save", Toast.LENGTH_SHORT).show();
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
                Animation a = AnimationUtils.loadAnimation(Take5Activity.this, R.anim.push_down_out);

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

            if (holder instanceof Take5PointsEditRvAdapter.PointViewHolderEx) {
                ((Take5PointsEditRvAdapter.PointViewHolderEx)holder).setLocked(lock);
            }
        }
    }

    //region UI
    private void showCancel() {
        if (!cancelVisible) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_right_in);

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
        AndroidUtils.UI.hideKeyboard(this);

        if (cancelVisible) {
            final Animation a = AnimationUtils.loadAnimation(this, R.anim.push_left_out);

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

            final Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

            ac.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabSSCommit.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabCancel.startAnimation(a);
            fabSSCommit.startAnimation(ac);
            cancelVisible = false;
        }
    }


    private void showCommitSS() {
        if (!commitSSVisible) {
            Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_left_in);

            ac.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabSSCommit.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabSSCommit.startAnimation(ac);
            commitSSVisible = true;
        }
    }

    private void hideCommitSS() {
        if (commitSSVisible) {
            final Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

            ac.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabSSCommit.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabSSCommit.startAnimation(ac);
            commitSSVisible = false;
        }
    }


    private void showCreateSS() {
        if (!createSSVisible) {
            Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_left_in);

            ac.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fabSS.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabSS.startAnimation(ac);
            createSSVisible = true;
        }
    }

    private void hideCreateSS() {
        if (createSSVisible) {
            final Animation ac = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

            ac.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabSS.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fabSS.startAnimation(ac);
            createSSVisible = false;
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
        //rvPoints.scrollToPosition(_Points.size() - 1);
        onStopCardMovement();
    }

    @Override
    protected void onEndShowExtraGpsStatus() {
        rvPoints.setVisibility(View.INVISIBLE);
    }

    //endregion

    //region GPS
    @Override
    protected void onNmeaBurstReceived(NmeaBurst nmeaBurst) {
        super.onNmeaBurstReceived(nmeaBurst);

        if (isLogging() && nmeaBurst.isValid()) {
            TtNmeaBurst burst = TtNmeaBurst.create(_AddTake5.getCN(), false, nmeaBurst);

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

                    Position position = GeoTools.getMidPioint(positions);

                    _AddTake5.setLatitude(position.getLatitudeSignedDecimal());
                    _AddTake5.setLongitude(position.getLongitudeSignedDecimal());
                    _AddTake5.setElevation(position.getElevation());
                    _AddTake5.setRMSEr(dRMSEr);
                    _AddTake5.setAndCalc(x, y, position.getElevation(), getPolygon());

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
            case Unknown:
                break;
        }
    }

    @Override
    public void nmeaBurstValidityChanged(boolean burstsValid) {
        super.nmeaBurstValidityChanged(burstsValid);

        String message = isReceivingNmea() ? "Now receiving NMEA data." : "No longer receiving NMEA data.";
        Toast.makeText(Take5Activity.this, message, Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region Media
    private void saveMedia() {
        if (_MediaUpdated && _CurrentMedia != null) {
            if (!getTtAppCtx().getMAL().updateMedia(_CurrentMedia)) {
                Toast.makeText(Take5Activity.this,
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

        if (delete) {
            File file = new File(media.getFilePath());
            file.deleteOnExit();
        }

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
            if (getTtAppCtx().getMAL().insertMedia(picture)) {
                mediaSelectionIndex = TtUtils.Media.getMediaIndex(picture, rvMediaAdapter.getItems());
                loadImageToList(picture);
            } else {
                Toast.makeText(Take5Activity.this, "Error saving picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addImages(final List<TtImage> pictures) {
        if (pictures.size() > 0) {
            int error = 0;

            Collections.sort(pictures, TtUtils.Media.PictureTimeComparator);

            for (int i = 0; i <pictures.size(); i++) {
                if (!getTtAppCtx().getMAL().insertMedia(pictures.get(i))) {
                    pictures.remove(i--);
                    error++;
                }
            }

            mediaSelectionIndex = TtUtils.Media.getMediaIndex(pictures.get(0), rvMediaAdapter.getItems());

            for (TtImage p : pictures) {
                loadImageToList(p);
            }

            if (error > 0) {
                Toast.makeText(Take5Activity.this, String.format("Error saving %d pictures", pictures.size()), Toast.LENGTH_LONG).show();
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

                        Collections.sort(pictures, TtUtils.Media.PictureTimeComparator);
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

        getTtAppCtx().getMAL().loadImage(picture, new MediaAccessLayer.SimpleMalListener() {
            @Override
            public void imageLoaded(TtImage image, View view, Bitmap bitmap) {
                addImageToList(picture, true, bitmap);
            }

            @Override
            public void loadingFailed(TtImage image, View view, String reason) {
                addInvalidImagesToList(picture);
            }
        });
    }

    private void addInvalidImagesToList(final TtImage picture) {
        Bitmap bitmap = BitmapFactory.decodeResource(Take5Activity.this.getResources(), R.drawable.ic_error_outline_black_48dp);
        if (bitmap != null) {
            addImageToList(picture, false, bitmap);
        }
    }

    private void addImageToList(final TtImage picture, boolean isValid, final Bitmap loadedImage) {
        if (picture.getPointCN().equals(_CurrentPoint.getCN())) {
            if (isValid) {
                bitmapManager.put(picture.getCN(), picture.getFilePath(), AndroidUtils.UI.scaleMinBitmap(loadedImage, getBitmapHeight(), false), scaleOptions);
            } else {
                bitmapManager.put(picture.getCN(), Integer.toString(R.drawable.ic_error_outline_black_48dp), AndroidUtils.UI.scaleMinBitmap(loadedImage, getBitmapHeight(), false), scaleOptions, true);
            }

            try {
                semaphore.acquire();

                final int order = TtUtils.Media.getMediaIndex(picture, rvMediaAdapter.getItems());

                Take5Activity.this.runOnUiThread(() -> {
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


    private Runnable onMediaChanged = new Runnable() {
        @Override
        public void run() {
            Take5Activity.this.runOnUiThread(() -> {
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
            toolbarMedia.setTitle(StringEx.format("Media (%d)", mediaCount));
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
    //endregion

    //region Controls
    public void btnTake5Click(View view) {
        if (isReceivingNmea()) {
            if (validateSideShot()) {
                setupTake5();
            }
        } else {
            Toast.makeText(Take5Activity.this, "Currently not receiving NMEA data.", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnSideShotClick(View view) {
        if (validateSideShot()) {
            if (isGpsExtraInfoVisible())
                hideExtraGpsStatus();

            setupSideShot();
        }
    }

    public void btnCommitSideShotClick(View view) {
        if (validateSideShot()) {
            savePoint(_CurrentPoint);
            lockLastPoint(true);
            hideCancel();
            hideCommitSS();
        }
    }

    public void btnCancelClick(View view) {
        hideCancel();
        hideCommitSS();

        if (isLogging()) {
            killAcquire = true;
            stopLogging();
            _Bursts = new ArrayList<>();
            _UsedBursts = new ArrayList<>();

            fabT5.setEnabled(true);
            fabSS.setEnabled(true);
        } else if (_Points.size() > 0 && _CurrentPoint.getOp() == OpType.SideShot) {
            _Points.remove(_Points.size() - 1);

            ignoreScroll = true;

            t5pAdapter.notifyItemRemoved(_Points.size());

            if (_Points.size() > 0) {
                onStartCardMovement(false);
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


    private void onMediaUpdate() {
        setMediaUpdated(true);

        if (listeners.containsKey(_CurrentMedia.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentMedia.getCN());
            if (listener != null) {
                listener.onMediaUpdated(_CurrentMedia);
            }
        }
    }

    private void onMediaUpdate(TtMedia media) {
        if (listeners.containsKey(media.getCN())) {
            PointMediaListener listener = listeners.get(media.getCN());
            if (listener != null) {
                listener.onMediaUpdated(media);
            }
        }
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
