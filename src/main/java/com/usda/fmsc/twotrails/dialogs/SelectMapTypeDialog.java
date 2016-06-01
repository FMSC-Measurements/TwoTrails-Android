package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.adapters.ArcGisMapSelectionAdapter;
import com.usda.fmsc.twotrails.adapters.GoogleMapSelectionAdapter;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.units.GoogleMapType;
import com.usda.fmsc.twotrails.units.MapType;

import java.util.ArrayList;
import java.util.List;

public class SelectMapTypeDialog extends DialogFragment {
    private static final String ARC_MAP_LAYERS = "ArcMapLayers";
    private static final String SELECT_MAP_MODE = "MadeMode";

    private OnMapSelectedListener listener;

    private List<ArcGisMapLayer> mapLayers;

    private SelectMapMode mode;
    private MapType mapType, defaultMapType;
    private int mapId = -1, defaultMapId;

    private LayoutInflater inflater;
    private View arcView, gmapView;
    private ListView lvArcMap;
    private ArcGisMapSelectionAdapter arcMapAdapter;
    private GoogleMapSelectionAdapter gMapAdapter;


    public static SelectMapTypeDialog newInstance(ArrayList<ArcGisMapLayer> layers) {
        return newInstance(layers, SelectMapMode.ALL);
    }

    public static SelectMapTypeDialog newInstance(ArrayList<ArcGisMapLayer> layers, SelectMapMode mode) {
        SelectMapTypeDialog dialog = new SelectMapTypeDialog();

        Bundle bundle = new Bundle();

        try {
            bundle.putInt(SELECT_MAP_MODE, mode.getValue());
            bundle.putParcelableArrayList(ARC_MAP_LAYERS, layers);
        } catch (Exception e) {
            e.printStackTrace();
        }


        dialog.setArguments(bundle);

        return dialog;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(ARC_MAP_LAYERS)) {
            try {
                mapLayers = bundle.getParcelableArrayList(ARC_MAP_LAYERS);
                mode = SelectMapMode.parse(bundle.getInt(SELECT_MAP_MODE, 0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

        inflater = LayoutInflater.from(getContext());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AndroidUtils.UI.setOverscrollColor(getResources(), getContext(), R.color.primary);
        }

        FrameLayout fl = new FrameLayout(getActivity());

        View view = inflater.inflate(mode == SelectMapMode.ALL ? R.layout.diag_select_map :  R.layout.diag_select_map_arc_map, fl);

        if (mode != SelectMapMode.ALL) {
            Toolbar toolbar = (Toolbar)view.findViewById(R.id.toolbar);

            toolbar.setTitle(String.format("%sArc Maps",
                    mode == SelectMapMode.ALL_ARC ? "" :
                    mode == SelectMapMode.ARC_ONLINE ? "Online " : "Offline "));

            lvArcMap = (ListView)view.findViewById(R.id.diagSelectArcMapListView);


            List<ArcGisMapLayer> amls;

            if (mode == SelectMapMode.ALL_ARC) {
                amls = mapLayers;
            } else {
                amls = new ArrayList<>();
                boolean online = mode == SelectMapMode.ARC_ONLINE;
                for (ArcGisMapLayer layer : mapLayers) {
                    if ((online && layer.isOnline()) || (!online && !layer.isOnline()))
                        amls.add(layer);
                }
            }

            arcMapAdapter = new ArcGisMapSelectionAdapter(getContext(), amls, -1, new ArcGisMapSelectionAdapter.IArcGisMapAdapterListener() {
                @Override
                public void onArcGisMapSelected(ArcGisMapLayer map) {
                    mapType = MapType.ArcGIS;
                    mapId = map.getId();

                    onMapSelected();
                }
            });
            lvArcMap.setAdapter(arcMapAdapter);
        } else {
            final MapTypePagerAdapter pagerAdapter = new MapTypePagerAdapter();

            // Set up the ViewPager with the sections arcMapAdapter.
            ViewPager viewPager = (ViewPager)view.findViewById(R.id.diagViewPager);
            viewPager.setAdapter(pagerAdapter);

            //Setup Tabs
            final TabLayout tabLayout = (TabLayout)view.findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
        }

        dialog.setView(fl)
        .setNeutralButton(R.string.str_cancel, null);

        return dialog.create();
    }

    public SelectMapTypeDialog setOnMapSelectedListener(OnMapSelectedListener listener) {
        this.listener = listener;
        return this;
    }


    private void onMapSelected() {
        if (listener != null && (mapType != null && mapId != -1 &&
                (mapType != defaultMapType || mapId != defaultMapId))) {
            listener.mapSelected(mapType, mapId);
        }

        dismiss();
    }


    //Change to PagerAdapter instead of Fragment
    private class MapTypePagerAdapter extends PagerAdapter implements
            ArcGisMapSelectionAdapter.IArcGisMapAdapterListener,
            GoogleMapSelectionAdapter.IGoogleMapAdapterListener {

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;

            if (position == 0) {
                if (arcView == null) {
                    arcView = inflater.inflate(R.layout.content_list_view, null);

                    lvArcMap = (ListView)arcView.findViewById(R.id.listView);
                    arcMapAdapter = new ArcGisMapSelectionAdapter(getContext(), mapLayers, -1, this);
                    lvArcMap.setAdapter(arcMapAdapter);
                }

                view = arcView;
            } else {
                if (gmapView == null) {
                    gmapView = inflater.inflate(R.layout.content_list_view, null);

                    ListView lvGmap = (ListView)gmapView.findViewById(R.id.listView);
                    gMapAdapter = new GoogleMapSelectionAdapter(getContext(), -1, this);
                    lvGmap.setAdapter(gMapAdapter);
                }

                view = gmapView;
            }

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }


        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                default:
                case 0: return "ArcGIS";
                case 1: return "Google";
            }
        }


        @Override
        public void onArcGisMapSelected(ArcGisMapLayer map) {
            mapType = MapType.ArcGIS;
            mapId = map.getId();

            gMapAdapter.deselectMap();

            onMapSelected();
        }

        @Override
        public void onGoogleMapSelected(GoogleMapType map) {
            mapType = MapType.Google;
            mapId = map.getValue();

            arcMapAdapter.deselectMap();

            onMapSelected();
        }
    }


    public interface OnMapSelectedListener {
        void mapSelected(MapType mapType, int mapId);
    }


    public enum SelectMapMode {
        ALL(0),
        ALL_ARC(1),
        ARC_OFFLINE(2),
        ARC_ONLINE(3);

        private final int value;

        SelectMapMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SelectMapMode parse(int id) {
            SelectMapMode[] dists = values();
            if(dists.length > id && id > -1)
                return dists[id];
            throw new IllegalArgumentException("Invalid SelectMapMode id: " + id);
        }

        @Override
        public String toString() {
            switch(this) {
                case ALL: return "ALL";
                case ALL_ARC: return "ALL ARC";
                case ARC_OFFLINE: return "ARC OFFLINE";
                case ARC_ONLINE: return "ARC_ONLINE";
                default: throw new IllegalArgumentException();
            }
        }
    }
}
