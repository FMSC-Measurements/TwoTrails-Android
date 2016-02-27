package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.Satellite;
import com.usda.fmsc.twotrails.R;

public class GpsStatusView extends View {
    NmeaBurst burst;

    private int xMin = 0, xMax, yMin = 0, yMax, shortSide;
    private PointF center;
    private Paint paint;
    private RectF outerCircle, innerCircle;
    int radius, outerCircleRadius, innerrCircleRadius;

    Bitmap satVis, satTrack, satUsed;

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
        paint = new Paint();
        outerCircle = new RectF();
        innerCircle = new RectF();

        satVis = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_black);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtils.Convert.dpToPx(context, 1));

        canvas.drawCircle(center.x, center.y, outerCircleRadius, paint);
        canvas.drawCircle(center.x, center.y, innerrCircleRadius, paint);

        canvas.drawLine(center.x, center.y - outerCircleRadius, center.x, center.y + outerCircleRadius, paint);
        canvas.drawLine(center.x - outerCircleRadius, center.y, center.x + outerCircleRadius, center.y, paint);


        Paint p = new Paint();
        p.setAlpha(0);

        if (burst != null && burst.getGSV().isValid()) {

            //10deg is outer most, 100 in inner

            for (Satellite sat : burst.getGSV().getSatellites()) {

            }
        }
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
