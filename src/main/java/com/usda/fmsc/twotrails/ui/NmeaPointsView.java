package com.usda.fmsc.twotrails.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.twotrails.gps.TtNmeaBurst;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class NmeaPointsView extends View {
    private static final DecimalFormat df = new DecimalFormat("#.###");

    private PointF center;
    private final ArrayList<PointF> nmeaPoints = new ArrayList<>(), nmeaPointsUsed = new ArrayList<>();

    private int xpad = 0, ypad = 0;
    private double shortSideAdjust, xaxis = 1, yaxis = 1;

    private float mlX = 0, mlY = 0, bpady, bpadx;

    private final Paint paintNmea = new Paint(), paintNmeaUsed = new Paint(), paintCenter = new Paint(), paintDist = new Paint();


    public NmeaPointsView(Context context) {
        this(context, null, 0);
    }

    public NmeaPointsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NmeaPointsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paintNmea.setColor(Color.GRAY);

        paintNmeaUsed.setColor(Color.BLACK);

        paintCenter.setColor(Color.RED);

        paintDist.setColor(Color.BLACK);
        paintDist.setStrokeWidth(10);
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

            xaxis = xmax - xmin;
            yaxis = ymax - ymin;

            if (xaxis > 1) {
                mlX = (float)(int)xaxis;
            } else if (xaxis > 0.75) {
                mlX = 0.75f;
            } else if (xaxis > 0.5) {
                mlX = 0.5f;
            } else if (xaxis > 0.25) {
                mlX = 0.25f;
            } else if (xaxis > 0.1) {
                mlX = 0.1f;
            } else if (xaxis > 0.05) {
                mlX = 0.05f;
            } else if (xaxis > 0.025) {
                mlX = 0.025f;
            } else if (xaxis > 0.01) {
                mlX = 0.01f;
            } else {
                mlX = (float) xaxis;
            }

            if (yaxis > 1) {
                mlY = (float)(int)yaxis;
            } else if (yaxis > 0.75) {
                mlY = 0.75f;
            } else if (yaxis > 0.5) {
                mlY = 0.5f;
            } else if (yaxis > 0.25) {
                mlY = 0.25f;
            } else if (yaxis > 0.1) {
                mlY = 0.1f;
            } else if (yaxis > 0.05) {
                mlY = 0.05f;
            } else if (yaxis > 0.025) {
                mlY = 0.025f;
            } else if (yaxis > 0.01) {
                mlY = 0.01f;
            } else {
                mlY = (float) yaxis;
            }

            PointF point;

            for (TtNmeaBurst burst : bursts ) {
                point = new PointF(
                        (float)((burst.getX(zone) - xmin) / xaxis * shortSideAdjust + xpad),
                        (float)((burst.getY(zone) - ymin) / yaxis * shortSideAdjust + ypad)
                );

                if (burst.isUsed()) {
                    nmeaPointsUsed.add(point);
                } else {
                    nmeaPoints.add(point);
                }
            }

            center = new PointF(
                    (float)((xc - xmin) / xaxis * shortSideAdjust + xpad),
                    (float)((yc - ymin) / yaxis * shortSideAdjust + ypad)
            );
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (PointF point : nmeaPoints) {
            canvas.drawCircle(point.x, point.y, 15, paintNmea);
        }

        for (PointF point : nmeaPointsUsed) {
            canvas.drawCircle(point.x, point.y, 15, paintNmeaUsed);
        }

        if (center != null) {
            canvas.drawCircle(center.x, center.y, 15, paintCenter);

            canvas.drawLine(
                    bpadx,
                    bpady,
                    (float)(mlX / xaxis * shortSideAdjust) - bpadx,
                    bpady,
                    paintDist
            );

            canvas.drawLine(
                    bpadx,
                    bpady * 2,
                    (float)(mlY / yaxis * shortSideAdjust) - bpadx,
                    bpady * 2,
                    paintDist
            );

            canvas.drawText(String.format("X-Axis: %s (meters)", df.format(mlX)), bpadx, bpady - 10, paintDist);

            canvas.drawText(String.format("Y-Axis: %s (meters)", df.format(mlY)), bpadx, bpady * 2 - 10, paintDist);
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
        bpady = (float) (shortSide * 0.05);
        bpadx = bpady / 2;
    }
}
