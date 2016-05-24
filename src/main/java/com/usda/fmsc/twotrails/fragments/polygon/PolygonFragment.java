package com.usda.fmsc.twotrails.fragments.polygon;

import android.content.Context;
import android.os.Bundle;
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
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.activities.PolygonsActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.ui.StaticPolygonView;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.utilities.IListener;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class PolygonFragment extends AnimationCardFragment implements PolygonsActivity.Listener {
    private static final String POLYGON_CN = "PolygonCN";

    private PolygonsActivity activity;

    private View viewPreFocus;
    private ScrollView scrollView;
    private TextView tvName;
    private EditText txtName, txtDesc, txtInc, txtPsi, txtAcc;

    private boolean settingView;

    private TtPolygon _Polygon;


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
        scrollView = (ScrollView)view.findViewById(R.id.polyFragScrollView);

        viewPreFocus = view.findViewById(R.id.preFocusView);
        
        if (_Polygon != null) {
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

        if (Global.getDAL().getPointCountInPolygon(_Polygon.getCN()) > 2) {
            View polyLayImage = view.findViewById(R.id.polyLayImage);

            if (polyLayImage != null) {
                polyLayImage.setVisibility(View.VISIBLE);
            }
        }

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

        //tvName.setAlpha(locked ? Consts.DISABLED_ALPHA : Consts.ENABLED_ALPHA);

        if (locked) {
            viewPreFocus.requestFocus();
        }
    }

    @Override
    public void onPolygonUpdated(TtPolygon polygon) {
        _Polygon = polygon;

        settingView = true;

        tvName.setText(_Polygon.getName());
        txtName.setText(_Polygon.getName());
        txtDesc.setText(_Polygon.getDescription());
        txtInc.setText(StringEx.toString(_Polygon.getIncrementBy()));
        txtPsi.setText(StringEx.toString(_Polygon.getPointStartIndex()));
        txtAcc.setText(StringEx.toString(_Polygon.getAccuracy()));

        settingView = false;
    }


    public void scrollToTop() {
        if (scrollView != null) {
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }

    @Override
    public void onCardFocused() {
        super.onCardFocused();

        View view = getView();

        if (view != null) {
            final StaticPolygonView spv = (StaticPolygonView)view.findViewById(R.id.polySPView);

            if (spv != null && !spv.isRendered() && Global.getDAL().getPointCountInPolygon(_Polygon.getCN()) > 2) {
                if (spv.getWidth() < 1) {
                    view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (!spv.isRendered()) {
                                spv.render(Global.getDAL().getPointsInPolygon(_Polygon.getCN()), Global.getDAL().getMetadataMap());
                            }
                        }
                    });
                } else {
                    spv.render(Global.getDAL().getPointsInPolygon(_Polygon.getCN()), Global.getDAL().getMetadataMap());
                }
            }
        }
    }
}
