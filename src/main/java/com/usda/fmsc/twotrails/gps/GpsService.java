package com.usda.fmsc.twotrails.gps;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.LocationSource;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.nmea.NmeaIDs;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.Manifest;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import org.joda.time.DateTime;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.NmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaParser;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.utilities.StringEx;

public class GpsService extends Service implements LocationListener, LocationSource, GpsStatus.Listener,
        GpsStatus.NmeaListener, NmeaParser.Listener, GpsBluetoothConnection.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    public final int GPS_UPDATE_INTERVAL = 1000;    //in milliseconds
    public final int GPS_MINIMUM_DISTANCE = 0;      //in meters

    private OnLocationChangedListener gmapListener;

    private boolean postAllNmeaStrings = true, logging, logBurstDetails;
    private GeoPosition lastPosition;

    private ConcurrentHashMap<Activity, Listener> activities = new ConcurrentHashMap<>();
    private final Binder binder = new GpsBinder();

    private TtBluetoothManager bluetoothManager;
    private GpsBluetoothConnection btConn;

    private PrintWriter logPrintWriter;

    private String _deviceUUID;
    private LocationManager locManager;

    private NmeaParser parser;

    private GpsSyncer gpsSyncer;


    public GpsService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //android.os.Debug.waitForDebugger();

        bluetoothManager = Global.getBluetoothManager();

        gpsSyncer = new GpsSyncer();

        parser = new NmeaParser();
        parser.addTalkerID(NmeaIDs.TalkerID.GL);
        parser.addTalkerID(NmeaIDs.TalkerID.GN);
        parser.addListener(this);


        SharedPreferences prefs = Global.Settings.PreferenceHelper.getPrefs();

        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        if (Global.Settings.DeviceSettings.getGpsExternal()) {
            _deviceUUID = Global.Settings.DeviceSettings.getGpsDeviceID();
        }

        if (Global.Settings.DeviceSettings.isGpsConfigured() && Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
            startGps();
        }

        logBurstDetails = Global.Settings.DeviceSettings.getGpsLogBurstDetails();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (parser != null) {
            parser.removeListener(this);
        }

        if (logPrintWriter != null) {
            logPrintWriter.flush();
            logPrintWriter.close();
        }

        Global.Settings.PreferenceHelper.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case Global.Settings.DeviceSettings.GPS_ALWAYS_ON:
            case Global.Settings.DeviceSettings.GPS_CONFIGURED: {
                boolean keepOn = sharedPreferences.getBoolean(Global.Settings.DeviceSettings.GPS_ALWAYS_ON, false);
                boolean gpsConfigured = sharedPreferences.getBoolean(Global.Settings.DeviceSettings.GPS_CONFIGURED, false);

                boolean running = isGpsRunning();

                if (keepOn && gpsConfigured && !running) {
                    startGps();
                } else if (running && !keepOn){
                    stopGps();
                }
                break;
            }
            case Global.Settings.DeviceSettings.GPS_LOG_BURST_DETAILS: {
                logBurstDetails = sharedPreferences.getBoolean(Global.Settings.DeviceSettings.GPS_LOG_BURST_DETAILS,
                        Global.Settings.DeviceSettings.DEFAULT_GPS_LOG_BURST_DETAILS);
            }
        }
    }



    //region Start / Stop GPS
    private GpsDeviceStatus startGps() {
        GpsDeviceStatus status;

        if (!isGpsRunning()) {
            parser.reset();
            gpsSyncer.reset();

            status = (_deviceUUID == null) ?
                    startInternalGps() : startExternalGps();

            if (status == GpsDeviceStatus.ExternalGpsStarted ||
                    status == GpsDeviceStatus.InternalGpsStarted) {
                //started
                Global.TtNotifyManager.setGpsOn();

                if (logging) {
                    writeStartLog();
                }
            }
        } else {
            status = GpsDeviceStatus.GpsAlreadyStarted;
            Global.TtNotifyManager.setGpsOn();
        }

        return status;
    }

    private GpsDeviceStatus stopGps() {
        GpsDeviceStatus status;

        if (!logging) {
            if (isGpsRunning()) {
                status = (_deviceUUID == null) ?
                        stopInternalGps() : stopExternalGps();

                if (status == GpsDeviceStatus.ExternalGpsStopped ||
                        status == GpsDeviceStatus.InternalGpsStopped) {
                    //stopped
                    Global.TtNotifyManager.setGpsOff();

                    if (logging) {
                        writeEndLog();
                    }
                }
            } else {
                status = GpsDeviceStatus.GpsAlreadyStopped;
                Global.TtNotifyManager.setGpsOff();
            }
        } else {
            status = GpsDeviceStatus.GpsServiceInUse;
        }

        return status;
    }

    private GpsDeviceStatus startInternalGps() {
        try {
            if(locManager == null)
                locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

            if (isInternalGpsEnabled()) {
                if (AndroidUtils.App.checkFineLocationPermission(this)) {
                    locManager.addNmeaListener(this);
                    locManager.addGpsStatusListener(this);
                    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, GPS_MINIMUM_DISTANCE, this);
                    return GpsDeviceStatus.InternalGpsStarted;
                } else {
                    ActivityCompat.requestPermissions(Global.getMainActivity(), new String[] {
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            Consts.Activities.Services.REQUEST_GPS_SERVICE);
                    return GpsDeviceStatus.InternalGpsNeedsPermissions;
                }
            } else {
                return GpsDeviceStatus.InternalGpsNotEnabled;
            }
        } catch (Exception ex) {
            TtUtils.TtReport.writeError(ex.getMessage(), "GpsService:startInternalGps");
        }

        return GpsDeviceStatus.InternalGpsError;
    }

    private GpsDeviceStatus stopInternalGps() {
        if(locManager != null) {
            if (AndroidUtils.App.checkFineLocationPermission(this)) {
                locManager.removeNmeaListener(this);
                locManager.removeGpsStatusListener(this);
                locManager.removeUpdates(this);

                locManager = null;
                postGpsStop();
                return GpsDeviceStatus.InternalGpsStopped;
            } else {
                locManager = null;
                return GpsDeviceStatus.InternalGpsNeedsPermissions;
            }
        }

        return GpsDeviceStatus.InternalGpsNotEnabled;
    }

    private GpsDeviceStatus startExternalGps() {
        try {
            BluetoothSocket socket = bluetoothManager.getSocket(_deviceUUID);

            if (socket != null) {
                btConn = new GpsBluetoothConnection(socket);
                btConn.register(this);
                btConn.start();
            } else {
                return GpsDeviceStatus.ExternalGpsNotFound;
            }
        } catch (Exception e) {
            TtUtils.TtReport.writeError(e.getMessage(), "GpsService:startExternalGps");
            return GpsDeviceStatus.ExternalGpsError;
        }

        return GpsDeviceStatus.ExternalGpsStarted;
    }

    private GpsDeviceStatus stopExternalGps() {
        if(btConn != null) {
            btConn.unregister(this);
            btConn.disconnect();
            btConn = null;

            postGpsStop();  //needs to be triggered manually
            return GpsDeviceStatus.ExternalGpsStopped;
        }

        return GpsDeviceStatus.ExternalGpsError;
    }
    //endregion


    //region Logging
    public void startLogging(String fileName) {
        try {
            if (logging && logPrintWriter != null) {
                logPrintWriter.close();
            }

            File logFileDir = new File(TtUtils.getTtLogFileDir());
            if (!logFileDir.exists()) {
                logFileDir.mkdirs();
            }

            logPrintWriter = new PrintWriter(fileName);

            writeStartLog();

            logging = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLogging() {
        if (logging && logPrintWriter != null) {
            writeEndLog();
            logPrintWriter.close();
            logPrintWriter = null;
        }

        logging = false;
    }

    private void writeStartLog() {
        logPrintWriter.println(String.format("[%s] %s GPS Started%s", DateTime.now(),
                isExternalGpsUsed() ? "External" : "Internal",
                isExternalGpsUsed() ? String.format(" [%s]", Global.Settings.DeviceSettings.getGpsDeviceName()) : StringEx.Empty));
        logPrintWriter.flush();
    }

    private void writeEndLog() {
        logPrintWriter.println(String.format("[%s] %s GPS Stopped", DateTime.now(),
                isExternalGpsUsed() ? "External" : "Internal"));
        logPrintWriter.flush();
    }

    public boolean isLogging() {
        return logging;
    }
    //endregion


    public void setGpsProvider(String deviceUUID) throws RuntimeException {
        if(isGpsRunning())
            throw new  RuntimeException("GPS must be stopped before setting provider.");
        else
            _deviceUUID = deviceUUID;
    }

    public boolean isGpsRunning() {
        if(getGpsProvider() == GpsProvider.Internal) {
            return locManager != null;
        } else {
            return btConn != null && btConn.isConnected();
        }
    }

    public boolean isInternalGpsEnabled() {
        return (locManager != null) ?
                locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) :
                ((LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isExternalGpsUsed() {
        return btConn != null;
    }

    public GpsProvider getGpsProvider() {
        return _deviceUUID == null ? GpsProvider.Internal : GpsProvider.External;
    }


    //region External Device GPS
    @Override
    public void receivedString(String data) {
        parseNmeaString(data);
    }

    @Override
    public void connectionLost() {

        postError(GpsError.LostDeviceConnection);

//        BluetoothSocket socket = bluetoothManager.getSocket(_deviceUUID);
//
//        if (btConn != null) {
//            btConn.disconnect();
//        }

//        if (socket != null) {
//            btConn = new GpsBluetoothConnection(socket);

//            btConn.register(this);
//            btConn.start();
//        } else {
//            postError(GpsError.NoExternalGpsSocket);
//        }
    }

    @Override
    public void connectionEnded() {
        //stopExternalGps();
        Global.TtNotifyManager.setGpsOff();
        postError(GpsError.DeviceConnectionEnded);
    }

    //endregion

    //region Internal Android GPS
    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        parseNmeaString(nmea);
    }


    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                postGpsStart();
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                postGpsStop();
                break;
        }
    }


    @Override
    public void onLocationChanged(Location location) {
//        if (gmapListener != null) {
//            gmapListener.onLocationChanged(location);
//        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //
    }

    @Override
    public void onProviderEnabled(String provider) {
        //if gps or coarse loc gets turned on
    }

    @Override
    public void onProviderDisabled(String provider) {
        //if gps or coarse loc gets turned off
    }
    //endregion


    private void parseNmeaString(String nmeaString) {
        if (nmeaString != null) {
            nmeaString = nmeaString.trim();

            boolean parsed = false;

            if (gpsSyncer.isSynced() || gpsSyncer.sync(nmeaString)) {
                parsed = parser.parse(nmeaString);
            }

            if (postAllNmeaStrings || parsed) {
                postNmeaString(nmeaString);
            }

            if (logging) {
                try {
                    logPrintWriter.println(nmeaString);
                    logPrintWriter.flush();
                } catch (Exception e) {
                    TtUtils.TtReport.writeError(e.getMessage(), "GpsService:parseNmeaString:logToFile");
                }
            }
        }
    }

    @Override
    public void onBurstReceived(NmeaBurst burst) {
        postNmeaBurst(burst);

        if (burst.hasPosition()) {
            lastPosition = burst.getPosition();

            if (gmapListener != null) {
                Location location = new Location(StringEx.Empty);

                location.setLatitude(lastPosition.getLatitudeSignedDecimal());
                location.setLongitude(lastPosition.getLongitudeSignedDecimal());
                location.setAltitude(lastPosition.getElevation());

                try {
                    gmapListener.onLocationChanged(location);
                } catch (Exception e) {
                    e.printStackTrace();
                    TtUtils.TtReport.writeError(e.getMessage(), "GpsService:onBurstReceived:gmap");
                }
            }
        }

        if (logBurstDetails) {
            try {
                logPrintWriter.println(burst.toString());
                logPrintWriter.flush();
            } catch (Exception e) {
                TtUtils.TtReport.writeError(e.getMessage(), "GpsService:parseNmeaString:logToFile");
            }
        }
    }

    @Override
    public void onNmeaReceived(NmeaSentence sentence) {
        postNmeaSentence(sentence);
    }

    public GeoPosition getLastPosition() {
        return lastPosition;
    }


    //region Post Events
    private void postNmeaBurst(final NmeaBurst burst) {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.nmeaBurstReceived(burst);
                        }
                    }
                });

            } catch (Exception ex) {

            }
        }
    }

    private void postNmeaString(final String nmeaString) {
        for (final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.nmeaStringReceived(nmeaString);
                        }
                    }
                });

            } catch (Exception ex) {

            }
        }
    }

    private void postNmeaSentence(final NmeaSentence sentence) {
        for (final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.nmeaSentenceReceived(sentence);
                        }
                    }
                });

            } catch (Exception ex) {

            }
        }
    }

    private void postGpsStart() {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.gpsStarted();
                        }
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postGpsStop() {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.gpsStopped();
                        }
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postServiceStart() {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.gpsServiceStarted();
                        }
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postServiceStop() {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.gpsServiceStopped();
                        }
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postError(final GpsError error) {
        for(final Activity client : activities.keySet()) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener callback = activities.get(client);
                        if (callback != null) {
                            callback.gpsError(error);
                        }
                    }
                });

            } catch (Exception ex) {

            }
        }
    }
    //endregion


    //region LocationSource for GMaps
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        gmapListener = onLocationChangedListener;

        if (!isGpsRunning()) {
            startGps();
        }
    }

    @Override
    public void deactivate() {
        gmapListener = null;

        if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
            stopGps();
        }
    }
    //endregion


    public enum GpsError {
        LostDeviceConnection,
        DeviceConnectionEnded,
        NoExternalGpsSocket,
        Unkown
    }

    public enum GpsDeviceStatus {
        InternalGpsStarted,
        InternalGpsStopped,
        InternalGpsNotEnabled,
        InternalGpsNeedsPermissions,
        InternalGpsError,
        ExternalGpsStarted,
        ExternalGpsStopped,
        ExternalGpsNotFound,
        ExternalGpsNotConnected,
        ExternalGpsError,
        GpsAlreadyStarted,
        GpsAlreadyStopped,
        GpsServiceInUse,
        Unkown
    }

    public enum GpsProvider {
        Internal,
        External
    }


    public class GpsBinder extends Binder implements Controller {

        public GpsService getService() {
            return GpsService.this;
        }

        //@Override
        public void registerActiviy(Activity activity, Listener callback) {
            activities.put(activity, callback);
        }

        @Override
        public void unregisterActivity(Activity activity) {
            try {
                activities.remove(activity);

                for (Activity a : activities.keySet()) {
                    if (activities.get(a) == null) {
                        activities.remove(a);
                    }
                }
            } catch (Exception e) {
                TtUtils.TtReport.writeError(e.getMessage(), "GpsService:unregisterActivity", e.getStackTrace());
            }
        }


        @Override
        public GpsDeviceStatus startGps() {
            return GpsService.this.startGps();
        }

        @Override
        public GpsDeviceStatus stopGps() {
            return GpsService.this.stopGps();
        }

        @Override
        public void stopService() {
            GpsService.this.stopGps();
            GpsService.this.stopLogging();
            GpsService.this.stopSelf();
        }

        @Override
        public void setGpsProvider(String provider) {
            GpsService.this.setGpsProvider(provider);
        }

        @Override
        public boolean isInternalGpsEnabled() {
            return GpsService.this.isInternalGpsEnabled();
        }

        @Override
        public boolean isGpsRunning() {
            return GpsService.this.isGpsRunning();
        }

        @Override
        public boolean isExternalGpsUsed() {
            return GpsService.this.isExternalGpsUsed();
        }

        @Override
        public GpsProvider getGpsProvider() {
            return GpsService.this.getGpsProvider();
        }

        @Override
        public void postAllNmeaStrings(boolean postAllStrings) {
            GpsService.this.postAllNmeaStrings = postAllStrings;
        }

        public boolean postsAllNmeaStrings() {
            return GpsService.this.postAllNmeaStrings;
        }

        @Override
        public void startLogging(String fileName) {
            GpsService.this.startLogging(fileName);
        }

        @Override
        public void stopLogging() {
            GpsService.this.stopLogging();
        }

        @Override
        public boolean isLogging() {
            return GpsService.this.isLogging();
        }

        public GeoPosition getLastPosition() {
            return GpsService.this.getLastPosition();
        }
    }


    public interface Listener {
        void nmeaBurstReceived(NmeaBurst nmeaBurst);
        void nmeaStringReceived(String nmeaString);
        void nmeaSentenceReceived(NmeaSentence nmeaSentence);

        void gpsStarted();
        void gpsStopped();

        void gpsServiceStarted();
        void gpsServiceStopped();
        void gpsError(GpsError error);
    }


    public interface Controller {
        void registerActiviy(Activity activity, Listener callback);

        void unregisterActivity(Activity activity);

        GpsDeviceStatus startGps();

        GpsDeviceStatus stopGps();

        void stopService();

        void setGpsProvider(String provider) throws Exception;

        boolean isInternalGpsEnabled();

        boolean isGpsRunning();

        boolean isExternalGpsUsed();

        GpsProvider getGpsProvider();

        void postAllNmeaStrings(boolean allStrings);

        void startLogging(String fileName);

        void stopLogging();

        boolean isLogging();
    }

}