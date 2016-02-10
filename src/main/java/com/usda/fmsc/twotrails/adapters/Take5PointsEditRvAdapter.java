package com.usda.fmsc.twotrails.adapters;

import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.widget.RecyclerViewEx;
import com.usda.fmsc.android.widget.RecyclerViewEx.ViewHolderEx;
import com.usda.fmsc.twotrails.activities.Take5Activity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.SideShotPoint;
import com.usda.fmsc.twotrails.objects.Take5Point;
import com.usda.fmsc.twotrails.objects.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.List;

import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class Take5PointsEditRvAdapter extends RecyclerViewEx.AdapterEx<ViewHolderEx> {
    Take5Activity activity;
    List<TtPoint> points;
    Drawable dOnBnd, dOffBnd;
    TtMetadata metadata;

    public Take5PointsEditRvAdapter(Take5Activity activity, List<TtPoint> points, TtMetadata metadata) {
        super(activity);

        this.points = points;
        this.activity = activity;
        this.metadata = metadata;

        dOnBnd = AndroidUtils.UI.getDrawable(this.activity, R.drawable.ic_onbnd_dark);
        dOffBnd = AndroidUtils.UI.getDrawable(this.activity, R.drawable.ic_offbnd_dark);

        setViewHasFooter(true);
    }


    @Override
    public int getItemViewTypeEx(int position) {
        return points.get(position).getOp().getValue();
    }

    @Override
    public ViewHolderEx onCreateViewHolderEx(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 4:
                return new SideShotViewHolderEx(inflater.inflate(R.layout.card_take5_sideshot, parent, false));
            case 6:
                return new Take5ViewHolderEx(inflater.inflate(R.layout.card_take5_take5, parent, false));
        }

        return null;
    }

    @Override
    public void onBindViewHolderEx(ViewHolderEx holder, int position) {
        final TtPoint point = points.get(position);

        final PointViewHolderEx pvh = (PointViewHolderEx)holder;

        pvh.tvPID.setText(StringEx.toString(point.getPID()));
        pvh.txtCmt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                point.setComment(s.toString());
                activity.updatePoint(point);
            }
        });

        pvh.ibBnd.setImageDrawable(point.isOnBnd() ? dOnBnd : dOffBnd);
        pvh.ibBnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean onBnd = !point.isOnBnd();

                pvh.ibBnd.setImageDrawable(onBnd ? dOnBnd : dOffBnd);
                point.setOnBnd(onBnd);
                activity.updatePoint(point);
            }
        });

        switch (point.getOp()) {
            case Take5: {
                Take5ViewHolderEx t5Holder = (Take5ViewHolderEx)holder;
                Take5Point t5 = (Take5Point)point;

                t5Holder.tvX.setText(StringEx.toString(t5.getUnAdjX(), 3));
                t5Holder.tvY.setText(StringEx.toString(t5.getUnAdjY(), 3));
                t5Holder.tvElev.setText(StringEx.toString(t5.getUnAdjZ(), 3));
                t5Holder.tvElevType.setText(metadata.getElevation().toString());
                break;
            }
            case SideShot: {
                final SideShotViewHolderEx ssHolder = (SideShotViewHolderEx)holder;
                final SideShotPoint ssp = (SideShotPoint)point;

                ssHolder.txtFwdAz.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() > 0) {
                            ssp.setFwdAz(ParseEx.parseDouble(s.toString()));
                        } else {
                            ssp.setFwdAz(null);
                        }

                        activity.updatePoint(ssp);
                        ssHolder.calcAzError(ssp);
                    }
                });

                ssHolder.txtBkAz.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() > 0) {
                            ssp.setBkAz(ParseEx.parseDouble(s.toString()));
                        } else {
                            ssp.setBkAz(null);
                        }

                        activity.updatePoint(ssp);
                        ssHolder.calcAzError(ssp);
                    }
                });

                ssHolder.txtSlpAng.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() > 0) {
                            ssp.setSlopeAngle(TtUtils.Convert.angle(
                                    ParseEx.parseDouble(s.toString()),
                                    Units.Slope.Percent,
                                    metadata.getSlope()
                            ));
                        } else {
                            ssp.setSlopeAngle(0);
                        }

                        activity.updatePoint(ssp);
                    }
                });

                ssHolder.txtSlpDist.addTextChangedListener(new SimpleTextWatcher() {

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() > 0) {
                            ssp.setSlopeDistance(ParseEx.parseDouble(s.toString()));
                        } else {
                            ssp.setSlopeDistance(0);
                        }

                        activity.updatePoint(ssp);
                    }
                });
                break;
            }
        }
    }

    @Override
    public ViewHolderEx onCreateHeaderViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    public ViewHolderEx onCreateFooterViewHolder(ViewGroup parent) {
        return new ViewHolderEx(LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_take5_footer, parent, false));
    }

    @Override
    public int getItemCountEx() {
        return points.size();
    }


    public class PointViewHolderEx extends ViewHolderEx {
        public AlphaAnimation anim;
        public boolean hidden;

        public android.support.v7.widget.CardView parent;
        public TextView tvPID;
        public EditText txtCmt;
        public ImageButton ibBnd;
        public ImageView ivOp;


        public PointViewHolderEx(View itemView) {
            super(itemView);

            tvPID = (TextView)itemView.findViewById(R.id.pointHeaderTvPid);
            ibBnd = (ImageButton)itemView.findViewById(R.id.pointHeaderIbBnd);
            txtCmt = (EditText)itemView.findViewById(R.id.pointTxtCmt);
            ivOp = (ImageView)itemView.findViewById(R.id.pointHeaderIvOp);
        }

        public void setLocked(boolean lock) {
            ibBnd.setEnabled(!lock);
            txtCmt.setEnabled(!lock);
        }

    }

    public class Take5ViewHolderEx extends PointViewHolderEx {
        TextView tvX, tvY, tvElev, tvElevType;

        public Take5ViewHolderEx(View itemView) {
            super(itemView);

            ivOp.setImageDrawable(TtUtils.UI.getTtOpDrawable(Units.OpType.Take5, AppUnits.IconColor.Dark, activity));

            tvX = (TextView)itemView.findViewById(R.id.pointCardTvX);
            tvY = (TextView)itemView.findViewById(R.id.pointCardTvY);
            tvElev = (TextView)itemView.findViewById(R.id.pointCardTvElev);
            tvElevType = (TextView)itemView.findViewById(R.id.pointCardTvElevType);
        }
    }

    public class SideShotViewHolderEx extends PointViewHolderEx {
        EditText txtFwdAz, txtBkAz, txtSlpDist, txtSlpAng;
        TextView tvDiff;

        public SideShotViewHolderEx(View itemView) {
            super(itemView);

            ivOp.setImageDrawable(TtUtils.UI.getTtOpDrawable(Units.OpType.SideShot, AppUnits.IconColor.Dark, activity));

            txtFwdAz = (EditText)itemView.findViewById(R.id.pointTravTxtAzFwd);
            txtBkAz = (EditText)itemView.findViewById(R.id.pointTravTxtAzBk);
            txtSlpDist = (EditText)itemView.findViewById(R.id.pointTravTxtSlopeDist);
            txtSlpAng = (EditText)itemView.findViewById(R.id.pointTravTxtSlopeAng);

            tvDiff = (TextView)itemView.findViewById(R.id.pointTravAzDiff);
        }

        @Override
        public void setLocked(boolean lock) {
            super.setLocked(lock);

            txtFwdAz.setEnabled(!lock);
            txtBkAz.setEnabled(!lock);
            txtSlpDist.setEnabled(!lock);
            txtSlpAng.setEnabled(!lock);
        }

        private void calcAzError(TravPoint point) {
            if (point.getFwdAz() != null && point.getBkAz() != null) {
                double diff = TtUtils.Math.azimuthDiff(point.getFwdAz(), point.getBkAz());

                if (diff >= 0.01) {
                    tvDiff.setText(StringEx.toString(TtUtils.Math.round(diff, 2)));
                    tvDiff.setVisibility(View.VISIBLE);
                    return;
                }
            }

            tvDiff.setVisibility(View.GONE);
        }
    }
}
