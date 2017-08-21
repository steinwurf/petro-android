package com.steinwurf.mediaextractor;

@SuppressWarnings("JniMissingFunction")
public class SequenceParameterSet {

    static
    {
        System.loadLibrary("sequence_parameter_set_jni");
    }

    public static native SequenceParameterSet parse(byte[] buffer);

    /**
     * A long representing a pointer to the underlying native object.
     */
    public final long pointer;

    /**
     * Construct SPS object.
     */
    protected SequenceParameterSet(long pointer)
    {
        this.pointer = pointer;
    }

    /**
     * Returns the video width in pixels
     * @return the video width in pixels
     */
    public native int getVideoWidth();

    /**
     * Returns the video height in pixels
     * @return the video height in pixels
     */
    public native int getVideoHeight();

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
