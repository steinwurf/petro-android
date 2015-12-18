// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <cstdint>

#include "media_interface.hpp"

namespace petro_android
{
    /// info: http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
    struct audio_interface : media_interface
    {
        /// There are 13 supported frequencies:
        /// 0 : 96000 Hz
        /// 1 : 88200 Hz
        /// 2 : 64000 Hz
        /// 3 : 48000 Hz
        /// 4 : 44100 Hz
        /// 5 : 32000 Hz
        /// 6 : 24000 Hz
        /// 7 : 22050 Hz
        /// 8 : 16000 Hz
        /// 9 : 12000 Hz
        /// 10: 11025 Hz
        /// 11:  8000 Hz
        /// 12:  7350 Hz
        virtual uint32_t sample_rate_index() = 0;

        /// There are 7 channel configurations:
        /// 0: Defined in AOT Specifc Config
        /// 1: 1 channel
        /// 2: 2 channels
        /// 3: 3 channels
        /// 4: 4 channels
        /// 5: 5 channels
        /// 6: 6 channels
        /// 7: 8 channels
        virtual uint32_t channel_config() = 0;
        virtual uint32_t codec_profile_level() = 0;
    };
}
