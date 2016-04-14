package com.usda.fmsc.twotrails.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.usda.fmsc.twotrails.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class EmptyFragment extends Fragment {

    public static EmptyFragment newInstance() {
        return new EmptyFragment();
    }

    public EmptyFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_empty, container, false);
    }

}
