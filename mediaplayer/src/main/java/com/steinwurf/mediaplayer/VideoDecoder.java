package com.steinwurf.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoDecoder
{
    private static final String TAG = "VideoDecoder";

    private static final int TIMEOUT_US = 10000;
    private static final String MIME = "video/avc";
    public static final byte[] NALU_HEADER = new byte[]{0x00, 0x00, 0x00, 0x01};

    private static boolean hasNALUHeader(byte[] buffer)
    {
        return Arrays.equals(Arrays.copyOfRange(buffer, 0, 4), NALU_HEADER);
    }

    public static VideoDecoder build(int width, int height, byte[] sps, byte[] pps)
    {
        if (!hasNALUHeader(sps)) {
            Log.e(TAG, "No header before SPS");
            return null;
        }
        if (!hasNALUHeader(pps)) {
            Log.e(TAG, "No header before PPS");
            return null;
        }

        MediaFormat format = MediaFormat.createVideoFormat(MIME, width, height);

        if (format == null)
        {
            Log.e(TAG, "Can't create format!");
            return null;
        }

        format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        format.setInteger(MediaFormat.KEY_DURATION, Integer.MAX_VALUE);

        return new VideoDecoder(format);
    }

    private final MediaFormat format;

    private SampleStorage mSampleStorage = null;
    private Surface mSurface = null;

    private Thread mThread = null;
    private boolean mRunning = false;

    private long mLastSleepTime = 0;
    private long mLastSampleTime = 0;
    private long mFrameDrops = 0;

    private VideoDecoder(MediaFormat format)
    {
        this.format = format;
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

    public void setSampleStorage(SampleStorage sampleStorage)
    {
        mSampleStorage = sampleStorage;
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
                    decoder = MediaCodec.createDecoderByType(MIME);
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
                while (mRunning)
                {
                    // Try to add new samples if our samples list contains some data
                    if (mSampleStorage != null && mSampleStorage.getCount() > 0)
                    {
                        int inIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                        if (inIndex >= 0)
                        {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();

                            try
                            {
                                // Pop the next sample from samples
                                SampleStorage.Sample sample = mSampleStorage.getNextSample();
                                if (!hasNALUHeader(sample.data))
                                {
                                    throw new Exception("Got sample without NALU header");
                                }
                                buffer.put(sample.data);
                                decoder.queueInputBuffer(
                                        inIndex, 0, sample.data.length, sample.timestamp, 0);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Check if output is available
                    int outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US);

                    if (outIndex >= 0)
                    {
                        long sampleTime = info.presentationTimeUs / 1000;
                        long playTime = System.currentTimeMillis() - startTime;
                        long sleepTime =  sampleTime - playTime;

                        mLastSleepTime = sleepTime;
                        mLastSampleTime = sampleTime;

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

                        // Drop the buffer if it is late by more than 50 ms
                        if (sleepTime < -50)
                        {
                            decoder.releaseOutputBuffer(outIndex, false);
                            mFrameDrops++;
                            Log.d(TAG, "Dropped Frame " + sleepTime);
                        }
                        else
                        {
                            // true indicates that the frame should be rendered
                            decoder.releaseOutputBuffer(outIndex, true);
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                        {
                            break;
                        }
                    }
                }

                decoder.stop();
                decoder.release();
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
