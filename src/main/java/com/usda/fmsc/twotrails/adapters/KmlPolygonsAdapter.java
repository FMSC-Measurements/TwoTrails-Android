package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.multiselection.SimpleMultiSelectorBindingHolder;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.utilities.kml.Polygon;

import java.util.List;

public class KmlPolygonsAdapter extends RecyclerViewEx.AdapterEx<SimpleMultiSelectorBindingHolder> {
    private static final int POLYGON = 1;

    private List<Polygon> polygons;

    private MultiSelector mSelector;


    public KmlPolygonsAdapter(Context context, List<Polygon> polygons, MultiSelector selector) {
        super(context);
        this.polygons = polygons;

        mSelector = selector;
    }


    @Override
    public int getItemViewTypeEx(int position) {
        return POLYGON;
    }

    @Override
    public SimpleMultiSelectorBindingHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        return new KmlPolygonHolder(inflater.inflate(R.layout.content_import_item, parent, false));
    }

    @Override
    public void onBindViewHolderEx(SimpleMultiSelectorBindingHolder holder, int position) {
        Polygon poly = polygons.get(position);
        ((KmlPolygonHolder)holder).bindPolygon(poly);
    }

    @Override
    public SimpleMultiSelectorBindingHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return new SimpleMultiSelectorBindingHolder(inflater.inflate(R.layout.rv_header, parent, false), mSelector);
    }

    @Override
    public SimpleMultiSelectorBindingHolder onCreateFooterViewHolder(ViewGroup parent) {
        return new SimpleMultiSelectorBindingHolder(inflater.inflate(R.layout.content_empty_100dp, parent, false), mSelector);
    }

    @Override
    public int getItemCountEx() {
        return polygons.size();
    }


    public class KmlPolygonHolder extends SimpleMultiSelectorBindingHolder implements MultiStateTouchCheckBox.OnCheckedStateChangeListener {
        protected MultiStateTouchCheckBox mcb;
        protected TextView tvName, tvPointCount;

        protected Polygon polygon;

        protected boolean selected;


        public KmlPolygonHolder(View itemView) {
            super(itemView, mSelector);

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
            mSelector.setSelected(this, isChecked);
        }

        public String getName() {
            return tvName.getText().toString();
        }

    }
}
