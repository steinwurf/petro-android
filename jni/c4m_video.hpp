// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <vector>
#include <cstdint>
#include <memory>
#include <string>

#include <sak/convert_endian.hpp>

#include "video_interface.hpp"

namespace
{
    template<class T>
    T read_from_file(std::fstream& file)
    {
        static char buffer[sizeof(T)];

        file.read(buffer, sizeof(T));

        return sak::big_endian::get<T>((const uint8_t*)buffer);
    }

    void read_from_file(std::fstream& file, char* data, uint32_t size)
    {
        file.read(data, size);
    }

}


namespace petro_android
{
    class c4m_video : public video_interface
    {

    public:

        c4m_video(const std::string& file)
        {
            m_file.open(file, std::ios::in | std::ios::binary);

            m_width = read_from_file<uint32_t>(m_file);
            m_height = read_from_file<uint32_t>(m_file);

            // Read SPS and PPS, both are preceeded by a 64bit timestamp so
            // we just discard that.
            read_from_file<uint64_t>(m_file);

            uint32_t sps_size = read_from_file<uint32_t>(m_file);

            assert(m_sps.size() == 4);
            m_sps.resize(4 + sps_size);
            read_from_file(m_file, m_sps.data() + 4, sps_size);

            read_from_file<uint64_t>(m_file);

            uint32_t pps_size = read_from_file<uint32_t>(m_file);

            assert(m_pps.size() == 4);
            m_pps.resize(4 + pps_size);
            read_from_file(m_file, m_pps.data() + 4, pps_size);

        }

        std::vector<char> next_sample()
        {
            uint64_t timestamp = read_from_file<uint64_t>(m_file);
            m_sample_time = timestamp - m_previous_timetamp;
            m_previous_timetamp = timestamp;

            std::vector<char> sample = {0, 0, 0, 1};

            uint32_t sample_size = read_from_file<uint32_t>(m_file);

            sample.resize(4 + sample_size);
            read_from_file(m_file, sample.data() + 4, sample_size);

            return sample;
        }

        uint32_t sample_time()
        {
            return m_sample_time;
        }

        double width()
        {
            return m_width;
        }

        double height()
        {
            return m_height;
        }

        std::vector<char> pps()
        {
            return m_pps;
        }

        std::vector<char> sps()
        {
            return m_sps;
        }

    private:

        std::fstream m_file;

        double m_width = 0;
        double m_height = 0;

        std::vector<char> m_pps = {0, 0, 0, 1};
        std::vector<char> m_sps = {0, 0, 0, 1};

        uint32_t m_sample_time = 0;
        uint64_t m_previous_timetamp = 0;
    };
}
