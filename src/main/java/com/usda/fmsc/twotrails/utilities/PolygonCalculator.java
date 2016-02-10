package com.usda.fmsc.twotrails.utilities;

import com.usda.fmsc.twotrails.objects.PointD;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class PolygonCalculator {
    int polyCorners;
    double[] polyX, polyY, constant, multiple;

    public PolygonCalculator(List<PointD> points) {
        if (points == null || points.size() < 3) {
            throw new InvalidParameterException("Input points are not a polygon.");
        }

        PointD current = points.get(0);
        List<PointD> nPoints = new ArrayList<>();
        nPoints.add(current);

        PointD temp = current;
        for (int i = 1; i < points.size(); i++) {
            current = points.get(i);
            if (!current.equals(temp)) {
                nPoints.add(current);
            }
            temp = current;
        }

        if (nPoints.size() < 3) {
            throw new InvalidParameterException("Input points are not a polygon.");
        }


        polyCorners = nPoints.size();
        polyX = new double[polyCorners];
        polyY = new double[polyCorners];
        constant = new double[polyCorners];
        multiple = new double[polyCorners];

        for (int i = 0; i < polyCorners; i++) {
            temp = nPoints.get(i);
            polyX[i] = temp.X;
            polyY[i] = temp.Y;
        }

        int i, j = polyCorners - 1 ;

        for (i = 0; i < polyCorners; i++) {
            if (polyY[j] == polyY[i]) {
                constant[i] = polyX[i];
                multiple[i] = 0;
            } else {
                constant[i] = polyX[i] -
                        (polyY[i] * polyX[j]) / (polyY[j] - polyY[i]) +
                        (polyY[i] * polyX[i]) / (polyY[j] - polyY[i]);

                multiple[i] = (polyX[j] - polyX[i]) / (polyY[j] - polyY[i]);
            }

            j = i;
        }
    }

    public boolean pointInPolygon(double x, double y) {
        int i, j = polyCorners - 1;
        boolean oddNodes = false;

        for (i = 0; i < polyCorners; i++) {
            if ((polyY[i] < y && polyY[j] >= y || polyY[j] < y && polyY[i] >= y)) {
                oddNodes ^= (y * multiple[i] + constant[i] < x);
            }
            j = i;
        }

        return oddNodes;
    }


    public Boundaries getPointBoundaries() {
        double top, bottom, left, right, x, y;

        top = bottom = polyX[0];
        left = right = polyY[0];

        for (int i = 1; i < polyCorners; i++) {
            x = polyX[i];
            y = polyY[i];

            if (y > top)
                top = y;

            if (y < bottom)
                bottom = y;

            if (x < left)
                left = x;

            if (x > right)
                right = x;
        }

        return new Boundaries(top, left, bottom, right);
    }

    public static class Boundaries {
        public PointD TopLeft, BottomRight;

        public Boundaries(PointD topLeft, PointD bottomRight) {
            TopLeft = topLeft;
            BottomRight = bottomRight;
        }

        public Boundaries(double top, double left, double bottom, double right) {
            TopLeft = new PointD(left, top);
            BottomRight = new PointD(right, bottom);
        }
    }
}
