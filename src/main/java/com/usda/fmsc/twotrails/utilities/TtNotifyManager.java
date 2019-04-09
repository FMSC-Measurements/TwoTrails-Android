package com.usda.fmsc.twotrails.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;

public class TtNotifyManager {
    private int GPS_NOTIFICATION_ID = 123;

    private TwoTrailsApp TtAppCtx;
    
    private NotificationManager _NotificationManager;
    private NotificationCompat.Builder _GpsBuilder;
    private int _UsedDrawable;
    private String _UsedText, _ChannelId;
    private boolean isExternal;
    private SparseArray<NotificationCompat.Builder> _DownloadingNotifs;

    public TtNotifyManager(TwoTrailsApp context) {
        TtAppCtx = context;

        _NotificationManager = (NotificationManager) TtAppCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = TtAppCtx.getString(R.string.app_notify_service_channel_name);
            String description = TtAppCtx.getString(R.string.app_notify_service_channel_desc);

            NotificationChannel channel = new NotificationChannel(
                    (_ChannelId = TtAppCtx.getString(R.string.app_notify_service_channel_id)),
                    name,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            channel.setSound(null, null);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            _NotificationManager.deleteNotificationChannel(channel.getId());
            _NotificationManager.createNotificationChannel(channel);
        }

        _GpsBuilder = new NotificationCompat.Builder(TtAppCtx, _ChannelId);
        _GpsBuilder.setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        _DownloadingNotifs = new SparseArray<>();
    }

    public NotificationManager getNotificationManager() {
        return _NotificationManager;
    }

    public void setGpsOn(boolean externalGps) {
        if(_NotificationManager != null && _GpsBuilder != null) {
            isExternal = externalGps;
            _UsedText = (isExternal ? "External " : "Internal ") + Consts.ServiceContent;

            _UsedDrawable = R.drawable.ic_ttgps_holo_dark_enabled;
            //_UsedDrawable = R.drawable.ic_ttpoint_gps_white;
            _GpsBuilder.setContentTitle(Consts.ServiceTitle)
                    .setContentText(_UsedText)
                    .setSmallIcon(_UsedDrawable)
                    .setColor(AndroidUtils.UI.getColor(TtAppCtx, android.R.color.white));

            _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
        }
    }

    public void setGpsOff() {
        if(_NotificationManager != null) {
            _NotificationManager.cancel(GPS_NOTIFICATION_ID);
        }
    }


    public void startWalking() {
        if (_NotificationManager != null && _GpsBuilder != null) {
            _GpsBuilder.setContentTitle(Consts.ServiceTitle);

            _UsedText = Consts.ServiceWalking;
            _GpsBuilder.setContentText(_UsedText);

            _UsedDrawable = R.drawable.ic_ttgps_holo_dark_enabled; //switch to walking anim
            _GpsBuilder.setSmallIcon(_UsedDrawable)
                    .setColor(AndroidUtils.UI.getColor(TtAppCtx, android.R.color.white));

            _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
        }
    }

    public void stopWalking() {
        if (TtAppCtx.getGps().isGpsRunning()) {
            setGpsOn(isExternal);
        } else {
            setGpsOff();
        }
    }

    public void showPointAquired() {
        if(_NotificationManager != null && _GpsBuilder != null) {

            _GpsBuilder.setContentTitle(Consts.ServiceTitle)
                    .setContentText(Consts.ServiceAcquiringPoint)
                    .setSmallIcon(R.drawable.ica_capturepoint)
                    .setColor(AndroidUtils.UI.getColor(TtAppCtx, android.R.color.white));

            _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());

            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(1000);

                        _GpsBuilder.setContentTitle(Consts.ServiceTitle)
                                .setContentText(_UsedText)
                                .setSmallIcon(_UsedDrawable)
                                .setColor(AndroidUtils.UI.getColor(TtAppCtx, android.R.color.white));

                        _NotificationManager.notify(GPS_NOTIFICATION_ID, _GpsBuilder.build());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();
        }
    }


    public void startMapDownload(int id, String name) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(TtAppCtx, Consts.LOG_TAG);
        builder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_map_black_36dp)
                .setColor(AndroidUtils.UI.getColor(TtAppCtx, android.R.color.black))
                .setContentTitle(String.format("Downloading Map %s", name))
                .setProgress(100, 0, false);

        _DownloadingNotifs.put(id, builder);

        _NotificationManager.notify(id, builder.build());
    }

    public void updateMapDownload(int id, int progress) {
        _NotificationManager.notify(id, _DownloadingNotifs.get(id).setProgress(100, progress, false).build());
    }

    public void endMapDownload(int id) {
        _NotificationManager.cancel(id);
        _DownloadingNotifs.remove(id);
    }
}