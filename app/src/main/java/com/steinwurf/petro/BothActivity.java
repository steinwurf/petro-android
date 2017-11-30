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

import com.steinwurf.mediaplayer.AudioDecoder;
import com.steinwurf.mediaplayer.Utils;
import com.steinwurf.mediaplayer.VideoDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BothActivity extends Activity implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "BothActivity";

    private AudioDecoder mAudioDecoder;
    private AACSampleExtractor mAACSampleExtractor;

    private VideoDecoder mVideoDecoder;
    private NALUExtractor mNALUExtractor;
    private Surface mSurface;

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

        mAudioDecoder = AudioDecoder.build(
                mAACSampleExtractor.getMPEGAudioObjectType(),
                mAACSampleExtractor.getFrequencyIndex(),
                mAACSampleExtractor.getChannelConfiguration(),
                new AudioActivity.AACSampleExtractorSampleProvider(mAACSampleExtractor));

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
            spsBuffer.write(Utils.NALU_HEADER);
            spsBuffer.write(sps);
            ppsBuffer.write(Utils.NALU_HEADER);
            ppsBuffer.write(pps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mVideoDecoder = VideoDecoder.build(
                sequenceParameterSet.getVideoWidth(),
                sequenceParameterSet.getVideoHeight(),
                spsBuffer.toByteArray(),
                ppsBuffer.toByteArray(),
                new VideoActivity.NaluExtractorSampleProvider(mNALUExtractor));

        if (mVideoDecoder == null)
        {
            finish();
            return;
        }

        TextureView textureView = findViewById(R.id.textureView);

        Point displayMetrics  = com.steinwurf.mediaplayer.Utils.getRealMetrics(this);
        textureView.setTransform(Utils.fitScale(
                sequenceParameterSet.getVideoWidth(),
                sequenceParameterSet.getVideoHeight(),
                displayMetrics.x,
                displayMetrics.y).toMatrix());
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAACSampleExtractor.close();
        mNALUExtractor.close();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        mVideoDecoder.setSurface(mSurface);

        mVideoDecoder.start();
        mAudioDecoder.start();
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
