// Copyright Steinwurf ApS 2015.
// All Rights Reserved

#pragma once

#include <algorithm>
#include <vector>
#include <cstdint>
#include <memory>
#include <string>

#include <petro/byte_stream.hpp>
#include <petro/box/all.hpp>
#include <petro/parser.hpp>
#include <petro/presentation_time.hpp>

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
                        petro::box::mdia<petro::parser<
                            petro::box::hdlr,
                            petro::box::mdhd,
                            petro::box::minf<petro::parser<
                                petro::box::stbl<petro::parser<
                                    petro::box::stsd,
                                    petro::box::stsc,
                                    petro::box::stco,
                                    petro::box::co64,
                                    petro::box::ctts,
                                    petro::box::stts,
                                    petro::box::stsz
                                >>
                            >>
                        >>
                    >>
                >>
            > parser;

            auto root = std::make_shared<petro::box::root>();

            parser.read(root, bs);

            auto avcc = root->get_child<petro::box::avcc>();
            assert(avcc != nullptr);

            m_trak = avcc->get_parent("trak");
            assert(m_trak != nullptr);

            auto stco = m_trak->get_child<petro::box::stco>();
            if (stco != nullptr)
            {
                m_chunk_offsets.resize(stco->entry_count());
                std::copy(
                    stco->entries().begin(),
                    stco->entries().end(),
                    m_chunk_offsets.begin());
            }
            else
            {
                auto co64 = m_trak->get_child<petro::box::co64>();
                assert(co64 != nullptr);
                m_chunk_offsets.resize(co64->entry_count());
                std::copy(
                    co64->entries().begin(),
                    co64->entries().end(),
                    m_chunk_offsets.begin());
            }

            auto sps = avcc->sequence_parameter_set(0);
            auto pps = avcc->picture_parameter_set(0);

            m_sps = {0, 0, 0, 1};
            std::copy(sps->data(), sps->data() + sps->size(),
                      std::back_inserter(m_sps));
            m_pps = {0, 0, 0, 1};
            std::copy(pps->data(), pps->data() + pps->size(),
                      std::back_inserter(m_pps));

            m_video_width = sps->width();
            m_video_height = sps->height();
        }

        bool advance()
        {
            auto stsz = m_trak->get_child<petro::box::stsz>();
            assert(stsz != nullptr);

            if (m_next_sample >= stsz->sample_count())
                return false;

            auto stts = m_trak->get_child<petro::box::stts>();
            assert(stts != nullptr);

            auto mdhd = m_trak->get_child<petro::box::mdhd>();
            assert(mdhd != nullptr);

            auto ctts = m_trak->get_child<petro::box::ctts>();

            m_presentation_time = petro::presentation_time(
                stts, ctts, mdhd->timescale(), m_next_sample);

            std::vector<char> start_code = {0, 0, 0, 1};

            auto stsc = m_trak->get_child<petro::box::stsc>();
            assert(stsc != nullptr);

            std::ifstream mp4_file(m_file, std::ios::binary);
            uint32_t found_samples = 0;
            m_sample.clear();
            for (uint32_t i = 0; i < m_chunk_offsets.size(); ++i)
            {
                auto samples_for_chunk = stsc->samples_for_chunk(i);
                if (found_samples + samples_for_chunk > m_next_sample)
                {
                    auto offset = m_chunk_offsets[i];
                    for (uint32_t j = 0; j < stsc->samples_for_chunk(i); ++j)
                    {
                        uint32_t sample_size = stsz->sample_size(found_samples);

                        if (found_samples == m_next_sample)
                        {
                            m_next_sample++;
                            mp4_file.seekg(offset);
                            m_sample.resize(sample_size);

                            // Read multiple NALUs from the Access Unit (AU)
                            // and replace the AVCC headers with the start code
                            uint32_t nalu_offset = 0;
                            while (nalu_offset < sample_size)
                            {
                                uint32_t nalu_size = read_nalu_size(mp4_file);
                                std::copy_n(start_code.begin(), 4,
                                            &m_sample[nalu_offset]);
                                nalu_offset += sizeof(uint32_t);
                                mp4_file.read(
                                    &m_sample[nalu_offset], nalu_size);
                                nalu_offset += nalu_size;
                            }
                            break;
                        }
                        offset += sample_size;
                        found_samples += 1;
                    }
                    break;
                }
                else
                {
                    found_samples += samples_for_chunk;
                }
            }

            return true;
        }

        std::vector<char> sample() const
        {
            return m_sample;
        }

        uint32_t presentation_time() const
        {
            return m_presentation_time;
        }

        double width()
        {
            return m_video_width;
        }

        double height()
        {
            return m_video_height;
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

        uint32_t read_nalu_size(std::istream& file)
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
        uint32_t m_video_width;
        uint32_t m_video_height;
        std::shared_ptr<const petro::box::box> m_trak;
        std::vector<char> m_sample;
        std::vector<char> m_sps;
        std::vector<char> m_pps;
        std::vector<uint64_t> m_chunk_offsets;
        uint32_t m_presentation_time;
    };
}
