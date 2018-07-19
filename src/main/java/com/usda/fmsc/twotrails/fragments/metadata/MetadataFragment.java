package com.usda.fmsc.twotrails.fragments.metadata;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.activities.MetadataActivity;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;


public class MetadataFragment extends AnimationCardFragment implements MetadataActivity.Listener{
    private static final String METADATA_CN = "MetadataCN";

    private MetadataActivity activity;

    private TextView txtName, txtZone, txtDec, txtDecType, txtDatum, txtDist, txtElev,
        txtSlope, txtGPSRec, txtRangeFinder, txtCompass, txtCrew, txtCmt;

    private View layGroup;
    private TtMetadata _Metadata;
    private String _MetaCN;


    public static MetadataFragment newInstance(String metaCN, boolean hidden) {
        MetadataFragment fragment = new MetadataFragment();
        Bundle args = new Bundle();
        args.putString(METADATA_CN, metaCN);
        args.putBoolean(HIDDEN, hidden);
        fragment.setArguments(args);
        return fragment;
    }


    public MetadataFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null) {
            _MetaCN = bundle.getString(METADATA_CN);

//            if (activity != null) {
//                _Metadata = activity.getMetadata(_MetaCN);
//                TtUtils.TtReport.writeError("Unable to get Metadata", "MetadataFragment");
//                activity.register(_MetaCN, this);
//            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metadata, container, false);

        txtName = view.findViewById(R.id.metaFragTxtName);
        txtZone = view.findViewById(R.id.metaFragTxtZone);
        txtDec = view.findViewById(R.id.metaFragTxtDec);
        txtDecType = view.findViewById(R.id.metaFragTxtDecType);
        txtDatum = view.findViewById(R.id.metaFragTxtDatum);
        txtDist = view.findViewById(R.id.metaFragTxtDist);
        txtElev = view.findViewById(R.id.metaFragTxtElev);
        txtSlope = view.findViewById(R.id.metaFragTxtSlope);
        txtGPSRec = view.findViewById(R.id.metaFragTxtGpsRec);
        txtRangeFinder = view.findViewById(R.id.metaFragTxtRangeFinder);
        txtCompass = view.findViewById(R.id.metaFragTxtCompass);
        txtCrew = view.findViewById(R.id.metaFragTxtCrew);
        txtCmt = view.findViewById(R.id.metaFragTxtCmt);

        layGroup = view.findViewById(R.id.metafragLayout);

        if (_Metadata != null) {
            onMetadataUpdated(_Metadata);
        }

        onLockChange(true);

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            this.activity = (MetadataActivity) activity;

            if (activity != null) {
                _Metadata = this.activity.getMetadata(_MetaCN);
                TtUtils.TtReport.writeError("Unable to get Metadata", "MetadataFragment");
                this.activity.register(_MetaCN, this);
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Metadata PointMediaListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activity != null && _Metadata != null) {
            activity.unregister(_Metadata.getCN());
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activity != null && _Metadata != null) {
            activity.unregister(_Metadata.getCN());
            activity = null;
        }
    }


    @Override
    public void onLockChange(boolean locked) {
        if (locked) {
            txtName.setAlpha(Consts.DISABLED_ALPHA);
            txtZone.setAlpha(Consts.DISABLED_ALPHA);
            txtDec.setAlpha(Consts.DISABLED_ALPHA);
            //txtDecType.setAlpha(Consts.DISABLED_ALPHA);
            //txtDatum.setAlpha(Consts.DISABLED_ALPHA);
            txtDist.setAlpha(Consts.DISABLED_ALPHA);
            txtElev.setAlpha(Consts.DISABLED_ALPHA);
            txtSlope.setAlpha(Consts.DISABLED_ALPHA);
            txtGPSRec.setAlpha(Consts.DISABLED_ALPHA);
            txtRangeFinder.setAlpha(Consts.DISABLED_ALPHA);
            txtCompass.setAlpha(Consts.DISABLED_ALPHA);
            txtCrew.setAlpha(Consts.DISABLED_ALPHA);
            txtCmt.setAlpha(Consts.DISABLED_ALPHA);
        } else {
            txtName.setAlpha(Consts.ENABLED_ALPHA);
            txtZone.setAlpha(Consts.ENABLED_ALPHA);
            txtDec.setAlpha(Consts.ENABLED_ALPHA);
            //txtDecType.setAlpha(Consts.ENABLED_ALPHA);
            //txtDatum.setAlpha(Consts.ENABLED_ALPHA);
            txtDist.setAlpha(Consts.ENABLED_ALPHA);
            txtElev.setAlpha(Consts.ENABLED_ALPHA);
            txtSlope.setAlpha(Consts.ENABLED_ALPHA);
            txtGPSRec.setAlpha(Consts.ENABLED_ALPHA);
            txtRangeFinder.setAlpha(Consts.ENABLED_ALPHA);
            txtCompass.setAlpha(Consts.ENABLED_ALPHA);
            txtCrew.setAlpha(Consts.ENABLED_ALPHA);
            txtCmt.setAlpha(Consts.ENABLED_ALPHA);
        }

        AndroidUtils.UI.setEnableViewGroup((ViewGroup) layGroup, !locked);

        txtDatum.setEnabled(false);
        txtDecType.setEnabled(false);
    }

    @Override
    public void onMetadataUpdated(TtMetadata metadata) {
        _Metadata = metadata;

        txtName.setText(_Metadata.getName());
        txtZone.setText(StringEx.toString(_Metadata.getZone()));
        txtDec.setText(StringEx.toString(_Metadata.getMagDec()));
        txtDecType.setText(_Metadata.getDecType().toString());
        txtDatum.setText(_Metadata.getDatum().toString());
        txtDist.setText(_Metadata.getDistance().toString());
        txtElev.setText(_Metadata.getElevation().toString());
        txtSlope.setText(_Metadata.getSlope().toString());
        txtGPSRec.setText(_Metadata.getGpsReceiver());
        txtRangeFinder.setText(_Metadata.getRangeFinder());
        txtCompass.setText(_Metadata.getCompass());
        txtCrew.setText(_Metadata.getCrew());
        txtCmt.setText(_Metadata.getComment());
    }
}
