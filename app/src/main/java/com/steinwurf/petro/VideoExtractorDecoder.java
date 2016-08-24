package com.steinwurf.petro;

import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

public class VideoExtractorDecoder extends Thread
{
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoExtractorDecoder";
    private static final int TIMEOUT_US = 10000;

    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private long mLastSampleTime = 0;
    private boolean mEosReceived = false;
    private MediaFormat mFormat;
    private int mVideoWidth;
    private int mVideoHeight;

    public VideoExtractorDecoder(String filePath)
    {
        try
        {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(filePath);

            for (int i = 0; i < mExtractor.getTrackCount(); i++)
            {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO))
                {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);

                    mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                    mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                    mFormat = format;
                    Log.d(TAG, "Media format : " + format);

                    break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean init(Surface surface)
    {
        try
        {
            mDecoder.configure(mFormat, surface, null, 0 /* Decoder */);
        }
        catch (IllegalStateException e)
        {
            Log.e(TAG, "MediaCodec configuration failed." + e);
            return false;
        }

        return true;
    }

    public int getVideoHeight()
    {
        return mVideoHeight;
    }

    public int getVideoWidth()
    {
        return mVideoWidth;
    }

    @Override
    public void run()
    {
        mEosReceived = false;
        mDecoder.start();

        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        mDecoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();

        long startWhen = System.currentTimeMillis();
        while (!mEosReceived)
        {
            int inputIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputIndex >= 0)
            {
                // fill inputBuffers[inputBufferIndex] with valid data
                ByteBuffer inputBuffer = inputBuffers[inputIndex];

                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                if (sampleSize > 0)
                {
                    long sampleTime = mExtractor.getSampleTime();
                    mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);

                    mExtractor.advance();
                }
                else
                {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inputIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mEosReceived = true;
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
            switch (outIndex)
            {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    mDecoder.getOutputBuffers();
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat format = mDecoder.getOutputFormat();
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + format);
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    break;

                default:

                    try
                    {
                        long sleepTime = (info.presentationTimeUs / 1000) -
                            (System.currentTimeMillis() - startWhen);
                        Log.d(TAG,
                            "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " " +
                            "playTime: " + (System.currentTimeMillis() - startWhen) + " " +
                            "sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                    mDecoder.releaseOutputBuffer(outIndex, true);
                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
            {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        mDecoder.stop();
        mDecoder.release();
        mExtractor.release();
    }

    public void close()
    {
        mEosReceived = true;
    }
}
