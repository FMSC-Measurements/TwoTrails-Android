package com.usda.fmsc.twotrails;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;

/**
 *
 * Modified From http://steveliles.github.io/is_my_android_app_currently_foreground_or_background.html
 *
 */
public class AppLifecycle implements Application.ActivityLifecycleCallbacks {

    private static final long CHECK_DELAY = 500;
    private static final String TAG = AppLifecycle.class.getName();

    public interface Listener {
        void onBecameForeground(Activity activity);
        void onBecameBackground(Activity activity);
        void onResume(Activity activity);
        void onCreated(Activity activity);
        void onDestroyed(Activity activity);
    }

    private static AppLifecycle instance;

    private boolean foreground = false, paused = true;
    private final Handler handler = new Handler();
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private Runnable check;

    /**
     * Its not strictly necessary to use this method - _usually_ invoking
     * get with a Context gives us a path to retrieve the Application and
     * initialise, but sometimes (e.g. in test harness) the ApplicationContext
     * is != the Application, and the docs make no guarantees.
     *
     * @param application
     * @return an initialised Foreground instance
     */
    public static AppLifecycle init(Application application){
        if (instance == null) {
            instance = new AppLifecycle();
            application.registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    public static AppLifecycle get(Application application){
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    public static AppLifecycle get(Context ctx){
        if (instance == null) {
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                init((Application)appCtx);
            }
            throw new IllegalStateException(
                    "Foreground is not initialised and " +
                            "cannot obtain the Application object");
        }
        return instance;
    }

    public static AppLifecycle get(){
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/get");
        }
        return instance;
    }

    public boolean isBackground(){
        return !foreground;
    }

    public void addListener(Listener listener){
        listeners.add(listener);
    }

    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(@NonNull final Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        for (Listener l : listeners) {
            try {
                l.onResume(activity);
            } catch (Exception exc) {
                Log.e(TAG, "onActivityResumed threw exception!", exc);
            }
        }

        if (wasBackground){
            Log.i(TAG, "went foreground");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground(activity);
                } catch (Exception exc) {
                    Log.e(TAG, "onBecameForeground threw exception!", exc);
                }
            }
        } else {
            Log.i(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(@NonNull final Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = () -> {
            if (foreground && paused) {
                foreground = false;
                Log.i(TAG, "went background");
                for (Listener l : listeners) {
                    try {
                        l.onBecameBackground(activity);
                    } catch (Exception exc) {
                        Log.e(TAG, "onBecameBackground threw exception!", exc);
                    }
                }
            } else {
                Log.i(TAG, "still foreground");
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        for (Listener l : listeners) {
            try {
                l.onCreated(activity);
            } catch (Exception exc) {
                Log.e(TAG, "onActivityCreated threw exception!", exc);
            }
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity,@NonNull  Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        for (Listener l : listeners) {
            try {
                l.onDestroyed(activity);
            } catch (Exception exc) {
                Log.e(TAG, "onActivityDestroyed threw exception!", exc);
            }
        }
    }
}