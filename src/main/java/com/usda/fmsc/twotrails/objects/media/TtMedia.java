package com.usda.fmsc.twotrails.objects.media;

import android.os.Parcel;

import com.usda.fmsc.twotrails.objects.TtObject;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

public abstract class TtMedia extends TtObject {
    private String _Name;
    private String _FilePath;
    private String _Comment;
    private DateTime _TimeCreated;
    private String _PointCN;


    public TtMedia() {}

    public TtMedia(Parcel source) {
        super(source);

        _Name = source.readString();
        _FilePath = source.readString();
        _Comment = source.readString();
        _TimeCreated = (DateTime)source.readSerializable();
        _PointCN = source.readString();
    }

    public TtMedia(String Name, String FilePath, String Comment, DateTime TimeCreated, String PointCN) {
        this._Name = Name;
        this._FilePath = FilePath;
        this._Comment = Comment;
        this._TimeCreated = TimeCreated;
        this._PointCN = PointCN;
    }

    public TtMedia(TtMedia media) {
        this._Name = media._Name;
        this._FilePath = media._FilePath;
        this._Comment = media._Comment;
        this._TimeCreated = media._TimeCreated;
        this._PointCN = media._PointCN;
    }

    public abstract MediaType getMediaType();

    public String getName() {
        return _Name;
    }

    public void setName(String name) {
        _Name = name;
    }


    public String getFilePath() {
        return _FilePath;
    }

    public void setFilePath(String filePath) {
        _FilePath = filePath;
    }

    public boolean isFileValid() {
        return FileUtils.fileExists(_FilePath);
    }


    public String getComment() {
        return _Comment;
    }

    public void setComment(String comment) {
        _Comment = comment;
    }


    public DateTime getTimeCreated() {
        return _TimeCreated;
    }

    public void setTimeCreated(DateTime timeCreated) {
        _TimeCreated = timeCreated;
    }


    public String getPointCN() {
        return _PointCN;
    }

    public void setPointCN(String _PointCN) {
        this._PointCN = _PointCN;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(_Name));
        dest.writeString(StringEx.getValueOrEmpty(_FilePath));
        dest.writeString(StringEx.getValueOrEmpty(_Comment));
        dest.writeSerializable(_TimeCreated);
        dest.writeString(StringEx.getValueOrEmpty(_PointCN));
    }
}
