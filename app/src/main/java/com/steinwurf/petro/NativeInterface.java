package com.steinwurf.petro;

import android.util.Log;

public class NativeInterface {

    private static final String TAG = "NativeInterface";

    private static long native_context;

    static {
        Log.d(TAG, "Loading petro android library");
        System.loadLibrary("petro_android");
    }

    interface NativeInterfaceListener
    {
        void onMessage(String message);
        void onInitialized();
    }

    private static NativeInterfaceListener mListener;

    public static void setNativeInterfaceListener(NativeInterfaceListener listener)
    {
        mListener = listener;
    }

    public static void onMessage(String message)
    {
        if (mListener != null)
            mListener.onMessage(message);
    }

    public static void onInitialized()
    {
        if (mListener != null)
            mListener.onInitialized();
    }

    public static native void nativeInitialize(String mp4_file);
    public static native byte[] getSample(int index);
    public static native void nativeFinalize();
}
