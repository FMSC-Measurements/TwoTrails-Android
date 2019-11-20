package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.widget.MultiSelectRecyclerView;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.utilities.StringEx;

import java.util.List;

public class TtxPolygonsAdapter extends MultiSelectRecyclerView.MSAdapter<MultiSelectRecyclerView.MSViewHolder> {
    private static final int POLYGON = 1;

    private List<TtPolygon> polygons;
    private DataAccessLayer dal;

    public TtxPolygonsAdapter(Context context, List<TtPolygon> polygons, DataAccessLayer dal, MultiSelector multiSelector) {
        super(context, multiSelector);
        this.polygons = polygons;
        this.dal = dal;
    }


    @Override
    public int getItemViewTypeEx(int position) {
        return POLYGON;
    }

    @Override
    public MultiSelectRecyclerView.MSViewHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        return new TtPolygonHolder(inflater.inflate(R.layout.content_import_item, parent, false), getMultiSelector());
    }

    @Override
    public void onBindViewHolderEx(MultiSelectRecyclerView.MSViewHolder holder, int position) {
        TtPolygon poly = polygons.get(position);
        ((TtPolygonHolder)holder).bindPolygon(poly, dal);
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

    public class TtPolygonHolder extends MultiSelectRecyclerView.MSViewHolder implements MultiStateTouchCheckBox.OnCheckedStateChangeListener {
        private MultiStateTouchCheckBox mcb;
        private TextView tvName, tvPointCount, tvDesv;

        protected boolean selected;


        public TtPolygonHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);

            mcb = itemView.findViewById(R.id.importContMcbImport);
            tvName = itemView.findViewById(R.id.importContTvName);
            tvPointCount = itemView.findViewById(R.id.importContTvPointCount);
            tvDesv = itemView.findViewById((R.id.importDesc));

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


        public void bindPolygon(TtPolygon poly, DataAccessLayer dal) {
            tvName.setText(poly.getName());
            tvPointCount.setText(StringEx.toString(dal.getItemsCount(TwoTrailsSchema.PolygonSchema.TableName, TwoTrailsSchema.SharedSchema.CN, poly.getCN())));
            if (!StringEx.isEmpty(poly.getDescription())) {
                tvDesv.setText(poly.getDescription());
                tvDesv.setVisibility(View.VISIBLE);
            }
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
