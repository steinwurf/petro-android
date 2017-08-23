package com.steinwurf.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoDecoder extends Decoder {

    /**
     * A {@link SampleStorage} extension which checks the added samples for H264 headers.
     */
    public static class H264SampleStorage extends SampleStorage
    {
        @Override
        public void addSample(long timestamp, byte[] data) {
            if (!hasNALUHeader(data)) {
                Log.e(TAG, "No NALU header before sample");
                return;
            }
            super.addSample(timestamp, data);
        }
    }

    private static final String TAG = "VideoDecoder";
    private static final String MIME = "video/avc";
    /**
     * A buffer containing a NALU header
     */
    public static final byte[] NALU_HEADER = new byte[]{0x00, 0x00, 0x00, 0x01};

    private static boolean hasNALUHeader(byte[] buffer)
    {
        return Arrays.equals(Arrays.copyOfRange(buffer, 0, 4), NALU_HEADER);
    }

    /**
     * Returns a {@link VideoDecoder} or null upon failure.
     * @param width The width of the video in pixels
     * @param height The height of the video in pixels
     * @param sps The SPS buffer with a NALU header present.
     * @param pps The PPS buffer with a NALU header present.
     * @return {@link VideoDecoder} or null upon failure.
     */
    public static VideoDecoder build(
            int width, int height, byte[] sps, byte[] pps, H264SampleStorage sampleStorage)
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

        return new VideoDecoder(format, sampleStorage);
    }

    private VideoDecoder(MediaFormat format, H264SampleStorage sampleStorage) {
        super(format, MIME, sampleStorage);
    }

    /**
     * Sets the surface to decode the video onto. This needs to be set before
     * calling {@link VideoDecoder#start()}.
     * @param surface The surface to decode the video onto.
     */
    public void setSurface(Surface surface)
    {
        mSurface = surface;
    }

    @Override
    public void start() {
        if (mSurface == null)
            throw new AssertionError();
        super.start();
    }

    @Override
    protected void render(MediaCodec decoder, int outIndex) {
        decoder.releaseOutputBuffer(outIndex, true);
    }
}
