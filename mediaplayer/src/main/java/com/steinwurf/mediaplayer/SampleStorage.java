package com.steinwurf.mediaplayer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SampleStorage implements SampleProvider
{
    private static final String TAG = "SampleStorage";

    private final List<Sample> samples = Collections.synchronizedList(new LinkedList<Sample>());

    /**
     * Delay in microseconds
     */
    private long mDelay = 0;

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
        // The samples list is synchronized, so it can be accessed from multiple threads
        samples.add(new Sample(timestamp + mDelay, data.clone()));
    }

    /**
     * Returns the number of samples in the storage
     * @return the number of samples in the storage
     */
    @Override
    public boolean hasSample()
    {
        return samples.size() != 0;
    }

    /**
     * Returns the next {@link Sample}
     * @return the next {@link Sample}
     * @throws IndexOutOfBoundsException if count < 1.
     */
    @Override
    public Sample getSample() throws IndexOutOfBoundsException
    {
        Sample sample = samples.get(0);
        samples.remove(0);
        return sample;
    }
}
