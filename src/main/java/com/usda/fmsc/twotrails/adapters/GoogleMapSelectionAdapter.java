package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.Units;

import java.util.Arrays;
import java.util.List;

public class GoogleMapSelectionAdapter extends BaseAdapter {

    Context context;

    LayoutInflater inflater;
    IGoogleMapAdapterListener listener;

    List<Units.GoogleMapType> mapTypes = Arrays.asList(Units.GoogleMapType.values());
    private View selectedView;
    private int selectedIndex;


    public GoogleMapSelectionAdapter(Context context, int defaultType, IGoogleMapAdapterListener listener) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        selectedIndex = defaultType - 1;

        this.listener = listener;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Units.GoogleMapType getItem(int position) {
        return mapTypes.get(position + 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Units.GoogleMapType map = getItem(position);

        MapViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.content_map_header, null);

            holder = new MapViewHolder(convertView);
            convertView.setTag(holder);

            //convertView.setBackgroundResource(R.drawable.list_item_selector);

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

        if (position == selectedIndex) {
            convertView.setSelected(true);
        }

        return  convertView;
    }

    public boolean isMapSelected() {
        return selectedIndex > -1;
    }

    public Units.GoogleMapType getSelectedMap() {
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

        public MapViewHolder(View view) {
            tvName = (TextView)view.findViewById(R.id.mhName);
            ofmbMenu = (PopupMenuButton)view.findViewById(R.id.mhMenu);
        }
    }

    public interface IGoogleMapAdapterListener {
        void onGoogleMapSelected(Units.GoogleMapType map);
    }
}
