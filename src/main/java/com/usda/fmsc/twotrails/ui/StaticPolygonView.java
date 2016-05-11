package com.usda.fmsc.twotrails.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.objects.PointD;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.List;

public class StaticPolygonView extends View {
    List<PointD> points;
    Paint paint;
    boolean rendered;


    public StaticPolygonView(Context context) {
        this(context, null, 0);
    }

    public StaticPolygonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticPolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtils.Convert.dpToPx(getContext(), 2));
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setPathEffect(new android.graphics.CornerPathEffect(2));
        paint.setAntiAlias(true);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int w = this.getMeasuredWidth();

        setMeasuredDimension(w, w);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points != null) {
            int offset = (int) (getWidth() * 0.05);

            PointD lastPt = points.get(0), pt;

            for (int i = 1; i < points.size(); i++) {
                pt = points.get(i);
                canvas.drawLine(
                        (float) (lastPt.X + offset),
                        (float) (lastPt.Y + offset),
                        (float) (pt.X + offset),
                        (float) (pt.Y + offset),
                        paint);

                lastPt = pt;
            }

            if (!rendered) {
                rendered = true;
            }
        }
    }
    public void render(final List<TtPoint> points, final HashMap<String, TtMetadata> metadata) {
        if (points.size() > 2) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int zone = metadata.get(Consts.EmptyGuid).getZone();

                    int width = getWidth();

                    width = (int)(width * 0.9);

                    if (width > 0) {
                        StaticPolygonView.this.points = TtUtils.generateStaticPolyPoints(points, metadata, zone, width);

                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StaticPolygonView.this.invalidate();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    public boolean isRendered() {
        return rendered;
    }
}
