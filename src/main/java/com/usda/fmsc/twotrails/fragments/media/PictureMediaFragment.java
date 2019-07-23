package com.usda.fmsc.twotrails.fragments.media;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.PointMediaController;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.utilities.StringEx;

public class PictureMediaFragment extends BaseMediaFragment {
    private ImageView ivBackground;

    private TtImage _Picture;
    private BitmapManager bitmapManager;

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

        ivBackground = view.findViewById(R.id.pmdIvBackground);

        bitmapManager = ((PointMediaController)getActivity()).getBitmapManager();

        txtAz = view.findViewById(R.id.pmdFragPicTxtAzimuth);
        txtPitch = view.findViewById(R.id.pmdFragPicTxtPitch);
        txtRoll = view.findViewById(R.id.pmdFragPicTxtRoll);

        if (_Picture != null) {
            setViews();
        }

        final View parent = view.findViewById(R.id.parentLayout);

        AndroidUtils.UI.removeSelectionOnUnfocus(txtAz);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtRoll);
        AndroidUtils.UI.removeSelectionOnUnfocus(txtPitch);

        EditText[] ets = new EditText[] {
                txtAz,
                txtRoll,
                txtPitch
        };

        AndroidUtils.UI.hideKeyboardOnTouch(parent, ets);

        return view;
    }

    @Override
    public void onMediaUpdated(TtMedia media) {
        super.onMediaUpdated(media);

        _Picture = (TtImage)media;
        setViews();
    }

    private void setViews() {
        ivBackground.setImageBitmap(bitmapManager.get(_Picture.getCN()));

        txtAz.setText(StringEx.toStringRound(_Picture.getAzimuth(), 3));
        txtPitch.setText(StringEx.toStringRound(_Picture.getPitch(), 3));
        txtRoll.setText(StringEx.toStringRound(_Picture.getRoll(), 3));
    }
}
