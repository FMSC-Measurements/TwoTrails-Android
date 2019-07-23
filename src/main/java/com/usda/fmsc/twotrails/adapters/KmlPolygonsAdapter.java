package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.widget.MultiSelectRecyclerView;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.kml.Polygon;

import java.util.List;

public class KmlPolygonsAdapter extends MultiSelectRecyclerView.MSAdapter<MultiSelectRecyclerView.MSViewHolder> {
    private static final int POLYGON = 1;

    private List<Polygon> polygons;


    public KmlPolygonsAdapter(Context context, List<Polygon> polygons, MultiSelector selector) {
        super(context, selector);
        this.polygons = polygons;
    }


    @Override
    public int getItemViewTypeEx(int position) {
        return POLYGON;
    }

    @Override
    public MultiSelectRecyclerView.MSViewHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        return new KmlPolygonHolder(inflater.inflate(R.layout.content_import_item, parent, false), getMultiSelector());
    }

    @Override
    public void onBindViewHolderEx(MultiSelectRecyclerView.MSViewHolder holder, int position) {
        Polygon poly = polygons.get(position);
        ((KmlPolygonHolder)holder).bindPolygon(poly);
    }

    @Override
    public MultiSelectRecyclerView.MSViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return new MultiSelectRecyclerView.MSViewHolder(inflater.inflate(R.layout.rv_header, parent, false), getMultiSelector());
    }

    @Override
    public MultiSelectRecyclerView.MSViewHolder onCreateFooterViewHolder(ViewGroup parent) {
        return new MultiSelectRecyclerView.MSViewHolder(inflater.inflate(R.layout.content_empty_100dp, parent, false), getMultiSelector());
    }

    @Override
    public int getItemCountEx() {
        return polygons.size();
    }


    public class KmlPolygonHolder extends MultiSelectRecyclerView.MSViewHolder implements MultiStateTouchCheckBox.OnCheckedStateChangeListener {
        private MultiStateTouchCheckBox mcb;
        private TextView tvName, tvPointCount;

        protected Polygon polygon;

        protected boolean selected;


        public KmlPolygonHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);

            mcb = itemView.findViewById(R.id.importContMcbImport);
            tvName = itemView.findViewById(R.id.importContTvName);
            tvPointCount = itemView.findViewById(R.id.importContTvPointCount);

            mcb.setOnCheckedStateChangeListener(this);
        }

        @Override
        public void setSelectable(boolean selectable) {
            //
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public void setActivated(boolean activated) {
            this.selected = activated;
            mcb.setCheckedStateNoEvent(activated ? MultiStateTouchCheckBox.CheckedState.Checked : MultiStateTouchCheckBox.CheckedState.NotChecked);
        }

        @Override
        public boolean isActivated() {
            return selected;
        }


        public void bindPolygon(Polygon poly) {
            this.polygon = poly;

            tvName.setText(polygon.getName());

            tvPointCount.setText(StringEx.toString(
                    poly.getInnerBoundary() != null ?
                            poly.getInnerBoundary().size() : poly.getOuterBoundary().size()
            ));
        }

        @Override
        public void onCheckedStateChanged(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
            setSelected(isChecked);
        }

        public String getName() {
            return tvName.getText().toString();
        }

    }
}
