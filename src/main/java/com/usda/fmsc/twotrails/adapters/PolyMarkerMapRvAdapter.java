package com.usda.fmsc.twotrails.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.widget.FlipCheckBoxEx;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox.CheckedState;
import com.usda.fmsc.android.widget.PopupMenuButton;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.dialogs.PointColorPickerDialog;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions.DrawCode;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicOptions;

import java.util.List;

public class PolyMarkerMapRvAdapter extends RecyclerView.Adapter<PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder> {
    private final AppCompatActivity activity;
    private final List<PolygonGraphicManager> markerMaps;
    private Listener listener;


    public PolyMarkerMapRvAdapter(AppCompatActivity activity, List<PolygonGraphicManager> markerMaps) {
        this.activity = activity;
        this.markerMaps = markerMaps;
    }

    public PolyMarkerMapRvAdapter(AppCompatActivity activity, List<PolygonGraphicManager> markerMaps, Listener listener) {
        this.activity = activity;
        this.markerMaps = markerMaps;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return markerMaps.size();
    }

    @Override
    public PolyMarkerMapViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_map_opts, parent, false);
        return new PolyMarkerMapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final PolyMarkerMapViewHolder holder, int position) {
        final PolygonGraphicManager graphicManager = markerMaps.get(position);
        final PolygonDrawOptions opt = graphicManager.getDrawOptions();


        holder.tvPolyName.setText(graphicManager.getPolyName());

        holder.tcbPoly.setCheckedState(opt.isVisible() ? CheckedState.Checked : CheckedState.NotChecked);
        holder.fcbAdjBnd.setChecked(opt.isAdjBnd());
        holder.fcbUnAdjBnd.setChecked(opt.isUnadjBnd());
        holder.fcbAdjBndPts.setChecked(opt.isAdjBndPts());
        holder.fcbUnAdjBndPts.setChecked(opt.isUnadjBndPts());
        holder.fcbAdjNav.setChecked(opt.isAdjNav());
        holder.fcbUnAdjNav.setChecked(opt.isUnadjNav());
        holder.fcbAdjNavPts.setChecked(opt.isAdjNavPts());
        holder.fcbUnAdjNavPts.setChecked(opt.isUnadjBndPts());
        holder.fcbAdjMiscPts.setChecked(opt.isAdjMiscPts());
        holder.fcbUnAdjMiscPts.setChecked(opt.isUnadjMiscPts());
        holder.fcbWayPts.setChecked(opt.isWayPts());


        holder.tcbPoly.setOnCheckedStateChangeListener((buttonView, isChecked, state) -> {
            graphicManager.setVisible(isChecked);
            onOptionChanged(holder, DrawCode.VISIBLE);
        });

        holder.fcbAdjBnd.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setAdjBndVisible(isChecked);
            onOptionChanged(holder, DrawCode.ADJBND);
        });

        holder.fcbUnAdjBnd.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setUnadjBndVisible(isChecked);
            onOptionChanged(holder, DrawCode.UNADJBND);
        });

        holder.fcbAdjBndPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setAdjBndPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.ADJBNDPTS);
        });

        holder.fcbUnAdjBndPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setUnadjBndPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.UNADJBNDPTS);
        });

        holder.fcbAdjNav.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setAdjNavVisible(isChecked);
            onOptionChanged(holder, DrawCode.ADJNAV);
        });

        holder.fcbUnAdjNav.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setUnadjNavVisible(isChecked);
            onOptionChanged(holder, DrawCode.UNADJNAV);
        });

        holder.fcbAdjNavPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setAdjNavPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.ADJNAVPTS);
        });

        holder.fcbUnAdjNavPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setUnadjNavPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.UNADJNAVPTS);
        });

        holder.fcbAdjMiscPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setAdjMiscPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.ADJMISCPTS);
        });

        holder.fcbUnAdjMiscPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setUnadjMiscPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.UNADJMISCPTS);
        });

        holder.fcbWayPts.setOnFlipCheckedChangeListener((flipCardView, isChecked) -> {
            graphicManager.setWayPtsVisible(isChecked);
            onOptionChanged(holder, DrawCode.WAYPTS);
        });

        setButtonColors(holder, graphicManager);

        holder.pmbOptions.setItemChecked(R.id.mapOptMenuCloseAdjBnd, graphicManager.isAdjBndClose());
        holder.pmbOptions.setItemChecked(R.id.mapOptMenuCloseUnAdjBnd, graphicManager.isUnadjBndClose());

        holder.pmbOptions.setListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.mapOptMenuCloseAdjBnd) {
                boolean checked = !item.isChecked();
                item.setChecked(checked);

                graphicManager.setAdjBndClose(checked);
                onOptionChanged(holder, DrawCode.ADJBNDCLOSE);
            } else if (itemId == R.id.mapOptMenuCloseUnAdjBnd) {
                boolean checked = !item.isChecked();
                item.setChecked(checked);

                graphicManager.setUnadjBndClose(checked);
                onOptionChanged(holder, DrawCode.UNADJBNDCLOSE);
            } else if (itemId == R.id.mapOptMenuColors) {
                PointColorPickerDialog dialog = PointColorPickerDialog.newInstance(
                        graphicManager.getGraphicOptions().getColors(),
                        graphicManager.getPolyName()
                );

                dialog.setListener(colorOptions -> {
                    graphicManager.setAdjBndColor(colorOptions[0]);
                    graphicManager.setAdjNavColor(colorOptions[1]);
                    graphicManager.setUnAdjBndColor(colorOptions[2]);
                    graphicManager.setUnAdjNavColor(colorOptions[3]);
                    graphicManager.setAdjPtsColor(colorOptions[4]);
                    graphicManager.setUnAdjPtsColor(colorOptions[5]);
                    graphicManager.setWayPtsColor(colorOptions[6]);

                    graphicManager.update(PolygonGraphicOptions.GraphicCode.ADJBND_COLOR, colorOptions[0]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.ADJNAV_COLOR, colorOptions[1]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.UNADJBND_COLOR, colorOptions[2]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.UNADJNAV_COLOR, colorOptions[3]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.ADJPTS_COLOR, colorOptions[4]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.UNADJPTS_COLOR, colorOptions[5]);
                    graphicManager.update(PolygonGraphicOptions.GraphicCode.WAYPTS_COLOR, colorOptions[6]);

                    setButtonColors(holder, graphicManager);
                });

                dialog.show(activity.getSupportFragmentManager(), "COLOR_PICKER");
            }

            return false;
        });

        graphicManager.addPolygonDrawListener(holder);
    }


    private void setButtonColors(PolyMarkerMapViewHolder holder, PolygonGraphicManager graphicManager) {
        holder.fcbAdjBnd.setAcceptColor(graphicManager.getAdjBndColor());
        holder.fcbUnAdjBnd.setAcceptColor(graphicManager.getUnAdjBndColor());
        holder.fcbAdjBndPts.setAcceptColor(graphicManager.getAdjPtsColor());
        holder.fcbUnAdjBndPts.setAcceptColor(graphicManager.getUnAdjPtsColor());
        holder.fcbAdjNav.setAcceptColor(graphicManager.getAdjNavColor());
        holder.fcbUnAdjNav.setAcceptColor(graphicManager.getUnAdjNavColor());
        holder.fcbAdjNavPts.setAcceptColor(graphicManager.getAdjPtsColor());
        holder.fcbUnAdjNavPts.setAcceptColor(graphicManager.getUnAdjPtsColor());
        holder.fcbAdjMiscPts.setAcceptColor(graphicManager.getAdjPtsColor());
        holder.fcbUnAdjMiscPts.setAcceptColor(graphicManager.getUnAdjPtsColor());
        holder.fcbWayPts.setAcceptColor(graphicManager.getWayPtsColor());
    }


    public void onOptionChanged(PolyMarkerMapViewHolder holder, DrawCode code) {
        if (listener != null) {
            listener.onHolderOptionChanged(holder, code);
        }
    }


    public class PolyMarkerMapViewHolder extends RecyclerView.ViewHolder implements PolygonDrawOptions.Listener {
        TextView tvPolyName;
        View layHeader, layContent;
        MultiStateTouchCheckBox tcbPoly;
        PopupMenuButton pmbOptions;
        FlipCheckBoxEx fcbAdjBnd, fcbAdjNav, fcbUnAdjBnd, fcbUnAdjNav,
                fcbAdjBndPts, fcbAdjNavPts, fcbUnAdjBndPts, fcbUnAdjNavPts,
                fcbAdjMiscPts, fcbUnAdjMiscPts, fcbWayPts;

        boolean cardExpanded = false;


        PolyMarkerMapViewHolder(View itemView) {
            super(itemView);

            tvPolyName = itemView.findViewById(R.id.pmcTvPolyName);

            layHeader = itemView.findViewById(R.id.pmcLayHeader);
            layContent = itemView.findViewById(R.id.pmcLayPolyContent);
            tcbPoly = itemView.findViewById(R.id.pmcTcbPoly);

            fcbAdjBnd = itemView.findViewById(R.id.pmcFcbAdjBnd);
            fcbAdjNav = itemView.findViewById(R.id.pmcFcbAdjNav);
            fcbUnAdjBnd = itemView.findViewById(R.id.pmcFcbUnadjBnd);
            fcbUnAdjNav = itemView.findViewById(R.id.pmcFcbUnadjNav);
            fcbAdjBndPts = itemView.findViewById(R.id.pmcFcbAdjBndPts);
            fcbAdjNavPts = itemView.findViewById(R.id.pmcFcbAdjNavPts);
            fcbUnAdjBndPts = itemView.findViewById(R.id.pmcFcbUnadjBndPts);
            fcbUnAdjNavPts = itemView.findViewById(R.id.pmcFcbUnadjNavPts);
            fcbAdjMiscPts = itemView.findViewById(R.id.pmcFcbAdjMiscPts);
            fcbUnAdjMiscPts = itemView.findViewById(R.id.pmcFcbUnadjMiscPts);
            fcbWayPts = itemView.findViewById(R.id.pmcFcbWayPts);

            pmbOptions = itemView.findViewById(R.id.pmcPmbMenu);

            layHeader.setOnClickListener(view -> toggleContent());
        }

        private void toggleContent() {
            if (cardExpanded) {
                ViewAnimator.collapseView(layContent);
                cardExpanded = false;
            } else {
                ViewAnimator.expandView(layContent);
                cardExpanded = true;
            }
        }

        @Override
        public void onOptionChanged(DrawCode code, boolean value) {
            switch (code) {
                case VISIBLE:
                    tcbPoly.setCheckedStateNoEvent(value ? CheckedState.Checked : CheckedState.NotChecked);
                    break;
                case ADJBND:
                    fcbAdjBnd.setCheckedNoEvent(value);
                    break;
                case UNADJBND:
                    fcbUnAdjBnd.setCheckedNoEvent(value);
                    break;
                case ADJBNDPTS:
                    fcbAdjBndPts.setCheckedNoEvent(value);
                    break;
                case UNADJBNDPTS:
                    fcbUnAdjBndPts.setCheckedNoEvent(value);
                    break;
                case ADJBNDCLOSE:
                    break;
                case UNADJBNDCLOSE:
                    break;
                case ADJNAV:
                    fcbAdjNav.setCheckedNoEvent(value);
                    break;
                case UNADJNAV:
                    fcbUnAdjNav.setCheckedNoEvent(value);
                    break;
                case ADJNAVPTS:
                    fcbAdjNavPts.setCheckedNoEvent(value);
                    break;
                case UNADJNAVPTS:
                    fcbUnAdjNavPts.setCheckedNoEvent(value);
                    break;
                case ADJMISCPTS:
                    fcbAdjMiscPts.setCheckedNoEvent(value);
                    break;
                case UNADJMISCPTS:
                    fcbUnAdjMiscPts.setCheckedNoEvent(value);
                    break;
                case WAYPTS:
                    fcbWayPts.setCheckedNoEvent(value);
                    break;
            }
        }
    }


    public interface Listener {
        void onHolderOptionChanged(PolyMarkerMapViewHolder holder, DrawCode code);
    }
}