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
                            addToMap(point.getAdjX(), point.getAdjY(), point);
                            apoints.add(new PointD(point.getAdjX(), point.getAdjY()));
                        } else {
                            UTMCoords coords = TtUtils.Points.forcePointZone(point, defaultZone, currMeta.getZone());
                            addToMap(coords.getX(), coords.getY(), point);
                            apoints.add(new PointD(coords.getX(), coords.getY()));
                        }
                    }
                }

                _PolygonsCalcs.put(poly.getCN(), new PolygonCalculator(apoints));
            }
        }
    }

    private void addToMap(Double x, Double y, TtPoint point) {
        int bx = (int)(x / BLOCK_SIZE);
        int by = (int)(y / BLOCK_SIZE);

        List<CalcPoint> points = _PointMap.get(bx, by);
        if (points == null)  {
            points = new ArrayList<>();
            points.add(new CalcPoint(bx, by, x, y, point));

            _PointMap.put(bx, by, points);
        } else {
            points.add(new CalcPoint(bx, by, x, y, point));
        }
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
        List<TtPoint> points = _PointsByPoly.get(polygon.getCN());
        PolygonCalculator polyCalc = _PolygonsCalcs.get(polygon.getCN());

        PointD prevPoint;
        TtPoint prevTtPoint = (point.getIndex() > 0) ?
                points.get(point.getIndex() - 1) :
                points.get(points.size() - 1);

        int prevMetaZone = _Metadata.get(prevTtPoint.getMetadataCN()).getZone();


        if (prevMetaZone != _DefaultZone) {
            UTMCoords coords = TtUtils.Points.forcePointZone(prevTtPoint, _DefaultZone, prevMetaZone);
            prevPoint = new PointD(coords.getX(), coords.getY());
        } else {
            prevPoint = new PointD(prevTtPoint.getAdjX(), prevTtPoint.getAdjY());
        }

        Tuple<PointD, Double> cd = getClosestPointAndDistance(new PointD(utmX, utmY), calcPoint.getPoint(), prevPoint);
        return new ClosestPosition(cd.Item1, cd.Item2, point, prevTtPoint, polyCalc.pointInPolygon(utmX, utmY));
    }

    private Tuple<PointD, Double> getClosestPointAndDistance(PointD cp, PointD p1, PointD p2) {
        double slope = (p2.Y - p1.Y) / (p2.X - p1.X);
        double b = p1.Y - slope * p1.X;
        double invSlope =  -1 / slope;
        double pb = cp.Y - invSlope * cp.X;
        double x = (b - pb) / (invSlope - slope);
        double y = invSlope * x + pb;
        PointD intersection = new PointD(x, y);
        return new Tuple<>(intersection, TtUtils.Math.distance(cp, intersection));
    }


    private static class CalcPoint {
        private final int bx, by;
        private final double x, y;
        private final TtPoint point;

        public CalcPoint(int bx, int by, double x, double y, TtPoint point) {
            this.x = x;
            this.y = y;
            this.bx = bx;
            this.by = by;
            this.point = point;
        }

        public int getXBlock() {
            return bx;
        }

        public int getYBlock() {
            return by;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public PointD getPoint() { return new PointD(x, y); }

        public TtPoint getTtPoint() {
            return point;
        }
    }

    public static class ClosestPosition {
        private final TtPoint Point1, Point2;
        private final PointD ClosestPoint;
        private final double Distance;
        private final boolean InsidePoly;

        public ClosestPosition(PointD closestPoint, double distance, TtPoint point1, TtPoint point2, boolean insidePoly) {
            this.ClosestPoint = closestPoint;
            this.Distance = distance;
            this.Point1 = point1;
            this.Point2 = point2;
            this.InsidePoly = insidePoly;
        }

        public TtPoint getPoint1() {
            return Point1;
        }

        public TtPoint getPoint2() {
            return Point2;
        }

        public PointD getClosestPoint() {
            return ClosestPoint;
        }

        public double getDistance() {
            return Distance;
        }

        public boolean isInsidePoly() { return InsidePoly; }
    }
}
