package com.usda.fmsc.twotrails.ins;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.geospatial.ins.IINSData;
import com.usda.fmsc.geospatial.ins.vectornav.INSStatus;
import com.usda.fmsc.geospatial.ins.vectornav.IVNMsgListener;
import com.usda.fmsc.geospatial.ins.vectornav.VNDataReader;
import com.usda.fmsc.geospatial.ins.vectornav.VNInsData;
import com.usda.fmsc.geospatial.ins.vectornav.VNParser;
import com.usda.fmsc.geospatial.ins.vectornav.binary.BinaryMsgConfig;
import com.usda.fmsc.geospatial.ins.vectornav.binary.messages.VNBinMessage;
import com.usda.fmsc.geospatial.ins.vectornav.nmea.sentences.base.VNNmeaSentence;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.devices.VNSerialBluetoothConnection;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class VNInsService extends Service implements
        IVNMsgListener,
        VNSerialBluetoothConnection.Listener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public final int DATA_WAIT_TIMEOUT = 5000;      //in milliseconds
    private final ArrayList<VNInsService.Listener> listeners = new ArrayList<>();

    private TwoTrailsApp TtAppCtx;

    private final Binder binder = new VNInsBinder();

    private boolean logging, logDetails, receivingValidData;
    private PrintWriter logPrintWriter;

    private VNSerialBluetoothConnection btConn;
    private String _deviceUUID;
    private InsMode _mode;

    private VNParser parser;
    private PostDelayHandler dataReceived;
    private boolean receivingData;

    private final VNDataReader.Listener vnDataReaderListner = new VNDataReader.Listener() {
        @Override
        public void onBinMsgBytesReceived(BinaryMsgConfig config, byte[] data) {
            onValidDataReceived(true);
            parser.parseBinMessage(data);
        }

        @Override
        public void onNmeaMsgBytesReceived(byte[] data) {
            onValidDataReceived(true);
            parser.parseNmeaMessage(data);
        }

        @Override
        public void onInvalidDataRecieved(byte[] data) {
            onValidDataReceived(false);
            parser.onInvalidData(data);
        }

        private void onValidDataReceived(boolean valid) {
            if (!receivingData) {
                postReceivingData(true);
                receivingData = true;
            }
            if (receivingValidData != valid) {
                receivingValidData = valid;
                postReceivingValidDataChanged(receivingValidData);
            }
        }
    };



    public VNInsService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        TtAppCtx = (TwoTrailsApp) getApplicationContext();

        dataReceived = new PostDelayHandler(DATA_WAIT_TIMEOUT, () -> {
            if (receivingData) {
                if (btConn != null && btConn.isConnected() && btConn.isReceiving()) {
                    postReceivingData(false);
                }

                receivingData = false;
            }
        });


        //settings


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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //TODO watch for changing settings
    }

    public InsMode getInsMode() {
        return _mode;
    }

    public void SetInsMode(InsMode mode) {
        _mode = mode;

        //TODO CHANGE INS MODE
    }

    //region Start / Stop
    private InsDeviceStatus startIns() {
        return InsDeviceStatus.InsStarted;
    }
    private InsDeviceStatus stopIns() {
        return InsDeviceStatus.InsStarted;
    }

    private InsDeviceStatus startInsSerial() {
        return InsDeviceStatus.InsStarted;
    }
    private InsDeviceStatus stopInsSerial() {
        return InsDeviceStatus.InsStarted;
    }

    private InsDeviceStatus startInsGatt() {
        return InsDeviceStatus.InsStarted;
    }
    private InsDeviceStatus stopInsGatt() {
        return InsDeviceStatus.InsStarted;
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
        logPrintWriter.println(String.format("[%s][%s] INS Started (Mode: %s) ",
                DateTime.now(),
                TtAppCtx.getDeviceSettings().getGpsDeviceName(),
                getInsMode() == InsMode.Serial ? "Serial" : "GATT")); //todo CHANGE TO INS
        logPrintWriter.flush();
    }

    private void writeEndLog() {
        logPrintWriter.println(String.format("[%s] INS Stopped", DateTime.now()));
        logPrintWriter.flush();
    }

    public boolean isLogging() {
        return logging;
    }
    //endregion

    //region INS Parser
    @Override
    public void onInsData(VNInsData data) {
        postInsData(data);
    }

    @Override
    public void onBinMsgReceived(VNBinMessage message) {
        //
    }

    @Override
    public void onNmeaMsgReceived(VNNmeaSentence sentence) {
        postNmeaSentence(sentence);
    }

    @Override
    public void onInvalidDataRecieved(byte[] data) {
        //
    }
    //endregion

    //region Bluetooth
    @Override
    public void connectionStarted() {

    }

    @Override
    public void connectionLost() {

    }

    @Override
    public void connectionEnded() {

    }

    @Override
    public void failedToConnect() {

    }
    //endregion

    //region Post Events
    private void postInsData(final IINSData insData) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.insDataReceived(insData));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaString(final String nmeaString) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaStringReceived(nmeaString));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postNmeaSentence(final VNNmeaSentence sentence) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.nmeaSentenceReceived(sentence));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postReceivingData(final boolean receivingData) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.receivingData(receivingData));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postReceivingValidDataChanged(final boolean receivingValidData) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.receivingValidData(receivingValidData));
            } catch (Exception ex) {
                //
            }
        }
    }

    private void postInsStart() {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::insStarted);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("InsService:postStart", ex.getMessage());
            }
        }
    }

    private void postInsStop() {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::insStopped);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("InsService:postStop", ex.getMessage());
            }
        }
    }

    private void postServiceStart() {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::insServiceStarted);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("InsService:postStart", ex.getMessage());
            }
        }
    }

    private void postServiceStop() {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(listener::insServiceStopped);
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError("InsService:postStop", ex.getMessage());
            }
        }
    }

    private void postError(final VNInsService.InsError error) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        listener.insError(error);
                    } catch (Exception e) {
                        TtAppCtx.getReport().writeError(e.getMessage(), "InsService:postError");
                    }
                });
            } catch (Exception ex) {
                TtAppCtx.getReport().writeError(ex.getMessage(), "InsService:postError-handler.post");
            }
        }
    }


    //endregion

    public enum InsError {
        LostDeviceConnection,
        DeviceConnectionEnded,
        FailedToConnect,
        Unknown
    }

    public enum InsDeviceStatus {
        InsStarted,
        InsStopped,
        InsNotEnabled,
        InsError,
        InsRequiresPermissions,
        
        GnsAlreadyStarted,
        InsAlreadyStopped,
        InsServiceInUse,
        Unknown
    }

    public enum InsMode {
        Serial,
        GATT
    }

    public class VNInsBinder extends Binder implements VNInsService.Controller {
        public VNInsService getService() {
            return VNInsService.this;
        }

        //@Override
        public boolean addListener(VNInsService.Listener callback) {
            if (!listeners.contains(callback)) {
                listeners.add(callback);
                return true;
            }

            return false;
        }

        @Override
        public void removeListener(VNInsService.Listener callback) {
            listeners.remove(callback);
        }

        @Override
        public InsDeviceStatus startIns() {
            return null;
        }

        @Override
        public InsDeviceStatus stopIns() {
            return null;
        }

        @Override
        public void stopService() {

        }

        @Override
        public void setInsMode(InsMode mode) throws Exception {

        }

        @Override
        public InsMode getInsMode() {
            return null;
        }

        @Override
        public boolean isInsRunning() {
            return false;
        }

        @Override
        public void startLogging(File logFile) {

        }

        @Override
        public void stopLogging() {

        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }


    public interface Listener {
        void insDataReceived(IINSData data);
        void nmeaStringReceived(String nmeaString);
        void nmeaSentenceReceived(VNNmeaSentence nmeaSentence);
        void receivingData(boolean receiving);
        void receivingValidData(boolean receiving);



        void insStarted();
        void insStopped();

        void insServiceStarted();
        void insServiceStopped();
        void insError(VNInsService.InsError error);
    }


    public interface Controller {
        boolean addListener(VNInsService.Listener callback);

        void removeListener(VNInsService.Listener callback);

        VNInsService.InsDeviceStatus startIns();

        VNInsService.InsDeviceStatus stopIns();

        void stopService();

        void setInsMode(InsMode mode) throws Exception;

        InsMode getInsMode();

        boolean isInsRunning();

        void startLogging(File logFile);

        void stopLogging();

        boolean isLogging();
    }
}
