package com.usda.fmsc.twotrails.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.adapters.SelectableArrayAdapter;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.List;

import com.usda.fmsc.utilities.StringEx;

public class PointDetailsAdapter extends SelectableArrayAdapter<TtPoint> {
    LayoutInflater inflater;
    AppUnits.IconColor iconColor = AppUnits.IconColor.Light;
    boolean showPolygon = false;
    boolean showQuondamLinks = false;

    public PointDetailsAdapter(Activity activity, ArrayList<TtPoint> points) {
        super(activity, 0, points);
        inflater = LayoutInflater.from(getContext());
    }

    public PointDetailsAdapter(Activity activity, ArrayList<TtPoint> points, AppUnits.IconColor iconColor) {
        super(activity, 0, points);
        inflater = LayoutInflater.from(getContext());
        this.iconColor = iconColor;
    }

    @Override
    public View getViewEx(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.content_details_points_ops, parent, false);
            mViewHolder = new ViewHolder();
            convertView.setTag(mViewHolder);

            mViewHolder.text = (TextView)convertView.findViewById(R.id.text1);
            mViewHolder.image = (ImageView)convertView.findViewById(R.id.image);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        TtPoint point = getItem(position);

        mViewHolder.image.setImageDrawable(TtUtils.UI.getTtOpDrawable(point.getOp(), iconColor, getContext()));

        String text;

        if (showQuondamLinks && point.getOp() == OpType.Quondam) {
            QuondamPoint qp = (QuondamPoint)point;

            text = String.format("%d%s (%d%s)",
                    point.getPID(), showPolygon ? " - " + point.getPolyName() : StringEx.Empty,
                    qp.getParentPID(), showPolygon ? " - " + qp.getParentPolyName() : StringEx.Empty);
        } else {
            text = String.format("%d%s", point.getPID(), showPolygon ? " - " + point.getPolyName() : StringEx.Empty);
        }

        mViewHolder.text.setText(text);

        return convertView;
    }


    public void setIconColor(AppUnits.IconColor iconColor) {
        this.iconColor = iconColor;
    }

    public void setShowPolygonName(boolean showPolygon) {
        this.showPolygon = showPolygon;
    }

    public void setShowQuondamLinks(boolean showQuondamLinks) {
        this.showQuondamLinks = showQuondamLinks;
    }

    private class ViewHolder {
        ImageView image;
        TextView text;
    }
}
