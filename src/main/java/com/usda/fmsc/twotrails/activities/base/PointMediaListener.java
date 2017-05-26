package com.usda.fmsc.twotrails.activities.base;

import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public interface PointMediaListener {
    void onLockChange(boolean locked);
    void onPointUpdated(TtPoint point);
    void onMediaUpdated(TtMedia media);
}
