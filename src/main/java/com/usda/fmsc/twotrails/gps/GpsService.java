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

import androidx.annotation.NonNull;

import com.google.android.gms.maps.LocationSource;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.base.parsers.ParseMode;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaBurst;
import com.usda.fmsc.geospatial.gnss.nmea.GnssNmeaParser;
import com.usda.fmsc.geospatial.nmea.INmeaParserListener;
import com.usda.fmsc.geospatial.nmea.codes.TalkerID;
import com.usda.fmsc.geospatial.nmea.sentences.NmeaSentence;
import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.devices.BluetoothConnection;
import com.usda.fmsc.utilities.StringEx;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;

public class GpsService extends Service implements LocationListener, LocationSource, OnNmeaMessageListener,
        INmeaParserListener<NmeaSentence, GnssNmeaBurst>, BluetoothConnection.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    public final int GPS_UPDATE_INTERVAL = 1000;    //in milliseconds
    public final int NMEA_WAIT_TIMEOUT = 5000;      //in milliseconds
    public final int GPS_MINIMUM_DISTANCE = 0;      //in meters

    private TwoTrailsApp TtAppCtx;

    private OnLocationChangedListener gmapListener;

    private boolean logging, logBurstDetails, receivingValidBursts;
    private Position lastPosition;

    private final ArrayList<Listener> listeners = new ArrayList<>();
    private final Binder binder = new GpsBinder();

    private BluetoothConnection btConn;

    private PrintWriter logPrintWriter;

    private String _deviceUUID;
    private LocationManager locManager;

    private GnssNmeaParser parser;
    private PostDelayHandler nmeaReceived;
    private boolean receivingNmea;

    private final GnssStatus.Callback mGnssStatusCallback = new GnssStatus.Callback() {
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
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            //
        }
    };


    public GpsService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TtAppCtx = (TwoTrailsApp) getApplicationContext();

        if (TtAppCtx.getDeviceSettings().isGpsParsingByTime()) {
            parser = new GnssNmeaParser(EnumSet.of(TalkerID.GP, TalkerID.GL, TalkerID.GA, TalkerID.GN));
        } else {
            parser = new GnssNmeaParser(EnumSet.of(TalkerID.GP, TalkerID.GL, TalkerID.GA, TalkerID.GN), TtAppCtx.getDeviceSettings().getGpsParseDelimiter());
        }
        parser.addListener(this);

        nmeaReceived = new PostDelayHandler(NMEA_WAIT_TIMEOUT, () -> {
            if (receivingNmea) {
                if (isGpsRunning()) {
                    postReceivingNmeaStrings(false);
                }

                receivingNmea = false;
            }
        });

        SharedPreferences prefs = TtAppCtx.getDeviceSettings().getPrefs();

        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);

            if (TtAppCtx.getDeviceSettings().getGpsExternal()) {
                _deviceUUID = TtAppCtx.getDeviceSettings().getGpsDeviceID();
            }

            if (TtAppCtx.getDeviceSettings().isGpsConfigured() && TtAppCtx.getDeviceSettings().isGpsAlwaysOn()) {
                if (_deviceUUID != null || AndroidUtils.App.checkLocationPermission(TtAppCtx)) {
                    startGps();
                }
            }

            logBurstDetails = TtAppCtx.getDeviceSettings().getGpsLogBurstDetails();
        } else {
            TtAppCtx.getReport().writeError("Unable to get preferences", "GpsService:onCreate");
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
                        DeviceSettings.DEFAULT_GPS_LOG_BURST_DETAILS);
            }
            case DeviceSettings.GPS_PARSE_METHOD: {
                if (sharedPreferences.getBoolean(DeviceSettings.GPS_PARSE_METHOD, DeviceSettings.DEFAULT_GPS_PARSE_METHOD)) {
                    parser.setParseMode(ParseMode.Time);
                } else {
                    parser.setDelimiter(sharedPreferences.getString(DeviceSettings.GPS_PARSE_DELIMITER, DeviceSettings.DEFAULT_GPS_PARSE_DELIMITER));
                    parser.setParseMode(ParseMode.Delimiter);
                }
            }
        }
    }



    //region Start / Stop GPS
    private GpsDeviceStatus startGps() {
        GpsDeviceStatus status;

        if (!isGpsRunning()) {
            parser.reset();

            status = (_deviceUUID == null) ?
                    startInternalGps() : startExternalGps();

            if (status == GpsDeviceStatus.ExternalGpsStarted ||
                    status == GpsDeviceStatus.InternalGpsStarted) {
                //started
                TtAppCtx.getTtNotifyManager().setGpsOn(isExternalGpsUsed());

                if (logging) {
                    writeStartLog();
                }

                receivingNmea = true;
                nmeaReceived.post();
            }
        } else {
            status = GpsDeviceStatus.GpsAlreadyStarted;
            TtAppCtx.getTtNotifyManager().setGpsOn(isExternalGpsUsed());

            receivingNmea = true;
            nmeaReceived.post();
        }

        return status;
    }

    private GpsDeviceStatus stopGps() {
        GpsDeviceStatus status;

        if (logging) {
            writeEndLog();
        }

        logging = false;
        receivingValidBursts = false;

        if (logPrintWriter != null) {
            logPrintWriter.flush();
            logPrintWriter.close();
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
        if (!AndroidUtils.App.checkLocationPermission(getApplicationContext())) {
            return GpsDeviceStatus.InternalGpsRequiresPermissions;
        }

        try {
            if(locManager == null)
                locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

            if (isInternalGpsEnabled()) {
                if (AndroidUtils.App.checkLocationPermission(this)) {
                    locManager.addNmeaListener(this);
                    locManager.registerGnssStatusCallback(mGnssStatusCallback);
                    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, GPS_MINIMUM_DISTANCE, this);

                    if (TtAppCtx.hasReport()) {
                        TtAppCtx.getReport().writeEvent("Internal GPS started");
                    }

                    return GpsDeviceStatus.InternalGpsStarted;
                } else {
                    return GpsDeviceStatus.InternalGpsNeedsPermissions;
                }
            } else {
                return GpsDeviceStatus.InternalGpsNotEnabled;
            }
        } catch (Exception ex) {
            TtAppCtx.getReport().writeError(ex.getMessage(), "GpsService:startInternalGps");
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
        if (!AndroidUtils.App.checkBluetoothScanAndConnectPermission(getApplicationContext())) {
            return GpsDeviceStatus.ExternalGpsRequiresPermissions;
        }

        try {
            BluetoothSocket socket = TtAppCtx.getBluetoothManager().getSocket(_deviceUUID);

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
            TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:startExternalGps");
            return GpsDeviceStatus.ExternalGpsError;
        }

        return GpsDeviceStatus.ExternalGpsConnecting;
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
    public void startLogging(File logFile) {
        try {
            if (logging && logPrintWriter != null) {
                logPrintWriter.close();
            }

//            File logFileDir = new File(TtUtils.getTtLogFileDir());
//            if (!logFileDir.exists()) {
//                logFileDir.mkdirs();
//            }

            logPrintWriter = new PrintWriter(new FileWriter(logFile, true));

            writeStartLog();

            logging = true;
        } catch (Exception e) {
            if (TtAppCtx.hasReport()) {
                TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:startLogging", e.getStackTrace());
            }
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
        logPrintWriter.println(String.format("[%s] %s GPS Started [%s]", DateTime.now(),
                isExternalGpsUsed() ? "External" : "Internal",
                isExternalGpsUsed() ?
                    TtAppCtx.getDeviceSettings().getGpsDeviceName() :
                    android.os.Build.MODEL));
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
        else {
            _deviceUUID = deviceUUID;

            if (TtAppCtx.hasReport()) {
                TtAppCtx.getReport().writeEvent("GPS changed to: " +
                        (_deviceUUID == null ? "Internal" : TtAppCtx.getDeviceSettings().getGpsDeviceName() + " (" + _deviceUUID + ")"));
            }
        }
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
        return _deviceUUID != null;// && btConn != null;
    }

    public GpsProvider getGpsProvider() {
        return _deviceUUID == null ? GpsProvider.Internal : GpsProvider.External;
    }
    public Position getLastPosition() {
        return lastPosition;
    }


    //region External Device GPS
    @Override
    public void receivedString(String data) {
        parseNmeaString(data);
    }

    @Override
    public void connectionStarted() {
        postGpsStart();
        if (TtAppCtx.hasReport()) {
            TtAppCtx.getReport().writeEvent("External GPS connection started");
        }
    }

    @Override
    public void connectionLost() {
        TtAppCtx.getTtNotifyManager().setGpsOff();

        if (btConn != null) {
            btConn.unregister(this);
            btConn = null;
        }

        if (TtAppCtx.hasReport()) {
            TtAppCtx.getReport().writeError("External GPS lost connection", "GpsService:connectionLost");
        }

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
    @Override
    public void onNmeaMessage(String message, long timestamp) {
        parseNmeaString(message);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        //
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //not available after Android Q
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        //if gps or coarse loc gets turned on
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        //if gps or coarse loc gets turned off
    }
    //endregion

    //region Parsing
    private void parseNmeaString(String nmeaString) {
        if (nmeaString != null) {
            nmeaString = nmeaString.trim();

            if (parser.isSynced() || parser.sync(nmeaString)) {
                try {
                    if (nmeaString.indexOf("$", 1) > -1) {
                        String[] nmeaStrings = nmeaString.split("\\$");

                        for (String ns : nmeaStrings) {
                            parser.parse("$" + ns);
                        }
                    } else {
                        parser.parse(nmeaString);
                    }
                } catch (Exception e) {
                    parser.reset();
                    postNmeaBurstValidityChanged(false);
                }
            }

            postNmeaString(nmeaString);

            if (logging) {
                try {
                    logPrintWriter.println(nmeaString);
                    logPrintWriter.flush();
                } catch (Exception e) {
                    TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:parseNmeaString:logToFile");
                }
            }

            if (!receivingNmea) {
                postReceivingNmeaStrings(true);
                receivingNmea = true;
            }

            nmeaReceived.post();
        }
    }

    @Override
    public void onBurstReceived(GnssNmeaBurst burst) {
        postNmeaBurst(burst);

        if (!receivingValidBursts) {
            receivingValidBursts = true;
            postNmeaBurstValidityChanged(true);
        }

        if (burst.hasPosition()) {
            lastPosition = burst.getPosition();

            if (gmapListener != null) {
                Location location = new Location(StringEx.Empty);

                location.setLatitude(lastPosition.getLatitude());
                location.setLongitude(lastPosition.getLongitude());
                location.setAltitude(lastPosition.hasElevation() ? lastPosition.getElevation() : 0);

                try {
                    gmapListener.onLocationChanged(location);
                } catch (Exception e) {
                    e.printStackTrace();
                    TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:onBurstReceived:gmap");
                }
            }
        }

        if (logBurstDetails) {
            try {
                logPrintWriter.println(burst.toString());
                logPrintWriter.flush();
            } catch (Exception e) {
                TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:parseNmeaString:logToFile");
            }
        }
    }

    @Override
    public void onNmeaReceived(NmeaSentence sentence) {
        postNmeaSentence(sentence);
    }

    @Override
    public void onMessageReceived(NmeaSentence sentence) {
        //
    }

    @Override
    public void onInvalidMessageReceived(String invalidMessageData) {
        //
    }
    //endregion

    //region Post Events
    private void postNmeaBurst(final GnssNmeaBurst burst) {
        for(final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaBurstReceived(burst));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaString(final String nmeaString) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaStringReceived(nmeaString));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaSentence(final NmeaSentence sentence) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaSentenceReceived(sentence));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaBurstValidityChanged(final boolean receivingValidBursts) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaBurstValidityChanged(receivingValidBursts));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postReceivingNmeaStrings(final boolean receivingNmea) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.receivingNmeaStrings(receivingNmea);
                });
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postGpsStart() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::gpsStarted);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postGpsStop() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::gpsStopped);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postServiceStart() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::gpsServiceStarted);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("GpsService:postStart", ex.getMessage());
            }
        }
    }

    private void postServiceStop() {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::gpsServiceStopped);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("GpsService:postStop", ex.getMessage());
            }
        }
    }

    private void postError(final GpsError error) {
        for (final Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        listener.gpsError(error);
                    } catch (Exception e) {
                        TtAppCtx.getReport().writeError(e.getMessage(), "GpsService:postError");
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError(ex.getMessage(), "GpsService:postError-handler.post");
            }
        }
    }
    //endregion

    //region LocationSource for GMaps
    @Override
    public void activate(@NonNull OnLocationChangedListener onLocationChangedListener) {
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
        InternalGpsRequiresPermissions,
        ExternalGpsConnecting,
        ExternalGpsStarted,
        ExternalGpsStopped,
        ExternalGpsNotFound,
        ExternalGpsNotConnected,
        ExternalGpsError,
        ExternalGpsRequiresPermissions,
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
        public boolean areGpsBurstsValid() {
            return GpsService.this.receivingValidBursts;
        }

        @Override
        public GpsProvider getGpsProvider() {
            return GpsService.this.getGpsProvider();
        }

        @Override
        public void startLogging(File logFile) {
            GpsService.this.startLogging(logFile);
        }

        @Override
        public void stopLogging() {
            GpsService.this.stopLogging();
        }

        @Override
        public boolean isLogging() {
            return GpsService.this.isLogging();
        }

        public Position getLastPosition() {
            return GpsService.this.getLastPosition();
        }
    }


    public interface Listener {
        void nmeaBurstReceived(GnssNmeaBurst GnssNmeaBurst);
        void nmeaStringReceived(String nmeaString);
        void nmeaSentenceReceived(NmeaSentence nmeaSentence);
        void nmeaBurstValidityChanged(boolean burstsAreValid);
        void receivingNmeaStrings(boolean receiving);

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

        boolean areGpsBurstsValid();

        GpsProvider getGpsProvider();

        void startLogging(File logFile);

        void stopLogging();

        boolean isLogging();
    }
}
