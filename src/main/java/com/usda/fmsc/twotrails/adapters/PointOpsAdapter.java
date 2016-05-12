package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class PointOpsAdapter extends BaseAdapter {

    OpType[] opTypes;
    LayoutInflater inflater;
    Context context;
    AppUnits.IconColor iconColor = AppUnits.IconColor.Light;

    public PointOpsAdapter(Context context) {
        opTypes = OpType.values();
        this.context = context;
        inflater = LayoutInflater.from(this.context);
    }

    public PointOpsAdapter(Context context, AppUnits.IconColor iconColor) {
        opTypes = OpType.values();
        this.context = context;
        inflater = LayoutInflater.from(this.context);
        this.iconColor = iconColor;
    }

    @Override
    public int getCount() {
        return opTypes.length;
    }

    @Override
    public OpType getItem(int position) {
        return opTypes[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;

        if (view == null) {
            view = inflater.inflate(R.layout.content_details_points_ops, null);
            viewHolder = new ViewHolder();
            view.setTag(viewHolder);

            viewHolder.text = (TextView)view.findViewById(R.id.text1);
            viewHolder.image = (ImageView)view.findViewById(R.id.image);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        OpType op = getItem(position);

        viewHolder.image.setImageDrawable(TtUtils.UI.getTtOpDrawable(op, iconColor, context));
        viewHolder.text.setText(op.toString());

        return view;
    }


    public void setIconColor(AppUnits.IconColor iconColor) {
        this.iconColor = iconColor;
    }


    private class ViewHolder {
        ImageView image;
        TextView text;
    }
}
