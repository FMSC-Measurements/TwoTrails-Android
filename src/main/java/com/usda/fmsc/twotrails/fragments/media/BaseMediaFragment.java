package com.usda.fmsc.twotrails.fragments.media;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.Clipboard;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public abstract class BaseMediaFragment extends Fragment implements PointMediaListener {
    protected static final String MEDIA = "Media";

    private PointMediaController controller;
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

            if (controller != null && _Media != null) {
                controller.register(_Media.getCN(), this);
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
                    getPointMediaController().updateMedia(_Media);
                }
            }
        });

        txtCmt.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    _Media.setComment(s.toString());
                    getPointMediaController().updateMedia(_Media);
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
        if (controller != null && _Media != null) {
            controller.unregister(_Media.getCN());
            controller = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null && _Media != null) {
            controller.unregister(_Media.getCN());
            controller = null;
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

    public PointMediaController getPointMediaController() {
        return controller;
    }
}
