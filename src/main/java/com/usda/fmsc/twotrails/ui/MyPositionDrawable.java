package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;

public class MyPositionDrawable extends Drawable {
    private static final int DEFAULT_RADIUS = 10;
    private static final int DEFAULT_STROKE_WIDTH = 1;

    private final Paint paintColor, paintInnerStroke, paintColorOuter, paintOuterStroke;

    private final float radius;
    private float outerRadius;

    private final int innerXY, size;
    private int outerXY;


    public MyPositionDrawable(Context context) {
        radius = AndroidUtils.Convert.dpToPx(context, DEFAULT_RADIUS);
        int strokeWidth = AndroidUtils.Convert.dpToPx(context, DEFAULT_STROKE_WIDTH);

        paintColor = new Paint(Color.BLUE);
        paintColor.setAntiAlias(true);

        paintInnerStroke = new Paint(Color.WHITE);
        paintInnerStroke.setAntiAlias(true);
        paintInnerStroke.setStyle(Paint.Style.STROKE);
        paintInnerStroke.setStrokeWidth(strokeWidth);

        paintColorOuter = new Paint(Color.BLUE);
        paintColorOuter.setAntiAlias(true);
        paintColorOuter.setAlpha(128);

        paintOuterStroke = new Paint(Color.WHITE);
        paintOuterStroke.setAntiAlias(true);
        paintOuterStroke.setAlpha(128);
        paintInnerStroke.setStyle(Paint.Style.STROKE);
        paintInnerStroke.setStrokeWidth(strokeWidth);

        size = (int)(radius * 2);
        innerXY = size / 2;

        outerRadius = size;
        outerXY = 0;
    }



    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(outerXY, outerXY, outerRadius, paintColorOuter);
        canvas.drawCircle(outerXY, outerXY, outerRadius, paintOuterStroke);
        canvas.drawCircle(innerXY, innerXY, radius, paintColor);
        canvas.drawCircle(innerXY, innerXY, radius, paintInnerStroke);
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }


    public void update(GnssNmeaBurst nmeaBurst) {
        int accuracyMultiplier = 1;

        outerRadius = radius + radius * accuracyMultiplier;

        outerXY = (int)(outerRadius / 2f - radius);
    }


    public int getWidth() {
        return size;
    }

    public int getHeight() {
        return size;
    }
}
