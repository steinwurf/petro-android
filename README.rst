petro-android
=============

This repository is used for experimenting with android and petro
(our mp4 parsing library).

The Android MediaCodec is capable of h264 playback, but only if fed with one
sample at a time.
We use petro to extract these samples.
It's the intention that the samples shall be received using some kind of
streaming application.

Findings
--------
No crash if given nulled data.
No crash if given nulled data doesn't match the expected size.

No crash but neither any picture if nulled key-frame is given (Nexus 4 showed)
some picture after running for a little while.

The first part of the sample is the size of the sample (you can get the size
of the sample size by looking at the avcC box - it's usually 4), if this is not
removed the MediaCodec crashes.

The MediaCodec crashes if the right (wrong?) kind of random data is given
instead of the actual sample.
