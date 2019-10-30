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
    private AVCSampleExtractor mAVCSampleExtractor;
    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mAVCSampleExtractor = new AVCSampleExtractor();

        mAACSampleExtractor = new AACSampleExtractor();

        try {
            TrackExtractor trackExtractor = new TrackExtractor();
            trackExtractor.open(filePath);
            TrackExtractor.Track[] tracks = trackExtractor.getTracks();
            int videoTrackId = -1;
            int audioTrackId = -1;
            for (TrackExtractor.Track track : tracks)
            {
                if (track.type == TrackExtractor.TrackType.AAC) {
                    audioTrackId = track.id;
                }

                if (track.type == TrackExtractor.TrackType.AVC1) {
                    videoTrackId = track.id;
                }
            }
            if (audioTrackId == -1 || videoTrackId == -1)
            {
                finish();
            }
            mAVCSampleExtractor.open(filePath, videoTrackId);
            mAACSampleExtractor.open(filePath, audioTrackId);
        } catch (UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }
        mAVCSampleExtractor.setLoopingEnabled(true);
        mAACSampleExtractor.setLoopingEnabled(true);

        mAudioDecoder = AudioDecoder.build(
                mAACSampleExtractor.getMPEGAudioObjectType(),
                mAACSampleExtractor.getFrequencyIndex(),
                mAACSampleExtractor.getChannelConfiguration(),
                new AudioActivity.AACSampleExtractorSampleProvider(mAACSampleExtractor));

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
                new VideoActivity.NaluExtractorSampleProvider(mAVCSampleExtractor));

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
        mAVCSampleExtractor.close();
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
