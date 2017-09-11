package com.steinwurf.mediaplayer;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SampleStorageUnitTest {

    @Test
    public void sampleStorage_api() throws Exception {

        long timestamp1 = 1337;
        byte [] data1 = new byte[]{1, 3, 3, 7};

        long timestamp2 = 42;
        byte [] data2 = new byte[]{ 4, 2, };

        SampleStorage sampleStorage = new SampleStorage();
        assertFalse(sampleStorage.hasSample());

        sampleStorage.addSample(timestamp1, data1);
        assertTrue(sampleStorage.hasSample());
        assertEquals(1, sampleStorage.sampleCount());

        sampleStorage.addSample(timestamp2, data2);
        assertTrue(sampleStorage.hasSample());
        assertEquals(4, sampleStorage.sampleCount());

        {
            Sample sample1 = sampleStorage.getSample();
            assertEquals(timestamp1, sample1.timestamp);
            assertEquals(data1.length, sample1.data.length);
            assertArrayEquals(data1, sample1.data);
        }

        assertTrue(sampleStorage.hasSample());
        assertEquals(1, sampleStorage.sampleCount());

        {
            Sample sample2 = sampleStorage.getSample();
            assertEquals(timestamp2, sample2.timestamp);
            assertEquals(data2.length, sample2.data.length);
            assertArrayEquals(data2, sample2.data);
        }

        assertFalse(sampleStorage.hasSample());
        assertEquals(0, sampleStorage.sampleCount());
    }
}
