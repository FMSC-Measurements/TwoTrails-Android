package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.utilities.StringEx;

public abstract class TtObject implements Parcelable {
    private String _CN = StringEx.Empty;

    public String getCN() {
        if(StringEx.isEmpty(_CN))
            _CN = java.util.UUID.randomUUID().toString();
        return _CN;
    }

    public void setCN(String CN) {
        this._CN = CN;
    }


    public TtObject() { }

    protected TtObject(TtObject object) {
        _CN = object._CN;
    }

    public TtObject(Parcel source) {
        _CN = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getCN());
    }
}
