package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.QuondamPoint;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.utilities.StringEx;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class HaidLogic {
    private static StringBuilder pointStats;
    private static double travLength, totalTravError, totalGpsError, travGpsError;//, totalError;
    private static boolean traversing;
    private static int traverseSegments, lastGpsPtPID;

    private static TtPoint _LastTtPoint;
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

        totalTravError = totalGpsError = travGpsError = 0; //totalError =
                traversing = false;
        traverseSegments = 0;
    }

    public synchronized static String generatePolyStats(TtPolygon polygon, DataAccessLayer dal, boolean showPoints) {
        StringBuilder sb = new StringBuilder();

        try {
            init(dal);

            List<TtPoint> points = dal.getPointsInPolygon(polygon.getCN());

            if (points.size() > 0) {
                points = TtUtils.filterOut(points, Units.OpType.WayPoint);

                if (points.size() > 0) {
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
                        _Legs.add(new Leg(_LastTtPoint, pt, _Polygons));
                    }

                    for (Leg leg : _Legs) {
                        totalGpsError += leg.getAreaError();
                    }

                    //totalError = totalGpsError + totalTravError;

                    sb.append(getPolygonSummary(polygon, false));
                    sb.append(pointStats);
                } else {
                    sb.append("There are only WayPoints in the polygon.");
                }
            } else {
                sb.append("There are no points in the polygon.");
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "HaidLogic:generatePolyStats");
            return "Error generating polygon info";
        }

        return sb.toString();
    }

    public synchronized static String generateAllPolyStats(DataAccessLayer dal, boolean showPoints, boolean save) {
        StringBuilder sb = new StringBuilder();

        try {
            init(dal);

            for (TtPolygon polygon : _Polygons.values()) {

                List<TtPoint> points = TtUtils.filterOut(dal.getPointsInPolygon(polygon.getCN()), Units.OpType.WayPoint);

                if (points.size() > 0) {
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
                        _Legs.add(new Leg(_LastTtPoint, pt, _Polygons));
                    }

                    for (Leg leg : _Legs) {
                        totalGpsError += leg.getAreaError();
                    }

                    //totalError = totalGpsError + totalTravError;

                    sb.append(getPolygonSummary(polygon, save));
                    sb.append(pointStats);
                } else {
                    sb.append("No Points in Polygon.");
                }

                sb.append("\r\n\r\n");
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
        double travError = travLength / closeError;

        sb.append(String.format("\tTraverse Total Segments: %d%s",
                traverseSegments, Consts.NewLine));
        sb.append(String.format("\tTraverse Total Distance: %.3f feet.%s",
                TtUtils.Convert.toFeetTenths(travLength, Units.Dist.Meters), Consts.NewLine));
        sb.append(String.format("\tTraverse Closing Distance: %.3f feet.%s",
                TtUtils.Convert.toFeetTenths(closeError, Units.Dist.Meters), Consts.NewLine));
        sb.append(String.format("\tTraverse Close Error: 1 part in %d.%s", Math.round(travError), Consts.NewLine));

        totalTravError += (travLength * closeError / 2);

        traversing = false;

        _Legs.add(new Leg(_LastTtPoint, point, _Polygons));

        if(_Legs.size() > 0) {
            double travStartAcc = _Legs.get(0).getPoint1Acc();
            double travEndAcc = _Legs.get(_Legs.size() - 1).getPoint2Acc();
            double diff = travEndAcc - travStartAcc;
            double sumLegLen = 0;
            double legacc;

            for (Leg leg : _Legs)
            {
                sumLegLen += leg.getDistance();
                legacc = (travStartAcc + Math.abs((sumLegLen / travLength) * diff));
                travGpsError += leg.getDistance() * legacc;
            }

            totalGpsError += travGpsError;
        }
    }

    private static String getPointSummary(TtPoint point, boolean fromQuondam, boolean showPoints) throws Exception{
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
                        if (_LastTtPoint != null && _LastTtPoint.isOnBnd()) {
                            _Legs.add(new Leg(_LastTtPoint, point, _Polygons));
                        }
                    }

                    _LastPoint = new PointD(point.getUnAdjX(), point.getAdjY());
                }

                if (!fromQuondam && showPoints) {
                    sb.append(String.format("Point %d: %s %s- ", point.getPID(), point.isOnBnd() ? " " : "*",
                            point.getOp().toString()));
                    sb.append(String.format("Accuracy is %.3f meters.%s", point.getAccuracy(), Consts.NewLine)); // TtUtils.getPointAcc(point, _Polygons)
                }
                break;
            }
            case Traverse:
            {
                if (_LastTtPoint != null) {
                    if(point.isOnBnd()) {
                        if (traversing) {
                            travLength += TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y, point.getUnAdjX(), point.getUnAdjY());

                            if (_LastTtPoint.isOnBnd()) {
                                _Legs.add(new Leg(_LastTtPoint, point, _Polygons));
                            }
                        } else {
                            traverseSegments = 0;
                            travLength = TtUtils.Math.distance(_LastPoint.X, _LastPoint.Y, point.getUnAdjX(), point.getUnAdjY());
                            traversing = true;

                            if (showPoints) {
                                sb.append(String.format("Traverse Start:%s", Consts.NewLine));
                            }

                            _Legs = new ArrayList<>();
                            if (_LastTtPoint.isOnBnd()) {
                                _Legs.add(new Leg(_LastTtPoint, point, _Polygons));
                            }
                        }
                        _LastPoint = new PointD(point.getUnAdjX(), point.getUnAdjY());

                    }

                    traverseSegments++;
                }
                break;
            }
            case SideShot:
            {
                if (showPoints)
                {
                    sb.append(String.format("Point %d: %s SideShot off Point %d.%s",
                            point.getPID(), point.isOnBnd() ? " " : "*", lastGpsPtPID, Consts.NewLine));
                }

                if (_LastTtPoint != null && _LastTtPoint.isOnBnd())
                {
                    _Legs.add(new Leg(_LastTtPoint, point, _Polygons));
                }

                _LastPoint = new PointD(point.getUnAdjX(), point.getUnAdjY());
                break;
            }
            case Quondam:
            {
                QuondamPoint qp = (QuondamPoint)point;

                if (qp.getParentOp() == Units.OpType.Traverse && _LastTtPoint != null) {
                    if (traversing)
                    {
                        closeTraverse(point, sb);
                    }
                    else
                    {
                        if (_LastTtPoint.isOnBnd()) {
                            _Legs.add(new Leg(_LastTtPoint, qp.getParentPoint(), _Polygons));
                        }

                        _LastTtPoint = point;
                    }
                } else {
                    sb.append(getPointSummary(qp.getParentPoint(), true, showPoints));
                }

                if (showPoints)
                {
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

        //sb.append(String.format("Polygon ID: %s%s", polygon.getName(), Consts.NewLine));

        if (!StringEx.isEmpty(polygon.getDescription()))
            sb.append(String.format("Description: %s\r\n\r\n", polygon.getDescription()));

        if (polygon.getArea() > 0)
        {
            sb.append(String.format("The polygon area is: %s%.3f Ha (%.3f ac).%s",
                            save ? "          " : "",
                            TtUtils.Convert.metersSquaredToHa(polygon.getArea()),
                            TtUtils.Convert.metersSquaredToAcres(polygon.getArea()),
                            Consts.NewLine));

            sb.append(String.format("The polygon exterior perimeter is: %s%.3f M (%.3f ft).%s",
                            save ? "     " : "",
                            polygon.getPerimeter(),
                            TtUtils.Convert.toFeetTenths(polygon.getPerimeter(), Units.Dist.Meters),
                            Consts.NewLine));
        }

        /*
        if (totalError > 0) {
            sb.append(String.format("The polygon area-error is: %s%.5f Ha (%.3f ac)%s",
                    save ? "    " : "",
                    TtUtils.Convert.metersSquaredToHa(totalError),
                    TtUtils.Convert.metersSquaredToAcres(totalError),
                    Consts.NewLine));

            sb.append(String.format("Ratio of area-error-area to area is: %.2f%%.%s",
                    totalError / polygon.getArea() * 100.0,
                    Consts.NewLine));

        }
        */

        if (totalGpsError > 0) {
            sb.append(String.format("GPS Contribution: %s%.5f Ha (%.3f ac)%s",
                    save ? "              " : "",
                    TtUtils.Convert.metersSquaredToHa(totalGpsError),
                    TtUtils.Convert.metersSquaredToAcres(totalGpsError),
                    Consts.NewLine));

            sb.append(String.format("GPS Contribution Ratio of area-error-area to area is: %.2f%%.%s",
                    totalGpsError / polygon.getArea() * 100.0,
                    Consts.NewLine));
        }

        if (totalTravError > 0) {
            sb.append(String.format("Traverse Contribution: %s%.5f Ha (%.3f ac)%s",
                    save ? "        " : "",
                    TtUtils.Convert.metersSquaredToHa(totalTravError),
                    TtUtils.Convert.metersSquaredToAcres(totalTravError),
                    Consts.NewLine));

            sb.append(String.format("Traverse Contribution Ratio of area-error-area to area is: %.2f%%.%s",
                    totalTravError / polygon.getArea() * 100.0,
                    Consts.NewLine));
        }

        sb.append(Consts.NewLine);
        return sb.toString();
    }
}


class Leg {
    private double point1Acc;
    private double point2Acc;
    private double distance;

    public double getPoint1Acc() { return point1Acc; }
    public double getPoint2Acc() { return point2Acc; }
    public double getDistance() {return distance; }

    public Leg(TtPoint point1, TtPoint point2, HashMap<String, TtPolygon> polygons) {
        point1Acc = point1.getAccuracy();// TtUtils.getPointAcc(point1, polygons);
        point2Acc = point2.getAccuracy();// TtUtils.getPointAcc(point2, polygons);
        distance = TtUtils.Math.distance(point1, point2);
    }

    public double getAreaError()
    {
        return distance * (point1Acc + point2Acc) / 2;
    }
}