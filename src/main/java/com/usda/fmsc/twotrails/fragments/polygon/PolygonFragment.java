package com.usda.fmsc.twotrails.fragments.polygon;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
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
    private TextView tvName, tvPerimFt, tvPerimMt, tvAreaAc, tvAreaHa;
    private EditText txtName, txtDesc, txtInc, txtPsi, txtAcc;
    private StaticPolygonView spv;

    private boolean settingView;

    private TtPolygon _Polygon;

    private List<PointD> polyPoints;


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
            String polyCN = bundle.getString(POLYGON_CN);

            if (activity != null) {
                _Polygon = activity.getPolygon(polyCN);
                activity.register(polyCN, this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_polygon_card, container, false);

        tvName = (TextView)view.findViewById(R.id.polyFragTvName);
        txtName = (EditText)view.findViewById(R.id.polyFragTxtName);
        txtDesc = (EditText)view.findViewById(R.id.polyFragTxtDesc);
        txtInc = (EditText)view.findViewById(R.id.polyFragTxtInc);
        txtPsi = (EditText)view.findViewById(R.id.polyFragTxtPsi);
        txtAcc = (EditText)view.findViewById(R.id.polyFragTxtAcc);

        tvPerimFt = (TextView)view.findViewById(R.id.polyFragTvPerimFt);
        tvPerimMt = (TextView)view.findViewById(R.id.polyFragTvPerimMt);
        tvAreaAc = (TextView)view.findViewById(R.id.polyFragTvAreaAc);
        tvAreaHa = (TextView)view.findViewById(R.id.polyFragTvAreaHa);

        scrollView = (ScrollView)view.findViewById(R.id.polyFragScrollView);
        spv = (StaticPolygonView)view.findViewById(R.id.polySPView);

        viewPreFocus = view.findViewById(R.id.preFocusView);

        if (_Polygon != null) {
            final View v = view;

            if (Global.getDAL().getPointCountInPolygon(_Polygon.getCN()) < 3) {
                View polyLayImage = view.findViewById(R.id.polyLayImage);

                if (polyLayImage != null) {
                    polyLayImage.setVisibility(View.GONE);
                }
            } else {
                polyPoints = activity.getDrawPoints(_Polygon.getCN(), 0);

                if (polyPoints == null) {
                    v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (spv != null && activity != null) {
                                polyPoints = activity.getDrawPoints(_Polygon.getCN(), spv.getWidth());
                                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    });
                }
            }

            onPolygonUpdated(_Polygon);
        }

        //region TextChange
        txtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

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

        txtDesc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {if (!settingView) {
                String value = s.toString();

                if (!StringEx.isEmpty(value)) {
                    _Polygon.setDescription(value);

                    activity.updatePolygon(_Polygon);
                }
            } }
        });

        txtInc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

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

        txtPsi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

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

        txtAcc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

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
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Polygon Listener");
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


    private void setViews() {
        settingView = true;

        tvName.setText(_Polygon.getName());
        txtName.setText(_Polygon.getName());
        txtDesc.setText(_Polygon.getDescription());
        txtInc.setText(StringEx.toString(_Polygon.getIncrementBy()));
        txtPsi.setText(StringEx.toString(_Polygon.getPointStartIndex()));
        txtAcc.setText(StringEx.toString(TtUtils.Math.round(_Polygon.getAccuracy(), 2)));

        tvPerimFt.setText(StringEx.toString(TtUtils.Math.round(TtUtils.Convert.toFeetTenths(_Polygon.getPerimeter(), Dist.Meters), 2)));
        tvPerimMt.setText(StringEx.toString(TtUtils.Math.round(_Polygon.getPerimeter(), 2)));
        tvAreaAc.setText(StringEx.toString(TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(_Polygon.getArea()), 2)));
        tvAreaHa.setText(StringEx.toString(TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(_Polygon.getArea()), 2)));


        if (polyPoints == null && activity != null) {
            polyPoints = activity.getDrawPoints(_Polygon.getCN(), 0);
        }

        spv.render(polyPoints);

        settingView = false;
    }

    public void scrollToTop() {
        if (scrollView != null) {
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }
}
