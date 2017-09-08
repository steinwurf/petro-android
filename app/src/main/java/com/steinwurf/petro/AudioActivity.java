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
import com.steinwurf.mediaplayer.Sample;
import com.steinwurf.mediaplayer.SampleProvider;

public class AudioActivity extends AppCompatActivity
{
    private static final String TAG = "AudioActivity";

    public static class AACSampleExtractorSampleProvider implements SampleProvider {
        private final AACSampleExtractor extractor;

        public AACSampleExtractorSampleProvider(AACSampleExtractor extractor) {
            this.extractor = extractor;
        }

        @Override
        public boolean hasSample() {
            return !extractor.atEnd();
        }

        @Override
        public Sample getSample() throws IndexOutOfBoundsException {
            if (extractor.atEnd())
                throw new IndexOutOfBoundsException();

            Sample sample = new Sample(
                    extractor.getPresentationTimestamp(),
                    extractor.getSample());

            extractor.advance();

            return sample;
        }
    }

    private AudioDecoder mAudioDecoder;

    private AACSampleExtractor mAACSampleExtractor;

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

        mAudioDecoder = AudioDecoder.build(
                mAACSampleExtractor.getMPEGAudioObjectType(),
                mAACSampleExtractor.getFrequencyIndex(),
                mAACSampleExtractor.getChannelConfiguration(),
                new AACSampleExtractorSampleProvider(mAACSampleExtractor));

        if (mAudioDecoder == null)
        {
            finish();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAudioDecoder.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAudioDecoder.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAACSampleExtractor.close();
    }
}
