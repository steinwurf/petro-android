package com.steinwurf.petro;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NativeInterface.NativeInterfaceListener {

    private static final String TAG = "MainActivity";
    private static final String MP4_FILE = "/sdcard/bunny.mp4";

    private ArrayList<ByteBuffer> mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mData = new ArrayList<>();

        NativeInterface.setNativeInterfaceListener(this);
        NativeInterface.nativeInitialize(MP4_FILE);
        for (int i = 0; i < 100; i++)
        {
            mData.add(ByteBuffer.wrap(NativeInterface.getSample(i)));
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void onInitialized() {
        Log.d(TAG, "initialized");
        NativeInterface.nativeFinalize();
    }
}
