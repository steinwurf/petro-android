// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <vector>
#include <cstdint>
#include <memory>
#include <string>

#include <petro/byte_stream.hpp>
#include <petro/box/all.hpp>
#include <petro/parser.hpp>

#include "video_interface.hpp"

namespace petro_android
{
    class mp4_video : public video_interface
    {

    public:
        mp4_video(const std::string& file):
            m_file(file),
            m_next_sample(0)
        {
            petro::byte_stream bs(m_file);

            petro::parser<
                petro::box::moov<petro::parser<
                    petro::box::trak<petro::parser<
                        petro::box::tkhd,
                        petro::box::mdia<petro::parser<
                            petro::box::hdlr,
                            petro::box::minf<petro::parser<
                                petro::box::stbl<petro::parser<
                                    petro::box::stco,
                                    petro::box::stsc,
                                    petro::box::stsd,
                                    petro::box::stsz
                                >>
                            >>
                        >>
                    >>
                >>
            > parser;

            auto root = std::make_shared<petro::box::root>();

            parser.read(root, bs);
            m_avcc = std::dynamic_pointer_cast<const petro::box::avcc>(
                root->get_child("avcC"));
            assert(m_avcc != nullptr);

            m_trak = m_avcc->get_parent("trak");
            assert(m_trak != nullptr);
        }

        std::vector<char> next_sample()
        {
            std::vector<char> nalu_seperator = {0, 0, 0, 1};

            auto stco = std::dynamic_pointer_cast<const petro::box::stco>(
                m_trak->get_child("stco"));
            assert(stco != nullptr);

            auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
                m_trak->get_child("stsz"));
            assert(stsz != nullptr);

            auto stsc = std::dynamic_pointer_cast<const petro::box::stsc>(
                m_trak->get_child("stsc"));
            assert(stsc != nullptr);


            std::ifstream mp4_file(m_file, std::ios::binary);
            std::vector<char> sample;
            uint32_t found_samples = 0;
            for (uint32_t i = 0; i < stco->entry_count(); ++i)
            {
                auto samples_for_chunk = stsc->samples_for_chunk(i);
                if (found_samples + samples_for_chunk > (uint32_t)m_next_sample)
                {
                    auto offset = stco->chunk_offset(i);
                    for (uint32_t j = 0; j < stsc->samples_for_chunk(i); ++j)
                    {
                        if (found_samples == m_next_sample)
                        {
                            m_next_sample++;
                            sample.insert(
                                sample.begin(),
                                nalu_seperator.begin(),
                                nalu_seperator.end());

                            mp4_file.seekg(offset);
                            auto sample_size = read_sample_size(mp4_file);

                            std::vector<char> temp(sample_size);

                            mp4_file.read(temp.data(), sample_size);

                            sample.insert(
                                sample.end(),
                                temp.data(),
                                temp.data() + (sample_size + nalu_seperator.size()));
                            break;
                        }
                        offset += stsz->sample_size(found_samples);
                        found_samples += 1;
                    }
                    break;
                }
                else
                {
                    found_samples += samples_for_chunk;
                }
            }
            return sample;
        }

        uint32_t sample_time()
        {
            auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
                m_trak->get_child("stsz"));
            assert(stsz != nullptr);

            auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
                m_trak->get_child("tkhd"));
            assert(tkhd != nullptr);

            return tkhd->duration() / stsz->sample_count();
        }

        double width()
        {
            auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
                m_trak->get_child("tkhd"));
            assert(tkhd != nullptr);

            return tkhd->width();
        }

        double height()
        {
            auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
                m_trak->get_child("tkhd"));
            assert(tkhd != nullptr);

            return tkhd->height();
        }

        std::vector<char> pps()
        {
            std::vector<char> pps_buffer = {0, 0, 0, 1};

            auto pps = m_avcc->picture_parameter_set(0);
            pps_buffer.insert(pps_buffer.end(), pps.begin(), pps.end());
            return pps_buffer;
        }

        std::vector<char> sps()
        {
            std::vector<char> sps_buffer = {0, 0, 0, 1};

            auto sps = m_avcc->sequence_parameter_set(0);
            sps_buffer.insert(sps_buffer.end(), sps.begin(), sps.end());
            return sps_buffer;
        }

    private:

        uint32_t read_sample_size(std::istream& file)
        {
            std::vector<uint8_t> data(4);
            file.read((char*)data.data(), data.size());

            uint32_t result =
               (uint32_t) data[0] << 24 |
               (uint32_t) data[1] << 16 |
               (uint32_t) data[2] << 8  |
               (uint32_t) data[3];
            return result;
        }

    private:

        std::string m_file;
        uint32_t m_next_sample;
        std::shared_ptr<const petro::box::avcc> m_avcc;
        std::shared_ptr<const petro::box::box> m_trak;
    };
}
