package com.steinwurf.petro;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class VideoActivity extends AppCompatActivity implements NativeInterface.NativeInterfaceListener, SurfaceHolder.Callback {

    private static final String TAG = "VideoActivity";
    private static final String MP4_FILE = Environment.getExternalStorageDirectory() + "/bunny.mp4";

    private VideoDecoder mVideoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);
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

        NativeInterface.nativeInitialize(MP4_FILE);
        mVideoDecoder = new VideoDecoder();
    }

    @Override
    public void onInitialized() {
        Log.d(TAG, "initialized");
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
