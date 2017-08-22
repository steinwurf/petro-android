// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import com.steinwurf.mediaextractor.AACSampleExtractor;
import com.steinwurf.mediaextractor.Extractor;
import com.steinwurf.mediaextractor.NALUExtractor;
import com.steinwurf.mediaextractor.SequenceParameterSet;
import com.steinwurf.mediaplayer.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BothActivity extends Activity implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "BothActivity";


    private AudioDecoder mAudioDecoder;
    private SampleStorage mAudioSampleStorage;
    private AACSampleExtractor mAACSampleExtractor;
    Thread mAudioExtractorThread;

    private VideoDecoder mVideoDecoder;
    private VideoDecoder.H264SampleStorage mVideoSampleStorage;
    private NALUExtractor mNALUExtractor;
    private Thread mVideoExtractorThread;
    private Surface mSurface;

    private boolean mRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mNALUExtractor = new NALUExtractor();
        mNALUExtractor.setFilePath(filePath);

        mAACSampleExtractor = new AACSampleExtractor();
        mAACSampleExtractor.setFilePath(filePath);

        try {
            mNALUExtractor.open();
            mAACSampleExtractor.open();
        } catch (Extractor.UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }

        mRunning = true;

        mAudioSampleStorage = new SampleStorage(0);
        mAudioExtractorThread = new Thread(){
            public void run(){
                while (mRunning && !mAACSampleExtractor.atEnd())
                {
                    mAudioSampleStorage.addSample(
                            mAACSampleExtractor.getDecodingTimestamp(),
                            mAACSampleExtractor.getSample());
                    mAACSampleExtractor.advance();
                }
            }
        };
        mAudioExtractorThread.start();

        mVideoSampleStorage = new VideoDecoder.H264SampleStorage(0);
        mVideoExtractorThread = new Thread(){
            public void run(){
                ByteArrayOutputStream sample = new ByteArrayOutputStream();
                while (mRunning && !mNALUExtractor.atEnd())
                {
                    long timestamp = mNALUExtractor.getPresentationTimestamp();
                    try {
                        sample.write(VideoDecoder.NALU_HEADER);
                        sample.write(mNALUExtractor.getSample());
                    } catch (IOException e) {
                        e.printStackTrace();
                        sample.reset();
                        continue;
                    }
                    mNALUExtractor.advance();
                    if (mNALUExtractor.isBeginningOfAVCSample())
                    {
                        mVideoSampleStorage.addSample(timestamp, sample.toByteArray());
                        sample.reset();
                    }
                }
            }
        };
        mVideoExtractorThread.start();

        mAudioDecoder = AudioDecoder.build(
                mAACSampleExtractor.getMPEGAudioObjectType(),
                mAACSampleExtractor.getFrequencyIndex(),
                mAACSampleExtractor.getChannelConfiguration(),
                mAudioSampleStorage);

        ByteArrayOutputStream spsBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream ppsBuffer = new ByteArrayOutputStream();

        byte[] sps = mNALUExtractor.getSPS();
        byte[] pps = mNALUExtractor.getPPS();

        SequenceParameterSet sequenceParameterSet = SequenceParameterSet.parse(sps);
        if (sequenceParameterSet == null)
        {
            finish();
            return;
        }

        try {
            spsBuffer.write(VideoDecoder.NALU_HEADER);
            spsBuffer.write(sps);
            ppsBuffer.write(VideoDecoder.NALU_HEADER);
            ppsBuffer.write(pps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mVideoDecoder = VideoDecoder.build(
                sequenceParameterSet.getVideoWidth(),
                sequenceParameterSet.getVideoHeight(),
                spsBuffer.toByteArray(),
                ppsBuffer.toByteArray(),
                mVideoSampleStorage);

        if (mVideoDecoder == null)
        {
            finish();
            return;
        }

        TextureView textureView = findViewById(R.id.textureView);

        Point displayMetrics  = com.steinwurf.mediaplayer.Utils.getRealMetrics(this);
        textureView.setTransform(
                com.steinwurf.mediaplayer.Utils.fitScaleMatrix(
                        sequenceParameterSet.getVideoWidth(),
                        sequenceParameterSet.getVideoHeight(),
                        displayMetrics.x,
                        displayMetrics.y));
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioExtractorThread != null && mVideoExtractorThread != null)
        {
            mRunning = false;
            try {
                mAudioExtractorThread.join();
                mVideoExtractorThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (mAACSampleExtractor != null)
            mAACSampleExtractor.close();
        if (mNALUExtractor != null)
            mNALUExtractor.close();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mAudioDecoder.start();

        mSurface = new Surface(surface);
        mVideoDecoder.setSurface(mSurface);
        mVideoDecoder.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mAudioDecoder.stop();
        mVideoDecoder.stop();
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
