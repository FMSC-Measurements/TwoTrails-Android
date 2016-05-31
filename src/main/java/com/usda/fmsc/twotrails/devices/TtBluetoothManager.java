package com.usda.fmsc.twotrails.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.usda.fmsc.twotrails.gps.GpsBluetoothConnection;
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


    private boolean gpsConnected, connecting;
    public void checkGPS(String uuid, final BluetoothListener listener) {
        BluetoothDeviceError error = null;
        BluetoothSocket socket = null;
        gpsConnected = false;

        if (!connecting) {
            connecting = true;

            try {
                BluetoothDevice device = getBluetoothDevice(uuid);

                if (device != null) {
                    socket = getSocket(uuid);

                    if (socket != null) {

                        socket.connect();

                        final GpsBluetoothConnection conn = new GpsBluetoothConnection(socket);

                        final GpsBluetoothConnection.Listener btListener = new GpsBluetoothConnection.Listener() {
                            @Override
                            public void receivedString(String data) {
                                if (NmeaSentence.validateChecksum(data)) {
                                    gpsConnected = true;
                                    conn.disconnect();

                                    if (listener != null) {
                                        listener.receivedValidNmea();
                                    }
                                }
                            }

                            @Override
                            public void connectionLost() {
                                if (listener != null) {
                                    listener.error(BluetoothDeviceError.ConnectionLost);
                                }
                            }

                            @Override
                            public void connectionEnded() {
                                if (!gpsConnected && listener != null) {
                                    listener.error(BluetoothDeviceError.ConnectionClosed);
                                }
                            }
                        };

                        conn.register(btListener);

                        conn.start();
                    } else {
                        error = BluetoothDeviceError.BadSocket;
                    }
                } else {
                    error = BluetoothDeviceError.NoDevice;
                }
            } catch (IOException e) {
                error = BluetoothDeviceError.Unknown;

                if (socket.isConnected()) {
                    error = BluetoothDeviceError.BadSocket;
                } else {
                    switch (socket.getRemoteDevice().getBondState()) {
                        case BluetoothDevice.BOND_BONDED:
                            error = BluetoothDeviceError.Timeout;
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            error = BluetoothDeviceError.ConnectionClosed;
                            break;
                        case BluetoothDevice.BOND_NONE:
                            error = BluetoothDeviceError.NoDevice;
                            break;
                    }
                }
            } catch (Exception e) {
                error = BluetoothDeviceError.Unknown;
                TtUtils.TtReport.writeError(e.getMessage(), "TtBluetoothManager:checkGPS", e.getStackTrace());
            } finally {
                connecting = false;

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e){
                        //
                    }
                }
            }
        }

        if (listener != null && error != null) {
            listener.error(error);
        }
    }

    public boolean checkRangeFinder() {
        return false;
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
        Unknown
    }
}
