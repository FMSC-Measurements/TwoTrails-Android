package AndroidExOld.UI.Animation;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class SlidingUpPanelResizeAnimation extends Animation {

    private SlidingUpPanelLayout mLayout;

    private float mTo;
    private float mFrom = 0;

    public SlidingUpPanelResizeAnimation(SlidingUpPanelLayout layout, float to, int duration) {
        mLayout = layout;
        mTo = to;

        setDuration(duration);
    }

    public SlidingUpPanelResizeAnimation(SlidingUpPanelLayout layout, float to, float from, int duration) {
        mLayout = layout;
        mTo = to;
        mFrom = from;

        setDuration(duration);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dimension = (mTo - mFrom) * interpolatedTime + mFrom;

        mLayout.setPanelHeight((int) dimension);

        mLayout.requestLayout();
    }
}