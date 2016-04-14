package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.android.widget.FlipCheckBoxEx;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.MultiStateTouchCheckBox.CheckedState;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions.GraphicCode;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;

import java.util.List;

public class PolyMarkerMapRvAdapter extends RecyclerView.Adapter<PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder> {
    private Context context;
    private List<PolygonGraphicManager> markerMaps;
    private Listener listener;

    public PolyMarkerMapRvAdapter(Context context, List<PolygonGraphicManager> markerMaps) {
        this.context = context;
        this.markerMaps = markerMaps;
    }

    public PolyMarkerMapRvAdapter(Context context, List<PolygonGraphicManager> markerMaps, Listener listener) {
        this.context = context;
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

        holder.tcbPoly.setCheckedState(opt.Visible ? CheckedState.Checked : CheckedState.NotChecked);
        holder.fcbAdjBnd.setChecked(opt.AdjBnd);
        holder.fcbUnAdjBnd.setChecked(opt.UnadjBnd);
        holder.fcbAdjBndPts.setChecked(opt.AdjBndPts);
        holder.fcbUnAdjBndPts.setChecked(opt.UnadjBndPts);
        holder.fcbAdjNav.setChecked(opt.AdjNav);
        holder.fcbUnAdjNav.setChecked(opt.UnadjNav);
        holder.fcbAdjNavPts.setChecked(opt.AdjNavPts);
        holder.fcbUnAdjNavPts.setChecked(opt.UnadjBndPts);
        holder.fcbAdjMiscPts.setChecked(opt.AdjMiscPts);
        holder.fcbUnadjMiscPts.setChecked(opt.UnadjMiscPts);
        holder.fcbWayPts.setChecked(opt.WayPts);


        holder.tcbPoly.setOnCheckedStateChangeListener(new MultiStateTouchCheckBox.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedStateChanged(View buttonView, boolean isChecked, CheckedState state) {
                graphicManager.setVisible(isChecked);
                onOptionChanged(holder, GraphicCode.VISIBLE);
            }
        });

        holder.fcbAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjBndVisible(isChecked);
                onOptionChanged(holder, GraphicCode.ADJBND);
            }
        });

        holder.fcbUnAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjBndVisible(isChecked);
                onOptionChanged(holder, GraphicCode.UNADJBND);
            }
        });

        holder.fcbAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjBndPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.ADJBNDPTS);
            }
        });

        holder.fcbUnAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjBndPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.UNADJBNDPTS);
            }
        });

        holder.fcbAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjNavVisible(isChecked);
                onOptionChanged(holder, GraphicCode.ADJNAV);
            }
        });

        holder.fcbUnAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjNavVisible(isChecked);
                onOptionChanged(holder, GraphicCode.UNADJNAV);
            }
        });

        holder.fcbAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjNavPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.ADJNAVPTS);
            }
        });

        holder.fcbUnAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjNavPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.UNADJNAVPTS);
            }
        });

        holder.fcbAdjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setAdjMiscPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.ADJMISCPTS);
            }
        });

        holder.fcbUnadjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setUnadjMiscPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.UNADJMISCPTS);
            }
        });

        holder.fcbWayPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                graphicManager.setWayPtsVisible(isChecked);
                onOptionChanged(holder, GraphicCode.WAYPTS);
            }
        });

        holder.ibBndMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setMultiChoiceItems(
                        R.array.arr_extra_map_opts,
                        new boolean[]{opt.AdjBndClose, opt.UnadjBndClose},
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (which == 0) {
                                    graphicManager.setAdjBndClose(isChecked);
                                    onOptionChanged(holder, GraphicCode.ADJBNDCLOSE);
                                } else if (which == 1) {
                                    graphicManager.setUnadjBndClose(isChecked);
                                    onOptionChanged(holder, GraphicCode.UNADJBNDCLOSE);
                                }
                            }
                        });

                dialog.setTitle(R.string.map_more_opts);
                dialog.setPositiveButton(R.string.str_ok, null);

                dialog.show();
            }
        });

        graphicManager.setListener(holder);
    }


    public void onOptionChanged(PolyMarkerMapViewHolder holder, GraphicCode code) {
        if (listener != null) {
            listener.onHolderOptionChanged(holder, code);
        }
    }


    public class PolyMarkerMapViewHolder extends RecyclerView.ViewHolder implements PolygonGraphicManager.Listener {
        TextView tvPolyName;
        View layHeader, layContent;
        MultiStateTouchCheckBox tcbPoly;
        ImageButton ibBndMenu;
        FlipCheckBoxEx fcbAdjBnd, fcbAdjNav, fcbUnAdjBnd, fcbUnAdjNav,
                fcbAdjBndPts, fcbAdjNavPts, fcbUnAdjBndPts, fcbUnAdjNavPts,
                fcbAdjMiscPts, fcbUnadjMiscPts, fcbWayPts;

        boolean cardExpanded = false;


        PolyMarkerMapViewHolder(View itemView) {
            super(itemView);

            tvPolyName = (TextView)itemView.findViewById(R.id.pmcTvPolyName);

            layHeader = itemView.findViewById(R.id.pmcLayHeader);
            layContent = itemView.findViewById(R.id.pmcLayPolyContent);
            tcbPoly = (MultiStateTouchCheckBox)itemView.findViewById(R.id.pmcTcbPoly);

            fcbAdjBnd = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbAdjBnd);
            fcbAdjNav = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbAdjNav);
            fcbUnAdjBnd = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbUnadjBnd);
            fcbUnAdjNav = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbUnadjNav);
            fcbAdjBndPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbAdjBndPts);
            fcbAdjNavPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbAdjNavPts);
            fcbUnAdjBndPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbUnadjBndPts);
            fcbUnAdjNavPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbUnadjNavPts);
            fcbAdjMiscPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbAdjMiscPts);
            fcbUnadjMiscPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbUnadjMiscPts);
            fcbWayPts = (FlipCheckBoxEx)itemView.findViewById(R.id.pmcFcbWayPts);

            ibBndMenu = (ImageButton)itemView.findViewById(R.id.pmcIbBndMenu);

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
        public void onOptionChanged(PolygonDrawOptions.GraphicCode code, boolean value) {
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
                    fcbUnadjMiscPts.setCheckedNoEvent(value);
                    break;
                case WAYPTS:
                    fcbWayPts.setCheckedNoEvent(value);
                    break;
            }
        }
    }

    public interface Listener {
        void onHolderOptionChanged(PolyMarkerMapViewHolder holder, GraphicCode code);
    }
}