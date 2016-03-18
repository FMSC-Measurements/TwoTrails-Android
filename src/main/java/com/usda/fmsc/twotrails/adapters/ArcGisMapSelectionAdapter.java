package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.ArcGisMapLayer;

import java.util.List;

public class ArcGisMapSelectionAdapter extends ArrayAdapter<ArcGisMapLayer> {
    private IArcGisMapAdapterListener listener;

    private LayoutInflater inflater;
    private List<ArcGisMapLayer> maps;

    private Drawable dOnline, dOffline;

    private View selectedView;
    private int selectedIndex;

    public ArcGisMapSelectionAdapter(Context context, List<ArcGisMapLayer> maps) {
        this(context, maps, -1, null);
    }

    public ArcGisMapSelectionAdapter(Context context, List<ArcGisMapLayer> maps, int defaultSelectedIndex, IArcGisMapAdapterListener listener) {
        super(context, 0, 0, maps);

        this.maps = maps;
        inflater = LayoutInflater.from(context);

        dOnline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_online_primary_36);
        dOffline = AndroidUtils.UI.getDrawable(context, R.drawable.ic_offline_primary_36);

        selectedIndex = defaultSelectedIndex;

        this.listener = listener;
    }

    @Override
    public ArcGisMapLayer getItem(int position) {
        return maps.get(position);
    }

    @Override
    public int getCount() {
        return maps.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ArcGisMapLayer map = getItem(position);

        MapViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.content_details_map, null);

            holder = new MapViewHolder(convertView);
            convertView.setTag(holder);

            convertView.setBackgroundResource(R.drawable.list_item_selector);

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
                        listener.onArcGisMapSelected(map);
                    }
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    new AlertDialog.Builder(getContext())
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

        holder.ivStatusIcon.setImageDrawable(map.isOnline() ? dOnline : dOffline);
        holder.tvName.setText(map.getName());
        holder.tvDesc.setText(map.getDescription());

        if (position == selectedIndex) {
            convertView.setSelected(true);
        }

        return  convertView;
    }

    public boolean isMapSelected() {
        return selectedIndex > -1;
    }

    public ArcGisMapLayer getSelectedMap() {
        if (selectedIndex > -1 && selectedIndex < maps.size()) {
            return maps.get(selectedIndex);
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
        ImageView ivStatusIcon;
        TextView tvName, tvDesc;

        public MapViewHolder(View view) {
            ivStatusIcon = (ImageView)view.findViewById(R.id.image);
            tvName = (TextView)view.findViewById(R.id.text1);
            tvDesc = (TextView)view.findViewById(R.id.text2);
        }
    }

    public interface IArcGisMapAdapterListener {
        void onArcGisMapSelected(ArcGisMapLayer map);
    }


}
