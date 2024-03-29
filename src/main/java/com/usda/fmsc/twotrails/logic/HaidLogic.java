package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class HaidLogic {
    private static final String INFINITE_SYMBOL = "\u221E";

    private double travLength, totalTravError, totalGpsError, polyPerim;
    private boolean traversing;
    private int traverseSegments, lastGpsPtPID;

    private TtPoint _LastTtPoint, _LastTtBndPt;
    private PointD _LastPoint;

    private final List<Leg> _Legs;
    private final HashMap<String, TtPolygon> _Polygons;

    private final TwoTrailsApp app;

    public HaidLogic(TwoTrailsApp app) {
        this.app = app;

        _Legs = new ArrayList<>();

        _Polygons = new HashMap<>();
        for (TtPolygon poly : app.getDAL().getPolygons()) {
            _Polygons.put(poly.getCN(), poly);
        }

        totalTravError = totalGpsError = 0;
        traversing = false;
        traverseSegments = 0;

        _LastTtPoint = _LastTtBndPt = null;
    }

    public synchronized String generatePolyStats(TtPolygon polygon, boolean showPoints, boolean save) {
        StringBuilder sbInfo = new StringBuilder();
        StringBuilder pointStats = new StringBuilder();

        _Legs.clear();
        totalTravError = totalGpsError = polyPerim = 0;
        traversing = false;
        traverseSegments = 0;

        _LastTtPoint = _LastTtBndPt = null;

        try {
            List<TtPoint> points = app.getDAL().getPointsInPolygon(polygon.getCN());

            if (save) {
                sbInfo.append(String.format(Locale.getDefault(), "Polygon Name: %s%s%s", polygon.getName(), Consts.NewLine, Consts.NewLine));
            }

            if (!StringEx.isEmpty(polygon.getDescription())) {
                sbInfo.append(String.format(Locale.getDefault(), "Description: %s%s%s", polygon.getDescription(), Consts.NewLine, Consts.NewLine));
            }

            if (points.size() > 0) {
                if (points.size() > 2) {
                    points = TtUtils.Points.filterOut(points, OpType.WayPoint);


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

                            if (!pt.sameAdjLocation(_LastTtBndPt)) {
                                _Legs.add(new Leg(_LastTtBndPt, pt));
                            }

                            for (Leg leg : _Legs) {
                                totalGpsError += leg.getAreaError();
                                polyPerim += leg.getDistance();
                            }

                            sbInfo.append(getPolygonSummary(polygon, save));
                            sbInfo.append(pointStats);
                        } else {
                            sbInfo.append("There are not enough valid points in the polygon.");
                        }
                    } else {
                        sbInfo.append("There are only WayPoints in the polygon.");
                    }
                } else {
                    sbInfo.append("There are not enough points in the polygon.");
                }
            } else {
                sbInfo.append("There are no points in the polygon.");
            }
        } catch (Exception ex) {
            app.getReport().writeError(ex.getMessage(), "HaidLogic:generatePolyStats", ex.getStackTrace());
            return "Error generating polygon info";
        }

        if (save) {
            sbInfo.append(String.format(Locale.getDefault(), "%s%s- - - - - - - - - - - - - - - - - - - -", Consts.NewLine, Consts.NewLine));
        }

        return sbInfo.toString();
    }

    public synchronized String generateAllPolyStats(boolean showPoints, boolean save) {
        StringBuilder sb = new StringBuilder();

        try {
            for (TtPolygon polygon : _Polygons.values()) {

                sb.append(generatePolyStats(polygon, showPoints, save));

                sb.append(String.format(Locale.getDefault(), "%s%s", Consts.NewLine, Consts.NewLine));
            }
        } catch (Exception ex) {
            app.getReport().writeError(ex.getMessage(), "HaidLogic:generateAllPolyStats");
            return "Error generating polygon info";
        }

        return sb.toString();
    }

    
    private void closeTraverse(TtPoint point, StringBuilder sb) {
        double closeError = TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y,
                point.getUnAdjX(), point.getUnAdjY());

        double travError = closeError < Consts.Minimum_Point_Accuracy ? Double.POSITIVE_INFINITY : travLength / closeError;

        sb.append(String.format(Locale.getDefault(), "\tTraverse Total Segments: %d%s",
                traverseSegments, Consts.NewLine));
        sb.append(String.format(Locale.getDefault(), "\tTraverse Total Distance: %.2f feet.%s",
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(travLength, Dist.Meters), 2), Consts.NewLine));
        sb.append(String.format(Locale.getDefault(), "\tTraverse Closing Distance: %.2f feet.%s",
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(closeError, Dist.Meters), 2), Consts.NewLine));
        sb.append(String.format(Locale.getDefault(), "\tTraverse Close Error: 1 part in %s.%s",
                Double.isInfinite(travError) ? INFINITE_SYMBOL : StringEx.toStringRound(travError, 2),
                Consts.NewLine));


        totalTravError += (travLength * closeError / 2);

        traversing = false;
    }

    private TtPoint _LastTravPoint = null;

    private String getPointSummary(TtPoint point, boolean fromQuondam, boolean showPoints) {
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
                        if (_LastTtBndPt != null) {
                            _Legs.add(new Leg(_LastTtBndPt, point));
                        }
                    }

                    _LastPoint = new PointD(point.getUnAdjX(), point.getAdjY());
                    _LastTtBndPt = point;
                }

                if (!fromQuondam && showPoints) {
                    sb.append(String.format(Locale.getDefault(), "Point %d: %s %s- ", point.getPID(), point.isOnBnd() ? " " : "*",
                            point.getOp().toString()));
                    sb.append(String.format(Locale.getDefault(), "Accuracy is %.3f meters.%s", point.getAccuracy(), Consts.NewLine));
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
                                sb.append(String.format(Locale.getDefault(), "Traverse Start:%s", Consts.NewLine));
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
                    sb.append(String.format(Locale.getDefault(), "Point %d: %s SideShot off Point %d.%s",
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
                    sb.append(String.format(Locale.getDefault(), "Point %d: %s Quondam to Point %d.%s", point.getPID(),
                            point.isOnBnd() ? " " : "*", qp.getParentPID(), Consts.NewLine));
                }
                break;
            }
        }

        _LastTtPoint = point;

        return sb.toString();
    }

    private String getPolygonSummary(TtPolygon polygon, boolean save) {
        StringBuilder sb = new StringBuilder();

        if (polygon.getArea() > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format(Locale.getDefault(), "The polygon area is: %s%.2f Ha (%.0f ac).%s",
                save ? "          " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(polygon.getArea()), 2),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(polygon.getArea()), 2),
                Consts.NewLine));

            sb.append(String.format(Locale.getDefault(), "The polygon exterior perimeter is: %s%.2f M (%.0f ft).%s%s",
                save ? "     " : "",
                TtUtils.Math.round(polyPerim, 2),
                TtUtils.Math.round(TtUtils.Convert.toFeetTenths(polyPerim, Dist.Meters), 2),
                Consts.NewLine, Consts.NewLine));
        }

        if (totalGpsError > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format(Locale.getDefault(), "GPS Contribution: %s%.4f Ha (%.0f ac)%s",
                save ? "              " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(totalGpsError), 4),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(totalGpsError), 2),
                Consts.NewLine));

            sb.append(String.format(Locale.getDefault(), "GPS Contribution Ratio of area-error-area to area is: %.2f%%.%s%s",
                TtUtils.Math.round(totalGpsError / polygon.getArea() * 100.0, 2),
                Consts.NewLine, Consts.NewLine));
        }

        if (totalTravError > Consts.Minimum_Point_Accuracy) {
            sb.append(String.format(Locale.getDefault(), "Traverse Contribution: %s%.2f Ha (%.0f ac)%s",
                save ? "        " : "",
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToHa(totalTravError), 2),
                TtUtils.Math.round(TtUtils.Convert.metersSquaredToAcres(totalTravError), 2),
                Consts.NewLine));

            sb.append(String.format(Locale.getDefault(), "Traverse Contribution Ratio of area-error-area to area is: %.2f%%.%s%s",
                TtUtils.Math.round(totalTravError / polygon.getArea() * 100.0, 2),
                Consts.NewLine, Consts.NewLine));
        }

        return sb.toString();
    }


    static class Leg {
        private final double point1Acc;
        private final double point2Acc;
        private final double distance;

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


