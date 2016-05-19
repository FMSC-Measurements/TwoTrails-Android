package com.usda.fmsc.twotrails.fragments.points;


import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class GPSPointFragment extends BasePointFragment {
    EditText txtX, txtY, txtElev, txtManAcc;
    TextView tvElev, tvRMSEr, tvNSSDA;

    private boolean settingView = false;

    private GpsPoint _GpsPoint;


    public static GPSPointFragment newInstance(GpsPoint point) {
        return newInstance(point, false);
    }

    public static GPSPointFragment newInstance(GpsPoint point, boolean hidden) {
        GPSPointFragment fragment = new GPSPointFragment();
        Bundle args = new Bundle();
        args.putParcelable(POINT, point);
        args.putBoolean(HIDDEN, hidden);
        fragment.setArguments(args);
        return fragment;
    }


    public GPSPointFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getBasePoint() != null) {
            _GpsPoint = (GpsPoint)getBasePoint();
        }
    }

    @Override
    public View onCreateViewEx(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_points_gpspoint_card, null);

        txtX = (EditText)view.findViewById(R.id.pointsFragGpsTxtX);
        txtY = (EditText)view.findViewById(R.id.pointsFragGpsTxtY);
        txtElev = (EditText)view.findViewById(R.id.pointsFragGpsTxtElev);
        txtManAcc = (EditText)view.findViewById(R.id.pointsFragGpsTvManAcc);

        tvElev = (TextView)view.findViewById(R.id.pointsFragGpsTvElev);
        tvRMSEr = (TextView)view.findViewById(R.id.pointsFragGpsTvRMSEr);
        tvNSSDA = (TextView)view.findViewById(R.id.pointsFragGpsTvNSSDA);

        if (_GpsPoint != null) {
            setViews();
        }

        //region TextChange
        txtX.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = 0d;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString(), 0d);
                    }

                    //if (value != null && !TtUtils.Math.cmpa(value, _GpsPoint.getUnAdjX())) {
                        _GpsPoint.setUnAdjX(value);
                        _GpsPoint.clearLatLon();
                        getPointsActivity().updatePoint(_GpsPoint);
                    //}
                }
            }
        });

        txtY.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = 0d;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString(), 0d);
                    }

                    //if (value != null && !TtUtils.Math.cmpa(value, _GpsPoint.getUnAdjY())) {
                        _GpsPoint.setUnAdjY(value);
                        _GpsPoint.clearLatLon();
                        getPointsActivity().updatePoint(_GpsPoint);
                    //}
                }
            }
        });

        txtElev.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = 0d;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString(), 0d);
                    }

                    //if (value != null) {
                        value = TtUtils.Convert.distance(value, UomElevation.Meters, getMetadata().getElevation());

                        //if (!TtUtils.Math.cmpa(value, _GpsPoint.getUnAdjZ())) {
                            _GpsPoint.setUnAdjZ(value);
                            _GpsPoint.clearLatLon();
                            getPointsActivity().updatePoint(_GpsPoint);
                        //}
                    //}
                }
            }
        });

        txtManAcc.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    //Double ma = _GpsPoint.getManualAccuracy();

                    //if ((value == null ^ ma == null) ||
                            //value != null && !TtUtils.Math.cmpa(value, ma)) {
                        _GpsPoint.setManualAccuracy(value);
                        getPointsActivity().updatePoint(_GpsPoint);
                    //}
                }
            }
        });
        //endregion

        final View parent = view.findViewById(R.id.parentLayout);
        EditText txtCmt = (EditText)view.findViewById(R.id.pointTxtCmt);

        //region Focus
        AndroidUtils.UI.removeSelectionOnUnfocus(txtX);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtY);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtElev);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtManAcc);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtCmt);
        //endregion

        EditText[] ets = new EditText[] {
                txtX,
                txtY,
                txtElev,
                txtManAcc,
                txtCmt
        };

        AndroidUtils.UI.hideKeyboardOnTouch(parent, ets);

        return view;
    }

    @Override
    public void onPointUpdated(TtPoint point) {
        super.onPointUpdated(point);
        _GpsPoint = (GpsPoint)point;
        setViews();
    }

    private void setViews() {
        settingView = true;

        txtX.setText(StringEx.toString(TtUtils.Math.round(_GpsPoint.getUnAdjX(), Consts.Minimum_Point_Display_Digits)));
        txtY.setText(StringEx.toString(TtUtils.Math.round(_GpsPoint.getUnAdjY(), Consts.Minimum_Point_Display_Digits)));

        txtElev.setText(StringEx.toString(TtUtils.Math.round(
                TtUtils.Convert.distance(_GpsPoint.getUnAdjZ(), getMetadata().getElevation(), UomElevation.Meters),
                Consts.Minimum_Point_Display_Digits
        )));

        txtManAcc.setText(StringEx.toString(TtUtils.Math.round(_GpsPoint.getManualAccuracy(), 5)));

        tvElev.setText(getMetadata().getElevation().toString());

        String rmser = getString(R.string.str_nullvalue);
        String nssda = rmser;

        if (_GpsPoint.getRMSEr() != null) {
            rmser = String.format("%.2f", _GpsPoint.getRMSEr());
        }

        if (_GpsPoint.getNSSDA_RMSEr() != null) {
            nssda = String.format("%.2f", _GpsPoint.getNSSDA_RMSEr());
        }

        tvRMSEr.setText(rmser);
        tvNSSDA.setText(nssda);

        settingView = false;
    }

    @Override
    public void onLockChange(boolean locked) {
        super.onLockChange(locked);

        txtX.setEnabled(!locked);
        txtY.setEnabled(!locked);
        txtElev.setEnabled(!locked);
        txtManAcc.setEnabled(!locked);
    }
}
