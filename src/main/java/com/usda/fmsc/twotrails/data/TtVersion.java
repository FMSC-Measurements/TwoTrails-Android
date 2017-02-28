package com.usda.fmsc.twotrails.data;

public class TtVersion {
    public int Major;
    public int Minor;
    public int Update;

    public TtVersion(int maj, int min, int up) {
        Major = maj;
        Minor = min;
        Update = up;
    }

    public TtVersion(String versionString) {
        Major = Minor = Update = 0;

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

    @Override
    public String toString()
    {
        return String.format("%d.%d.%d", Major, Minor, Update);
    }

    public int toIntVersion() {
        return Major * 10000 + Minor * 100 + Update;
    }
}
