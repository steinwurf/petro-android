package com.steinwurf.petro;

@SuppressWarnings("JniMissingFunction")
public abstract class SampleExtractor
{
    /**
     * A long representing a pointer to the underlying native object.
     */
    public final long pointer;

    /**
     * Construct SampleExtractor object.
     * @param pointer long representing a pointer to the underlying native object.
     */
    protected SampleExtractor(long pointer)
    {
        this.pointer = pointer;
    }

    /**
     * Open the SampleExtractor
     * @param filePath the path of the file.
     * @param trackId the track Id to use for the extraction
     * @throws UnableToOpenException if the extractor was unable to open.
     */
    public abstract void open(String filePath, int trackId) throws UnableToOpenException;

    /**
     * Get the set track ID
     * @return the set track ID
     */
    public abstract long getTrackId();

    /**
     * Close the SampleExtractor.
     */
    public abstract void close();

    /**
     * Reset the state of the extractor
     */
    public abstract void reset();

    /**
     * Get a sample at the current position
     * @return sample at current position
     */
    public native byte[] getSample();


    /**
     * Returns the decoding timestamp related to the current sample in microseconds
     * @return the decoding timestamp related to the current sample in microseconds
     */
    public abstract long getDecodingTimestamp();

    /**
     * Returns the presentation timestamp related to the current sample in microseconds
     * @return the presentation timestamp related to the current sample in microseconds
     */
    public abstract long getPresentationTimestamp();

    /**
     * Returns the current sample index
     * @return the current sample index
     */
    public abstract long getSampleIndex();

    /**
     * Returns the number of samples
     * @return the number of samples
     */
    public abstract long getSampleCount();

    /**
     * Returns the total media duration in microseconds
     * @return the total media duration in microseconds
     */
    public abstract long getDuration();

    /**
     * Returns true if no more sample are available otherwise false.
     * @return true if no more sample are available otherwise false.
     */
    public abstract boolean atEnd();

    /**
     * Advance to next sample
     */
    public abstract void advance();

    /**
     * Set looping enabled or disabled
     * @param enabled if true, looping will be enabled, otherwise not.
     */
    public abstract void setLoopingEnabled(boolean enabled);

    /**
     * Get the number of times the extractor has looped.
     * @return the number of times the extractor has looped.
     */
    public abstract int getLoopCount();
}
