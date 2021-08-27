package com.usda.fmsc.twotrails.objects.media;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.objects.TtObject;
import com.usda.fmsc.twotrails.units.MediaType;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.nio.file.Paths;

public abstract class TtMedia extends TtObject {
    private String _Name;
    private String _FileName;
    private String _Comment;
    private DateTime _TimeCreated;
    private String _PointCN;
    private boolean _IsExternal;


    public TtMedia() {}

    public TtMedia(Parcel source) {
        super(source);

        _Name = source.readString();
        _FileName = source.readString();
        _Comment = source.readString();
        _TimeCreated = (DateTime)source.readSerializable();
        _PointCN = source.readString();
        _IsExternal = ParcelTools.readBool(source);
    }

    public TtMedia(String name, String filename, String comment, DateTime timeCreated, String PointCN, boolean IsExternal) {
        this._Name = name;
        this._FileName = filename;
        this._Comment = comment;
        this._TimeCreated = timeCreated;
        this._PointCN = PointCN;
        this._IsExternal = IsExternal;
    }

    public TtMedia(TtMedia media) {
        super(media);
        this._Name = media._Name;
        this._FileName = media._FileName;
        this._Comment = media._Comment;
        this._TimeCreated = media._TimeCreated;
        this._PointCN = media._PointCN;
        this._IsExternal = media._IsExternal;
    }

    public abstract MediaType getMediaType();

    public String getName() {
        return _Name;
    }

    public void setName(String name) {
        _Name = name;
    }


    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String fileName) {
        _FileName = fileName;
    }


    public boolean externalFileExists(TwoTrailsApp app) {
        return _IsExternal && _FileName != null && FileUtils.fileExists(app, app.getMediaFileByFileName(_FileName));
    }

    public Uri getFilePath(TwoTrailsApp app) {
        if (_FileName == null) throw new RuntimeException("No External File");
        return app.getMediaFileByFileName(_FileName);
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


    public boolean isExternal() { return _IsExternal;}

    public  void setIsExternal(boolean isExternal) {
        _IsExternal = isExternal;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(_Name));
        dest.writeString(_FileName);
        dest.writeString(StringEx.getValueOrEmpty(_Comment));
        dest.writeSerializable(_TimeCreated);
        dest.writeString(StringEx.getValueOrEmpty(_PointCN));
        ParcelTools.writeBool(dest, _IsExternal);
    }
}
