package com.usda.fmsc.twotrails.objects;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.utilities.ListUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;

public class DataDictionaryField extends TtObject {
    public static final Parcelable.Creator<DataDictionaryField> CREATOR = new Parcelable.Creator<DataDictionaryField>() {
        @Override
        public DataDictionaryField createFromParcel(Parcel source) {
            return new DataDictionaryField(source);
        }

        @Override
        public DataDictionaryField[] newArray(int size) {
            return new DataDictionaryField[size];
        }
    };

    private String _Name;
    private int _Order;
    private FieldType _FieldType;
    private int _Flags;
    private ArrayList<String> _Values = new ArrayList<>();
    private Object _DefaultValue;
    private DataType _DataType;
    private boolean _ValueRequired;


    public DataDictionaryField(String cn) {
        setCN(cn);
    }

    public DataDictionaryField(Parcel source) {
        _Name = source.readString();
        _Order = source.readInt();
        _FieldType = FieldType.parse(source.readInt());
        _Flags = source.readInt();
        source.readStringList(_Values);
        _DefaultValue = source.readValue(getClass().getClassLoader()); //dest.writeValue(_DefaultValue);
        _DataType = DataType.parse(source.readInt());
        _ValueRequired = ParcelTools.readBool(source);
    }

    //region Get/Set

    public String getName() {
        return _Name;
    }

    public void setName(String _Name) {
        this._Name = _Name;
    }

    public int getOrder() {
        return _Order;
    }

    public void setOrder(int _Order) {
        this._Order = _Order;
    }

    public FieldType getFieldType() {
        return _FieldType;
    }

    public void setFieldType(FieldType _FieldType) {
        this._FieldType = _FieldType;
    }

    public int getFlags() {
        return _Flags;
    }

    public void setFlags(int _Flags) {
        this._Flags = _Flags;
    }

    public ArrayList<String> getValues() {
        return _Values;
    }

    public void setValues(ArrayList<String> _Values) {
        this._Values = _Values;
    }

    public Object getDefaultValue() {
        return _DefaultValue;
    }

    public void setDefaultValue(Object _DefaultValue) {
        this._DefaultValue = _DefaultValue;
    }

    public DataType getDataType() {
        return _DataType;
    }

    public void setDataType(DataType _DataType) {
        this._DataType = _DataType;
    }

    public boolean isValueRequired() {
        return _ValueRequired;
    }

    public void setValueRequired(boolean _ValueRequired) {
        this._ValueRequired = _ValueRequired;
    }

    //endregion


    public Object GetDefaultValue() {
        if (_DefaultValue != null) {
            return _DefaultValue;
        } else if (_ValueRequired) {
            switch (_DataType) {
                case INTEGER: return 0;
                case DECIMAL: return 0d; //no decimal type
                case FLOAT: return 0d;
                case TEXT: return StringEx.Empty;
                case BYTE_ARRAY: return null;// new byte[0];
                case BOOLEAN: return false;
                default: throw new RuntimeException("Invalid DataType");
            }
        }

        return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(StringEx.getValueOrEmpty(_Name));
        dest.writeInt(_Order);
        dest.writeInt(_FieldType.getValue());
        dest.writeInt(_Flags);
        dest.writeStringList(_Values);
        dest.writeValue(_DefaultValue);
        dest.writeInt(_DataType.getValue());
        ParcelTools.writeBool(dest, _ValueRequired);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataDictionaryField) {
            DataDictionaryField other = (DataDictionaryField)obj;
            return
                super.equals(obj) &&
                this._Name.equals(other._Name ) &&
                this._Order == other._Order &&
                this._FieldType == other._FieldType &&
                this._Flags == other._Flags &&
                this._DataType == other._DataType &&
                this._ValueRequired == other._ValueRequired &&
                ((this._Values == null) == (other._Values == null) &&
                        (this._Values == null || (this._Values.size() == other._Values.size() && ListUtils.equalLists(this._Values, other._Values)))) &&
                ((this._DefaultValue == null) == (other._DefaultValue == null) &&
                        (this._DefaultValue == null || this._DefaultValue.equals(other._DefaultValue)));
        }

        return false;
    }
}