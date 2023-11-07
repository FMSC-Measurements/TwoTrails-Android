package com.usda.fmsc.twotrails.ins;


import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.PostDelayHandler;
import com.usda.fmsc.geospatial.ins.vectornav.IVNMsgListener;
import com.usda.fmsc.geospatial.ins.vectornav.VNDataReader;
import com.usda.fmsc.geospatial.ins.vectornav.VNInsData;
import com.usda.fmsc.geospatial.ins.vectornav.VNParser;
import com.usda.fmsc.geospatial.ins.vectornav.binary.BinaryMsgConfig;
import com.usda.fmsc.geospatial.ins.vectornav.binary.codes.CommonGroup;
import com.usda.fmsc.geospatial.ins.vectornav.binary.messages.VNBinMessage;
import com.usda.fmsc.geospatial.ins.vectornav.commands.VNCommand;
import com.usda.fmsc.geospatial.ins.vectornav.commands.attitude.TareCommand;
import com.usda.fmsc.geospatial.ins.vectornav.nmea.sentences.base.VNNmeaSentence;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.devices.VNSerialBluetoothConnection;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import kotlin.NotImplementedError;

public class VNInsService extends Service implements
        IVNMsgListener,
        VNSerialBluetoothConnection.Listener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public final int DATA_WAIT_TIMEOUT = 5000;      //in milliseconds
    public final byte[] TARE = new TareCommand().toBytes();
    private final ArrayList<VNInsService.Listener> listeners = new ArrayList<>();

    private TwoTrailsApp TtAppCtx;

    private final Binder binder = new VNInsBinder();

    private boolean logging, receivingValidData;
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
            parser.postInvalidData(data);
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
        TtAppCtx.getDeviceSettings().getPrefs().registerOnSharedPreferenceChangeListener(this);

        setDevice(TtAppCtx.getDeviceSettings().getVN100DeviceID());

        parser = new VNParser(new BinaryMsgConfig(CommonGroup.ALL_FIELDS_VN100));
        parser.addListener(this);

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
        //
    }

    public InsMode getInsMode() {
        //TODO update with GATT
        return InsMode.Serial;
    }

    public boolean isInsRunning() {
        return getInsMode() == InsMode.Serial ?
                btConn != null && btConn.isConnected() && btConn.isReceiving() :
                false;
    }

    public void setInsMode(InsMode mode) {
        if (_mode != mode) {
            if (isInsRunning()) {
                InsDeviceStatus status = stopIns();

                if (status == InsDeviceStatus.InsAlreadyStopped || status == InsDeviceStatus.InsStopped) {
                    startIns();
                }
            }

            _mode = mode;

            if (TtAppCtx.hasReport()) {
                TtAppCtx.getReport().writeEvent(String.format("INS mode switched to %s", getInsMode()));
            }
        }
    }

    public void setDevice(String deviceUUID) {
        if (isInsRunning())
            throw new RuntimeException("INS must be stopped before changing devices.");
        else {
            _deviceUUID = deviceUUID;

            if (TtAppCtx.hasReport()) {
                TtAppCtx.getReport().writeEvent(String.format("INS changed to: (%s)", _deviceUUID));
            }
        }
    }

    //region Start / Stop
    private InsDeviceStatus startIns() {
        InsDeviceStatus status;

        if (!isInsRunning()) {
            status = getInsMode() == InsMode.Serial ? startInsSerial() : startInsGatt();

            if (status == InsDeviceStatus.InsStarted) {
                if (logging) {
                    writeStartLog();
                }

                receivingData = true;
                dataReceived.post();
            }
        } else {
            status = InsDeviceStatus.InsAlreadyStarted;
            receivingData = true;
            dataReceived.post();
        }


        return status;
    }
    private InsDeviceStatus stopIns() {
        InsDeviceStatus status;

        if (logging) {
            writeEndLog();
        }

        logging = false;
        receivingValidData = false;

        if (logPrintWriter != null) {
            logPrintWriter.flush();
            logPrintWriter.close();
        }

        if (isInsRunning()) {
            status = getInsMode() == InsMode.Serial ? stopInsSerial() : stopInsGatt();
        } else {
            status = InsDeviceStatus.InsAlreadyStopped;
        }

        return status;
    }

    private InsDeviceStatus startInsSerial() {
        if (!AndroidUtils.App.checkBluetoothScanAndConnectPermission(getApplicationContext())) {
            return InsDeviceStatus.InsRequiresPermissions;
        }

        try {
            BluetoothSocket socket = TtAppCtx.getBluetoothManager().getSocket(_deviceUUID);

            if (socket != null) {
                if (btConn != null) {
                    btConn.unregister(this);
                    btConn.disconnect();
                }

                btConn = new VNSerialBluetoothConnection(socket, vnDataReaderListner);
                receivingData = true;
                btConn.register(this);
                btConn.start();
            } else {
                return InsDeviceStatus.DeviceNotFound;
            }
        } catch (Exception e) {
            TtAppCtx.getReport().writeError(e.getMessage(), "VNInsService:startInsSerial");
            return InsDeviceStatus.InsError;
        }

        return InsDeviceStatus.InsStarted;
    }
    private InsDeviceStatus stopInsSerial() {
        if (btConn != null) {
            btConn.unregister(this);
            btConn.disconnect();
            btConn = null;

            postInsStop();
            return InsDeviceStatus.InsStopped;
        }

        return InsDeviceStatus.InsAlreadyStopped;
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
    public void onCommandResponseReceived(VNCommand command) {
        postCommandResponse(command);
    }

    @Override
    public void onInvalidDataRecieved(byte[] data) {
        //
    }
    //endregion

    //region Bluetooth
    @Override
    public void connectionStarted() {
        postInsStart();
        if (TtAppCtx.hasReport()) {
            TtAppCtx.getReport().writeEvent("INS connection started");
        }
    }

    @Override
    public void connectionLost() {
        if (btConn != null) {
            btConn.unregister(this);
            btConn = null;
        }

        if (TtAppCtx.hasReport()) {
            TtAppCtx.getReport().writeError("INS lost connection", "InsService:connectionLost");
        }
        postError(InsError.LostDeviceConnection);
    }

    @Override
    public void connectionEnded() {
        //
    }

    @Override
    public void failedToConnect() {
        postError(InsError.FailedToConnect);
    }
    //endregion

    //region Post Events
    private void postInsData(final VNInsData insData) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.insDataReceived(insData));
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

    private void postCommandResponse(final VNCommand command) {
        for (final VNInsService.Listener listener : listeners) {
            try {
                new Handler(Looper.getMainLooper()).post(() -> listener.commandRespone(command));
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

    //region Commands
    public void sendData(byte[] data) throws IOException {
        if (isInsRunning()) {
            if (getInsMode() == InsMode.Serial) {
                btConn.sendData(data);
            } else {
                throw new NotImplementedError("Send Command via GATT");
            }
        } else {
            throw new RuntimeException("VN is not running");
        }
    }

    public void sendCommand(VNCommand command) throws IOException {
        sendData(command.toBytes());
    }

    public void tare() throws IOException {
        sendData(TARE);
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
        InsError,
        InsRequiresPermissions,
        InsAlreadyStarted,
        InsAlreadyStopped,
        DeviceNotFound,
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
            return VNInsService.this.startIns();
        }

        @Override
        public InsDeviceStatus stopIns() {
            return VNInsService.this.stopIns();
        }

        @Override
        public void stopService() {
            VNInsService.this.stopIns();
            VNInsService.this.stopLogging();
            VNInsService.this.stopSelf();

            postServiceStop();
        }

        @Override
        public void setDevice(String deviceUUID) throws Exception {
            VNInsService.this.setDevice(deviceUUID);
        }

        @Override
        public void setInsMode(InsMode mode) {
            VNInsService.this.setInsMode(mode);
        }

        @Override
        public InsMode getInsMode() {
            return VNInsService.this.getInsMode();
        }

        @Override
        public boolean isInsRunning() {
            return VNInsService.this.isInsRunning();
        }

        @Override
        public void startLogging(File logFile) {
            VNInsService.this.startLogging(logFile);
        }

        @Override
        public void stopLogging() {
            VNInsService.this.stopLogging();
        }

        @Override
        public boolean isLogging() {
            return VNInsService.this.isLogging();
        }

        @Override
        public void sendCommand(VNCommand command) throws IOException {
            VNInsService.this.sendCommand(command);
        }

        @Override
        public void tare() throws IOException {
            VNInsService.this.tare();
        }
    }


    public interface Listener {
        void insDataReceived(VNInsData data);
        void nmeaStringReceived(String nmeaString);
        void nmeaSentenceReceived(VNNmeaSentence nmeaSentence);
        void commandRespone(VNCommand command);
        void receivingData(boolean receiving);
        void receivingValidData(boolean valid);



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

        void setInsMode(InsMode mode);

        void setDevice(String deviceUUID) throws Exception;

        InsMode getInsMode();

        boolean isInsRunning();

        void startLogging(File logFile);

        void stopLogging();

        boolean isLogging();

        void sendCommand(VNCommand command) throws IOException;
        void tare() throws IOException;
    }
}
