package com.usda.fmsc.twotrails.fragments.media;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.Clipboard;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.PointsActivity;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public abstract class BaseMediaFragment extends Fragment implements PointsActivity.Listener {
    protected static final String MEDIA = "Media";

    private PointsActivity activity;
    private TtMedia _Media;

    private EditText txtName, txtCmt;
    private TextView tvFile;
    private boolean settingView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(MEDIA)) {
            _Media = bundle.getParcelable(MEDIA);

            if (activity != null && _Media != null) {
                activity.register(_Media.getCN(), this);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = onCreateViewEx(inflater, container, savedInstanceState);

        txtName = (EditText)view.findViewById(R.id.pmdFragTxtName);
        txtCmt = (EditText)view.findViewById(R.id.pmdFragTxtCmt);
        tvFile = (TextView) view.findViewById(R.id.pmdFragTvFile);

        setView();

        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    _Media.setName(s.toString());
                    getPointsActivity().updateMedia(_Media);
                }
            }
        });

        txtCmt.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    _Media.setComment(s.toString());
                    getPointsActivity().updateMedia(_Media);
                }
            }
        });

        //region Focus
        AndroidUtils.UI.removeSelectionOnUnfocus(txtName);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtCmt);
        //endregion

        tvFile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Clipboard.copyText(getContext(), _Media.getFilePath());
                Toast.makeText(getContext(), "File Path Copied", Toast.LENGTH_LONG).show();
                AndroidUtils.Device.vibrate(getContext(), 100);
                return false;
            }
        });

        return view;
    }

    public abstract View onCreateViewEx(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

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
        if (activity != null && _Media != null) {
            activity.unregister(_Media.getCN());
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activity != null && _Media != null) {
            activity.unregister(_Media.getCN());
            activity = null;
        }
    }

    @Override
    public void onLockChange(boolean locked) {
        txtCmt.setEnabled(!locked);
        txtName.setEnabled(!locked);
    }

    @Override
    public void onPointUpdated(TtPoint point) {
        //
    }

    @Override
    public void onMediaUpdated(TtMedia media) {
        _Media = media;
        setView();
    }

    private void setView() {
        settingView = true;

        txtCmt.setText(_Media.getComment());
        txtName.setText(_Media.getName());
        tvFile.setText(_Media.getFilePath());

        settingView = false;
    }

    protected TtMedia getBaseMedia() {
        return _Media;
    }

    protected PointsActivity getPointsActivity() {
        return activity;
    }
}
