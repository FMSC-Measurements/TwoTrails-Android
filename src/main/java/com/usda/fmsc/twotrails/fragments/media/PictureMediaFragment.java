package com.usda.fmsc.twotrails.fragments.media;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.PointsActivity;
import com.usda.fmsc.twotrails.objects.media.TtPicture;

public class PictureMediaFragment extends BaseMediaFragment {

    private ImageView ivBackground;

    private TtPicture _Picture;
    private BitmapManager bitmapManager;

    public static PictureMediaFragment newInstance(TtPicture picture) {
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
            _Picture = (TtPicture)getBaseMedia();
        }
    }


    @Nullable
    @Override
    public View onCreateViewEx(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_details_media, null);

        ivBackground = (ImageView) view.findViewById(R.id.pmdIvBackground);

        if (ivBackground != null && _Picture != null && getActivity() != null) {
            bitmapManager = ((PointsActivity)getActivity()).getBitmapManager();

            if (_Picture.isFileValid()) {
                ivBackground.setImageBitmap(bitmapManager.get(_Picture.getFilePath()));
            }
        }

        return view;
    }
}
