package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.utilities.StringEx;

public abstract class TtObject {
    protected String CN = StringEx.Empty;

    public String getCN() {
        if(StringEx.isEmpty(CN))
            CN = java.util.UUID.randomUUID().toString();
        return CN;
    }

    public abstract String getName();
    public abstract String getType();

    public abstract TtObject clone();
}
