package com.example.realtimechewingdetection;

public class SensorData {
    public final long timestamp;
    public final double accX;
    public final double accY;
    public final double accZ;
    public final double gyroX;
    public final double gyroY;
    public final double gyroZ;

    public SensorData(long timestamp, double accX, double accY, double accZ, double gyroX, double gyroY, double gyroZ) {
        this.timestamp = timestamp;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
    }
}
