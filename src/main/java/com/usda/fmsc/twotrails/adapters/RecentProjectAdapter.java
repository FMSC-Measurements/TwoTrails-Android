package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.R;

import java.io.File;
import java.util.ArrayList;

public class RecentProjectAdapter extends BaseAdapter {
    private ArrayList<RecentProject> projList = new ArrayList<>();
    private LayoutInflater inflater;


    public RecentProjectAdapter(Context context, ArrayList<RecentProject> values) {
        projList = values;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return projList.size();
    }

    @Override
    public RecentProject getItem(int position) {
        return projList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_main_recent_projs, parent, false);
            mViewHolder = new ViewHolder();
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        RecentProject proj = projList.get(position);

        mViewHolder.Name = detail(convertView, R.id.recProjTvName, proj.Name);
        mViewHolder.File  = detail(convertView, R.id.recFileTvName, new File(proj.File).getName());

        return convertView;
    }

    // or you can try better way
    private TextView detail(View v, int resId, String text) {
        TextView tv = (TextView) v.findViewById(resId);
        tv.setText(text);
        return tv;
    }

    private class ViewHolder {
        TextView Name, File;
    }
}
