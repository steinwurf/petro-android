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

#include "audio_interface.hpp"

namespace petro_android
{
    class mp4_audio : public audio_interface
    {

    public:
        mp4_audio(const std::string& file):
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

            auto mp4a = root->get_child("mp4a");
            assert(mp4a != nullptr);

            auto esds = std::dynamic_pointer_cast<const petro::box::esds>(
                mp4a->get_child("esds"));
            m_descriptor = esds->descriptor()->decoder_config_descriptor();

            m_trak = mp4a->get_parent("trak");
            assert(m_trak != nullptr);

            auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
                m_trak->get_child("stsz"));
            assert(stsz != nullptr);

            auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
                m_trak->get_child("tkhd"));
            assert(tkhd != nullptr);

            m_sample_time =
                tkhd->duration() * 1000 / stsz->sample_count();
        }

        bool advance()
        {

            auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
                m_trak->get_child("stsz"));
            assert(stsz != nullptr);


            if (m_next_sample > stsz->sample_count())
                return false;


            /// @todo fix this to always return the same as android
            ///       MediaExtractor?
            m_presentation_time = m_sample_time * m_next_sample;

            auto stco = std::dynamic_pointer_cast<const petro::box::stco>(
                m_trak->get_child("stco"));
            assert(stco != nullptr);

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
                        uint16_t sample_size = stsz->sample_size(found_samples);
                        if (found_samples == m_next_sample)
                        {
                            m_next_sample++;
                            mp4_file.seekg(offset);

                            std::vector<char> temp(sample_size);
                            mp4_file.read(temp.data(), sample_size);

                            sample.insert(sample.end(), temp.begin(), temp.end());
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
            m_sample = sample;

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

        uint32_t sample_rate_index()
        {
            return m_descriptor->frequency_index();
        }

        uint32_t channel_config()
        {
            return m_descriptor->channel_configuration();
        }

        uint32_t codec_profile_level()
        {
            return m_descriptor->mpeg_audio_object_type();
        }

    private:

        std::string m_file;
        uint32_t m_next_sample;
        std::shared_ptr<const petro::descriptor::decoder_config_descriptor>
            m_descriptor;
        std::shared_ptr<const petro::box::box> m_trak;
        std::vector<char> m_sample;
        uint32_t m_presentation_time;
        uint32_t m_sample_time;
    };
}
