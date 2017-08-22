// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.steinwurf.mediaextractor.AACSampleExtractor;
import com.steinwurf.mediaextractor.Extractor;
import com.steinwurf.mediaplayer.AudioDecoder;
import com.steinwurf.mediaplayer.SampleStorage;

public class AudioActivity extends AppCompatActivity
{
    private static final String TAG = "AudioActivity";

    private AudioDecoder mAudioDecoder;
    private SampleStorage mSampleStorage;

    private AACSampleExtractor mAACSampleExtractor;

    Thread mExtractorThread;
    boolean mRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mAACSampleExtractor = new AACSampleExtractor();
        mAACSampleExtractor.setFilePath(filePath);
        try {
            mAACSampleExtractor.open();
        } catch (Extractor.UnableToOpenException e) {
            e.printStackTrace();
            finish();
            return;
        }
        mSampleStorage = new SampleStorage(0);
        mAudioDecoder = AudioDecoder.build(
                mAACSampleExtractor.getMPEGAudioObjectType(),
                mAACSampleExtractor.getFrequencyIndex(),
                mAACSampleExtractor.getChannelConfiguration(),
                mSampleStorage);
        if (mAudioDecoder == null)
        {
            finish();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRunning = true;
        mExtractorThread = new Thread(){
            public void run(){
                while (mRunning && !mAACSampleExtractor.atEnd())
                {
                    mSampleStorage.addSample(
                            mAACSampleExtractor.getDecodingTimestamp(),
                            mAACSampleExtractor.getSample());
                    mAACSampleExtractor.advance();
                }
            }
        };

        mExtractorThread.start();
        mAudioDecoder.start();
    }

    @Override
    protected void onStop()
    {
        if (mExtractorThread != null) {
            try {
                mRunning = false;
                mExtractorThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mAudioDecoder.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAACSampleExtractor.close();
    }
}
