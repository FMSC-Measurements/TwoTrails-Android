package com.usda.fmsc.twotrails.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.usda.fmsc.geospatial.nmea.sentences.base.NmeaSentence;

public class TtBluetoothManager {
    private BluetoothAdapter adapter;

    public TtBluetoothManager() {
        adapter = BluetoothAdapter.getDefaultAdapter();
    }


    public boolean isAvailable() {
        return adapter != null;
    }

    public boolean isEnabled() {
        return adapter.isEnabled();
    }


    public BluetoothAdapter getAdapter() {
        return adapter;
    }


    public BluetoothDevice getBluetoothDevice(String uuid) {
        try {
            return adapter.getRemoteDevice(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public BluetoothSocket getSocket(String uuid) {
        BluetoothSocket socket = null;
        BluetoothDevice device = null;

        try {
            device = adapter.getRemoteDevice(uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (device != null) {
            try {
                Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                socket = (BluetoothSocket)m.invoke(device, 1);
            }
            catch (NoSuchMethodException ignore) {
                try {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return socket;
    }


    public interface BluetoothListener {
        void receivedData();
        void receivedValidNmea();
        void error(BluetoothDeviceError error);
    }

    public enum BluetoothDeviceError {
        Timeout,
        ConnectionLost,
        ConnectionClosed,
        NoDevice,
        BadSocket,
        FailedToConnect,
        Unknown
    }
}
