package com.steinwurf.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoDecoder extends Decoder {

    private static final String TAG = "VideoDecoder";
    private static final String MIME = "video/avc";

    /**
     * A buffer containing a NALU header
     */
    public static final byte[] NALU_HEADER = new byte[]{0x00, 0x00, 0x00, 0x01};

    private static boolean isMissingNALUHeader(byte[] buffer)
    {
        return !Arrays.equals(Arrays.copyOfRange(buffer, 0, 4), NALU_HEADER);
    }

    private static class H264HeaderCheckerWrapper implements SampleProvider
    {
        final SampleProvider sampleProvider;

        H264HeaderCheckerWrapper(SampleProvider sampleProvider)
        {
            this.sampleProvider = sampleProvider;
        }

        @Override
        public boolean hasSample() {
            return sampleProvider.hasSample();
        }

        @Override
        public Sample getSample() {
            Sample sample = sampleProvider.getSample();
            if (isMissingNALUHeader(sample.data)) {
                Log.e(TAG, "No NALU header before sample");
            }
            return sample;
        }
    }

    /**
     * Returns a {@link VideoDecoder} or null upon failure.
     * @param width The width of the video in pixels
     * @param height The height of the video in pixels
     * @param sps The SPS buffer with a NALU header present.
     * @param pps The PPS buffer with a NALU header present.
     * @param sampleProvider The sample provider
     * @return {@link VideoDecoder} or null upon failure.
     */
    public static VideoDecoder build(
            int width, int height, byte[] sps, byte[] pps, SampleProvider sampleProvider)
    {
        if (isMissingNALUHeader(sps)) {
            Log.e(TAG, "No header before SPS");
            return null;
        }
        if (isMissingNALUHeader(pps)) {
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

        return new VideoDecoder(format, sampleProvider);
    }

    private VideoDecoder(MediaFormat format, SampleProvider sampleProvider) {
        super(format, MIME, new H264HeaderCheckerWrapper(sampleProvider));
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


    /**
     * Starts the playback. Call this after {@link VideoDecoder#setSurface(Surface)} returns.
     */
    @Override
    public void start() throws IOException {
        if (mSurface == null)
            throw new IllegalStateException();
        super.start();
    }

    @Override
    protected void render(MediaCodec decoder, MediaCodec.BufferInfo info, int outIndex) {
        decoder.releaseOutputBuffer(outIndex, true);
    }
}
