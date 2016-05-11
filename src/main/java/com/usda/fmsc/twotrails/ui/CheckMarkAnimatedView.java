package com.usda.fmsc.twotrails.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;

public class CheckMarkAnimatedView extends View {
    private float[] points = new float[6];
    private Paint paint;

    float progress;


    public CheckMarkAnimatedView(Context context) {
        this(context, null, 0);

    }

    public CheckMarkAnimatedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckMarkAnimatedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setDither(true);
        paint.setColor(AndroidUtils.UI.getColor(getContext(), R.color.primary));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtils.Convert.dpToPx(context, 2));
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setPathEffect(new android.graphics.CornerPathEffect(2));
        paint.setAntiAlias(true);
    }

    public void start() {
        ValueAnimator va = ValueAnimator.ofFloat(0, 1).setDuration(500);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue(); // 0f ~ 1f
                invalidate();
            }
        });
        va.start();
    }

    public void reset() {
        progress = 0;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if(progress > 0) {
            if (progress < 1/3f) {
                float x = points[0] + (points[2] - points[0]) * progress;
                float y = points[1] + (points[3] - points[1]) * progress;
                canvas.drawLine(points[0], points[1], x, y, paint);
            } else {
                float x = points[2] + (points[4] - points[2]) * progress;
                float y = points[3] + (points[5] - points[3]) * progress;
                canvas.drawLine(points[0], points[1], points[2], points[3], paint);
                canvas.drawLine(points[2], points[3], x,y, paint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float r = w / 2f;
        points[0] = r / 2f;
        points[1] = r;

        points[2] = r * 5f / 6f;
        points[3] = r + r / 3f;

        points[4] = r * 1.5f;
        points[5] = r - r / 3f;
    }
}
