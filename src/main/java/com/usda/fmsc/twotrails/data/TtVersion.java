package com.usda.fmsc.twotrails.data;

import androidx.annotation.NonNull;

import java.util.Locale;

public class TtVersion {
    public int Major;
    public int Minor;
    public int Update;
    public final int DbVersion;

    public TtVersion(int maj, int min, int up, int dbVersion) {
        Major = maj;
        Minor = min;
        Update = up;
        DbVersion = dbVersion;
    }

    public TtVersion(String versionString, int dbVersion) {
        Major = Minor = Update = 0;
        DbVersion = dbVersion;

        if (versionString == null)
            return;

        String[] vals =  versionString.split("\\.");

        if (vals.length > 0)
        {
            try {
                Major = Integer.parseInt(vals[0]);
            }
            catch (Exception ex) {
                //
            }
        }

        if (vals.length > 1)
        {
            try {
                Minor = Integer.parseInt(vals[1]);
            }
            catch (Exception ex) {
                //
            }
        }

        if (vals.length > 2)
        {
            try {
                Update = Integer.parseInt(vals[2]);
            }
            catch (Exception ex) {
                //
            }
        }
    }

    @NonNull
    @Override
    public String toString()
    {
        return String.format(Locale.getDefault(), "%d.%d.%d", Major, Minor, Update);
    }

    public int toIntVersion() {
        return Major * 10000 + Minor * 100 + Update;
    }
}
