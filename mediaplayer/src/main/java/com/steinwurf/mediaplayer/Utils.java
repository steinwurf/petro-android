package com.steinwurf.mediaplayer;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

import java.lang.reflect.Method;

public class Utils {

    public static Matrix fitScaleMatrix(int videoWidth, int videoHeight,
                                        int viewWidth, int viewHeight)
    {
        float scale = Math.min(
                (float)viewHeight / videoHeight,
                (float)viewWidth / videoWidth);

        float scaledWidth = videoWidth * scale;
        float scaledHeight = videoHeight * scale;

        final Matrix matrix = new Matrix();
        matrix.setScale(
                scaledWidth / viewWidth,
                scaledHeight / viewHeight,
                viewWidth / 2f,
                viewHeight / 2f);
        return matrix;
    }

    public static Point getRealMetrics(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 17) {
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            return new Point(realMetrics.widthPixels, realMetrics.heightPixels);

        } else {
            //reflection for this weird in-between time
            try {
                Method getRawHeight = Display.class.getMethod("getRawHeight");
                Method getRawWidth = Display.class.getMethod("getRawWidth");
                return new Point(
                        (Integer) getRawWidth.invoke(display),
                        (Integer) getRawHeight.invoke(display));
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                //noinspection deprecation
                return new Point(display.getWidth(), display.getHeight());
            }

        }
    }
}
