// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <cstdint>
#include <vector>

namespace petro_android
{
    struct media_interface
    {
        virtual bool advance() = 0;
        virtual std::vector<char> sample() const = 0;
        virtual uint32_t presentation_time() const = 0;
    };
}
