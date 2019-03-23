package com.usda.fmsc.twotrails;

import android.content.Context;

import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.units.Datum;
import com.usda.fmsc.twotrails.units.DeclinationType;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.StringEx;

public class MetadataSettings extends Settings {
    private static final String META_NAME = "Name";
    private static final String META_ZONE = "Zone";
    private static final String META_DATUM = "Datum";
    private static final String META_DISTANCE = "Distance";
    private static final String META_ELEVATION = "Elevation";
    private static final String META_SLOPE = "Slope";
    private static final String META_DECTYPE = "Declination";
    private static final String META_MAGDEC = "MagneticDeclination";
    private static final String META_RECEIVER = "Receiver";
    private static final String META_LASER = "RangeFinder";
    private static final String META_COMPASS = "Compass";
    private static final String META_CREW = "Crew";


    public MetadataSettings(Context context) {
        super(context);
    }


    //region Default Meta Settings
    public String getName() {
        return getString(META_NAME);
    }

    public void setName(String value) {
        setString(META_NAME, value);
    }


    public int getZone() {
        return getInt(META_ZONE, 13);
    }

    public void setZone(int value) {
        setInt(META_ZONE, value);
    }


    public Datum getDatum() {
        return Datum.parse(getInt(META_DATUM, Datum.NAD83.getValue()));
    }

    public void setDatm(Datum value) {
        setInt(META_ZONE, value.getValue());
    }


    public Dist getDistance() {
        return Dist.parse(getInt(META_DISTANCE, Dist.FeetTenths.getValue()));
    }

    public void setDistance(Dist value) {
        setInt(META_DISTANCE, value.getValue());
    }


    public UomElevation getElevation() {
        return UomElevation.parse(getInt(META_ELEVATION, UomElevation.Feet.getValue()));
    }

    public void setElevation(UomElevation value) {
        setInt(META_ELEVATION, value.getValue());
    }


    public Slope getSlope() {
        return Slope.parse(getInt(META_SLOPE, Slope.Percent.getValue()));
    }

    public void setSlope(Slope value) {
        setInt(META_SLOPE, value.getValue());
    }


    public DeclinationType getDeclinationType() {
        return DeclinationType.parse(getInt(META_DECTYPE, DeclinationType.MagDec.getValue()));
    }

    public void setDeclinationType(DeclinationType value) {
        setInt(META_DECTYPE, value.getValue());
    }


    public double getDeclination() {
        return getDouble(META_MAGDEC, 0);
    }

    public void setDeclination(double value) {
        setDouble(META_MAGDEC, value);
    }


    public String getReceiver() {
        return getString(META_RECEIVER);
    }

    public void setReceiver(String value) {
        setString(META_RECEIVER, value);
    }


    public String getRangeFinder() {
        return getString(META_LASER);
    }

    public void setRangeFinder(String value) {
        setString(META_LASER, value);
    }


    public String getCompass() {
        return getString(META_COMPASS);
    }

    public void setCompass(String value) {
        setString(META_COMPASS, value);
    }


    public String getCrew() {
        return getString(META_CREW);
    }

    public void setCrew(String value) {
        setString(META_CREW, value);
    }
    //endregion

    public TtMetadata getDefaultMetadata() {
        TtMetadata meta = new TtMetadata();

        String tmp;

        meta.setCN(Consts.EmptyGuid);

        tmp = getName();
        meta.setName(StringEx.isEmpty(tmp) ? "Default MetaData" : tmp);
        meta.setZone(getZone());
        meta.setDatum(getDatum());
        meta.setDistance(getDistance());
        meta.setElevation(getElevation());
        meta.setSlope(getSlope());
        meta.setDecType(getDeclinationType());
        meta.setMagDec(0);
        meta.setGpsReceiver(getReceiver());
        meta.setRangeFinder(getRangeFinder());
        meta.setCompass(getCompass());
        meta.setCrew(getCrew());

        return meta;
    }

    public void setDefaultMetadata(TtMetadata metadata) {
        setName(metadata.getName());
        setZone(metadata.getZone());
        setDatm(metadata.getDatum());
        setDistance(metadata.getDistance());
        setElevation(metadata.getElevation());
        setSlope(metadata.getSlope());
        setDeclinationType(metadata.getDecType());
        setDeclination(metadata.getMagDec());
    }
}