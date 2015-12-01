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

public class MainActivity extends AppCompatActivity implements NativeInterface.NativeInterfaceListener, SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    private static final String MP4_FILE = Environment.getExternalStorageDirectory() + "/bunny.mp4";

    private VideoDecoder mVideoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        verifyStoragePermissions(this);
        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

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
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            Log.d(TAG, br.readLine());
            br.close();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
            //You'll need to add proper error handling here
        }

        NativeInterface.nativeInitialize(MP4_FILE);
        mVideoDecoder = new VideoDecoder();
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mVideoDecoder != null) {
            if (mVideoDecoder.init(
                    holder.getSurface(),
                    NativeInterface.getSPS(),
                    NativeInterface.getPPS()))
            {
                mVideoDecoder.start();
            } else {
                mVideoDecoder = null;
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mVideoDecoder != null) {
            mVideoDecoder.close();
        }
    }
}
