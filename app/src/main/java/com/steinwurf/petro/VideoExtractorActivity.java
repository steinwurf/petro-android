// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class VideoExtractorActivity extends FullscreenActivity implements SurfaceHolder.Callback
{
    private static final String TAG = "VideoExtractorActivity";
    private VideoExtractorDecoder mVideoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mVideoDecoder = new VideoExtractorDecoder(filePath);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        fillAspectRatio(surfaceView, mVideoDecoder.getVideoWidth(),
                        mVideoDecoder.getVideoHeight());
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(TAG, "surfaceChanged");
        if (mVideoDecoder != null)
        {
            if (mVideoDecoder.init(holder.getSurface()))
            {
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
}
