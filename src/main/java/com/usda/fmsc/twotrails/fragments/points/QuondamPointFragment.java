package com.usda.fmsc.twotrails.fragments.points;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.adapters.SelectableStringArrayAdapter;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.adapters.PointDetailsAdapter;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;
import java.util.List;

import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;


public class QuondamPointFragment extends BasePointFragment {
    private boolean settingView = false;

    private ListView lvPolys, lvPoints;
    private EditText txtManAcc;
    private int selectedColor, nonSelectedColor;

    private ArrayList<?>[] _Points;
    private ArrayList<TtPolygon> _Polygons;

    private int selectedPolyIndex = -1;
    private TtPoint selectedPoint;

    private QuondamPoint _Quondam;

    private SelectableStringArrayAdapter polysAdapter;
    private PointDetailsAdapter pointsAdapter;



    public static QuondamPointFragment newInstance(QuondamPoint point) {
        return newInstance(point, false);
    }

    public static QuondamPointFragment newInstance(QuondamPoint point, boolean hidden) {
        QuondamPointFragment fragment = new QuondamPointFragment();
        Bundle args = new Bundle();
        args.putParcelable(POINT, point);
        args.putBoolean(HIDDEN, hidden);
        fragment.setArguments(args);
        return fragment;
    }


    public QuondamPointFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getBasePoint() != null) {
            _Quondam = (QuondamPoint)getBasePoint();
        }

        selectedColor = AndroidUtils.UI.getColor(getContext(), R.color.primaryLighter);
        nonSelectedColor = AndroidUtils.UI.getColor(getContext(), R.color.grey_50);
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateViewEx(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_points_quondam_card, container, false);

        _Polygons = Global.getDAL().getPolygons();
        ArrayList<String> _PolygonNames = new ArrayList<>();
        _Points = new ArrayList<?>[_Polygons.size()];


        for (TtPolygon poly : _Polygons) {
            _PolygonNames.add(poly.getName());
        }

        lvPolys = (ListView)view.findViewById(R.id.pointsFragQndmLvPolys);
        lvPoints = (ListView)view.findViewById(R.id.pointsFragQndmLvPoints);
        txtManAcc = (EditText)view.findViewById(R.id.pointsFragQndmTxtManAcc);

        polysAdapter = new SelectableStringArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, _PolygonNames);
        polysAdapter.setSelectedColor(selectedColor);
        polysAdapter.setNonSelectedColor(nonSelectedColor);

        lvPolys.setAdapter(polysAdapter);
        lvPolys.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {

                setPoints(index);

                selectedPolyIndex = index;

                polysAdapter.setSelected(index);
                lvPolys.invalidateViews();
            }
        });


        lvPoints.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                if (selectedPolyIndex > -1) {
                    List<TtPoint> points = (ArrayList<TtPoint>) _Points[selectedPolyIndex];

                    if (points != null && !settingView) {
                        TtPoint parent = points.get(index);

                        if (parent.getOp() == OpType.Quondam) {
                            parent = ((QuondamPoint)parent).getParentPoint();
                        }

                        if (!_Quondam.hasParent() || _Quondam.getParentPID() != parent.getPID()) {
                            selectedPoint = parent;
                            _Quondam.setParentPoint(parent);
                            getPointsActivity().updatePoint(_Quondam);
                        }

                        pointsAdapter.setSelected(index);
                        lvPoints.invalidateViews();
                    }
                }
            }
        });

        txtManAcc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Double value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseDouble(s.toString());
                    }

                    _Quondam.setManualAccuracy(value);
                    getPointsActivity().updatePoint(_Quondam);
                }
            }
        });

        if (_Quondam != null) {
            for (int i = 0; i < _Polygons.size(); i++) {
                if (_Quondam.getPolyCN().equals(_Polygons.get(i).getCN())) {
                    selectedPolyIndex = i;
                    setViews();
                    break;
                }
            }
        }

        return view;
    }

    @Override
    protected void onBasePointUpdated() {
        _Quondam = (QuondamPoint)getBasePoint();
        setViews();
    }

    @SuppressWarnings("unchecked")
    private void setViews() {
        if (_Quondam.hasParent() && (selectedPoint == null || selectedPoint.getPID() != _Quondam.getParentPID())) {
            settingView = true;

            int i = 0;
            for (TtPolygon polygon : _Polygons) {
                if (_Quondam.getParentPoint().getPolyCN().equals(polygon.getCN())) {
                    lvPolys.setSelection(i);
                    polysAdapter.setSelected(i);

                    int j = 0;

                    setPoints(selectedPolyIndex);

                    for (TtPoint p : (ArrayList<TtPoint>)_Points[selectedPolyIndex]) {
                        if (_Quondam.getParentPoint().getCN().equals(p.getCN())) {
                            lvPoints.setSelection(j);
                            pointsAdapter.setSelected(j);
                            break;
                        }
                        j++;
                    }

                    break;
                }
                i++;
            }

            txtManAcc.setText(StringEx.toString(_Quondam.getManualAccuracy()));

            settingView = false;
        }
    }

    @Override
    protected void onBaseLockChanged(boolean locked) {
        lvPoints.setAlpha(locked ? Consts.DISABLED_ALPHA : Consts.ENABLED_ALPHA);
        lvPolys.setAlpha(locked ? Consts.DISABLED_ALPHA : Consts.ENABLED_ALPHA);
        lvPoints.setEnabled(!locked);
        lvPolys.setEnabled(!locked);
        txtManAcc.setEnabled(!locked);
    }

    @SuppressWarnings("unchecked")
    private void setPoints(int index) {
        ArrayList<TtPoint> points = new ArrayList<>();

        if (_Points[index] == null) {
            ArrayList<TtPoint> tmpPoints = new ArrayList<>();
            for (TtPoint point : Global.getDAL().getPointsInPolygon(_Polygons.get(index).getCN())) {
                if (point.getOp() != OpType.WayPoint) {
                    if (point.getOp() != OpType.Quondam || !point.getCN().equals(_Quondam.getCN())) {
                        tmpPoints.add(point);
                        points.add(point);
                    }
                }
            }

            _Points[index] = tmpPoints;
        }

        if (points.size() < 1) {
            for (TtPoint point : (ArrayList<TtPoint>) _Points[index]) {
                points.add(point);
            }
        }
        pointsAdapter = new PointDetailsAdapter(getActivity(), points, AppUnits.IconColor.Black);

        pointsAdapter.setSelectedColor(selectedColor);
        pointsAdapter.setNonSelectedColor(nonSelectedColor);

        pointsAdapter.setShowQuondamLinks(true);

        lvPoints.setAdapter(pointsAdapter);
    }
}
