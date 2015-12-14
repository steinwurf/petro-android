package com.steinwurf.petro;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jpihl on 11/27/15.
 */
public class AudioDecoder extends Thread {

    private static final String TAG = "AudioDecoder";
    private static final String MIME = "audio/mp4a-latm";

    private MediaCodec mDecoder;

    private boolean mEosReceived;

    public boolean init(int sampleRate, int channelCount)
    {
        try {
            mDecoder = MediaCodec.createDecoderByType(MIME);
            MediaFormat format = new MediaFormat();

            format.setString(MediaFormat.KEY_MIME, MIME);

            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            format.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            mDecoder.configure(format, null, null, 0 /* Decoder */);
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

        Log.d(TAG, "Decoding Started");
        while (!mEosReceived) {
            if (!mEosReceived) {
                int inIndex = mDecoder.dequeueInputBuffer(1000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    byte[] data = NativeInterface.getAudioSample(i % 100);
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
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = mDecoder.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "New format " + mDecoder.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "dequeueOutputBuffer timed out! " + info);
                    break;
                default:
                    ByteBuffer buffer = outputBuffers[outIndex];
                    Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

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
