package com.usda.fmsc.twotrails.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.usda.fmsc.android.adapters.FragmentStatePagerAdapterEx;
import com.usda.fmsc.twotrails.fragments.media.PictureMediaFragment;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtMedia;

public class MediaPagerAdapter extends FragmentStatePagerAdapterEx {
    private final MediaRvAdapter adapter;

    public MediaPagerAdapter(FragmentManager fm, MediaRvAdapter adapter) {
        super(fm);
        saveFragmentStates(false);

        this.adapter = adapter;
    }

    @Override
    public Fragment getItem(int position) {
        TtMedia media = adapter.getItem(position);

        switch (media.getMediaType()) {
            case Picture:
                return PictureMediaFragment.newInstance((TtImage) media);
            case Video:
                break;
        }

        return null;
    }

    @Override
    public int getCount() {
        return adapter.getItemCountEx();
    }
}
