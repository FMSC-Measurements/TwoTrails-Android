package com.usda.fmsc.twotrails.utilities;


import com.usda.fmsc.geospatial.utm.UTMCoords;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.utilities.Tuple;

import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClosestPositionCalculator {
    public static final int BLOCK_SIZE = 10; //in meters

    private final MultiKeyMap<Integer, List<CalcPoint>> _PointMap = new MultiKeyMap<>();
    private final HashMap<String, TtMetadata> _Metadata;
    private final HashMap<String, List<TtPoint>> _PointsByPoly = new HashMap<>();
    private final HashMap<String, TtPolygon> _Polygons = new HashMap<>();
    private final HashMap<String, PolygonCalculator> _PolygonsCalcs = new HashMap<>();
    private final int _DefaultZone;

    public ClosestPositionCalculator(List<TtPolygon> polygons, int defaultZone, DataAccessLayer dal) {
        _DefaultZone = defaultZone;
        _Metadata = dal.getMetadataMap();

        for (TtPolygon poly : polygons) {
            ArrayList<TtPoint> points = dal.getPointsInPolygon(poly.getCN());

            if (points.size() > 1) {
                _Polygons.put(poly.getCN(), poly);
                _PointsByPoly.put(poly.getCN(), points);

                ArrayList<PointD> apoints = new ArrayList<>();

                for (TtPoint point : points) {
                    if (point.isOnBnd()) {
                        TtMetadata currMeta = _Metadata.get(point.getMetadataCN());

                        if (currMeta == null) throw new RuntimeException("Metadata Not Found");
                        if (currMeta.getZone() == defaultZone) {
                            addToMap(new UTMCoords(point.getAdjX(), point.getAdjY(), currMeta.getZone()), point);
                            apoints.add(new PointD(point.getAdjX(), point.getAdjY()));
                        } else {
                            UTMCoords coords = TtUtils.Points.forcePointZone(point, defaultZone, currMeta.getZone());
                            addToMap(coords, point);
                            apoints.add(new PointD(coords.getX(), coords.getY()));
                        }
                    }
                }

                _PolygonsCalcs.put(poly.getCN(), new PolygonCalculator(apoints));
            }
        }
    }

    private void addToMap(UTMCoords coords, TtPoint point) {
        int bx = (int)(coords.getX() / BLOCK_SIZE);
        int by = (int)(coords.getY() / BLOCK_SIZE);

        List<CalcPoint> points = _PointMap.get(bx, by);
        if (points == null)  {
            points = new ArrayList<>();
            points.add(new CalcPoint(bx, by, coords, point));

            _PointMap.put(bx, by, points);
        } else {
            points.add(new CalcPoint(bx, by, coords, point));
        }
    }

    public ClosestPosition getClosestPosition(UTMCoords coords) {
        return getClosestPosition(coords.getX(), coords.getY());
    }

    public ClosestPosition getClosestPosition(Double utmX, Double utmY) {
        int bx = (int)(utmX / BLOCK_SIZE);
        int by = (int)(utmY / BLOCK_SIZE);

        int shell = 0, blockCount = 0;
        final int totalBlocks = _PointMap.size();


        ClosestPosition closestPosition = null;

        while (blockCount < totalBlocks) {
            int startX = bx - (BLOCK_SIZE * shell);
            int startY = by - (BLOCK_SIZE * shell);

            int column = 0, row = 0, lastColumnRow = (1 + 2 * shell);

            while (row < lastColumnRow) {
                while (column < lastColumnRow) {
                    if (row == 0 || row == lastColumnRow - 1 || column == 0 || column == lastColumnRow - 1) {
                        List<CalcPoint> block = _PointMap.get(startX + BLOCK_SIZE * column, startY + BLOCK_SIZE * row);
                        if (block != null) {

                            for (CalcPoint calcPoint : block) {
                                ClosestPosition cp = getPosition(utmX, utmY, calcPoint);

                                if (closestPosition == null || cp.getDistance() < closestPosition.getDistance()) {
                                    closestPosition = cp;
                                }
                            }

                            blockCount++;
                        }
                    }
                    column++;
                }
                row++;
            }

            if (closestPosition != null) {
                return closestPosition;
            }

            shell++;
        }

        return null;
    }

    private ClosestPosition getPosition(Double utmX, Double utmY, CalcPoint calcPoint) {
        TtPoint point = calcPoint.getTtPoint();
        TtPolygon polygon = _Polygons.get(point.getPolyCN());
        UTMCoords currCoords = new UTMCoords(utmX, utmY, calcPoint.getCoords().getZone());
        List<TtPoint> points = _PointsByPoly.get(polygon.getCN());
        PolygonCalculator polyCalc = _PolygonsCalcs.get(polygon.getCN());

        UTMCoords prevCoords;
        TtPoint prevTtPoint = (point.getIndex() > 0) ?
                points.get(point.getIndex() - 1) :
                points.get(points.size() - 1);

        int oMetaZone = _Metadata.get(prevTtPoint.getMetadataCN()).getZone();


        if (oMetaZone != _DefaultZone) {
            prevCoords = TtUtils.Points.forcePointZone(prevTtPoint, _DefaultZone, oMetaZone);
        } else {
            prevCoords = new UTMCoords(prevTtPoint.getAdjX(), prevTtPoint.getAdjY(), oMetaZone);
        }

        Tuple<UTMCoords, Double> cdPrev = getClosestPointAndDistance(currCoords, calcPoint.getCoords(), prevCoords);

        UTMCoords nextPoint;
        TtPoint nextTtPoint = (point.getIndex() < points.size() - 1) ?
                points.get(point.getIndex() + 1) :
                points.get(0);

        oMetaZone = _Metadata.get(prevTtPoint.getMetadataCN()).getZone();

        if (oMetaZone != _DefaultZone) {
            nextPoint = TtUtils.Points.forcePointZone(nextTtPoint, _DefaultZone, oMetaZone);
        } else {
            nextPoint = new UTMCoords(nextTtPoint.getAdjX(), nextTtPoint.getAdjY(), oMetaZone);
        }

        Tuple<UTMCoords, Double> cdNext = getClosestPointAndDistance(currCoords, calcPoint.getCoords(), nextPoint);

        return (cdPrev.Item2 < cdNext.Item2) ?
                new ClosestPosition(cdPrev.Item1, cdPrev.Item2, point, prevTtPoint, polygon, polyCalc.pointInPolygon(utmX, utmY)) :
                new ClosestPosition(cdNext.Item1, cdNext.Item2, point, nextTtPoint, polygon, polyCalc.pointInPolygon(utmX, utmY));
    }

    public static Tuple<UTMCoords, Double> getClosestPointAndDistance(UTMCoords cp, UTMCoords p1, UTMCoords p2) {
        double slope = (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
        double b = p1.getY() - slope * p1.getX();
        double invSlope =  -1 / slope;
        double pb = cp.getY() - invSlope * cp.getX();
        double x = (b - pb) / (invSlope - slope);
        double y = invSlope * x + pb;
        UTMCoords intersection = new UTMCoords(x, y, cp.getZone());
        return new Tuple<>(intersection, TtUtils.Math.distance(cp, intersection));
    }


    private static class CalcPoint {
        private final int bx, by;
        private final UTMCoords coords;
        private final TtPoint point;

        public CalcPoint(int bx, int by, UTMCoords coords, TtPoint point) {
            this.bx = bx;
            this.by = by;
            this.coords = coords;
            this.point = point;
        }

        public int getXBlock() {
            return bx;
        }

        public int getYBlock() {
            return by;
        }

        public UTMCoords getCoords() { return coords; }

        public TtPoint getTtPoint() {
            return point;
        }
    }

    public static class ClosestPosition {
        private final TtPoint Point1, Point2;
        private final UTMCoords ClosestPosition;
        private final TtPolygon Polygon;
        private final double Distance;
        private final boolean InsidePoly;
        private final boolean PositionIsPoint1;

        /**
         * @param closestPosition Closest position to the initial position
         * @param distance Distance from the initial position to the closest one on the polygon
         * @param point1 Point 1 of the line the closest position is on
         * @param point2 Point 2 of the line the closest position is on
         * @param polygon Polygon the closest position is on
         * @param insidePoly Whether or not the initial position was inside the polygon
         */
        public ClosestPosition(UTMCoords closestPosition, double distance, TtPoint point1, TtPoint point2, TtPolygon polygon, boolean insidePoly) {
            this.ClosestPosition = closestPosition;
            this.Distance = distance;
            this.Point1 = point1;
            this.Point2 = point2;
            this.Polygon = polygon;
            this.InsidePoly = insidePoly;

            this.PositionIsPoint1 = this.Point1.sameAdjLocation(this.ClosestPosition.getX(), this.ClosestPosition.getY(), this.Point1.getAdjZ());
        }

        /**
         * @return First Point in closest line to the closest position
         */
        public TtPoint getPoint1() {
            return Point1;
        }

        /**
         * @return Second Point in closest line to the closest position
         */
        public TtPoint getPoint2() {
            return Point2;
        }

        /**
         * @return Polygon of the closest position
         */
        public TtPolygon getPolygon() {
            return Polygon;
        }

        /**
         * @return Closest position on the polygon
         */
        public UTMCoords getCoords() {
            return ClosestPosition;
        }

        /**
         * @return Distance to the closest position
         */
        public double getDistance() {
            return Distance;
        }

        /**
         * @return Whether or not the initial position is inside the polygon
         */
        public boolean isInsidePoly() { return InsidePoly; }

        /**
         * @return Whether Point 1 is closest (same and poly) distance from the initial position
         */
        public boolean IsPositionPoint1() {
            return PositionIsPoint1;
        }
    }
}
