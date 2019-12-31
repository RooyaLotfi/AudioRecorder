package com.example.realtimechewingdetection;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SensorDataRecorder {
    private static FileWriter writer;
    private static boolean isRecording = false;
    private static String dateTime;
    private static List<SensorData> dataPoints = new ArrayList<>();

    public static void createCSV(String fileName){
        isRecording =  true;
        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, fileName + ".csv");
        try {
            writer = new FileWriter(gpxfile);
            writeCsvHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static  void writeCsvHeader() throws IOException {
        String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n", "Time", "Timestamp", "Acc X","Acc Y", "Acc Z", "Gyro X", "Gyro Y", "Gyro Z");
        writer.write(line);
    }

    public static void writeSensorData(SensorData sensorData){
        dataPoints.add(sensorData);
    }

    public static void writeCsvData() throws IOException {
        for (SensorData datapoint : dataPoints) {
            //dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());
            String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",datapoint.dateTime, datapoint.timestamp, datapoint.accX, datapoint.accY, datapoint.accZ, datapoint.gyroX, datapoint.gyroY, datapoint.gyroZ);
            writer.write(line);
        }
    }

    public static void disconnect(){
        try {
            writeCsvData();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        writer = null;
        isRecording = false;
        dataPoints = new ArrayList<>();
    }

    public static boolean isRecording() {
        return isRecording;
    }
}
