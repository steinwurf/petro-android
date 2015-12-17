package com.steinwurf.petro;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class BothExtractorActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "BothExtractorActivity";
    private static final String MP4_FILE = Environment.getExternalStorageDirectory() + "/bunny.mp4";

    private BothExtractorDecoder mBothDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);
        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

        NativeInterface.nativeInitialize(MP4_FILE);
        mBothDecoder = new BothExtractorDecoder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mBothDecoder != null) {
            if (mBothDecoder.init(holder.getSurface(), MP4_FILE)) {
                Log.d(TAG, "initialized");
                mBothDecoder.start();
                Log.d(TAG, "started");
            } else {
                Log.d(TAG, "initialization failed");
                mBothDecoder = null;
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mBothDecoder != null) {
            mBothDecoder.close();
        }
    }
}
