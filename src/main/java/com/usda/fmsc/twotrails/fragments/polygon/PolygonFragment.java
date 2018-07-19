package com.usda.fmsc.twotrails.fragments.polygon;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.activities.PolygonsActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.ui.StaticPolygonView;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

import java.util.List;

public class PolygonFragment extends AnimationCardFragment implements PolygonsActivity.Listener {
    private static final String POLYGON_CN = "PolygonCN";

    private PolygonsActivity activity;

    private View viewPreFocus;
    private ScrollView scrollView;
    private TextView tvName, tvPerimFt, tvPerimMt, tvPerimLineFt, tvPerimLineMt, tvAreaAc, tvAreaHa;
    private EditText txtName, txtDesc, txtInc, txtPsi, txtAcc;
    private StaticPolygonView spv;

    private boolean settingView;

    private TtPolygon _Polygon;
    private String _PolyCN;


    public static PolygonFragment newInstance(String polyCN, boolean hidden) {
        PolygonFragment fragment = new PolygonFragment();
        Bundle args = new Bundle();
        args.putString(POLYGON_CN, polyCN);
        args.putBoolean(HIDDEN, hidden);
        fragment.setArguments(args);
        return fragment;
    }



    public PolygonFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null) {
            _PolyCN = bundle.getString(POLYGON_CN);

//            if (activity != null) {
//                _Polygon = activity.getPolygon(_PolyCN);
//                activity.register(_PolyCN, this);
//            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_polygon_card, container, false);

        tvName = view.findViewById(R.id.polyFragTvName);
        txtName = view.findViewById(R.id.polyFragTxtName);
        txtDesc = view.findViewById(R.id.polyFragTxtDesc);
        txtInc = view.findViewById(R.id.polyFragTxtInc);
        txtPsi = view.findViewById(R.id.polyFragTxtPsi);
        txtAcc = view.findViewById(R.id.polyFragTxtAcc);

        tvPerimFt = view.findViewById(R.id.polyFragTvPerimFt);
        tvPerimMt = view.findViewById(R.id.polyFragTvPerimMt);
        tvPerimLineFt = view.findViewById(R.id.polyFragTvPerimLineFt);
        tvPerimLineMt = view.findViewById(R.id.polyFragTvPerimLineMt);
        tvAreaAc = view.findViewById(R.id.polyFragTvAreaAc);
        tvAreaHa = view.findViewById(R.id.polyFragTvAreaHa);

        scrollView = view.findViewById(R.id.polyFragScrollView);
        spv = view.findViewById(R.id.polySPView);

        viewPreFocus = view.findViewById(R.id.preFocusView);

        if (_Polygon != null) {
            if (spv.getWidth() == 0) {
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (activity != null)
                            activity.getDrawPoints(_Polygon, spv.getWidth(), false);
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }

            if (Global.getDAL().getBoundaryPointsCountInPoly(_Polygon.getCN()) < 3) {
                View polyLayImage = view.findViewById(R.id.polyLayImage);

                if (polyLayImage != null) {
                    polyLayImage.setVisibility(View.GONE);
                }
            }

            onPolygonUpdated(_Polygon);
        }

        //region TextChange
        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    String value = s.toString();

                    if (!StringEx.isEmpty(value)) {
                        _Polygon.setName(value);
                        tvName.setText(value);

                        activity.updatePolygon(_Polygon);
                    }
                }
            }
        });

        txtDesc.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {if (!settingView) {
                String value = s.toString();

                if (!StringEx.isEmpty(value)) {
                    _Polygon.setDescription(value);

                    activity.updatePolygon(_Polygon);
                }
            } }
        });

        txtInc.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Integer value;

                    if (s.length() > 0) {
                        value = ParseEx.parseInteger(s.toString());
                    } else {
                        value = 10;
                    }

                    if (value > 0) {
                        _Polygon.setIncrementBy(value);

                        activity.updatePolygon(_Polygon);
                    }
                }
            }
        });

        txtPsi.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Integer value;

                    if (s.length() > 0) {
                        value = ParseEx.parseInteger(s.toString());
                    } else {
                        value = 1010;
                    }

                    if (value >= 0) {
                        _Polygon.setPointStartIndex(value);

                        activity.updatePolygon(_Polygon);
                    }
                }
            }
        });

        txtAcc.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    double ma = _Polygon.getAccuracy();

                    if (value == null || !TtUtils.Math.cmpa(value, ma)) {
                        if (value == null)
                            value = Consts.Default_Point_Accuracy;

                        _Polygon.setAccuracy(value);
                        activity.updatePolygon(_Polygon);
                    }
                }
            }
        });
        //endregion

        //region Focus
        AndroidUtils.UI.removeSelectionOnUnfocus(txtName);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtDesc);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtInc);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtPsi);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtAcc);
        //endregion

        final View parent = view.findViewById(R.id.parentLayout);

        EditText[] ets = new EditText[] {
                txtName,
                txtDesc,
                txtInc,
                txtPsi,
                txtAcc
        };

        AndroidUtils.UI.hideKeyboardOnSelect(parent, ets);

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            this.activity = (PolygonsActivity) activity;

            if (activity != null) {
                _Polygon = this.activity.getPolygon(_PolyCN);
                TtUtils.TtReport.writeError("Unable to get Polygon", "PolygonFragment");
                this.activity.register(_PolyCN, this);
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PolygonsActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activity != null && _Polygon != null) {
            activity.unregister(_Polygon.getCN());
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activity != null && _Polygon != null) {
            activity.unregister(_Polygon.getCN());
            activity = null;
        }
    }


    @Override
    public void onLockChange(boolean locked) {
        txtName.setEnabled(!locked);
        txtDesc.setEnabled(!locked);
        txtInc.setEnabled(!locked);
        txtPsi.setEnabled(!locked);
        txtAcc.setEnabled(!locked);

        if (locked) {
            viewPreFocus.requestFocus();
        }
    }

    @Override
    public void onPolygonUpdated(TtPolygon polygon) {
        _Polygon = polygon;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            setViews();
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setViews();
                }
            });
        }
    }

    @Override
    public void onPolygonPointsUpdated(TtPolygon polygon) {
        List<PointD> polyPoints = activity.getDrawPoints(_Polygon, spv.getWidth(), true);

        if (polyPoints != null)
            spv.render(polyPoints);
    }


    private void setViews() {
        settingView = true;

        tvName.setText(_Polygon.getName());
        txtName.setText(_Polygon.getName());
        txtDesc.setText(_Polygon.getDescription());

        txtInc.setText(StringEx.toString(_Polygon.getIncrementBy()));
        txtPsi.setText(StringEx.toString(_Polygon.getPointStartIndex()));
        txtAcc.setText(StringEx.toStringRound(_Polygon.getAccuracy(), 2));

        tvPerimFt.setText(StringEx.toStringRound(TtUtils.Convert.toFeetTenths(_Polygon.getPerimeter(), Dist.Meters), 2));
        tvPerimMt.setText(StringEx.toStringRound(_Polygon.getPerimeter(), 2));

        tvPerimLineFt.setText(StringEx.toStringRound(TtUtils.Convert.toFeetTenths(_Polygon.getPerimeterLine(), Dist.Meters), 2));
        tvPerimLineMt.setText(StringEx.toStringRound(_Polygon.getPerimeterLine(), 2));

        tvAreaAc.setText(StringEx.toStringRound(TtUtils.Convert.metersSquaredToAcres(_Polygon.getArea()), 4));
        tvAreaHa.setText(StringEx.toStringRound(TtUtils.Convert.metersSquaredToHa(_Polygon.getArea()), 4));

        List<PointD> polyPoints = activity.getDrawPoints(_Polygon, spv.getWidth(), false);

        if (polyPoints != null)
            spv.render(polyPoints);

        settingView = false;
    }

    public void scrollToTop() {
        if (scrollView != null) {
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }
}
