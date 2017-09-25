package com.steinwurf.mediaplayer;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

import java.lang.reflect.Method;

public class Utils {

    /**
     * Returns the default display Metrics as a {@link Point}.
     * @param activity The activity.
     * @return the default display Metrics as a {@link Point}.
     */
    public static Point getRealMetrics(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return getRealMetrics(display);
    }

    /**
     * Returns the Metrics of the given display as a {@link Point}.
     * @param display The display.
     * @return the Metrics of the given display as a {@link Point}.
     */
    public static Point getRealMetrics(Display display ) {
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

    private static int gcd(int a, int b)
    {
        if (b == 0)
            return a;
        return gcd(b, a % b);
    }

    /**
     * Returns a {@link String} representing the aspect ratio.
     * @param width With of item
     * @param height Height of item
     * @return {@link String} representing the aspect ratio.
     */
    public static String aspectRatio(int width, int height)
    {
        int r = gcd(width, height);
        return width / r + ":" + height / r;
    }

    public static class Scale
    {
        final float sx;
        final float sy;
        final float px;
        final float py;

        Scale(float sx, float sy, float px, float py)
        {
            this.sx = sx;
            this.sy = sy;
            this.px = px;
            this.py = py;
        }

        /**
         * Returns a scaled {@link Matrix} to be used with {@link Matrix#setScale}.
         * @return a scaled {@link Matrix} to be used with {@link Matrix#setScale}.
         */
        public Matrix toMatrix() {
            Matrix matrix = new Matrix();
            matrix.setScale(sx, sy, px, py);
            return matrix;
        }
    }

    /**
     * Returns a {@link Scale} that will fill the view.
     * With this approach some content may be lost, depending on the aspect ratio of the view
     * and video.
     * @param videoWidth The width of the video
     * @param videoHeight The height of the video
     * @param viewWidth The width of the view
     * @param viewHeight The height of the view
     * @return a {@link Scale}.
     */
    public static Scale fillScale(int videoWidth, int videoHeight, int viewWidth, int viewHeight)
    {
        float scale = Math.max(
                (float)viewHeight / videoHeight,
                (float)viewWidth / videoWidth);

        float scaledWidth = videoWidth * scale;
        float scaledHeight = videoHeight * scale;
        return new Scale(
                scaledWidth / viewWidth,
                scaledHeight / viewHeight,
                viewWidth / 2f,
                viewHeight / 2f);
    }

    /**
     * Returns a {@link Scale} that will fit the view.
     * This will expand the video to fit the view, potentially adding black bars, depending on
     * the aspect ratio of the view and video.
     * @param videoWidth The width of the video
     * @param videoHeight The height of the video
     * @param viewWidth The width of the view
     * @param viewHeight The height of the view
     * @return a {@link Scale} that will fit the view.
     */
    public static Scale fitScale(int videoWidth, int videoHeight, int viewWidth, int viewHeight)
    {
        float scale = Math.min(
                (float)viewHeight / videoHeight,
                (float)viewWidth / videoWidth);

        float scaledWidth = videoWidth * scale;
        float scaledHeight = videoHeight * scale;

        return new Scale(
                scaledWidth / viewWidth,
                scaledHeight / viewHeight,
                viewWidth / 2f,
                viewHeight / 2f);
    }
}
