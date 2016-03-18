package com.usda.fmsc.twotrails.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.adapters.ArcGisMapSelectionAdapter;
import com.usda.fmsc.twotrails.adapters.GoogleMapSelectionAdapter;
import com.usda.fmsc.twotrails.objects.ArcGisMapLayer;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.IOException;
import java.util.List;

public class SelectMapTypeDialog extends DialogFragment {
    private static final String ARC_MAP_LAYERS = "ArcMapLayers";

    OnMapSelectedListener listener;

    List<ArcGisMapLayer> mapLayers;

    ViewPager viewPager;

    Units.MapType mapType, defaultMapType;
    int mapId = -1, defaultMapId;

    public static SelectMapTypeDialog newInstance(List<ArcGisMapLayer> layers) {
        SelectMapTypeDialog dialog = new SelectMapTypeDialog();

        Bundle bundle = new Bundle();

        try {
            bundle.putByteArray(ARC_MAP_LAYERS, TtUtils.Convert.listToByteArray(layers));
        } catch (IOException e) {
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
                mapLayers = (List<ArcGisMapLayer>) TtUtils.Convert.bytesToList(bundle.getByteArray(ARC_MAP_LAYERS));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

        LayoutInflater inflater = LayoutInflater.from(getContext());

        AndroidUtils.UI.setOverscrollColor(getResources(), getContext(), getResources().getColor(R.color.primary));

        FrameLayout fl = new FrameLayout(getActivity());

        View view = inflater.inflate(R.layout.diag_select_map, fl);

        final MapTypePagerAdapter pagerAdapter = new MapTypePagerAdapter(inflater);

        // Set up the ViewPager with the sections arcMapAdapter.
        viewPager = (ViewPager)view.findViewById(R.id.diagViewPager);
        viewPager.setAdapter(pagerAdapter);

        //Setup Tabs
        final TabLayout tabLayout = (TabLayout)view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        dialog.setView(fl);

        dialog.setPositiveButton("Set Map", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null && (mapType != null && mapId != -1 &&
                        (mapType != defaultMapType || mapId != defaultMapId))) {
                    listener.mapSelected(mapType, mapId);
                }
            }
        })
        .setNeutralButton(R.string.str_cancel, null);

        return dialog.create();
    }

    public void setOnMapSelectedListener(OnMapSelectedListener listener) {
        this.listener = listener;
    }


    //Change to PagerAdapter instead of Fragment
    private class MapTypePagerAdapter extends PagerAdapter implements
            ArcGisMapSelectionAdapter.IArcGisMapAdapterListener,
            GoogleMapSelectionAdapter.IGoogleMapAdapterListener {

        LayoutInflater inflater;
        View arcView, gmapView;
        ListView lvArcMap, lvGmap;
        ArcGisMapSelectionAdapter arcMapAdapter;
        GoogleMapSelectionAdapter gMapAdapter;

        public MapTypePagerAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;

            if (position == 0) {
                if (arcView == null) {
                    arcView = inflater.inflate(R.layout.diag_select_map_arc_map, null);

                    lvArcMap = (ListView)arcView.findViewById(R.id.diagSelectArcMapListView);
                    arcMapAdapter = new ArcGisMapSelectionAdapter(getContext(), mapLayers, -1, this);
                    lvArcMap.setAdapter(arcMapAdapter);
                }

                view = arcView;
            } else {
                if (gmapView == null) {
                    gmapView = inflater.inflate(R.layout.diag_select_map_google_map, null);

                    lvGmap = (ListView)gmapView.findViewById(R.id.diagSelectGoogleMapListView);
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
            mapType = Units.MapType.ArcGIS;
            mapId = map.getId();

            gMapAdapter.deselectMap();
        }

        @Override
        public void onGoogleMapSelected(Units.GoogleMapType map) {
            mapType = Units.MapType.Google;
            mapId = map.getValue();

            arcMapAdapter.deselectMap();
        }
    }


    public interface OnMapSelectedListener {
        void mapSelected(Units.MapType mapType, int mapId);
    }
}
