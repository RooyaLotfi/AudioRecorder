package com.example.realtimechewingdetection;

import java.util.Arrays;

import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseSensorListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.os.Environment;

public class SensorListener implements ESenseSensorListener {

    FileWriter writer;

    public void startSaving(){
        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, "mydata.csv");
        try {
            writer = new FileWriter(gpxfile);
            writeCsvHeader("Time","Acc","Gyr");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void onSensorChanged(ESenseEvent eSenseEvent) {

        long time = System.nanoTime();
        //System.out.println("Accel: " + Arrays.toString(eSenseEvent.getAccel()));
        //System.out.println("Gyro: " + Arrays.toString(eSenseEvent.getGyro()));
        try {
            writeCsvData(time, Arrays.toString(eSenseEvent.getAccel()), Arrays.toString(eSenseEvent.getGyro()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCsvHeader(String h1, String h2, String h3) throws IOException {
        String line = String.format("%s,%s,%s\n", h1, h2, h3);
        writer.write(line);

    }

    private void writeCsvData(long d, String s1, String s2) throws IOException {
        String line = String.format("%d %s,%s\n", d, s1, s2);
        writer.write(line);
    }

    public void disconnect(){
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}