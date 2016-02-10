package com.usda.fmsc.twotrails.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.usda.fmsc.android.widget.MultiStateTouchCheckBox;
import com.usda.fmsc.android.widget.drawables.PolygonProgressDrawable;

public class UnadjustedDrawable extends PolygonProgressDrawable {

    public UnadjustedDrawable(){
        super(5, 90);
        points = new PointF[6];
    }

    @Override
    public void onDraw(Canvas canvas, Paint paint, float progress) {
        for (int i = 0; i < (progress * sides) && i < 6; i++) {
            if (progress < (i + 1f) / sides) {
                float x = points[i].x + (points[i + 1].x - points[i].x) * progress;
                float y = points[i].y + (points[i + 1].y - points[i].y) * progress;

                canvas.drawLine(points[i].x, points[i].y, x, y, paint);
            } else {
                canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, paint);
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh, int size, int pL, int pT, int pR, int pB) {
        float radius = size / 2f * 0.7f;

        float step = 360f / sides;


        PointF center = new PointF(w /2f, h /2f);

        float angle = startingAngle;
        for (int i = 0; i < 5; i++) {
            points[i] = DegreesToXY(angle, radius, center);

            angle -= step;
        }

        angle += step / 2f;

        points[5] = DegreesToXY(angle, radius, center);
    }
}
