package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class HaidLogic {
    private static final String INFINITE_SYMBOL = "\u221E";

    private static StringBuilder pointStats;
    private static double travLength, totalTravError, totalGpsError, travGpsError;
    private static boolean traversing;
    private static int traverseSegments, lastGpsPtPID;

    private static TtPoint _LastTtPoint, _LastTtBndPt;
    private static PointD _LastPoint;

    private static List<Leg> _Legs;
    private static HashMap<String, TtPolygon> _Polygons;


    private static void init(DataAccessLayer dal) {
        pointStats = new StringBuilder();

        _Legs = new ArrayList<>();

        _Polygons = new HashMap<>();
        for (TtPolygon poly : dal.getPolygons()) {
            _Polygons.put(poly.getCN(), poly);
        }

        totalTravError = totalGpsError = travGpsError = 0;
        traversing = false;
        traverseSegments = 0;

        _LastTtPoint = _LastTtBndPt = null;
    }

    public synchronized static String generatePolyStats(TtPolygon polygon, DataAccessLayer dal, boolean showPoints, boolean save) {
        StringBuilder sb = new StringBuilder();

        try {
            init(dal);

            List<TtPoint> points = dal.getPointsInPolygon(polygon.getCN());

            if (save) {
                sb.append(String.format("Polygon Name: %s%s%s", polygon.getName(), Consts.NewLine, Consts.NewLine));
            }

            if (!StringEx.isEmpty(polygon.getDescription())) {
                sb.append(String.format("Description: %s%s%s", polygon.getDescription(), Consts.NewLine, Consts.NewLine));
            }

            if (points.size() > 0) {
                if (points.size() > 2) {
                    points = TtUtils.filterOut(points, OpType.WayPoint);

                    if (points.size() > 0) {
                        if (points.size() > 2) {
                            for (TtPoint point : points) {
                                pointStats.append(getPointSummary(point, false, showPoints));
                            }

                            TtPoint pt = points.get(0);
                            for (TtPoint point : points) {
                                if (point.isOnBnd()) {
                                    pt = point;
                                    break;
                                }
                            }

                            if (!pt.sameAdjLocation(_LastTtPoint)) {
                                _Legs.add(new Leg(_LastTtPoint, pt));
                            }

                            for (Leg leg : _Legs) {
                                totalGpsError += leg.getAreaError();
                            }

                            sb.append(getPolygonSummary(polygon, save));
                            sb.append(pointStats);
                        } else {
                            sb.append("There are not enough valid points in the polygon.");
                        }
                    } else {
                        sb.append("There are only WayPoints in the polygon.");
                    }
                } else {
                    sb.append("There are not enough points in the polygon.");
                }
            } else {
                sb.append("There are no points in the polygon.");
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "HaidLogic:generatePolyStats", ex.getStackTrace());
            return "Error generating polygon info";
        }

        if (save) {
            sb.append(String.format("%s%s- - - - - - - - - - - - - - - - - - - -", Consts.NewLine, Consts.NewLine));
        }

        return sb.toString();
    }

    public synchronized static String generateAllPolyStats(DataAccessLayer dal, boolean showPoints, boolean save) {
        StringBuilder sb = new StringBuilder();

        try {
            init(dal);

            for (TtPolygon polygon : _Polygons.values()) {

                sb.append(generatePolyStats(polygon, dal, showPoints, save));

                sb.append(String.format("%s%s", Consts.NewLine, Consts.NewLine));
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "HaidLogic:generateAllPolyStats");
            return "Error generating polygon info";
        }

        return sb.toString();
    }

    
    private static void closeTraverse(TtPoint point, StringBuilder sb) {
        double closeError = TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y,
                point.getUnAdjX(), point.getUnAdjY());

        double travError = closeError < Consts.Minimum_Point_Accuracy ? Double.POSITIVE_INFINITY : travLength / closeError;

        sb.append(String.format("\tTraverse Total Segments: %d%s",
                traverseSegments, Consts.NewLine));
        sb.append(String.format("\tTraverse Total Distance: %.2f feet.%s",
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(travLength, Dist.Meters), 2), Consts.NewLine));
        sb.append(String.format("\tTraverse Closing Distance: %.2f feet.%s",
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(closeError, Dist.Meters), 2), Consts.NewLine));
        sb.append(String.format("\tTraverse Close Error: 1 part in %s.%s",
                Double.isInfinite(travError) ? INFINITE_SYMBOL : StringEx.toString(TtUtils.Math.round(travError, 2), 2),
                Consts.NewLine));


        totalTravError += (travLength * closeError / 2);

        traversing = false;
    }

    static TtPoint _LastTravPoint = null;

    private static String getPointSummary(TtPoint point, boolean fromQuondam, boolean showPoints) throws Exception {
        StringBuilder sb = new StringBuilder();

        switch (point.getOp()) {
            case GPS:
            case Take5:
            case Walk:
            case WayPoint:
            {
                lastGpsPtPID = point.getPID();

                if (point.isOnBnd()) {
                    if (traversing) {
                       closeTraverse(point, sb);
                    } else {
                        if (_LastTtPoint != null) {
                            _Legs.add(new Leg(_LastTtPoint, point));
                        }
                    }

                    _LastPoint = new PointD(point.getUnAdjX(), point.getAdjY());
                    _LastTtBndPt = point;
                }

                if (!fromQuondam && showPoints) {
                    sb.append(String.format("Point %d: %s %s- ", point.getPID(), point.isOnBnd() ? " " : "*",
                            point.getOp().toString()));
                    sb.append(String.format("Accuracy is %.3f meters.%s", point.getAccuracy(), Consts.NewLine));
                }
                break;
            }
            case Traverse:
            {
                if (_LastTtPoint != null) {
                    if (point.isOnBnd()) {
                        if (traversing) {
                            travLength += TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y, point.getUnAdjX(), point.getUnAdjY());

                            if (_LastTtBndPt != null) {
                                _Legs.add(new Leg(_LastTravPoint, point));
                            }
                        } else {
                            traverseSegments = 0;
                            travLength = TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y, point.getUnAdjX(), point.getUnAdjY());
                            traversing = true;

                            if (showPoints) {
                                sb.append(String.format("Traverse Start:%s", Consts.NewLine));
                            }

                            if (_LastTtBndPt != null) {
                                _Legs.add(new Leg(_LastTtBndPt, point));
                            }
                        }

                        _LastPoint = new PointD(point.getUnAdjX(), point.getUnAdjY());
                        _LastTtBndPt = point;
                    }

                    traverseSegments++;
                }

                _LastTravPoint = point;
                break;
            }
            case SideShot:
            {
                if (showPoints)
                {
                    sb.append(String.format("Point %d: %s SideShot off Point %d.%s",
                            point.getPID(), point.isOnBnd() ? " " : "*", lastGpsPtPID, Consts.NewLine));
                }

                if (_LastTtBndPt != null)
                {
                    _Legs.add(new Leg(_LastTtBndPt, point));
                }

                _LastPoint = new PointD(point.getUnAdjX(), point.getUnAdjY());

                if (point.isOnBnd()) {
                    _LastTtBndPt = point;
                }
                break;
            }
            case Quondam:
            {
                QuondamPoint qp = (QuondamPoint)point;

                if (qp.getParentOp() == OpType.Traverse && _LastTtPoint != null) {
                    if (traversing)
                    {
                        closeTraverse(point, sb);
                    }
                    else
                    {
                        if (_LastTtBndPt != null) {
                            _Legs.add(new Leg(_LastTtBndPt, qp.getParentPoint()));
                        }
                    }
                } else {
                    sb.append(getPointSummary(qp.getParentPoint(), true, showPoints));
                }

                if (showPoints) {
                    sb.append(String.format("Point %d: %s Quondam to Point %d.%s", point.getPID(),
                            point.isOnBnd() ? " " : "*", qp.getParentPID(), Consts.NewLine));
                }
                break;
            }
        }

        _LastTtPoint = point;

        return sb.toString();
    }

    private static String getPolygonSummary(TtPolygon polygon, boolean save) throws Exception {
        StringBuilder sb = new StringBuilder();

        if (polygon.getArea() > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format("The polygon area is: %s%.2f Ha (%.0f ac).%s",
                save ? "          " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(polygon.getArea()), 2),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(polygon.getArea()), 0),
                Consts.NewLine));

            sb.append(String.format("The polygon exterior perimeter is: %s%.2f M (%.0f ft).%s%s",
                save ? "     " : "",
                TtUtils.Math.round(polygon.getPerimeter(), 2),
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(polygon.getPerimeter(), Dist.Meters), 0),
                Consts.NewLine, Consts.NewLine));
        }

        if (totalGpsError > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format("GPS Contribution: %s%.5f Ha (%.2f ac)%s",
                save ? "              " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(totalGpsError), 2),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(totalGpsError), 2),
                Consts.NewLine));

            sb.append(String.format("GPS Contribution Ratio of area-error-area to area is: %.2f%%.%s%s",
                TtUtils.Math.round(totalGpsError / polygon.getArea() * 100.0, 2),
                Consts.NewLine, Consts.NewLine));
        }

        if (totalTravError > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format("Traverse Contribution: %s%.2f Ha (%.2f ac)%s",
                save ? "        " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(totalTravError), 2),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(totalTravError), 2),
                Consts.NewLine));

            sb.append(String.format("Traverse Contribution Ratio of area-error-area to area is: %.2f%%.%s%s",
                TtUtils.Math.round(totalTravError / polygon.getArea() * 100.0, 2),
                Consts.NewLine, Consts.NewLine));
        }

        return sb.toString();
    }


    static class Leg {
        private double point1Acc;
        private double point2Acc;
        private double distance;

        public double getPoint1Acc() { return point1Acc; }
        public double getPoint2Acc() { return point2Acc; }
        public double getDistance() {return distance; }

        public Leg(TtPoint point1, TtPoint point2) {
            point1Acc = point1.getAccuracy();
            point2Acc = point2.getAccuracy();
            distance = TtUtils.Math.distance(point1, point2);
        }

        public double getAreaError()
        {
            return distance * (point1Acc + point2Acc) / 2;
        }
    }
}


