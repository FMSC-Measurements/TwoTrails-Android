package com.usda.fmsc.twotrails.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.Transitions.ElevationTransition;
import com.usda.fmsc.android.dialogs.InputDialog;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.SheetFab;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.dialogs.NewArcMapDialogTt;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.ui.MSFloatingActionButton;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;
import com.usda.fmsc.utilities.IListener;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class MapManagerActivity extends CustomToolbarActivity implements ArcGISTools.IArcToolsListener {
    private static final String SELECT_MAP = "SelectMap";

    private SheetFab<MSFloatingActionButton> fabSheet;

    private ArcGisMapAdapter adapter;
    private ArrayList<ArcGisMapLayer> maps, visibleMaps;

    private boolean inDetails = false;
    private int notifyAdapter = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map_manager);

        getWindow().getSharedElementReenterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {

            }

            @Override
            public void onTransitionEnd(Transition transition) {
                inDetails = false;

                if (notifyAdapter > -1) {
                    adapter.notifyItemRemoved(notifyAdapter);
                    notifyAdapter = -1;
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });

        maps = new ArrayList<>(getTtAppCtx().getArcGISTools().getMapLayers());
        visibleMaps = new ArrayList<>();

        maps.sort(ArcGisMapLayer.Comparator);

        visibleMaps.addAll(maps);

        RecyclerViewEx rvMaps = findViewById(R.id.mmRvMaps);

        if (rvMaps != null) {
            rvMaps.setViewHasFooter(true);
            rvMaps.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this));
            rvMaps.setHasFixedSize(true);
            rvMaps.setItemAnimator(new SlideInLeftAnimator());

            adapter = new ArcGisMapAdapter(this);
            rvMaps.setAdapter(adapter);
        }

        MSFloatingActionButton fabMenu = findViewById(R.id.mmFabMenu);
        View overlay = findViewById(R.id.overlay);
        View sheetView = findViewById(R.id.fab_sheet);

        int bc = AndroidUtils.UI.getColor(this, R.color.background_card_view);
        int fc = AndroidUtils.UI.getColor(this, R.color.primaryLight);

        fabSheet = new SheetFab<>(fabMenu, sheetView, overlay, bc, fc);

        getTtAppCtx().getArcGISTools().addListener(this);
    }

    @Override
    protected void onDestroy() {
        getTtAppCtx().getArcGISTools().removeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.Codes.Activities.MAP_DETAILS) {
            if (data != null && data.hasExtra(Consts.Codes.Data.MAP_DATA)) {
                ArcGisMapLayer layer = data.getParcelableExtra(Consts.Codes.Data.MAP_DATA);

                if (layer != null) {
                    if (resultCode == Consts.Codes.Results.MAP_UPDATED) {
                        updateMap(layer);
                    } else if (resultCode == Consts.Codes.Results.MAP_DELETED) {
                        removeMap(layer, true);
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateMap(ArcGisMapLayer layer) {
        for (ArcGisMapLayer agml : maps) {
            if (agml.getId() == layer.getId()) {
                agml.update(layer);
                break;
            }
        }

        int pos = 0;
        for (ArcGisMapLayer agml : visibleMaps) {
            if (agml.getId() == layer.getId()) {
                agml.update(layer);
                adapter.notifyItemChanged(pos);
                break;
            }
            pos++;
        }
    }

    private void removeMap(ArcGisMapLayer layer, boolean fromMapDetails) {
        boolean mr = false, vmr = false;

        for (int i = 0; i < maps.size(); i++) {
            if (!mr && maps.get(i).getId() == layer.getId()) {
                mr = true;
            }

            if (!vmr && visibleMaps.get(i).getId() == layer.getId()) {
                vmr = true;
            }

            if (vmr && mr) {
                maps.remove(i);
                visibleMaps.remove(i);

                if (!inDetails || !fromMapDetails)
                    adapter.notifyItemRemoved(i);
                else
                    notifyAdapter = i;

                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        inflateMenu(R.menu.menu_map_manager, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.mmMenuLogin) {
            Toast.makeText(MapManagerActivity.this, "ArcGIS Login is currently not supported.", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void arcLayerAdded(final ArcGisMapLayer layer) {
        runOnUiThread(() -> {
            maps.add(layer);
            visibleMaps.add(layer);
            adapter.notifyDataSetChanged();
        });
    }


    public void btnMmAddClick(View view) {
        fabSheet.showSheet();
    }

    public void btnMmAddOnlineClick(View view) {
        fabSheet.hideSheet();

        Toast.makeText(MapManagerActivity.this, "Adding custom online maps is currently not supported.", Toast.LENGTH_LONG).show();
    }

    public void btnMmAddOfflineClick(View view) {
        fabSheet.hideSheet();
        createMap(null, null, NewArcMapDialogTt.CreateMode.OFFLINE_FROM_FILE); //addOfflineMap();
    }

    private void createMap(String name, String uri, NewArcMapDialogTt.CreateMode mode) {
        NewArcMapDialogTt.newInstance(name, uri, mode)
            .show(getSupportFragmentManager(), SELECT_MAP);
    }


    private class ArcGisMapAdapter extends RecyclerViewEx.BaseAdapterEx {
        private final Drawable dOffline, dOnline, dOfflineInvalid;
        private final LayoutInflater inflater;

        private ArcGisMapAdapter(Context context) {
            super(context);

            inflater = LayoutInflater.from(context);
            dOnline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_online_primary_36);
            dOffline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_offline_primary_36);
            dOfflineInvalid = AndroidUtils.UI.getDrawable(context, R.drawable.ic_offline_red_36);
        }

        @Override
        public RecyclerViewEx.ViewHolderEx onCreateViewHolderEx(ViewGroup parent, int viewType) {
            return new MapViewHolder(inflater.inflate(R.layout.content_map_info, parent, false));
        }

        @Override
        public void onBindViewHolderEx(RecyclerViewEx.ViewHolderEx holder, int position) {
            ((MapViewHolder)holder).bind(visibleMaps.get(position));
        }

        @Override
        public int getItemCountEx() {
            return visibleMaps.size();
        }

        private class MapViewHolder extends RecyclerViewEx.ViewHolderEx {
            public final ImageView ivStatusIcon;
            public final PopupMenuButton ofmbMenu;
            public final TextView tvName;
            private final View lay;

            private MapViewHolder(View view) {
                super(view);

                ivStatusIcon = view.findViewById(R.id.mhIcon);
                tvName = view.findViewById(R.id.mhName);
                ofmbMenu = view.findViewById(R.id.mhMenu);
                lay = view.findViewById(R.id.lay1);
            }

            private void bind(final ArcGisMapLayer layer) {
                ivStatusIcon.setImageDrawable(layer.isOnline() ? dOnline :
                        layer.hasValidFile() ? dOffline : dOfflineInvalid);
                tvName.setText(layer.getName());

                ofmbMenu.setListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.ctx_menu_rename) {
                        final InputDialog id = new InputDialog(MapManagerActivity.this);

                        id.setInputText(layer.getName())
                                .setPositiveButton(R.string.str_rename, (dialog, which) -> {
                                    getTtAppCtx().getArcGISTools().updateMapLayer(layer);
                                    layer.setName(id.getText());
                                    tvName.setText(id.getText());
                                })
                                .setNeutralButton(R.string.str_cancel, null)
                                .show();
                    } else if (itemId == R.id.ctx_menu_delete) {
                        new AlertDialog.Builder(MapManagerActivity.this)
                                .setMessage("Arc you sure you want to delete this map?")
                                .setPositiveButton(R.string.str_delete, (dialog, which) -> getTtAppCtx().getArcGISTools().deleteMapLayer(MapManagerActivity.this, layer.getId(), true, new IListener() {
                                    @Override
                                    public void onEventTriggered(Object o) {
                                        removeMap(layer, false);
                                    }
                                }))
                                .setNeutralButton(R.string.str_cancel, null)
                                .show();
                    }

                    return false;
                });

                lay.setOnClickListener(v -> viewMapDetails(lay, layer));
            }
        }

        private void viewMapDetails(View view, ArcGisMapLayer agml) {
            Intent i = new Intent(MapManagerActivity.this, MapDetailsActivity.class);
            i.putExtra(Consts.Codes.Data.MAP_DATA, agml);

            inDetails = true;
            ElevationTransition.startTransition(MapManagerActivity.this, i, Consts.Codes.Activities.MAP_DETAILS,
                    Pair.create(findViewById(R.id.toolbar), getString(R.string.trans_toolbar)),
                    Pair.create(view, getString(R.string.trans_map_details)));
        }
    }
}
