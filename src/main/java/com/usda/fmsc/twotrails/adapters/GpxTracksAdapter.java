package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.widget.multiselection.SimpleMultiSelectorBindingHolder;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.utilities.gpx.GpxBaseTrack;
import com.usda.fmsc.utilities.gpx.GpxRoute;
import com.usda.fmsc.utilities.gpx.GpxTrack;
import com.usda.fmsc.utilities.gpx.GpxTrackSeg;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.R;

import java.util.List;

public class GpxTracksAdapter extends RecyclerViewEx.AdapterEx<SimpleMultiSelectorBindingHolder> {
    private static final int TRACK = 1, ROUTE = 2;

    private List<GpxBaseTrack> tracks;

    private MultiSelector mSelector;


    public GpxTracksAdapter(Context context, List<GpxBaseTrack> tracks, MultiSelector selector) {
        super(context);
        this.tracks = tracks;

        mSelector = selector;
    }


    @Override
    public int getItemViewTypeEx(int position) {
        GpxBaseTrack track = tracks.get(position);
        if (track instanceof GpxTrack) {
            return TRACK;
        } else if (track instanceof GpxRoute) {
            return ROUTE;
        }

        return INVALID_TYPE;
    }

    @Override
    public SimpleMultiSelectorBindingHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        if (viewType == TRACK)
            return new GpxTrackHolder(inflater.inflate(R.layout.content_import_gpx_item, parent, false));
        else if (viewType == ROUTE)
            return new GpxRouteHolder(inflater.inflate(R.layout.content_import_gpx_item, parent, false));
        else
            return null;
    }

    @Override
    public void onBindViewHolderEx(SimpleMultiSelectorBindingHolder holder, int position) {
        GpxBaseTrack track = tracks.get(position);
        ((GpxBaseTrackHolder)holder).bindTrack(track);
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
        return tracks.size();
    }


    public abstract class GpxBaseTrackHolder extends SimpleMultiSelectorBindingHolder implements MultiStateTouchCheckBox.OnCheckedStateChangeListener {
        protected MultiStateTouchCheckBox mcb;
        protected TextView tvName, tvPointCount;

        protected GpxBaseTrack track;

        protected boolean selected;


        public GpxBaseTrackHolder(View itemView) {
            super(itemView, mSelector);

            mcb = (MultiStateTouchCheckBox)itemView.findViewById(R.id.importContMcbImport);
            tvName = (TextView)itemView.findViewById(R.id.importContTvName);
            tvPointCount = (TextView)itemView.findViewById(R.id.importContTvPointCount);

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


        public void bindTrack(GpxBaseTrack track) {
            this.track = track;

            tvName.setText(track.getName());
            tvPointCount.setText(StringEx.toString(getPointCount(track)));
        }

        @Override
        public void onCheckedStateChanged(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
            mSelector.setSelected(this, isChecked);
        }

        abstract int getPointCount(GpxBaseTrack track);

        public String getName() {
            return tvName.getText().toString();
        }

    }

    public class GpxTrackHolder extends GpxBaseTrackHolder {
        public GpxTrackHolder(View itemView) {
            super(itemView);
        }

        @Override
        int getPointCount(GpxBaseTrack track) {
            GpxTrack trk = (GpxTrack)track;

            int pc = 0;

            for (GpxTrackSeg seg : trk.getSegments()) {
                pc += seg.getPoints().size();
            }

            return pc;
        }
    }

    public class GpxRouteHolder extends GpxBaseTrackHolder {
        public GpxRouteHolder(View itemView) {
            super(itemView);
        }

        @Override
        int getPointCount(GpxBaseTrack track) {
            return ((GpxRoute)track).getPoints().size();
        }
    }
}
