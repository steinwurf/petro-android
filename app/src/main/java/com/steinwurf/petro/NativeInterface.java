// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.util.Log;

@SuppressWarnings("JniMissingFunction")
public class NativeInterface {

    private static final String TAG = "NativeInterface";

    private static long native_context;

    static {
        Log.d(TAG, "Loading petro android library");
        System.loadLibrary("petro_android");
    }

    interface NativeInterfaceListener
    {
        void onInitialized();
    }

    private static NativeInterfaceListener mListener;

    public static void setNativeInterfaceListener(NativeInterfaceListener listener)
    {
        mListener = listener;
    }

    public static void onInitialized()
    {
        if (mListener != null)
            mListener.onInitialized();
    }

    public static native void nativeInitialize(String mp4_file);
    public static native void nativeFinalize();

    public static native void advanceVideo();
    public static native boolean videoAtEnd();
    public static native byte[] getVideoSample();
    public static native int getVideoPresentationTime();
    public static native int getVideoWidth();
    public static native int getVideoHeight();
    public static native byte[] getPPS();
    public static native byte[] getSPS();

    public static native void advanceAudio();
    public static native boolean audioAtEnd();
    public static native byte[] getAudioSample();
    public static native int getAudioPresentationTime();
    public static native int getAudioSampleRate();
    public static native int getAudioChannelCount();
    public static native int getAudioCodecProfileLevel();

}
