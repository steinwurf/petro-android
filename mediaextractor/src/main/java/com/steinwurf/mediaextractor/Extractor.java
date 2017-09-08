package com.steinwurf.mediaextractor;

@SuppressWarnings("JniMissingFunction")
public abstract class Extractor
{
    public static class UnableToOpenException extends Exception {
        public UnableToOpenException (String message) {
            super(message);
        }
    }

    /**
     * A long representing a pointer to the underlying native object.
     */
    public final long pointer;

    /**
     * Construct Extractor object.
     */
    protected Extractor(long pointer)
    {
        this.pointer = pointer;
    }

    /**
     * Sets the file path of the fíle to open.
     * @param filePath the path of the file.
     */
    public abstract void setFilePath(String filePath);

    /**
     * Returns the set file path.
     */
    public abstract String getFilePath();

    /**
     * Open the Extractor.
     */
    public abstract void open() throws UnableToOpenException;

    /**
     * Reset the state of the extractor
     */
    public abstract void reset();

    /**
     * Return the decoding timestamp related to the current sample
     */
    public abstract long getDecodingTimestamp();

    /**
     * Return the presentation timestamp related to the current sample
     */
    public abstract long getPresentationTimestamp();

    /**
     * Return the current sample index
     */
    public abstract long getSampleIndex();

    /**
     * Return the number of samples
     */
    public abstract long getSampleCount();

    /**
     * Return the total media duration in microseconds
     */
    public abstract long getDuration();

    /**
     * Return true if no more sample are available.
     */
    public abstract boolean atEnd();

    /**
     * Advance to next sample
     */
    public abstract void advance();

    /**
     * Close the Extractor.
     */
    public abstract void close();
}

