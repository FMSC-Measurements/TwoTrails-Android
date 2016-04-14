package com.usda.fmsc.twotrails.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.SheetFab;
import com.usda.fmsc.android.widget.layoutmanagers.LinearLayoutManagerWithSmoothScroller;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.dialogs.ArcMapFromUrlDialog;
import com.usda.fmsc.twotrails.dialogs.SelectMapTypeDialog;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.ui.MSFloatingActionButton;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class MapManagerActivity extends CustomToolbarActivity {
    private static final String SELECT_MAP = "SelectMap";

    RecyclerViewEx rvMaps;
    SheetFab fabSheet;

    ArcGisMapAdapter adapter;
    ArrayList<ArcGisMapLayer> maps, visibleMaps, onlineMaps, offlineMaps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map_manager);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        maps = new ArrayList<>(ArcGISTools.getLayers());
        visibleMaps = new ArrayList<>();
        onlineMaps = new ArrayList<>();
        offlineMaps = new ArrayList<>();

        for (ArcGisMapLayer layer : maps) {
            if (layer.isOnline())
                onlineMaps.add(layer);
            else
                offlineMaps.add(layer);

            visibleMaps.add(layer);
        }

        rvMaps = (RecyclerViewEx) findViewById(R.id.mmRvMaps);
        rvMaps.setViewHasFooter(true);
        rvMaps.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this));
        rvMaps.setHasFixedSize(true);
        rvMaps.setItemAnimator(new SlideInUpAnimator());

        adapter = new ArcGisMapAdapter(this);
        rvMaps.setAdapter(adapter);


        MSFloatingActionButton fabMenu = (MSFloatingActionButton)findViewById(R.id.mmFabMenu);
        View overlay = findViewById(R.id.overlay);
        View sheetView = findViewById(R.id.fab_sheet);

        int bc = AndroidUtils.UI.getColor(this, R.color.background_card_view);
        int fc = AndroidUtils.UI.getColor(this, R.color.primaryLight);

        fabSheet = new SheetFab<>(fabMenu, sheetView, overlay, bc, fc);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void btnMmAddClick(View view) {
        fabSheet.showSheet();
    }

    public void btnMmAddOnlineClick(View view) {
        fabSheet.hideSheet();
        createMap(null, null, true);
    }

    public void btnMmAddOfflineClick(View view) {
        fabSheet.hideSheet();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("Create Offline map from:")
                .setPositiveButton("Online Map", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectMapTypeDialog odialog = SelectMapTypeDialog.newInstance(onlineMaps, true);

                        odialog.setOnMapSelectedListener(new SelectMapTypeDialog.OnMapSelectedListener() {
                            @Override
                            public void mapSelected(Units.MapType mapType, int mapId) {
                                ArcGisMapLayer layer = ArcGISTools.getMapLayer(mapId);
                                createMap(String.format("%s (Offline)", layer.getName()), layer.getUri(), false);
                            }
                        });

                        odialog.show(getSupportFragmentManager(), SELECT_MAP);
                    }
                })
                .setNegativeButton("Url", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createMap(null, null, false);
                    }
                })
                .setNeutralButton(R.string.str_cancel, null)
                .show();
    }

    private void createMap(String name, String url, boolean isOnline) {
        ArcMapFromUrlDialog odialog = ArcMapFromUrlDialog.newInstance(name, url, isOnline);
        odialog.show(getSupportFragmentManager(), SELECT_MAP);
    }


    private class ArcGisMapAdapter extends RecyclerViewEx.BaseAdapterEx {
        private Drawable dOffline, dOnline;
        private LayoutInflater inflater;

        public ArcGisMapAdapter(Context context) {
            super(context);

            inflater = LayoutInflater.from(context);
            dOnline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_online_primary_36);
            dOffline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_offline_primary_36);
        }

        @Override
        public RecyclerViewEx.ViewHolderEx onCreateViewHolderEx(ViewGroup parent, int viewType) {
            return new MapViewHolder(inflater.inflate(R.layout.content_details_map, null));
        }

        @Override
        public void onBindViewHolderEx(RecyclerViewEx.ViewHolderEx holder, int position) {
            ((MapViewHolder)holder).bind(visibleMaps.get(position));
        }

        @Override
        public int getItemCountEx() {
            return visibleMaps.size();
        }

        class MapViewHolder extends RecyclerViewEx.ViewHolderEx {
            ImageView ivStatusIcon;
            TextView tvName, tvDesc;
            boolean collpased = true;

            public MapViewHolder(View view) {
                super(view);

                ivStatusIcon = (ImageView) view.findViewById(R.id.image);
                tvName = (TextView) view.findViewById(R.id.text1);
                tvDesc = (TextView) view.findViewById(R.id.text2);

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (collpased) {
                            collpased = false;
                            AndroidUtils.Animation.expandTextView(tvDesc);
                        } else {
                            collpased = true;
                            AndroidUtils.Animation.collapseTextView(tvDesc, 1);
                        }
                        return true;
                    }
                });
            }

            public void bind(ArcGisMapLayer layer) {
                ivStatusIcon.setImageDrawable(layer.isOnline() ? dOnline : dOffline);
                tvName.setText(layer.getName());
                tvDesc.setText(layer.getDescription());
            }
        }
    }
}
