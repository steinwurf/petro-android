package com.steinwurf.petro;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class VideoExtractorActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "VideoExtractorActivity";
    private static final String MP4_FILE = Environment.getExternalStorageDirectory() + "/bunny.mp4";

    private VideoExtractorDecoder mVideoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);
        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

        NativeInterface.nativeInitialize(MP4_FILE);
        mVideoDecoder = new VideoExtractorDecoder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mVideoDecoder != null) {
            if (mVideoDecoder.init(
                    holder.getSurface(), MP4_FILE))
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
