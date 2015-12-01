package com.steinwurf.petro;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by jpihl on 11/27/15.
 */
public class VideoDecoder extends Thread {

    private static final String TAG = "VideoDecoder";
    private static final String MIME = "video/avc";
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;

    private MediaCodec mDecoder;

    private boolean mEosReceived;

    public boolean init(Surface surface, byte[] sps, byte[] pps)
    {
        try {
            mDecoder = MediaCodec.createDecoderByType(MIME);
            MediaFormat format = MediaFormat.createVideoFormat(MIME, WIDTH, HEIGHT);

            format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, HEIGHT * WIDTH);
            format.setInteger("durationUs", Integer.MAX_VALUE);

            mDecoder.configure(format, surface, null, 0 /* Decoder */);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void run() {
        mDecoder.start();
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();
        long startMs = System.currentTimeMillis();
        int i = 0;
        while (!mEosReceived) {
            if (!mEosReceived) {
                int inIndex = mDecoder.dequeueInputBuffer(1000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    byte[] data = NativeInterface.getSample(i % 100);
                    i++;
                    buffer.clear();
                    buffer.put(data);
                    buffer.clear();

                    if (data.length <= 0) {
                        mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mEosReceived = true;
                    } else {
                        mDecoder.queueInputBuffer(inIndex, 0, data.length, 0, 0);
                    }
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = mDecoder.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d("DecodeActivity", "New format " + mDecoder.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d("DecodeActivity", "dequeueOutputBuffer timed out! " + info);
                    break;
                default:
                    ByteBuffer buffer = outputBuffers[outIndex];
                    Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                    while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    mDecoder.releaseOutputBuffer(outIndex, true);
                    break;
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
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
