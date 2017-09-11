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

import com.steinwurf.mediaplayer.Utils;

public class VideoExtractorActivity extends Activity implements TextureView.SurfaceTextureListener
{

    private static final String TAG = "VideoExtractorActivity";
    private VideoExtractorDecoder mVideoDecoder;

    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(MainActivity.FILEPATH);

        mVideoDecoder = VideoExtractorDecoder.build(filePath);

        TextureView textureView = findViewById(R.id.textureView);

        Point displayMetrics  = com.steinwurf.mediaplayer.Utils.getRealMetrics(this);
        textureView.setTransform(
                Utils.fitScale(
                        mVideoDecoder.getVideoWidth(),
                        mVideoDecoder.getVideoHeight(),
                        displayMetrics.x,
                        displayMetrics.y).toMatrix());
        textureView.setSurfaceTextureListener(this);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurface = new Surface(surface);
        mVideoDecoder.setSurface(mSurface);
        mVideoDecoder.start();
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
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
