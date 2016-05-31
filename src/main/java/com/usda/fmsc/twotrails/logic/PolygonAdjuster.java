package com.usda.fmsc.twotrails.logic;


import android.content.Context;

import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.data.TwoTrailsSchema;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.Hashtable;

public class PolygonAdjuster {
    private static final long ADJUSTING_SLOW_TIME = 30000;

    public enum AdjustResult {
        ADJUSTING,
        STARTS_WITH_TRAV_TYPE,
        NO_POLYS,
        BAD_POINT,
        ERROR,
        SUCCESSFUL,
        CANCELED
    }

    private static boolean _processing = false;
    private static boolean _cancelToken = false;

    public static boolean isProcessing() {
        return _processing;
    }


    public static AdjustResult adjust(DataAccessLayer dal, Context ctx) {
        return adjust(dal, ctx, false);
    }

    public static AdjustResult adjust(final DataAccessLayer dal, final Context ctx, final boolean updateIndexes) {
        if(_processing)
            return AdjustResult.ADJUSTING;

        _cancelToken = false;

        //check for point issues
        if(dal.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) < 1)
            return AdjustResult.NO_POLYS;
        else {
            for(TtPolygon poly : dal.getPolygons()) {
                TtPoint p = dal.getFirstPointInPolygon(poly.getCN());

                if(p != null) {
                    if(p.isTravType() || (p.getOp() == OpType.Quondam &&
                            ((QuondamPoint)p).getParentPoint().isTravType()))
                        return AdjustResult.STARTS_WITH_TRAV_TYPE;
                }
                /* no point (empty polygon)
                else {
                    return AdjustResult.BAD_POINT;
                }
                */
            }
        }

        _processing = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                AdjustResult result = AdjustResult.ADJUSTING;
                boolean success = false;
                Listener listener = null;

                if(ctx instanceof Listener)
                    listener = (Listener)ctx;

                try {
                    if(listener != null)
                        listener.adjusterStarted();

                    if(updateIndexes)
                        updateIndexes(dal);

                    if(!_cancelToken) {
                        success = adjustPoints(dal, listener);
                    }

                    if (success) {
                        result = AdjustResult.SUCCESSFUL;
                    } else {
                        result = AdjustResult.ERROR;
                    }
                } catch (Exception ex) {
                    TtUtils.TtReport.writeError(ex.getMessage(), "PolygonAdjuster:adjust");
                    result = AdjustResult.ERROR;
                } finally {
                    _processing = false;

                    if(listener != null) {
                        if (_cancelToken) {
                            result = AdjustResult.CANCELED;
                        }

                        listener.adjusterStopped(result);
                    }
                }
            }
        }).start();

        return AdjustResult.ADJUSTING;
    }


    private static void updateIndexes(DataAccessLayer dal) {
        ArrayList<TtPoint> savePoints = new ArrayList<>();

        for (TtPolygon poly : dal.getPolygons()) {
            long index = 0;

            for(TtPoint point : dal.getPointsInPolygon(poly.getCN())) {
                if (point.getIndex() != index)
                {
                    point.setIndex(index);
                    savePoints.add(point);
                }

                index++;
            }
        }

        dal.updatePoints(savePoints, savePoints);
    }

    private static boolean adjustPoints(DataAccessLayer dal, Listener listener) {
        long startTime = System.currentTimeMillis();
        boolean slowTimeTriggered = false;

        try {
            SegmentFactory sf = new SegmentFactory(dal);

            if(sf.hasNext()) {
                SegmentList sl = new SegmentList();
                ArrayList<Segment> adjusted = new ArrayList<>();

                while (sf.hasNext()) {
                    sl.addSegment(sf.next());
                }

                Segment seg;
                while (sl.hasNext()) {
                    if(_cancelToken)
                        return false;

                    seg = sl.next();

                    if (seg.calculate()) {
                        seg.adjust();
                        adjusted.add(seg);
                    } else {
                        seg.setWeight(seg.getWeight() - 1);
                        sl.addSegment(seg);
                    }

                    if (!slowTimeTriggered && System.currentTimeMillis() - startTime > ADJUSTING_SLOW_TIME) {

                        if (listener != null) {
                            listener.adjusterRunningSlow();
                        }

                        slowTimeTriggered = true;
                    }
                }

                if(_cancelToken)
                    return false;

                TtPoint p;
                Hashtable<String, TtPoint> pointsTable = new Hashtable<>();

                for (int s = 0; s < adjusted.size(); s++)
                {
                    for (int i = 0; i < adjusted.get(s).getPointCount(); i++)
                    {
                        p = adjusted.get(s).get(i);

                        if (!pointsTable.containsKey(p.getCN()))
                            pointsTable.put(p.getCN(), p);
                    }
                }

                ArrayList<TtPoint> points = new ArrayList<>(pointsTable.values());

                /*
                //set sideshot accuracies
                if(TtUtils.filterOnly(points, OpType.SideShot).size() > 0) {
                    Collections.sort(points);
                    HashMap<String, TtPolygon> polys = dal.getPolygonsMap();
                    TtPoint currPoint, lastPoint = points.get(0);

                    for (int i = 0; i < points.size(); i++)
                    {
                        currPoint = points.get(i);

                        if (currPoint.getOp() == OpType.SideShot)
                        {
                            ((TravPoint)currPoint).setAccuracy(TtUtils.getPointAcc(lastPoint, polys));
                        }

                        lastPoint = currPoint;
                    }
                }
                */

                dal.updatePoints(points);

                calculateAreaAndPerimeter(dal);
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "PolygonAdjuster:adjust");
            return false;
        }

        return true;
    }

    private static void calculateAreaAndPerimeter(DataAccessLayer dal) {
        ArrayList<TtPolygon> polys = dal.getPolygons();

        if (polys != null && polys.size() > 0) {
            for (TtPolygon poly : polys) {
                try {
                    TtPolygon newPoly = new TtPolygon(poly);
                    ArrayList<TtPoint> points = dal.getBoundaryPointsInPoly(poly.getCN());

                    if (points.size() > 2) {
                        double perim = 0, area = 0;

                        points.add(points.get(0));
                        TtPoint p1, p2;
                        for (int i = 0; i < points.size() - 1; i++)
                        {
                            p1 = points.get(i);
                            p2 = points.get(i + 1);

                            perim += TtUtils.Math.distance(p1, p2);
                            area += ((p2.getAdjX() - p1.getAdjX()) * (p2.getAdjY() + p1.getAdjY()) / 2);
                        }

                        newPoly.setPerimeter(perim);
                        newPoly.setArea(Math.abs(area));
                    } else {
                        newPoly.setPerimeter(0);
                        newPoly.setArea(0);
                    }

                    dal.updatePolygon(newPoly);
                } catch (Exception ex) {
                    TtUtils.TtReport.writeError(ex.getMessage(), "SegmentFactory:CalculateAreaAndPerimeter");
                }
            }
        }
    }


    public static void cancel() {
        _cancelToken = true;
    }

    public interface Listener {
        void adjusterStarted();
        void adjusterStopped(final AdjustResult result);
        void adjusterRunningSlow();
    }
}
