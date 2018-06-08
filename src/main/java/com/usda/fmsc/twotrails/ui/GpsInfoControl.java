package com.usda.fmsc.twotrails.ui;

import android.animation.Animator;
import android.view.View;
import android.view.ViewTreeObserver;

import com.usda.fmsc.android.animation.ViewAnimator;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.gps.GpsService;

public class GpsInfoControl implements GpsService.Listener {

    private View viewGpsInfoLaySatInfo;
    private GpsStatusSkyView skyView;
    private GpsStatusSatView statusView;

    private boolean gpsExtraVisable = true, animating, gpsExtraLayoutSet;


    public GpsInfoControl(View view) {
        viewGpsInfoLaySatInfo = view.findViewById(R.id.gpsInfoLaySatInfo);

        skyView = view.findViewById(R.id.gpsInfoSatSky);
        statusView = view.findViewById(R.id.gpsInfoSatStatus);

        final ViewTreeObserver observer = skyView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                skyView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                hideExtraGpsStatus(false);
            }
        });

        Global.getGpsBinder().addListener(this);
    }


    //region PointMediaListener
    @Override
    public void gpsError(GpsService.GpsError error) {
    }

    @Override
    public void nmeaBurstReceived(INmeaBurst burst) {
        if (gpsExtraVisable) {
            skyView.update(burst);
            statusView.update(burst);
        }
    }

    @Override
    public void nmeaStringReceived(String nmeaString) {

    }

    @Override
    public void nmeaSentenceReceived(NmeaSentence nmeaSentence) {
        //
    }

    @Override
    public void gpsStarted() {

    }

    @Override
    public void gpsStopped() {

    }

    @Override
    public void gpsServiceStarted() {

    }

    @Override
    public void gpsServiceStopped() {

    }
    //endregion


    public void hideExtraGpsStatus() {
        hideExtraGpsStatus(true);
    }

    public void hideExtraGpsStatus(boolean animate) {
        if (gpsExtraVisable) {
            if (!gpsExtraLayoutSet) {
                viewGpsInfoLaySatInfo.getLayoutParams().width = viewGpsInfoLaySatInfo.getWidth();
                viewGpsInfoLaySatInfo.getLayoutParams().height = viewGpsInfoLaySatInfo.getHeight();
                viewGpsInfoLaySatInfo.requestLayout();
                gpsExtraLayoutSet = true;
            }

            if (animate) {
                if (!animating) {
                    animating = true;

                    ViewAnimator.collapseView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            super.onAnimationEnd(animator);
                            gpsExtraVisable = false;
                            animating = false;
                        }
                    });

                    animating = true;
                }
            } else {
                viewGpsInfoLaySatInfo.getLayoutParams().height = 0;
                viewGpsInfoLaySatInfo.requestLayout();
                viewGpsInfoLaySatInfo.setVisibility(View.GONE);
                gpsExtraVisable = false;
            }
        }
    }

    public void showExtraGpsStatus() {
        if (!animating && !gpsExtraVisable) {
            ViewAnimator.expandView(viewGpsInfoLaySatInfo, new ViewAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    gpsExtraVisable = true;
                    animating = false;
                }
            });

            animating = true;
        }
    }

    public boolean isGpsExtraInfoVisible() {
        return gpsExtraVisable;
    }


    public void pause() {
        if (skyView != null) {
            skyView.pause();
        }
    }

    public void resume() {
        if (skyView != null) {
            skyView.resume();
        }
    }

    public void destroy() {
        Global.getGpsBinder().removeListener(this);
    }
}
