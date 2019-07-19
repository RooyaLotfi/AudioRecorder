package com.example.realtimechewingdetection;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SensorDataRecorder {
    private static FileWriter writer;
    private static boolean isRecording = false;
    private static String dateTime;

    public static void startSaving(String fileName){
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

    public static void writeCsvData(SensorData sensorData) throws IOException {
        dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());
        String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",dateTime, sensorData.timestamp, sensorData.accX, sensorData.accY, sensorData.accZ, sensorData.gyroX, sensorData.gyroY, sensorData.gyroZ);
        writer.write(line);
    }

    public static void disconnect(){
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
    }

    public static boolean isRecording() {
        return isRecording;
    }
}
