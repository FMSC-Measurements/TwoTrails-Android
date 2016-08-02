package com.usda.fmsc.twotrails.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import com.usda.fmsc.geospatial.DMS;
import com.usda.fmsc.utilities.StringEx;

public class LatLonDialog extends DialogFragment {
    private static String CN = "CN";
    private static String PID = "PID";
    private static String LAT = "LAT";
    private static String LON = "LON";

    private OnEditedListener listener;

    private Button posButton;
    private EditText txtLat, txtLon, txtLatDeg, txtLatMin, txtLatSec, txtLonDeg, txtLonMin, txtLonSec;
    private View layDD, layDMS;
    private String cn;
    private int pid;
    private Double lat, lon;
    private boolean dmsVisible = true, calculating;
    private int btnSelectedColor, btnColor;


    public static LatLonDialog newInstance(GpsPoint point) {
        LatLonDialog dialog = new LatLonDialog();

        Bundle bundle = new Bundle();
        bundle.putString(CN, point.getCN());
        bundle.putInt(PID, point.getPID());

        if (point.hasLatLon()) {
            bundle.putDouble(LAT, point.getLatitude());
            bundle.putDouble(LON, point.getLongitude());
        }

        dialog.setArguments(bundle);

        return dialog;
    }

    public LatLonDialog() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && !bundle.isEmpty()) {
            cn = bundle.getString(CN);
            pid = bundle.getInt(PID);

            if (bundle.containsKey(LAT)) {
                lat = bundle.getDouble(LAT);
                lon = bundle.getDouble(LON);
            }
        }

        btnColor = AndroidUtils.UI.getColor(getContext(), R.color.grey_300);
        btnSelectedColor = AndroidUtils.UI.getColor(getContext(), R.color.primaryLighter);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View view = inflater.inflate(R.layout.diag_latlon, null);

        layDD = view.findViewById(R.id.diagLatLonLayDD);
        layDMS = view.findViewById(R.id.diagLatLonLayDMS);

        txtLat = (EditText)view.findViewById(R.id.diagLatLonTxtLat);
        txtLatDeg = (EditText)view.findViewById(R.id.diagLatLonTxtLatDeg);
        txtLatMin = (EditText)view.findViewById(R.id.diagLatLonTxtLatMin);
        txtLatSec = (EditText)view.findViewById(R.id.diagLatLonTxtLatSec);

        txtLon = (EditText)view.findViewById(R.id.diagLatLonTxtLon);
        txtLonDeg = (EditText)view.findViewById(R.id.diagLatLonTxtLonDeg);
        txtLonMin = (EditText)view.findViewById(R.id.diagLatLonTxtLonMin);
        txtLonSec = (EditText)view.findViewById(R.id.diagLatLonTxtLonSec);

        final Button btnDD = (Button)view.findViewById(R.id.diagLatLonBtnDD);
        final Button btnDMS = (Button)view.findViewById(R.id.diagLatLonBtnDMS);

        btnDD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dmsVisible) {
                    ViewAnimator.expandView(layDD);
                    ViewAnimator.collapseView(layDMS);

                    btnDD.setBackgroundColor(btnSelectedColor);
                    btnDMS.setBackgroundColor(btnColor);
                    dmsVisible = false;
                }
            }
        });

        btnDMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dmsVisible) {
                    ViewAnimator.expandView(layDMS);
                    ViewAnimator.collapseView(layDD);

                    btnDD.setBackgroundColor(btnColor);
                    btnDMS.setBackgroundColor(btnSelectedColor);
                    dmsVisible = true;
                }
            }
        });
        
        
        if (lat != null) {
            DMS dmsLat = new DMS(lat);
            DMS dmsLon = new DMS(lon);
            
            txtLat.setText(StringEx.toString(lat));
            txtLon.setText(StringEx.toString(lon));
            
            txtLatDeg.setText(StringEx.toString(dmsLat.getDegrees()));
            txtLatMin.setText(StringEx.toString(dmsLat.getMinutes()));
            txtLatSec.setText(StringEx.toString(dmsLat.getSeconds()));

            txtLonDeg.setText(StringEx.toString(dmsLon.getDegrees()));
            txtLonMin.setText(StringEx.toString(dmsLon.getMinutes()));
            txtLonSec.setText(StringEx.toString(dmsLon.getSeconds()));
        }
        
        txtLat.addTextChangedListener(textWatcher);
        txtLon.addTextChangedListener(textWatcher);

        txtLatDeg.addTextChangedListener(textWatcher);
        txtLatMin.addTextChangedListener(textWatcher);
        txtLatSec.addTextChangedListener(textWatcher);

        txtLonDeg.addTextChangedListener(textWatcher);
        txtLonMin.addTextChangedListener(textWatcher);
        txtLonSec.addTextChangedListener(textWatcher);
        
        dialog.setView(view);

        dialog.setTitle(String.format("Point %d", pid));

        dialog.setPositiveButton(R.string.str_ok , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onEdited(cn, lat, lon);
                }
            }
        }).setNeutralButton(R.string.str_cancel, null);

        final Dialog d =  dialog.create();


        final ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                int width = d.getWindow().getDecorView().getWidth();
                int height = d.getWindow().getDecorView().getHeight();
                d.getWindow().setLayout(width, height);

                ViewTreeObserver obs = view.getViewTreeObserver();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }
        });

        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();

        if (d != null) {
            posButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
            posButton.setEnabled(false);
        }
    }


    public void setOnEditedListener(OnEditedListener listener) {
        this.listener = listener;
    }
    
    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            calc();
        }
    };
    
    private void calc() {
        if (!calculating) {
            try {
                calculating = true;
                if (dmsVisible) {
                    String latDeg = txtLatDeg.getText().toString();
                    String latMin = txtLatMin.getText().toString();
                    String latSec = txtLatSec.getText().toString();

                    if (StringEx.isEmpty(latDeg) || StringEx.isEmpty(latSec) || StringEx.isEmpty(latMin)) {
                        posButton.setEnabled(false);
                        calculating = false;
                        return;
                    }

                    String lonDeg = txtLonDeg.getText().toString();
                    String lonMin = txtLonMin.getText().toString();
                    String lonSec = txtLonSec.getText().toString();

                    if (StringEx.isEmpty(lonDeg) || StringEx.isEmpty(lonSec) || StringEx.isEmpty(lonMin)) {
                        posButton.setEnabled(false);
                        calculating = false;
                        return;
                    }

                    DMS lat = new DMS(Integer.parseInt(latDeg), Integer.parseInt(latMin), Double.parseDouble(latSec));
                    DMS lon = new DMS(Integer.parseInt(lonDeg), Integer.parseInt(lonMin), Double.parseDouble(lonSec));

                    this.lat = TtUtils.Math.round(lat.toDecimal() * (latDeg.contains("-") ? -1 : 1), 6);
                    this.lon = TtUtils.Math.round(lon.toDecimal() * (lonDeg.contains("-") ? -1 : 1), 6);

                    txtLat.setText(StringEx.toString(this.lat));
                    txtLon.setText(StringEx.toString(this.lon));
                } else {
                    String lat = txtLat.getText().toString();
                    String lon = txtLon.getText().toString();

                    if (StringEx.isEmpty(lat) || StringEx.isEmpty(lon)) {
                        posButton.setEnabled(false);
                        calculating = false;
                        return;
                    }

                    this.lat = Double.parseDouble(lat);
                    this.lon = Double.parseDouble(lon);

                    DMS dmsLat = new DMS(this.lat);
                    DMS dmsLon = new DMS(this.lon);

                    txtLatDeg.setText(StringEx.toString(dmsLat.getDegrees() * (lat.contains("-") ? -1 : 1)));
                    txtLatMin.setText(StringEx.toString(dmsLat.getMinutes()));
                    txtLatSec.setText(StringEx.toStringRound(dmsLat.getSeconds(), 4));

                    txtLonDeg.setText(StringEx.toString(dmsLon.getDegrees() * (lon.contains("-") ? -1 : 1)));
                    txtLonMin.setText(StringEx.toString(dmsLon.getMinutes()));
                    txtLonSec.setText(StringEx.toStringRound(dmsLon.getSeconds(), 4));
                }

                posButton.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                calculating = false;
            }

            calculating = false;
        }
    }

    
    public interface OnEditedListener {
        void onEdited(String cn, Double lat, Double lon);
    }
}
