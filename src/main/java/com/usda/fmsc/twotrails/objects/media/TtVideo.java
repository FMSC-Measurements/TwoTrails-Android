package com.usda.fmsc.twotrails.objects.media;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtVideo extends TtMedia {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtVideo(source);
        }

        @Override
        public TtVideo[] newArray(int size) {
            return new TtVideo[size];
        }
    };

    private PositionTimeline _Timeline;
    private String _PositionTimelineFile;

    public TtVideo(Parcel source) {
        super(source);

        _Timeline = source.readParcelable(PositionTimeline.class.getClassLoader());
        _PositionTimelineFile = source.readString();
    }

    public TtVideo(String Name, String FilePath, String Comment, DateTime TimeCreated, String PointCN, PositionTimeline _Timeline, String _PositionTimelineFile) {
        super(Name, FilePath, Comment, TimeCreated, PointCN);
        this._Timeline = _Timeline;
        this._PositionTimelineFile = _PositionTimelineFile;
    }

    public TtVideo(TtVideo video) {
        super(video);

        this._Timeline = new PositionTimeline(video._Timeline);
        this._PositionTimelineFile = video._PositionTimelineFile;
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


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeParcelable(_Timeline != null ? _Timeline : new PositionTimeline(), flags);
        dest.writeString(StringEx.getValueOrEmpty(_PositionTimelineFile));
    }

    //get video details (size, length, etc..)
}
