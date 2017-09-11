package com.steinwurf.mediaplayer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SampleStorage implements SampleProvider
{
    private static final String TAG = "SampleStorage";

    private final List<Sample> samples = new LinkedList<>();

    /**
     * Adds a sample to the storage (this operation is synchronized)
     * @param timestamp timestamp in microseconds
     * @param data data buffer
     */
    public synchronized void addSample(long timestamp, byte[] data)
    {
        // The samples list is synchronized, so it can be accessed from multiple threads
        samples.add(new Sample(timestamp, data.clone()));
        Sample sample;
    }

    /**
     * Returns the number of samples in the storage
     * @return the number of samples in the storage
     */
    public synchronized long sampleCount()
    {
        return samples.size();
    }

    /**
     * Returns true if the sample provider has a sample available
     * @return true if the sample provider has a sample available
     */
    @Override
    public synchronized boolean hasSample()
    {
        return samples.size() != 0;
    }

    /**
     * Returns the next {@link Sample}
     * @return the next {@link Sample}
     * @throws IndexOutOfBoundsException if no more samples are available.
     */
    @Override
    public Sample getSample() throws IndexOutOfBoundsException
    {
        Sample sample;
        synchronized (this) {
            sample = samples.get(0);
            samples.remove(0);
        }
        return sample;
    }
}
