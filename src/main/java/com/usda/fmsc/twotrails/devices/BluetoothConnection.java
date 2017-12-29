package com.usda.fmsc.twotrails.devices;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.usda.fmsc.utilities.StringEx;

public class BluetoothConnection extends Thread {
    private BluetoothSocket btSocket;
    private BufferedReader bufferedReader;

    private boolean running, disconnect;

    private ArrayList<Listener> listeners;

    public BluetoothConnection(BluetoothSocket socket) throws IOException {
        listeners = new ArrayList<>();

        btSocket = socket;

        bufferedReader = new BufferedReader(new InputStreamReader(btSocket.getInputStream()));
    }


    public void run() {
        running = true;

        if (!btSocket.isConnected()) {
            try {
                btSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (btSocket.isConnected()) {
            try {
                String str = StringEx.Empty;

                while (!disconnect) {
                    try {
                        str += bufferedReader.readLine();

                        if (!StringEx.isEmpty(str) && str.contains("*") && !disconnect) {
                            for (Listener l : listeners) {
                                l.receivedString(str);
                            }

                            str = StringEx.Empty;
                        }
                      } catch (Exception e) {
                        if (!disconnect) {
                            for (Listener l : listeners) {
                                l.connectionLost();
                            }

                            btSocket.connect();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }

            for (Listener l : listeners) {
                l.connectionEnded();
            }
        } else {

            for (Listener l : listeners) {
                l.failedToConnect();
            }
        }
    }

    public void disconnect() {
        disconnect = true;
        running = false;

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


    public void register(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }


    public interface Listener {
        void receivedString(String data);
        void connectionLost();
        void connectionEnded();
        void failedToConnect();
    }
}
