package com.usda.fmsc.twotrails.logic;

import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.security.InvalidParameterException;
import java.util.List;

public class PointNamer {
    public static int nameFirstPoint(TtPolygon parentPolygon) {
        return parentPolygon.getPointStartIndex();
    }

    public static int namePoint(TtPoint previousPoint, TtPolygon parentPolygon) {
        if (previousPoint == null)
            return nameFirstPoint(parentPolygon);

        return namePoint(previousPoint, parentPolygon.getIncrementBy());
    }

    public static int namePoint(TtPoint previousPoint, int incrementBy) {
        if (incrementBy < 1)
            throw new InvalidParameterException("Invalid Increment Value");

        if (previousPoint == null)
            throw new NullPointerException("Point is null");

        int rem = incrementBy - (previousPoint.getPID() % incrementBy);

        if (rem > 0)
            return (previousPoint.getPID() + rem);

        return (previousPoint.getPID() + incrementBy);
    }

    public static int nameInsertPoint(TtPoint previousPoint) {
        if (previousPoint == null)
            throw new NullPointerException("Point is null");

        return (previousPoint.getPID() + 1);
    }

    public static void renamePoints(List<TtPoint> points, TtPolygon parentPoly) {
        renamePoints(points, parentPoly.getPointStartIndex(), parentPoly.getIncrementBy());
    }

    public static void renamePoints(List<TtPoint> points, int pointStartIndex, int incrementBy) {
        if (incrementBy < 1)
            throw new InvalidParameterException("Invalid Increment Value");

        if (pointStartIndex < 0)
            throw new InvalidParameterException("Invalid Point Start Index Value");

        if (points != null && points.size() > 0)
        {
            int pid = pointStartIndex;

            for (int i = 0; i < points.size(); i++)
            {
                points.get(i).setPID(pid);
                pid += incrementBy;
            }
        }
    }
}
