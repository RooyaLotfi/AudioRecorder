package com.example.realtimechewingdetection;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseSensorListener;

import com.google.firebase.database.*;
import com.google.firebase.database.FirebaseDatabase;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import io.esense.esenselib.ESenseEvent;

public class SensorListener implements ESenseSensorListener {

    DatabaseReference dbref = FirebaseDatabase.getInstance().getReference();
    private static ESenseConfig config = new ESenseConfig(ESenseConfig.AccRange.G_2, ESenseConfig.GyroRange.DEG_250, ESenseConfig.AccLPF.BW_5, ESenseConfig.GyroLPF.BW_5);

    @Override
    public void onSensorChanged(ESenseEvent eSenseEvent) {
        System.out.println("Accel: " + Arrays.toString(eSenseEvent.convertAccToG(config)));
        System.out.println("Gyro: " + Arrays.toString(eSenseEvent.convertGyroToDegPerSecond(config)));
        //writeNewDataFirebase(Arrays.toString(eSenseEvent.convertAccToG(config)), Arrays.toString(eSenseEvent.convertGyroToDegPerSecond(config)));

        if (SensorDataRecorder.isRecording()) {
            long time = System.currentTimeMillis();

            //System.out.println("Accel: " + Arrays.toString(eSenseEvent.getAccel()));
            //System.out.println("Gyro: " + Arrays.toString(eSenseEvent.getGyro()));

            double[] accel = eSenseEvent.convertAccToG(config);
            double[] gyro = eSenseEvent.convertGyroToDegPerSecond(config);

            String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());

            SensorData sensorData = new SensorData(dateTime, time, accel[0], accel[1], accel[2], gyro[0], gyro[1], gyro[2]);
            //try {
                SensorDataRecorder.writeSensorData(sensorData);
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
        }
    }

    //private void writeNewData(String acc, String gyr) {
    //  Data data = new Data(acc, gyr);
    //  dbref.child("Sensor Data").push().setValue(data);
    // }

    private void writeNewDataFirebase(String acc, String gyr) {
        long time = System.nanoTime();
        DatabaseReference nodeReference = dbref.child("Sensor Data");
        nodeReference.child(String.valueOf(time)).child("_Acc").setValue(acc);
        nodeReference.child(String.valueOf(time)).child("_Gyr").setValue(gyr);
        nodeReference.child(String.valueOf(time)).child("_moduletime").setValue(time);
    }
}
