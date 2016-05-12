package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.FileUtils;

public class TtVideo extends TtMedia {
    private PositionTimeline _Timeline;
    private String _PositionTimelineFile;


    @Override
    public MediaType getMediaType() {
        return MediaType.Video;
    }

    public boolean isTimeLineFileValid() {
        return FileUtils.fileExists(_PositionTimelineFile);
    }

    public PositionTimeline getTimeline() {
        return _Timeline;
    }

    //get video details (size, length, etc..)
}
