// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.view.SurfaceView;
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

        if (videoWidth > videoHeight)
        {
            // landscape
            layoutParams.width = screen.x;
            layoutParams.height = screen.x * videoHeight / videoWidth;
        }
        else
        {
            // portrait
            layoutParams.width = screen.y * videoWidth / videoHeight;
            layoutParams.height = screen.y;
        }

        surfaceView.setLayoutParams(layoutParams);
    }
}
