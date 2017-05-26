package com.usda.fmsc.twotrails.activities.base;

import com.usda.fmsc.android.utilities.BitmapManager;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.points.TtPoint;

public interface PointMediaController {
    void updatePoint(TtPoint point);
    void updateMedia(TtMedia media);

    void register(String cn, PointMediaListener listener);
    void unregister(String cn);

    TtMetadata getMetadata(String cn);

    BitmapManager getBitmapManager();
}