package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.objects.points.QuondamPoint;
import com.usda.fmsc.twotrails.objects.points.TravPoint;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

public class SegmentFactoryRedux {
    private ArrayList<TtPoint> points;
    private HashMap<String, TtPolygon> polys;

    private int index = 0;

    public SegmentFactoryRedux(DataAccessLayer dal) {
        HashMap<String, TtMetadata> meta = dal.getMetadataMap();
        polys = dal.getPolygonsMap();

        ArrayList<TtPoint> tmpWayPoints = new ArrayList<>();
        Hashtable<String, TtPoint> tmpPoints = new Hashtable<>();

        for (TtPoint p : dal.getPoints()) {
            if(p.getOp() == OpType.WayPoint) {
                p.calculatePoint(polys.get(p.getPolyCN()));
                p.adjustPoint();
                tmpWayPoints.add(p);
            } else {
                tmpPoints.put(p.getCN(), p);

                if(p.isTravType()) {
                    ((TravPoint)p).setDeclination(meta.get(p.getMetadataCN()).getMagDec());
                }
            }
        }

        if (tmpWayPoints.size() > 0) {
            dal.updatePoints(tmpWayPoints, tmpWayPoints);
        }

        QuondamPoint qp;
        for (TtPoint p : tmpPoints.values())
        {
            if(p.getOp() == OpType.Quondam) {
                qp = (QuondamPoint)p;
                qp.setParentPoint(tmpPoints.get(qp.getParentCN()));
            }
        }

        points = new ArrayList<>(tmpPoints.values());
        Collections.sort(points);
    }


    public boolean hasNext() {
        return index < points.size();
    }

    public Segment next() {
        if (!hasNext())
            return null;

        Segment seg = new Segment(polys);

        seg.addPoint(points.get(index == points.size() ? index - 1 : index));

        index++;

        TtPoint current;
        OpType op;

        while (true)
        {
            if (index >= points.size())
                return seg;

            TtPoint prev = points.get(index - 1);
            current = points.get(index);
            op = getOpAtBase(current);

            switch (op)
            {
                case GPS:
                case WayPoint:
                case Walk:
                case Take5: {
                    if (isGpsAtBase(prev) || prev.getOp() == OpType.SideShot) {
                        return seg;
                    } else if (prev.getOp() == OpType.Traverse) {
                        seg.addPoint(current);
                        index++;
                        return seg;
                    }
                    break;
                }
                case Traverse:
                case SideShot: {
                    seg.addPoint(current);
                    index++;
                    break;
                }
            }
        }
    }

    private OpType getOpAtBase(TtPoint point) {
        if (point.getOp() == OpType.Quondam)
            return ((QuondamPoint)point).getParentOp();
        return  point.getOp();
    }

    private boolean isGpsAtBase(TtPoint point) {
        if (point.getOp() == OpType.Quondam)
            return ((QuondamPoint)point).getParentOp().isGpsType();
        return  point.getOp().isGpsType();
    }
}
