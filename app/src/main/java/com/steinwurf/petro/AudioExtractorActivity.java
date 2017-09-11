// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class AudioExtractorActivity extends AppCompatActivity
{
    private static final String TAG = "AudioExtractorActivity";

    private AudioExtractorDecoder mAudioDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mAudioDecoder = new AudioExtractorDecoder();
        mAudioDecoder.init(filePath);
        mAudioDecoder.start();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mAudioDecoder != null)
        {
            mAudioDecoder.close();
        }
    }
}
