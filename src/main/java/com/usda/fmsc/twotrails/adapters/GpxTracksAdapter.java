package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usda.fmsc.android.widget.MultiSelectRecyclerView;
import com.usda.fmsc.android.widget.multiselection.MultiSelector;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.utilities.gpx.GpxBaseTrack;
import com.usda.fmsc.utilities.gpx.GpxRoute;
import com.usda.fmsc.utilities.gpx.GpxTrack;
import com.usda.fmsc.utilities.gpx.GpxTrackSeg;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.R;

import java.util.List;

public class GpxTracksAdapter extends MultiSelectRecyclerView.MSAdapter<MultiSelectRecyclerView.MSViewHolder> {
    private static final int TRACK = 1, ROUTE = 2;

    private final List<GpxBaseTrack> tracks;


    public GpxTracksAdapter(Context context, List<GpxBaseTrack> tracks, MultiSelector multiSelector) {
        super(context, multiSelector);
        this.tracks = tracks;
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
    public MultiSelectRecyclerView.MSViewHolder onCreateViewHolderEx(ViewGroup parent, int viewType) {
        if (viewType == TRACK)
            return new GpxTrackHolder(inflater.inflate(R.layout.content_import_item, parent, false), getMultiSelector());
        else if (viewType == ROUTE)
            return new GpxRouteHolder(inflater.inflate(R.layout.content_import_item, parent, false), getMultiSelector());
        else
            return null;
    }

    @Override
    public void onBindViewHolderEx(MultiSelectRecyclerView.MSViewHolder holder, int position) {
        GpxBaseTrack track = tracks.get(position);
        ((GpxBaseTrackHolder)holder).bindTrack(track);
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
        return tracks.size();
    }


    public abstract class GpxBaseTrackHolder extends MultiSelectRecyclerView.MSViewHolder implements MultiStateTouchCheckBox.OnCheckedStateChangeListener {
        private final MultiStateTouchCheckBox mcb;
        private final TextView tvName, tvPointCount;

        protected boolean selected;

        public GpxBaseTrackHolder(View itemView, MultiSelector multiSelector) {
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


        public void bindTrack(GpxBaseTrack track) {
            tvName.setText(track.getName());
            tvPointCount.setText(StringEx.toString(getPointCount(track)));
        }

        @Override
        public void onCheckedStateChanged(View view, boolean isChecked, MultiStateTouchCheckBox.CheckedState state) {
            setSelected(isChecked);
        }

        abstract int getPointCount(GpxBaseTrack track);

        public String getName() {
            return tvName.getText().toString();
        }

    }

    public class GpxTrackHolder extends GpxBaseTrackHolder {
        public GpxTrackHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);
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
        public GpxRouteHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);
        }

        @Override
        int getPointCount(GpxBaseTrack track) {
            return ((GpxRoute)track).getPoints().size();
        }
    }
}
