// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class VideoActivity extends FullscreenActivity
    implements NativeInterface.NativeInterfaceListener, SurfaceHolder.Callback
{
    private static final String TAG = "VideoActivity";

    private VideoDecoder mVideoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        NativeInterface.setNativeInterfaceListener(this);
        NativeInterface.nativeInitialize(filePath);
        mVideoDecoder = new VideoDecoder();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        fillAspectRatio(surfaceView, NativeInterface.getVideoWidth(),
                        NativeInterface.getVideoHeight());
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void onInitialized()
    {
        Log.d(TAG, "initialized");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (mVideoDecoder != null)
        {
            if (mVideoDecoder.init(
                holder.getSurface(),
                NativeInterface.getSPS(),
                NativeInterface.getPPS()))
            {
                // A 500 ms warm-up time prevents frame drops at the start of playback
                long startTime = System.currentTimeMillis() + 500;
                mVideoDecoder.setStartTime(startTime);
                mVideoDecoder.start();
            }
            else
            {
                mVideoDecoder = null;
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (mVideoDecoder != null)
        {
            mVideoDecoder.close();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        NativeInterface.nativeFinalize();
    }
}
