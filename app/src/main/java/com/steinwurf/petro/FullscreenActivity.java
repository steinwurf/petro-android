// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class FullscreenActivity extends Activity
{
    private static final String TAG = "FullscreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Lock the current orientation so that the playback can
        // continue even if the user turns the device
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void fillAspectRatio(SurfaceView surfaceView, int videoWidth, int videoHeight)
    {
        Point screen = new Point();
        getWindowManager().getDefaultDisplay().getSize(screen);

        android.view.ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();

        int viewWidth = screen.x;
        int viewHeight = screen.y;
        double aspectRatio = (double) videoHeight / videoWidth;
        Log.d(TAG, String.format("View: %d x %d", viewWidth, viewHeight));
        Log.d(TAG, String.format("Video: %d x %d", videoWidth, videoHeight));

        if (viewHeight > (int) (viewWidth * aspectRatio))
        {
            // limited by narrow width; restrict height
            layoutParams.width = viewWidth;
            layoutParams.height = (int) (viewWidth * aspectRatio);
        }
        else
        {
            // limited by short height; restrict width
            layoutParams.width = (int) (viewHeight / aspectRatio);
            layoutParams.height = viewHeight;
        }
        Log.d(TAG, String.format("Layout: %d x %d", layoutParams.width, layoutParams.height));

        surfaceView.setLayoutParams(layoutParams);
    }
}
