package com.usda.fmsc.twotrails.gps;


import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import com.usda.fmsc.android.utilities.ParcelTools;
import com.usda.fmsc.geospatial.EastWest;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.GnssType;
import com.usda.fmsc.geospatial.NorthSouth;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.Satellite;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.twotrails.objects.TtObject;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;

public class TtNmeaBurst extends TtObject implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new TtNmeaBurst(source);
        }

        @Override
        public TtNmeaBurst[] newArray(int size) {
            return new TtNmeaBurst[size];
        }
    };

    private String pointCN;
    private boolean used;
    private DateTime timeCreated;

    private Double _X, _Y;
    private Integer _Zone;

    //region NmeaBurst Values
    private GeoPosition position;

    //rmc
    private DateTime fixTime;
    private double groundSpeed; //groud speed in knots
    private double trackAngle;  //in degrees, true
    private double magVar;
    private EastWest magVarDir;

    //gsa
    private GSASentence.Mode mode;
    private GSASentence.Fix fix;
    private ArrayList<Integer> satsUsed;
    private double pdop, hdop, vdop;

    //gga
    private GGASentence.GpsFixType fixQuality;
    private int trackedSatellites;
    private double horizDilution;
    private double geoidHeight;
    private UomElevation geoUom;

    //gsv
    private int numberOfSatellitesInView;
    private ArrayList<Satellite> satellitesInView;
    //endregion

    @SuppressWarnings("unchecked")
    private TtNmeaBurst(Parcel source) {
        super(source);

        Integer val = null;

        this.timeCreated = (DateTime) source.readSerializable();
        this.pointCN = source.readString();
        this.used = source.readInt() > 0;

        this.position = (GeoPosition) source.readSerializable();

        this.fixTime = (DateTime) source.readSerializable();
        this.groundSpeed = source.readDouble();
        this.trackAngle = source.readDouble();
        this.magVar = source.readDouble();
        this.magVarDir = (val = ParcelTools.readNInt(source)) != null ?  EastWest.parse(val) : null;

        this.mode = (val = ParcelTools.readNInt(source)) != null ?  GSASentence.Mode.parse(val) : null;
        this.fix = (val = ParcelTools.readNInt(source)) != null ?  GSASentence.Fix.parse(val) : null;
        this.satsUsed = source.readArrayList(Integer.class.getClassLoader());
        this.pdop = source.readDouble();
        this.hdop = source.readDouble();
        this.vdop = source.readDouble();

        this.fixQuality = GGASentence.GpsFixType.parse(source.readInt());
        this.trackedSatellites = source.readInt();
        this.horizDilution = source.readDouble();
        this.geoidHeight = source.readDouble();
        this.geoUom = (val = ParcelTools.readNInt(source)) != null ?  UomElevation.parse(val) : null;

        this.numberOfSatellitesInView = source.readInt();

        setSatellitesInViewFromString(source.readString());
    }

    private TtNmeaBurst(String cn, DateTime timeCreated, String pointCN, boolean used,
                       GeoPosition position, DateTime fixTime, double groundSpeed, double trackAngle,
                       double magVar, EastWest magVarDir, GSASentence.Mode mode, GSASentence.Fix fix,
                       ArrayList<Integer> satsUsed, double pdop, double hdop, double vdop, GGASentence.GpsFixType fixQuality,
                       int trackedSatellites, double horizDilution, double geoidHeight, UomElevation geoUom,
                       int numberOfSatellitesInView) {
        setCN(cn);
        this.timeCreated = timeCreated;
        this.pointCN = pointCN;
        this.used = used;

        this.position = position;

        this.fixTime = fixTime;
        this.groundSpeed = groundSpeed;
        this.trackAngle = trackAngle;
        this.magVar = magVar;
        this.magVarDir = magVarDir;

        this.mode = mode;
        this.fix = fix;
        this.satsUsed = satsUsed;
        this.pdop = pdop;
        this.hdop = hdop;
        this.vdop = vdop;

        this.fixQuality = fixQuality;
        this.trackedSatellites = trackedSatellites;
        this.horizDilution = horizDilution;
        this.geoidHeight = geoidHeight;
        this.geoUom = geoUom;

        this.numberOfSatellitesInView = numberOfSatellitesInView;
    }

    public TtNmeaBurst(String cn, DateTime timeCreated, String pointCN, boolean used,
        GeoPosition position, DateTime fixTime, double groundSpeed, double trackAngle,
        double magVar, EastWest magVarDir, GSASentence.Mode mode, GSASentence.Fix fix,
        ArrayList<Integer> satsUsed, double pdop, double hdop, double vdop, GGASentence.GpsFixType fixQuality,
        int trackedSatellites, double horizDilution, double geoidHeight, UomElevation geoUom,
        int numberOfSatellitesInView, ArrayList<Satellite> satellitesInView) {

        this(cn, timeCreated, pointCN, used, position, fixTime, groundSpeed, trackAngle, magVar, magVarDir, mode,
                fix, satsUsed, pdop, hdop, vdop, fixQuality, trackedSatellites, horizDilution, geoidHeight, geoUom,
                numberOfSatellitesInView);

        this.satellitesInView = satellitesInView;
    }

    public TtNmeaBurst(String cn, DateTime timeCreated, String pointCN, boolean used,
                       GeoPosition position, DateTime fixTime, double groundSpeed, double trackAngle,
                       double magVar, EastWest magVarDir, GSASentence.Mode mode, GSASentence.Fix fix,
                       ArrayList<Integer> satsUsed, double pdop, double hdop, double vdop, GGASentence.GpsFixType fixQuality,
                       int trackedSatellites, double horizDilution, double geoidHeight, UomElevation geoUom,
                       int numberOfSatellitesInView, String satellitesInView) {

        this(cn, timeCreated, pointCN, used, position, fixTime, groundSpeed, trackAngle, magVar, magVarDir, mode,
                fix, satsUsed, pdop, hdop, vdop, fixQuality, trackedSatellites, horizDilution, geoidHeight, geoUom,
                numberOfSatellitesInView);

        setSatellitesInViewFromString(satellitesInView);
    }

    public static TtNmeaBurst create(String pointCN, boolean used, INmeaBurst burst) {
        return new TtNmeaBurst(java.util.UUID.randomUUID().toString(),
                DateTime.now(),
                pointCN,
                used,
                burst.getPosition(),
                burst.getFixTime(),
                burst.getGroundSpeed(),
                burst.getTrackAngle(),
                burst.getMagVar(),
                burst.getMagVarDir(),
                burst.getMode(),
                burst.getFix(),
                burst.getUsedSatelliteIDs(),
                burst.getPDOP(),
                burst.getHDOP(),
                burst.getVDOP(),
                burst.getFixQuality(),
                burst.getTrackedSatellitesCount(),
                burst.getHorizDilution(),
                burst.getGeoidHeight(),
                burst.getGeoUom(),
                burst.getSatellitesInViewCount(),
                burst.getSatellitesInView());
    }




    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        try {
            dest.writeSerializable(timeCreated);
            dest.writeString(pointCN);
            dest.writeInt(used ? 1 : 0);

            dest.writeSerializable(position);

            dest.writeSerializable(fixTime);
            dest.writeDouble(groundSpeed);
            dest.writeDouble(trackAngle);
            dest.writeDouble(magVar);
            ParcelTools.writeNInt(dest, magVarDir != null ? magVarDir.getValue() : null);

            ParcelTools.writeNInt(dest, mode != null ? mode.getValue() : null);
            ParcelTools.writeNInt(dest, fix != null ? fix.getValue() : null);
            dest.writeList(satsUsed);
            dest.writeDouble(pdop);
            dest.writeDouble(hdop);
            dest.writeDouble(vdop);

            dest.writeInt(fixQuality.getValue());
            dest.writeInt(trackedSatellites);
            dest.writeDouble(horizDilution);
            dest.writeDouble(geoidHeight);
            ParcelTools.writeNInt(dest, geoUom != null ? geoUom.getValue() : null);

            dest.writeInt(numberOfSatellitesInView);

            dest.writeString(getSatellitesInViewString());
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "TtNmeaBurst:writeToParcel");
            throw e;
        }
    }

    public String getPointCN() {
        return pointCN;
    }


    public Boolean isUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }


    public DateTime getTimeCreated() {
        return timeCreated;
    }


    public double getX(int zone) {
        if (_Zone == null || _X == null || _Zone != zone) {
            UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(
                    position.getLatitude().toSignedDecimal(),
                    position.getLongitude().toSignedDecimal(),
                    zone);

            _X = coords.getX();
            _Y = coords.getY();
            _Zone = coords.getZone();
        }

        return _X;
    }

    public double getY(int zone) {
        if (_Zone == null || _Y == null || _Zone != zone) {
            UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(getLatitude(), getLongitude(), zone);

            _X = coords.getX();
            _Y = coords.getY();
            _Zone = coords.getZone();
        }

        return _Y;
    }



    public DateTime getFixTime() {
        return fixTime;
    }

    public double getMagVar() {
        return magVar;
    }

    public EastWest getMagVarDir() {
        return magVarDir;
    }

    public double getTrackAngle() {
        return trackAngle;
    }

    public double getGroundSpeed() {
        return groundSpeed;
    }


    public GeoPosition getPosition() {
        return position;
    }

    public boolean hasPosition() {
        return position != null;
    }

    public double getLatitude() {
        return position.getLatitude().toDecimal();
    }

    public NorthSouth getLatDir() {
        return position.getLatitude().getHemisphere();
    }

    public double getLongitude() {
        return position.getLongitude().toDecimal();
    }

    public EastWest getLonDir() {
        return position.getLongitude().getHemisphere();
    }


    public double getElevation() {
        return position.hasElevation() ? position.getElevation() : 0;
    }

    public UomElevation getUomElevation() {
        return position.getUomElevation();
    }


    public UTMCoords getTrueUTM() {
        return UTMTools.convertLatLonToUTM(position);
    }

    public UTMCoords getUTM(int utmZone) {
        return UTMTools.convertLatLonToUTM(position, utmZone);
    }


    public double getHorizDilution() {
        return horizDilution;
    }

    public double getGeoidHeight() {
        return geoidHeight;
    }

    public UomElevation getGeoUom() {
        return geoUom;
    }

    public GGASentence.GpsFixType getFixQuality() {
        return fixQuality;
    }

    public int getTrackedSatellitesCount() {
        return trackedSatellites;
    }

    public int getSatellitesInViewCount() {
        return numberOfSatellitesInView;
    }

    public List<Integer> getUsedSatelliteIDs() {
        return satsUsed;
    }

    public String getUsedSatelliteIDsString() {

        if (satsUsed.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (int prn : satsUsed) {
                sb.append(prn);
                sb.append("_");
            }

            return sb.toString();
        }

        return StringEx.Empty;
    }

    public int getUsedSatellitesCount() {
        return satsUsed.size();
    }

    public ArrayList<Satellite> getSatellitesInView() { return satellitesInView; }

    public String getSatellitesInViewString() {
        if (satellitesInView.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (Satellite sat : satellitesInView) {
                sb.append(StringEx.format("%d;%f;%f;%f;%d_", sat.getNmeaID(), sat.getElevation(), sat.getAzimuth(), sat.getSRN(), sat.getGnssType().getValue()));
            }

            return sb.toString();
        }

        return StringEx.Empty;
    }

    private void setSatellitesInViewFromString(String satsInView) {
        if (!StringEx.isEmpty(satsInView)) {
            String[] sats = satsInView.split("_");

            satellitesInView = new ArrayList<>();

            for (String sat : sats) {
                if (!StringEx.isEmpty(sat)) {
                    String[] tokens = sat.split(";");
                    if (tokens.length > 4) {
                        Integer nmeaID = ParseEx.parseInteger(tokens[0]);

                        if (nmeaID != null) {
                            satellitesInView.add(new Satellite(
                                    nmeaID,
                                    ParseEx.parseFloat(tokens[1]),
                                    ParseEx.parseFloat(tokens[2]),
                                    ParseEx.parseFloat(tokens[3])));
                        }
                    }
                }
            }
        }
    }


    public GSASentence.Fix getFix() {
        return fix;
    }

    public GSASentence.Mode getMode() {
        return mode;
    }

    public double getHDOP() {
        return hdop;
    }

    public double getPDOP() {
        return pdop;
    }

    public double getVDOP() {
        return vdop;
    }

    public boolean hasDifferential() {
        return fixQuality == GGASentence.GpsFixType.DGPS;
    }
}
