package com.steinwurf.mediaplayer;

import org.junit.Test;

import static org.junit.Assert.*;

public class SampleUnitTest {
    @Test
    public void constructor_isCorrect() throws Exception {

        long timestamp = 1337;
        byte [] data = new byte[]{0x01, 0x03, 0x03, 0x07};
        Sample sample = new Sample(timestamp, data);

        assertEquals(timestamp, sample.timestamp);
        assertEquals(data, sample.data);
    }
}
