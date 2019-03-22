package com.usda.fmsc.twotrails;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.RecentProject;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.twotrails.units.Datum;
import com.usda.fmsc.twotrails.units.DeclinationType;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.units.MapTracking;
import com.usda.fmsc.twotrails.units.MapType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.StringEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Settings {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }


    public SharedPreferences getPrefs() {
        return  prefs;
    }

    public  SharedPreferences.Editor getEditor() {
        return editor;
    }


    protected int getInt(String settingName)
    {
        return getInt(settingName, -1);
    }

    protected int getInt(String settingName, int defaultValue) {
        return prefs.getInt(settingName, defaultValue);
    }

    protected void setInt(String settingName, int value) {
        editor.putInt(settingName, value).apply();
    }


    protected long getLong(String settingName)
    {
        return getLong(settingName, -1);
    }

    protected long getLong(String settingName, long defaultValue) {
        return prefs.getLong(settingName, defaultValue);
    }

    protected void setLong(String settingName, long value) {
        editor.putLong(settingName, value).apply();
    }


    protected String getString(String settingName) {
        return getString(settingName, "");
    }

    protected String getString(String settingName, String defaultValue) {
        return prefs.getString(settingName, defaultValue);
    }

    protected void setString(String settingName, String value) {
        editor.putString(settingName, value).apply();
    }


    protected double getDouble(String settingName) {
        return getDouble(settingName, 0);
    }

    protected double getDouble(String settingName, double defaultValue) {
        return  Double.longBitsToDouble(prefs.getLong(settingName, Double.doubleToRawLongBits(defaultValue)));
    }

    protected void setDouble(String settingName, double value) {
        editor.putLong(settingName, Double.doubleToRawLongBits(value)).apply();
    }


    protected float getFloat(String settingName)
    {
        return getFloat(settingName, 0);
    }

    protected float getFloat(String settingName, float defaultValue) {
        return prefs.getFloat(settingName, defaultValue);
    }

    protected void setFloat(String settingName, float value) {
        editor.putFloat(settingName, value).apply();
    }


    protected boolean getBool(String settingName) {
        return getBool(settingName, false);
    }

    protected boolean getBool(String settingName, boolean defaultValue) {
        return prefs.getBoolean(settingName, defaultValue);
    }

    protected void setBool(String settingName, boolean value) {
        editor.putBoolean(settingName, value).apply();
    }



    public class MetaDataSetting extends Global.Settings.PreferenceHelper {
        private final String META_NAME = "Name";
        private final String META_ZONE = "Zone";
        private final String META_DATUM = "Datum";
        private final String META_DISTANCE = "Distance";
        private final String META_ELEVATION = "Elevation";
        private final String META_SLOPE = "Slope";
        private final String META_DECTYPE = "Declination";
        private final String META_MAGDEC = "MagneticDeclination";
        private final String META_RECEIVER = "Receiver";
        private final String META_LASER = "RangeFinder";
        private final String META_COMPASS = "Compass";
        private final String META_CREW = "Crew";


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

        public TtMetadata getDefaultmetaData() {
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
}