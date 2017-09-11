package com.steinwurf.mediaplayer;

import android.graphics.Matrix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsUnitTest {
    @Test
    public void aspectRatio_isCorrect() throws Exception {
        {
            int width = 2;
            int height = 1;
            String aspectRatio = Utils.aspectRatio(width, height);
            assertEquals("2:1", aspectRatio);
        }
        {
            int width = 1920;
            int height = 1080;
            String aspectRatio = Utils.aspectRatio(width, height);
            assertEquals("16:9", aspectRatio);
        }
    }

    @Test
    public void fitScale_isCorrect() throws Exception {
        {
            int videoWidth = 100;
            int videoHeight = 100;
            int viewWidth = 100;
            int viewHeight = 100;
            Utils.Scale scale = Utils.fitScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(50.0, scale.px, 0.0);
            assertEquals(50.0, scale.py, 0.0);
            assertEquals(1.0, scale.sx, 0.0);
            assertEquals(1.0, scale.sy, 0.0);
        }

        {
            int videoWidth = 100;
            int videoHeight = 100;
            int viewWidth = 200;
            int viewHeight = 200;
            Utils.Scale scale = Utils.fitScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(100.0, scale.px, 0.0);
            assertEquals(100.0, scale.py, 0.0);
            assertEquals(1.0, scale.sx, 0.0);
            assertEquals(1.0, scale.sy, 0.0);
        }

        {
            int videoWidth = 200;
            int videoHeight = 100;
            int viewWidth = 100;
            int viewHeight = 200;
            Utils.Scale scale = Utils.fitScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(50.0, scale.px, 0.0);
            assertEquals(100.0, scale.py, 0.0);
            assertEquals(1.0, scale.sx, 0.0);
            assertEquals(0.25, scale.sy, 0.0);
        }
    }

    @Test
    public void fillScale_isCorrect() throws Exception {
        {
            int videoWidth = 100;
            int videoHeight = 100;
            int viewWidth = 100;
            int viewHeight = 100;
            Utils.Scale scale = Utils.fillScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(50.0, scale.px, 0.0);
            assertEquals(50.0, scale.py, 0.0);
            assertEquals(1.0, scale.sx, 0.0);
            assertEquals(1.0, scale.sy, 0.0);
        }

        {
            int videoWidth = 100;
            int videoHeight = 100;
            int viewWidth = 200;
            int viewHeight = 200;
            Utils.Scale scale = Utils.fillScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(100.0, scale.px, 0.0);
            assertEquals(100.0, scale.py, 0.0);
            assertEquals(1.0, scale.sx, 0.0);
            assertEquals(1.0, scale.sy, 0.0);
        }

        {
            int videoWidth = 200;
            int videoHeight = 100;
            int viewWidth = 100;
            int viewHeight = 200;
            Utils.Scale scale = Utils.fillScale(videoWidth, videoHeight, viewWidth, viewHeight);
            assertEquals(50.0, scale.px, 0.0);
            assertEquals(100.0, scale.py, 0.0);
            assertEquals(4.0, scale.sx, 0.0);
            assertEquals(1.0, scale.sy, 0.0);
        }
    }
}
