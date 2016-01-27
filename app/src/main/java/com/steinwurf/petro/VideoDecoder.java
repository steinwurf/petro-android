package com.steinwurf.petro;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder extends Thread {
    private static final String TAG = "VideoDecoder";

    private static final int TIMEOUT_US = 10000;
    private static final String MIME = "video/avc";

    private MediaCodec mDecoder;

    private boolean mEosReceived;

    public boolean init(Surface surface, byte[] sps, byte[] pps) {
        int width = NativeInterface.getWidth();
        int height = NativeInterface.getHeight();

        try {
            mDecoder = MediaCodec.createDecoderByType(MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat format = MediaFormat.createVideoFormat(MIME, width, height);

        if (format == null) {
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
    public void run() {
        mEosReceived = false;
        mDecoder.start();
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        mDecoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();

        long startWhen = System.currentTimeMillis();
        long sampleTime =  0;
        int i = 0;
        while (!mEosReceived) {
            int inputIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputIndex >= 0) {
                // fill inputBuffers[inputBufferIndex] with valid data
                ByteBuffer inputBuffer = inputBuffers[inputIndex];
                byte[] data = NativeInterface.getVideoSample();
                i++;
                inputBuffer.clear();
                inputBuffer.put(data);

                int sampleSize = data.length;
                if (sampleSize > 0) {
                    sampleTime += NativeInterface.getVideoSampleTime() * 1000;
                    mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                } else {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }
            int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
            switch (outIndex) {
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
                    try {
                        long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mDecoder.releaseOutputBuffer(outIndex, true);
                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        mDecoder.stop();
        mDecoder.release();
    }

    public void close()
    {
        mEosReceived = true;
    }
}
