// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;

public class BothActivity extends FullscreenActivity
    implements NativeInterface.NativeInterfaceListener, SurfaceHolder.Callback
{
    private static final String TAG = "BothActivity";

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;

    private DebugOverlay mDebugOverlay = null;

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
        mAudioDecoder = new AudioDecoder();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        fillAspectRatio(surfaceView, NativeInterface.getVideoWidth(),
            NativeInterface.getVideoHeight());
        surfaceView.getHolder().addCallback(this);

        setupDebugOverlay();
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
        if (mVideoDecoder != null && mAudioDecoder != null)
        {
            if (mVideoDecoder.init(
                    holder.getSurface(),
                    NativeInterface.getSPS(),
                    NativeInterface.getPPS()) &&
                mAudioDecoder.init(
                    NativeInterface.getAudioCodecProfileLevel(),
                    NativeInterface.getAudioSampleRate(),
                    NativeInterface.getAudioChannelCount()))
            {
                mVideoDecoder.start();
                mAudioDecoder.start();
            }
            else
            {
                mVideoDecoder = null;
                mAudioDecoder = null;
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
        if (mAudioDecoder != null)
        {
            mAudioDecoder.close();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        NativeInterface.nativeFinalize();
    }

    private void setupDebugOverlay()
    {
        FrameLayout Frame = (FrameLayout) findViewById(R.id.frame);
        final TextView textView = new TextView(this);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(15);
        textView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        textView.setBackgroundColor(0x88000000);
        textView.setPadding(10, 10, 10, 10);
        Frame.addView(
            textView,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START));
        mDebugOverlay = new DebugOverlay();

        mDebugOverlay.setDebugInfoHandler(
            new DebugOverlay.DebugInfoHandler()
            {
                @Override
                public void handle(final String debugInfo)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            textView.setText(debugInfo);
                        }
                    });
                }
            }
        );

        mDebugOverlay.addDebugOverlayLineConstructor(
            new DebugOverlay.DebugOverlayLineConstructor()
            {
                @Override
                public String constructLine()
                {
                    if (mVideoDecoder != null)
                    {
                        return "PlayTime  : " + mVideoDecoder.lastPlayTime() + "ms";
                    }

                    return "";
                }
            }
        );

        mDebugOverlay.addDebugOverlayLineConstructor(
            new DebugOverlay.DebugOverlayLineConstructor()
            {
                @Override
                public String constructLine()
                {
                    long audio = 0;
                    long video = 0;

                    if (mAudioDecoder != null)
                    {
                        audio = mAudioDecoder.frameDrops();
                    }

                    if (mVideoDecoder != null)
                    {
                        video = mVideoDecoder.frameDrops();
                    }

                    return "FrameDrops: " + String.format("%d/%d A/V", audio, video);
                }
            }
        );

        mDebugOverlay.addDebugOverlayLineConstructor(
            new DebugOverlay.DebugOverlayLineConstructor()
            {
                @Override
                public String constructLine()
                {
                    if (mAudioDecoder != null)
                    {
                        return "AudioSleep: " + mAudioDecoder.lastSleepTime() + " ms";
                    }

                    return "";
                }
            }
        );

        mDebugOverlay.addDebugOverlayLineConstructor(
            new DebugOverlay.DebugOverlayLineConstructor()
            {
                @Override
                public String constructLine()
                {
                    if (mVideoDecoder != null)
                    {
                        return "VideoSleep: " + mVideoDecoder.lastSleepTime() + " ms";
                    }
                    return "";
                }
            }
        );


        mDebugOverlay.start();
    }
}
