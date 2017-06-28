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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.Satellite;
import com.usda.fmsc.twotrails.R;

public class GpsStatusSkyView extends GpsStatusView implements SensorEventListener {
    private PointF center;
    private Paint paintCompass, paintVisSatellite, paintInvisSatellite, paintText, northPaint, southPaint;
    private RectF outerCircle, innerCircle;
    private int outerCircleRadius, innerCircleRadius, satRad;
    private float orientation = 0;

    private long lastUpdate = System.currentTimeMillis();
    private boolean compassLock = true, sensorsActive;
    private float[] mGravity, mGeomagnetic;

    private Path northPath = new Path();
    private Path southPath = new Path();

    private Bitmap bmSatVis, bmSatUsed, bmSatSBAS;

    private SensorManager mSensorManager;
    private Sensor accelerometer, magnetometer;


    public GpsStatusSkyView(Context context) {
        this(context, null);
    }

    public GpsStatusSkyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GpsStatusSkyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paintCompass = new Paint();
        outerCircle = new RectF();
        innerCircle = new RectF();

        bmSatVis = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_grey_54);
        bmSatUsed = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_green_54);
        bmSatSBAS = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_satellite_blue_54);
        satRad = bmSatVis.getWidth() / 2;

        paintCompass.setColor(Color.GRAY);
        paintCompass.setStyle(Paint.Style.STROKE);
        paintCompass.setStrokeWidth(AndroidUtils.Convert.dpToPx(context, 3));
        paintCompass.setAntiAlias(true);

        paintVisSatellite = new Paint();
        paintVisSatellite.setAntiAlias(true);

        paintInvisSatellite = new Paint();
        paintInvisSatellite.setAlpha(128);
        paintInvisSatellite.setAntiAlias(true);

        paintText = new Paint(Color.BLACK);
        paintText.setFakeBoldText(true);
        paintText.setTextSize(22);
        paintText.setAntiAlias(true);

        northPaint = new Paint();
        northPaint.setColor(AndroidUtils.UI.getColor(context, R.color.red_300));
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setAntiAlias(true);

        southPaint = new Paint();
        southPaint.setColor(Color.LTGRAY);
        southPaint.setStyle(Paint.Style.FILL);
        southPaint.setAntiAlias(true);

        if (AndroidUtils.Device.isFullOrientationAvailable(getContext())) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }


    public void resume() {
        if (!compassLock && mSensorManager != null) {
            startCompass();
        }
    }


    public void pause() {
        stopCompass();
    }


    private void startCompass() {
        if (!sensorsActive) {
            try {
                mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
                sensorsActive = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopCompass() {
        if (sensorsActive) {
            mSensorManager.unregisterListener(this);
            sensorsActive = false;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int compassWidth = satRad / 2;

        float a1 = (float)Math.toRadians(90 - orientation);
        float a2 = (float)Math.toRadians(270 - orientation);

        canvas.drawLine(getX(a1, outerCircleRadius), getY(a1, outerCircleRadius),
                getX(a2, outerCircleRadius), getY(a2, outerCircleRadius), paintCompass);

        float a3 = (float)Math.toRadians(-orientation);

        northPath.reset();
        northPath.moveTo(getX(a3, outerCircleRadius), getY(a3, outerCircleRadius));
        northPath.lineTo(getX(a1, compassWidth), getY(a1, compassWidth));
        northPath.lineTo(getX(a2, compassWidth), getY(a2, compassWidth));
        northPath.close();

        a3 = (float)Math.toRadians(180 - orientation);

        southPath.reset();
        southPath.moveTo(getX(a3, outerCircleRadius), getY(a3, outerCircleRadius));
        southPath.lineTo(getX(a1, compassWidth), getY(a1, compassWidth));
        southPath.lineTo(getX(a2, compassWidth), getY(a2, compassWidth));
        southPath.close();

        canvas.drawPath(northPath, northPaint);
        canvas.drawPath(southPath, southPaint);

        canvas.drawCircle(center.x, center.y, outerCircleRadius, paintCompass);
        canvas.drawCircle(center.x, center.y, innerCircleRadius, paintCompass);

        if (getValidSatelliteCount() > 0) {
            for (Satellite sat : getSatellites().values()) {
                drawSatellite(canvas, sat);
            }
        }
    }


    private void drawSatellite(Canvas canvas, Satellite sat) {
        if (getSatellitesValid().containsKey(sat.getNmeaID()) && getSatellitesValid().get(sat.getNmeaID())) {
            float radius = elevationToRadius(sat.getElevation());
            float az = sat.getAzimuth() - orientation;
            float angle = (float) Math.toRadians(az);

            float x = (float) (center.x + (radius * Math.sin(angle)));
            float y = (float) (center.y - (radius * Math.cos(angle)));

            if (sat.isSBAS()) {
                canvas.drawBitmap(bmSatSBAS, x - satRad, y - satRad, paintVisSatellite);
            } else {
                canvas.drawBitmap(getSatellitesUsed().get(sat.getNmeaID()) ? bmSatUsed : bmSatVis, x - satRad, y - satRad,
                        getSatellitesVisibility().get(sat.getNmeaID()) ? paintVisSatellite : paintInvisSatellite);
            }

            float txtX = sat.getNmeaID() < 10 ? x - 7 : x - 12;

            canvas.drawText(String.valueOf(sat.getNmeaID()), txtX, y + 7, paintText);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int shortSide = w < h ? w : h;

        center = new PointF(w / 2, h / 2);

        outerCircleRadius = (int)(shortSide * .45);
        innerCircleRadius = (int)(shortSide * .25);

        outerCircle.set(center.x - outerCircleRadius, center.y - outerCircleRadius,
                center.x + outerCircleRadius, center.y + outerCircleRadius);

        innerCircle.set(center.x - innerCircleRadius, center.y - innerCircleRadius,
                center.x + innerCircleRadius, center.y + innerCircleRadius);
    }

    public void lockCompass(boolean lock) {
        compassLock = lock;
        if (compassLock)
            orientation = 0;
    }


    private float getX(float angle, int distance) {
        return (float) (center.x + (distance * Math.sin(angle)));
    }

    private float getY(float angle, int distance) {
        return (float) (center.y - (distance * Math.cos(angle)));
    }

    private float elevationToRadius(float elev) {
        return (outerCircleRadius - (bmSatVis.getWidth() / 2)) * (1.0f - (elev / 90.0f));
    }


    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD: {
                mGeomagnetic = event.values;
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                mGravity = event.values;
                break;
            }
        }

        if (!compassLock && mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float orientation[] = new float[9];
                SensorManager.getOrientation(R, orientation);

                float azimuth = Double.valueOf(Math.toDegrees(orientation[0])).floatValue();

                long now = System.currentTimeMillis();

                if ((now - lastUpdate) > 33) {

                    this.orientation = azimuth;
                    invalidate();
                    lastUpdate = now;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
