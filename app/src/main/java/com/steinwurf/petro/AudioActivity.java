package com.steinwurf.petro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AudioActivity extends AppCompatActivity implements NativeInterface.NativeInterfaceListener {

    private static final String TAG = "AudioActivity";
    private static final String MP4_FILE = Environment.getExternalStorageDirectory() + "/bunny.mp4";

    private AudioDecoder mAudioDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NativeInterface.setNativeInterfaceListener(this);
        Log.d(TAG, MP4_FILE);
        File file = new File(MP4_FILE);
        if(file.exists()) {
            Log.d(TAG, "file exists");
        }
        else
        {
            Log.d(TAG, "file does not exists");
        }

        mAudioDecoder = new AudioDecoder();
        NativeInterface.nativeInitialize(MP4_FILE);
    }

    @Override
    public void onInitialized() {
        Log.d(TAG, "initialized");
        if (mAudioDecoder != null) {
            if (mAudioDecoder.init(
                    NativeInterface.getAudioCodecProfileLevel(),
                    NativeInterface.getAudioSampleRate(),
                    NativeInterface.getAudioChannelCount()))
            {
                mAudioDecoder.start();
            } else {
                mAudioDecoder = null;
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAudioDecoder != null) {
            mAudioDecoder.close();
        }
        NativeInterface.nativeFinalize();
    }
}
