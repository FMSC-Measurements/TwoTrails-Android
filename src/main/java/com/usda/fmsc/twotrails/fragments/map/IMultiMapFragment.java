package com.usda.fmsc.twotrails.fragments.map;

import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.Units;

public interface IMultiMapFragment {

    void setMap(int mapType);

    void moveToLocation(float lat, float lon, boolean animate);

    Position getLatLon();

    int getZoomLevel();


    interface MultiMapListener {

        void onMapReady();
        void onMapTypeChanged(Units.MapType mapType, int mapId);
        void onMapLocationChanged();

    }
}
