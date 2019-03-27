package com.usda.fmsc.twotrails.rangefinder;


import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.usda.fmsc.twotrails.DeviceSettings;
import com.usda.fmsc.twotrails.TwoTrailApp;
import com.usda.fmsc.twotrails.devices.BluetoothConnection;
import com.usda.fmsc.twotrails.devices.TtBluetoothManager;
import com.usda.fmsc.twotrails.units.Dist;
import com.usda.fmsc.twotrails.units.Slope;
import com.usda.fmsc.twotrails.utilities.TtUtils;
import com.usda.fmsc.utilities.ParseEx;

import org.joda.time.DateTime;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class RangeFinderService extends Service implements BluetoothConnection.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TwoTrailApp TtAppCtx;

    private boolean postAllRFStrings = true, logging;
    private TtRangeFinderData lastRFData;

    private ArrayList<Listener> listeners = new ArrayList<>();
    private final Binder binder = new RangeFinderBinder();

    private TtBluetoothManager bluetoothManager;
    private BluetoothConnection btConn;

    private PrintWriter logPrintWriter;

    private String _deviceUUID;

    
    public RangeFinderService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TtAppCtx = (TwoTrailApp) getApplicationContext();

        bluetoothManager = new TtBluetoothManager();

        SharedPreferences prefs = TtAppCtx.getDeviceSettings().getPrefs();

        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);

            _deviceUUID = TtAppCtx.getDeviceSettings().getRangeFinderDeviceID();

            if (TtAppCtx.getDeviceSettings().isRangeFinderConfigured() && TtAppCtx.getDeviceSettings().isRangeFinderAlwaysOn()) {
                startRangeFinder();
            }
        } else {
            TtAppCtx.getReport().writeError("Unable to get preferences", "RangeFinderService:onCreate");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
            case DeviceSettings.RANGE_FINDER_ALWAYS_ON:
            case DeviceSettings.RANGE_FINDER_CONFIGURED: {
                boolean keepOn = sharedPreferences.getBoolean(DeviceSettings.RANGE_FINDER_ALWAYS_ON, false);
                boolean rangeFinderConfigured = sharedPreferences.getBoolean(DeviceSettings.RANGE_FINDER_CONFIGURED, false);

                boolean running = isRangeFinderRunning();

                if (keepOn && rangeFinderConfigured && !running) {
                    startRangeFinder();
                } else if (running && !keepOn){
                    stopRangeFinder();
                }
                break;
            }
        }
    }



    //region Start / Stop RangeFinder
    private RangeFinderDeviceStatus startRangeFinder() {
        RangeFinderDeviceStatus status;

        if (!isRangeFinderRunning()) {
            status = startExternalRangeFinder();

            if (status == RangeFinderDeviceStatus.RangeFinderStarted) {
                if (logging) {
                    writeStartLog();
                }
            }
        } else {
            status = RangeFinderDeviceStatus.RangeFinderAlreadyStarted;
        }

        return status;
    }

    private RangeFinderDeviceStatus stopRangeFinder() {
        RangeFinderDeviceStatus status;

        if (isRangeFinderRunning()) {
            status = stopExternalRangeFinder();

            if (logging) {
                writeEndLog();
            }

            logging = false;

            if (logPrintWriter != null) {
                logPrintWriter.flush();
                logPrintWriter.close();
            }
        } else {
            status = RangeFinderDeviceStatus.RangeFinderAlreadyStopped;
        }

        return status;
    }

    private RangeFinderDeviceStatus startExternalRangeFinder() {
        try {
            BluetoothSocket socket = bluetoothManager.getSocket(_deviceUUID);

            if (socket != null) {
                btConn = new BluetoothConnection(socket);
                btConn.register(this);
                btConn.start();
                postRangeFinderStart();
            } else {
                if (btConn != null) {
                    btConn.disconnect();
                }
                postError(RangeFinderError.NoExternalRangeFinderSocket);

                return RangeFinderDeviceStatus.RangeFinderNotFound;
            }
        } catch (Exception e) {
            TtAppCtx.getReport().writeError(e.getMessage(), "RangeFinderService:startExternalRangeFinder");
            return RangeFinderDeviceStatus.RangeFinderError;
        }

        return RangeFinderDeviceStatus.RangeFinderStarted;
    }

    private RangeFinderDeviceStatus stopExternalRangeFinder() {
        if (btConn != null) {
            btConn.unregister(this);
            btConn.disconnect();
            btConn = null;

            postRangeFinderStop();  //needs to be triggered manually
            return RangeFinderDeviceStatus.RangeFinderStopped;
        }

        return RangeFinderDeviceStatus.RangeFinderError;
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
        logPrintWriter.println(String.format("[%s] RangeFinder Started [%s]", DateTime.now(), TtAppCtx.getDeviceSettings().getRangeFinderDeviceName()));
        logPrintWriter.flush();
    }

    private void writeEndLog() {
        logPrintWriter.println(String.format("[%s] RangeFinder Stopped", DateTime.now()));
        logPrintWriter.flush();
    }

    public boolean isLogging() {
        return logging;
    }
    //endregion


    public void setRangeFinderProvider(String deviceUUID) throws RuntimeException {
        if(isRangeFinderRunning())
            throw new RuntimeException("RangeFinder must be stopped before setting provider.");
        else
            _deviceUUID = deviceUUID;
    }

    public boolean isRangeFinderRunning() {
        return btConn != null && btConn.isConnected();
    }

    @Override
    public void receivedString(String data) {
        parseRFString(data);
    }

    @Override
    public void connectionLost() {
        postError(RangeFinderError.LostDeviceConnection);
    }

    @Override
    public void connectionEnded() {
        //postError(RangeFinderError.DeviceConnectionEnded);
    }

    @Override
    public void failedToConnect() {
        postError(RangeFinderError.FailedToConnect);
    }



    private void parseRFString(String rfString) {
        if (rfString != null) {
            rfString = rfString.trim();

            boolean valid = false;

            if ((validateChecksum(rfString) && (valid = parseRFData(rfString))) || postAllRFStrings) {
                postRFStringReceived(rfString, valid);
            }

            if (logging) {
                try {
                    logPrintWriter.println(rfString);
                    logPrintWriter.flush();
                } catch (Exception e) {
                    TtAppCtx.getReport().writeError(e.getMessage(), "RangeFinderService:parseRFString:logToFile");
                }
            }
        }
    }

    public static boolean validateChecksum(String data) {
        if (data.length() > 10 && data.startsWith("$") && data.contains("*")) {
            String calcString = data.substring(1);
            int ast = calcString.indexOf("*");
            String checkSumStr = calcString.substring(ast + 1, ast + 3);
            calcString = calcString.substring(0, ast);

            int checksum = 0;

            for(int i = 0; i < calcString.length(); i++) {
                checksum ^= (byte)calcString.charAt(i);
            }

            String hex = Integer.toHexString(checksum);
            if (hex.length() < 2) {
                hex = "0" + hex;
            }

            return hex.equalsIgnoreCase(checkSumStr);
        }

        return false;
    }

    private boolean parseRFData(String rfString) {
        String[] tokens = rfString.substring(0, rfString.indexOf("*")).split(",", -1);

        if (tokens.length > 9 && tokens[1].length() > 0) {
            try {
                String horizVectorMsg = tokens[1];
                Double horizDist = ParseEx.parseDouble(tokens[2]);
                Dist horizDistType = Dist.parse(tokens[3]);
                Double azimuth = ParseEx.parseDouble(tokens[4]);
                Slope azType = Slope.parse(tokens[5]);
                Double inclination = ParseEx.parseDouble(tokens[6]);
                Slope incType = Slope.parse(tokens[5]);
                Double slopeDist = ParseEx.parseDouble(tokens[8]);
                Dist slopeDistType = Dist.parse(tokens[9]);

                lastRFData = TtRangeFinderData.create(horizVectorMsg, horizDist, horizDistType,
                        azimuth, azType, inclination, incType, slopeDist, slopeDistType);

                postRFDataReceived(lastRFData);

                return true;
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError(ex.getMessage(), "RangeFinderService:parseRFData");
            }
        }

        return false;
    }

    public TtRangeFinderData getLastRFData() {
        return lastRFData;
    }


    //region Post Events
    private void postRFDataReceived(final TtRangeFinderData data) {
        for(final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.rfDataReceived(data);
                    }
                });

            } catch (Exception ex) {
                //
            }
        }
    }

    private void postRFStringReceived(final String rfString, final boolean valid) {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (valid)
                            listener.rfStringReceived(rfString);
                        else
                            listener.rfInvalidStringReceived(rfString);
                    }
                });

            } catch (Exception ex) {
                //
            }
        }
    }

    private void postRangeFinderStart() {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.rangeFinderStarted();
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("RangeFinderService:postStart", ex.getMessage());
            }
        }
    }

    private void postRangeFinderStop() {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.rangeFinderStopped();
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("RangeFinderService:postStop", ex.getMessage());
            }
        }
    }

    private void postServiceStart() {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.rangeFinderServiceStarted();
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("RangeFinderService:postStart", ex.getMessage());
            }
        }
    }

    private void postServiceStop() {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.rangeFinderServiceStopped();
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("RangeFinderService:postStop", ex.getMessage());
            }
        }
    }

    private void postError(final RangeFinderError error) {
        for (final Listener listener : listeners) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.rangeFinderError(error);
                        } catch (Exception e) {
                            TtAppCtx.getReport().writeError(e.getMessage(), "RangeFinderService:postError");
                        }
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError(ex.getMessage(), "RangeFinderService:postError-handler.post");
            }
        }
    }
    //endregion


    public enum RangeFinderError {
        LostDeviceConnection,
        DeviceConnectionEnded,
        NoExternalRangeFinderSocket,
        FailedToConnect,
        Unknown
    }

    public enum RangeFinderDeviceStatus {
        RangeFinderStarted,
        RangeFinderStopped,
        RangeFinderNeedsPermissions,
        RangeFinderError,
        RangeFinderServiceInUse,
        RangeFinderNotFound,
        RangeFinderAlreadyStarted,
        RangeFinderAlreadyStopped,
        Unknown
    }


    public class RangeFinderBinder extends Binder implements Controller {

        public RangeFinderService getService() {
            return RangeFinderService.this;
        }

        //@Override
        public void addListener(Listener callback) {
            if (!listeners.contains(callback)) {
                listeners.add(callback);
            }
        }

        @Override
        public void removeListener(Listener callback) {
            try {
                if (listeners.contains(callback)) {
                    listeners.remove(callback);
                }
            } catch (Exception e) {
                TtAppCtx.getReport().writeError(e.getMessage(), "RangeFinderService:removeListener", e.getStackTrace());
            }
        }

        public boolean isListening(Listener callback) {
            return listeners.contains(callback);
        }


        @Override
        public RangeFinderDeviceStatus startRangeFinder() {
            return RangeFinderService.this.startRangeFinder();
        }

        @Override
        public RangeFinderDeviceStatus stopRangeFinder() {
            return RangeFinderService.this.stopRangeFinder();
        }

        @Override
        public void stopService() {
            RangeFinderService.this.stopRangeFinder();
            RangeFinderService.this.stopLogging();
            RangeFinderService.this.stopSelf();
        }

        @Override
        public void setRangeFinderProvider(String provider) {
            RangeFinderService.this.setRangeFinderProvider(provider);
        }

        @Override
        public boolean isRangeFinderRunning() {
            return RangeFinderService.this.isRangeFinderRunning();
        }

        public boolean postsAllRFStrings() {
            return RangeFinderService.this.postAllRFStrings;
        }

        @Override
        public void startLogging(String fileName) {
            RangeFinderService.this.startLogging(fileName);
        }

        @Override
        public void stopLogging() {
            RangeFinderService.this.stopLogging();
        }

        @Override
        public boolean isLogging() {
            return RangeFinderService.this.isLogging();
        }
    }


    public interface Listener {
        void rfDataReceived(TtRangeFinderData rfData);
        void rfStringReceived(String rfString);
        void rfInvalidStringReceived(String rfString);

        void rangeFinderStarted();
        void rangeFinderStopped();

        void rangeFinderServiceStarted();
        void rangeFinderServiceStopped();
        void rangeFinderError(RangeFinderError error);
    }


    public interface Controller {
        void addListener(Listener callback);

        void removeListener(Listener callback);

        RangeFinderDeviceStatus startRangeFinder();

        RangeFinderDeviceStatus stopRangeFinder();

        void setRangeFinderProvider(String provider) throws Exception;

        void stopService();

        boolean isRangeFinderRunning();

        void startLogging(String fileName);

        void stopLogging();

        boolean isLogging();
    }
}
