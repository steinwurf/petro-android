// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder extends Thread
{
    private static final String TAG = "VideoDecoder";

    private static final int TIMEOUT_US = 10000;
    private static final String MIME = "video/avc";

    private MediaCodec mDecoder;

    private boolean mEosReceived;
    private long mLastSleepTime = 0;
    private long mLastPlayTime = 0;
    private long mFrameDrops = 0;

    long lastSleepTime()
    {
        return mLastSleepTime;
    }

    public long lastPlayTime()
    {
        return mLastPlayTime;
    }

    public long frameDrops()
    {
        return mFrameDrops;
    }

    public boolean init(Surface surface, byte[] sps, byte[] pps)
    {
        int width = NativeInterface.getVideoWidth();
        int height = NativeInterface.getVideoHeight();

        try
        {
            mDecoder = MediaCodec.createDecoderByType(MIME);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        MediaFormat format = MediaFormat.createVideoFormat(MIME, width, height);

        if (format == null)
        {
            Log.e(TAG, "Can't create format!");
            return false;
        }

        format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        format.setInteger("durationUs", Integer.MAX_VALUE);

        mDecoder.configure(format, surface, null, 0 /* Decoder */);

        return true;
    }

    @Override
    public void run()
    {
        mEosReceived = false;
        mDecoder.start();

        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        mDecoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();

        long startTime = System.currentTimeMillis();
        while (!mEosReceived)
        {
            // Fill up as many input buffers as possible
            while (!NativeInterface.videoAtEnd())
            {
                int inputIndex = mDecoder.dequeueInputBuffer(0);

                if (inputIndex < 0)
                    break;

                // fill inputBuffers[inputBufferIndex] with valid data
                ByteBuffer inputBuffer = inputBuffers[inputIndex];

                long sampleTime = NativeInterface.getVideoPresentationTime();
                byte[] data = NativeInterface.getVideoSample();
                inputBuffer.clear();
                inputBuffer.put(data);
                int sampleSize = data.length;

                mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                NativeInterface.advanceVideo();
            }

            if (NativeInterface.videoAtEnd())
            {
                int inputIndex = mDecoder.dequeueInputBuffer(0);
                if (inputIndex >= 0)
                {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inputIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mEosReceived = true;
                }
            }

            // Check if output is available
            int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);

            if (outIndex >= 0)
            {
                long playTime = System.currentTimeMillis() - startTime;
                long sleepTime = (info.presentationTimeUs / 1000) - playTime;

                mLastPlayTime = playTime;
                mLastSleepTime = sleepTime;

                if (sleepTime > 0)
                {
                    try
                    {
                        Thread.sleep(sleepTime);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                //Log.d(TAG, "Output sample: " + info.presentationTimeUs);

                if (sleepTime < -30)
                {
                    mDecoder.releaseOutputBuffer(outIndex, false);
                    mFrameDrops++;
                }
                else
                {
                    // true indicates that the frame should be rendered
                    mDecoder.releaseOutputBuffer(outIndex, true);
                }

//                Log.d(TAG,
//                    "playTime: " + playTime + " " +
//                    "sleepTime : " + sleepTime);
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
            {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;
    }

    public void close()
    {
        mEosReceived = true;
    }


}
