package com.usda.fmsc.twotrails.logic;

import com.usda.fmsc.twotrails.TwoTrailsApp;
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

    private static Listener _Listener = null;

    public static boolean isProcessing() {
        return _processing;
    }


    public static AdjustResult adjust(DataAccessLayer dal) {
        return adjust(dal, false, _Listener);
    }

    public static AdjustResult adjust(final DataAccessLayer dal, final boolean updateIndexes) {
        return adjust(dal, updateIndexes, _Listener);
    }

    public static AdjustResult adjust(final DataAccessLayer dal, final boolean updateIndexes, Listener listener) {
        _Listener = listener;

        if (!_processing) {

            _cancelToken = false;

            //check for point issues
            if (dal.getItemCount(TwoTrailsSchema.PolygonSchema.TableName) < 1)
                return AdjustResult.NO_POLYS;
            else {
                for (TtPolygon poly : dal.getPolygons()) {
                    TtPoint p = dal.getFirstPointInPolygon(poly.getCN());

                    if (p != null) {
                        if (p.isTravType() || (p.getOp() == OpType.Quondam &&
                                ((QuondamPoint) p).getParentPoint().isTravType()))
                            return AdjustResult.STARTS_WITH_TRAV_TYPE;
                    }
                }
            }

            _processing = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    AdjustResult result = AdjustResult.ADJUSTING;
                    boolean success = false;

                    AdjustingException.AdjustingError error = AdjustingException.AdjustingError.None;

                    try {
                        if (_Listener != null)
                            _Listener.adjusterStarted();

                        if (updateIndexes)
                            updateIndexes(dal);

                        if (!_cancelToken) {
                            success = adjustPoints(dal);
                        }

                        if (success) {
                            result = AdjustResult.SUCCESSFUL;
                        } else {
                            result = AdjustResult.ERROR;
                        }
                    } catch (AdjustingException ex) {
                        TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "PolygonAdjuster:adjust", ex.getStackTrace());
                        result = AdjustResult.ERROR;
                        error = ex.getErrorType();
                    } catch (Exception ex) {
                        TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "PolygonAdjuster:adjust", ex.getStackTrace());
                        result = AdjustResult.ERROR;
                    } finally {
                        _processing = false;

                        if (_Listener != null) {
                            if (_cancelToken) {
                                result = AdjustResult.CANCELED;
                            }

                            _Listener.adjusterStopped(result, error);
                        }
                    }
                }
            }).start();
        }

        return AdjustResult.ADJUSTING;
    }


    private static void updateIndexes(DataAccessLayer dal) {
        ArrayList<TtPoint> savePoints = new ArrayList<>();

        for (TtPolygon poly : dal.getPolygons()) {
            int index = 0;

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

    private static boolean adjustPoints(DataAccessLayer dal) throws AdjustingException {
        long startTime = System.currentTimeMillis();
        boolean slowTimeTriggered = false;

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

                    if (_Listener != null) {
                        _Listener.adjusterRunningSlow();
                    }

                    slowTimeTriggered = true;
                }
            }

            if (_cancelToken)
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

            dal.updatePoints(points);

            calculateAreaAndPerimeter(dal);
        }

        return true;
    }

    private static void calculateAreaAndPerimeter(DataAccessLayer dal) {
        ArrayList<TtPolygon> polys = dal.getPolygons();

        if (polys != null && polys.size() > 0) {
            for (TtPolygon poly : polys) {
                try {
                    ArrayList<TtPoint> points = dal.getBoundaryPointsInPoly(poly.getCN());

                    if (points.size() > 2) {
                        double perim = 0, area = 0;

                        TtPoint p1, p2;
                        for (int i = 0; i < points.size() - 1; i++) {
                            p1 = points.get(i);
                            p2 = points.get(i + 1);

                            perim += TtUtils.Math.distance(p1, p2);
                            area += ((p2.getAdjX() - p1.getAdjX()) * (p2.getAdjY() + p1.getAdjY()) / 2);
                        }

                        poly.setPerimeterLine(perim);

                        p1 = points.get(points.size() - 1);
                        p2 = points.get(0);
                        perim += TtUtils.Math.distance(p1, p2);
                        area += ((p2.getAdjX() - p1.getAdjX()) * (p2.getAdjY() + p1.getAdjY()) / 2);

                        poly.setPerimeter(perim);
                        poly.setArea(Math.abs(area));
                    } else {
                        poly.setPerimeter(0);
                        poly.setArea(0);
                    }

                    dal.updatePolygon(poly);
                } catch (Exception ex) {
                    TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "SegmentFactory:CalculateAreaAndPerimeter");
                }
            }
        }
    }


    public static void cancel() {
        _cancelToken = true;
    }


    public static void register(Listener listener) {
        _Listener = listener;

        if (_Listener != null && isProcessing()) {
            _Listener.adjusterStarted();
        }
    }

    public static void unregister(Listener listener) {
        if (_Listener == listener)
            _Listener = null;
    }


    public interface Listener {
        void adjusterStarted();
        void adjusterStopped(final AdjustResult result, final AdjustingException.AdjustingError error);
        void adjusterRunningSlow();
    }


}
