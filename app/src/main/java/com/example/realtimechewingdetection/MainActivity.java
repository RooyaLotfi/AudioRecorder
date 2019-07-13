package com.example.realtimechewingdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.esense.esenselib.*;

public class MainActivity extends AppCompatActivity {

    ESenseManager manager;
    FileWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectionListener connectionListener = new ConnectionListener();
        manager = new ESenseManager("eSense-0636", MainActivity.this.getApplicationContext(), connectionListener);
        boolean connect_device = manager.connect(5000);


    }
}