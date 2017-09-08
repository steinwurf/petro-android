package com.steinwurf.mediaplayer;

public class Sample
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
    public Sample(long timestamp, byte[] data)
    {
        this.timestamp = timestamp;
        this.data = data;
    }
}