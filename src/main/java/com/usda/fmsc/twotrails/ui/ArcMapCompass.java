package com.usda.fmsc.twotrails.ui;

/* Copyright 2012 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the sample code usage restrictions document for further information.
 *
 */

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPinchListener;
import com.usda.fmsc.twotrails.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ArcMapCompass extends View implements View.OnClickListener {
    float mAngle = 0;

    Paint compassPaint;
    Bitmap bitmap;
    Matrix matrix;

    MapView mapView;

    Integer oLeft, oTop, oRight, oBottom;
    boolean visible = false;


    public ArcMapCompass(Context context, AttributeSet attrs) {
        super(context, attrs);

        compassPaint = new Paint();
        matrix = new Matrix();

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_compass_36dp);

        setOnClickListener(this);
    }

    public void setMapView(MapView mapView) {
        // Save reference to the MapView passed in to this compass.
        this.mapView = mapView;
        if (this.mapView != null) {

            // Set an OnPinchListener on the map to listen for the pinch gesture which may change the map rotation.
            this.mapView.setOnPinchListener(new OnPinchListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void prePointersUp(float arg0, float arg1, float arg2, float arg3, double arg4) {
                }

                @Override
                public void prePointersMove(float arg0, float arg1, float arg2, float arg3, double arg4) {
                }

                @Override
                public void prePointersDown(float arg0, float arg1, float arg2, float arg3, double arg4) {
                }

                @Override
                public void postPointersUp(float arg0, float arg1, float arg2, float arg3, double arg4) {
                }

                @Override
                public void postPointersMove(float arg0, float arg1, float arg2, float arg3, double arg4) {
                    // Update the compass angle from the map rotation angle (the arguments passed in to the method are not
                    // relevant in this case).
                    setRotationAngle(ArcMapCompass.this.mapView.getRotationAngle());
                }

                @Override
                public void postPointersDown(float arg0, float arg1, float arg2, float arg3, double arg4) {
                }
            });
        }
    }

    public void setRotationAngle(double angle) {
        // Save the new rotation angle.
        mAngle = (float) angle;

        // Force the compass to re-paintNmea itself.
        if (mAngle != 0 && !visible) {
            visible = true;
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (visible) {
            matrix.reset();

            matrix.postRotate(-this.mAngle, bitmap.getHeight() / 2, bitmap.getWidth() / 2);

            canvas.drawBitmap(bitmap, matrix, compassPaint);
        }

        super.onDraw(canvas);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)getLayoutParams();

        //get original padding
        if (oLeft == null) {
            oLeft = params.leftMargin;
            oTop = params.topMargin;
            oRight = params.rightMargin;
            oBottom = params.bottomMargin;
        }

        params.setMargins(left + oLeft, top + oTop, right + oRight, bottom + oBottom);
        this.setLayoutParams(params);
    }

    @Override
    public void onClick(View v) {
        mapView.setRotationAngle(0);
        setRotationAngle(0);
        visible = false;
    }

    public void resetCompass() {
        setRotationAngle(0);
        visible = false;
        invalidate();
    }
}