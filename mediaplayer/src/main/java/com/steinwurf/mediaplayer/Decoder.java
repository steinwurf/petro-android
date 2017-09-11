package com.steinwurf.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract class Decoder {
    private static final String TAG = "Decoder";

    private static final int TIMEOUT_US = 10000;

    // The minimum time interval (in milliseconds) by which a sample should be delayed.
    // We increase the value of startTime so that the playback is delayed by the specified
    // interval. This is necessary to prevent starvation during playback and to allow implicit
    // synchronization with other platforms.
    private static final long MINIMUM_BUFFERING_DELAY_MS = 200;

    private final String type;
    private final SampleProvider sampleProvider;

    private final MediaFormat format;

    Surface mSurface = null;
    private Thread mThread = null;
    private boolean mRunning = false;

    private long mLastSleepTime = 0;
    private long mLastSampleTime = 0;
    private long mFrameDrops = 0;
    private long mDropBufferLimit = 50;

    /**
     * Delay in microseconds
     */
    private long mDelayUs = 0;

    Decoder(@NotNull MediaFormat format, String type, @NotNull SampleProvider sampleProvider)
    {
        this.format = format;
        this.type = type;
        this.sampleProvider = sampleProvider;
    }

    /**
     * Returns the last sleep time.
     * @return the last sleep time.
     */
    public long lastSleepTime()
    {
        return mLastSleepTime;
    }

    /**
     * Returns the last sample time.
     * @return the last sample time.
     */
    public long lastSampleTime()
    {
        return mLastSampleTime;
    }

    /**
     * Returns the number of times a frame has been dropped due to being too late
     * @return the number of times a frame has been dropped due to being too late
     */
    public long frameDrops()
    {
        return mFrameDrops;
    }

    /**
     * The limit determining whether a buffer arrived too late.
     * @return limit determining whether a buffer arrived too late.
     */
    public long dropBufferLimit()
    {
        return mDropBufferLimit;
    }

    /**
     * Change the limit determining whether a buffer arrived too late. If set to 0 no buffers will
     * be dropped.
     * @param dropBufferLimit The new limit.
     */
    public void setDropBufferLimit(long dropBufferLimit)
    {
        mDropBufferLimit = dropBufferLimit;
    }

    /**
     * Gets the set delay in microseconds
     * @return delay in microseconds
     */
    public long getDelay()
    {
        return mDelayUs;
    }

    /**
     * Sets the delay in microseconds
     * @param delay delay in microseconds
     */
    public void setDelay(long delay)
    {
        mDelayUs = delay;
    }

    /**
     * Starts the playback
     */
    public void start() {
        mRunning = true;
        mThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mRunning = true;
                MediaCodec decoder;
                try
                {
                    decoder = MediaCodec.createDecoderByType(type);
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

                Long startTime = null;
                while (mRunning) {
                    // Try to add new samples if our samples list contains some data
                    if (sampleProvider.hasSample()) {
                        int inIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                        if (inIndex >= 0) {
                            // fill inputBuffers[inputBufferIndex] with valid data
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();

                            try {
                                // Pop the next sample from samples
                                Sample sample = sampleProvider.getSample();
                                buffer.put(sample.data);
                                decoder.queueInputBuffer(
                                        inIndex,
                                        0,
                                        sample.data.length,
                                        sample.timestamp + mDelayUs,
                                        0);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to push buffer to decoder");
                                e.printStackTrace();
                            }
                        }
                    }

                    int outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US);

                    try {
                        switch (outIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                outputBuffersChanged(decoder);
                                break;

                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                outputFormatChanged(decoder);
                                break;

                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                tryAgainLater(decoder);
                                break;
                            default:

                                if (startTime == null) {
                                    startTime = System.currentTimeMillis() + MINIMUM_BUFFERING_DELAY_MS;
                                }
                                long sampleTime = info.presentationTimeUs / 1000;
                                long playTime = System.currentTimeMillis() - startTime;
                                long sleepTime = sampleTime - playTime;

                                mLastSleepTime = sleepTime;
                                mLastSampleTime = sampleTime;

                                if (sleepTime > 0) {
                                    Thread.sleep(sleepTime);
                                }

                                // Determine of the buffer should be dropped due to being too late
                                if (mDropBufferLimit == 0 || sleepTime >= -mDropBufferLimit) {
                                    render(decoder, info, outIndex);
                                } else {
                                    decoder.releaseOutputBuffer(outIndex, false);
                                    mFrameDrops++;
                                    Log.d(TAG, "Dropped Frame " + sleepTime);
                                }
                                break;
                        }


                        // All decoded frames have been rendered, we can stop playing now
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                            break;
                        }
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d(TAG, "stopping and releasing decoder");
                decoder.stop();
                decoder.release();
            }
        });
        mThread.start();
    }

    /**
     * If the outIndex returned by @{@link MediaCodec#dequeueOutputBuffer(MediaCodec.BufferInfo, long)}
     * is {@link MediaCodec#INFO_TRY_AGAIN_LATER}.
     * @param decoder The {@link MediaCodec}
     */
    protected void tryAgainLater(MediaCodec decoder) { }

    /**
     * If the outIndex returned by @{@link MediaCodec#dequeueOutputBuffer(MediaCodec.BufferInfo, long)}
     * is {@link MediaCodec#INFO_OUTPUT_FORMAT_CHANGED}.
     * @param decoder The {@link MediaCodec}
     */
    protected void outputFormatChanged(MediaCodec decoder) { }

    /**
     * If the outIndex returned by @{@link MediaCodec#dequeueOutputBuffer(MediaCodec.BufferInfo, long)}
     * is {@link MediaCodec#INFO_OUTPUT_BUFFERS_CHANGED}.
     * @param decoder The {@link MediaCodec}
     */
    protected void outputBuffersChanged(MediaCodec decoder) { }

    /**
     * Render the buffer at the give index
     * @param decoder The {@link MediaCodec}
     * @param info The {@link MediaCodec.BufferInfo}.
     * @param index The index returned by @{@link MediaCodec#dequeueOutputBuffer(MediaCodec.BufferInfo, long)}
     */
    protected abstract void render(MediaCodec decoder, MediaCodec.BufferInfo info, int index);

    /**
     * Stops the playback.
     */
    public void stop()
    {
        mRunning = false;
        if (mThread != null)
        {
            mThread.interrupt();
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
