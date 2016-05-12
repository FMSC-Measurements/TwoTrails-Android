package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;

public class FilterOptions {
    public com.usda.fmsc.twotrails.units.DopType DopType;
    public double DopValue;
    public GGASentence.GpsFixType Fix;

    public FilterOptions() {
        this.DopType = com.usda.fmsc.twotrails.units.DopType.HDOP;
        DopValue = 20;
        this.Fix = GGASentence.GpsFixType.GPS;
    }
}
