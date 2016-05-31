package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;

import java.util.List;

public class MetadataDetailsAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<TtMetadata> metadata;
    private int color, ds = -1;
    boolean autoHighlight;

    public MetadataDetailsAdapter(Context context, List<TtMetadata> metadata, int ds) {
        this(context, metadata, ds, true);
    }

    public MetadataDetailsAdapter(Context context, List<TtMetadata> metadata, int ds, boolean autoHighlight) {
        this.metadata = metadata;
        inflater = LayoutInflater.from(context);
        color = AndroidUtils.UI.getColor(context, R.color.primaryLightest);
        this.ds = ds;
        this.autoHighlight = autoHighlight;
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
            convertView = inflater.inflate(R.layout.content_details_meta, parent, false);
            mViewHolder = new ViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.name = (TextView)convertView.findViewById(R.id.listRowDiagPointEditorTvName);
            mViewHolder.dist = (TextView)convertView.findViewById(R.id.listRowDiagPointEditorTvDist);
            mViewHolder.elev = (TextView)convertView.findViewById(R.id.listRowDiagPointEditorTvElev);
            mViewHolder.parent = convertView.findViewById(R.id.parent);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        TtMetadata meta = getItem(position);

        mViewHolder.name.setText(meta.getName());
        mViewHolder.dist.setText(meta.getDistance().toString());
        mViewHolder.elev.setText(meta.getElevation().toString());

        if (autoHighlight) {
            mViewHolder.parent.setBackgroundResource(R.drawable.list_item_selector);
        }

        if (position == ds) {
            convertView.setBackgroundColor(color);
            ds = -1;
        }

        return convertView;
    }

    private class ViewHolder {
        public TextView name, dist, elev;
        public View parent;
    }
}
