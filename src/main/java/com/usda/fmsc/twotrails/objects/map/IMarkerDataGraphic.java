package com.usda.fmsc.twotrails.objects.map;

import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;

import java.util.HashMap;

public interface IMarkerDataGraphic {
    HashMap<String, IMultiMapFragment.MarkerData> getMarkerData();
}
