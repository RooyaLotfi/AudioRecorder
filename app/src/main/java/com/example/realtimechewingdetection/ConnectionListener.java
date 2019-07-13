package com.example.realtimechewingdetection;

import io.esense.esenselib.ESenseConnectionListener;

import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseManager;

public class ConnectionListener implements ESenseConnectionListener {

    SensorListener sensor_listener;

    @Override
    public void onDeviceFound(ESenseManager eSenseManager) {
        System.out.println("***********DeviceFound");
    }

    @Override
    public void onDeviceNotFound(ESenseManager eSenseManager) {
        System.out.println("***********NOT FOUND");
    }

    @Override
    public void onConnected(ESenseManager eSenseManager) {
        System.out.println("***********CONNECTEDDDDD");

        sensor_listener = new SensorListener();
        sensor_listener.startSaving();
        eSenseManager.registerSensorListener(sensor_listener, 100);

    }

    @Override
    public void onDisconnected(ESenseManager eSenseManager) {
        System.out.println("***********DISCONNECTED");
        sensor_listener.disconnect();
    }
}
