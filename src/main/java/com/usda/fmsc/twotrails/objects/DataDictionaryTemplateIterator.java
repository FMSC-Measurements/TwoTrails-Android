package com.usda.fmsc.twotrails.objects;

import java.util.ArrayList;
import java.util.Iterator;

public class DataDictionaryTemplateIterator implements Iterator<DataDictionaryField> {
    private int position = 0;

    private ArrayList<DataDictionaryField> fields = new ArrayList<>();

    public DataDictionaryTemplateIterator(DataDictionaryTemplate template) {
        fields.addAll(template.getFields().values());
    }

    public boolean hasNext() {
        return position < fields.size();
    }

    public DataDictionaryField next() {
        return fields.get(position++);
    }
}