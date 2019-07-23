package com.usda.fmsc.twotrails.objects;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Iterator;

public class DataDictionaryTemplate implements Iterable<DataDictionaryField>{
    private HashMap<String, DataDictionaryField> _Fields = new HashMap<>();
    public DataDictionaryTemplate(Iterable<DataDictionaryField> dataDictionaryFields) {
        if (dataDictionaryFields != null) {
            for (DataDictionaryField field : dataDictionaryFields) {
                AddField(field);
            }
        }
    }

    public HashMap<String, DataDictionaryField> getFields() {
        return _Fields;
    }


    public boolean HasField(String cn) {
        return _Fields.containsKey(cn);
    }

    public DataDictionaryField getField(String cn) {
        if (_Fields.containsKey(cn))
            return _Fields.get(cn);
        throw new RuntimeException("Field not found");
    }

    public void AddField(DataDictionaryField field) {
        if (_Fields.containsKey(field.getCN()))
            _Fields.put(field.getCN(), field);
        else
            _Fields.put(field.getCN(), field);
    }

    public void RemoveField(String cn) {
        _Fields.remove(cn);
    }

    @NonNull
    @Override
    public Iterator<DataDictionaryField> iterator() {
        return new DataDictionaryTemplateIterator(this);
    }


    public int size() {
        return _Fields.size();
    }

//    public DataDictionary CreateDefaultDataDictionary(String pointCN) {
//        return new DataDictionary(pointCN, this.getFields());
//    }
}
