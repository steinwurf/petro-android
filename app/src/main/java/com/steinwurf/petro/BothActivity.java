package com.steinwurf.petro;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class BothActivity extends FullscreenActivity
    implements NativeInterface.NativeInterfaceListener, SurfaceHolder.Callback
{
    private static final String TAG = "BothActivity";

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;

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
}
