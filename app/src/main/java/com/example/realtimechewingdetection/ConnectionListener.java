package com.example.realtimechewingdetection;

import io.esense.esenselib.ESenseConnectionListener;

import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseManager;

public class ConnectionListener implements ESenseConnectionListener {
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

        SensorListener sensor_listener = new SensorListener();
        eSenseManager.registerSensorListener(sensor_listener, 100);
        eSenseManager.setAdvertisementAndConnectiontInterval(100, 200, 20, 40);
    }

    @Override
    public void onDisconnected(ESenseManager eSenseManager) {
        System.out.println("***********DISCONNECTED");
    }
}
