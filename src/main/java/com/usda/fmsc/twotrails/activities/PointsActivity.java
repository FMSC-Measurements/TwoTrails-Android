package com.usda.fmsc.twotrails.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableAdapterEx;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.utilities.ResourceBitmapProvider;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.SheetFab;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.activities.base.PointCollectionActivity;
import com.usda.fmsc.twotrails.adapters.MediaPagerAdapter;
import com.usda.fmsc.twotrails.adapters.MediaRvAdapter;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.data.TwoTrailsMediaSchema;
import com.usda.fmsc.twotrails.dialogs.LatLonDialog;
import com.usda.fmsc.twotrails.dialogs.MoveToPointDialog;
import com.usda.fmsc.twotrails.dialogs.PointEditorDialog;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.points.BasePointFragment;
import com.usda.fmsc.twotrails.fragments.points.GPSPointFragment;
import com.usda.fmsc.twotrails.fragments.points.QuondamPointFragment;
import com.usda.fmsc.twotrails.fragments.points.TraversePointFragment;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.AdjustingException;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.rangefinder.RangeFinderService;
import com.usda.fmsc.twotrails.rangefinder.TtRangeFinderData;
import com.usda.fmsc.twotrails.ui.MSFloatingActionButton;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.utilities.StringEx;

import jp.wasabeef.recyclerview.animators.FadeInAnimator;

@SuppressWarnings({"RestrictedApi"})
public class PointsActivity extends PointCollectionActivity implements PointMediaController, RangeFinderService.Listener {
    private final HashMap<String, PointMediaListener> listeners = new HashMap<>();

    private MenuItem miLock, miLink, miReset, miEnterLatLon, miNmeaRecalc, miDelete, miGoto;//, miMovePoint;
    private SheetLayoutEx slexAqr, slexCreate;
    private FloatingActionButton fabAqr;
    private MSFloatingActionButton fabMenu;
    private SheetFab fabSheet;
    private ImageView ivFullscreen;

    private SlidingUpPanelLayout slidingLayout;
    private View pmdScroller;
    private TextView tvPmdTitle;

    private ViewPager pointViewPager, mediaViewPager;
    private PointsPagerAdapter pointSectionsPagerAdapter;
    private MediaPagerAdapter mediaPagerAdapter;

    private RecyclerViewEx rvMedia;
    private MediaRvAdapter rvMediaAdapter;
    private PopupMenuButton pmbMedia;

    private boolean ignorePointChange, ignoreMediaChange, adjust, menuCreated, aqrVisible = false;
    private OpType currentAqrOp = OpType.GPS, createOpType;

    private ArrayList<TtPoint> _Points;
    private HashMap<String, TtPolygon> _Polygons;
    private HashMap<String, TtMetadata> _Metadata;
    private TtPoint _CurrentPoint, _deletePoint;
    private TtPolygon _CurrentPolygon;
    private TtMetadata _CurrentMetadata;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _PointUpdated, _PointLocked, _MediaUpdated, overrideHalfTrav;
    private String addedPoint;

    private boolean autoSetTrav, autoSetAzFwd, autoSetAz, warnedTravNotFinished;


    private BitmapManager bitmapManager;

    private TtMedia _CurrentMedia, _BackupMedia;

    private float collapsedHeight, anchoredPercent;
    private int height, bitmapHeight;
    private boolean mediaLoaded;
    private int mediaCount, mediaSelectionIndex;

    private final Semaphore semaphore = new Semaphore(1);
    private final PostDelayHandler mediaLoaderDelayedHandler = new PostDelayHandler(500);


    public HashMap<String, TtPolygon> getPolygons() {
        if (_Polygons == null && getTtAppCtx().hasDAL()) {
            _Polygons = getTtAppCtx().getDAL().getPolygonsMap();
            if (_Polygons == null) throw new RuntimeException("getPolygons Failed");
        }

        return _Polygons;
    }

    public HashMap<String, TtMetadata> getMetadata() {
        if (_Metadata == null || _Metadata.size() == 0) {
            _Metadata = getTtAppCtx().getDAL().getMetadataMap();
            if (_Metadata == null) throw new RuntimeException("getMetadataMap Failed");
        }

        return _Metadata;
    }


