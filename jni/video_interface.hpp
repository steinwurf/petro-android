// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <vector>

#include "media_interface.hpp"

namespace petro_android
{
    struct video_interface : media_interface
    {
        virtual double width() = 0;
        virtual double height() = 0;
        virtual std::vector<char> pps() = 0;
        virtual std::vector<char> sps() = 0;
    };
}