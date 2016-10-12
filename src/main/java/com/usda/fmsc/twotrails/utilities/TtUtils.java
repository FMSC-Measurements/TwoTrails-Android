package com.usda.fmsc.twotrails.utilities;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.fragments.map.IMultiMapFragment;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.FilterOptions;
import com.usda.fmsc.twotrails.objects.media.TtMedia;
import com.usda.fmsc.twotrails.objects.media.TtPanorama;
import com.usda.fmsc.twotrails.objects.media.TtPhotoSphere;
import com.usda.fmsc.twotrails.objects.media.TtImage;
import com.usda.fmsc.twotrails.objects.media.TtVideo;
import com.usda.fmsc.twotrails.objects.points.GpsPoint;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.SideShotPoint;
import com.usda.fmsc.twotrails.objects.points.Take5Point;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.points.WalkPoint;
import com.usda.fmsc.twotrails.objects.points.WayPoint;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.geospatial.utm.UTMTools;
import com.usda.fmsc.geospatial.UomElevation;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.DopType;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.units.PictureType;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.ParseEx;
import com.usda.fmsc.utilities.StringEx;


public class TtUtils {
    //region Error / Event Reporting
    public static TtReport TtReport = new TtReport();
    //endregion

    public static class Convert {

        //region Coeff
        private final static double HA_Coeff = 2.471;

        private final static double FeetToMeters_Coeff = 1200d / 3937d;
        private final static double YardsToMeters_Coeff = FeetToMeters_Coeff * 3d;
        private final static double ChainsToMeters_Coeff = FeetToMeters_Coeff * 22d;

        private final static double MetersToFeet_Coeff = 3937d / 1200d;
        private final static double YardsToFeet_Coeff = 3d;
        private final static double ChainsToFeet_Coeff = 66d;

        private final static double FeetToYards_Coeff = 1d / 3d;
        private final static double MetersToYards_Coeff = 1d / YardsToMeters_Coeff;
        private final static double ChainsToYards_Coeff = 22d;

        private final static double FeetToChains_Coeff = 1d / 66d;
        private final static double MetersToChains_Coeff = MetersToFeet_Coeff / 66d;
        private final static double YardsToChains_Coeff = 3d / 66d;

        private final static double Meters2ToAcres_Coeff = 0.00024711;
        private final static double Meters2ToHectares_Coeff = 0.0001;

        private final static double Degrees2Radians_Coeff = java.lang.Math.PI / 180.0;
        private final static double Radians2Degrees_Coeff = 180.0 / java.lang.Math.PI;
        //endregion

        public static double distance(double distance, Dist to, Dist from) throws RuntimeException {
            if(to == from)
                return distance;

            switch (to) {
                case FeetTenths:
                    return toFeetTenths(distance, from);
                case Chains:
                    return toChains(distance, from);
                case Meters:
                    return toMeters(distance, from);
                case Yards:
                    return toYards(distance, from);
            }

            throw new RuntimeException("Invalid Option");
        }

        public static double distance(double distance, UomElevation to, UomElevation from) throws RuntimeException {
            return distance(distance, elevationToDistance(to), elevationToDistance(from));
        }

        public static double distanceLatLngInMeters(double lat1, double lon1, double lat2, double lon2) {
            double r = 6371d; // Radius of the earth in km
            double dLat = Degrees2Radians_Coeff * (lat2-lat1);
            double dLon = Degrees2Radians_Coeff * (lon2-lon1);
            double a = java.lang.Math.sin(dLat/2) * java.lang.Math.sin(dLat/2) +
                    java.lang.Math.cos(Degrees2Radians_Coeff * (lat1)) *
                            java.lang.Math.cos(Degrees2Radians_Coeff * (lat2)) *
                            java.lang.Math.sin(dLon / 2) * java.lang.Math.sin(dLon/2);


            double c = 2 * java.lang.Math.atan2(java.lang.Math.sqrt(a), java.lang.Math.sqrt(1d - a));
            double dist = r * c; // Distance in km

            return dist * 1000d;
        }

        public static Dist elevationToDistance(UomElevation elevation) throws RuntimeException {
            switch (elevation) {
                case Feet: return Dist.FeetTenths;
                case Meters: return Dist.Meters;
            }

            throw new RuntimeException("Invalid Option");
        }

        public static UomElevation distanceToElevation(Dist distance) throws RuntimeException {
            switch (distance) {
                case Meters: return UomElevation.Meters;
                case FeetTenths:
            }

            throw new RuntimeException("Invalid Option");
        }

        public static double toFeetTenths(double distance, Dist dist) throws RuntimeException {
            switch (dist) {
                case FeetTenths:
                    return distance;
                case Meters:
                    return distance * MetersToFeet_Coeff;
                case Yards:
                    return distance * YardsToFeet_Coeff;
                case Chains:
                    return distance * ChainsToFeet_Coeff;
            }

            throw new RuntimeException("Invalid Option");
        }

        public static double toYards(double distance, Dist dist) throws RuntimeException {
            switch (dist) {
                case FeetTenths:
                    return distance * FeetToYards_Coeff;
                case Meters:
                    return distance * MetersToYards_Coeff;
                case Yards:
                    return distance;
                case Chains:
                    return distance * ChainsToYards_Coeff;
            }

            throw new RuntimeException("Invalid Option");
        }

        public static double toMeters(double distance, Dist dist) throws RuntimeException {
            switch (dist) {
                case FeetTenths:
                    return distance * FeetToMeters_Coeff;
                case Meters:
                    return distance;
                case Yards:
                    return distance * YardsToMeters_Coeff;
                case Chains:
                    return distance * ChainsToMeters_Coeff;
            }

            throw new RuntimeException("Invalid Option");
        }

        public static double toChains(double distance, Dist dist) throws RuntimeException {
            switch (dist) {
                case FeetTenths:
                    return distance * FeetToChains_Coeff;
                case Meters:
                    return distance * MetersToChains_Coeff;
                case Yards:
                    return distance * YardsToChains_Coeff;
                case Chains:
                    return distance;
            }

            throw new RuntimeException("Invalid Option");
        }


        public static double degreesToPercent(double degrees) {
            return java.lang.Math.tan(degreesToRadians(degrees)) * 100;
        }

        public static double percentToDegrees(double percent) {
            return radiansToDegrees(java.lang.Math.atan(percent / 100d));
        }

        public static double degreesToRadians(double degrees) {
            return degrees * Degrees2Radians_Coeff;
        }

        public static double radiansToDegrees(double radians) {
            return radians * Radians2Degrees_Coeff;
        }


        public static double hectaAcresToAcres(double hectaAcres) {
            return hectaAcres * HA_Coeff;
        }


        private static double getInchesFromFeetInches(double feetInches) {
            return ((feetInches - (int)(feetInches)) * 12.0);
        }

