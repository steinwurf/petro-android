News for petro-android
======================

This file lists the major changes between versions. For a more detailed list of
every change, see the Git log.

Latest
------
* Major: Upgrade to petro 11.
* Minor: Use gradle 4.4.
* Major: Move arm and armv7 libraries into armeabi-v7a jni folder.

7.0.0
-----
* Major: Moved mediaplayer to https://github.com/steinwurf/mediaplayer-android
* Major: Use androidGitVersion for setting the version of the apps and
  libraries automatically.

6.0.0
-----
* Major: Make sure exceptions can be catched when starting a ``****oDecoder``.

5.0.0
-----
* Major: Deploy to artifactory.

4.0.0
-----
* Major: Changed `mediaplayer.Utils` API to return custom `Scale` object. Use
  the `Scale` object's `toMatrix` method to get the scaled `Matrix`.
* Minor: Added `sampleCount` to `SampleStorage`.

3.0.0
-----
* Major: Introduced `SampleProvider` to be used instead of `SampleStorage`.
* Minor: Added `SampleCount` to extractor.
* Major: Removed `getSample` from extractor.


2.0.1
-----
* Patch: Fix sound stutter on older android devices.

2.0.0
-----
* Major: Removed offset from sample storage. This functionality must now be
  performed outside of the SampleStorage.

1.0.0
-----
* Major: Upgrade to petro 9
* Major: Upgrade to waf-tools 4
* Major: Upgrade to petro 7
* Major: Upgrade to petro 6
