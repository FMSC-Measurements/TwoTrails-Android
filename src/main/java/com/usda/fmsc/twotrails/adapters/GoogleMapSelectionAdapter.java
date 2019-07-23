package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.units.GoogleMapType;

import java.util.Arrays;
import java.util.List;

public class GoogleMapSelectionAdapter extends BaseAdapter {
    private Context context;

    private LayoutInflater inflater;
    private IGoogleMapAdapterListener listener;

    private Drawable dMapOffline;

    private List<GoogleMapType> mapTypes = Arrays.asList(GoogleMapType.values());
    private View selectedView;
    private int selectedIndex;


    public GoogleMapSelectionAdapter(Context context, int defaultType, IGoogleMapAdapterListener listener) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        selectedIndex = defaultType;

        dMapOffline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_offline_primary_36);

        this.listener = listener;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public GoogleMapType getItem(int position) {
        return mapTypes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final GoogleMapType map = getItem(position);

        MapViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.content_map_header, null);

            holder = new MapViewHolder(convertView);
            convertView.setTag(holder);

            final View fview = convertView;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedView != null) {
                        selectedView.setSelected(false);
                    }

                    fview.setSelected(true);
                    selectedView = fview;
                    selectedIndex = position;

                    if (listener != null) {
                        listener.onGoogleMapSelected(map);
                    }
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    new AlertDialog.Builder(context)
                            .setTitle(map.getName())
                            .setMessage(map.getDescription())
                            .setPositiveButton(R.string.str_ok, null)
                            .show();
                    return true;
                }
            });
        } else {
            holder = (MapViewHolder)convertView.getTag();
        }

        holder.tvName.setText(map.getName());
        holder.ofmbMenu.setVisibility(View.INVISIBLE);
        holder.ofmbMenu.setEnabled(false);

        if (map == GoogleMapType.MAP_TYPE_NONE) {
            holder.ivMhIcon.setImageDrawable(dMapOffline);
        }

        if (position == selectedIndex) {
            convertView.setSelected(true);
        }

        return  convertView;
    }

    public boolean isMapSelected() {
        return selectedIndex > -1;
    }

    public GoogleMapType getSelectedMap() {
        if (selectedIndex > -1) {
            return getItem(selectedIndex);
        }

        throw new NullPointerException("No Selected Map");
    }

    public void deselectMap() {
        if (selectedView != null) {
            selectedView.setSelected(false);
            selectedView = null;
        }

        selectedIndex = -1;
    }

    private class MapViewHolder {
        TextView tvName;
        PopupMenuButton ofmbMenu;
        ImageView ivMhIcon;

        private MapViewHolder(View view) {
            tvName = view.findViewById(R.id.mhName);
            ofmbMenu = view.findViewById(R.id.mhMenu);
            ivMhIcon = view.findViewById(R.id.mhIcon);
        }
    }

    public interface IGoogleMapAdapterListener {
        void onGoogleMapSelected(GoogleMapType map);
    }
}
