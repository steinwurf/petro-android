package com.steinwurf.mediaplayer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SampleStorage
{
    private static final String TAG = "SampleStorage";

    protected class Sample
    {
        final long timestamp;
        final byte[] data;

        public Sample(long timestamp, byte[] data)
        {
            this.timestamp = timestamp;
            this.data = data;
        }
    }

    private final List<Sample> samples = Collections.synchronizedList(new LinkedList<Sample>());

    public final long offset;

    private int mDelay = 0;

    public SampleStorage(long offset)
    {
        this.offset = offset;
    }

    public int getDelay()
    {
        return mDelay / 1000;
    }
    public void setDelay(int delay)
    {
        mDelay = delay * 1000;
    }

    public void addSample(long timestamp, byte[] data)
    {
        timestamp -= offset;
        // The samples list is synchronized, so it can be accessed from multiple threads
        samples.add(new Sample(timestamp + mDelay, data.clone()));
    }

    public int getCount()
    {
        return samples.size();
    }

    Sample getNextSample()
    {
        Sample sample = samples.get(0);
        samples.remove(0);
        return sample;
    }
}
