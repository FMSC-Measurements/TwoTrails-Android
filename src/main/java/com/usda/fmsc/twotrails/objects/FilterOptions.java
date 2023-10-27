package com.usda.fmsc.twotrails.objects;

import com.usda.fmsc.geospatial.gnss.codes.GnssFix;
import com.usda.fmsc.geospatial.gnss.codes.GnssFixQuality;

public class FilterOptions {
    public com.usda.fmsc.twotrails.units.DopType DopType;
    public int DopValue;
    public GnssFixQuality FixType;
    public GnssFix Fix;
    public boolean FilterFix;

    public FilterOptions() {
        this.DopType = com.usda.fmsc.twotrails.units.DopType.HDOP;
        DopValue = 20;
        this.FixType = GnssFixQuality.GPS;
        this.Fix = GnssFix._3D;
        this.FilterFix = true;
    }
}
