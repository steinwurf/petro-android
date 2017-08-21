package com.steinwurf.mediaplayer;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;

public class AudioDecoder
{
    private static final String TAG = "AudioDecoder";

    private static final int TIMEOUT_US = 10000;
    private static final String MIME = "audio/mp4a-latm";

    private static final int[] SAMPLE_RATES = new int[]
            {
                    96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                    16000, 12000, 11025, 8000
            };

    public static AudioDecoder build(int audioProfile, int sampleRateIndex, int channelCount)
    {
        int sampleRate = SAMPLE_RATES[sampleRateIndex];

        MediaFormat format = MediaFormat.createAudioFormat(MIME, sampleRate, channelCount);

        if (format == null)
        {
            Log.e(TAG, "Can't create format!");
            return null;
        }

        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put((byte) ((audioProfile << 3) | (sampleRateIndex >> 1)));
        csd.position(1);
        csd.put((byte) ((byte) ((sampleRateIndex << 7) & 0x80) | (channelCount << 3)));
        csd.flip();
        format.setByteBuffer("csd-0", csd); // add csd-0

        return new AudioDecoder(format);
    }

    private final MediaFormat format;

    private SampleStorage mSampleStorage = null;
    private Thread mThread = null;
    private boolean mRunning = false;

    private long mLastSleepTime = 0;
    private long mLastSampleTime = 0;
    private long mFrameDrops = 0;

    private AudioDecoder(MediaFormat format)
    {
        this.format = format;
    }

    public void setSampleStorage(SampleStorage sampleStorage)
    {
        mSampleStorage = sampleStorage;
    }

    long lastSleepTime()
    {
        return mLastSleepTime;
    }

    long lastSampleTime()
    {
        return mLastSampleTime;
    }

    long frameDrops()
    {
        return mFrameDrops;
    }

    public void start()
    {
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

                decoder.configure(format, null, null, 0);

                int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        AudioTrack.getMinBufferSize(
                                sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT),
                        AudioTrack.MODE_STREAM);

                audioTrack.play();

                decoder.start();
                ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

                BufferInfo info = new BufferInfo();

                while (mRunning)
                {
                    // Try to add new samples if our samples list contains some data
                    if (mSampleStorage != null && mSampleStorage.getCount() > 0)
                    {
                        int inIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                        if (inIndex >= 0)
                        {
                            // fill inputBuffers[inputBufferIndex] with valid data
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();

                            try
                            {
                                // Pop the first sample from samples
                                SampleStorage.Sample sample = mSampleStorage.getNextSample();
                                buffer.put(sample.data);
                                int sampleSize = sample.data.length;
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, sample.timestamp, 0);
                            }
                            catch (Exception e)
                            {
                                Log.e(TAG, "Failed to push buffer to decoder");
                                e.printStackTrace();
                            }
                        }
                    }

                    int outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US);
                    switch (outIndex)
                    {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = decoder.getOutputBuffers();
                            break;

                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MediaFormat format = decoder.getOutputFormat();
                            Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + format);
                            audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                            break;

                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            break;

                        default:

                            long playTime = mSampleStorage.getPlayTime();
                            long sleepTime = (info.presentationTimeUs / 1000) - playTime;

                            mLastSleepTime = sleepTime;
                            mLastSampleTime = info.presentationTimeUs / 1000;

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
                                Log.d(TAG, "Dropped Frame " + sleepTime);
                                mFrameDrops++;
                            }
                            else
                            {
                                ByteBuffer outBuffer = outputBuffers[outIndex];
                                final byte[] chunk = new byte[info.size];
                                outBuffer.get(chunk);
                                outBuffer.clear();
                                audioTrack.write(chunk, info.offset, info.offset + info.size);
                            }
                            decoder.releaseOutputBuffer(outIndex, false);
                            break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    {
                        Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

                decoder.stop();
                decoder.release();

                audioTrack.stop();
                audioTrack.release();
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
