package com.example.realtimechewingdetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import io.esense.esenselib.*;

public class MainActivity extends AppCompatActivity {

    Button btnStartRecord, btnStopRecord, btnPlay, btnStop;
    private ESenseManager manager;
    ConnectionListener connectionListener;
    SensorListener sensor_listener;
    private static final String TAG = "MainActivity";
    String pathSaveAudio = "";
    String pathSave = "";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    AudioManager audioManager;
    final int REQUEST_PERMISSION_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartRecord = findViewById(R.id.btnStartRecord);
        btnStopRecord = findViewById(R.id.btnStopRecord);
        btnPlay = findViewById(R.id.btnPlay);
        btnStop = findViewById(R.id.btnStop);

        btnStartRecord.setEnabled(true);
        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
        btnStopRecord.setEnabled(false);

        if (checkPermissionFromDevices()) {
            btnStartRecord.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {

                    String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());

                    //Date currentTime = Calendar.getInstance().getTime();
                    //long timestamp = System.currentTimeMillis();

                    File folder = new File(Environment.getExternalStorageDirectory() +
                            File.separator + "eSenseData");

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        pathSave = folder.getAbsolutePath()+File.separator+ dateTime;
                        pathSaveAudio = folder.getAbsolutePath()+File.separator+ dateTime + "-Audio.wav";
                        Log.i(TAG,pathSave);
                    } else {
                        // Do something else on failure
                    }

                    //pathSave = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    //     + dateTime + "-Audio.wav";
                    setupMediaRecorder();
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    btnStartRecord.setEnabled(false);
                    btnPlay.setEnabled(false);
                    btnStop.setEnabled(false);
                    btnStopRecord.setEnabled(true);

                    Toast.makeText(MainActivity.this, "Recording has begun...", Toast.LENGTH_SHORT).show();

                    ConnectionListener connectionListener = new ConnectionListener();
                    manager = new ESenseManager("eSense-0883", MainActivity.this.getApplicationContext(), connectionListener);
                    manager.connect(5000); // timeout = scan timeout in milli seconds

                    SensorDataRecorder.startSaving( "eSenseData" + File.separator + dateTime + "-SensorData");
                }
            });

            btnStopRecord.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {

                    mediaRecorder.stop();
                    if (manager.isConnected() && SensorDataRecorder.isRecording()) {
                        manager.unregisterSensorListener();
                        SensorDataRecorder.disconnect();
                        manager.disconnect();
                    }
                    btnPlay.setEnabled(true);
                    btnStop.setEnabled(false);
                    btnStartRecord.setEnabled(true);
                    btnStopRecord.setEnabled(false);

                }
            });

            btnPlay.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    btnStop.setEnabled(true);
                    btnStopRecord.setEnabled(false);
                    btnStartRecord.setEnabled(false);

                    mediaPlayer = new MediaPlayer();

                    try {

                        mediaPlayer.setDataSource(pathSaveAudio);
                        mediaPlayer.prepare();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.start();
                    Toast.makeText(MainActivity.this, "Playing.......", Toast.LENGTH_SHORT).show();
                }
            });

            btnStop.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    btnStop.setEnabled(false);
                    btnStopRecord.setEnabled(false);
                    btnStartRecord.setEnabled(true);
                    btnPlay.setEnabled(true);

                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        setupMediaRecorder();
                    }
                }
            });

        } else {
            requestPermission();
        }

    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setAudioSamplingRate(48000);
        mediaRecorder.setOutputFile(pathSaveAudio);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,  int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }

    /*
     * To check if you have a permission, call the  ContextCompat.checkSelfPermission() method.
     * For example, this snippet shows how to check if the activity has permission to write to the calendar:
     * int permissionCheck = ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.WRITE_CALENDAR);
     *
     * This means that you check if your application has the permission to use dangerous permissions.
     * Where as activitycompat.requestPermission() is used to request user to give us permission to use dangerous permissions.
     * */
    private boolean checkPermissionFromDevices() {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return write_external_storage_result == PackageManager.PERMISSION_GRANTED &&
                record_audio_result == PackageManager.PERMISSION_GRANTED;
    }


    private BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            System.out.println("ANDROID Audio SCO state: " + state);
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {


                Log.i(TAG, "Connection is being Established");
                /*
                 * Now the connection has been established to the bluetooth device.
                 * Record audio or whatever (on another thread).With AudioRecord you can record with an object created like this:
                 * new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                 * AudioFormat.ENCODING_PCM_16BIT, audioBufferSize);
                 *
                 * After finishing, don't forget to unregister this receiver and
                 * to stop the bluetooth connection with am.stopBluetoothSco();
                 */
                setupMediaRecorder();
                //unregisterReceiver(this);
            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                Log.i(TAG, "SCO_AUDIO_STATE_DISCONNECTED The connection has not been established");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(mBluetoothScoReceiver, intentFilter);
        audioManager = (AudioManager) getApplicationContext().getSystemService(getApplicationContext().AUDIO_SERVICE);
        // Start Bluetooth SCO.
        audioManager.setMode(audioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
        // Stop Speaker.
        audioManager.setSpeakerphoneOn(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothScoReceiver);
        // Stop Bluetooth SCO.
        audioManager.stopBluetoothSco();
        audioManager.setMode(audioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(false);
        // Start Speaker.
        audioManager.setSpeakerphoneOn(true);
    }
}