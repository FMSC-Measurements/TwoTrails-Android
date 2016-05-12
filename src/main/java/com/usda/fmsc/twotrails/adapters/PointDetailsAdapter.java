package com.usda.fmsc.twotrails.adapters;

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

import com.usda.fmsc.utilities.StringEx;

public class PointDetailsAdapter extends BaseAdapter {

    List<TtPoint> points;
    LayoutInflater inflater;
    Context context;
    AppUnits.IconColor iconColor = AppUnits.IconColor.Light;
    boolean showPolygon = false;

    public PointDetailsAdapter(ArrayList<TtPoint> points, Context context) {
        this.points = points;
        this.context = context;
        inflater = LayoutInflater.from(this.context);
    }

    public PointDetailsAdapter(List<TtPoint> points, Context context, AppUnits.IconColor iconColor) {
        this.points = points;
        this.context = context;
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
            convertView = inflater.inflate(R.layout.content_details_points_ops, null);
            mViewHolder = new ViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.text = (TextView)convertView.findViewById(R.id.text1);
            mViewHolder.image = (ImageView)convertView.findViewById(R.id.image);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        TtPoint point = getItem(position);

        mViewHolder.image.setImageDrawable(TtUtils.UI.getTtOpDrawable(point.getOp(), iconColor, context));
        mViewHolder.text.setText(String.format("%d%s", point.getPID(),
                showPolygon ? " - " + point.getPolyName() : StringEx.Empty));

        return convertView;
    }


    public void setIconColor(AppUnits.IconColor iconColor) {
        this.iconColor = iconColor;
    }

    public void setShowPolygonName(boolean showPolygon) {
        this.showPolygon = showPolygon;
    }

    private class ViewHolder {
        ImageView image;
        TextView text;
    }

    public TtPoint getPoint(int index) {
        return points.get(index);
    }
}
