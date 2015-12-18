// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <cstdint>
#include <vector>

namespace petro_android
{
    struct media_interface
    {
        virtual std::vector<char> next_sample() = 0;
        virtual uint32_t sample_time() = 0;
    };
}
