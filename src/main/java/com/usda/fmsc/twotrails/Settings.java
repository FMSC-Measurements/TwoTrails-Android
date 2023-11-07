package com.usda.fmsc.twotrails;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public abstract class Settings {
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final TwoTrailsApp context;

    public Settings(TwoTrailsApp context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }

    protected TwoTrailsApp getContext() {
        return context;
    }

    public SharedPreferences getPrefs() {
        return  prefs;
    }

    protected int getInt(String settingName)
    {
        return getInt(settingName, -1);
    }

    protected int getInt(String settingName, int defaultValue) {
        try {
            return prefs.getInt(settingName, defaultValue);
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setInt(String settingName, int value) {
        editor.putInt(settingName, value).apply();
    }


    protected long getLong(String settingName)
    {
        return getLong(settingName, -1);
    }

    protected long getLong(String settingName, long defaultValue) {
        try {
            return prefs.getLong(settingName, defaultValue);
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setLong(String settingName, long value) {
        editor.putLong(settingName, value).apply();
    }


    protected String getString(String settingName) {
        return getString(settingName, "");
    }

    protected String getString(String settingName, String defaultValue) {
        try {
            return prefs.getString(settingName, defaultValue);
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setString(String settingName, String value) {
        editor.putString(settingName, value).apply();
    }


    protected double getDouble(String settingName) {
        return getDouble(settingName, 0);
    }

    protected double getDouble(String settingName, double defaultValue) {
        try {
            return Double.longBitsToDouble(prefs.getLong(settingName, Double.doubleToRawLongBits(defaultValue)));
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setDouble(String settingName, double value) {
        editor.putLong(settingName, Double.doubleToRawLongBits(value)).apply();
    }


    protected float getFloat(String settingName)
    {
        return getFloat(settingName, 0);
    }

    protected float getFloat(String settingName, float defaultValue) {
        try {
            return prefs.getFloat(settingName, defaultValue);
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setFloat(String settingName, float value) {
        editor.putFloat(settingName, value).apply();
    }


    protected boolean getBool(String settingName) {
        return getBool(settingName, false);
    }

    protected boolean getBool(String settingName, boolean defaultValue) {
        try {
            return prefs.getBoolean(settingName, defaultValue);
        } catch (IllegalStateException e) {
            getContext().getReport().writeError(e.getMessage(), settingName);
            return defaultValue;
        }
    }

    protected void setBool(String settingName, boolean value) {
        editor.putBoolean(settingName, value).apply();
    }
}