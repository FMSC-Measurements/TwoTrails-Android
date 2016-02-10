package com.usda.fmsc.twotrails.gps;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.usda.fmsc.utilities.StringEx;

//https://android.googlesource.com/platform/development/+/25b6aed7b2e01ce7bdc0dfa1a79eaf009ad178fe/samples/BluetoothChat/src/com/example/android/BluetoothChat

public class GpsBluetoothConnection extends Thread {
    private BluetoothSocket btSocket;
    private BufferedReader bufferedReader;

    private boolean running, disconnect;

    private ArrayList<Listener> listeners;

    public GpsBluetoothConnection(BluetoothSocket socket) {
        listeners = new ArrayList<>();

        btSocket = socket;

        try {
            InputStream btInStream = btSocket.getInputStream();

            bufferedReader = new BufferedReader(new InputStreamReader(btInStream));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    }
}
