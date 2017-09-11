package com.steinwurf.mediaextractor;

@SuppressWarnings("JniMissingFunction")
public class NALUExtractor extends Extractor {

    static
    {
        System.loadLibrary("nalu_extractor_jni");
    }

    /**
     * Construct a native Extractor and returns a long value which represents
     * the pointer of the created object.
     */
    private static native long init();

    /**
     * Constructs an NALUExtractor
     */
    public NALUExtractor()
    {
        super(init());
    }

    /**
     * Returns true if this NALU sample is the beginning of the AVCSample
     * @return true if this NALU sample is the beginning of the AVCSample
     */
    public native boolean isBeginningOfAVCSample();

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

    public native byte[] getNalu();

    @Override
    public native void setFilePath(String filePath);

    @Override
    public native String getFilePath();

    @Override
    public native void open() throws UnableToOpenException;

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
