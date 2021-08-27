package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.Tuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class DataDictionaryIterator implements Iterator<Tuple<String, Object>> {
    private int position = 0;

    private final ArrayList<Tuple<String, Object>> data = new ArrayList<>();

    public DataDictionaryIterator(DataDictionary dataDictionary) {
        for (Map.Entry<String, Object> entry : dataDictionary.getData().entrySet()) {
            data.add(new Tuple<>(entry.getKey(), entry.getValue()));
        }
    }

    public boolean hasNext() {
        return position < data.size();
    }

    public Tuple<String, Object> next() {
        return data.get(position++);
    }
}