package com.steinwurf.petro;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;

public class AudioDecoder  extends Thread {
    private static final String TAG = "AudioDecoder";

    private static final int TIMEOUT_US = 10000;
    private static final String MIME = "audio/mp4a-latm";

    private MediaCodec mDecoder;

    private boolean mEosReceived;

    AudioTrack mAudioTrack;

    public boolean init(int audioProfile, int sampleRateIndex, int channelCount) {

        try {
            mDecoder = MediaCodec.createDecoderByType(MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int sampleRate = new int[]{
                96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                16000, 12000, 11025, 8000}[sampleRateIndex];

        MediaFormat format = MediaFormat.createAudioFormat(MIME, sampleRate, channelCount);

        if (format == null) {
            Log.e(TAG, "Can't create format!");
            return false;
        }

        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put((byte) ((audioProfile << 3) | (sampleRateIndex >> 1)));
        csd.position(1);
        csd.put((byte) ((byte) ((sampleRateIndex << 7) & 0x80) | (channelCount << 3)));
        csd.flip();
        format.setByteBuffer("csd-0", csd); // add csd-0

        mDecoder.configure(format, null, null, 0);

        // create an audiotrack object
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);

        return true;
    }

    @Override
    public void run() {
        mEosReceived = false;
        mDecoder.start();

        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();

        BufferInfo info = new BufferInfo();

        mAudioTrack.play();

        long sampleTime =  0;
        int i = 0;
        while (!mEosReceived) {
            int inputIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputIndex >= 0) {
                // fill inputBuffers[inputBufferIndex] with valid data
                ByteBuffer inputBuffer = inputBuffers[inputIndex];
                int sampleIndex = i % NativeInterface.getAudioSampleCount();
                byte[] data = NativeInterface.getAudioSample(sampleIndex);
                i++;
                inputBuffer.clear();
                inputBuffer.put(data);
                inputBuffer.clear();
                int sampleSize = data.length;
                if (sampleSize > 0) {
                    sampleTime += NativeInterface.getAudioSampleTime() * 1000;
                    mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                } else {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }


                int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = mDecoder.getOutputBuffers();
                        break;

                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat format = mDecoder.getOutputFormat();
                        Log.d(TAG, "New format " + format);
                        mAudioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "dequeueOutputBuffer timed out!");
                        break;

                    default:

                        ByteBuffer outBuffer = outputBuffers[outIndex];
                        Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + outBuffer);

                        final byte[] chunk = new byte[info.size];
                        outBuffer.get(chunk); // Read the buffer all at once
                        outBuffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                        mAudioTrack.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data
                        mDecoder.releaseOutputBuffer(outIndex, false);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
        }

        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;

        mAudioTrack.stop();
        mAudioTrack.release();
    }

    public void close()
    {
        mEosReceived = true;
    }

}
