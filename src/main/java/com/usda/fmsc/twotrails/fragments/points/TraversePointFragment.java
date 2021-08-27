package com.usda.fmsc.twotrails.fragments.points;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class TraversePointFragment extends BasePointFragment {
    private EditText txtFwd, txtBk, txtSlopeDist, txtSlopeAng;
    private TextView tvMagDec, tvAzDiff;

    private boolean settingView = false;
    private TravPoint _TravPoint;


    public static TraversePointFragment newInstance(TravPoint point) {
        return newInstance(point, false);
    }

    public static TraversePointFragment newInstance(TravPoint point, boolean hidden) {
        TraversePointFragment fragment = new TraversePointFragment();
        Bundle args = new Bundle();
        args.putParcelable(POINT, point);
        args.putBoolean(HIDDEN, hidden);
        fragment.setArguments(args);
        return fragment;
    }


    public TraversePointFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getBasePoint() != null) {
            _TravPoint = (TravPoint)getBasePoint();
        }
    }


    @Override
    public View onCreateViewEx(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_points_travpoint_card, container, false);

        txtFwd = view.findViewById(R.id.pointTravTxtAzFwd);
        txtBk = view.findViewById(R.id.pointTravTxtAzBk);
        txtSlopeDist = view.findViewById(R.id.pointTravTxtSlopeDist);
        txtSlopeAng = view.findViewById(R.id.pointTravTxtSlopeAng);

        tvMagDec = view.findViewById(R.id.pointTravTvMagDec);
        tvAzDiff = view.findViewById(R.id.pointTravAzDiff);

        if (_TravPoint != null) {
            setViews();
        }

        //region TextChange
        txtFwd.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    _TravPoint.setFwdAz(value);
                    calcAzError();
                    getPointController().updatePoint(_TravPoint);
                }
            }
        });

        txtBk.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    _TravPoint.setBkAz(value);
                    calcAzError();
                    getPointController().updatePoint(_TravPoint);
                }
            }
        });

        txtSlopeDist.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = 0d;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    value = TtUtils.Convert.distance(value, Dist.Meters, getMetadata().getDistance());

                    _TravPoint.setSlopeDistance(value);
                    getPointController().updatePoint(_TravPoint);
                }
            }
        });

        txtSlopeAng.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString(), 0d);
                    }

                    value = TtUtils.Convert.angle(value, Slope.Percent, getMetadata().getSlope());

                    _TravPoint.setSlopeAngle(value);
                    getPointController().updatePoint(_TravPoint);
                }
            }
        });
        //endregion

        //region Focus
        AndroidUtils.UI.removeSelectionOnUnfocus(txtFwd);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtBk);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtSlopeAng);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtSlopeAng);
        //endregion


        final View parent = view.findViewById(R.id.parentLayout);

        EditText[] ets = new EditText[] {
                txtFwd,
                txtBk,
                txtSlopeAng,
                txtSlopeDist
        };

        AndroidUtils.UI.hideKeyboardOnSelect(parent, ets);

        return view;
    }

    @Override
    protected void onBasePointUpdated() {
        _TravPoint = (TravPoint)getBasePoint();
        setViews();
    }

    private void setViews() {
        settingView = true;

        txtFwd.setText(StringEx.toStringRound(_TravPoint.getFwdAz(), Consts.Minimum_Point_Display_Digits));
        txtBk.setText(StringEx.toStringRound(_TravPoint.getBkAz(), Consts.Minimum_Point_Display_Digits));

        txtSlopeDist.setText(StringEx.toStringRound(
                TtUtils.Convert.distance(_TravPoint.getSlopeDistance(), getMetadata().getDistance(), Dist.Meters),
                Consts.Minimum_Point_Display_Digits
        ));

        txtSlopeAng.setText(StringEx.toStringRound(
                TtUtils.Convert.angle(_TravPoint.getSlopeAngle(), getMetadata().getSlope(), Slope.Percent),
                Consts.Minimum_Point_Display_Digits
        ));

        tvMagDec.setText(StringEx.toString(getMetadata().getMagDec()));

        calcAzError();

        settingView = false;
    }

    private void calcAzError() {
        if (_TravPoint.getFwdAz() != null && _TravPoint.getBkAz() != null) {
            double diff = TtUtils.Math.azimuthDiff(_TravPoint.getFwdAz(), _TravPoint.getBkAz());

            if (diff >= 0.01) {
                tvAzDiff.setText(String.format("(%s)", StringEx.toStringRound(diff, 2)));
                tvAzDiff.setVisibility(View.VISIBLE);
                return;
            }
        }

        tvAzDiff.setVisibility(View.GONE);
    }

    @Override
    protected void onBaseLockChanged(boolean locked) {
        txtFwd.setEnabled(!locked);
        txtBk.setEnabled(!locked);
        txtSlopeDist.setEnabled(!locked);
        txtSlopeAng.setEnabled(!locked);
    }
}
