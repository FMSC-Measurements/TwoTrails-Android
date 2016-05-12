package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

public abstract class TtObject {
    private String _CN = StringEx.Empty;

    public String getCN() {
        if(StringEx.isEmpty(_CN))
            _CN = java.util.UUID.randomUUID().toString();
        return _CN;
    }

    public void setCN(String CN) {
        this._CN = CN;
    }
}
