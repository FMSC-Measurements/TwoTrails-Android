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
    public static final int DEFAULT_MAX_SHELL = 5; //100 meters from center [5(MAX_SHELL) x 10(BLOCK_SIZE) x 2]

    private final MultiKeyMap<Integer, List<CalcPoint>> _PointMap = new MultiKeyMap<>();
    private final HashMap<String, TtMetadata> _Metadata;
    private final HashMap<String, HashMap<Integer, TtPoint>> _PointsByPoly = new HashMap<>();
    private final HashMap<String, Integer> _FirstIndexInPoly = new HashMap<>();
    private final HashMap<String, Integer> _LastIndexInPoly = new HashMap<>();
    private final HashMap<String, TtPolygon> _Polygons = new HashMap<>();
    private final HashMap<String, PolygonCalculator> _PolygonsCalcs = new HashMap<>();
    private final int _DefaultZone, _MaxShellSize;

    public ClosestPositionCalculator(List<TtPolygon> polygons, int defaultZone, DataAccessLayer dal) {
        _DefaultZone = defaultZone;
        _Metadata = dal.getMetadataMap();

        double maxDistBetweenPoints = 0;

        for (TtPolygon poly : polygons) {
            ArrayList<TtPoint> points = dal.getPointsInPolygon(poly.getCN());

            if (points.size() > 1) {
                HashMap<Integer, TtPoint> ttPoints = new HashMap<>();
                _Polygons.put(poly.getCN(), poly);
                _PointsByPoly.put(poly.getCN(), ttPoints);

                ArrayList<PointD> apoints = new ArrayList<>();

                int lastIndex = 0;
                TtPoint firstPoint = null, lastPoint = null;

                for (TtPoint point : points) {
                    if (point.isOnBnd()) {
                        TtMetadata currMeta = _Metadata.get(point.getMetadataCN());
                        ttPoints.put(point.getIndex(), point);

                        if (firstPoint == null) {
                            firstPoint = point;
                        }

                        if (!_FirstIndexInPoly.containsKey(poly.getCN())) {
                            _FirstIndexInPoly.put(poly.getCN(), point.getIndex());
                        }

                        if (currMeta == null) throw new RuntimeException("Metadata Not Found");
                        if (currMeta.getZone() == defaultZone) {
                            addToMap(new UTMCoords(point.getAdjX(), point.getAdjY(), currMeta.getZone()), point);
                            apoints.add(new PointD(point.getAdjX(), point.getAdjY()));
                        } else {
                            UTMCoords coords = TtUtils.Points.forcePointZone(point, defaultZone, currMeta.getZone());
                            addToMap(coords, point);
                            apoints.add(new PointD(coords.getX(), coords.getY()));
                        }

                        lastIndex = point.getIndex();

                        if (lastPoint != null) {
                            maxDistBetweenPoints = Double.max(TtUtils.Math.distance(lastPoint, point), maxDistBetweenPoints);
                        }

                        lastPoint = point;
                    }
                }

                if (lastPoint != null) {
                    maxDistBetweenPoints = Double.max(TtUtils.Math.distance(lastPoint, firstPoint), maxDistBetweenPoints);
                }

                _LastIndexInPoly.put(poly.getCN(), lastIndex);

                _PolygonsCalcs.put(poly.getCN(), apoints.size() > 2 ? new PolygonCalculator(apoints) : null);
            }
        }

        double maxCalcShellSize = maxDistBetweenPoints / BLOCK_SIZE;

        if (maxCalcShellSize > DEFAULT_MAX_SHELL) {
            _MaxShellSize = (int)maxCalcShellSize + 1;
        } else {
            _MaxShellSize = DEFAULT_MAX_SHELL;
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

    /**
     * @param coords Coordinates in UTM
     * @return Closest Position to current position
     */
    public ClosestPosition getClosestPosition(UTMCoords coords) {
        return getClosestPosition(coords.getX(), coords.getY(), false);
    }

    /**
     * @param coords Coordinates in UTM
     * @param lookBeyondMaxDist Whether to look past the maximum possible distance between points
     * @return Closest Position to current position
     */
    public ClosestPosition getClosestPosition(UTMCoords coords, boolean lookBeyondMaxDist) {
        return getClosestPosition(coords.getX(), coords.getY(), lookBeyondMaxDist);
    }

    /**
     * @param utmX UTM X of current position
     * @param utmY UTM Y of current position
     * @param lookBeyondMaxDist Whether to look past the maximum possible distance between points
     * @return Closest Position to current position
     */
    public ClosestPosition getClosestPosition(Double utmX, Double utmY, boolean lookBeyondMaxDist) {
        int bx = (int)(utmX / BLOCK_SIZE);
        int by = (int)(utmY / BLOCK_SIZE);

        int shell = 0, blockCount = 0;
        final int totalBlocks = _PointMap.size();

        ClosestPosition closestPosition = null;

        while (blockCount < totalBlocks && shell < _MaxShellSize) {
            int startX = bx - shell;
            int startY = by - shell;

            int column = 0, row = 0, lastColumnRow = (1 + 2 * shell);

            while (row < lastColumnRow) {
                while (column < lastColumnRow) {
                    if (row == 0 || row == lastColumnRow - 1 || column == 0 || column == lastColumnRow - 1) {
                        List<CalcPoint> block = _PointMap.get(startX + column, startY + row);
                        if (block != null) {

                            for (CalcPoint calcPoint : block) {
                                ClosestPosition cp = getClosestPosition(utmX, utmY, calcPoint);

                                if (closestPosition == null || (cp != null && cp.getDistance() < closestPosition.getDistance())) {
                                    closestPosition = cp;
                                }
                            }

                            blockCount++;
                        }
                    }
                    column++;
                }
                column = 0;
                row++;
            }

            shell++;
        }

        if (closestPosition == null && lookBeyondMaxDist) {
            for (List<CalcPoint> blocks : _PointMap.values()) {
                for (CalcPoint block : blocks) {
                    ClosestPosition cp = getClosestPosition(utmX, utmY, block);

                    if (closestPosition == null || (cp != null && cp.getDistance() < closestPosition.getDistance())) {
                        closestPosition = cp;
                    }
                }
            }
        }

        return closestPosition;
    }

    private ClosestPosition getClosestPosition(Double utmX, Double utmY, CalcPoint calcPoint) {
        TtPoint point = calcPoint.getTtPoint();
        TtPolygon polygon = _Polygons.get(point.getPolyCN());
        UTMCoords currCoords = new UTMCoords(utmX, utmY, calcPoint.getCoords().getZone());
        HashMap<Integer, TtPoint> points = _PointsByPoly.get(polygon.getCN());
        PolygonCalculator polyCalc = _PolygonsCalcs.get(polygon.getCN());

        int lastIndex = _LastIndexInPoly.get(polygon.getCN());
        int firstIndex = _FirstIndexInPoly.get(polygon.getCN());

        UTMCoords prevCoords;
        TtPoint prevTtPoint = null;
        if (point.getIndex() > firstIndex) {
            for (int i = point.getIndex() - 1; i >= firstIndex; i--) {
                prevTtPoint = points.get(i);
                if (prevTtPoint != null) break;
            }
        } else {
            prevTtPoint = points.get(lastIndex);
        }

        int oMetaZone = _Metadata.get(prevTtPoint.getMetadataCN()).getZone();


        if (oMetaZone != _DefaultZone) {
            prevCoords = TtUtils.Points.forcePointZone(prevTtPoint, _DefaultZone, oMetaZone);
        } else {
            prevCoords = new UTMCoords(prevTtPoint.getAdjX(), prevTtPoint.getAdjY(), oMetaZone);
        }

        Tuple<UTMCoords, Double> cdPrev = getClosestPointAndDistance(currCoords, calcPoint.getCoords(), prevCoords);

        UTMCoords nextPoint;
        TtPoint nextTtPoint = null;

        if (point.getIndex() < lastIndex) {
            for (int i = point.getIndex() + 1; i <= lastIndex; i++) {
                nextTtPoint = points.get(i);
                if (nextTtPoint != null) break;
            }
        } else {
            nextTtPoint = points.get(firstIndex);
        }


        oMetaZone = _Metadata.get(prevTtPoint.getMetadataCN()).getZone();

        if (oMetaZone != _DefaultZone) {
            nextPoint = TtUtils.Points.forcePointZone(nextTtPoint, _DefaultZone, oMetaZone);
        } else {
            nextPoint = new UTMCoords(nextTtPoint.getAdjX(), nextTtPoint.getAdjY(), oMetaZone);
        }

        Tuple<UTMCoords, Double> cdNext = getClosestPointAndDistance(currCoords, calcPoint.getCoords(), nextPoint);

        if (cdPrev != null && cdNext != null) {
            if (cdPrev.Item2.isNaN() && !cdNext.Item2.isNaN()) {
                return new ClosestPosition(cdNext.Item1, cdNext.Item2, point, nextTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY));
            } else if (!cdPrev.Item2.isNaN() && cdNext.Item2.isNaN()) {
                return new ClosestPosition(cdPrev.Item1, cdPrev.Item2, point, prevTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY));
            } else {
                return (cdPrev.Item2 < cdNext.Item2) ?
                    new ClosestPosition(cdPrev.Item1, cdPrev.Item2, point, prevTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY)) :
                    new ClosestPosition(cdNext.Item1, cdNext.Item2, point, nextTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY));
            }
        } else if (cdPrev != null) {
            return new ClosestPosition(cdPrev.Item1, cdPrev.Item2, point, prevTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY));
        } else if (cdNext != null) {
            return new ClosestPosition(cdNext.Item1, cdNext.Item2, point, nextTtPoint, polygon, polyCalc != null && polyCalc.pointInPolygon(utmX, utmY));
        } else {
            return null;
        }
    }

    public static Tuple<UTMCoords, Double> getClosestPointAndDistance(UTMCoords cp, UTMCoords p1, UTMCoords p2) {
        try {
            UTMCoords intersection = TtUtils.Math.getClosestPointOnLineSegment(cp, p1, p2);
            return new Tuple<>(intersection, TtUtils.Math.distance(cp, intersection));
        } catch (Exception e) {
            return null;
        }
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
        private final TtPoint _Point1, _Point2;
        private final UTMCoords _ClosestPosition;
        private final TtPolygon _Polygon;
        private final double _Distance;
        private final boolean _InsidePoly;
        private final boolean _PositionIsPoint1;
        private final boolean _PositionIsPoint2;

        /**
         * @param closestPosition Closest position to the initial position
         * @param distance Distance from the initial position to the closest one on the polygon
         * @param point1 Point 1 of the line the closest position is on
         * @param point2 Point 2 of the line the closest position is on
         * @param polygon Polygon the closest position is on
         * @param insidePoly Whether or not the initial position was inside the polygon
         */
        public ClosestPosition(UTMCoords closestPosition, double distance, TtPoint point1, TtPoint point2, TtPolygon polygon, boolean insidePoly) {
            this._ClosestPosition = closestPosition;
            this._Distance = distance;
            this._Point1 = point1;
            this._Point2 = point2;
            this._Polygon = polygon;
            this._InsidePoly = insidePoly;

            this._PositionIsPoint1 = this._Point1.sameAdjLocation(this._ClosestPosition.getX(), this._ClosestPosition.getY(), this._Point1.getAdjZ());
            this._PositionIsPoint2 = this._Point2.sameAdjLocation(this._ClosestPosition.getX(), this._ClosestPosition.getY(), this._Point1.getAdjZ());
        }

        /**
         * @return First Point in closest line to the closest position
         */
        public TtPoint getPoint1() {
            return _Point1;
        }

        /**
         * @return Second Point in closest line to the closest position
         */
        public TtPoint getPoint2() {
            return _Point2;
        }

        /**
         * @return Polygon of the closest position
         */
        public TtPolygon getPolygon() {
            return _Polygon;
        }

        /**
         * @return Closest position on the polygon
         */
        public UTMCoords getCoords() {
            return _ClosestPosition;
        }

        /**
         * @return Distance to the closest position
         */
        public double getDistance() {
            return _Distance;
        }

        /**
         * @return Whether or not the initial position is inside the polygon
         */
        public boolean isInsidePoly() { return _InsidePoly; }

        /**
         * @return Whether Point 1 is closest (same and poly) distance from the initial position
         */
        public boolean isPositionPoint1() {
            return _PositionIsPoint1;
        }

        /**
         * @return Whether Point 2 is closest (same and poly) distance from the initial position
         */
        public boolean isPositionPoint2() {
            return _PositionIsPoint2;
        }

        /**
         * @return Whether the position is at a specific point
         */
        public boolean isPositionAtAPoint() {
            return _PositionIsPoint1 || _PositionIsPoint2;
        }

        /**
         * @return The closest TtPoint on the polygon or null if it is between points on the polygon
         */
        public TtPoint getClosestPoint() {
            return isPositionAtAPoint() ? (_PositionIsPoint1 ? _Point1 : _Point2) : null;
        }

        /**
         * @param currCoods Current Coordinates
         * @return The azimuth to the Closest Position on the Polygon
         */
        public double getAzimuthToClosestPosition(UTMCoords currCoods) {
            return getAzimuthToClosestPosition(currCoods.getX(), currCoods.getY());
        }

        /**
         * @param point Current Point
         * @return The azimuth to the Closest Position on the Polygon
         */
        public double getAzimuthToClosestPosition(TtPoint point) {
            return getAzimuthToClosestPosition(point.getAdjX(), point.getAdjY());
        }

        /**
         * @param utmX UTM X
         * @param utmY UTM Y
         * @return The azimuth to the Closest Position on the Polygon
         */
        public double getAzimuthToClosestPosition(double utmX, double utmY) {
            return TtUtils.Math.azimuthOfPoint(
                    utmX, utmY,
                    _ClosestPosition.getX(), _ClosestPosition.getY());
        }
    }
}
