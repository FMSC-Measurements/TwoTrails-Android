package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

public class SegmentFactory {
    private final ArrayList<TtPoint> points;
    private final HashMap<String, TtPolygon> polys;

    public SegmentFactory(DataAccessLayer dal) {
        HashMap<String, TtMetadata> meta = dal.getMetadataMap();
        polys = dal.getPolygonsMap();

        Hashtable<String, TtPoint> tmpPoints = new Hashtable<>();

        for (TtPoint p : dal.getPoints()) {
            tmpPoints.put(p.getCN(), p);

            if (p.isTravType()) {
                ((TravPoint)p).setDeclination(meta.get(p.getMetadataCN()).getMagDec());
            }
        }

        QuondamPoint qp;
        for (TtPoint p : tmpPoints.values()) {
            if (p.getOp() == OpType.Quondam) {
                qp = (QuondamPoint)p;
                qp.setParentPoint(tmpPoints.get(qp.getParentCN()));
            }
        }

        points = new ArrayList<>(tmpPoints.values());
        Collections.sort(points);
    }


    public boolean hasNext() {
        return points.size() > 0;
    }

    public Segment next() {
        if (!hasNext())
            return null;

        int index = 0;
        Segment seg = new Segment(polys);
        TtPoint prev = points.get(index);
        index++;

        seg.addPoint(prev);
        boolean finished = false;
        boolean travStarted = false;
        boolean startTypeFound = (prev.isGpsType() || (prev.getOp() == OpType.Quondam &&
                ((QuondamPoint)prev).getParentPoint().isGpsType()));
        boolean savePrev = false;

        TtPoint current;
        String currentPolygon = prev.getPolyCN();
        if (index == points.size())
            points.remove(prev);

        while (index < points.size() && !finished) {
            current = points.get(index);
            if (!currentPolygon.equals(current.getPolyCN())) {
                finished = true;
                points.remove(prev);
                continue;
            }

            OpType tmpOpType = current.getOp();
            TtPoint tmpQp = null;

            while (tmpOpType == OpType.Quondam) {
                if (tmpQp == null)
                    tmpQp = ((QuondamPoint)current).getParentPoint();
                else
                    tmpQp = ((QuondamPoint)tmpQp).getParentPoint();

                tmpOpType = tmpQp.getOp();
            }

            switch (tmpOpType) {
                case GPS:
                case WayPoint:
                case Walk:
                case Take5: {
                    if (seg.getPointCount() == 1) {
                        if (startTypeFound) {
                            finished = true; //Already have a point (and only 1), Segment is finished
                        } else {
                            seg = new Segment(polys); //left over trav point from a sideshot
                            seg.addPoint(current);
                            startTypeFound = true;
                        }
                    } else if (travStarted) { //Or we are at the closing end of a traverse
                        finished = true;
                        seg.addPoint(current);
                    }

                    points.remove(prev);
                    index--;
                    prev = current;
                    break;
                }
                case Traverse: {
                    if (prev.getOp() == OpType.Traverse ||
                            prev.getOp() == OpType.Quondam &&
                                    ((QuondamPoint)prev).getParentOp() == OpType.Traverse) {
                        //finished = true;
                        seg.addPoint(current);

                        points.remove(prev);
                    } else {
                        seg.addPoint(current);

                        if (startTypeFound)
                            travStarted = true;

                        if (!savePrev) {
                            if (points.size() == 1)
                                points.remove(current);
                            points.remove(prev);
                        } else {
                            savePrev = false;
                            index++;
                        }
                    }

                    prev = current;
                    break;
                }
                case SideShot: {
                    if (seg.getPointCount() == 1) { //Only the parent point for this sideshot so far
                        seg.addPoint(current);
                        points.remove(current); // don't remove the prev point, may be needed again to calculate next point
                        finished = true;
                    } else if (prev.getOp() == OpType.Traverse) {
                        seg.addPoint(current);
                        points.remove(current); // don't remove the prev point, may be needed again to calculate next point
                    } else { // skip this point
                        savePrev = true;
                        index++;
                    }
                    break;
                }
            }
        }

        return seg;
    }
}
