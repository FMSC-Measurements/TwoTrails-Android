package com.usda.fmsc.twotrails.activities.old;

import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public interface IAcquireMapActivity {
    void setupMap();
    void startMap();
    void moveToMapPoint(int position);
    void addMapMarker(TtPoint point, TtMetadata metadata);
    void addMapMarker(TtPoint point, TtMetadata metadata, boolean moveToPointAfterAdd);
    void setMapMyLocationEnabled(boolean enabled);
    void setMapFollowMyPosition(boolean followPosition);
    void setMapGesturesEnabled(boolean enabled);
}
