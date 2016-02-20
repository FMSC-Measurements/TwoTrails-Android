package com.usda.fmsc.twotrails.gps;


import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.GGASentence;
import com.usda.fmsc.geospatial.nmea.sentences.GSASentence;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.geospatial.Units;
import com.usda.fmsc.utilities.StringEx;

public class TtNmeaBurst implements INmeaBurst, Serializable {
    private String cn;
    private String pointCN;
    private Boolean used;
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
    private Units.EastWest magVarDir;

    //gsa
    private GSASentence.Mode mode;
    private GSASentence.Fix fix;
    private List<Integer> satsUsed;
    double pdop, hdop, vdop;

    //gga
    private GGASentence.GpsFixType fixQuality;
    private int trackedSatellites;
    private double horizDilution;
    private double geoidHeight;
    private Units.UomElevation geoUom;

    //gsv
    private int numberOfSatellitesInView;
    //private List<Satellite> satellites;
    //endregion

    public TtNmeaBurst(String cn, DateTime timeCreated, String pointCN, boolean used,
                       GeoPosition position, DateTime fixTime, double groundSpeed, double trackAngle,
                       double magVar, Units.EastWest magVarDir, GSASentence.Mode mode, GSASentence.Fix fix,
                       List<Integer> satsUsed, double pdop, double hdop, double vdop, GGASentence.GpsFixType fixQuality,
                       int trackedSatellites, double horizDilution, double geoidHeight, Units.UomElevation geoUom,
                       int numberOfSatellitesInView) {//, List<Satellite> satellites) {
        this.cn = cn;
        this.timeCreated = timeCreated;
        this.pointCN = pointCN;
        this.used = used;

        this.position = position;

        this.fixTime = fixTime;
        //this.status =
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

        //this.fixTimeGGA = burst.getF
        this.fixQuality = fixQuality;
        this.trackedSatellites = trackedSatellites;
        this.horizDilution = horizDilution;
        this.geoidHeight = geoidHeight;
        this.geoUom = geoUom;

        this.numberOfSatellitesInView = numberOfSatellitesInView;
        //this.satellites = satellites;
    }
    
    public static TtNmeaBurst create(String pointCN, boolean used, NmeaBurst burst) {
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
                burst.getSatellitesInViewCount());
                //burst.getSatellitesInView()

    }


    public String getCN() {
        return cn;
    }

    public void setCN(String cn) {
        this.cn = cn;
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

    public Units.EastWest getMagVarDir() {
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

    public Units.NorthSouth getLatDir() {
        return position.getLatitude().getHemisphere();
    }

    public double getLongitude() {
        return position.getLongitude().toDecimal();
    }

    public Units.EastWest getLonDir() {
        return position.getLongitude().getHemisphere();
    }


    public double getElevation() {
        return position.getElevation();
    }

    public Units.UomElevation getUomElevation() {
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

    public Units.UomElevation getGeoUom() {
        return geoUom;
    }

    public GGASentence.GpsFixType getFixQuality() {
        return fixQuality;
    }

    public int getTrackedSatellitesCount() {
        return trackedSatellites;
    }

    //public List<Satellite> getSatellitesInView() {
    //    return satellites;
    //}

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


    public static byte[] burstsToByteArray(List<TtNmeaBurst> bursts) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(bursts);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static List<TtNmeaBurst> bytesToBursts(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (List<TtNmeaBurst>) ois.readObject();
    }
}
