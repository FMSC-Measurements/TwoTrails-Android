package com.usda.fmsc.twotrails.fragments.points;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.PointsActivity;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

public abstract class BasePointFragment extends AnimationCardFragment implements PointsActivity.Listener {
    public static final String POINT = "Point";

    private static String sOnBnd = "On Boundary", sOffBnd = "Off Boundary";

    private TextView tvPID;
    private ImageButton ibBnd;
    private EditText txtCmt;
    private ImageView ivOp;
    private Drawable dOnBnd, dOffBnd;

    private boolean locked, updating;
    private View header, preFocus;
    private PointsActivity activity;
    private TtPoint _Point;
    private TtMetadata _Metadata;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(POINT)) {
            _Point = bundle.getParcelable(POINT);

            if (activity != null && _Point != null) {
                _Metadata = activity.getMetadata(_Point.getMetadataCN());
                activity.register(_Point.getCN(), this);
            }
        }
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = onCreateViewEx(inflater, container, savedInstanceState);
        header = view.findViewById(R.id.cardHeader);

        tvPID = (TextView)view.findViewById(R.id.pointHeaderTvPid);
        ibBnd = (ImageButton)view.findViewById(R.id.pointHeaderIbBnd);
        ivOp = (ImageView)view.findViewById(R.id.pointHeaderIvOp);
        txtCmt = (EditText)view.findViewById(R.id.pointTxtCmt);

        preFocus = view.findViewById(R.id.preFocusView);

        //region set Values and Listeners
        ivOp.setImageDrawable(TtUtils.UI.getTtOpDrawable(_Point.getOp(), AppUnits.IconColor.Dark, getActivity()));
        setView();

        ibBnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean onbnd = !_Point.isOnBnd();

                String strBnd = onbnd ? sOnBnd : sOffBnd;

                ibBnd.setImageDrawable(getBndDrawable(onbnd));
                ibBnd.setContentDescription(strBnd);

                _Point.setOnBnd(onbnd);
                activity.updatePoint(_Point);

                Toast.makeText(getContext(), strBnd, Toast.LENGTH_SHORT).show();
            }
        });

        ibBnd.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getContext(), _Point.isOnBnd() ? sOnBnd : sOffBnd, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        ivOp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getContext(), _Point.getOp().toString(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        txtCmt.setText(_Point.getComment() != null ? _Point.getComment() : StringEx.Empty);

        txtCmt.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!updating) {
                    _Point.setComment(s.toString());
                    activity.updatePoint(_Point);
                }
            }
        });
        //endregion

        return view;
    }

    public abstract View onCreateViewEx(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);


    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            this.activity = (PointsActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Points Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activity != null && _Point != null) {
            activity.unregister(_Point.getCN());
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activity != null && _Point != null) {
            activity.unregister(_Point.getCN());
            activity = null;
        }
    }

    @Override
    public void onLockChange(boolean locked) {
        if (header != null) {
            if (this.locked != locked) {
                this.locked = locked;

                header.setEnabled(!locked);
                tvPID.setEnabled(!locked);
                ibBnd.setEnabled(!locked);
                txtCmt.setEnabled(!locked);
                ibBnd.setAlpha(locked ? Consts.DISABLED_ALPHA : Consts.ENABLED_ALPHA);
                ivOp.setAlpha(locked ? Consts.DISABLED_ALPHA : Consts.ENABLED_ALPHA);
            }
        }
    }

    @Override
    public void onPointUpdated(TtPoint point) {

        _Point = point;
        _Metadata = activity.getMetadata(_Point.getMetadataCN());
        setView();

    }

    @Override
    public void onMediaUpdated(TtMedia media) {
        //
    }

    private void setView() {
        updating = true;

        tvPID.setText(StringEx.toString(_Point.getPID()));
        txtCmt.setText(_Point.getComment());
        ibBnd.setImageDrawable(getBndDrawable(_Point.isOnBnd()));

        preFocus.requestFocus();

        updating = false;
    }

    private Drawable getBndDrawable(boolean onBnd) {
        if (onBnd) {
            if (dOnBnd == null)
                dOnBnd = AndroidUtils.UI.getDrawable(getContext(), R.drawable.ic_onbnd_dark);
            return dOnBnd;
        } else {
            if (dOffBnd == null)
                dOffBnd = AndroidUtils.UI.getDrawable(getContext(), R.drawable.ic_offbnd_dark);
            return dOffBnd;
        }
    }

    protected TtPoint getBasePoint() {
        return _Point;
    }

    protected PointsActivity getPointsActivity() {
        return activity;
    }

    protected TtMetadata getMetadata() {
        return _Metadata;
    }

}
