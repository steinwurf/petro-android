package com.steinwurf.petro;

/**
 * Track extractor for extracting the tracks of an MP4 file.
 */
public class TrackExtractor {
    /**
     * The type of the track.
     */
    enum TrackType
    {
        UNKNOWN,
        UNKNOWN_AUDIO,
        TEXT,
        AAC,
        AVC1,
        HVC1
    }

    /**
     * The track.
     */
    public static class Track
    {
        public final int id;
        public final TrackType type;

        public Track(int id, TrackType type) {
            this.id = id;
            this.type = type;
        }
    }

    static
    {
        System.loadLibrary("track_extractor_jni");
    }

    /**
     * A long representing a pointer to the underlying native object.
     */
    public final long pointer;

    /**
     * Construct TrackExtractor object.
     */
    TrackExtractor()
    {
        this.pointer = init();
    }

    /**
     * Construct a native TrackExtractor and returns a long value which represents
     * the pointer of the created object.
     */
    private static native long init();


    /**
     * Open the TrackExtractor
     * @param filePath the path of the file.
     * @throws UnableToOpenException if the extractor was unable to open.
     */
    public native void open(String filePath) throws UnableToOpenException;

    public native Track[] getTracks();

    /**
     * Close the TrackExtractor.
     */
    public native void close();

    /**
     * Finalizes the object and it's underlying native part.
     */
    @Override
    protected void finalize() throws Throwable
    {
        finalize(pointer);
        super.finalize();
    }

    /**
     * Finalizes the underlying native part.
     * @param pointer A long representing a pointer to the underlying native object.
     */
    private native void finalize(long pointer);
}
