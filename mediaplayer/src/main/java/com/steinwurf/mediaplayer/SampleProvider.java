package com.steinwurf.mediaplayer;

public interface SampleProvider {

    long getCount();

    Sample getNextSample();
}
