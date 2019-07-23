package com.usda.fmsc.twotrails.adapters;

import androidx.annotation.ColorInt;
import androidx.annotation.Size;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    private AppCompatActivity activity;
    private List<PolygonGraphicManager> markerMaps;
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


        holder.tcbPoly.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, CheckedState state) {
                graphicManager.setVisible(isChecked);
                onOptionChanged(holder, DrawCode.VISIBLE);
            }
        });

        holder.fcbAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjBndVisible(isChecked);
                onOptionChanged(holder, PolygonDrawOptions.DrawCode.ADJBND);
            }
        });

        holder.fcbUnAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjBndVisible(isChecked);
                onOptionChanged(holder, DrawCode.UNADJBND);
            }
        });

        holder.fcbAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjBndPtsVisible(isChecked);
                onOptionChanged(holder, DrawCode.ADJBNDPTS);
            }
        });

        holder.fcbUnAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjBndPtsVisible(isChecked);
                onOptionChanged(holder, PolygonDrawOptions.DrawCode.UNADJBNDPTS);
            }
        });

        holder.fcbAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjNavVisible(isChecked);
                onOptionChanged(holder, DrawCode.ADJNAV);
            }
        });

        holder.fcbUnAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjNavVisible(isChecked);
                onOptionChanged(holder, DrawCode.UNADJNAV);
            }
        });

        holder.fcbAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjNavPtsVisible(isChecked);
                onOptionChanged(holder, PolygonDrawOptions.DrawCode.ADJNAVPTS);
            }
        });

        holder.fcbUnAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjNavPtsVisible(isChecked);
                onOptionChanged(holder, DrawCode.UNADJNAVPTS);
            }
        });

        holder.fcbAdjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjMiscPtsVisible(isChecked);
                onOptionChanged(holder, DrawCode.ADJMISCPTS);
            }
        });

        holder.fcbUnAdjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjMiscPtsVisible(isChecked);
                onOptionChanged(holder, DrawCode.UNADJMISCPTS);
            }
        });

        holder.fcbWayPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setWayPtsVisible(isChecked);
                onOptionChanged(holder, DrawCode.WAYPTS);
            }
        });

        setButtonColors(holder, graphicManager);

        holder.pmbOptions.setItemChecked(R.id.mapOptMenuCloseAdjBnd, graphicManager.isAdjBndClose());
        holder.pmbOptions.setItemChecked(R.id.mapOptMenuCloseUnAdjBnd, graphicManager.isUnadjBndClose());

        holder.pmbOptions.setListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mapOptMenuCloseAdjBnd: {
                        boolean checked = !item.isChecked();
                        item.setChecked(checked);

                        graphicManager.setAdjBndClose(checked);
                        onOptionChanged(holder, DrawCode.ADJBNDCLOSE);
                        break;
                    }
                    case R.id.mapOptMenuCloseUnAdjBnd: {
                        boolean checked = !item.isChecked();
                        item.setChecked(checked);

                        graphicManager.setUnadjBndClose(checked);
                        onOptionChanged(holder, DrawCode.UNADJBNDCLOSE);
                        break;
                    }
                    case R.id.mapOptMenuColors: {
                        PointColorPickerDialog dialog = PointColorPickerDialog.newInstance(
                                graphicManager.getGraphicOptions().getColors(),
                                graphicManager.getPolyName()
                        );

                        dialog.setListener(new PointColorPickerDialog.PointColorListener() {
                            @Override
                            public void onUpdated(@Size(7) @ColorInt int[] colorOptions) {
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
                            }
                        });

                        dialog.show(activity.getSupportFragmentManager(), "COLOR_PICKER");
                        break;
                    }
                }

                return false;
            }
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

            layHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleContent();
                }
            });
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