package com.usda.fmsc.twotrails.logic;


import com.usda.fmsc.twotrails.objects.QuondamPoint;
import com.usda.fmsc.twotrails.objects.SideShotPoint;
import com.usda.fmsc.twotrails.objects.TravPoint;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.Units;
import com.usda.fmsc.twotrails.Units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Segment {
    private List<TtPoint> points;
    private int weight = -1;
    private HashMap<String, TtPolygon> polys;

    private boolean calculated;
    public boolean isCalculated() { return calculated; }

    private boolean adjusted;
    public boolean isAdjusted() { return adjusted; }

    public int getPointCount() { return points.size(); }


    public int getWeight() {
        if(weight < 0)
            weight = calculateWeight();
        return weight;
    }

    public void setWeight(int weight) {
        if (weight > -1) {
            this.weight = weight;
        } else {
            this.weight = 0;
        }
    }


    public TtPoint get(int location) { return points.get(location); }


    public Segment(HashMap<String, TtPolygon> polys) {
        calculated = true;
        points = new ArrayList<>();
        this.polys = polys;
    }


    public void addPoint(TtPoint point) {
        points.add(point);
        calculated = false;
        weight = -1;
    }


    public boolean adjust() {
        int len = points.size();

        if (len == 0) {
            adjusted = true;
        } else if (!calculated) {
            adjusted = false;
        } else {
            TtPoint startPoint = points.get(0);

            if (len == 1)
                return startPoint.adjustPoint();

            if(!startPoint.isAdjusted())
                startPoint.adjustPoint();

            TtPoint endPoint = points.get(len - 1);

            if (endPoint.getOp() == Units.OpType.SideShot) {
                if (!endPoint.isAdjusted()) {
                    adjusted = adjustSideShot(startPoint, (SideShotPoint)points.get(1));
                }
            } else if (len > 2 && points.get(1).getOp() == Units.OpType.Traverse) {
                adjusted = adjustTraverse();
            } else {
                for (TtPoint point : points) {
                    point.adjustPoint();
                }
            }
        }

        return adjusted;
    }

    public boolean adjustTraverse() {
        double lastX, lastY, lastZ, currX, currY, currZ, deltaX, deltaY, deltaZ, deltaDistance, leg_length,
                adj_perimeter, deltaX_Correction, deltaY_Correction, deltaZ_Correction, adjX, adjY, adjZ;

        int size = points.size();
        TtPoint tmpPoint = points.get(size -1);

        //X,Y of the final point of the travers, should be the closing point if it exists
        lastX = tmpPoint.getUnAdjX(); //GPS Coords at the end of the traverse
        lastY = tmpPoint.getUnAdjY();
        lastZ = tmpPoint.getUnAdjZ();

        adj_perimeter = getAdjustmentPerimeter();

        //No adjustment if there isn't a closing point
        if (tmpPoint.getOp() == Units.OpType.Traverse) {
            deltaX = 0;
            deltaY = 0;
            deltaZ = 0;
        } else {
            tmpPoint = points.get(size - 2);

            deltaX = lastX - tmpPoint.getUnAdjX();
            deltaY = lastY - tmpPoint.getUnAdjY();
            deltaZ = lastZ - tmpPoint.getUnAdjZ();
        }

        //deltaDistance = trav closure error
        deltaDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        deltaX_Correction = deltaX / adj_perimeter;
        deltaY_Correction = deltaY / adj_perimeter;
        deltaZ_Correction = deltaZ / adj_perimeter;

        tmpPoint = points.get(0);

        //Loop through the points and apply the correction
        lastX = tmpPoint.getUnAdjX();
        lastY = tmpPoint.getUnAdjY();
        lastZ = tmpPoint.getUnAdjZ();
        leg_length = 0;


        double accuracy = tmpPoint.getAccuracy();// TtUtils.getPointAcc(tmpPoint, polys);
        double accuracyEnd = points.get(points.size() - 1).getAccuracy();// TtUtils.getPointAcc(points.get(points.size() - 1), polys);
        double accuracyIncrement = (accuracyEnd - accuracy) / (points.size() - 1);


        TtPoint currPoint;
        for (int i = 0; i < size; i++) {
            currPoint = points.get(i);

            if (i == size - 1 && currPoint.getOp() == Units.OpType.Quondam)
                break;

            if (currPoint.isGpsType()) {
                if (!currPoint.isAdjusted())
                    currPoint.adjustPoint();
                continue;
            }

            currX = currPoint.getUnAdjX();
            currY = currPoint.getUnAdjY();
            currZ = currPoint.getUnAdjZ();

            leg_length += Math.sqrt(Math.pow(currX - lastX, 2) + Math.pow(currY - lastY, 2)); // add lastZ for 3D length "+ Math.pow(currZ - lastZ, 2")
            adjX = leg_length * deltaX_Correction + currX;
            adjY = leg_length * deltaY_Correction + currY;
            adjZ = leg_length * deltaZ_Correction + currZ;

            currPoint.setAdjX(adjX);
            currPoint.setAdjY(adjY);
            currPoint.setAdjZ(adjZ);

            accuracy += accuracyIncrement;

            if (currPoint.isTravType())
            {
                (currPoint).setAccuracy(accuracy);
                ((TravPoint)currPoint).setAdjusted();
            }

            lastX = currX;
            lastY = currY;
            lastZ = currZ;
        }

        return true;
    }

    public boolean adjustSideShot(TtPoint prevPoint, SideShotPoint ss) {
        ss.setAccuracy(prevPoint.getAccuracy());
        return ss.adjustPoint(prevPoint);
    }

    public double getAdjustmentPerimeter() {
        if (points.size() < 1)
            return 0;
        double adjustment_perim = 0;

        TtPoint currPoint = points.get(0);

        double currX, currY, lastX, lastY;
        lastX = currPoint.getUnAdjX();
        lastY = currPoint.getUnAdjY();

        //loop through all the points, stop if we get to the closing gps
        for (int i = 1; i < points.size(); i++) {
            currPoint = points.get(i);

            if (currPoint.getOp() == OpType.Traverse) {
                currX = currPoint.getUnAdjX();
                currY = currPoint.getUnAdjY();

                adjustment_perim += Math.sqrt(Math.pow(currX - lastX, 2) + Math.pow(currY - lastY, 2));

                lastX = currX;
                lastY = currY;
            }
        }

        return adjustment_perim;
    }


    public boolean calculate() {
        if (points.size() == 0) {
            calculated = true;
            return true;
        }

        TtPoint firstPoint = points.get(0);

        boolean success = firstPoint.isCalculated();
        switch (firstPoint.getOp()) {
            case GPS:
            case WayPoint:
            case Take5:
            case Walk:
            case Quondam: {
                if (!success)
                    success = firstPoint.calculatePoint(polys.get(firstPoint.getPolyCN()));
                break;
            }
            case Traverse: {
                //Must be a trav with a SideShot off of it. Must already be calculated.
                if (!success)
                    return false;
                break;
            }
            default: {
                throw new RuntimeException("Sideshots can not come first");
            }
        }

        int index = 1;
        while (index < points.size()) {
            TtPoint p = points.get(index);

            switch (p.getOp()) {
                case GPS:
                case WayPoint:
                case Walk:
                case Take5:
                case Quondam: {
                    success &= p.calculatePoint(polys.get(p.getPolyCN()));
                    break;
                }
                case Traverse:
                case SideShot: {
                    success &= p.calculatePoint(polys.get(p.getPolyCN()), points.get(index - 1));
                    break;
                }
            }

            index++;
        }

        calculated = success;
        return success;
    }

    private int calculateWeight() {
        int count = points.size();
        TtPoint fPoint = points.get(0);


        if (count < 3) {
            if (fPoint.getOp().isGpsType()) {
                return 10;
            } else if (fPoint.getOp() == OpType.Quondam) {
                if (((QuondamPoint)fPoint).getParentOp().isGpsType()) {
                    return 8;
                }

                return 4;
            }

            return 3;
        } else {
            if (TtUtils.allPointsAreQndmType(points)) {
                return 2;
            } else {
                TtPoint lPoint = points.get(points.size() - 1);

                boolean fGPS = fPoint.getOp().isGpsType(), lGPS = lPoint.getOp().isGpsType();

                if (fGPS && lGPS || fGPS && (lPoint.getOp() == OpType.Quondam && ((QuondamPoint)lPoint).getParentCN().equals(fPoint.getCN()))) {
                    return 9;
                } else if ((fGPS || (fPoint.getOp() == OpType.Quondam &&
                        ((QuondamPoint)fPoint).getParentOp().isGpsType())) &&

                        (lGPS || (lPoint.getOp() == OpType.Quondam &&
                                ((QuondamPoint)lPoint).getParentOp().isGpsType()))) {
                    return 7;
                } else {
                    if (lPoint.getOp().isTravType())
                        return 3;
                    else
                        return 4;   //is quondam
                }
            }
        }
    }

    @Override
    public String toString() {
        if (points != null && points.size() == 0) {
            TtPoint tmpPoint = points.get(0);

            if (points.size() == 1)
                return String.format("%s:%s", tmpPoint.getOp(), tmpPoint.getPID());
            else
                return String.format("%s to %s", tmpPoint.getPID(), points.get(points.size() - 1).getPID());
        }
        else
            return "No Points";
    }
}