package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;

public class FilterOptions {
    public Units.DopType DopType;
    public double DopValue;
    public GGASentence.GpsFixType Fix;

    public FilterOptions() {
        this.DopType = Units.DopType.HDOP;
        DopValue = 20;
        this.Fix = GGASentence.GpsFixType.GPS;
    }
}
