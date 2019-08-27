package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.nmea41.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea41.sentences.GSASentence;

public class FilterOptions {
    public com.usda.fmsc.twotrails.units.DopType DopType;
    public int DopValue;
    public GGASentence.GpsFixType FixType;
    public GSASentence.Fix Fix;
    public boolean FilterFix;

    public FilterOptions() {
        this.DopType = com.usda.fmsc.twotrails.units.DopType.HDOP;
        DopValue = 20;
        this.FixType = GGASentence.GpsFixType.GPS;
        this.Fix = GSASentence.Fix._3D;
        this.FilterFix = true;
    }
}
