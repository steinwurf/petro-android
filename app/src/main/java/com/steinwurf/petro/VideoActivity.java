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

import com.steinwurf.mediaplayer.Sample;
import com.steinwurf.mediaplayer.SampleProvider;
import com.steinwurf.mediaplayer.Utils;
import com.steinwurf.mediaplayer.VideoDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalChannelGroupException;
import java.util.List;


public class VideoActivity extends Activity implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "VideoActivity";

    public static class NaluExtractorSampleProvider implements SampleProvider
    {
        private final AVCSampleExtractor extractor;

        NaluExtractorSampleProvider(AVCSampleExtractor extractor)
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
                ByteBuffer sample = ByteBuffer.wrap(extractor.getSample());
                int naluLengthSize = extractor.getNALULengthSize();

                while (sample.hasRemaining())
                {
                    data.write(Utils.NALU_HEADER);
                    int naluSize = com.steinwurf.petro.Utils.getNALUSize(naluLengthSize, sample);
                    data.write(sample.array(), sample.position(), naluSize);
                    sample.position(sample.position() + naluSize);
                }

                extractor.advance();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Sample(timestamp, data.toByteArray());
        }
    }

    private VideoDecoder mVideoDecoder;

    private AVCSampleExtractor mAVCSampleExtractor;
    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mAVCSampleExtractor = new AVCSampleExtractor();

        try {
            TrackExtractor trackExtractor = new TrackExtractor();
            trackExtractor.open(filePath);
            TrackExtractor.Track[] tracks = trackExtractor.getTracks();
            int trackId = -1;
            for (TrackExtractor.Track track : tracks)
            {
                if (track.type == TrackExtractor.TrackType.AVC1) {
                    trackId = track.id;
                    break;
                }
            }
            if (trackId == -1)
            {
                finish();
            }
            mAVCSampleExtractor.open(filePath, trackId);
        } catch (UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }
        mAVCSampleExtractor.setLoopingEnabled(true);
        ByteArrayOutputStream spsBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream ppsBuffer = new ByteArrayOutputStream();

        byte[] sps = mAVCSampleExtractor.getSPS();
        byte[] pps = mAVCSampleExtractor.getPPS();

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
                new NaluExtractorSampleProvider(mAVCSampleExtractor));

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
        mAVCSampleExtractor.close();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurface = new Surface(surface);
        mVideoDecoder.setSurface(mSurface);
        mVideoDecoder.start();
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
