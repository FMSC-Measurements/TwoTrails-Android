package com.usda.fmsc.twotrails.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableAdapterEx;
import com.usda.fmsc.android.dialogs.DontAskAgainDialog;
import com.usda.fmsc.android.listeners.ComplexOnPageChangeListener;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.SheetFab;
import com.usda.fmsc.android.widget.SheetLayoutEx;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.adapters.MediaRvAdapter;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.dialogs.LatLonDialog;
import com.usda.fmsc.twotrails.dialogs.MoveToPointDialog;
import com.usda.fmsc.twotrails.dialogs.PointEditorDialog;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.fragments.media.PictureMediaFragment;
import com.usda.fmsc.twotrails.fragments.points.BasePointFragment;
import com.usda.fmsc.twotrails.fragments.points.GPSPointFragment;
import com.usda.fmsc.twotrails.fragments.points.QuondamPointFragment;
import com.usda.fmsc.twotrails.fragments.points.TraversePointFragment;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PointNamer;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.ui.MSFloatingActionButton;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import jp.wasabeef.recyclerview.animators.FadeInAnimator;

public class PointsActivity extends CustomToolbarActivity {
    private HashMap<String, Listener> listeners;

    private MenuItem miLock, miLink, miMovePoint, miReset, miEnterLatLon, miNmeaRecalc, miDelete, miGoto;
    private SheetLayoutEx slexAqr, slexCreate;
    private android.support.design.widget.FloatingActionButton fabAqr;
    private MSFloatingActionButton fabMenu;
    private SheetFab fabSheet;

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
    private HashMap<String, TtMetadata> _MetaData;
    private TtPoint _CurrentPoint, _deletePoint;
    private TtPolygon _CurrentPolygon;
    private TtMetadata _CurrentMetadata;
    private int _CurrentIndex = INVALID_INDEX, _deleteIndex = INVALID_INDEX;
    private boolean _PointUpdated, _PointLocked, _MediaUpdated;
    private String addedPoint;

    private BitmapManager bitmapManager;
    private BitmapManager.ScaleOptions scaleOptions = new BitmapManager.ScaleOptions();

    private TtMedia _CurrentMedia, _BackupMedia;

    private float collapsedHeight, anchoredPercent;
    private int height, bitmapHeight;
    private boolean mediaLoaded;
    private int mediaCount, mediaSelectionIndex;

    private Uri captureImageUri;

    private Semaphore semaphore = new Semaphore(1);
    private PostDelayHandler mediaLoaderDelayedHandler = new PostDelayHandler(500);


