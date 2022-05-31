package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;

import java.util.List;

public class MetadataDetailsSpinnerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<TtMetadata> metadata;
    private final int itemView;


    public MetadataDetailsSpinnerAdapter(Context context, List<TtMetadata> metadata) {
        this(context, metadata, android.R.layout.simple_spinner_item);
    }

    public MetadataDetailsSpinnerAdapter(Context context, List<TtMetadata> metadata, int itemView) {
        this.metadata = metadata;
        this.itemView = itemView;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public int getCount() {
        return metadata.size();
    }

    @Override
    public TtMetadata getItem(int position) {
        return metadata.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(itemView, parent, false);
            mViewHolder = new ViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.text = convertView.findViewById(android.R.id.text1);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        TtMetadata meta = getItem(position);

        mViewHolder.text.setText(meta.getName());

        return convertView;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        DropDownViewHolder mViewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.content_details_meta, parent, false);
            mViewHolder = new DropDownViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.name = convertView.findViewById(R.id.listRowDiagPointEditorTvName);
            mViewHolder.dist = convertView.findViewById(R.id.listRowDiagPointEditorTvDist);
            mViewHolder.elev = convertView.findViewById(R.id.listRowDiagPointEditorTvElev);
            mViewHolder.parent = convertView.findViewById(R.id.parent);
        } else {
            mViewHolder = (DropDownViewHolder) convertView.getTag();
        }

        TtMetadata meta = getItem(position);

        mViewHolder.name.setText(meta.getName());
        mViewHolder.dist.setText(meta.getDistance().toString());
        mViewHolder.elev.setText(meta.getElevation().toString());

        return convertView;
    }

    private class DropDownViewHolder {
        public TextView name, dist, elev;
        public View parent;
    }

    private class ViewHolder {
        public TextView text;
    }
}
