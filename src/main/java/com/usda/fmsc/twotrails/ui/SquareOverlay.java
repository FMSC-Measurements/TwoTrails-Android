package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.R;

public class SquareOverlay extends View {
    private Paint paint;

    public SquareOverlay(Context context) {
        this(context, null, 0);
    }

    public SquareOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setColor(AndroidUtils.UI.getColor(context, R.color.red_A200));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtils.Convert.dpToPx(context, 6));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int a = getWidth();
        canvas.drawRect(0, 0, a, a, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size;
        if(widthMode == MeasureSpec.EXACTLY && widthSize > 0){
            size = widthSize;
        }
        else if(heightMode == MeasureSpec.EXACTLY && heightSize > 0){
            size = heightSize;
        }
        else{
            size = widthSize < heightSize ? widthSize : heightSize;
        }

        int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(finalMeasureSpec, finalMeasureSpec);
    }

    public int[] getWindowExtents() {
        int[] location = new int[2];
        this.getLocationInWindow(location);

        return new int[] { location[0] , location[1], location[0] + getWidth(), location[1] + getHeight() };
    }

    public int[] getScreenExtents() {
        int[] location = new int[2];
        this.getLocationOnScreen(location);

        return new int[] { location[0] , location[1], location[0] + getWidth(), location[1] + getHeight() };
    }
}