        private static double feetInchesToFeetTenths(double d) {
            return (int)(d) + (getInchesFromFeetInches(d) / 12.0);
        }

        private static double feetTenthsToFeetInches(double d) {
            int feet =(int)(d);

            return (feet + (d - feet) / 12.0);
        }


        public static double metersSquaredToHa(double m2) {
            return m2 * Meters2ToHectares_Coeff;
        }

        public static double metersSquaredToAcres(double m2) {
            return m2 * Meters2ToAcres_Coeff;
        }

        public static double angle(Double value, Slope to, Slope from) {
            if(value == null)
                return 0;

            if (to == from)
                return value;

            if (to == Slope.Degrees)
            {
                if (from == Slope.Percent)
                {
                    return percentToDegrees(value);
                }
            }
            else
            {
                if (from == Slope.Degrees)
                {
                    return degreesToPercent(value);
                }
            }

            return value;
        }
    }

    public static class Math {
        //region Azimuth

        public static double azimuthModulo(double az) {
            double integerPart = java.lang.Math.floor(az);
            double fraction = az - integerPart;

            return (integerPart % 360) + fraction;
        }

        public static double azimuthReverse(double az) {
            return azimuthModulo(az + 180);
        }

        public static double azimuthDiff(double fwd, double back) {
            double diff;

            if (back > fwd)
                diff = back - 180;
            else
                diff = back + 180;

            diff = java.lang.Math.abs(diff - fwd);

            return diff;
        }

        public static double azimuthOfPoint(PointD p1, PointD p2) {
            return azimuthOfPoint(p1.X, p1.Y, p2.X, p2.Y);
        }

        public static double azimuthOfPoint(double fromX, double fromY, double toX, double toY) {
            double azimuth = java.lang.Math.atan2(toX - fromX, toY - fromY) * (180d / java.lang.Math.PI);

            if (azimuth < 0)
                azimuth += 360;

            return azimuth;
        }
        //endregion

        //region Distance
        public static double distance(Point p1, Point p2) {
            return distance(p1.x, p1.y, p2.x, p2.y);
        }

        public static double distance(TtPoint p1, TtPoint p2)
        {
            return distance(p1, p2, true);
        }

        public static double distance(TtPoint p1, TtPoint p2, boolean adjusted) {
            if (p1 == null || p2 == null)
                return -1;
            else
            {
                if(adjusted)
                    return distance(p1.getAdjX(), p1.getAdjY(), p2.getAdjX(), p2.getAdjY());
                else
                    return distance(p1.getUnAdjX(), p1.getUnAdjY(), p2.getUnAdjX(), p2.getUnAdjY());
            }
        }

        public static double distance(PointD p1, PointD p2) {
            return distance(p1.X, p1.Y, p2.X, p2.Y);
        }

        public static double distance(double x1, double y1, double x2, double y2) {
            return java.lang.Math.sqrt(java.lang.Math.pow((x2 - x1), 2) + java.lang.Math.pow((y2 - y1), 2));
        }

        //output in meters
        public static double distanceLL(double lat1, double lng1, double lat2, double lng2) {
            double earthRadius = 6371000; //meters
            double dLat = java.lang.Math.toRadians(lat2-lat1);
            double dLng = java.lang.Math.toRadians(lng2-lng1);
            double a = java.lang.Math.sin(dLat/2) * java.lang.Math.sin(dLat/2) +
                    java.lang.Math.cos(java.lang.Math.toRadians(lat1)) * java.lang.Math.cos(java.lang.Math.toRadians(lat2)) *
                            java.lang.Math.sin(dLng/2) * java.lang.Math.sin(dLng/2);
            return (earthRadius * 2 * java.lang.Math.atan2(java.lang.Math.sqrt(a), java.lang.Math.sqrt(1-a)));
        }
        //endregion


        public static boolean cmpa(double value1, double value2) {
            double d = java.lang.Math.abs(value1 - value2);
            return d < Consts.Minimum_Point_Accuracy;
        }

        @Nullable
        public static Double round(Double value, int decimalPlaces) {
            if (decimalPlaces < 0) throw new IllegalArgumentException();

            if (value == null)
                return null;

            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }

        @Nullable
        public static Float round(Float value, int decimalPlaces) {
            if (decimalPlaces < 0) throw new IllegalArgumentException();

            if (value == null)
                return null;

            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
            return bd.floatValue();
        }


        public static boolean linesIntersect(TtPoint p1s, TtPoint p1e, TtPoint p2s, TtPoint p2e, HashMap<String, TtMetadata> metadata) {
            return linesIntersect(p1s.getAdjX(), p1s.getAdjY(), p1e.getAdjX(), p1e.getAdjY(), p2s.getAdjX(), p2s.getAdjY(), p2e.getAdjX(), p2e.getAdjY());
        }

        public static boolean linesIntersect(double l1x1, double l1y1, double l1x2, double l1y2, double l2x1, double l2y1, double l2x2, double l2y2) {
            double denom = ((l2y2 - l2y1) * (l1x2 - l1x1)) - ((l2x2 - l2x1) * (l1y2 - l1y1));

            if (denom == 0.0f) {
                return false;
            }

            double ua = (((l2x2 - l2x1) * (l1y1 - l2y1)) - ((l2y2 - l2y1) * (l1x1 - l2x1))) / denom;
            double ub = (((l1x2 - l1x1) * (l1y1 - l2y1)) - ((l1y2 - l1y1) * (l1x1 - l2x1))) / denom;

            return ((ua >= 0.0d) && (ua <= 1.0d) && (ub >= 0.0d) && (ub <= 1.0d));
        }
    }


    //region Points

    public static TtPoint clonePoint(TtPoint point) {
        switch (point.getOp()) {
            case GPS:       return new GpsPoint(point);
            case Traverse:  return new TravPoint(point);
            case WayPoint:  return new WayPoint(point);
            case Quondam:   return new QuondamPoint(point);
            case SideShot:  return new SideShotPoint(point);
            case Walk:      return new WalkPoint(point);
            case Take5:     return new Take5Point(point);
            default: throw new RuntimeException("Op Type does not exist");
        }
    }

    public static TtPoint getPointByOpType(OpType op) {
        switch (op) {
            case GPS:       return new GpsPoint();
            case Traverse:  return new TravPoint();
            case WayPoint:  return new WayPoint();
            case Quondam:   return new QuondamPoint();
            case SideShot:  return new SideShotPoint();
            case Walk:      return new WalkPoint();
            case Take5:     return new Take5Point();
            default: throw new RuntimeException("Op Type does not exist");
        }
    }

    public static boolean allPointsAreGpsType(List<TtPoint> points) {
        for (TtPoint p : points) {
            if(!p.isGpsType())
                return false;
        }

        return true;
    }

