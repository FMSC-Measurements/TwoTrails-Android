package com.usda.fmsc.twotrails.fragments.media;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.listeners.SimpleTextWatcher;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.PointsActivity;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class PictureMediaFragment extends BaseMediaFragment {
    private ImageView ivBackground;

    private TtImage _Picture;
    private BitmapManager bitmapManager;
    private boolean settingView;

    private EditText txtAz, txtPitch, txtRoll;

    public static PictureMediaFragment newInstance(TtImage picture) {
        PictureMediaFragment fragment = new PictureMediaFragment();
        Bundle args = new Bundle();
        args.putParcelable(MEDIA, picture);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getBaseMedia() != null) {
            _Picture = (TtImage)getBaseMedia();
        }
    }


    @Nullable
    @Override
    public View onCreateViewEx(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_details_media_pic, container, false);

        ivBackground = (ImageView) view.findViewById(R.id.pmdIvBackground);

        bitmapManager = ((PointsActivity)getActivity()).getBitmapManager();

        txtAz = (EditText)view.findViewById(R.id.pmdFragPicTxtAzimuth);
        txtPitch = (EditText)view.findViewById(R.id.pmdFragPicTxtPitch);
        txtRoll = (EditText)view.findViewById(R.id.pmdFragPicTxtRoll);

        if (_Picture != null) {
            setViews();
        }

        txtAz.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Picture.setAzimuth(value);
                    getPointsActivity().updateMedia(_Picture);
                }
            }
        });

        txtPitch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Picture.setPitch(value);
                    getPointsActivity().updateMedia(_Picture);
                }
            }
        });

        txtRoll.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!settingView) {
                    Float value = null;

                    if (s.length() > 0) {
                        value = ParseEx.parseFloat(s.toString());
                    }

                    _Picture.setRoll(value);
                    getPointsActivity().updateMedia(_Picture);
                }
            }
        });

        final View parent = view.findViewById(R.id.parentLayout);

        AndroidUtils.UI.removeSelectionOnUnfocus(txtAz);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtRoll);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtPitch);

        EditText txtName = (EditText)view.findViewById(R.id.pmdFragTxtName);
        EditText txtCmt = (EditText)view.findViewById(R.id.pmdFragTxtCmt);

        EditText[] ets = new EditText[] {
                txtAz,
                txtRoll,
                txtPitch,
                txtName,
                txtCmt
        };

        AndroidUtils.UI.hideKeyboardOnTouch(parent, ets);

        return view;
    }

    @Override
    public void onLockChange(boolean locked) {
        super.onLockChange(locked);

        txtAz.setEnabled(!locked);
        txtRoll.setEnabled(!locked);
        txtPitch.setEnabled(!locked);
    }

    @Override
    public void onMediaUpdated(TtMedia media) {
        super.onMediaUpdated(media);

        _Picture = (TtImage)media;
        setViews();
    }

    private void setViews() {
        settingView = true;

        if (_Picture.isFileValid()) {
            ivBackground.setImageBitmap(bitmapManager.get(_Picture.getFilePath()));
        }

        txtAz.setText(StringEx.toStringRound(_Picture.getAzimuth(), 3));
        txtPitch.setText(StringEx.toStringRound(_Picture.getPitch(), 3));
        txtRoll.setText(StringEx.toStringRound(_Picture.getRoll(), 3));

        settingView = false;
    }
}
