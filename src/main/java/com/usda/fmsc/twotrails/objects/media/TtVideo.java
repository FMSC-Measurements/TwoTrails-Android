package com.usda.fmsc.twotrails.objects.media;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public class TtVideo extends TtMedia {
    public static final Parcelable.Creator<TtVideo> CREATOR = new Parcelable.Creator<TtVideo>() {
        @Override
        public TtVideo createFromParcel(Parcel source) {
            return new TtVideo(source);
        }

        @Override
        public TtVideo[] newArray(int size) {
            return new TtVideo[size];
        }
    };

    private final PositionTimeline _Timeline;
    private final String _PositionTimelineFile;

    public TtVideo(Parcel source) {
        super(source);

        _Timeline = source.readParcelable(PositionTimeline.class.getClassLoader());
        _PositionTimelineFile = source.readString();
    }

    public TtVideo(String Name, String filename, String Comment, DateTime TimeCreated, String PointCN, boolean isExternal, PositionTimeline _Timeline, String _PositionTimelineFile) {
        super(Name, filename, Comment, TimeCreated, PointCN, isExternal);
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

    public boolean isTimeLineFileValid(Context context) {
        return false; //TODO update file api
        //return FileUtils.fileExists(_PositionTimelineFile, context);
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
