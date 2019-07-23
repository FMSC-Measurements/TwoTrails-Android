package com.usda.fmsc.twotrails.fragments.points;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.fragments.AnimationCardFragment;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.AppUnits;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

public abstract class BasePointFragment extends AnimationCardFragment implements PointMediaListener {
    public static final String POINT = "Point";

    private static String sOnBnd = "On Boundary", sOffBnd = "Off Boundary";

    private TextView tvPID;
    private ImageButton ibBnd;
    private EditText txtCmt;
    private ImageView ivOp;
    private Drawable dOnBnd, dOffBnd;

    private boolean locked, updating;
    private View header, preFocus;
    private PointMediaController controller;
    private TtPoint _Point;
    private TtMetadata _Metadata;

    private InputMethodManager input;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(POINT)) {
            _Point = bundle.getParcelable(POINT);

            if (controller != null && _Point != null) {
                _Metadata = controller.getMetadata(_Point.getMetadataCN());
                controller.register(_Point.getCN(), this);
            }
        }
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = onCreateViewEx(inflater, container, savedInstanceState);

        input = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        header = view.findViewById(R.id.cardHeader);

        tvPID = view.findViewById(R.id.pointHeaderTvPid);
        ibBnd = view.findViewById(R.id.pointHeaderIbBnd);
        ivOp = view.findViewById(R.id.pointHeaderIvOp);
        txtCmt = view.findViewById(R.id.pointTxtCmt);

        preFocus = view.findViewById(R.id.preFocusView);

        //region set Values and Listeners
        ivOp.setImageDrawable(TtUtils.UI.getTtOpDrawable(_Point.getOp(), AppUnits.IconColor.Dark, getActivity()));
        setView();

        if (_Point.getOp() == OpType.WayPoint) {
            ibBnd.setVisibility(View.INVISIBLE);
        } else {
            ibBnd.setOnClickListener(v -> {
                boolean onbnd = !_Point.isOnBnd();

                String strBnd = onbnd ? sOnBnd : sOffBnd;

                ibBnd.setImageDrawable(getBndDrawable(onbnd));
                ibBnd.setContentDescription(strBnd);

                _Point.setOnBnd(onbnd);
                controller.updatePoint(_Point);

                Toast.makeText(getContext(), strBnd, Toast.LENGTH_SHORT).show();
            });
        }

        ibBnd.setOnLongClickListener(view1 -> {
            Toast.makeText(getContext(), _Point.isOnBnd() ? sOnBnd : sOffBnd, Toast.LENGTH_SHORT).show();
            return true;
        });

        ivOp.setOnLongClickListener(view12 -> {
            Toast.makeText(getContext(), _Point.getOp().toString(), Toast.LENGTH_SHORT).show();
            return true;
        });

        txtCmt.setText(_Point.getComment() != null ? _Point.getComment() : StringEx.Empty);

        txtCmt.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!updating) {
                    _Point.setComment(s.toString());
                    controller.updatePoint(_Point);
                }
            }
        });
        //endregion

        return view;
    }

    public abstract View onCreateViewEx(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.controller = (PointMediaController) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement MediaController");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (controller != null && _Point != null) {
            controller.unregister(_Point.getCN());
            controller = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null && _Point != null) {
            controller.unregister(_Point.getCN());
            controller = null;
        }
    }

    @Override
    public final void onLockChange(boolean locked) {
        onBaseLockChanged(locked);

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

            if (locked) {
                input.hideSoftInputFromWindow(preFocus.getWindowToken(), 0);
            }
        }
    }

    protected abstract void onBaseLockChanged(boolean locked);

    @Override
    public final void onPointUpdated(TtPoint point) {
        _Point = point;
        _Metadata = controller.getMetadata(_Point.getMetadataCN());

        onBasePointUpdated();

        setView();
    }

    protected abstract void onBasePointUpdated();

    @Override
    public void onMediaUpdated(TtMedia media) {
        //
    }

    private void setView() {
        updating = true;

        tvPID.setText(StringEx.toString(_Point.getPID()));
        txtCmt.setText(_Point.getComment());
        ibBnd.setImageDrawable(getBndDrawable(_Point.isOnBnd()));

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

    protected PointMediaController getPointController() {
        return controller;
    }

    protected TtMetadata getMetadata() {
        return _Metadata;
    }

}
