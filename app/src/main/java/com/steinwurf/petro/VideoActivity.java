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
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.steinwurf.mediaextractor.Extractor;
import com.steinwurf.mediaextractor.NALUExtractor;
import com.steinwurf.mediaextractor.SequenceParameterSet;
import com.steinwurf.mediaplayer.Sample;
import com.steinwurf.mediaplayer.SampleProvider;
import com.steinwurf.mediaplayer.Utils;
import com.steinwurf.mediaplayer.VideoDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class VideoActivity extends Activity implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "VideoActivity";

    public static class NaluExtractorSampleProvider implements SampleProvider
    {
        private final NALUExtractor extractor;

        public NaluExtractorSampleProvider(NALUExtractor extractor)
        {
            this.extractor = extractor;
        }

        @Override
        public boolean hasSample() {
            return !extractor.atEnd();
        }

        @Override
        public Sample getSample() {
            if (extractor.atEnd())
                throw new IndexOutOfBoundsException();

            long timestamp = extractor.getPresentationTimestamp();
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try
            {
                do {
                    data.write(VideoDecoder.NALU_HEADER);
                    data.write(extractor.getNalu());
                    extractor.advance();
                } while  (!extractor.atEnd() && !extractor.isBeginningOfAVCSample());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Sample(timestamp, data.toByteArray());
        }
    }

    private VideoDecoder mVideoDecoder;

    private NALUExtractor mNALUExtractor;
    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mNALUExtractor = new NALUExtractor();
        mNALUExtractor.setFilePath(filePath);

        try {
            mNALUExtractor.open();
        } catch (Extractor.UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }

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
                new NaluExtractorSampleProvider(mNALUExtractor));

        if (mVideoDecoder == null)
        {
            finish();
            return;
        }

        TextureView textureView = findViewById(R.id.textureView);

        Point displayMetrics  = Utils.getRealMetrics(this);
        textureView.setTransform(
                Utils.fitScale(
                        sequenceParameterSet.getVideoWidth(),
                        sequenceParameterSet.getVideoHeight(),
                        displayMetrics.x,
                        displayMetrics.y).toMatrix());
        textureView.setSurfaceTextureListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mNALUExtractor.close();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurface = new Surface(surface);
        mVideoDecoder.setSurface(mSurface);
        try {
            mVideoDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
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
