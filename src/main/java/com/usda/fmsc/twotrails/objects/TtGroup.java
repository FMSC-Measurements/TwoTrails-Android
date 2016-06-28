package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.utilities.StringEx;

public class TtGroup extends TtObject {
    public static final Parcelable.Creator<TtGroup> CREATOR = new Parcelable.Creator<TtGroup>() {
        @Override
        public TtGroup createFromParcel(Parcel source) {
            return new TtGroup(source);
        }

        @Override
        public TtGroup[] newArray(int size) {
            return new TtGroup[size];
        }
    };

    public enum GroupType {
        General(0),
        Walk(1),
        Take5(2);

        private final int value;

        GroupType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static GroupType parse(int id) {
            GroupType[] gts = values();
            if(gts.length > id && id > -1)
                return gts[id];
            throw new IllegalArgumentException(String.format("Invalid GroupTye id: %d", id));
        }

        @Override
        public String toString() {
            switch(this) {
                case General: return "General";
                case Walk: return "Walk";
                case Take5: return "Take5";
                default: throw new IllegalArgumentException();
            }
        }
    }


    private String _Name;
    private String _Description = StringEx.Empty;
    private GroupType _GroupType = GroupType.General;

    public TtGroup() { }

    public TtGroup(Parcel source) {
        super(source);

        _Name = source.readString();
        _Description = source.readString();
        _GroupType = GroupType.parse(source.readInt());
    }

    public TtGroup(String name) {
        _Name = name;
    }

    public TtGroup(GroupType type) {
        _GroupType = type;
        _Name = String.format("%s %s", _GroupType.toString(), getCN().substring(0, 8));
    }

    public TtGroup(TtGroup ttgroup) {
        setCN(ttgroup.getCN());
        _Name = ttgroup.getName();
        _GroupType = ttgroup.getGroupType();
        _Description = ttgroup.getDescription();
    }


    //region Get/Set
    public String getName() {
        return _Name;
    }

    public void setName(String Name) {
        this._Name = Name;
    }

    public String getDescription() {
        return _Description;
    }

    public void setDescription(String Description) {
        this._Description = Description;
    }

    public GroupType getGroupType() {
        return _GroupType;
    }

    public void setGroupType(GroupType GroupType) {
        this._GroupType = GroupType;
    }

    //endregion


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(_Name));
        dest.writeString(StringEx.getValueOrEmpty(_Description));
        dest.writeInt(_GroupType.getValue());
    }

    @Override
    public String toString()
    {
        return _Name;
    }
}
