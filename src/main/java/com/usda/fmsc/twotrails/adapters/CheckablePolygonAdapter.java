package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.usda.fmsc.utilities.Tuple;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;
import java.util.Locale;

public class CheckablePolygonAdapter extends ArrayAdapter<TtPolygon> {
    private final ArrayList<Tuple<TtPolygon, Boolean>> _Polygons;
    private final ArrayList<Integer> _PointInPolygonsCount;
    private final LayoutInflater inflater;

    public CheckablePolygonAdapter(Context context, int resource, ArrayList<TtPolygon> polygons, ArrayList<Integer> pointInPolysCounts) {
        super(context, resource);

        _PointInPolygonsCount = pointInPolysCounts;

        _Polygons = new ArrayList<>();
        for (TtPolygon poly : polygons) {
            _Polygons.add(new Tuple<>(poly, true));
        }


        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        final Tuple<TtPolygon, Boolean> polygon = _Polygons.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.content_checkable_polygon, parent, false);
            holder = new ViewHolder(convertView);
            holder.checkBox.setChecked(polygon.Item2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.checkBox.setText(polygon.Item1.getName());
        holder.textView.setText(String.format(Locale.getDefault(), "(%d)", _PointInPolygonsCount.get(position)));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> polygon.Item2 = isChecked);

        return convertView;
    }


    public ArrayList<TtPolygon> getCheckedPolygons() {
        ArrayList<TtPolygon> polys = new ArrayList<>();

        for (Tuple<TtPolygon, Boolean> poly : _Polygons) {
            polys.add(poly.Item1);
        }

        return polys;
    }


    private static class ViewHolder {
        private final CheckBox checkBox;
        private final TextView textView;

        private ViewHolder(View view) {
            checkBox = view.findViewById(R.id.checkBox1);
            textView = view.findViewById(R.id.text1);
        }
    }
}