    //region Listeners
    private ComplexOnPageChangeListener onPointPageChangeListener = new ComplexOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (!ignorePointChange) {
                savePoint();
                saveMedia();

                _CurrentIndex = position;
                _CurrentPoint = getPointAtIndex(_CurrentIndex);
                _CurrentMetadata = _MetaData.get(_CurrentPoint.getMetadataCN());
                updateButtons();

                if (slidingLayout != null && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    loadMedia(_CurrentPoint, false);
                } else {
                    loadMedia(_CurrentPoint);
                }
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
                            pointViewPager.setCurrentItem(_deleteIndex);

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

    private ComplexOnPageChangeListener onMediaPageChangeListener = new ComplexOnPageChangeListener() {
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

    private SelectableAdapterEx.Listener<TtMedia> mediaListener = new SelectableAdapterEx.Listener<TtMedia>() {
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

    private SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
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
                        loadMedia(_CurrentPoint);
                    }

                    fabAqr.hide();
                    fabMenu.hide();
                    aqrVisible = false;

                    setCurrentMedia(_CurrentMedia);

                    pmdScroller.setTranslationY(height - collapsedHeight - 1);
                    break;
                case ANCHORED:
                    if (!mediaLoaded && _CurrentPoint != null) {
                        loadMedia(_CurrentPoint);
                    }
                case COLLAPSED:
                case HIDDEN:
                    if (!_PointLocked && _CurrentPoint != null && (_CurrentPoint.getOp() == OpType.GPS || _CurrentPoint.getOp() == OpType.WayPoint)) {
                        fabAqr.show();
                        aqrVisible = true;
                    }

                    fabMenu.show();

                    tvPmdTitle.setText(String.format("Media (%d)", mediaCount));
                    break;
                case DRAGGING:
                    break;
            }

        }
    };

    private PopupMenu.OnMenuItemClickListener menuPopupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.ctx_menu_add: {
                    Intent intent = new Intent(Intent.ACTION_PICK);

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }

                    intent.setType("image/*");
                    startActivityForResult(intent, Consts.Codes.Requests.ADD_IMAGES);
                    break;
                }
                case R.id.ctx_menu_capture: {
                    if (Global.Settings.DeviceSettings.getUseTtCameraAsk()) {
                        DontAskAgainDialog dialog = new DontAskAgainDialog(PointsActivity.this,
                                Global.Settings.DeviceSettings.USE_TTCAMERA_ASK,
                                Global.Settings.DeviceSettings.USE_TTCAMERA,
                                Global.Settings.PreferenceHelper.getPrefs());

                        dialog.setMessage(PointsActivity.this.getString(R.string.points_camera_diag))
                        .setPositiveButton("TwoTrails", new DontAskAgainDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, Object value) {
                                captureImage((int)value);
                            }
                        }, 2)
                        .setNegativeButton("Android", new DontAskAgainDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, Object value) {
                                captureImage((int)value);
                            }
                        }, 1)
                        .setNeutralButton(getString(R.string.str_cancel), null, 0)
                        .show();
                    } else {
                        captureImage(Global.Settings.DeviceSettings.getUseTtCamera());
                    }
                    break;
                }
                case R.id.ctx_menu_reset: {
                    resetMedia();
                    break;
                }
                case R.id.ctx_menu_delete: {
                    if (!_PointLocked && _CurrentMedia != null) {
                        new AlertDialog.Builder(PointsActivity.this)
                                .setMessage(String.format(
                                        "Would you like to delete %s '%s' from storage or only remove its association with the point?",
                                        _CurrentMedia.getMediaType().toString().toLowerCase(),
                                        _CurrentMedia.getName()))
                                .setPositiveButton(R.string.str_remove, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeMedia(_CurrentMedia, false);
                                    }
                                })
                                .setNegativeButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new AlertDialog.Builder(PointsActivity.this)
                                                .setMessage(String.format("You are about to delete file '%s'.", _CurrentMedia.getFilePath()))
                                                .setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        removeMedia(_CurrentMedia, true);
                                                    }
                                                })
                                                .setNeutralButton(R.string.str_cancel, null)
                                                .show();
                                    }
                                })
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


    //region Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayShowTitleEnabled(false);
        }

        setUseExitWarning(true);

        listeners = new HashMap<>();

        _Polygons = Global.getDAL().getPolygonsMap();
        _MetaData = Global.getDAL().getMetadataMap();

        final TtPolygon[] polyArray = _Polygons.values().toArray(new TtPolygon[_Polygons.size()]);
        Arrays.sort(polyArray);

        _Points = new ArrayList<>();
        _CurrentIndex = INVALID_INDEX;

        pointSectionsPagerAdapter = new PointsPagerAdapter(getSupportFragmentManager());
        pointSectionsPagerAdapter.saveFragmentStates(false);

        pointViewPager = (ViewPager)findViewById(R.id.pointsViewPager);

        if (pointViewPager != null) {
            pointViewPager.setAdapter(pointSectionsPagerAdapter);
            pointViewPager.addOnPageChangeListener(onPointPageChangeListener);
        }

        bitmapManager = new BitmapManager(getResources());

        //region Main Buttons
        fabAqr = (FloatingActionButton)findViewById(R.id.pointsFabAqr);
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
        if (spinnerPoly != null) {
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
        }
        //endregion

        //region Transitions
        slexAqr = (SheetLayoutEx)findViewById(R.id.pointsSLExAqr);
        if (slexAqr != null) {
            slexAqr.setFab(fabAqr);
            slexAqr.setFabAnimationEndListener(new SheetLayoutEx.OnFabAnimationEndListener() {
                @Override
                public void onFabAnimationEnd() {
                    acquireGpsPoint(_CurrentPoint, null);
                }
            });
        }

        slexCreate = (SheetLayoutEx)findViewById(R.id.pointsSLExCreate);
        if (slexCreate != null) {
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
        //endregion

        //region Media Layouts
        pmbMedia = (PopupMenuButton)findViewById(R.id.pmdMenu);
        if (pmbMedia != null) {
            pmbMedia.setListener(menuPopupListener);
        }

        tvPmdTitle = (TextView)findViewById(R.id.pmdTitle);

        slidingLayout = (SlidingUpPanelLayout) findViewById(R.id.pointSlidingPanelLayout);
        pmdScroller = findViewById(R.id.pmdScroller);
        final View layMDH = findViewById(R.id.pmdHeader);
        rvMedia = (RecyclerViewEx) findViewById(R.id.pmdRvMedia);
        if (rvMedia != null) {
            rvMedia.setViewHasFooter(true);
            rvMedia.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this, LinearLayoutManager.HORIZONTAL, false));
            rvMedia.setHasFixedSize(true);
            rvMedia.setItemAnimator(new FadeInAnimator());
        }

        mediaViewPager = (ViewPager)findViewById(R.id.pmdViewPager);
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
                    scaleOptions.setScaleMode(BitmapManager.ScaleMode.Max);
                    scaleOptions.setSize(bitmapHeight);

                    rvMediaAdapter = new MediaRvAdapter(PointsActivity.this, Collections.synchronizedList(new ArrayList<TtMedia>()), mediaListener,
                            pmdScroller.getHeight() - AndroidUtils.Convert.dpToPx(PointsActivity.this, 10), bitmapManager);

                    rvMedia.setAdapter(rvMediaAdapter);

                    ViewTreeObserver obs = slidingLayout.getViewTreeObserver();
                    obs.removeOnGlobalLayoutListener(this);

                    mediaPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager());
                    mediaViewPager.setAdapter(mediaPagerAdapter);
                    mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);

                    loadMedia(_CurrentPoint, false);
                }
            });

            slidingLayout.addPanelSlideListener(panelSlideListener);

            layMDH.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                }
            });
        }


        ImageView ivFullscreen = (ImageView)findViewById(R.id.pmdIvFullscreen);
        if (ivFullscreen != null) {
            ivFullscreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (_CurrentMedia != null && _CurrentMedia.getMediaType() == MediaType.Picture &&
                            (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                                    slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + _CurrentMedia.getFilePath()), "image/*");
                        startActivity(intent);
                    }
                }
            });

            AndroidUtils.UI.setContentDescToast(ivFullscreen, "View in Fullscreen");
        }
        //endregion
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
        savePoint();
        saveMedia();

        if (adjust) {
            PolygonAdjuster.adjust(Global.getDAL(), Global.getMainActivity(), true);
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
            AndroidUtils.UI.disableMenuItem(miGoto);
            AndroidUtils.UI.disableMenuItem(miLock);
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
                    anchorMediaIfExpanded();

                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setMessage(String.format("Delete Point %d", _CurrentPoint.getPID()));

                    alert.setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AnimationCardFragment card = ((AnimationCardFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex));

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
                                                    AndroidUtils.UI.disableMenuItem(miLock);
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
                anchorMediaIfExpanded();
                resetPoint();
                break;
            }
            case R.id.pointsMenuEnterLatLon: {
                if (_CurrentPoint.isGpsType()) {
                    anchorMediaIfExpanded();

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
                anchorMediaIfExpanded();
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
        } else if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Consts.Codes.Activites.ACQUIRE:
            case Consts.Codes.Activites.CALCULATE: {
                slexAqr.contractFab();
                calculateResult(resultCode, data);
                break;
            }
            case Consts.Codes.Activites.TAKE5:
            case Consts.Codes.Activites.WALK: {
                slexCreate.contractFab();
                addInsertPointsResult(resultCode, data);
                break;
            }
            case Consts.Codes.Requests.ADD_IMAGES: {
                if (data != null) {
                    addImages(TtUtils.getPicturesFromImageIntent(PointsActivity.this, data, _CurrentPoint.getCN()));
                }
                break;
            }
            case Consts.Codes.Activites.TTCAMERA: {
                if (data != null) {
                    TtImage image = TtUtils.getPictureFromTtCameraIntent(data);

                    if (image == null) {
                        Toast.makeText(PointsActivity.this, "Unable to add Image", Toast.LENGTH_LONG).show();
                    } else {
                        addImage(image);
                    }
                }
                break;
            }
            case Consts.Codes.Requests.CAPTURE_IMAGE: {
                if (resultCode != RESULT_CANCELED) {
                    TtImage image = TtUtils.getPictureFromUri(captureImageUri.getPath(), _CurrentPoint.getCN());

                    if (image == null) {
                        Toast.makeText(PointsActivity.this, "Unable to add Image", Toast.LENGTH_LONG).show();
                    } else {
                        addImage(image);
                    }
                }
                break;
            }
        }
    }

    private void calculateResult(int resultCode, Intent data) {
        if (resultCode == Consts.Codes.Results.POINT_CREATED) {
            GpsPoint point = data.getParcelableExtra(Consts.Codes.Data.POINT_DATA);

            updatePoint(point);
            onPointUpdate();
        }
    }

    private void addInsertPointsResult(int resultCode, Intent data) {
        if (resultCode == Consts.Codes.Results.POINT_CREATED) {
            Bundle bundle = data.getExtras();
            int created = 1;

            if (bundle.containsKey(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS)) {
                created = bundle.getInt(Consts.Codes.Data.NUMBER_OF_CREATED_POINTS);
            }

            if (_CurrentIndex < _Points.size() - 1) {
                ArrayList<TtPoint> updatePoints = new ArrayList<>();
                TtPoint tmpPoint;

                for (int i = _CurrentIndex + 1; i < _Points.size(); i++) {
                    tmpPoint = _Points.get(i);
                    tmpPoint.setIndex(tmpPoint.getIndex() + created);
                    updatePoints.add(tmpPoint);
                }

                Global.getDAL().updatePoints(updatePoints);
            }

            int goToPoint = _CurrentIndex + created;

            _Points = Global.getDAL().getPointsInPolygon(_CurrentPolygon.getCN());

            pointSectionsPagerAdapter.notifyDataSetChanged();

            int pointSize = _Points.size();

            if (pointSize > 0) {
                if (menuCreated) {
                    AndroidUtils.UI.enableMenuItem(miGoto);
                    AndroidUtils.UI.enableMenuItem(miLock);
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


    private void captureImage(int setting) {
        if (setting == 2) {
            Intent intent = new Intent(PointsActivity.this, TtCameraActivity.class);

            if (_CurrentPoint != null) {
                intent.putExtra(Consts.Codes.Data.POINT_CN, _CurrentPoint.getCN());
            }

            startActivityForResult(intent, Consts.Codes.Activites.TTCAMERA);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            DateTime dateTime = DateTime.now();
            String imageName = String.format("IMG_%d%d%d_%d", dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getMillisOfDay());

            File photo = new File(String.format("%s%s%s.jpg", Global.getTtMediaDir(), File.separator, imageName));
            captureImageUri = Uri.fromFile(photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, Consts.Codes.Requests.CAPTURE_IMAGE);
            } else {
                Toast.makeText(PointsActivity.this, "Unable to find a Camera application", Toast.LENGTH_LONG).show();
            }
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

                        Global.getDAL().updatePoint(_CurrentPoint);
                        updated = true;
                    } else {
                        Global.getDAL().insertPoint(_CurrentPoint);
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
                Global.getDAL().deletePointSafe(point);

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
                        TtPoint convertedPoint = Global.getDAL().getPointByCN(qndmCN);

                        for (int i = 0; i < _Points.size(); i++) {
                            if (_Points.get(i).getCN().equals(qndmCN)) {
                                _Points.set(i, convertedPoint);
                            }
                        }
                    }
                }

                if (index > INVALID_INDEX) {
                    _Points.remove(index);
                    pointSectionsPagerAdapter.notifyDataSetChanged();
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

            BasePointFragment fragment = (BasePointFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex);

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
                    } else {
                        throw new RuntimeException("Unable to delete point.");
                    }

                    createPoint(op);

                    BasePointFragment fragment = (BasePointFragment) pointSectionsPagerAdapter.getFragments().get(_CurrentIndex);
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
            saveMedia();
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
            pointSectionsPagerAdapter.notifyDataSetChanged();
            moveToPoint(_CurrentIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AndroidUtils.UI.enableMenuItem(miLock);
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
                    Global.getDAL().updatePoints(tmpPoints);
                }
            }
        }).start();
    }

    private void resetPoint() {
        if (_PointUpdated) {
            new AlertDialog.Builder(this)
            .setTitle(String.format("Reset Point %d", _CurrentPoint.getPID()))
            .setMessage(getString(R.string.points_reset_diag))
            .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    _CurrentPoint = getPointAtIndex(_CurrentIndex);
                    onPointUpdate(_CurrentPoint);
                    updateButtons();
                    setPointUpdated(false);
                    lockPoint(false);
                }
            })
            .setNeutralButton(getString(R.string.str_cancel), null)
            .show();
        }
    }


    private void saveMedia() {
        if (_MediaUpdated && _CurrentMedia != null) {
            if (!Global.getDAL().updateMedia(_CurrentMedia)) {
                Toast.makeText(PointsActivity.this,
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

        Global.getDAL().deleteMedia(media);

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
                tvPmdTitle.setText(String.format("Media (%d)", mediaCount));
            }
        }

        rvMediaAdapter.remove(media);
        mediaPagerAdapter.notifyDataSetChanged();
        mediaViewPager.setCurrentItem(index);
        setCurrentMedia(changeTo);
    }

    private void addImage(final TtImage picture) {
        if (picture != null) {
            if (Global.getDAL().insertMedia(picture)) {
                mediaSelectionIndex = TtUtils.getMediaIndex(picture, rvMediaAdapter.getItems());
                addImageToList(picture);
            } else {
                Toast.makeText(PointsActivity.this, "Error saving picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addImages(final List<TtImage> pictures) {
        if (pictures.size() > 0) {
            int error = 0;

            Collections.sort(pictures, TtUtils.PictureTimeComparator);

            for (int i = 0; i <pictures.size(); i++) {
                if (!Global.getDAL().insertMedia(pictures.get(i))) {
                    pictures.remove(i--);
                    error++;
                }
            }

            mediaSelectionIndex = TtUtils.getMediaIndex(pictures.get(0), rvMediaAdapter.getItems());

            for (TtImage p : pictures) {
                addImageToList(p);
            }

            if (error > 0) {
                Toast.makeText(PointsActivity.this, String.format("Error saving %d pictures"), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetMedia() {
        if (_MediaUpdated) {
            new AlertDialog.Builder(this)
                    .setTitle(String.format("Reset Media %s", _CurrentMedia.getName()))
                    .setMessage(String.format("This will reset this %s back to its original values.",
                            _CurrentMedia.getMediaType().toString().toLowerCase()))
                    .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            _CurrentMedia = TtUtils.cloneMedia(_BackupMedia);
                            setMediaUpdated(false);
                        }
                    })
                    .setNeutralButton(getString(R.string.str_cancel), null)
                    .show();
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

    private TtPoint getPoint(String cn) {
        for (TtPoint point : _Points) {
            if (point.getCN().equals(cn)) {
                return TtUtils.clonePoint(point);
            }
        }

        return null;
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
            pointViewPager.setCurrentItem(index, smoothScroll);
            _CurrentPoint = getPointAtIndex(index);
            _CurrentMetadata = _MetaData.get(_CurrentPoint.getMetadataCN());
            _CurrentIndex = index;
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
                _Points = Global.getDAL().getPointsInPolygon(_CurrentPolygon.getCN());

                if (_Points == null) {
                    Toast.makeText(this, "DATA ERROR", Toast.LENGTH_SHORT).show();
                    _Points = new ArrayList<>();
                    return;
                }

                pointSectionsPagerAdapter.notifyDataSetChanged();

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

                Global.Settings.ProjectSettings.setLastEditedPolyCN(polygon.getCN());
            }
        }
    }

    private void jumpToQuondam(TtPoint point) {

        final ArrayList<TtPoint> points = new ArrayList<>();
        for (String cn : point.getLinkedPoints()) {
            points.add(Global.getDAL().getPointByCN(cn));
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
        lockPoint(true);

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
    }

    private void lockPoint(boolean lockPoint) {
        if (lockPoint) {
            if (menuCreated) {
                miLock.setTitle(R.string.str_unlock);
                miLock.setIcon(R.drawable.ic_action_lock_closed);

                AndroidUtils.UI.disableMenuItem(miMovePoint);
                AndroidUtils.UI.disableMenuItem(miReset);
                AndroidUtils.UI.disableMenuItem(miDelete);
                AndroidUtils.UI.disableMenuItem(miNmeaRecalc);
                AndroidUtils.UI.disableMenuItem(miEnterLatLon);

                pmbMedia.setItemEnabled(R.id.ctx_menu_delete, false);
                pmbMedia.setItemEnabled(R.id.ctx_menu_reset, false);
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
                miLock.setIcon(R.drawable.ic_action_lock_open);

                AndroidUtils.UI.enableMenuItem(miMovePoint);
                AndroidUtils.UI.enableMenuItem(miDelete);
                AndroidUtils.UI.enableMenuItem(miNmeaRecalc);
                AndroidUtils.UI.enableMenuItem(miEnterLatLon);

                if (_CurrentMedia != null) {
                    pmbMedia.setItemEnabled(R.id.ctx_menu_delete, true);
                    pmbMedia.setItemEnabled(R.id.ctx_menu_reset, _MediaUpdated);
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
                        slidingLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
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

    private void loadMedia(TtPoint point) {
        loadMedia(point, true);
    }

    private void loadMedia(TtPoint point, boolean loadPoints) {
        if (rvMediaAdapter != null) {
            mediaViewPager.removeOnPageChangeListener(onMediaPageChangeListener);

            rvMediaAdapter.clear();
            mediaPagerAdapter.notifyDataSetChanged();

            mediaLoaded = false;

            if (point != null) {
                if (loadPoints) {
                    mediaCount = 0;
                    mediaSelectionIndex = INVALID_INDEX;

                    ArrayList<TtImage> pictures = Global.getDAL().getPicturesInPoint(point.getCN());

                    Collections.sort(pictures, TtUtils.PictureTimeComparator);
                    for (final TtImage p : pictures) {
                        addImageToList(p);
                    }

                    if (mediaCount > 0)
                        mediaSelectionIndex = 0;
                } else {
                    mediaCount = Global.getDAL().getItemsCount(
                            TwoTrailsSchema.MediaSchema.TableName,
                            TwoTrailsSchema.MediaSchema.PointCN,
                            point.getCN());
                }

                tvPmdTitle.setText(String.format("Media (%d)", mediaCount));
            } else {
                tvPmdTitle.setText(StringEx.Empty);
            }

            setCurrentMedia(null);

            mediaViewPager.addOnPageChangeListener(onMediaPageChangeListener);
        }
    }


    private void addImageToList(final TtImage picture) {
        mediaCount++;

        if (picture.isFileValid()) {
            ImageLoader.getInstance().loadImage("file://" + picture.getFilePath(), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    addImageToList(picture, true, loadedImage);
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, final FailReason failReason) {
                    addInvalidImagesToList(picture);
                }
            });
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    addInvalidImagesToList(picture);
                }
            }).start();
        }
    }

    private void addInvalidImagesToList(final TtImage picture) {
        Bitmap bitmap = BitmapFactory.decodeResource(PointsActivity.this.getResources(), R.drawable.ic_error_outline_black_48dp);
        if (bitmap != null) {
            addImageToList(picture, false, bitmap);
        }
    }

    private void addImageToList(final TtImage picture, boolean isValid, final Bitmap loadedImage) {
        if (isValid) {
            bitmapManager.put(picture.getFilePath(), picture.getFilePath(), AndroidUtils.UI.scaleMinBitmap(loadedImage, bitmapHeight, false), scaleOptions);
        } else {
            bitmapManager.put(picture.getFilePath(), Integer.toString(R.drawable.ic_error_outline_black_48dp), AndroidUtils.UI.scaleMinBitmap(loadedImage, bitmapHeight, false), scaleOptions, true);
        }

        try {
            semaphore.acquire();

            final int order = TtUtils.getMediaIndex(picture, rvMediaAdapter.getItems());

            PointsActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rvMediaAdapter.add(order, picture);

                    //don't bombard the adapter with lots of changes
                    mediaLoaderDelayedHandler.post(onMediaChanged);

                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Runnable onMediaChanged = new Runnable() {
        @Override
        public void run() {
            PointsActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mediaPagerAdapter.notifyDataSetChanged();

                    if (mediaSelectionIndex > INVALID_INDEX && mediaSelectionIndex < rvMediaAdapter.getItemCountEx()) {
                        setCurrentMedia(rvMediaAdapter.getItem(mediaSelectionIndex));
                        rvMediaAdapter.selectItem(mediaSelectionIndex);
                        mediaViewPager.setCurrentItem(mediaSelectionIndex);
                        mediaSelectionIndex = INVALID_INDEX;
                    }

                    mediaLoaded = true;
                }
            });
        }
    };


    private void setCurrentMedia(TtMedia media) {
        if (media != null) {
            if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                tvPmdTitle.setText(media.getName());
            }

            if (_CurrentMedia == null || !media.getCN().equals(_CurrentMedia.getCN())) {
                _BackupMedia = TtUtils.cloneMedia(media);

                setMediaUpdated(false);
            }
        }

        _CurrentMedia = media;

        if (_CurrentMedia == null)
            _BackupMedia = null;
    }

    private void setMediaUpdated(boolean updated) {
        _MediaUpdated = updated;

        pmbMedia.setItemEnabled(R.id.ctx_menu_reset, _MediaUpdated);
    }
    //endregion


    //region Fragment Interaction
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

    private void onMediaUpdate() {
        setMediaUpdated(true);

        if (listeners.containsKey(_CurrentMedia.getCN())) {
            listeners.get(_CurrentMedia.getCN()).onMediaUpdated(_CurrentMedia);
        }
    }

    private void onMediaUpdate(TtMedia media) {
        if (listeners.containsKey(media.getCN())) {
            listeners.get(media.getCN()).onMediaUpdated(media);
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

    public void updateMedia(TtMedia media) {
        //only update if current media
        if (_CurrentMedia.getCN().equals(media.getCN())) {
            setCurrentMedia(media);
            setMediaUpdated(true);
        }
    }


    public TtMetadata getMetadata(String cn) {
        return _MetaData.get(cn);
    }

    public HashMap<String, TtPolygon> getPolygons() {
        return _Polygons;
    }

    public BitmapManager getBitmapManager() {
        return bitmapManager;
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
            intent.putExtra(Consts.Codes.Data.POINT_DATA, TtUtils.clonePoint(point));
            intent.putExtra(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);
            intent.putExtra(Consts.Codes.Data.METADATA_DATA, _MetaData.get(point.getMetadataCN()));

            if (bursts != null && bursts.size() > 0) {
                try {
                    intent.putExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA, TtNmeaBurst.burstsToByteArray(bursts));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            startActivityForResult(intent, Consts.Codes.Activites.ACQUIRE);
        }
    }

    private void acquireT5Points(TtPoint currentPoint) {
        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            configGps();
        } else {
            Intent intent = new Intent(this, Take5Activity.class);

            if (currentPoint != null) {
                intent.putExtra(Consts.Codes.Data.POINT_DATA, TtUtils.clonePoint(currentPoint));
            }

            if (_CurrentMetadata != null) {
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, _CurrentMetadata);
            } else {
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, _MetaData.get(Consts.EmptyGuid));
            }

            intent.putExtra(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);

            startActivityForResult(intent, Consts.Codes.Activites.TAKE5);
        }
    }

    private void acquireWalkPoints(TtPoint currentPoint) {
        if (!Global.Settings.DeviceSettings.isGpsConfigured()) {
            configGps();
        } else {
            Intent intent = new Intent(this, WalkActivity.class);

            if (currentPoint != null) {
                intent.putExtra(Consts.Codes.Data.POINT_DATA, TtUtils.clonePoint(currentPoint));
            }

            if (_CurrentMetadata != null) {
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, _CurrentMetadata);
            } else {
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, _MetaData.get(Consts.EmptyGuid));
            }

            intent.putExtra(Consts.Codes.Data.POLYGON_DATA, _CurrentPolygon);

            startActivityForResult(intent, Consts.Codes.Activites.WALK);
        }
    }

    private void calculateGpsPoint() {
        if (_CurrentPoint != null && _CurrentPoint.isGpsType()) {
            ArrayList<TtNmeaBurst> bursts = Global.getDAL().getNmeaBurstsByPointCN(_CurrentPoint.getCN());

            if (bursts.size() > 0) {
                Intent intent = new Intent(this, CalculateGpsActivity.class);
                intent.putExtra(Consts.Codes.Data.POINT_DATA, TtUtils.clonePoint(_CurrentPoint));
                intent.putExtra(Consts.Codes.Data.METADATA_DATA, _MetaData.get(_CurrentPoint.getMetadataCN()));

                try {
                    intent.putExtra(Consts.Codes.Data.ADDITIVE_NMEA_DATA, TtNmeaBurst.burstsToByteArray(bursts));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                startActivityForResult(intent, Consts.Codes.Activites.CALCULATE);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage("This point has no NMEA data associated with it. Would you like to acquire some data?");

                alert.setPositiveButton("Acquire NMEA", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getBaseContext(), AcquireGpsActivity.class);
                        intent.putExtra(Consts.Codes.Data.POINT_DATA, new GpsPoint(_CurrentPoint));
                        intent.putExtra(Consts.Codes.Data.METADATA_DATA, _MetaData.get(_CurrentPoint.getMetadataCN()));

                        startActivityForResult(intent, Consts.Codes.Activites.ACQUIRE);
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
                            AlertDialog.Builder dialog = new AlertDialog.Builder(PointsActivity.this);

                            dialog.setMessage(R.string.points_aqr_diag_gps_msg);

                            dialog.setPositiveButton(R.string.points_aqr_diag_add, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ArrayList<TtNmeaBurst> bursts = Global.getDAL().getNmeaBurstsByPointCN(_CurrentPoint.getCN());
                                    acquireGpsPoint(_CurrentPoint, bursts);
                                }
                            });

                            dialog.setNegativeButton(R.string.points_aqr_diag_overwrite, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog.Builder dialogA = new AlertDialog.Builder(PointsActivity.this);

                                    dialogA.setMessage(R.string.points_aqr_diag_del_msg);

                                    dialogA.setPositiveButton(R.string.str_delete, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Global.getDAL().deleteNmeaByPointCN(_CurrentPoint.getCN());
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


    public class PointsPagerAdapter extends FragmentStatePagerAdapterEx {

        public PointsPagerAdapter(FragmentManager fm) {
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
            if (listeners != null) {
                listeners.clear();
            }

            super.notifyDataSetChanged();
        }
    }


    public class MediaPagerAdapter extends FragmentStatePagerAdapterEx {
        public MediaPagerAdapter(FragmentManager fm) {
            super(fm);
            saveFragmentStates(false);
        }

        @Override
        public Fragment getItem(int position) {
            TtMedia media = rvMediaAdapter.getItem(position);

            switch (media.getMediaType()) {
                case Picture:
                    return PictureMediaFragment.newInstance((TtImage) media);
                case Video:
                    break;
            }

            return null;
        }

        @Override
        public int getCount() {
            return rvMediaAdapter.getItemCountEx();
        }
    }

    public interface Listener {
        void onLockChange(boolean locked);
        void onPointUpdated(TtPoint point);
        void onMediaUpdated(TtMedia media);
    }
}
