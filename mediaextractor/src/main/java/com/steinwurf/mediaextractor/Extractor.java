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
     * @param pointer long representing a pointer to the underlying native object.
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
     * Returns the file path.
     * @return the file path.
     */
    public abstract String getFilePath();

    /**
     * Open the Extractor
     * @throws UnableToOpenException if the extractor was unable to open.
     */
    public abstract void open() throws UnableToOpenException;

    /**
     * Reset the state of the extractor
     */
    public abstract void reset();

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
     * Close the Extractor.
     */
    public abstract void close();
}

