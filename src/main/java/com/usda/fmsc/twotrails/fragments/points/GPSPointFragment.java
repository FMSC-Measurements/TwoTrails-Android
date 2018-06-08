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
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class GPSPointFragment extends BasePointFragment {
    private EditText txtX, txtY, txtElev, txtManAcc;
    private TextView tvElev, tvRMSEr, tvNSSDA;

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
        View view = inflater.inflate(R.layout.fragment_points_gpspoint_card, container, false);

        txtX = view.findViewById(R.id.pointsFragGpsTxtX);
        txtY = view.findViewById(R.id.pointsFragGpsTxtY);
        txtElev = view.findViewById(R.id.pointsFragGpsTxtElev);
        txtManAcc = view.findViewById(R.id.pointsFragGpsTvManAcc);

        tvElev = view.findViewById(R.id.pointsFragGpsTvElev);
        tvRMSEr = view.findViewById(R.id.pointsFragGpsTvRMSEr);
        tvNSSDA = view.findViewById(R.id.pointsFragGpsTvNSSDA);

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

                    _GpsPoint.setUnAdjX(value);
                    _GpsPoint.clearLatLon();
                    getPointController().updatePoint(_GpsPoint);
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

                    _GpsPoint.setUnAdjY(value);
                    _GpsPoint.clearLatLon();
                    getPointController().updatePoint(_GpsPoint);
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

                    value = TtUtils.Convert.distance(value, UomElevation.Meters, getMetadata().getElevation());

                    _GpsPoint.setUnAdjZ(value);
                    _GpsPoint.clearLatLon();
                    getPointController().updatePoint(_GpsPoint);
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

                    _GpsPoint.setManualAccuracy(value);
                    getPointController().updatePoint(_GpsPoint);
                }
            }
        });
        //endregion

        final View parent = view.findViewById(R.id.parentLayout);
        EditText txtCmt = view.findViewById(R.id.pointTxtCmt);

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
    protected void onBasePointUpdated() {
        _GpsPoint = (GpsPoint)getBasePoint();
        setViews();
    }

    private void setViews() {
        settingView = true;

        txtX.setText(StringEx.toStringRound(_GpsPoint.getUnAdjX(), Consts.Minimum_Point_Display_Digits));
        txtY.setText(StringEx.toStringRound(_GpsPoint.getUnAdjY(), Consts.Minimum_Point_Display_Digits));

        txtElev.setText(StringEx.toStringRound(
                TtUtils.Convert.distance(_GpsPoint.getUnAdjZ(), getMetadata().getElevation(), UomElevation.Meters),
                Consts.Minimum_Point_Display_Digits
        ));

        txtManAcc.setText(StringEx.toStringRound(_GpsPoint.getManualAccuracy(), 5));

        tvElev.setText(getMetadata().getElevation().toString());

        String rmser = getString(R.string.str_nullvalue);
        String nssda = rmser;

        if (_GpsPoint.getRMSEr() != null) {
            rmser = StringEx.toString( _GpsPoint.getRMSEr(), 2);
        }

        if (_GpsPoint.getNSSDA_RMSEr() != null) {
            nssda = StringEx.toString(_GpsPoint.getNSSDA_RMSEr(), 2);
        }

        tvRMSEr.setText(rmser);
        tvNSSDA.setText(nssda);

        settingView = false;
    }

    @Override
    protected void onBaseLockChanged(boolean locked) {
        txtX.setEnabled(!locked);
        txtY.setEnabled(!locked);
        txtElev.setEnabled(!locked);
        txtManAcc.setEnabled(!locked);
    }
}
