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
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
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

        txtFwd = (EditText)view.findViewById(R.id.pointTravTxtAzFwd);
        txtBk = (EditText)view.findViewById(R.id.pointTravTxtAzBk);
        txtSlopeDist = (EditText)view.findViewById(R.id.pointTravTxtSlopeDist);
        txtSlopeAng = (EditText)view.findViewById(R.id.pointTravTxtSlopeAng);

        tvMagDec = (TextView)view.findViewById(R.id.pointTravTvMagDec);
        tvAzDiff = (TextView)view.findViewById(R.id.pointTravAzDiff);

        if (_TravPoint != null) {
            setViews();
        }

        //region TextChange
        txtFwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    //Double az = _TravPoint.getFwdAz();

                    //if ((value == null ^ az == null) ||
                            //value != null && !TtUtils.Math.cmpa(value, az)) {
                        _TravPoint.setFwdAz(value);
                        calcAzError();
                        getPointsActivity().updatePoint(_TravPoint);
                    //}
                }
            }
        });

        txtBk.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    //Double az = _TravPoint.getBkAz();

                    //if ((value == null ^ az == null) ||
                            //value != null && !TtUtils.Math.cmpa(value, az)) {
                        _TravPoint.setBkAz(value);
                        calcAzError();
                        getPointsActivity().updatePoint(_TravPoint);
                    //}
                }
            }
        });

        txtSlopeDist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = 0d;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    //if (value != null) {
                        value = TtUtils.Convert.distance(value, Dist.Meters, getMetadata().getDistance());

                        //if (!TtUtils.Math.cmpa(value, _TravPoint.getSlopeDistance())) {
                            _TravPoint.setSlopeDistance(value);
                            getPointsActivity().updatePoint(_TravPoint);
                        //}
                    //}
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

                    //if (value != null) {
                        value = TtUtils.Convert.angle(value, Slope.Percent, getMetadata().getSlope());

                        //if (!TtUtils.Math.cmpa(value, _TravPoint.getSlopeAngle())) {
                            _TravPoint.setSlopeAngle(value);
                            getPointsActivity().updatePoint(_TravPoint);
                        //}
                    //}
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
    public void onPointUpdated(TtPoint point) {
        super.onPointUpdated(point);
        _TravPoint = (TravPoint)point;
        setViews();
    }

    private void setViews() {
        settingView = true;

        txtFwd.setText(StringEx.toString(TtUtils.Math.round(_TravPoint.getFwdAz(), Consts.Minimum_Point_Display_Digits)));
        txtBk.setText(StringEx.toString(TtUtils.Math.round(_TravPoint.getBkAz(), Consts.Minimum_Point_Display_Digits)));

        txtSlopeDist.setText(StringEx.toString(TtUtils.Math.round(
                TtUtils.Convert.distance(_TravPoint.getSlopeDistance(), getMetadata().getDistance(), Dist.Meters),
                Consts.Minimum_Point_Display_Digits
        )));

        txtSlopeAng.setText(StringEx.toString(TtUtils.Math.round(
                TtUtils.Convert.angle(_TravPoint.getSlopeAngle(), getMetadata().getSlope(), Slope.Percent),
                Consts.Minimum_Point_Display_Digits
        )));

        tvMagDec.setText(StringEx.toString(getMetadata().getMagDec()));

        calcAzError();

        settingView = false;
    }

    private void calcAzError() {
        if (_TravPoint.getFwdAz() != null && _TravPoint.getBkAz() != null) {
            double diff = TtUtils.Math.azimuthDiff(_TravPoint.getFwdAz(), _TravPoint.getBkAz());

            if (diff >= 0.01) {
                tvAzDiff.setText(StringEx.toString(TtUtils.Math.round(diff, 2)));
                tvAzDiff.setVisibility(View.VISIBLE);
                return;
            }
        }

        tvAzDiff.setVisibility(View.GONE);
    }

    @Override
    public void onLockChange(boolean locked) {
        super.onLockChange(locked);

        txtFwd.setEnabled(!locked);
        txtBk.setEnabled(!locked);
        txtSlopeDist.setEnabled(!locked);
        txtSlopeAng.setEnabled(!locked);
    }
}
