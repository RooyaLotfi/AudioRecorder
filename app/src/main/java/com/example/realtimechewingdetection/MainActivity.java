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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.esense.esenselib.*;

public class MainActivity extends AppCompatActivity {

    Button btnStartRecord, btnStopRecord, bluetoothButton;
    private ESenseManager manager;
    ConnectionListener connectionListener;
    SensorListener sensor_listener;
    String pathSaveAudio = "";
    String pathSave = "";
    final int REQUEST_PERMISSION_CODE = 1000;

    private static final String TAG = MainActivity.class.getCanonicalName();

    private static final int SAMPLING_RATE_IN_HZ = 48000;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {

        private BluetoothState bluetoothState = BluetoothState.UNAVAILABLE;

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1); // Checks if bluetooth is connected
            switch (state) {
                case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                    Log.i(TAG, "Bluetooth HFP Headset is connected");
                    handleBluetoothStateChange(BluetoothState.AVAILABLE);
                    break;
                case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                    Log.i(TAG, "Bluetooth HFP Headset is connecting");
                    handleBluetoothStateChange(BluetoothState.UNAVAILABLE);
                case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    Log.i(TAG, "Bluetooth HFP Headset is disconnected");
                    handleBluetoothStateChange(BluetoothState.UNAVAILABLE);
                    break;
                case AudioManager.SCO_AUDIO_STATE_ERROR:
                    Log.i(TAG, "Bluetooth HFP Headset is in error state");
                    handleBluetoothStateChange(BluetoothState.UNAVAILABLE);
                    break;
            }
        }

        private void handleBluetoothStateChange(BluetoothState state) {
            if (bluetoothState == state) {
                return;
            }

            bluetoothState = state;
            bluetoothStateChanged(state);
        }
    };

    private AudioRecord recorder = null;

    private AudioManager audioManager;

    private Thread recordingThread = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartRecord = findViewById(R.id.btnStartRecord);
        btnStopRecord = findViewById(R.id.btnStopRecord);


        btnStartRecord.setEnabled(true);
        btnStopRecord.setEnabled(false);

        if (checkPermissionFromDevices()) {
            btnStartRecord.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {

                    String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());

                    File folder = new File(Environment.getExternalStorageDirectory() +
                            File.separator + "eSenseData");

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        pathSave = folder.getAbsolutePath()+File.separator+ dateTime;
                        Log.i(TAG,pathSave);
                    } else {
                        // Do something else on failure
                    }

                    btnStartRecord.setEnabled(false);
                    btnStopRecord.setEnabled(true);

                    Toast.makeText(MainActivity.this, "Recording has begun...", Toast.LENGTH_SHORT).show();

                    SensorDataRecorder.createCSV( "eSenseData" + File.separator + dateTime + "-SensorData");
                    ConnectionListener connectionListener = new ConnectionListener();
                    manager = new ESenseManager("eSense-0883", MainActivity.this.getApplicationContext(), connectionListener);
                    manager.connect(25000); // timeout = scan timeout in milli seconds

                    startRecording();
                }
            });

            btnStopRecord.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {

                    if (manager.isConnected() && SensorDataRecorder.isRecording()) {
                        manager.unregisterSensorListener();
                        manager.disconnect();
                        SensorDataRecorder.disconnect();
                    }
                    btnStartRecord.setEnabled(true);
                    btnStopRecord.setEnabled(false);

                    stopRecording();

                }
            });

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            bluetoothButton = (Button) findViewById(R.id.btnBluetooth);
            bluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activateBluetoothSco();
                }
            });

        } else {
            requestPermission();
        }

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

    @Override
    protected void onResume() {
        super.onResume();

        bluetoothButton.setEnabled(calculateBluetoothButtonState());
        btnStartRecord.setEnabled(calculateStartRecordButtonState());
        btnStopRecord.setEnabled(calculateStopRecordButtonState());

        registerReceiver(bluetoothStateReceiver, new IntentFilter(
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRecording();
        unregisterReceiver(bluetoothStateReceiver);
    }

    private void startRecording() {
        // Depending on the device one might has to change the AudioSource, e.g. to DEFAULT
        // or VOICE_COMMUNICATION
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        recordingInProgress.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();

        bluetoothButton.setEnabled(calculateBluetoothButtonState());
        btnStartRecord.setEnabled(calculateStartRecordButtonState());
        btnStopRecord.setEnabled(calculateStopRecordButtonState());
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;

        bluetoothButton.setEnabled(calculateBluetoothButtonState());
        btnStartRecord.setEnabled(calculateStartRecordButtonState());
        btnStopRecord.setEnabled(calculateStopRecordButtonState());
    }

    private void activateBluetoothSco() {
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Log.e(TAG, "SCO ist not available, recording is not possible");
            return;
        }

        if (!audioManager.isBluetoothScoOn()) {
            audioManager.startBluetoothSco();
        }
    }

    private void bluetoothStateChanged(BluetoothState state) {
        Log.i(TAG, "Bluetooth state changed to:" + state);

        if (BluetoothState.UNAVAILABLE == state && recordingInProgress.get()) {
            stopRecording();
        }

        bluetoothButton.setEnabled(calculateBluetoothButtonState());
        btnStartRecord.setEnabled(calculateStartRecordButtonState());
        btnStopRecord.setEnabled(calculateStopRecordButtonState());
    }

    private boolean calculateBluetoothButtonState() {
        return !audioManager.isBluetoothScoOn();
    }

    private boolean calculateStartRecordButtonState() {
        return audioManager.isBluetoothScoOn() && !recordingInProgress.get();
    }

    private boolean calculateStopRecordButtonState() {
        return audioManager.isBluetoothScoOn() && recordingInProgress.get();
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {

            String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS").format(Calendar.getInstance().getTime());

            final File file = new File(Environment.getExternalStorageDirectory() + File.separator + "eSenseData", dateTime+"-audio.pcm");

            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE); // Writes the output in
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    enum BluetoothState {
        AVAILABLE, UNAVAILABLE
    }
}