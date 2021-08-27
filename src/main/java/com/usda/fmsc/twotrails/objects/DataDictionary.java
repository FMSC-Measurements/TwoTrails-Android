package com.usda.fmsc.twotrails.objects;

import androidx.annotation.NonNull;

import com.usda.fmsc.utilities.Tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class DataDictionary implements Iterable<Tuple<String, Object>> {

    private final HashMap<String, Object> _Data = new HashMap<>();
    private final String _PointCN;


    public DataDictionary(String pointCN, Collection<Tuple<String, Object>> data) {
        _PointCN = pointCN;

        if (data != null && data.size() > 0) {
            for (Tuple<String, Object> pair : data) {
                _Data.put(pair.Item1, pair.Item2);
            }
        }
    }

    public DataDictionary(DataDictionary dataDictionary) {
        _PointCN = dataDictionary.getPointCN();

        for (Tuple<String, Object> pair : dataDictionary) {
            _Data.put(pair.Item1, pair.Item2);
        }
    }

    public DataDictionary(String pointCN, DataDictionaryTemplate dataDictionaryTemplate) {
        _PointCN = pointCN;

        if (dataDictionaryTemplate != null && dataDictionaryTemplate.size() > 0) {
            for (DataDictionaryField field : dataDictionaryTemplate) {
                _Data.put(field.getCN(), field.getDefaultValue());
            }
        }
    }

    public String getPointCN() {
        return _PointCN;
    }

    public Object getValue(String cn) {
        return _Data.get(cn);
    }

    public HashMap<String, Object> getData() {
        return _Data;
    }

    public void ClearValues() {
        _Data.replaceAll((i, v) -> null);
    }

    public void update(String cn, Object value) {
        if (_Data.containsKey(cn)) {
            Object oval = _Data.get(cn);

            if (oval == null || !oval.equals(value))
                _Data.put(cn, value);
        } else {
            _Data.put(cn, value);
        }
    }

    public boolean HasField(String cn) {
        return _Data.containsKey(cn);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataDictionary) {
            DataDictionary dd = (DataDictionary) obj;

            for (String key : _Data.keySet()) {
                Object tval = _Data.get(key);
                Object oval = dd.getValue(key);

                if (tval == null ^ oval == null || (tval != null && !tval.equals(oval)))
                    return  false;
            }

            return true;
        }

        return false;
    }

    @Override
    @NonNull
    public Iterator<Tuple<String, Object>> iterator() {
        return new DataDictionaryIterator(this);
    }
}
