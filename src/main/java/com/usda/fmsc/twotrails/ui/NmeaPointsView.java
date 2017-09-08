package com.usda.fmsc.twotrails.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.gps.TtNmeaBurst;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class NmeaPointsView extends View {
    private static final DecimalFormat df = new DecimalFormat("#.###");

    private PointF center;
    private final ArrayList<PointF> nmeaPoints = new ArrayList<>(), nmeaPointsUsed = new ArrayList<>();

    private int xpad = 0, ypad = 0;
    private double shortSideAdjust, axis = 1;

    private float ml = 0, bpad, pointRadius = 15;

    private final Paint paintNmea = new Paint(), paintNmeaUsed = new Paint(), paintCenter = new Paint(), paintDist = new Paint();


    public NmeaPointsView(Context context) {
        this(context, null, 0);
    }

    public NmeaPointsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NmeaPointsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        pointRadius = AndroidUtils.Convert.dpToPx(getContext(), 5);

        paintNmea.setColor(Color.GRAY);

        paintNmeaUsed.setColor(Color.BLACK);

        paintCenter.setColor(Color.RED);

        paintDist.setColor(Color.BLACK);
        paintDist.setStrokeWidth(AndroidUtils.Convert.dpToPx(getContext(), 3));
        paintDist.setTextSize(30);
    }



    public void update(List<TtNmeaBurst> bursts, int zone, double xc, double yc) {
        nmeaPoints.clear();
        nmeaPointsUsed.clear();
        center = null;

        if (bursts != null) {
            double xmin, xmax, ymin, ymax;
            xmin = xmax = bursts.get(0).getX(zone);
            ymin = ymax = bursts.get(0).getY(zone);

            for (TtNmeaBurst burst : bursts) {
                double x = burst.getX(zone);
                if (x < xmin)
                    xmin = x;

                if (x > xmax)
                    xmax = x;

                double y = burst.getY(zone);
                if (y < ymin)
                    ymin = y;

                if (y > ymax)
                    ymax = y;
            }

            double xaxis = xmax - xmin, yaxis = ymax - ymin, xoffset = 0, yoffset = 0;

            if (xaxis > yaxis) {
                axis = xaxis;
                yoffset = (xaxis - yaxis) / 2;
            } else {
                axis = yaxis;
                xoffset = (yaxis - xaxis) / 2;
            }

            //axis = xaxis > yaxis ? xaxis : yaxis;

            if (axis > 1) {
                ml = (float)(int)axis;
            } else if (axis > 0.75) {
                ml = 0.75f;
            } else if (axis > 0.5) {
                ml = 0.5f;
            } else if (axis > 0.25) {
                ml = 0.25f;
            } else if (axis > 0.1) {
                ml = 0.1f;
            } else if (axis > 0.05) {
                ml = 0.05f;
            } else if (axis > 0.025) {
                ml = 0.025f;
            } else if (axis > 0.01) {
                ml = 0.01f;
            } else {
                ml = (float) axis;
            }

            PointF point;

            for (TtNmeaBurst burst : bursts ) {
                point = new PointF(
                        (float)((burst.getX(zone) - xmin + xoffset) / axis * shortSideAdjust + xpad),
                        (float)((burst.getY(zone) - ymin + yoffset) / axis * shortSideAdjust + ypad)
                );

                if (burst.isUsed()) {
                    nmeaPointsUsed.add(point);
                } else {
                    nmeaPoints.add(point);
                }
            }

            center = new PointF(
                    (float)((xc - xmin) / axis * shortSideAdjust + xpad + xoffset),
                    (float)((yc - ymin) / axis * shortSideAdjust + ypad + yoffset)
            );
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (PointF point : nmeaPoints) {
            canvas.drawCircle(point.x, point.y, pointRadius, paintNmea);
        }

        for (PointF point : nmeaPointsUsed) {
            canvas.drawCircle(point.x, point.y, pointRadius, paintNmeaUsed);
        }

        if (center != null) {
            canvas.drawCircle(center.x, center.y, pointRadius, paintCenter);

            canvas.drawLine(
                    bpad,
                    bpad,
                    (float)(ml / axis * shortSideAdjust) - bpad,
                    bpad,
                    paintDist
            );

            canvas.drawText(String.format("%s (meters)", df.format(ml)), bpad, bpad - 10, paintDist);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        center = new PointF(w / 2, h / 2);

        int shortSide;

        if (w > h) {
            shortSide = h;
            ypad = h / 20;
            xpad = (w - h) / 2 + ypad;
        } else {
            shortSide = w;
            xpad = w / 20;
            ypad = (h - w) / 2 + xpad;
        }

        shortSideAdjust = shortSide * 0.9;
        bpad = (float) (shortSide * 0.1);
    }
}
