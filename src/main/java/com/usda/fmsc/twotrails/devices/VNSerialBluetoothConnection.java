package com.usda.fmsc.twotrails.devices;

import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;

import com.usda.fmsc.geospatial.ins.vectornav.VNDataReader;
import com.usda.fmsc.geospatial.ins.vectornav.binary.BinaryMsgConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class VNSerialBluetoothConnection extends Thread {
    private BluetoothSocket btSocket;

    private boolean running, disconnect, receiving;

    private final ArrayList<Listener> listeners;

    private final VNDataReader.Listener vnDataReaderListener;

    public VNSerialBluetoothConnection(BluetoothSocket socket, VNDataReader.Listener listener) {
        listeners = new ArrayList<>();

        btSocket = socket;
        vnDataReaderListener = listener;
    }


    public void run() {
        try {
            running = true;

            if (!btSocket.isConnected()) {
                btSocket.connect();
            }

            if (btSocket.isConnected()) {
                for (Listener l : listeners) {
                    l.connectionStarted();
                }

                VNDataReader vnDataReader = new VNDataReader(btSocket.getInputStream(), vnDataReaderListener);

                while (!disconnect) {
                    try {

                        vnDataReader.readBytes();

                        receiving = true;
                    } catch (Exception e) {
                        receiving = false;

                        if (!disconnect) {
                            for (Listener l : listeners) {
                                l.connectionLost();
                            }

                            btSocket.connect();
                        }
                        break;
                    }
                }

                receiving = false;

                for (Listener l : listeners) {
                    l.connectionEnded();
                }
            } else {
                for (Listener l : listeners) {
                    l.failedToConnect();
                }
            }
        } catch (IOException ioex) {
            receiving = false;

            for (Listener l : listeners) {
                l.failedToConnect();
            }
        }

        running = false;
    }

    public void disconnect() {
        disconnect = true;
        running = false;
        receiving = false;

        if (btSocket != null) {
            try {
                btSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                btSocket = null;
            }
        }
    }


    public boolean isConnected() {
        return running;
    }

    public boolean isReceiving() {
        return receiving;
    }


    public void register(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        listeners.remove(listener);
    }


    public interface Listener {
        void connectionStarted();
        void connectionLost();
        void connectionEnded();
        void failedToConnect();
    }
}
