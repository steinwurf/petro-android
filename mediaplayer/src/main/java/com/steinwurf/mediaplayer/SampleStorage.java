package com.steinwurf.mediaplayer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SampleStorage
{
    private static final String TAG = "SampleStorage";

    protected class Sample
    {
        /**
         * The timestamp in microseconds
         */
        final long timestamp;

        /**
         * The data buffer
         */
        final byte[] data;

        /**
         * Constructs a sample
         * @param timestamp timestamp in microseconds
         * @param data data buffer
         */
        Sample(long timestamp, byte[] data)
        {
            this.timestamp = timestamp;
            this.data = data;
        }
    }

    private final List<Sample> samples = Collections.synchronizedList(new LinkedList<Sample>());

    /**
     * Offset in microseconds
     */
    public final long offset;

    /**
     * Delay in microseconds
     */
    private long mDelay = 0;

    /**
     * Creates a sample storage
     * @param offset timestamp offset in microseconds
     */
    public SampleStorage(long offset)
    {
        this.offset = offset;
    }

    /**
     * Gets the set delay in microseconds
     * @return delay in microseconds
     */
    public long getDelay()
    {
        return mDelay;
    }

    /**
     * Sets the delay in microseconds
     * @param delay delay in microseconds
     */
    public void setDelay(long delay)
    {
        mDelay = delay;
    }

    /**
     * Adds a sample to the storage (this operation is synchronized)
     * @param timestamp timestamp in microseconds
     * @param data data buffer
     */
    public void addSample(long timestamp, byte[] data)
    {
        timestamp -= offset;
        // The samples list is synchronized, so it can be accessed from multiple threads
        samples.add(new Sample(timestamp + mDelay, data.clone()));
    }

    /**
     * Returns the number of samples in the storage
     * @return the number of samples in the storage
     */
    public int getCount()
    {
        return samples.size();
    }

    /**
     * Returns the next {@link Sample}
     * @return the next {@link Sample}
     * @throws IndexOutOfBoundsException if count < 1.
     */
    Sample getNextSample() throws IndexOutOfBoundsException
    {
        Sample sample = samples.get(0);
        samples.remove(0);
        return sample;
    }
}
