package com.usda.fmsc.twotrails.devices;

import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class BluetoothConnection extends Thread {
    private BluetoothSocket btSocket;
    private BufferedReader bufferedReader;
    private InputStreamReader inputStreamReader;

    private boolean running, disconnect, receiving;

    private final ArrayList<Listener> listeners;

    public BluetoothConnection(BluetoothSocket socket) {
        listeners = new ArrayList<>();

        btSocket = socket;
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

                inputStreamReader = new InputStreamReader(btSocket.getInputStream());
                bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder str = new StringBuilder();

                while (!disconnect) {
                    try {
                        str.append(bufferedReader.readLine());

                        if (!TextUtils.isEmpty(str) && str.indexOf("*") != -1 && !disconnect) {
                            for (Listener l : listeners) {
                                l.receivedString(str.toString());
                            }
                            str.setLength(0);
                        }

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

                if (bufferedReader != null) {
                    bufferedReader.close();
                    bufferedReader = null;
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                    inputStreamReader = null;
                }

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
        void receivedString(String data);
        void connectionStarted();
        void connectionLost();
        void connectionEnded();
        void failedToConnect();
    }
}
