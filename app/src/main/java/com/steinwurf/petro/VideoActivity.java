// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.steinwurf.mediaextractor.NALUSampleExtractor;
import com.steinwurf.mediaextractor.Extractor;
import com.steinwurf.mediaplayer.SampleStorage;
import com.steinwurf.mediaplayer.VideoDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class VideoActivity extends FullscreenActivity implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "VideoActivity";

    private VideoDecoder mVideoDecoder;

    private NALUSampleExtractor mNALUSampleExtractor;
    private SampleStorage mSampleStorage;
    private Thread mExtractorThread;
    private boolean mRunning = false;
    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        TextureView textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mNALUSampleExtractor = new NALUSampleExtractor();
        mNALUSampleExtractor.setFilePath(filePath);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            mNALUSampleExtractor.open();
        } catch (Extractor.UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }

        int width = 1280;
        int height = 720;
        mVideoDecoder = VideoDecoder.build(
                width, height, mNALUSampleExtractor.getSPSData(), mNALUSampleExtractor.getPPSData());
        if (mVideoDecoder == null)
        {
            finish();
            return;
        }

        if (mSurface != null)
        {
            mVideoDecoder.setSurface(mSurface);
            mVideoDecoder.start();
        }

        mSampleStorage = new SampleStorage(0);
        mVideoDecoder.setSampleStorage(mSampleStorage);

        mRunning = true;
        mExtractorThread = new Thread(){
            public void run(){
                //byte[] header = {0x00, 0x00, 0x00, 0x01};
                ByteArrayOutputStream sample = new ByteArrayOutputStream();
                while (mRunning && !mNALUSampleExtractor.atEnd())
                {
                    try {
                        //sample.write(header);
                        sample.write(mNALUSampleExtractor.getSample());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    long timestamp = mNALUSampleExtractor.getPresentationTimestamp();
                    mNALUSampleExtractor.advance();
                    //if (mNALUSampleExtractor.isBeginningOfAVCSample())
                    {
                        mSampleStorage.addSample(timestamp, sample.toByteArray());
                        sample.reset();
                    }
                }
            }
        };

        mExtractorThread.start();
    }

    @Override
    protected void onStop() {
        try {
            mRunning = false;
            mExtractorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mNALUSampleExtractor.close();
        super.onStop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurface = new Surface(surface);
        if (mVideoDecoder != null)
        {
            mVideoDecoder.setSurface(mSurface);
            mVideoDecoder.start();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mSurface != null)
        {
            mSurface.release();
            mSurface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
