package com.usda.fmsc.twotrails.objects.media;

import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.FileUtils;

import org.joda.time.DateTime;

public class TtVideo extends TtMedia {
    private PositionTimeline _Timeline;
    private String _PositionTimelineFile;

    public TtVideo() {

    }

    public TtVideo(String Name, String FilePath, String Comment, DateTime TimeCreated, String PointCN, PositionTimeline _Timeline, String _PositionTimelineFile) {
        super(Name, FilePath, Comment, TimeCreated, PointCN);
        this._Timeline = _Timeline;
        this._PositionTimelineFile = _PositionTimelineFile;
    }

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
