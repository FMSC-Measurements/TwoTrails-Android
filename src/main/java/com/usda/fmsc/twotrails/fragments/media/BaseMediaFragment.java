package com.usda.fmsc.twotrails.fragments.media;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.Clipboard;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.activities.base.PointMediaListener;
import com.usda.fmsc.twotrails.fragments.TtBaseFragment;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public abstract class BaseMediaFragment extends TtBaseFragment implements PointMediaListener {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = onCreateViewEx(inflater, container, savedInstanceState);

        txtName = view.findViewById(R.id.pmdFragTxtName);
        txtCmt = view.findViewById(R.id.pmdFragTxtCmt);
        tvFile = view.findViewById(R.id.pmdFragTvFile);

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

        tvFile.setOnLongClickListener(v -> {
            Clipboard.copyText(getContext(), _Media.getFileName().toString());
            Toast.makeText(getContext(), "File Path Copied", Toast.LENGTH_LONG).show();
            AndroidUtils.Device.vibrate(getContext(), 100);
            return false;
        });

        return view;
    }

    public abstract View onCreateViewEx(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @Override
    public void onAttach(@NonNull Context context) {
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
        tvFile.setText(_Media.getFileName().toString());

        settingView = false;
    }

    protected TtMedia getBaseMedia() {
        return _Media;
    }

    public PointMediaController getPointMediaController() {
        return controller;
    }
}
