package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.Satellite;
import com.usda.fmsc.twotrails.R;

import java.util.List;

public class GpsStatusView extends View {
    NmeaBurst burst;

    private int xMin = 0, xMax, yMin = 0, yMax, shortSide;
    private PointF center;
    private Paint paintCompass, paintInView, paintText, northPaint, southPaint;
    private RectF outerCircle, innerCircle;
    int radius, outerCircleRadius, innerrCircleRadius, satRad;
    float orientation = 0;

    Path northPath = new Path();
    Path southPath = new Path();

    Bitmap satVis, satUsed, satSBAS;

    Context context;

    public GpsStatusView(Context context) {
        this(context, null);
    }

    public GpsStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GpsStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;
        paintCompass = new Paint();
        outerCircle = new RectF();
        innerCircle = new RectF();

        satVis = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_grey_54);
        satUsed = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_green_54);
        satSBAS = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_blue_54);
        satRad = satVis.getWidth() / 2;

        paintCompass.setColor(Color.GRAY);
        paintCompass.setStyle(Paint.Style.STROKE);
        paintCompass.setStrokeWidth(AndroidUtils.Convert.dpToPx(context, 3));

        paintInView = new Paint(Color.BLACK);

        paintText = new Paint(Color.BLACK);
        paintText.setFakeBoldText(true);
        paintText.setTextSize(22);

        northPaint = new Paint();
        northPaint.setColor(AndroidUtils.UI.getColor(context, R.color.red_300));
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setAntiAlias(true);

        southPaint = new Paint();
        southPaint.setColor(Color.LTGRAY);
        southPaint.setStyle(Paint.Style.FILL);
        southPaint.setAntiAlias(true);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int compassWidth = satRad / 2;

        canvas.drawLine(center.x - outerCircleRadius, center.y, center.x + outerCircleRadius, center.y, paintCompass);

        northPath.reset();
        northPath.moveTo(center.x, center.y - outerCircleRadius);
        northPath.lineTo(center.x + compassWidth, center.y);
        northPath.lineTo(center.x - compassWidth, center.y);
        northPath.close();

        southPath.reset();
        southPath.moveTo(center.x, center.y + outerCircleRadius);
        southPath.lineTo(center.x + compassWidth, center.y);
        southPath.lineTo(center.x - compassWidth, center.y);
        southPath.close();

        canvas.drawPath(northPath, northPaint);
        canvas.drawPath(southPath, southPaint);

        canvas.drawCircle(center.x, center.y, outerCircleRadius, paintCompass);
        canvas.drawCircle(center.x, center.y, innerrCircleRadius, paintCompass);

        if (burst != null && burst.getGSV().isValid()) {
            for (Satellite sat : burst.getGSV().getSatellites()) {
                drawSatellite(canvas, sat, burst.getGSA().getSatellitesUsed());
            }

            canvas.drawText(String.format("%d/%d/%d",
                            burst.getGSA().isValid() ? burst.getUsedSatellitesCount() : 0,
                            burst.getGGA().isValid() ? burst.getTrackedSatellitesCount() : 0,
                            burst.getSatellitesInViewCount()), 30,
                    30, paintInView);

        }
    }


    private void drawSatellite(Canvas canvas, Satellite sat, List<Integer> usedSats) {

        if (sat.getAzimuth() != null && sat.getElevation() != null) {
            float radius = elevationToRadius(sat.getElevation());
            float az = sat.getAzimuth() - orientation;
            float angle = (float) Math.toRadians(az);

            float x = (float) (center.x + (radius * Math.sin(angle)));
            float y = (float) (center.y - (radius * Math.cos(angle)));

            if (sat.isSBAS()) {
                canvas.drawBitmap(satSBAS, x - satRad, y - satRad, paintInView);
            } else {
                if (usedSats != null)
                    canvas.drawBitmap(usedSats.contains(sat.getNmeaID()) ? satUsed : satVis, x - satRad, y - satRad, paintInView);
                else
                    canvas.drawBitmap(satVis, x - satRad, y - satRad, paintInView);
            }

            float txtX = sat.getNmeaID() < 10 ? x - 7 : x - 12;

            canvas.drawText(String.valueOf(sat.getNmeaID()), txtX, y + 7, paintText);
        }
    }

    private float elevationToRadius( float elev) {
        return ((outerCircleRadius) - satVis.getWidth() / 2) * (1.0f - (elev / 90.0f));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        xMax = w;
        yMax = h;

        shortSide = xMax < yMax ? xMax : yMax;

        center = new PointF(xMax / 2, yMax / 2);

        radius = shortSide / 2;
        outerCircleRadius = (int)(shortSide * .45);
        innerrCircleRadius = (int)(shortSide * .25);

        outerCircle.set(center.x - outerCircleRadius, center.y - outerCircleRadius,
                center.x + outerCircleRadius, center.y + outerCircleRadius);

        innerCircle.set(center.x - innerrCircleRadius, center.y - innerrCircleRadius,
                center.x + innerrCircleRadius, center.y + innerrCircleRadius);
    }

    public void update(NmeaBurst burst) {
        this.burst = burst;
        invalidate();
    }
}
