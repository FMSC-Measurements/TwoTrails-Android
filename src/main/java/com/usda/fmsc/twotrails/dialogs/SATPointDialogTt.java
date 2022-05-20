package com.usda.fmsc.twotrails.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.points.WayPoint;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.utilities.ClosestPositionCalculator;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.Locale;
import java.util.MissingFormatArgumentException;

public class SATPointDialogTt extends TtBaseDialogFragment {
    private final static String SAVE_POINT = "save_point";
    private final static String CLOSEST_POINT1 = "closest_point1";
    private final static String CLOSEST_POINT2 = "closest_point2";
    private final static String CLOSEST_POLYGON = "closest_polygon";
    private final static String DIST_TO_POLY = "dist_to_poly";
    private final static String AZ_TO_POLY = "az_to_poly";
    private final static String IS_INSIDE_POLY = "inside_poly";
    private final static String CP_X = "cp_x";
    private final static String CP_Y = "cp_y";

    private WayPoint _Point;
    private TtPoint _CPoint1 = null, _CPoint2 = null;
    private TtPolygon _ClosestPolygon;
    private double _DistToPoly, _AzToPoly, _CPX, _CPY;
    boolean _IsInsidePoly;

    private Listener _Listener = null;

    public static SATPointDialogTt newInstance(WayPoint point, ClosestPositionCalculator.ClosestPosition position) {
        Bundle args = new Bundle();

        args.putParcelable(SAVE_POINT, point);

        if (position.isPositionPoint1()) {
            args.putParcelable(CLOSEST_POINT1, position.getPoint1());
        } else if (position.isPositionPoint2()) {
            args.putParcelable(CLOSEST_POINT2, position.getPoint2());
        } else {
            args.putParcelable(CLOSEST_POINT1, position.getPoint1());
            args.putParcelable(CLOSEST_POINT2, position.getPoint2());
        }

        double az = position.getAzimuthToClosestPosition(point);

        args.putParcelable(CLOSEST_POLYGON, position.getPolygon());
        args.putDouble(DIST_TO_POLY, position.getDistance());
        args.putDouble(AZ_TO_POLY, az);
        args.putBoolean(IS_INSIDE_POLY, position.isInsidePoly());

        args.putDouble(CP_X, position.getCoords().getX());
        args.putDouble(CP_Y, position.getCoords().getY());

        SATPointDialogTt fragment = new SATPointDialogTt();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null) {
            _Point = bundle.getParcelable(SAVE_POINT);

            if (bundle.containsKey(CLOSEST_POINT1))
                _CPoint1 = bundle.getParcelable(CLOSEST_POINT1);

            if (bundle.containsKey(CLOSEST_POINT2))
                _CPoint2 = bundle.getParcelable(CLOSEST_POINT2);

            _ClosestPolygon = bundle.getParcelable(CLOSEST_POLYGON);
            _DistToPoly = bundle.getDouble(DIST_TO_POLY);
            _AzToPoly = bundle.getDouble(AZ_TO_POLY);
            _IsInsidePoly = bundle.getBoolean(IS_INSIDE_POLY);

            _CPX = bundle.getDouble(CP_X);
            _CPY = bundle.getDouble(CP_Y);
        } else {
            throw new MissingFormatArgumentException("Bundle");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (_Point == null) {
            throw new RuntimeException("No Point");
        }

        AlertDialog.Builder db = new AlertDialog.Builder(getActivity());

        View view = getLayoutInflater().inflate(R.layout.diag_sat_point, null);

        TtMetadata defMeta = getTtAppCtx().getDAL().getDefaultMetadata();

        TextView tvUtmX, tvUtmY, tvPoly, tvElev, tvCDist, tvCPoly, tvPoP, tvCPUtmX, tvCPUtmY, tvAzTrue, tvAzMag;
        EditText txtPid, txtDesc;
        ImageView imgInsidePoly;

        txtPid = view.findViewById(R.id.txtPid);
        imgInsidePoly = view.findViewById(R.id.ivInPoly);
        tvUtmX = view.findViewById(R.id.tvUtmX);
        tvUtmY = view.findViewById(R.id.tvUtmY);
        tvPoly = view.findViewById(R.id.tvPoly);
        tvElev = view.findViewById(R.id.tvElev);

        tvCDist = view.findViewById(R.id.tvCDist);
        tvCPoly = view.findViewById(R.id.tvCPoly);
        tvPoP = view.findViewById(R.id.tvPoP);
        tvCPUtmX = view.findViewById(R.id.tvCPUtmX);
        tvCPUtmY = view.findViewById(R.id.tvCPUtmY);
        tvAzTrue = view.findViewById(R.id.tvAzTrue);
        tvAzMag = view.findViewById(R.id.tvAzMag);

        txtDesc = view.findViewById(R.id.txtDesc);

        txtPid.setText(StringEx.toString(_Point.getPID()));
        imgInsidePoly.setImageResource(_IsInsidePoly ? R.drawable.ic_in_poly_dark : R.drawable.ic_out_poly_dark);
        imgInsidePoly.setContentDescription(_IsInsidePoly ? "Inside Polygon" : "Outside Polygon");

        tvUtmX.setText(StringEx.toString(_Point.getAdjX(), Consts.Minimum_Point_Display_Digits));
        tvUtmY.setText(StringEx.toString(_Point.getAdjY(), Consts.Minimum_Point_Display_Digits));
        tvElev.setText(String.format(Locale.getDefault(), "%.3f (%s)",
                TtUtils.Convert.distance(_Point.getElevation(), TtUtils.Convert.elevationToDistance(defMeta.getElevation()), Dist.Meters),
                defMeta.getElevation().toStringAbv()));
        tvPoly.setText(_Point.getPolyName());

        tvCDist.setText(String.format(Locale.getDefault(), "%.2f (%s)",
                TtUtils.Convert.distance(_DistToPoly, defMeta.getDistance(), Dist.Meters),
                defMeta.getDistance().toStringAbv()));
        tvCPoly.setText(_ClosestPolygon.getName());

        if (_CPoint1 == null ^ _CPoint2 == null) {
            TtPoint pt = _CPoint2 == null ? _CPoint1 : _CPoint2;
            tvPoP.setText(String.format(Locale.getDefault(), "%d (%s)", pt.getPID(), pt.getOp()));
        } else {
            tvPoP.setText(String.format(Locale.getDefault(), "%d \u21F9 %d", _CPoint1.getPID(), _CPoint2.getPID()));
        }

        tvCPUtmX.setText(StringEx.toString(_CPX, Consts.Minimum_Point_Display_Digits));
        tvCPUtmY.setText(StringEx.toString(_CPY, Consts.Minimum_Point_Display_Digits));

        tvAzTrue.setText(String.format(Locale.getDefault(), "%.0f\u00B0", _AzToPoly));
        tvAzMag.setText(String.format(Locale.getDefault(), "%.0f\u00B0", _AzToPoly - defMeta.getMagDec()));

        txtDesc.setText(_Point.getComment());

        db.setView(view)
        .setPositiveButton(getString(R.string.str_create), (dialog, which) -> {
            if (_Listener != null) {

                String pidv = txtPid.getText().toString();
                String cmtv = txtDesc.getText().toString();

                try {
                    int pid = Integer.parseUnsignedInt(pidv);
                    _Listener.onSave(pid, cmtv);
                } catch (NumberFormatException e) {
                    _Listener.onSave(null, cmtv);
                }
            }
        })
        .setNegativeButton("Retake", (dialog, which) -> {
            if (_Listener != null) {
                _Listener.retake();
            }
        })
        .setNeutralButton(getString(R.string.str_cancel), (dialog, which) -> {
            if (_Listener != null) {
                _Listener.onCancel();
            }
        })
        .setOnCancelListener(dialog -> {
            if (_Listener != null) {
                _Listener.onCancel();
            }
        })
        .setOnDismissListener(dialog -> {
            if (_Listener != null) {
                _Listener.onCancel();
            }
        });

        return db.create();
    }

    public SATPointDialogTt setListener(Listener listener) {
        _Listener = listener;
        return this;
    }

    public interface Listener {
        void onSave(Integer pid, String comment);
        void retake();
        void onCancel();
    }
}
