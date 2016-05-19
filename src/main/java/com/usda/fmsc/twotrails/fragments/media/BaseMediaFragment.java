package com.usda.fmsc.twotrails.fragments.media;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.usda.fmsc.twotrails.activities.PointsActivity;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public abstract class BaseMediaFragment extends Fragment implements PointsActivity.Listener{
    protected static final String MEDIA = "Media";

    private PointsActivity activity;
    private TtMedia _Media;

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
        return onCreateViewEx(inflater, container, savedInstanceState);
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

    protected TtMedia getBaseMedia() {
        return _Media;
    }

    @Override
    public void onLockChange(boolean locked) {

    }

    @Override
    public void onPointUpdated(TtPoint point) {
        //
    }

    @Override
    public void onMediaUpdated(TtMedia media) {

    }

    public TtMedia getMedia() {
        return _Media;
    }
}
