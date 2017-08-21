// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

package com.steinwurf.petro;

import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class VideoExtractorDecoder
{
    private static final String TAG = "VideoExtractorDecoder";

    private static final String VIDEO = "video/";
    private static final int TIMEOUT_US = 10000;

    private final MediaExtractor extractor;
    private final MediaFormat format;
    private boolean mRunning = false;
    private Surface mSurface = null;
    private Thread mThread = null;
    private long mLastSleepTime = 0;
    private long mLastSampleTime = 0;
    private long mFrameDrops = 0;


    public static VideoExtractorDecoder build(String filePath)
    {
        MediaExtractor extractor = new MediaExtractor();
        try
        {
            extractor.setDataSource(filePath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        VideoExtractorDecoder videoExtractorDecoder = null;
        for (int i = 0; i < extractor.getTrackCount(); i++)
        {
            MediaFormat format = extractor.getTrackFormat(i);

            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(VIDEO))
            {
                extractor.selectTrack(i);
                videoExtractorDecoder = new VideoExtractorDecoder(extractor, format);
                break;
            }
        }
        return videoExtractorDecoder;
    }

    private VideoExtractorDecoder(MediaExtractor extractor, MediaFormat format)
    {
        this.extractor = extractor;
        this.format = format;
    }

    public int getVideoHeight()
    {
        return format.getInteger(MediaFormat.KEY_HEIGHT);
    }

    public int getVideoWidth()
    {
        return format.getInteger(MediaFormat.KEY_WIDTH);
    }

    public long lastSleepTime()
    {
        return mLastSleepTime;
    }

    public long lastSampleTime()
    {
        return mLastSampleTime;
    }

    public long frameDrops()
    {
        return mFrameDrops;
    }

    public void setSurface(Surface surface)
    {
        mSurface = surface;
    }

    public void start()
    {
        if (mSurface == null)
            throw new AssertionError();

        mThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mRunning = true;
                MediaCodec decoder;
                try
                {
                    decoder =
                            MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return;
                }
                decoder.configure(format, mSurface, null, 0);
                decoder.start();
                ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                long startTime = System.currentTimeMillis();
                while (mRunning) {

                    int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (inputIndex >= 0) {
                        // fill inputBuffers[inputBufferIndex] with valid data
                        ByteBuffer inputBuffer = inputBuffers[inputIndex];
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);

                        if (sampleSize > 0) {
                            long sampleTime = extractor.getSampleTime();
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);

                            extractor.advance();
                        } else {
                            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mRunning = false;
                        }
                    }

                    int outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US);
                    if (outIndex >= 0) {
                        long sampleTime = info.presentationTimeUs / 1000;
                        long playTime = System.currentTimeMillis() - startTime;
                        long sleepTime = sampleTime - playTime;

                        mLastSleepTime = sleepTime;
                        mLastSampleTime = sampleTime;

                        if (sleepTime > 0) {
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // Drop the buffer if it is late by more than 50 ms
                        if (sleepTime < -50) {
                            decoder.releaseOutputBuffer(outIndex, false);
                            mFrameDrops++;
                            Log.d(TAG, "Dropped Frame " + sleepTime);
                        } else {
                            // true indicates that the frame should be rendered
                            decoder.releaseOutputBuffer(outIndex, true);
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break;
                        }
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mRunning = false;
                    }
                }

                decoder.stop();
                decoder.release();
                extractor.release();
            }
        });
        mThread.start();
    }

    public void stop()
    {
        mRunning = false;
        if (mThread != null)
        {
            try
            {
                mThread.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            mThread = null;
        }
    }
}
