petro-android
=============

.. image:: https://img.shields.io/badge/API-8%2B-brightgreen.svg?style=flat
    :target: https://android-arsenal.com/api?level=8

.. image:: https://jitpack.io/v/steinwurf/petro-android.svg?style=flat-square
    :target: https://jitpack.io/#steinwurf/petro-android

This repository contains a library and an application that demonstrates
different approaches for audio and video playback with the Android
`MediaCodec API <https://developer.android.com/reference/android/media/MediaCodec.html>`_.

Usage
-----
To depend on petro-android you will need add the jitpack.io repository in the
project build.gradle file:

.. code-block::

    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
            // ...
        }
    }

And then add the petro-android dependency to your module's dependencies scope:


.. code-block::

    dependencies {
        implementation 'com.github.steinwurf:petro-android:9.0.0'
    }

Activities
----------
Each Activity implements a different solution, either using the Android
MediaExtractor component or Steinwurf's `petro library <https://github.com/steinwurf/petro>`_
to extract AAC and H264 samples from a local mp4 file.

* MainActivity: A frontend for choosing a media file and the actual playback
  Activity.
* VideoExtractorActivity: The video samples are extracted with the Android
  MediaExtractor and played with a MediaCodec decoder.
* VideoActivity: The H264 video samples are extracted with the petro library
  and played with a MediaCodec decoder.
* AudioExtractorActivity: The audio samples are extracted with the Android
  MediaExtractor and played with a MediaCodec decoder and an AudioTrack object.
* AudioActivity: The AAC video samples are extracted with the petro library
  and played with a MediaCodec decoder and an AudioTrack object.
* BothActivity: A combination of VideoActivity and AudioActivity, where the
  petro library is used to extract H264 and AAC samples in parallel. Two
  separate decoders are used to play the video and audio.

This demo application extracts sample data from a local file, but the same
data can be received from a network source. Therefore the code can be used
as a basis for a streaming application.



Building
--------

First, the native C++ libraries should be configured
and built with waf. This should be done outside Android Studio.

See our detailed description for configuring and using the Android SDK and the
standalone toolchain: http://docs.steinwurf.com/cross_compile.html#android

If you already set up your ``PATH`` to point the SDK and toolchain binaries,
then you can configure this project with a simple command::

    python waf configure --cxx_mkspec=cxx_android_gxx49_armv7

After the configure step, you can build the C++ library::

    python waf build

If the compilation with waf is successful, then you can open the project in
Android Studio and you can build and deploy it like a normal Android
application.

Note that the ``app/build.gradle`` file contains a pre-build step that will run
``python waf build`` before each build within Android Studio to make sure that
the native library is always up-to-date.

If you clean (or rebuild) the project in Android Studio, then you have to
repeat the waf configure step, because the build folder is removed when the
project is cleaned.