    public static boolean allPointsAreTravType(List<TtPoint> points) {
        for (TtPoint p : points) {
            if(!p.isTravType())
                return false;
        }

        return true;
    }

    public static boolean allPointsAreQndmType(List<TtPoint> points) {
        for (TtPoint p : points) {
            if(p.getOp() != OpType.Quondam)
                return false;
        }

        return true;
    }

    public static boolean allPointsHaveSameMetadata(List<TtPoint> points) {
        if (points.size() > 1) {
            String metaCN = points.get(0).getMetadataCN();

            for (TtPoint point : points) {
                if (!point.getMetadataCN().equals(metaCN))
                    return false;
            }
        }

        return true;
    }

    public static boolean isValidPolygon(List<TtPoint> points) {
        int validPoints = 0;

        for (TtPoint point : points) {
            if (point.isOnBnd()) {
                validPoints++;

                if (validPoints > 2)
                    return true;
            }
        }

        return false;
    }


    public static List<TtPoint> filterOnly(List<TtPoint> points, OpType op) {
        List<TtPoint> filtered = new ArrayList<>();

        for(TtPoint p : points) {
            if(p.getOp() == op)
                filtered.add(p);
        }

        return filtered;
    }

    public static List<TtPoint> filterOut(List<TtPoint> points, OpType op) {
        List<TtPoint> filtered = new ArrayList<>();

        for(TtPoint p : points) {
            if(p.getOp() != op)
                filtered.add(p);
        }

        return filtered;
    }

    public static boolean pointHasValue(TtPoint point) {
        boolean hasValue = false;

        if (point != null)
        {
            if (point.isGpsType())
            {
                GpsPoint gps = ((GpsPoint)point);

                if (gps.getUnAdjX() != 0 || gps.getUnAdjY() != 0 || gps.getUnAdjZ() != 0)
                    hasValue = true;
            }
            else if (point.isTravType())
            {
                TravPoint travPoint = ((TravPoint)point);

                if ((travPoint.getFwdAz() != null || travPoint.getBkAz() != null) &&
                        travPoint.getSlopeDistance() > 0)
                    hasValue = true;
            }
            else if (point.getOp() == OpType.Quondam)
            {
                QuondamPoint q = ((QuondamPoint)point);

                if (q.hasParent())
                    hasValue = true;
            }
        }

        return hasValue;
    }

