package com.usda.fmsc.twotrails.gps;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.maps.LocationSource;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.geospatial.GeoPosition;
import com.usda.fmsc.geospatial.nmea.INmeaBurst;
import com.usda.fmsc.geospatial.nmea.NmeaBurstEx;
import com.usda.fmsc.geospatial.nmea.NmeaIDs;
import com.usda.fmsc.geospatial.nmea.NmeaParser;
import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.TwoTrailApp;
import com.usda.fmsc.twotrails.devices.BluetoothConnection;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GpsService extends Service implements LocationListener, LocationSource, OnNmeaMessageListener,

        NmeaParser.Listener, BluetoothConnection.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    public final int GPS_UPDATE_INTERVAL = 1000;    //in milliseconds
    public final int GPS_MINIMUM_DISTANCE = 0;      //in meters

    private TwoTrailApp TtAppCtx;

    private OnLocationChangedListener gmapListener;

    private boolean postAllNmeaStrings = true, logging, logBurstDetails;
    private GeoPosition lastPosition;

    private ArrayList<Listener> listeners = new ArrayList<>();
    private final Binder binder = new GpsBinder();

    private TtBluetoothManager bluetoothManager;
    private BluetoothConnection btConn;

    private PrintWriter logPrintWriter;

    private String _deviceUUID;
    private LocationManager locManager;

    private NmeaParser parser;

    private GpsSyncer gpsSyncer;

    GnssStatus.Callback mGnssStatusCallback = new GnssStatus.Callback() {
        @Override
        public void onStarted() {
            postGpsStart();
        }

        @Override
        public void onStopped() {
            postGpsStop();
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            //
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            //
        }
    };


    public GpsService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TtAppCtx = (TwoTrailApp) getApplicationContext();

        bluetoothManager = new TtBluetoothManager();

        gpsSyncer = new GpsSyncer();

        parser = new NmeaParser<>(NmeaBurstEx.class);
        parser.addTalkerID(NmeaIDs.TalkerID.GL);
        parser.addTalkerID(NmeaIDs.TalkerID.GN);
        parser.addListener(this);


        SharedPreferences prefs = TtAppCtx.getDeviceSettings().getPrefs();

        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);

            if (TtAppCtx.getDeviceSettings().getGpsExternal()) {
                _deviceUUID = TtAppCtx.getDeviceSettings().getGpsDeviceID();
            }

            if (TtAppCtx.getDeviceSettings().isGpsConfigured() && TtAppCtx.getDeviceSettings().isGpsAlwaysOn()) {
                startGps();
            }

            logBurstDetails = TtAppCtx.getDeviceSettings().getGpsLogBurstDetails();
        } else {
            TtUtils.TtReport.writeError("Unable to get preferences", "GpsService:onCreate");
        }

        postServiceStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        postServiceStop();

        if (parser != null) {
            parser.removeListener(this);
        }

        if (logPrintWriter != null) {
            logPrintWriter.flush();
            logPrintWriter.close();
        }

        TtAppCtx.getDeviceSettings().getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case DeviceSettings.GPS_ALWAYS_ON:
            case DeviceSettings.GPS_CONFIGURED: {
                boolean keepOn = sharedPreferences.getBoolean(DeviceSettings.GPS_ALWAYS_ON, false);
                boolean gpsConfigured = sharedPreferences.getBoolean(DeviceSettings.GPS_CONFIGURED, false);

                boolean running = isGpsRunning();

                if (keepOn && gpsConfigured && !running) {
                    startGps();
                } else if (running && !keepOn){
                    stopGps();
                }
                break;
            }
            case DeviceSettings.GPS_LOG_BURST_DETAILS: {
                logBurstDetails = sharedPreferences.getBoolean(DeviceSettings.GPS_LOG_BURST_DETAILS,
                        TtAppCtx.getDeviceSettings().DEFAULT_GPS_LOG_BURST_DETAILS);
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
                TtAppCtx.getTtNotifyManager().setGpsOn(isExternalGpsUsed());

                if (logging) {
                    writeStartLog();
                }
            }
        } else {
            status = GpsDeviceStatus.GpsAlreadyStarted;
            //TtAppCtx.getTtNotifyManager().setGpsOn(isExternalGpsUsed());
        }

        return status;
    }

    private GpsDeviceStatus stopGps() {
        GpsDeviceStatus status;

        if (logging) {
            writeEndLog();
        }

        if (isGpsRunning()) {
            status = (_deviceUUID == null) ? stopInternalGps() : stopExternalGps();
        } else {
            status = GpsDeviceStatus.GpsAlreadyStopped;
        }

        TtAppCtx.getTtNotifyManager().setGpsOff();

        return status;
    }

    private GpsDeviceStatus startInternalGps() {
        try {
            if(locManager == null)
                locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

            if (isInternalGpsEnabled()) {
                if (AndroidUtils.App.checkLocationPermission(this)) {
                    locManager.addNmeaListener(this);
                    locManager.registerGnssStatusCallback(mGnssStatusCallback);
                    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, GPS_MINIMUM_DISTANCE, this);
                    return GpsDeviceStatus.InternalGpsStarted;
                } else {
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
            if (AndroidUtils.App.checkLocationPermission(this)) {
                locManager.removeNmeaListener(this);
                locManager.unregisterGnssStatusCallback(mGnssStatusCallback);
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
                if (btConn != null) {
                    btConn.unregister(this);
                    btConn.disconnect();
                }

                btConn = new BluetoothConnection(socket);
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
        if (btConn != null) {
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
                isExternalGpsUsed() ? String.format(" [%s]", TtAppCtx.getDeviceSettings().getGpsDeviceName()) : StringEx.Empty));
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
        if (isGpsRunning())
            throw new RuntimeException("GPS must be stopped before setting provider.");
        else
            _deviceUUID = deviceUUID;
    }

    public boolean isGpsRunning() {
        if(getGpsProvider() == GpsProvider.Internal) {
            return locManager != null;
        } else {
            return btConn != null && btConn.isConnected() && btConn.isReceiving();
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
    }

    @Override
    public void connectionEnded() {
        TtAppCtx.getTtNotifyManager().setGpsOff();
        //postError(GpsError.DeviceConnectionEnded);
    }

    @Override
    public void failedToConnect() {
        TtAppCtx.getTtNotifyManager().setGpsOff();
        postError(GpsError.FailedToConnect);
    }

    //endregion

    //region Internal Android GPS
//    @Override
//    public void onNmeaReceived(long timestamp, String nmea) {
//        parseNmeaString(nmea);
//    }


    @Override
    public void onNmeaMessage(String message, long timestamp) {
        parseNmeaString(message);
    }

    @Override
    public void onLocationChanged(Location location) {
        //
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
    public void onBurstReceived(INmeaBurst burst) {
        postINmeaBurst(burst);

        if (burst.hasPosition()) {
            lastPosition = burst.getPosition();

            if (gmapListener != null) {
                Location location = new Location(StringEx.Empty);

                location.setLatitude(lastPosition.getLatitudeSignedDecimal());
                location.setLongitude(lastPosition.getLongitudeSignedDecimal());
                location.setAltitude(lastPosition.hasElevation() ? lastPosition.getElevation() : 0);

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
    private void postINmeaBurst(final INmeaBurst burst) {
        for(final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.nmeaBurstReceived(burst);
                    }
                });

            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaString(final String nmeaString) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.nmeaStringReceived(nmeaString);
                    }
                });

            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaSentence(final NmeaSentence sentence) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.nmeaSentenceReceived(sentence);
                    }
                });

            } catch (Exception ex) {
                //
            }
        }
    }

    private void postGpsStart() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.gpsStarted();
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postGpsStop() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.gpsStopped();
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postServiceStart() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.gpsServiceStarted();
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postServiceStop() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.gpsServiceStopped();
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postError(final GpsError error) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.gpsError(error);
                        } catch (Exception e) {
                            TtUtils.TtReport.writeError(e.getMessage(), "GpsService:postError");
                        }
                    }
                });
            } catch (Exception ex) {
                TtUtils.TtReport.writeError(ex.getMessage(), "GpsService:postError-handler.post");
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
    }
    //endregion


    public enum GpsError {
        LostDeviceConnection,
        DeviceConnectionEnded,
        NoExternalGpsSocket,
        FailedToConnect,
        Unknown
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
        Unknown
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
        public boolean addListener(Listener callback) {
            if (!listeners.contains(callback)) {
                listeners.add(callback);
                return true;
            }

            return false;
        }

        @Override
        public void removeListener(Listener callback) {
            listeners.remove(callback);
        }

        public boolean isListening(Listener callback) {
            return listeners.contains(callback);
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

            postServiceStop();
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
        void nmeaBurstReceived(INmeaBurst INmeaBurst);
        void nmeaStringReceived(String nmeaString);
        void nmeaSentenceReceived(NmeaSentence nmeaSentence);

        void gpsStarted();
        void gpsStopped();

        void gpsServiceStarted();
        void gpsServiceStopped();
        void gpsError(GpsError error);
    }


    public interface Controller {
        boolean addListener(Listener callback);

        void removeListener(Listener callback);

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
