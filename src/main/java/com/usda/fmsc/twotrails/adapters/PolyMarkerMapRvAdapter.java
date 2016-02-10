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
import com.usda.fmsc.twotrails.objects.PolyDrawOptions;
import com.usda.fmsc.twotrails.objects.PolyMarkerMap;

import java.util.List;

public class PolyMarkerMapRvAdapter extends RecyclerView.Adapter<PolyMarkerMapRvAdapter.PolyMarkerMapViewHolder> {
    private Context context;
    private List<PolyMarkerMap> markerMaps;
    private Listener listener;

    public PolyMarkerMapRvAdapter(Context context, List<PolyMarkerMap> markerMaps) {
        this.context = context;
        this.markerMaps = markerMaps;
    }

    public PolyMarkerMapRvAdapter(Context context, List<PolyMarkerMap> markerMaps, Listener listener) {
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
        final PolyMarkerMap markerMap = markerMaps.get(position);
        final PolyDrawOptions opt = markerMap.getOptions();


        holder.tvPolyName.setText(markerMap.getPolyName());

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
                markerMap.setVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.VISIBLE);
            }
        });

        holder.fcbAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setAdjBndVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.ADJBND);
            }
        });

        holder.fcbUnAdjBnd.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setUnadjBndVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.UNADJBND);
            }
        });

        holder.fcbAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setAdjBndPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.ADJBNDPTS);
            }
        });

        holder.fcbUnAdjBndPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setUnadjBndPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.UNADJBNDPTS);
            }
        });

        holder.fcbAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setAdjNavVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.ADJNAV);
            }
        });

        holder.fcbUnAdjNav.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setUnadjNavVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.UNADJNAV);
            }
        });

        holder.fcbAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setAdjNavPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.ADJNAVPTS);
            }
        });

        holder.fcbUnAdjNavPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setUnadjNavPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.UNADJNAVPTS);
            }
        });

        holder.fcbAdjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setAdjMiscPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.ADJMISCPTS);
            }
        });

        holder.fcbUnadjMiscPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setUnadjMiscPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.UNADJMISCPTS);
            }
        });

        holder.fcbWayPts.setOnFlipCheckedChangeListener(new FlipCheckBoxEx.OnFlipCheckedChangeListener() {
            @Override
            public void onCheckedChanged(FlipCheckBoxEx flipCardView, boolean isChecked) {
                markerMap.setWayPtsVisible(isChecked);
                onOptionChanged(holder, PolyDrawOptions.WAYPTS);
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
                                    markerMap.setAdjBndClose(isChecked);
                                    onOptionChanged(holder, PolyDrawOptions.ADJBNDCLOSE);
                                } else if (which == 1) {
                                    markerMap.setUnadjBndClose(isChecked);
                                    onOptionChanged(holder, PolyDrawOptions.UNADJBNDCLOSE);
                                }
                            }
                        });

                dialog.setTitle(R.string.map_more_opts);
                dialog.setPositiveButton(R.string.str_ok, null);

                dialog.show();
            }
        });

        markerMap.setListener(holder);
    }


    public void onOptionChanged(PolyMarkerMapViewHolder holder, String option) {
        if (listener != null) {
            listener.onHolderOptionChanged(holder, option);
        }
    }


    public class PolyMarkerMapViewHolder extends RecyclerView.ViewHolder implements PolyMarkerMap.Listener {
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
        public void onOptionChanged(String option, boolean value) {
            switch (option) {
                case PolyDrawOptions.VISIBLE:
                    tcbPoly.setCheckedStateNoEvent(value ? CheckedState.Checked : CheckedState.NotChecked);
                    break;
                case PolyDrawOptions.ADJBND:
                    fcbAdjBnd.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.UNADJBND:
                    fcbUnAdjBnd.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.ADJBNDPTS:
                    fcbAdjBndPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.UNADJBNDPTS:
                    fcbUnAdjBndPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.ADJBNDCLOSE:
                    break;
                case PolyDrawOptions.UNADJBNDCLOSE:
                    break;
                case PolyDrawOptions.ADJNAV:
                    fcbAdjNav.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.UNADJNAV:
                    fcbUnAdjNav.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.ADJNAVPTS:
                    fcbAdjNavPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.UNADJNAVPTS:
                    fcbUnAdjNavPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.ADJMISCPTS:
                    fcbAdjMiscPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.UNADJMISCPTS:
                    fcbUnadjMiscPts.setCheckedNoEvent(value);
                    break;
                case PolyDrawOptions.WAYPTS:
                    fcbWayPts.setCheckedNoEvent(value);
                    break;
            }
        }
    }

    public interface Listener {
        void onHolderOptionChanged(PolyMarkerMapViewHolder holder, String option);
    }
}