    public static boolean pointHasChanges(TtPoint point, TtPoint origPoint) {
        if (point == null ^ origPoint == null)
            return true;

        if (point != null) {
            if (point.isOnBnd() != origPoint.isOnBnd() ||
                    point.getIndex() != origPoint.getIndex() ||
                    point.getPID() != origPoint.getPID() ||
                    !point.getMetadataCN().equals(origPoint.getMetadataCN()) ||
                    !point.getGroupCN().equals(origPoint.getGroupCN()) ||
                    !point.getPolyCN().equals(origPoint.getPolyCN()) ||
                    !point.getLinkedPoints().equals(origPoint.getLinkedPoints())) {
                return true;
            }

            if (point.getComment() == null ^ origPoint.getComment() == null) {
                return true;
            } else if (point.getComment() != null && !point.getComment().equals(origPoint.getComment())) {
                return true;
            }

            if (point.isGpsType()) {
                GpsPoint gps = (GpsPoint) point;
                GpsPoint origGps = (GpsPoint) origPoint;

                if (!TtUtils.Math.cmpa(gps.getUnAdjX(), origGps.getUnAdjX()) ||
                        !TtUtils.Math.cmpa(gps.getUnAdjY(), origGps.getUnAdjY()) ||
                        !TtUtils.Math.cmpa(gps.getUnAdjZ(), origGps.getUnAdjZ())) {
                    return true;
                }

                Double acc = gps.getManualAccuracy();
                Double oAcc = origGps.getManualAccuracy();

                if ((acc == null ^ oAcc == null) ||
                    (acc != null && !TtUtils.Math.cmpa(acc, oAcc))) {
                    return true;
                }
            } else if (point.isTravType()) {
                TravPoint trav = (TravPoint) point;
                TravPoint origTrav = (TravPoint) origPoint;

                Double origValue = origTrav.getFwdAz();
                Double value = trav.getFwdAz();

                if ((value == null ^ origValue == null) ||
                    value != null && !TtUtils.Math.cmpa(value, origValue)) {
                    return true;
                }

                origValue = origTrav.getBkAz();
                value = trav.getBkAz();

                if ((value == null ^ origValue == null) ||
                        value != null && !TtUtils.Math.cmpa(value, origValue)) {
                    return true;
                }

                origValue = origTrav.getSlopeDistance();
                value = trav.getSlopeDistance();

                if (!TtUtils.Math.cmpa(value, origValue)) {
                    return true;
                }

                origValue = origTrav.getSlopeAngle();
                value = trav.getSlopeAngle();

                if (!TtUtils.Math.cmpa(value, origValue)) {
                    return true;
                }
            } else if (point.getOp() == OpType.Quondam) {
                QuondamPoint qp = (QuondamPoint)point;
                QuondamPoint origQp = (QuondamPoint)origPoint;

                if (qp.hasParent() ^ !origQp.hasParent() ||
                    (qp.hasParent() && !qp.getParentCN().equals(origQp.getParentCN()))) {
                    return true;
                }

                Double origValue = origQp.getManualAccuracy();
                Double value = qp.getManualAccuracy();

                if ((value == null ^ origValue == null) ||
                        value != null && !TtUtils.Math.cmpa(value, origValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Location getPointLocation(TtPoint point, boolean adjusted, HashMap<String, TtMetadata> metadata) {
        Location location = new Location(StringEx.Empty);

        //if (!adjusted && point.isGpsType() && ((GpsPoint)point).hasLatLon()) {
        if (point.isGpsType() && ((GpsPoint)point).hasLatLon()) { //ignore adjusted since gps types dont adjust to new values
            GpsPoint gps = ((GpsPoint)point);

            location.setLatitude(gps.getLatitude());
            location.setLongitude(gps.getLongitude());
            location.setAltitude(gps.getElevation());
        } else {
            double utmx, utmy;

            if (adjusted) {
                utmx = point.getAdjX();
                utmy = point.getAdjY();
            } else {
                utmx = point.getUnAdjX();
                utmy = point.getUnAdjY();
            }

            int zone = metadata.get(point.getMetadataCN()).getZone();

            GeoPosition position = UTMTools.convertUTMtoLatLonSignedDec(utmx, utmy, zone);

            location.setLatitude(position.getLatitudeSignedDecimal());
            location.setLongitude(position.getLongitudeSignedDecimal());
            location.setAltitude(point.getAdjZ());
        }

        return location;
    }


    public static UTMCoords forcePointZone(TtPoint point, int targetZone, int currentZone) {
        return forcePointZone(point, targetZone, currentZone, true);
    }

    public static UTMCoords forcePointZone(TtPoint point, int targetZone, int currentZone, boolean adjusted) {
        if (targetZone == currentZone) {
            if (adjusted) {

                if (point.getAdjX() == null || point.getAdjY() == null)
                    return new UTMCoords(0, 0, targetZone);
                return new UTMCoords(point.getAdjX(), point.getAdjY(), targetZone);
            } else {
                return new UTMCoords(point.getUnAdjX(), point.getUnAdjY(), targetZone);
            }
        }

        if (point.isGpsType() && ((GpsPoint)point).hasLatLon()) {
            GpsPoint gps = (GpsPoint)point;

            return UTMTools.convertLatLonSignedDecToUTM(gps.getLatitude(), gps.getLongitude(), targetZone);
        } else {
            GeoPosition position;

            if (adjusted) {
                if (point.getAdjX() == null || point.getAdjY() == null)
                    return new UTMCoords(0, 0, targetZone);

                position = UTMTools.convertUTMtoLatLonSignedDec(point.getAdjX(), point.getAdjY(), currentZone);
            } else {
                position = UTMTools.convertUTMtoLatLonSignedDec(point.getUnAdjX(), point.getUnAdjY(), currentZone);
            }

            return UTMTools.convertLatLonSignedDecToUTM(
                position.getLatitude().toSignedDecimal(),
                position.getLongitude().toSignedDecimal(),
                targetZone);
        }
    }

    public static TtPoint reCalculateGps(TtPoint point, int newZone, DataAccessLayer dal, FilterOptions options) {
        if (point.isGpsType()) {
            GpsPoint gps = new GpsPoint(point);

            double lat, lon, utmX, utmY;
            Double height = null;

            if (gps.hasLatLon() && options == null) {
                lat = gps.getLatitude();
                lon = gps.getLongitude();

                UTMCoords coords = UTMTools.convertLatLonSignedDecToUTM(lat, lon, newZone);
                utmX = coords.getX();
                utmY = coords.getY();
            } else {
                ArrayList<TtNmeaBurst> bursts = dal.getNmeaBurstsByPointCN(gps.getCN());

                if (bursts.size() > 0) {
                    ArrayList<GeoPosition> positions = new ArrayList<>();

                    //recalculate using only bursts that were used before
                    if (options == null) {
                        for (TtNmeaBurst burst : bursts) {
                            if (burst.isUsed()) {
                                positions.add(burst.getPosition());
                            }
                        }
                        //recalculate using new filter
                    } else {
                        for (TtNmeaBurst burst : bursts) {
                            if (isUsableNmeaBurst(burst, options)) {
                                positions.add(burst.getPosition());
                                burst.setUsed(true);
                            } else {
                                burst.setUsed(false);
                            }
                        }

                        dal.updateNmeaBursts(bursts);
                    }


                    Double x, y, z, q, utmXA, utmYA;
                    x = y = z = q = utmXA = utmYA = 0.0;
                    UTMCoords coords;

                    int size = positions.size();

                    ArrayList<PointD> points = new ArrayList<>(positions.size());

                    for (GeoPosition p : positions) {
                        lat = p.getLatitudeSignedDecimal();
                        lon = p.getLongitudeSignedDecimal();

                        x += java.lang.Math.cos(lat) * java.lang.Math.cos(lon);
                        y += java.lang.Math.cos(lat) * java.lang.Math.sin(lon);
                        q += java.lang.Math.sin(lat);
                        z += p.getElevation();

                        coords = UTMTools.convertLatLonSignedDecToUTM(lat, lon, newZone);

                        utmXA += coords.getX();
                        utmYA += coords.getY();

                        points.add(new PointD(coords.getX(), coords.getY()));
                    }

                    x /= size;
                    y /= size;
                    z /= size;
                    q /= size;

                    utmX = utmXA / size;
                    utmY = utmYA / size;


                    double dRMSEx = 0, dRMSEy = 0;

                    for (PointD p : points) {
                        dRMSEx += java.lang.Math.pow(p.X - x, 2);
                        dRMSEy += java.lang.Math.pow(p.Y - y, 2);
                    }

                    dRMSEx = java.lang.Math.sqrt(dRMSEx / size);
                    dRMSEy = java.lang.Math.sqrt(dRMSEy / size);

                    gps.setRMSEr(java.lang.Math.sqrt(java.lang.Math.pow(dRMSEx, 2) + java.lang.Math.pow(dRMSEy, 2)) * Consts.RMSEr95_Coeff);


                    lon = java.lang.Math.atan2(y, x);
                    Double hyp = java.lang.Math.sqrt(java.lang.Math.pow(x, 2) + java.lang.Math.pow(y, 2));
                    lat = java.lang.Math.atan2(q, hyp);

                    lat = lat * 180.0 / java.lang.Math.PI;
                    lon = lon * 180.0 / java.lang.Math.PI;

                    height = Convert.distance(z,
                            UomElevation.Meters, positions.get(0).getUomElevation());

                    gps.setLatitude(lat);
                    gps.setLongitude(lon);
                    gps.setElevation(height);
                } else {
                    return point;
                }
            }

            gps.setUnAdjX(utmX);
            gps.setUnAdjY(utmY);

            if (height != null) {
                gps.setUnAdjZ(height);
            }

            return gps;
        }

        return point;
    }


    public static GeoPosition getLatLonFromPoint(TtPoint point, boolean adjusted, TtMetadata meta) {
        Double z = adjusted ? point.getAdjZ() : point.getUnAdjZ();

        if (z == null) {
            z = 0d;
            //throw new RuntimeException("Point not adjusted");
        } else {
            z = TtUtils.Convert.distance(z, meta.getElevation(), UomElevation.Meters);
        }

        double lat, lon;

        if (point.getOp().isGpsType() && ((GpsPoint)point).hasLatLon()) {
            GpsPoint gps = ((GpsPoint)point);

            lat = gps.getLatitude();
            lon = gps.getLongitude();
        } else {
            Double x = adjusted ? point.getAdjX() : point.getUnAdjX();
            Double y = adjusted ? point.getAdjY() : point.getUnAdjY();

            if (x == null)
                x = 0d;
            if (y == null)
                y = 0d;

            GeoPosition position = UTMTools.convertUTMtoLatLonSignedDec(x, y, meta.getZone());
            lat = position.getLatitudeSignedDecimal();
            lon = position.getLongitudeSignedDecimal();
        }

        return new GeoPosition(lat, lon, z, meta.getElevation());
    }

    public static PointD getFarthestCorner(double pX, double pY, double top, double bottom, double left, double right) {
        PointD fp;

        double dist, temp;

        dist = Math.distance(pX, pY, left, top);
        fp = new PointD(left, top);

        temp = Math.distance(pX, pY, right, top);

        if (temp > dist) {
            dist = temp;
            fp.X = right;
            fp.Y = top;
        }

        temp = Math.distance(pX, pY, left, bottom);

        if (temp > dist) {
            dist = temp;
            fp.X = left;
            fp.Y = bottom;
        }

        temp = Math.distance(pX, pY, right, bottom);

        if (temp > dist) {
            fp.X = right;
            fp.Y = bottom;
        }

        return fp;
    }

    public static PointD RotatePoint(PointD point, double angle, PointD rPoint) {
        return RotatePoint(point.X, point.Y, angle, rPoint.X, rPoint.Y);
    }

    public static PointD RotatePoint(double x, double y, double angle, double rX, double rY) {
        return new PointD(
                (java.lang.Math.cos(angle) * (x - rX) - java.lang.Math.sin(angle) * (y - rY) + rX),
                (java.lang.Math.sin(angle) * (x - rX) + java.lang.Math.cos(angle) * (y - rY) + rY));
    }

    public static Point RotatePoint(int x, int y, double angle, int rX, int rY) {
        return new Point(
                (int) (java.lang.Math.cos(angle) * (x - rX) - java.lang.Math.sin(angle) * (y - rY) + rX),
                (int) (java.lang.Math.sin(angle) * (x - rX) + java.lang.Math.cos(angle) * (y - rY) + rY));
    }


    public static ArrayList<PointD> generateStaticPolyPoints(List<TtPoint> points, HashMap<String, TtMetadata> metadata, int zone, int canvasSize) {
        ArrayList<PointD> pts = new ArrayList<>();

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
                minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (TtPoint point : points) {
            UTMCoords coords = forcePointZone(point, zone, metadata.get(point.getMetadataCN()).getZone(), true);
            PointD pt = new PointD(coords.getX() + 500000, coords.getY() + 10000000);
            
            if (pt.X > maxX) {
                maxX = pt.X;
            }

            if (pt.X < minX) {
                minX = pt.X;
            }

            if (pt.Y > maxY) {
                maxY = pt.Y;
            }

            if (pt.Y < minY) {
                minY = pt.Y;
            }
            
            pts.add(pt);
        }

        double width = maxX - minX;
        double height = maxY - minY;

        double adjustment = canvasSize / (width > height ? width : height);

        double xOffset = (height > width ? (canvasSize - width * adjustment) / 2 : 0);
        double yOffset = (width > height ? (canvasSize - height * adjustment) / 2 : 0);

        for (PointD pt : pts) {
            pt.X = (pt.X - minX) * adjustment + xOffset;
            pt.Y = canvasSize - (pt.Y - minY) * adjustment - yOffset;
        }

        return pts;
    }
    //endregion

    //region Media

    public static TtImage getPictureByType(PictureType type) {
        switch (type) {
            case Regular: return new TtImage();
            case Panorama: return new TtPanorama();
            case PhotoSphere: return new TtPhotoSphere();
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }

    public static TtMedia cloneMedia(TtMedia media) {
        switch (media.getMediaType()) {
            case Picture:
                TtImage picture = (TtImage)media;
                switch (picture.getPictureType()) {
                    case Regular:
                        return new TtImage(picture);
                    case Panorama:
                        return new TtPanorama((TtPanorama) picture);
                    case PhotoSphere:
                        return new TtPhotoSphere((TtPhotoSphere) picture);
                }
                break;
            case Video:
                return new TtVideo((TtVideo)media);
        }
        return null;
    }

    public static int getMediaIndex(TtMedia media, List<TtMedia> mediaList) {
        int index = 0;

        if (mediaList.size() > 0) {
            for (; index < mediaList.size(); index++) {
                if (media.getTimeCreated().isBefore(mediaList.get(index).getTimeCreated())) {
                    break;
                }
            }
        }

        return index;
    }

    public static Comparator<TtMedia> PictureTimeComparator = new Comparator<TtMedia>() {
        @Override
        public int compare(TtMedia lhs, TtMedia rhs) {
            return lhs.getTimeCreated().isAfter(rhs.getTimeCreated()) ? 1 : -1;
        }
    };


    public static TtImage getPictureFromTtCameraIntent(Intent intent) {
        if (intent.getExtras() != null && intent.getExtras().containsKey(Consts.Codes.Data.TTIMAGE)) {
            return intent.getExtras().getParcelable(Consts.Codes.Data.TTIMAGE);
        }

        return null;
    }

    public static TtImage getPictureFromUri(String path, String pointCN) {
        return getImageFromFile(path, pointCN);
    }

    public static ArrayList<TtImage> getPicturesFromImageIntent(Context context, Intent intent, String pointCN) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        ArrayList<TtImage> pictures = new ArrayList<>();

        String mediaDirStr = Global.getTtMediaDir();
        boolean copyToProject = false;
        if (Global.Settings.DeviceSettings.getMediaCopyToProject()) {
            copyToProject = true;

            File mediaDir = new File(mediaDirStr);
            File noMedia = new File(String.format("%s%s%s", mediaDirStr, File.separator, ".nomedia"));
            try {
                if (!mediaDir.exists() || !mediaDir.isDirectory()) {
                    mediaDir.mkdirs();
                }

                if (!noMedia.exists()) {
                    noMedia.createNewFile();
                }
            } catch (Exception e) {
                //
            }
        }

        if (intent.getClipData() != null) {
            ClipData cd = intent.getClipData();

            for (int i = 0; i < cd.getItemCount(); i++) {
                ClipData.Item item = cd.getItemAt(i);
                Uri uri = item.getUri();
                Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);

                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);

                    if (copyToProject) {
                        String newFilePath = String.format("%s%s%s", mediaDirStr, File.separator, FileUtils.getFileName(filePath));
                        if (FileUtils.copyFile(filePath, newFilePath)) {
                            filePath = newFilePath;
                        }
                    }

                    TtImage image = getImageFromFile(filePath, pointCN);
                    if (image != null) {
                        pictures.add(image);
                    }

                    cursor.close();
                }
            }
        } else if (intent.getData() != null) {
            Cursor cursor = context.getContentResolver().query(intent.getData(), filePathColumn, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String uri  = cursor.getString(columnIndex);
                cursor.close();

                if (copyToProject) {
                    String newFilePath = String.format("%s%s%s", mediaDirStr, File.separator, FileUtils.getFileName(uri));
                    if (FileUtils.copyFile(uri, newFilePath)) {
                        uri = newFilePath;
                    }
                }

                TtImage image = getImageFromFile(uri, pointCN);
                if (image != null) {
                    pictures.add(image);
                }
            }
        }

        return pictures;
    }

    private static TtImage getImageFromFile(String filePath, String pointCN) {
        String name = FileUtils.getFileNameWoType(filePath);
        ExifInterface exifInterface;
        String info;

        PictureType type;

        if (FileUtils.fileExists(filePath)) {


            DateTime time = null;
            Integer width, height;
            try {
                exifInterface = new ExifInterface(filePath);

                info = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);

                if (info != null) {
                    try {
                        time = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss").parseDateTime(info);
                    } catch (Exception e) {
                        //
                    }
                }

                type = PictureType.Regular;
                info = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                if (info != null) {
                    width = ParseEx.parseInteger(info);

                    if (width != null) {
                        info = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                        if (info != null) {
                            height = ParseEx.parseInteger(info);

                            if (height != null && (width / height > 1 || height / width > 1)) {
                                type = PictureType.Panorama;
                            }
                        }
                    }
                }

                if (time == null)
                    time = new DateTime(new File(filePath).lastModified());

                if (type == PictureType.Panorama) {
                    return new TtPanorama(name, filePath, time, pointCN);
                } else {
                    return new TtImage(name, filePath, time, pointCN);
                }

            } catch (IOException e) {
                //
            }
        }

        return null;
    }
    //endregion

    //region NMEA
    public static boolean isUsableNmeaBurst(INmeaBurst nmeaBurst, FilterOptions options) {
        boolean valid = false;

        if (options == null) {
            if (nmeaBurst.getFixQuality().getValue() > 0) {
                valid = true;
            }
        } else {
            int value = options.FixType.getValue();

            //reverse RTK and FRTK
            if (value == 4)
                value = 5;
            else if (value == 5)
                value = 4;

            if (value >= options.FixType.getValue() && nmeaBurst.getFix().getValue() >= options.Fix.getValue() &&
                    (options.DopType == DopType.HDOP && nmeaBurst.getHDOP() <= options.DopValue) ||
                    (options.DopType == DopType.PDOP && nmeaBurst.getPDOP() <= options.DopValue)) {
                valid = true;
            }
        }

        return valid;
    }

    public static boolean isUsableNmeaBurst(TtNmeaBurst nmeaBurst, FilterOptions options) {
        boolean valid = false;

        if (options == null) {
            if (nmeaBurst.getFixQuality().getValue() > 0 && nmeaBurst.getFix().getValue() > 1) {
                valid = true;
            }
        } else {

            int fixType = options.FixType.getValue();

            //reverse RTK and FRTK
            if (fixType == 4)
                fixType = 5;
            else if (fixType == 5)
                fixType = 4;

            if (fixType >= options.FixType.getValue() && nmeaBurst.getFix().getValue() >= options.Fix.getValue() &&
                    (options.DopType == DopType.HDOP && nmeaBurst.getHDOP() <= options.DopValue) ||
                    (options.DopType == DopType.PDOP && nmeaBurst.getPDOP() <= options.DopValue)) {
                valid = true;
            }
        }

        return valid;
    }
    //endregion

    //region Device Info
    public static String getDeviceName() {
        return String.format("%s %s %s",
                Build.MANUFACTURER, Build.MODEL, Build.ID);
    }


    public static String exportReport(DataAccessLayer dal) {
        String filename = String.format("%s%sTwoTrailsReport_%s.zip",
                Global.getTtLogFileDir(),
                File.separator,
                DateTime.now().toString());

        boolean exported;

        if (dal != null) {
            exported = FileUtils.zipFiles(filename, TtReport.getFilePath(), dal.getFilePath());
        } else {
            exported = FileUtils.zipFiles(filename, TtReport.getFilePath());
        }

        return exported ? filename : null;
    }
    //endregion

    public static class UI {

        public static Drawable getTtOpDrawable(OpType op, AppUnits.IconColor iconColor, Context context) {
            int id = R.drawable.ic_ttpoint_gps_full;


            switch (iconColor) {
                case Light:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_light;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_light;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_light;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_light;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_light;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_light;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_light;
                            break;
                    }
                    break;
                case White:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_white;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_white;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_white;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_white;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_white;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_white;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_white;
                            break;
                    }
                    break;
                case Black:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_black;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_black;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_black;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_black;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_black;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_black;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_black;
                            break;
                    }
                    break;
                case Dark:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_dark;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_dark;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_dark;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_dark;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_dark;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_dark;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_dark;
                            break;
                    }
                    break;
                case Darker:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_darker;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_darker;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_darker;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_darker;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_darker;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_darker;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_darker;
                            break;
                    }
                    break;
                case Primary:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_primary;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_primary;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_primary;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_primary;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_primary;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_primary;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_primary;
                            break;
                    }
                    break;
                case PrimaryLight:
                    switch(op) {
                        case GPS:
                            id = R.drawable.ic_ttpoint_gps_primary_light;
                            break;
                        case Traverse:
                            id = R.drawable.ic_ttpoint_traverse_primary_light;
                            break;
                        case WayPoint:
                            id = R.drawable.ic_ttpoint_way_primary_light;
                            break;
                        case Quondam:
                            id = R.drawable.ic_ttpoint_quondam_primary_light;
                            break;
                        case SideShot:
                            id = R.drawable.ic_ttpoint_sideshot_primary_light;
                            break;
                        case Walk:
                            id = R.drawable.ic_ttpoint_walk_primary_light;
                            break;
                        case Take5:
                            id = R.drawable.ic_ttpoint_take5_primary_light;
                            break;
                    }
                    break;
            }

            return AndroidUtils.UI.getDrawable(context, id);
        }

        public static Drawable getTtMiniOpDrawable(OpType op, Context context) {
            int id = R.drawable.ic_ttpoint_gps_full;

            switch(op) {
                case GPS:
                    id = R.drawable.ic_ttpoint_gps_white_mini;
                    break;
                case Traverse:
                    id = R.drawable.ic_ttpoint_traverse_white_mini;
                    break;
                case WayPoint:
                    id = R.drawable.ic_ttpoint_way_white_mini;
                    break;
                case Quondam:
                    id = R.drawable.ic_ttpoint_quondam_white_mini;
                    break;
                case SideShot:
                    id = R.drawable.ic_ttpoint_sideshot_white_mini;
                    break;
                case Walk:
                    id = R.drawable.ic_ttpoint_walk_white_mini;
                    break;
                case Take5:
                    id = R.drawable.ic_ttpoint_take5_white_mini;
                    break;
            }

            return AndroidUtils.UI.getDrawable(context, id);
        }
    }


    public static class GMap {

        public static MarkerOptions createMarkerOptions(TtPoint point, boolean adjusted, HashMap<String, TtMetadata> meta) {
            switch (point.getOp()) {
                case GPS:
                case Take5:
                case Walk:
                case WayPoint:
                    return createMarkerOptions((GpsPoint) point, adjusted, meta.get(point.getMetadataCN()));
                case Traverse:
                case SideShot:
                    return createMarkerOptions((TravPoint) point, adjusted, meta.get(point.getMetadataCN()));
                case Quondam:
                    return createMarkerOptions((QuondamPoint)point, adjusted, meta);
            }

            return null;
        }

        public static MarkerOptions createMarkerOptions(GpsPoint point, boolean adjusted, TtMetadata meta) {

            try {
                Double x = adjusted ? point.getAdjX() : point.getUnAdjX();
                Double y = adjusted ? point.getAdjY() : point.getUnAdjY();
                Double z = adjusted ? point.getAdjZ() : point.getUnAdjZ();

                if (x == null) {
                    x = point.getUnAdjX();
                    y = point.getUnAdjY();
                    z =  point.getUnAdjZ();
                }

                z = TtUtils.Convert.distance(z, meta.getElevation(), UomElevation.Meters);

                double lat, lon;

                if (point.hasLatLon()) { //ignore adjust since gps dont adjust to new positions
                    lat = point.getLatitude();
                    lon = point.getLongitude();
                } else {
                    GeoPosition position = UTMTools.convertUTMtoLatLonSignedDec(x, y, meta.getZone());
                    lat = position.getLatitudeSignedDecimal();
                    lon = position.getLongitudeSignedDecimal();
                }

                String snippet = String.format("UTM X: %.3f\nUTM Y: %.3f\nElev (%s): %.1f\n\nLat: %.4f\nLon: %.4f%s",
                        x, y, meta.getElevation().toStringAbv(), Convert.distance(z, meta.getElevation(), UomElevation.Meters), lat, lon,
                        !StringEx.isEmpty(point.getComment()) ?
                                String.format("\n\nComment: %s", point.getComment()) :
                                StringEx.Empty
                );


                MarkerOptions options = new MarkerOptions();

                options.title(String.format("%d (%s)", point.getPID(), adjusted ? "Adj" : "UnAdj"));
                options.snippet(String.format("%s\n\n%s", point.getOp().toString(), snippet));

                LatLng ll = new LatLng(lat, lon);

                try {
                    options.position(ll);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return options;
            } catch (Exception ex) {

                TtReport.writeError(ex.getMessage(), "TtUtils:createMarkerOptions");
                return null;
            }
        }

        public static MarkerOptions createMarkerOptions(TravPoint point, boolean adjusted, TtMetadata meta) {
            double x = adjusted ? point.getAdjX() : point.getUnAdjX();
            double y = adjusted ? point.getAdjY() : point.getUnAdjY();
            double z = adjusted ? point.getAdjZ() : point.getUnAdjZ();
            z = TtUtils.Convert.distance(z, meta.getElevation(), UomElevation.Meters);

            Double faz = point.getFwdAz();
            Double baz = point.getBkAz();

            String sFaz = faz == null ? StringEx.Empty : String.format("%.2f", faz);
            String sBaz = baz == null ? StringEx.Empty : String.format("%.2f", baz);

            String snippet = String.format("UTM X: %.3f\nUTM Y: %.3f\nElev (%s): %.1f\n\nFwd Az: %s\nBk Az:   %s\nSlpDist (%s): %.2f\nSlope (%s): %.2f%s",
                    x, y, meta.getElevation().toStringAbv(), Convert.distance(z, meta.getElevation(), UomElevation.Meters),
                    sFaz, sBaz, meta.getDistance().toString(), point.getSlopeAngle(),
                    meta.getSlope().toStringAbv(), point.getSlopeAngle(),
                    !StringEx.isEmpty(point.getComment()) ?
                    String.format("\n\nComment: %s", point.getComment()) :
                    StringEx.Empty
            );

            GeoPosition position = UTMTools.convertUTMtoLatLonSignedDec(x, y, meta.getZone());

            return new MarkerOptions()
                    .title(String.format("%d (%s)", point.getPID(), adjusted ? "Adj" : "UnAdj"))
                    .snippet(String.format("%s\n\n%s%s", point.getOp(), snippet,
                            !StringEx.isEmpty(point.getComment()) ?
                                    String.format("\n\nComment: %s", point.getComment()) :
                                    StringEx.Empty))
                    .position(new LatLng(position.getLatitudeSignedDecimal(), position.getLongitudeSignedDecimal()));
        }

        public static MarkerOptions createMarkerOptions(QuondamPoint point, boolean adjusted, HashMap<String, TtMetadata> meta) {

            MarkerOptions markerOptions = createMarkerOptions(point.getParentPoint(), adjusted, meta);

            return markerOptions
                    .title(Integer.toString(point.getPID()))
                    .snippet(String.format("%s -> %d %s",
                            point.getOp().toString(),
                            point.getParentPID(),
                            markerOptions.getSnippet()));
        }


        public static LatLngBounds getStartPosInZone(int zone) {
            GeoPosition ne = UTMTools.convertUTMtoLatLonSignedDec(750000, 5420000, zone);
            GeoPosition sw = UTMTools.convertUTMtoLatLonSignedDec(200000, 3210000, zone);

            return new LatLngBounds(new LatLng(sw.getLatitudeSignedDecimal(), sw.getLongitudeSignedDecimal()),
                    new LatLng(ne.getLatitudeSignedDecimal(), ne.getLongitudeSignedDecimal()));
        }

        public static ArrayList<Marker> createMarkersFromPointsInPoly(GoogleMap map, DataAccessLayer dal, String polyCN, boolean adjusted, boolean visible) {
            ArrayList<Marker> markers = new ArrayList<>();
            HashMap<String, TtMetadata> meta = dal.getMetadataMap();

            for (TtPoint point : dal.getPointsInPolygon(polyCN)) {
                markers.add(map.addMarker(
                        createMarkerOptions(point, adjusted, meta)
                                .visible(visible)
                ));
            }

            return markers;
        }


        public static LatLngBounds getBoundsFromMarkers(List<Marker> markers) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (Marker marker : markers) {
                builder.include(marker.getPosition());
            }

            return builder.build();
        }

        public static LatLngBounds getBoundsFromMarkers(List<Marker> markers, double minDistInMeters) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (Marker marker : markers) {
                builder.include(marker.getPosition());
            }

            LatLngBounds bounds = builder.build();

            double distEastWest = Convert.distanceLatLngInMeters(
                    bounds.northeast.latitude, bounds.northeast.longitude,
                    bounds.northeast.latitude, bounds.southwest.longitude);


            double distNorthSouth = Convert.distanceLatLngInMeters(
                    bounds.northeast.latitude, bounds.southwest.longitude,
                    bounds.southwest.latitude, bounds.southwest.longitude);

            if (distEastWest < minDistInMeters || distNorthSouth < minDistInMeters) {
                minDistInMeters /= 2000.0;
                LatLng center = bounds.getCenter();

                double lat = center.latitude + (minDistInMeters / 6378.0 ) * (180.0 / java.lang.Math.PI);
                double lon = center.longitude + (minDistInMeters / 6378.0) * (180.0 / java.lang.Math.PI) /
                        java.lang.Math.cos(lat * java.lang.Math.PI / 180.0);

                LatLng ne = new LatLng(lat, lon);


                lat = center.latitude - (minDistInMeters / 6378.0 ) * (180.0 / java.lang.Math.PI);
                lon = center.longitude - (minDistInMeters / 6378.0) * (180.0 / java.lang.Math.PI) /
                        java.lang.Math.cos(lat * java.lang.Math.PI / 180.0);

                LatLng sw = new LatLng(lat, lon);

                return new LatLngBounds(sw, ne);
            }

            return bounds;
        }


        public static LatLngBounds getBoundsFromMarkers(List<Marker> markers, List<Marker> markers2) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (Marker marker : markers) {
                builder.include(marker.getPosition());
            }

            for (Marker marker : markers2) {
                builder.include(marker.getPosition());
            }

            return builder.build();
        }

        public static LatLngBounds getBoundsFromMarkers(List<Marker> markers, List<Marker> markers2, double minDistInMeters) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (Marker marker : markers) {
                builder.include(marker.getPosition());
            }

            for (Marker marker : markers2) {
                builder.include(marker.getPosition());
            }


            LatLngBounds bounds = builder.build();

            double distEastWest = Convert.distanceLatLngInMeters(
                    bounds.northeast.latitude, bounds.northeast.longitude,
                    bounds.northeast.latitude, bounds.southwest.longitude);


            double distNorthSouth = Convert.distanceLatLngInMeters(
                    bounds.northeast.latitude, bounds.southwest.longitude,
                    bounds.southwest.latitude, bounds.southwest.longitude);

            if (distEastWest < minDistInMeters || distNorthSouth < minDistInMeters) {
                minDistInMeters /= 2.0;
                LatLng center = bounds.getCenter();

                LatLng ne = new LatLng(center.latitude + minDistInMeters, center.longitude + minDistInMeters);
                LatLng sw = new LatLng(center.latitude - minDistInMeters, center.longitude - minDistInMeters);

                return new LatLngBounds(sw, ne);
            }

            return bounds;
        }


        public static LatLngBounds getBounds(List<LatLng> latlngs) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (LatLng latlng : latlngs) {
                builder.include(latlng);
            }

            return builder.build();
        }
    }

    public static class ArcMap {

        public static View createInfoWindow(LayoutInflater inflater, IMultiMapFragment.MarkerData markerData) {
            View view = inflater.inflate(R.layout.content_map_popup, null);

            TextView title = (TextView)view.findViewById(R.id.title);
            TextView content = (TextView)view.findViewById(R.id.text1);

            title.setText(String.format("%d", markerData.Point.getPID()));
            content.setText(getInfoWindowSnippet(markerData.Point, markerData.Adjusted, markerData.Metadata));

            return view;
        }



        private static String getInfoWindowSnippet(TtPoint point, boolean adjusted, TtMetadata meta) {
            switch (point.getOp()) {
                case GPS:
                case Take5:
                case Walk:
                case WayPoint:
                    return getInfoWindowSnippet((GpsPoint) point, adjusted, meta);
                case Traverse:
                case SideShot:
                    return getInfoWindowSnippet((TravPoint) point, adjusted, meta);
                case Quondam:
                    return getInfoWindowSnippet((QuondamPoint) point, adjusted, meta);
            }

            return null;
        }

        private static String getInfoWindowSnippet(GpsPoint point, boolean adjusted, TtMetadata meta) {

            try {
                Double x = adjusted ? point.getAdjX() : point.getUnAdjX();
                Double y = adjusted ? point.getAdjY() : point.getUnAdjY();
                Double z = adjusted ? point.getAdjZ() : point.getUnAdjZ();

                if (x == null) {
                    x = point.getUnAdjX();
                    y = point.getUnAdjY();
                    z =  point.getUnAdjZ();
                }

                z = TtUtils.Convert.distance(z, meta.getElevation(), UomElevation.Meters);

                double lat, lon;

                if (point.hasLatLon()) { //ignore adjust since gps dont adjust to new positions
                    lat = point.getLatitude();
                    lon = point.getLongitude();
                } else {
                    GeoPosition position = UTMTools.convertUTMtoLatLonSignedDec(x, y, meta.getZone());
                    lat = position.getLatitudeSignedDecimal();
                    lon = position.getLongitudeSignedDecimal();
                }

                return String.format("%s\n\nUTM X: %.3f\nUTM Y: %.3f\nElev (%s): %.1f\n\nLat: %.4f\nLon: %.4f%s",
                        point.getOp().toString(),
                        x, y, meta.getElevation().toStringAbv(), Convert.distance(z, meta.getElevation(), UomElevation.Meters), lat, lon,
                        !StringEx.isEmpty(point.getComment()) ?
                                String.format("\n\nComment: %s", point.getComment()) :
                                StringEx.Empty);
            } catch (Exception ex) {
                TtReport.writeError(ex.getMessage(), "TtUtils:getInfoWindowSnippet");
                return null;
            }
        }

        private static String getInfoWindowSnippet(TravPoint point, boolean adjusted, TtMetadata meta) {
            double x = adjusted ? point.getAdjX() : point.getUnAdjX();
            double y = adjusted ? point.getAdjY() : point.getUnAdjY();
            double z = adjusted ? point.getAdjZ() : point.getUnAdjZ();
            z = TtUtils.Convert.distance(z, meta.getElevation(), UomElevation.Meters);

            Double faz = point.getFwdAz();
            Double baz = point.getBkAz();

            String sFaz = faz == null ? StringEx.Empty : String.format("%.2f", faz);
            String sBaz = baz == null ? StringEx.Empty : String.format("%.2f", baz);

            return String.format("%s\n\nUTM X: %.3f\nUTM Y: %.3f\nElev (%s): %.1f\n\nFwd Az: %s\nBk Az:   %s\nSlpDist (%s): %.2f\nSlope (%s): %.2f%s",
                    point.getOp(),
                    x, y, meta.getElevation().toStringAbv(), Convert.distance(z, meta.getElevation(), UomElevation.Meters),
                    sFaz, sBaz, meta.getDistance().toString(), point.getSlopeAngle(),
                    meta.getSlope().toStringAbv(), point.getSlopeAngle(),
                    !StringEx.isEmpty(point.getComment()) ?
                            String.format("\n\nComment: %s", point.getComment()) :
                            StringEx.Empty);
        }

        private static String getInfoWindowSnippet(QuondamPoint point, boolean adjusted, TtMetadata meta) {
            return String.format("%s -> %d %s%s",
                point.getOp().toString(),
                point.getParentPID(),
                getInfoWindowSnippet(point.getParentPoint(), adjusted, meta),
                    !StringEx.isEmpty(point.getComment()) ?
                            String.format("\n\nComment: %s", point.getComment()) :
                            StringEx.Empty);
        }

    }
}