    //region Listeners
    private final ComplexOnPageChangeListener onPointPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignorePointChange) {
                savePoint();
                saveMedia();

                _CurrentIndex = position;
                _CurrentPoint = getPointAtIndex(_CurrentIndex);
                _CurrentMetadata = getMetadata().get(_CurrentPoint.getMetadataCN());
                updateButtons();

                loadMedia(_CurrentPoint, slidingLayout != null && slidingLayout.getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED);
            }

            ignorePointChange = false;

            AndroidUtils.UI.hideKeyboard(PointsActivity.this);
        }

        @Override
        public void onPageChanged() {
            super.onPageChanged();

            if (_deleteIndex > INVALID_INDEX) {
                boolean halfFinishedTrav = false;

                if (!overrideHalfTrav && _deletePoint.isTravType()) {
                    TravPoint tp = (TravPoint)_deletePoint;

                    if ((tp.getFwdAz() != null || tp.getBkAz() != null) && tp.getSlopeDistance() <= 0) {
                        halfFinishedTrav = true;
                    }
                }

                if (!_deletePoint.getPolyCN().equals(_CurrentPolygon.getCN()) || overrideHalfTrav || (!halfFinishedTrav && getTtAppCtx().getDeviceSettings().getDropZeros())) {
                    deleteWithoutMoving();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(PointsActivity.this);

                    dialog.setTitle("Invalid Point");

                    if (halfFinishedTrav) {
                        dialog.setMessage(String.format(Locale.getDefault(), "The %s point %d has a partial value. Would you like to finish or delete the point.",
                                _deletePoint.getOp().toString(),
                                _deletePoint.getPID()));
                    } else {
                        dialog.setMessage(String.format(Locale.getDefault(), "The point %d has no value. Would you like to edit or delete the point.",
                                _deletePoint.getPID()));
                    }

                    dialog.setPositiveButton(getString(R.string.str_delete), (dialogInterface, i) -> deleteWithoutMoving());

                    dialog.setNegativeButton(getString(R.string.str_edit), (dialogInterface, i) -> {
                        ignorePointChange = true;
                        pointViewPager.setCurrentItem(_deleteIndex);

                        _CurrentPoint = _deletePoint;
                        _CurrentIndex = _deleteIndex;

                        lockPoint(false);

                        _deleteIndex = INVALID_INDEX;
                        _deletePoint = null;
                    });

                    dialog.show();
                }
            }
        }
    };

    private final ComplexOnPageChangeListener onMediaPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignoreMediaChange && rvMediaAdapter.getItemCountEx() > 0) {
                saveMedia();

                setCurrentMedia(rvMediaAdapter.getItem(position));
                rvMediaAdapter.selectItem(position);
                rvMedia.smoothScrollToPosition(position);
                onLockChange();
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
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                mediaViewPager.setCurrentItem(adapterPosition, true);
            }
        }
    };

    private final SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            if (slideOffset < anchoredPercent) {
                pmdScroller.setTranslationY(height - slideOffset * height + collapsedHeight - 1);
            }
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            switch (newState) {
                case EXPANDED:
                    if (!mediaLoaded && _CurrentPoint != null) {
                        loadMedia(_CurrentPoint, true);
                    }

                    fabAqr.hide();
                    fabMenu.hide();
                    aqrVisible = false;

                    setCurrentMedia(_CurrentMedia);

                    pmdScroller.setTranslationY(height - collapsedHeight - 1);
                    break;
                case ANCHORED:
                    if (!mediaLoaded && _CurrentPoint != null) {
                        loadMedia(_CurrentPoint, true);
                    }
                case COLLAPSED:
                case HIDDEN:
                    if (!_PointLocked && _CurrentPoint != null && (_CurrentPoint.getOp() == OpType.GPS || _CurrentPoint.getOp() == OpType.WayPoint)) {
                        fabAqr.show();
                        aqrVisible = true;
                    }

                    fabMenu.show();

                    setMediaTitle(null);
                    break;
                case DRAGGING:
                    break;
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
                if (AndroidUtils.Device.isFullOrientationAvailable(PointsActivity.this)) {
                    if (getTtAppCtx().getDeviceSettings().getUseTtCameraAsk()) {
                        DontAskAgainDialog dialog = new DontAskAgainDialog(PointsActivity.this,
                                DeviceSettings.USE_TTCAMERA_ASK,
                                DeviceSettings.USE_TTCAMERA,
                                getTtAppCtx().getDeviceSettings().getPrefs());

                        dialog.setMessage(PointsActivity.this.getString(R.string.points_camera_diag))
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
                deleteMedia();
            }

            return false;
        }
    };
    //endregion


    //region Requests

    private final ActivityResultLauncher<Intent> addInsertPointsOnResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        slexCreate.contractFab();

        if (result.getResultCode() == Consts.Codes.Results.POINT_CREATED) {
            _Points = getTtAppCtx().getDAL().getPointsInPolygon(_CurrentPolygon.getCN());

            onPointsChanged();

            int numberOfPoints = _Points.size();

            if (numberOfPoints > 0 && menuCreated) {
                AndroidUtils.UI.enableMenuItem(miGoto);
                AndroidUtils.UI.enableMenuItem(miLock);
            }

            moveToPoint(numberOfPoints - 1);

            adjust = true;
        }
    });

    private void addOrInsertPoints(Intent data) {
        addInsertPointsOnResult.launch(data);
    }


    private final ActivityResultLauncher<Intent> acquireAndCalculateOnResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        slexAqr.contractFab();

        if (result.getResultCode() == Consts.Codes.Results.POINT_CREATED) {
            if (result.getData() != null && result.getData().hasExtra(Consts.Codes.Data.POINT_DATA)) {
                GpsPoint point = result.getData().getParcelableExtra(Consts.Codes.Data.POINT_DATA);

                updatePoint(point);
                onPointUpdate();
            }
        }
    });

    private void acquireAndOrCalculate(Intent data) {
        acquireAndCalculateOnResult.launch(data);
    }
    //endregion


    //region Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getTtAppCtx().hasDAL()) {
            setResult(Consts.Codes.Results.NO_DAL);
            finish();
        }

        setContentView(R.layout.activity_points);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        setUseExitWarning(true);

        final TtPolygon[] polyArray = getPolygons().values().toArray(new TtPolygon[0]);
        Arrays.sort(polyArray);

        _Points = new ArrayList<>();
        _CurrentIndex = INVALID_INDEX;

        pointSectionsPagerAdapter = new PointsPagerAdapter(getSupportFragmentManager());
        pointSectionsPagerAdapter.saveFragmentStates(false);

        pointViewPager = findViewById(R.id.pointsViewPager);

        if (pointViewPager != null) {
            pointViewPager.setAdapter(pointSectionsPagerAdapter);
            pointViewPager.addOnPageChangeListener(onPointPageChangeListener);
        }



        bitmapManager = new BitmapManager(new ResourceBitmapProvider(getTtAppCtx()), getTtAppCtx().getMAL());

        //region Main Buttons
        fabAqr = findViewById(R.id.pointsFabAqr);
        fabMenu = findViewById(R.id.pointsFabMenu);
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
                        case InertialStart:
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

        final AppCompatSpinner spinnerPoly = findViewById(R.id.pointsToolBarSpinnerPoly);
        if (spinnerPoly != null) {
            spinnerPoly.setAdapter(polyAdapter);

            String lastPolyCN = getTtAppCtx().getProjectSettings().getLastEditedPolyCN();
            if (getPolygons().containsKey(lastPolyCN)) {
                TtPolygon tmp;
                boolean lastSet = false;
                for (int i = 0; i < getPolygons().size(); i++) {
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
        }
        //endregion

        //region Transitions
        slexAqr = findViewById(R.id.pointsSLExAqr);
        if (slexAqr != null) {
            slexAqr.setFab(fabAqr);
            slexAqr.setFabAnimationEndListener(() -> acquireGpsPoint(_CurrentPoint, null));
        }

        slexCreate = findViewById(R.id.pointsSLExCreate);
        if (slexCreate != null) {
            slexCreate.setFab(fabMenu);
            slexCreate.setFabAnimationEndListener(() -> {

                TtPoint point = null;

                if (TtUtils.Points.pointHasValue(_CurrentPoint)) {
                    point = _CurrentPoint;
                } else if (_CurrentIndex > 0) {
                    point = _Points.get(_CurrentIndex - 1);
                }

                if (createOpType == OpType.Take5) {
                    startTake5Activity(point);
                } else if (createOpType == OpType.Walk) {
                    startWalkActivity(point);
                } else if (createOpType == OpType.InertialStart) {
                    startInertialActivity(point);
                }

                createOpType = null;
            });
        }
        //endregion

        //region Media Layouts
        pmbMedia = findViewById(R.id.pmdMenu);
        if (pmbMedia != null) {
            if (!AndroidUtils.Device.isCameraAvailable(PointsActivity.this)) {
                pmbMedia.setItemEnabled(R.id.ctx_menu_capture, false);
            }

            pmbMedia.setListener(menuPopupListener);
        }

        tvPmdTitle = findViewById(R.id.pmdTitle);

        slidingLayout = findViewById(R.id.pointSlidingPanelLayout);
        pmdScroller = findViewById(R.id.pmdScroller);
        final View layMDH = findViewById(R.id.pmdHeader);
        rvMedia = findViewById(R.id.pmdRvMedia);
        if (rvMedia != null) {
            rvMedia.setViewHasFooter(true);
            rvMedia.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this, LinearLayoutManager.HORIZONTAL, false));
            rvMedia.setHasFixedSize(true);
            rvMedia.setItemAnimator(new FadeInAnimator());
        }

        mediaViewPager = findViewById(R.id.pmdViewPager);
        if (slidingLayout != null && layMDH != null && pmdScroller != null && mediaViewPager != null) {
            collapsedHeight = AndroidUtils.Convert.dpToPx(this, 50);

            ViewTreeObserver vto = slidingLayout.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    collapsedHeight = layMDH.getHeight();
                    height = pointViewPager.getHeight();
                    anchoredPercent = (float) pmdScroller.getHeight() / height;
                    slidingLayout.setAnchorPoint(anchoredPercent);

                    bitmapHeight = mediaViewPager.getHeight();
                    bitmapManager.setImageLimitSize(bitmapHeight);

                    rvMediaAdapter = new MediaRvAdapter(PointsActivity.this, Collections.synchronizedList(new ArrayList<>()), mediaListener,
                            pmdScroller.getHeight() - AndroidUtils.Convert.dpToPx(PointsActivity.this, 10), bitmapManager);

                    rvMedia.setAdapter(rvMediaAdapter);

                    ViewTreeObserver obs = slidingLayout.getViewTreeObserver();
                    obs.removeOnGlobalLayoutListener(this);

                    mediaPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager(), rvMediaAdapter);
                    mediaViewPager.setAdapter(mediaPagerAdapter);
                    mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);

                    rvMediaAdapter.setListener(() -> mediaPagerAdapter.notifyDataSetChanged());

                    loadMedia(_CurrentPoint, false);
                }
            });

            slidingLayout.addPanelSlideListener(panelSlideListener);

            layMDH.setOnClickListener(v -> {
                switch (slidingLayout.getPanelState()) {

                    case EXPANDED:
                    case COLLAPSED:
                        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                        break;
                    case ANCHORED:
                    case HIDDEN:
                        if (mediaLoaded || mediaCount == 0) {
                            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        }
                        break;
                }
            });
        }


        ivFullscreen = findViewById(R.id.pmdIvFullscreen);
        if (ivFullscreen != null) {
            ivFullscreen.setOnClickListener(v -> {
                if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture &&
                        (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                                slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
                    TtUtils.Media.openInImageViewer(getTtAppCtx(), _CurrentMedia);
                }
            });

            AndroidUtils.UI.setContentDescToast(ivFullscreen, "View in Fullscreen");
        }
        //endregion
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!getTtAppCtx().hasDAL()) {
            setResult(Consts.Codes.Results.NO_DAL);
            finish();
        }

        _deletePoint = null;
        _deleteIndex = INVALID_INDEX;
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePoint();
        saveMedia();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getTtAppCtx().hasDAL()) {
            savePoint();
            saveMedia();

            if (adjust) {
                getTtAppCtx().adjustProject(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenuEx(Menu menu) {
        inflateMenu(R.menu.menu_points, menu);

        miLock = menu.findItem(R.id.pointsMenuLock);
        miLink = menu.findItem(R.id.pointsMenuLink);
        //miMovePoint = menu.findItem(R.id.pointsMenuMovePoint);
        miReset = menu.findItem(R.id.pointsMenuReset);
        miEnterLatLon = menu.findItem(R.id.pointsMenuEnterLatLon);
        miNmeaRecalc = menu.findItem(R.id.pointsMenuRecalcNmea);
        miDelete = menu.findItem(R.id.pointsMenuDelete);
        miGoto = menu.findItem(R.id.pointsMenuGotoPoint);

        if (_Points.size() < 1) {
            AndroidUtils.UI.disableMenuItem(miGoto);
            AndroidUtils.UI.disableMenuItem(miLock);
        }

        menuCreated = true;
        updateButtons();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.pointsMenuLink) {
            jumpToQuondam(_CurrentPoint);
        } else if (itemId == R.id.pointsMenuLock) {
            lockPoint(!_PointLocked);
        } else if (itemId == R.id.pointsMenuSettings) {
            openSettings();
        } else if (itemId == R.id.pointsMenuGotoPoint) {
            if (_Points.size() > 0) {
                MoveToPointDialog mdialog = new MoveToPointDialog();

                mdialog.setOnItemClick((parent, view, position, id) -> moveToPoint(position));

                mdialog.setFirstListener((dialog, which) -> moveToPoint(0));

                mdialog.setLastListener((dialog, which) -> moveToPoint(_Points.size() - 1));

                mdialog.setNegativeButton("Cancel", null);

                mdialog.setItems(_Points, _CurrentIndex);
                mdialog.setTitle("Jump To Point");

                mdialog.show(getSupportFragmentManager(), "JUMP_POINTS");
            }
        } else if (itemId == R.id.pointsMenuMovePoint) {//TODO move points around in polygon
            Toast.makeText(this, "Unimplemented", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.pointsMenuDelete) {
            if (!_PointLocked) {
                anchorMediaIfExpanded();

                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage(String.format("Delete Point %d", _CurrentPoint.getPID()));

                alert.setPositiveButton(R.string.str_delete, (dialog, which) -> {
                    overrideHalfTrav = true;

                    AnimationCardFragment card = ((AnimationCardFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex));

                    card.setVisibilityListener(new AnimationCardFragment.VisibilityListener() {
                        @Override
                        public void onHidden() {
                            new Handler().post(() -> {
                                if (_CurrentIndex == 0 && _Points.size() < 2) { //only 1 point in poly
                                    deletePoint(_CurrentPoint, _CurrentIndex);

                                    _CurrentPoint = null;
                                    _CurrentIndex = INVALID_INDEX;
                                    lockPoint(true);
                                    AndroidUtils.UI.disableMenuItem(miLock);
                                    hideAqr();
                                } else if (_CurrentIndex < _Points.size() - 1) { //point is not at the end
                                    _deleteIndex = _CurrentIndex;
                                    _deletePoint = _CurrentPoint;

                                    moveToPoint(_CurrentIndex + 1);
                                } else if (_CurrentIndex == _Points.size() - 1) { //point it at the end
                                    _deleteIndex = _CurrentIndex;
                                    _deletePoint = _CurrentPoint;

                                    moveToPoint(_CurrentIndex - 1);
                                }
                            });
                        }

                        @Override
                        public void onVisible() {

                        }
                    });

                    card.hideCard();
                });

                alert.setNeutralButton(R.string.str_cancel, null);

                alert.create().show();
            }
        } else if (itemId == R.id.pointsMenuReset) {
            anchorMediaIfExpanded();
            resetPoint();
        } else if (itemId == R.id.pointsMenuEnterLatLon) {
            if (_CurrentPoint.isGpsType()) {
                anchorMediaIfExpanded();

                LatLonDialog dialog = LatLonDialog.newInstance((GpsPoint) _CurrentPoint);

                dialog.setOnEditedListener((cn, lat, lon) -> {
                    if (_CurrentPoint.getCN().equals(cn)) {
                        UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(lat, lon, _CurrentMetadata.getZone());

                        GpsPoint point = (GpsPoint) _CurrentPoint;

                        point.setLatitude(lat);
                        point.setLongitude(lon);

                        point.setUnAdjX(coords.getX());
                        point.setUnAdjY(coords.getY());

                        onPointUpdate();
                    }
                });

                dialog.show(getSupportFragmentManager(), "ENTER_LATLON");
            }
        } else if (itemId == R.id.pointsMenuRecalcNmea) {
            anchorMediaIfExpanded();
            calculateGpsPoint();
        } else if (itemId == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (fabSheet.isSheetVisible()) {
            fabSheet.hideSheet();
        } else if (slidingLayout != null && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        } else if (_Points.size() > 2 && _Points.get(_Points.size() - 1).getOp() == OpType.Traverse && !warnedTravNotFinished) {
            Toast.makeText(PointsActivity.this, "Warning: Traverse is not completed.", Toast.LENGTH_LONG).show();
            warnedTravNotFinished = true;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        warnedTravNotFinished = false;
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onAppSettingsUpdated() {
        super.onAppSettingsUpdated();

        if (getTtAppCtx().getDeviceSettings().isRangeFinderConfigured()) {
            getTtAppCtx().getRF().startRangeFinder();
        }
    }
    //endregion


    //region Save Delete Create Reset
    private boolean savePoint() {
        if (_CurrentPoint != null) {
            boolean phv = TtUtils.Points.pointHasValue(_CurrentPoint);

            if (_PointUpdated && phv) {
                try {
                    boolean updated = false;

                    TtPoint oldPoint = _Points.get(_CurrentIndex);

                    if (TtUtils.Points.pointHasValue(oldPoint)) {
                        if (!TtUtils.Points.pointHasChanges(_CurrentPoint, oldPoint)) {
                            setPointUpdated(false);
                            return true;
                        }

                        getTtAppCtx().getDAL().updatePoint(_CurrentPoint, _Points.get(_CurrentIndex));
                        updated = true;
                    } else {
                        getTtAppCtx().getDAL().insertPoint(_CurrentPoint);
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
                                //remove link from old linked point in list
                                tmp = getPoint(oldQndm.getParentCN());

                                if (tmp != null) {
                                    tmp.removeQuondamLink(currQndm.getCN());
                                    onPointUpdate(tmp);
                                }
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
        int deletedIndex = _deleteIndex;

        if (deletePoint(_deletePoint, _deleteIndex)) {
            lockPoint(true);

            if (deletedIndex < _CurrentIndex && _CurrentIndex > 0) {
                moveToPoint(_CurrentIndex - 1);
            }

            _deleteIndex = INVALID_INDEX;
            _deletePoint = null;
        } else {
            Toast.makeText(this, "Error deleting point.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deletePoint(TtPoint point, int index) {
        try {
            if (point != null) {
                overrideHalfTrav = false;

                getTtAppCtx().getDAL().deletePointSafe(point);

                if (point.getOp() == OpType.Quondam) {
                    QuondamPoint qp = (QuondamPoint) point;
                    if (qp.hasParent() && qp.getParentPoint().getPolyCN().equals(_CurrentPolygon.getCN())) {
                        TtPoint tmp = getPoint(qp.getParentCN());
                        if (tmp != null) {
                            tmp.removeQuondamLink(qp.getCN());
                            onPointUpdate(tmp);
                        }
                    }
                }

                if (point.hasQuondamLinks()) {
                    for (String qndmCN : point.getLinkedPoints()) {
                        TtPoint convertedPoint = getTtAppCtx().getDAL().getPointByCN(qndmCN);

                        for (int i = 0; i < _Points.size(); i++) {
                            if (_Points.get(i).getCN().equals(qndmCN)) {
                                _Points.set(i, convertedPoint);
                            }
                        }
                    }
                }

                if (index > INVALID_INDEX) {
                    _Points.remove(index);

                    ArrayList<TtPoint> update = new ArrayList<>();
                    for (int i = index; i < _Points.size(); i++) {
                        TtPoint riPoint = _Points.get(i);
                        riPoint.setIndex(i);
                        update.add(riPoint);
                    }
                    getTtAppCtx().getDAL().updatePoints(update);

                    onPointsChanged();
                } else {
                    _deleteIndex = INVALID_INDEX;
                    _deletePoint = null;
                }

                setPointUpdated(false);

                adjust = true;
            }
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PointsActivity:deletePoint");
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
        if (_CurrentPoint != null && !TtUtils.Points.pointHasValue(_CurrentPoint)) {
            if (_CurrentPoint.getOp() == op) {
                if (op.isTravType()) {
                    Toast.makeText(PointsActivity.this, "Point must have at least a Forward or Backward Azimuth and a Distance", Toast.LENGTH_SHORT).show();
                } else if (op.isGpsType()) {
                    Toast.makeText(PointsActivity.this, "Point must have a X, Y and Z value", Toast.LENGTH_SHORT).show();
                } else if (op == OpType.Quondam) {
                    Toast.makeText(PointsActivity.this, "Quondam must have a Parent Point selected", Toast.LENGTH_SHORT).show();
                }
            } else if ((!_CurrentPoint.isGpsType() || (_CurrentPoint.getOp() == OpType.Quondam && !((QuondamPoint)_CurrentPoint).getParentOp().isGpsType()))
                    && op.isTravType()) {
                Toast.makeText(PointsActivity.this,
                        String.format("A %s cannot be the first point in a polygon. You must have a valid GPS Type point before it.", op),
                        Toast.LENGTH_LONG).show();
            } else {
                _deletePoint = _CurrentPoint;

                final BasePointFragment fragment = (BasePointFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex);

                fragment.setVisibilityListener(new BasePointFragment.VisibilityListener() {
                    @Override
                    public void onHidden() {

                        if (deletePoint(_deletePoint, INVALID_INDEX)) {
                            _Points.remove(_CurrentIndex);
                            _CurrentIndex--;

                            if (_CurrentIndex > INVALID_INDEX) {
                                _CurrentPoint = getPointAtIndex(_CurrentIndex);
                            } else {
                                _CurrentPoint = null;
                            }

                            onPointsChanged();
                        } else {
                            throw new RuntimeException("Unable to delete point.");
                        }

                        createPoint(op);

                        if (_CurrentIndex < _Points.size() && _CurrentIndex > INVALID_INDEX) {
                            BasePointFragment frag = ((BasePointFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex));
                            if (frag != null) {
                                frag.showCard();
                            }
                        }
                    }

                    @Override
                    public void onVisible() {

                    }
                });
                fragment.hideCard();
            }
        } else {
            savePoint();
            saveMedia();

            if (op.isTravType() && _CurrentIndex < 0) {
                Toast.makeText(PointsActivity.this,
                        String.format("A %s cannot be the first point in a polygon. Take a GPS Type point first.", op),
                        Toast.LENGTH_LONG).show();
                return;
            }

            TtPoint newPoint = TtUtils.Points.createNewPointByOpType(op);
            newPoint.setCN(java.util.UUID.randomUUID().toString());

            if (_CurrentPoint != null) {
                if (op != OpType.WayPoint) {
                    if (_CurrentPoint.getOp() == OpType.WayPoint) {
                        boolean onBnd = true;
                        for (int i = _CurrentIndex - 1; i > -1; i--) {
                            TtPoint p = _Points.get(i);
                            if (p.getOp() != OpType.WayPoint) {
                                onBnd = p.isOnBnd();
                                break;
                            }
                        }

                        newPoint.setOnBnd(onBnd);
                    } else {
                        newPoint.setOnBnd(_CurrentPoint.isOnBnd());
                    }
                } else {
                    newPoint.setOnBnd(false);
                }
            } else {
                newPoint.setOnBnd(true);
            }

            newPoint.setPolyCN(_CurrentPolygon.getCN());
            newPoint.setPolyName(_CurrentPolygon.getName());

            newPoint.setGroupCN(Consts.EmptyGuid);
            newPoint.setGroupName(Consts.Defaults.MainGroupName);

            if (_CurrentMetadata != null) {
                newPoint.setMetadataCN(_CurrentMetadata.getCN());
            } else {
                newPoint.setMetadataCN(Consts.EmptyGuid);
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
                ignorePointChange = true;
                onPointsChanged();
                moveToPoint(_CurrentIndex);
            } catch (Exception e) {
                getTtAppCtx().getReport().writeError(e.getMessage(), "PointsActivity:createPoint(OpType)", e.getStackTrace());
                e.printStackTrace();
            }

            AndroidUtils.UI.enableMenuItem(miLock);
            lockPoint(false);
            adjust = true;
        }
    }

    private void updatePointIndexes(final int startIndex) {
        new Thread(() -> {
            ArrayList<TtPoint> tmpPoints = new ArrayList<>();

            for (int i = startIndex; i < _Points.size(); i++) {
                final TtPoint tmp = _Points.get(i);

                if (tmp.getIndex() != i) {
                    tmp.setIndex(i);
                    tmpPoints.add(tmp);

                    //update the fragments before and after the current point
                    if (i > _CurrentIndex - 2 || i < _CurrentIndex + 2) {
                        new Handler(Looper.getMainLooper()).post(() -> onPointUpdate(tmp));
                    }
                }
            }

            if (tmpPoints.size() > 0) {
                getTtAppCtx().getDAL().updatePoints(tmpPoints);
            }
        }).start();
    }

    private void resetPoint() {
        if (_PointUpdated) {
            new AlertDialog.Builder(this)
            .setTitle(String.format(Locale.getDefault(), "Reset Point %d", _CurrentPoint.getPID()))
            .setMessage(getString(R.string.points_reset_diag))
            .setPositiveButton("Reset", (dialogInterface, i) -> {
                _CurrentPoint = getPointAtIndex(_CurrentIndex);
                onPointUpdate(_CurrentPoint);
                updateButtons();
                setPointUpdated(false);
                lockPoint(false);
            })
            .setNeutralButton(getString(R.string.str_cancel), null)
            .show();
        }
    }


    private void saveMedia() {
        if (_MediaUpdated && _CurrentMedia != null) {
            if (!getTtAppCtx().getMAL().updateMedia(_CurrentMedia)) {
                Toast.makeText(PointsActivity.this,
                        String.format("Unable to save %s", _CurrentMedia.getMediaType().toString()),
                        Toast.LENGTH_LONG
                ).show();
            } else {
                setMediaUpdated(false);
            }
        }
    }

    private void removeMedia(TtMedia media) {
        List<TtMedia> mediaList = rvMediaAdapter.getItems();
        int index = mediaList.indexOf(media);

        getTtAppCtx().getMAL().deleteMedia(media);

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
                loadImageToAdapter(picture);
            } else {
                Toast.makeText(PointsActivity.this, "Error saving picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addImages(final List<TtImage> pictures) {
        if (pictures.size() > 0) {
            int error = 0;

            pictures.sort(TtUtils.Media.PictureTimeComparator);

            for (int i = 0; i <pictures.size(); i++) {
                if (!getTtAppCtx().getMAL().insertImage(pictures.get(i))) {
                    pictures.remove(i--);
                    error++;
                }
            }

            mediaSelectionIndex = TtUtils.Media.getMediaIndex(pictures.get(0), rvMediaAdapter.getItems());

            for (TtImage p : pictures) {
                loadImageToAdapter(p);
            }

            if (error > 0) {
                Toast.makeText(PointsActivity.this, String.format(Locale.getDefault(), "Error saving %d picture%s", pictures.size(), pictures.size() > 1 ? "s" : ""), Toast.LENGTH_LONG).show();
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
                        onMediaUpdated();
                        setMediaUpdated(false);
                    })
                    .setNeutralButton(getString(R.string.str_cancel), null)
                    .show();
        }
    }

    private void deleteMedia() {
        if (!_PointLocked && _CurrentMedia != null) {
            removeMedia(_CurrentMedia);
        }
    }


    @Override
    protected void onImageOrientationUpdated(TtImage image) {
        onMediaUpdated(image);
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


    //region Get Move
    private TtPoint getPointAtIndex(int index) {
        TtPoint point = null;

        if (index > INVALID_INDEX && index < _Points.size()) {
            point = TtUtils.Points.clonePoint(_Points.get(index));
        }

        return point;
    }


    protected TtPoint getPoint(String cn) {
        for (TtPoint point : _Points) {
            if (point.getCN().equals(cn)) {
                return TtUtils.Points.clonePoint(point);
            }
        }

        return null;
    }

    private void moveToPoint(TtPoint point) {
        if (!_CurrentPolygon.getCN().equals(point.getPolyCN())) {
            changePolygon(getPolygons().get(point.getPolyCN()));
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
            _CurrentPoint = getPointAtIndex(index);
            _CurrentMetadata = getMetadata().get(_CurrentPoint.getMetadataCN());
            _CurrentIndex = index;
            pointViewPager.setCurrentItem(index, smoothScroll);
            loadMedia(_CurrentPoint, false);
        } else {
            _CurrentPoint = null;
            _CurrentMetadata = null;
            _CurrentIndex = INVALID_INDEX;
            loadMedia(null, false);
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }

        updateButtons();
    }

    private void changePolygon(TtPolygon polygon) {
        if (_CurrentPolygon == null || !_CurrentPolygon.getCN().equals(polygon.getCN())) {
            if (savePoint()) {
                saveMedia();

                if (_deleteIndex > INVALID_INDEX) {
                    deletePoint(_deletePoint, INVALID_INDEX);
                }

                _CurrentPolygon = polygon;
                _CurrentPoint = null;
                _CurrentIndex = INVALID_INDEX;
                _Points = getTtAppCtx().getDAL().getPointsInPolygon(_CurrentPolygon.getCN());

                if (_Points == null) {
                    Toast.makeText(this, "DATA ERROR", Toast.LENGTH_SHORT).show();
                    _Points = new ArrayList<>();
                    return;
                }

                onPointsChanged();

                int pointSize = _Points.size();
                if (pointSize > 0) {
                    moveToPoint(pointSize - 1);

                    if (menuCreated) {
                        AndroidUtils.UI.enableMenuItem(miGoto);
                        AndroidUtils.UI.enableMenuItem(miLock);
                    }
                } else {
                    _CurrentMetadata = null;
                    updateButtons();

                    if (menuCreated) {
                        AndroidUtils.UI.disableMenuItem(miGoto);
                        AndroidUtils.UI.disableMenuItem(miLock);
                    }
                }

                getTtAppCtx().getProjectSettings().setLastEditedPolyCN(polygon.getCN());

                autoSetTrav = false;
            }
        }
    }

    private void jumpToQuondam(TtPoint point) {

        final ArrayList<TtPoint> points = new ArrayList<>();
        for (String cn : point.getLinkedPoints()) {
            points.add(getTtAppCtx().getDAL().getPointByCN(cn));
        }

        if (points.size() > 0) {
            if (points.size() > 1) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                dialogBuilder.setTitle("Linked Points");
                ListView listView = new ListView(this);

                final PointDetailsAdapter pda = new PointDetailsAdapter(this, points, AppUnits.IconColor.Dark);
                pda.setShowPolygonName(true);

                listView.setAdapter(pda);

                dialogBuilder.setView(listView);
                dialogBuilder.setNegativeButton(R.string.str_cancel, null);

                final AlertDialog dialog = dialogBuilder.create();

                listView.setOnItemClickListener((adapterView, view, i, l) -> {
                    TtPoint point1 = pda.getItem(i);
                    if (point1 != null) {
                        moveToPoint(point1);
                    }
                    dialog.dismiss();
                });

                dialog.show();
            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                final TtPoint linkedPoint = points.get(0);

                TtPolygon linkedPoly = getPolygons().get(linkedPoint.getPolyCN());
                if (linkedPoly != null) {
                    dialog.setMessage(String.format(Locale.getDefault(), "Move to Quondam %d in polygon %s.",
                            linkedPoint.getPID(), linkedPoly.getName()));

                    dialog.setPositiveButton(R.string.str_move, (dialogInterface, i) -> moveToPoint(linkedPoint));

                    dialog.setNeutralButton(R.string.str_cancel, null);

                    dialog.show();
                } else {
                    Toast.makeText(PointsActivity.this, "Cannot find polygon from linked point. Database may be corrupted. See log for details.", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "No Linked Points", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    //region Update UI
    private void updateButtons() {
        lockPoint(true);

        boolean setLinkVisible = false;
        //boolean setPolyChangeVisible = _Polygons.size() > 1;
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
        } else if (aqrVisible) {
            hideAqr();
        }

        //menu items that dont rely on a valid point
        if (menuCreated) {
            miLink.setVisible(setLinkVisible);
            //miMovePoint.setVisible(setPolyChangeVisible);
            miNmeaRecalc.setVisible(setGpsTypeVisible);
            miEnterLatLon.setVisible(setGpsTypeVisible);
        }
    }

    private void lockPoint(boolean lockPoint) {
        if (lockPoint) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed_white_36dp);

                //AndroidUtils.UI.disableMenuItem(miMovePoint);
                AndroidUtils.UI.disableMenuItem(miReset);
                AndroidUtils.UI.disableMenuItem(miDelete);
                AndroidUtils.UI.disableMenuItem(miNmeaRecalc);
                AndroidUtils.UI.disableMenuItem(miEnterLatLon);

                pmbMedia.setItemEnabled(R.id.ctx_menu_delete, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_reset, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_update_orientation, false);
            }

            fabAqr.setEnabled(false);
            if (_CurrentPoint != null && (_CurrentPoint.getOp() == OpType.GPS || _CurrentPoint.getOp() == OpType.WayPoint)) {
                fabAqr.hide();
                aqrVisible = false;
            }

            _PointLocked = true;
            onLockChange();
        } else if (_Points.size() > 0) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_lock);
                miLock.setIcon(R.drawable.ic_action_lock_open_white_36dp);

                //AndroidUtils.UI.enableMenuItem(miMovePoint);
                AndroidUtils.UI.enableMenuItem(miDelete);
                AndroidUtils.UI.enableMenuItem(miNmeaRecalc);
                AndroidUtils.UI.enableMenuItem(miEnterLatLon);

                if (_CurrentMedia != null) {
                    pmbMedia.setItemEnabled(R.id.ctx_menu_delete, true);
                    pmbMedia.setItemEnabled(R.id.ctx_menu_reset, _MediaUpdated);
                    pmbMedia.setItemEnabled(R.id.ctx_menu_update_orientation, _CurrentMedia.getMediaType() == MediaType.Picture);
                }

                if (_CurrentPoint != null) {
                    if (_PointUpdated) {
                        AndroidUtils.UI.enableMenuItem(miReset);
                    } else {
                        AndroidUtils.UI.disableMenuItem(miReset);
                    }
                }
            }

            fabAqr.setEnabled(true);
            if (_CurrentPoint != null &&(_CurrentPoint.getOp() == OpType.GPS || _CurrentPoint.getOp() == OpType.WayPoint) &&
                    slidingLayout != null && slidingLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
                fabAqr.show();
                aqrVisible = true;
            }

            _PointLocked = false;
            onLockChange();
        }
    }

    private void setPointUpdated(boolean updated) {
        _PointUpdated = updated;

        if (menuCreated) {
            if (_PointUpdated) {
                AndroidUtils.UI.enableMenuItem(miReset);
            } else {
                AndroidUtils.UI.disableMenuItem(miReset);
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
        if (!aqrVisible && !_PointLocked) {
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_left_in);

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
            Animation a = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

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


    private void anchorMediaIfExpanded() {
        if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }
    }

    private void loadMedia(final TtPoint point, final boolean loadMedia) {
        if (rvMediaAdapter != null) {
            mediaViewPager.removeOnPageChangeListener(onMediaPageChangeListener);

            rvMediaAdapter.clear();

            mediaLoaded = false;

            if (point != null) {
                if (getTtAppCtx().hasMAL()) {
                    if (loadMedia) {
                        mediaCount = 0;
                        mediaSelectionIndex = INVALID_INDEX;

                        ArrayList<TtImage> pictures = getTtAppCtx().getMAL().getImagesInPoint(point.getCN());

                        if (pictures != null) {
                            pictures.sort(TtUtils.Media.PictureTimeComparator);
                            for (final TtImage p : pictures) {
                                loadImageToAdapter(p);
                            }

                            if (mediaCount > 0) {
                                mediaSelectionIndex = 0;
                                setCurrentMedia(pictures.get(0));
                            } else {
                                setCurrentMedia(null);
                            }
                        } else {
                            setCurrentMedia(null);
                        }
                    } else {
                        mediaCount = getTtAppCtx().getMAL().getItemsCount(
                                TwoTrailsMediaSchema.Media.TableName,
                                TwoTrailsMediaSchema.Media.PointCN,
                                point.getCN());
                        setCurrentMedia(null);
                    }
                } else {
                    mediaCount = 0;
                    setCurrentMedia(null);
                }
            }

            mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);
        }
    }


    private void loadImageToAdapter(final TtImage picture) {
        mediaCount++;

        try {
            Bitmap bmp = bitmapManager.get(getTtAppCtx().getMAL().getProviderId(), picture.getCN());

            addImageToAdapter(picture, true, bmp);
        } catch (Exception e) {
            getTtAppCtx().getReport().writeError(e.getMessage(), "PointsActivity:loadImageToList", e.getStackTrace());
            addInvalidImagesToAdapter(picture);
        }
//
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

    private void addInvalidImagesToAdapter(final TtImage picture) {
        Bitmap bitmap = BitmapFactory.decodeResource(PointsActivity.this.getResources(), R.drawable.ic_error_outline_black_48dp);
        if (bitmap != null) {
            addImageToAdapter(picture, false, bitmap);
        }
    }

    private void addImageToAdapter(final TtImage picture, boolean isValid, final Bitmap loadedImage) {
        if (picture.getPointCN().equals(_CurrentPoint.getCN())) {
//            if (isValid) {
//                bitmapManager.put(picture.getCN(), picture.getPath(), AndroidUtils.UI.scaleMinBitmap(loadedImage, bitmapHeight, false), scaleOptions);
//            } else {
//                bitmapManager.put(picture.getCN(), TtUtils.getResourceUri(getTtAppCtx(), R.drawable.ic_error_outline_black_48dp), AndroidUtils.UI.scaleMinBitmap(loadedImage, bitmapHeight, false), scaleOptions, true);
//            }

            try {
                semaphore.acquire();

                final int order = TtUtils.Media.getMediaIndex(picture, rvMediaAdapter.getItems());

                PointsActivity.this.runOnUiThread(() -> {
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


    private final Runnable onMediaChanged = new Runnable() {
        @Override
        public void run() {
            PointsActivity.this.runOnUiThread(() -> {
                if (mediaSelectionIndex > INVALID_INDEX && mediaSelectionIndex < rvMediaAdapter.getItemCountEx()) {
                    setCurrentMedia(rvMediaAdapter.getItem(mediaSelectionIndex));
                    rvMediaAdapter.selectItem(mediaSelectionIndex);
                    mediaViewPager.setCurrentItem(mediaSelectionIndex);
                    mediaSelectionIndex = INVALID_INDEX;
                }

                mediaLoaded = true;

                setMediaTitle(slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ?
                    _CurrentMedia.getName() : null);
            });
        }
    };


    private void setCurrentMedia(TtMedia media) {
        if (media != null) {
            if (slidingLayout != null && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                setMediaTitle(media.getName());
            } else {
                setMediaTitle(null);
            }

            if (_CurrentMedia == null || !media.getCN().equals(_CurrentMedia.getCN())) {
                _BackupMedia = TtUtils.Media.cloneMedia(media);

                setMediaUpdated(false);
            }

//            if (media.isExternal()) {
                ivFullscreen.setEnabled(true);
                ivFullscreen.setAlpha(Consts.ENABLED_ALPHA);
//            } else {
//                ivFullscreen.setEnabled(false);
//                ivFullscreen.setAlpha(Consts.DISABLED_ALPHA);
//            }
        } else {
            setMediaTitle(null);
            _BackupMedia = null;

            ivFullscreen.setEnabled(false);
            ivFullscreen.setAlpha(Consts.DISABLED_ALPHA);
        }

        _CurrentMedia = media;
    }

    private void setMediaTitle(String title) {
        if (title != null) {
            tvPmdTitle.setText(title);
        } else {
            tvPmdTitle.setText(String.format(Locale.getDefault(), "Media (%d)", mediaCount));
        }
    }

    private void setMediaUpdated(boolean updated) {
        _MediaUpdated = updated;

        pmbMedia.setItemEnabled(R.id.ctx_menu_reset, _MediaUpdated);
    }


    private void onPointsChanged() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> pointSectionsPagerAdapter.notifyDataSetChanged());
        } else {
            pointSectionsPagerAdapter.notifyDataSetChanged();
        }
    }
    //endregion


    //region Fragment Interaction
    private void onLockChange() {
        if (_CurrentPoint != null && listeners.containsKey(_CurrentPoint.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentPoint.getCN());
            if (listener != null) {
                listener.onLockChange(_PointLocked);
            } else {
                getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onLockChange:point");
            }
        }

        if (_CurrentMedia != null && listeners.containsKey(_CurrentMedia.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentMedia.getCN());
            if (listener != null) {
                listener.onLockChange(_PointLocked);
            } else {
                getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onLockChange:media");
            }
        }
    }

    private void onPointUpdate() {
        setPointUpdated(true);

        PointMediaListener listener = listeners.get(_CurrentPoint.getCN());
        if (listener != null) {
            listener.onPointUpdated(_CurrentPoint);
        } else {
            getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onPointUpdate");
        }
    }

    private void onPointUpdate(TtPoint point) {
        if (listeners.containsKey(point.getCN())) {
            PointMediaListener listener = listeners.get(point.getCN());
            if (listener != null) {
                listener.onPointUpdated(point);
            } else {
                getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onPointUpdate(point)");
            }
        }
    }

    private void onMediaUpdated() {
        setMediaUpdated(true);

        if (listeners.containsKey(_CurrentMedia.getCN())) {
            PointMediaListener listener = listeners.get(_CurrentMedia.getCN());
            if (listener != null) {
                listener.onMediaUpdated(_CurrentMedia);
            } else {
                getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onMediaUpdate");
            }
        }
    }

    protected void onMediaUpdated(TtMedia media) {
        if (media.getCN().equals(_CurrentMedia.getCN())) {
            setMediaUpdated(true);
        }

        if (listeners.containsKey(media.getCN())) {
            PointMediaListener listener = listeners.get(media.getCN());
            if (listener != null) {
                listener.onMediaUpdated(media);
            } else {
                getTtAppCtx().getReport().writeError("Listener is null", "PointsActivity:onMediaUpdate(media)");
            }
        }
    }

    public void updatePoint(TtPoint point) {
        //only update if current point
        if (point != null) {
            if (_CurrentPoint.getCN().equals(point.getCN())) {
                _CurrentPoint = point;
                setPointUpdated(true);
            }

            adjust = true;
        }
    }

    public void updateMedia(TtMedia media) {
        //only update if current media
        if (_CurrentMedia != null && _CurrentMedia.getCN().equals(media.getCN())) {
            setCurrentMedia(media);
            setMediaUpdated(true);
        }
    }


    @Override
    protected TtMedia getCurrentMedia() {
        return _CurrentMedia;
    }


    public TtMetadata getMetadata(String cn) {
        if (getMetadata().containsKey(cn)) {
            return getMetadata().get(cn);
        } else {
            getTtAppCtx().getReport().writeError("Metadata not found", "PointsActivity:getMetadata");
            throw new RuntimeException("Metadata not found");
        }
    }

    public BitmapManager getBitmapManager() {
        return bitmapManager;
    }
    //endregion


    //region Misc
    private void configGps() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("The GPS is currently not configured. Would you like to configure it now?");

        dialog.setPositiveButton("Configure", (dialog1, which) -> startActivity(new Intent(getBaseContext(), SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.GPS_SETTINGS_PAGE)));

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }

    private void configIns() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("The INS is currently not configured. Would you like to configure it now?");

        dialog.setPositiveButton("Configure", (dialog1, which) -> startActivity(new Intent(getBaseContext(), SettingsActivity.class).putExtra(SettingsActivity.SETTINGS_PAGE, SettingsActivity.VN_SETTINGS_PAGE)));

        dialog.setNeutralButton(R.string.str_cancel, null);

        dialog.show();
    }
    //endregion


    //region Acquire Calculate
    private void acquireGpsPoint(final TtPoint point, final ArrayList<TtNmeaBurst> bursts) {
        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            configGps();
        } else if (getTtAppCtx().getDAL().needsAdjusting()) {

            startActivityAfterAdjustment = GpsTypeActivity.Gps;
            startPoint = point;
            startBursts = bursts;

            handleStartAdjustingResult(getTtAppCtx().adjustProject());
        } else {
            startAcquireGpsActivity(point, bursts);
        }
    }

    private void startAcquireGpsActivity(TtPoint point, ArrayList<TtNmeaBurst> bursts) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Consts.Codes.Data.POINT_DATA, TtUtils.Points.clonePoint(point));
        bundle.putParcelable(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);
        bundle.putParcelable(Consts.Codes.Data.METADATA_DATA, getMetadata().get(point.getMetadataCN()));


        Intent intent = new Intent(this, AcquireAndCalculateGpsActivity.class);
        intent.putExtra(Consts.Codes.Data.POINT_PACKAGE, bundle);

        if (bursts != null && bursts.size() > 0) {
            try {
                intent.putParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA, bursts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        acquireAndOrCalculate(intent);
    }


    private void startTake5Activity(final TtPoint point) {
        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            configGps();
        } else if (getTtAppCtx().getDAL().needsAdjusting()) {

            startActivityAfterAdjustment = GpsTypeActivity.Take5;

            startPoint = point;

            handleStartAdjustingResult(getTtAppCtx().adjustProject());

        } else {
            Intent intent = new Intent(this, Take5Activity.class);

            intent.putExtra(Consts.Codes.Data.POINT_PACKAGE, createPointDataBundle());

            addOrInsertPoints(intent);
        }
    }

    private void startWalkActivity(final TtPoint point) {
        if (!getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
            configGps();
            slexCreate.contractFab();
        } else if (getTtAppCtx().getDAL().needsAdjusting()) {

            startActivityAfterAdjustment = GpsTypeActivity.Walk;

            startPoint = point;

            handleStartAdjustingResult(getTtAppCtx().adjustProject());

        } else {
            Intent intent = new Intent(this, WalkActivity.class);

            intent.putExtra(Consts.Codes.Data.POINT_PACKAGE, createPointDataBundle());

            addOrInsertPoints(intent);
        }
    }

    private void startInertialActivity(final TtPoint point) {
        TwoTrailsApp app = getTtAppCtx();
        if (!app.getDeviceSettings().isGpsConfigured()) {
            configGps();
            slexCreate.contractFab();
        } else if (!app.getDeviceSettings().isVN100Configured()) {
            configIns();
            slexCreate.contractFab();
        } else if (getTtAppCtx().getDAL().needsAdjusting()) {
            startActivityAfterAdjustment = GpsTypeActivity.Inertial;
            startPoint = point;
            handleStartAdjustingResult(getTtAppCtx().adjustProject());
        } else {
            Intent intent = new Intent(this, InertialActivity.class);

            intent.putExtra(Consts.Codes.Data.POINT_PACKAGE, createPointDataBundle());

            addOrInsertPoints(intent);
        }
    }

    private Bundle createPointDataBundle() {
        Bundle bundle = new Bundle();

        if (_CurrentPoint != null) {
            bundle.putParcelable(Consts.Codes.Data.POINT_DATA, TtUtils.Points.clonePoint(_CurrentPoint));
        }

        if (_CurrentMetadata != null) {
            bundle.putParcelable(Consts.Codes.Data.METADATA_DATA, _CurrentMetadata);
        } else {
            bundle.putParcelable(Consts.Codes.Data.METADATA_DATA, getMetadata().get(Consts.EmptyGuid));
        }

        bundle.putParcelable(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);

        return bundle;
    }


    private void calculateGpsPoint() {
        if (_CurrentPoint != null && _CurrentPoint.isGpsType()) {
            ArrayList<TtNmeaBurst> bursts = getTtAppCtx().getDAL().getNmeaBurstsByPointCN(_CurrentPoint.getCN());

            if (bursts.size() > 0) {
                Intent intent = new Intent(this, AcquireAndCalculateGpsActivity.class);

                Bundle bundle = new Bundle();
                bundle.putParcelable(Consts.Codes.Data.POINT_DATA, TtUtils.Points.clonePoint(_CurrentPoint));
                bundle.putParcelable(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);
                bundle.putParcelable(Consts.Codes.Data.METADATA_DATA, getMetadata().get(_CurrentPoint.getMetadataCN()));

                intent.putExtra(Consts.Codes.Data.POINT_PACKAGE, bundle);
                intent.putExtra(AcquireAndCalculateGpsActivity.CALCULATE_ONLY_MODE, true);

                try {
                    intent.putParcelableArrayListExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA, bursts);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                acquireAndOrCalculate(intent);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage("This point has no NMEA data associated with it. Would you like to acquire some data?");

                alert.setPositiveButton("Acquire NMEA", (dialogInterface, i) -> {
                    Intent intent = new Intent(getBaseContext(), AcquireAndCalculateGpsActivity.class);
                    intent.putExtra(Consts.Codes.Data.POINT_DATA, new GpsPoint(_CurrentPoint));
                    intent.putExtra(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);
                    intent.putExtra(Consts.Codes.Data.METADATA_DATA, getMetadata().get(_CurrentPoint.getMetadataCN()));
                    intent.putExtra(AcquireAndCalculateGpsActivity.CALCULATE_ONLY_MODE, false);

                    acquireAndOrCalculate(intent);
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
                case GPS:
                case WayPoint: {
                    //region GPS
                    if (getTtAppCtx().getDeviceSettings().isGpsConfigured()) {
                        if (TtUtils.Points.pointHasValue(_CurrentPoint)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(PointsActivity.this);

                            dialog.setMessage(R.string.points_aqr_diag_gps_msg);

                            dialog.setPositiveButton(R.string.points_aqr_diag_add, (dialog1, which) -> {
                                ArrayList<TtNmeaBurst> bursts = getTtAppCtx().getDAL().getNmeaBurstsByPointCN(_CurrentPoint.getCN());
                                acquireGpsPoint(_CurrentPoint, bursts);
                            });

                            dialog.setNegativeButton(R.string.points_aqr_diag_overwrite, (dialog12, which) -> {
                                AlertDialog.Builder dialogA = new AlertDialog.Builder(PointsActivity.this);

                                dialogA.setMessage(R.string.points_aqr_diag_del_msg);

                                dialogA.setPositiveButton(R.string.str_delete, (dialog121, which1) -> {
                                    getTtAppCtx().getDAL().deleteNmeaByPointCN(_CurrentPoint.getCN());
                                    acquireGpsPoint(_CurrentPoint, null);
                                });

                                dialogA.setNeutralButton(R.string.str_cancel, null);

                                dialogA.show();
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
        PointEditorDialog dialog = PointEditorDialog.newInstance(_CurrentPoint.getCN(), _CurrentPoint.getPID(), _CurrentPoint.getMetadataCN(), getMetadata());

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

    public void btnPointNewInertialClick(View view) {
        createOpType = OpType.InertialStart;
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


    @Override
    public boolean requiresRFService() {
        return true;
    }

    //region Range Finder
    @Override


    public void rfDataReceived(final TtRangeFinderData rfData) {
        if (rfData.isValid()) {
            if (!_PointLocked) {
                if (_CurrentPoint.getOp().isTravType()) {
                    TravPoint tp = (TravPoint) _CurrentPoint;

                    if (tp.getFwdAz() != null || tp.getBkAz() != null || tp.getSlopeDistance() > 0) {
                        new AlertDialog.Builder(PointsActivity.this)
                                .setMessage("This point already has data associated with it. Would you like to overwrite it?")
                                .setPositiveButton(R.string.str_yes, (dialog, which) -> promptToFillTravDataFromRF(rfData))
                                .setNeutralButton(R.string.str_no, null)
                                .show();
                    } else {
                        promptToFillTravDataFromRF(rfData);
                    }
                }
            } else {
                createPoint(_CurrentPoint.getOp() == OpType.Traverse ? OpType.Traverse : OpType.SideShot);
                updateCurrentPointFromRangeFinderData(rfData, true, true);
                lockPoint(true);
                Toast.makeText(PointsActivity.this, String.format("Created %s from RF Data.", _CurrentPoint.getOp().toString()), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(PointsActivity.this, "Range Finder did not supply Slope and/or Distance", Toast.LENGTH_LONG).show();
        }
    }

    private  void promptToFillTravDataFromRF(final TtRangeFinderData rfData) {
        if (!autoSetTrav) {
            final DontAskAgainDialog dialog = new DontAskAgainDialog(PointsActivity.this,
                    DeviceSettings.AUTO_FILL_FROM_RANGE_FINDER_ASK,
                    DeviceSettings.AUTO_FILL_FROM_RANGE_FINDER,
                    getTtAppCtx().getDeviceSettings().getPrefs());

            dialog.setMessage("Would You like to set the compass value to Forward or Backwards?")
                    .setPositiveButton("Fwd", (dialogInterface, i, value) -> {
                        if (dialog.isDontAskAgainChecked()) {
                            autoSetTrav = true;
                            autoSetAz = true;
                            autoSetAzFwd = true;
                        }

                        updateCurrentPointFromRangeFinderData(rfData, true, true);
                        Toast.makeText(PointsActivity.this, "RF Data Applied", Toast.LENGTH_LONG).show();
                    }, 2)
                    .setNegativeButton("Back", (dialogInterface, i, value) -> {
                        if (dialog.isDontAskAgainChecked()) {
                            autoSetTrav = true;
                            autoSetAz = true;
                            autoSetAzFwd = false;
                        }
                        updateCurrentPointFromRangeFinderData(rfData, true, false);
                        Toast.makeText(PointsActivity.this, "RF Data Applied", Toast.LENGTH_LONG).show();
                    }, 1)
                    .setNeutralButton("No Az", (dialogInterface, i, value) -> {
                        if (dialog.isDontAskAgainChecked()) {
                            autoSetTrav = false;
                            autoSetAz = false;
                        }
                        updateCurrentPointFromRangeFinderData(rfData, false, false);
                        Toast.makeText(PointsActivity.this, "RF Data Applied", Toast.LENGTH_LONG).show();
                    })
                    .show();
        } else {
            updateCurrentPointFromRangeFinderData(rfData, autoSetAz, autoSetAzFwd);
        }
    }

    private void updateCurrentPointFromRangeFinderData(TtRangeFinderData rfData, boolean useCompassData, boolean useFwdDir) {
        if (_CurrentPoint != null && _CurrentPoint.getOp().isTravType()) {
            TravPoint trav = ((TravPoint)_CurrentPoint);

            trav.setSlopeAngle(TtUtils.Convert.angle(rfData.getInclination(), Slope.Percent, rfData.getIncType()));
            trav.setSlopeDistance(TtUtils.Convert.distance(rfData.getSlopeDist(), Dist.Meters, rfData.getSlopeDistType()));

            if (useCompassData && rfData.hasCompassData()) {
                if (useFwdDir)
                    trav.setFwdAz(TtUtils.Convert.angle(rfData.getAzimuth(), Slope.Degrees, rfData.getAzType()));
                else
                    trav.setBkAz(TtUtils.Convert.angle(rfData.getAzimuth(), Slope.Degrees, rfData.getAzType()));
            }

            onPointUpdate();
        }
    }

    @Override
    public void rfStringReceived(String rfString) {

    }

    @Override
    public void rfInvalidStringReceived(String rfString) {
        Toast.makeText(this, "Invalid Range Finder data received", Toast.LENGTH_LONG).show();
    }

    @Override
    public void rangeFinderStarted() {

    }

    @Override
    public void rangeFinderStopped() {

    }

    @Override
    public void rangeFinderConnecting() {

    }

    @Override
    public void rangeFinderServiceStarted() {

    }

    @Override
    public void rangeFinderServiceStopped() {

    }

    @Override
    public void rangeFinderError(RangeFinderService.RangeFinderError error) {

    }
    //endregion


    //region Adjuster
    private GpsTypeActivity startActivityAfterAdjustment = GpsTypeActivity.None;
    private TtPoint startPoint = null;
    private ArrayList<TtNmeaBurst> startBursts = null;

    @Override
    public void onAdjusterStarted() {
        runOnUiThread(() -> Toast.makeText(PointsActivity.this, "Adjusting Points. Starting Acquire soon.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAdjusterStopped(TwoTrailsApp.ProjectAdjusterResult result, AdjustingException.AdjustingError error) {
        super.onAdjusterStopped(result, error);

        runOnUiThread(() -> {
            if (result == TwoTrailsApp.ProjectAdjusterResult.SUCCESSFUL) {
                switch (startActivityAfterAdjustment) {
                    case Gps: startAcquireGpsActivity(startPoint, startBursts); break;
                    case Take5: startTake5Activity(startPoint); break;
                    case Walk: startWalkActivity(startPoint); break;
                    case Inertial: startInertialActivity(startPoint); break;
                }
            }

            startPoint = null;
            startBursts = null;

            startActivityAfterAdjustment = GpsTypeActivity.None;
        });
    }

    private void handleStartAdjustingResult(TwoTrailsApp.ProjectAdjusterResult result) {
        switch (result) {
            case STARTS_WITH_TRAV_TYPE:
                Toast.makeText(PointsActivity.this, "Project failed to adjust. Polygon started with Traverse or Sideshot.", Toast.LENGTH_LONG).show();
                break;
            case BAD_POINT:
                Toast.makeText(PointsActivity.this, "Project failed to adjust. There is a bad point in a polygon.", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private enum GpsTypeActivity {
        None,
        Gps,
        Take5,
        Walk,
        Inertial
    }
    //endregion

    public void register(String pointCN, PointMediaListener listener) {
        if (listener != null && !listeners.containsKey(pointCN)) {
            listeners.put(pointCN, listener);
        }
    }

    public void unregister(String pointCN) {
        listeners.remove(pointCN);
    }

    private class PointsPagerAdapter extends FragmentStatePagerAdapterEx {
        private PointsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TtPoint point = getPointAtIndex(position);

            boolean hideCard = !StringEx.isEmpty(addedPoint) && point.getCN().equals(addedPoint);

            if (hideCard) {
                addedPoint = null;
            }

            switch (point.getOp()) {
                case GPS:
                case Take5:
                case Walk:
                case WayPoint: {
                    return GPSPointFragment.newInstance((GpsPoint)point, hideCard);
                }
                case SideShot:
                case Traverse: {
                    return TraversePointFragment.newInstance((TravPoint)point, hideCard);
                }
                case Quondam: {
                    return QuondamPointFragment.newInstance((QuondamPoint)point, hideCard);
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
            listeners.clear();
            super.notifyDataSetChanged();
        }
    }
}
