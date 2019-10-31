package com.steinwurf.petro;

public class AVCSampleExtractor extends SampleExtractor {

    static
    {
        System.loadLibrary("avc_sample_extractor_jni");
    }

    /**
     * Construct a native SampleExtractor and returns a long value which represents
     * the pointer of the created object.
     */
    private static native long init();

    /**
     * Constructs an AACSampleExtractor
     */
    public AVCSampleExtractor()
    {
        super(init());
    }

    /**
     * Returns the pps data without NALU header
     * @return pps data
     */
    public native byte[] getPPS();

    /**
     * Returns the pps data without NALU header
     * @return sps data
     */
    public native byte[] getSPS();

    /**
     * Get the size of the nalu sizes within the samples.
     * @return the size of the nalu sizes within the samples.
     */
    public native int getNALULengthSize();

    @Override
    public native byte[] getSample();

    @Override
    public native void open(String filePath, int trackId) throws UnableToOpenException;

    @Override
    public native long getTrackId();

    @Override
    public native void reset();

    @Override
    public native long getDecodingTimestamp();

    @Override
    public native long getPresentationTimestamp();

    @Override
    public native long getSampleIndex();

    @Override
    public native long getSampleCount();

    @Override
    public native long getDuration();

    @Override
    public native boolean atEnd();

    @Override
    public native void advance();

    @Override
    public native void setLoopingEnabled(boolean enabled);

    @Override
    public native int getLoopCount();

    @Override
    public native void close();

    /**
     * Finalizes the object and it's underlying native part.
     */
    @Override
    protected void finalize() throws Throwable
    {
        finalize(pointer);
        super.finalize();
    }

    /**
     * Finalizes the underlying native part.
     * @param pointer A long representing a pointer to the underlying native object.
     */
    private native void finalize(long pointer);
}
