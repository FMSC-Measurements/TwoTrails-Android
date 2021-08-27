package com.usda.fmsc.twotrails.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.usda.fmsc.utilities.StringEx;

public class PointDetailsSpinnerAdapter extends BaseAdapter {
    private final List<TtPoint> points;
    private final LayoutInflater inflater;
    private final Context context;
    private AppUnits.IconColor iconColor;
    private boolean showPolygon = false;
    private final int itemView;

    public PointDetailsSpinnerAdapter(ArrayList<TtPoint> points, Context context) {
        this(points, context, AppUnits.IconColor.Light, android.R.layout.simple_spinner_item);
    }

    public PointDetailsSpinnerAdapter(List<TtPoint> points, Context context, AppUnits.IconColor iconColor) {
        this(points, context, iconColor, android.R.layout.simple_spinner_item);
    }

    public PointDetailsSpinnerAdapter(List<TtPoint> points, Context context, AppUnits.IconColor iconColor, int itemView) {
        this.points = points;
        this.context = context;
        this.itemView = itemView;
        inflater = LayoutInflater.from(this.context);
        this.iconColor = iconColor;
    }

    @Override
    public int getCount() {
        return points.size();
    }

    @Override
    public TtPoint getItem(int i) {
        return points.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
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

        TtPoint point = getItem(position);

        mViewHolder.text.setText(String.format(Locale.getDefault(), "%d", point.getPID()));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        DropDownViewHolder mViewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.content_details_points_ops, parent, false);
            mViewHolder = new DropDownViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.text = convertView.findViewById(R.id.text1);
            mViewHolder.image = convertView.findViewById(R.id.image);
        } else {
            mViewHolder = (DropDownViewHolder) convertView.getTag();
        }

        TtPoint point = getItem(position);

        mViewHolder.image.setImageDrawable(TtUtils.UI.getTtOpDrawable(point.getOp(), iconColor, context));
        mViewHolder.text.setText(String.format(Locale.getDefault(), "%d%s", point.getPID(),
                showPolygon ? " - " + point.getPolyName() : StringEx.Empty));

        return convertView;
    }

    public void setIconColor(AppUnits.IconColor iconColor) {
        this.iconColor = iconColor;
    }

    public void setShowPolygonName(boolean showPolygon) {
        this.showPolygon = showPolygon;
    }

    private class DropDownViewHolder {
        ImageView image;
        TextView text;
    }

    private class ViewHolder {
        TextView text;
    }
    public TtPoint getPoint(int index) {
        return points.get(index);
    }
}
