package com.usda.fmsc.twotrails.rangefinder;


import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;

import org.joda.time.DateTime;

public class TtRangeFinderData implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtRangeFinderData(source);
        }

        @Override
        public TtRangeFinderData[] newArray(int size) {
            return new TtRangeFinderData[size];
        }
    };


    private DateTime time;
    private String horizVectorMsg;
    private double horizDist;
    private Dist horizDistType;
    private double azimuth;
    private Slope azType;
    private double inclination;
    private Slope incType;
    private double slopeDist;
    private Dist slopeDistType;



    private TtRangeFinderData(Parcel source) {
        time = (DateTime) source.readSerializable();
        horizVectorMsg = source.readString();
        horizDist = source.readDouble();
        horizDistType = Dist.parse(source.readInt());
        azimuth = source.readDouble();
        azType = Slope.parse(source.readInt());
        inclination = source.readDouble();
        incType = Slope.parse(source.readInt());
        slopeDist = source.readDouble();
        slopeDistType = Dist.parse(source.readInt());
    }

    private TtRangeFinderData(DateTime time, String horizVectorMsg, double horizDist, Dist horizDistType, double azimuth,
                        Slope azType, double inclination, Slope incType, double slopeDist, Dist slopeDistType) {
        this.time = time;
        this.horizVectorMsg = horizVectorMsg;
        this.horizDist = horizDist;
        this.horizDistType = horizDistType;
        this.azimuth = azimuth;
        this.azType = azType;
        this.inclination = inclination;
        this.incType = incType;
        this.slopeDist = slopeDist;
        this.slopeDistType = slopeDistType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(time);
        dest.writeString(horizVectorMsg);
        dest.writeDouble(horizDist);
        dest.writeInt(horizDistType.getValue());
        dest.writeDouble(azimuth);
        dest.writeInt(azType.getValue());
        dest.writeDouble(inclination);
        dest.writeInt(incType.getValue());
        dest.writeDouble(slopeDist);
        dest.writeInt(slopeDistType.getValue());
    }


    public static TtRangeFinderData create(String horizVectorMsg, double horizDist, Dist horizDistType, double azimuth,
                                Slope azType, double inclination, Slope incType, double slopeDist, Dist slopeDistType) {
        return new TtRangeFinderData(DateTime.now(), horizVectorMsg, horizDist, horizDistType, azimuth, azType, inclination, incType, slopeDist, slopeDistType);
    }


    public DateTime getTime() {
        return time;
    }

    public String getHorizVectorMsg() {
        return horizVectorMsg;
    }

    public double getHorizDist() {
        return horizDist;
    }

    public Dist getHorizDistType() {
        return horizDistType;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public Slope getAzType() {
        return azType;
    }

    public double getInclination() {
        return inclination;
    }

    public Slope getIncType() {
        return incType;
    }

    public double getSlopeDist() {
        return slopeDist;
    }

    public Dist getSlopeDistType() {
        return slopeDistType;
    }
}
