#ifdef ANDROID

#include <unistd.h>

#include <jni.h>
#include <pthread.h>
#include <thread>
#include <cassert>
#include <fstream>

#include <vector>
#include <cstdlib>
#include <chrono>

#include <petro/byte_stream.hpp>
#include <petro/box/all.hpp>
#include <petro/parser.hpp>

#include "jni_utils.hpp"
#include "logging.hpp"

static JavaVM* java_vm = 0;

static jclass native_interface_class = 0;

static jmethodID on_initialized_method = 0;
static jfieldID native_context_field = 0;

static std::thread main_thread;
static pthread_key_t current_jni_env;

struct context
{
    std::shared_ptr<petro::box::root> root;
    std::string mp4_file;
};

static uint32_t read_sample_size(std::istream& file)
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

// Unregister this thread from the VM
static void detach_current_thread(void* value)
{
    JNIEnv* env = (JNIEnv*) value;
    if (env != NULL)
    {
        LOGI << "Detaching thread";
        java_vm->DetachCurrentThread();
        pthread_setspecific(current_jni_env, NULL);
        LOGI << "Thread detached";
    }
}

static context* get_native_context(JNIEnv* env)
{
    // LOGI << "get_native_context";

    return (context*)
        env->GetStaticLongField(native_interface_class, native_context_field);
}

static void set_native_context(JNIEnv* env, context* context)
{
    LOGI << "set_native_context";

    env->SetStaticLongField(
        native_interface_class, native_context_field, (jlong)context);
}

#ifdef __cplusplus
extern "C"
{
#endif

    void Java_com_steinwurf_petro_NativeInterface_nativeInitialize(
        JNIEnv* env, jobject thiz, jstring jmp4_file)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeInitialize";

        const char* mp4_file_str = env->GetStringUTFChars(jmp4_file, 0);

        auto c = new context();
        c->mp4_file = std::string(mp4_file_str);
        petro::byte_stream bs(mp4_file_str);

        env->ReleaseStringUTFChars(jmp4_file, mp4_file_str);

        petro::parser<
            petro::box::moov<petro::parser<
                petro::box::trak<petro::parser<
                    petro::box::tkhd,
                    petro::box::mdia<petro::parser<
                        petro::box::mdhd,
                        petro::box::hdlr,
                        petro::box::minf<petro::parser<
                            petro::box::stbl<petro::parser<
                                petro::box::stco,
                                petro::box::stsc,
                                petro::box::stsd,
                                petro::box::stts,
                                petro::box::stsz
                            >>
                        >>
                    >>
                >>
            >>
        > parser;

        auto root = std::make_shared<petro::box::root>();

        LOGI << bs.remaining_bytes();
        parser.read(root, bs);

        c->root = root;
        set_native_context(env, c);

        env->CallStaticVoidMethod(native_interface_class, on_initialized_method);
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoPPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getPPS";

        std::vector<char> pps_buffer = {0, 0, 0, 1};

        auto avcc = std::dynamic_pointer_cast<const petro::box::avcc>(
            get_native_context(env)->root->get_child("avcC"));

        assert(avcc != nullptr);

        auto pps = avcc->picture_parameter_set(0);
        pps_buffer.insert(pps_buffer.end(), pps.begin(), pps.end());

        auto jpps = env->NewByteArray(pps_buffer.size());
        env->SetByteArrayRegion(jpps, 0, pps_buffer.size(), (const jbyte*)pps_buffer.data());
        return jpps;
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoSPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getSPS";

        std::vector<char> sps_buffer = {0, 0, 0, 1};

        auto avcc = std::dynamic_pointer_cast<const petro::box::avcc>(
            get_native_context(env)->root->get_child("avcC"));

        assert(avcc != nullptr);

        auto sps = avcc->sequence_parameter_set(0);
        sps_buffer.insert(sps_buffer.end(), sps.begin(), sps.end());

        auto jpps = env->NewByteArray(sps_buffer.size());
        env->SetByteArrayRegion(jpps, 0, sps_buffer.size(), (const jbyte*)sps_buffer.data());
        return jpps;
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoWidth(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto avc1 = get_native_context(env)->root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
            trak->get_child("tkhd"));
        assert(tkhd != nullptr);

        return tkhd->width();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoHeight(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto avc1 = get_native_context(env)->root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
            trak->get_child("tkhd"));
        assert(tkhd != nullptr);

        return tkhd->height();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoSampleCount(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto avc1 = get_native_context(env)->root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        return stsz->sample_count();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoSampleTime(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto avc1 = get_native_context(env)->root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
            trak->get_child("tkhd"));
        assert(tkhd != nullptr);

        return tkhd->duration() / stsz->sample_count();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoSample(
        JNIEnv* env, jobject thiz, jint index)
    {
        (void)thiz;

        std::vector<char> nalu_seperator = {0, 0, 0, 1};

        auto c = get_native_context(env);

        std::ifstream mp4_file(c->mp4_file, std::ios::binary);

        auto root = c->root;

        // don't handle special case with fragmented samples
        assert(root->get_child("mvex") == nullptr);

        auto avc1 = root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto stco = std::dynamic_pointer_cast<const petro::box::stco>(
            trak->get_child("stco"));
        assert(stco != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        auto stsc = std::dynamic_pointer_cast<const petro::box::stsc>(
            trak->get_child("stsc"));
        assert(stsc != nullptr);

        std::vector<char> sample;
        auto found_samples = 0;
        for (uint32_t i = 0; i < stco->entry_count(); ++i)
        {
            auto samples_for_chunk = stsc->samples_for_chunk(i);
            if (found_samples + samples_for_chunk > (uint32_t)index)
            {
                auto offset = stco->chunk_offset(i);
                for (uint32_t j = 0; j < stsc->samples_for_chunk(i); ++j)
                {
                    if (found_samples == index)
                    {
                        sample.insert(sample.begin(), nalu_seperator.begin(), nalu_seperator.end());

                        mp4_file.seekg(offset);
                        auto sample_size = read_sample_size(mp4_file);

                        std::vector<char> temp(sample_size);

                        mp4_file.read(temp.data(), sample_size);

                        sample.insert(sample.end(), temp.data(), temp.data() + (sample_size + nalu_seperator.size()));
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
        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(jsample, 0, sample.size(), (const jbyte*)sample.data());
        return jsample;
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioCodecProfileLevel(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto c = get_native_context(env);
        auto root = c->root;

        auto mp4a = root->get_child("mp4a");
        assert(mp4a != nullptr);

        auto esds = std::dynamic_pointer_cast<const petro::box::esds>(
            mp4a->get_child("esds"));
        assert(esds != nullptr);
        auto decoder_config_descriptor =
            esds->descriptor()->decoder_config_descriptor();

        return decoder_config_descriptor->mpeg_audio_object_type();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleRate(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto c = get_native_context(env);
        auto root = c->root;

        auto mp4a = root->get_child("mp4a");
        assert(mp4a != nullptr);

        auto esds = std::dynamic_pointer_cast<const petro::box::esds>(
            mp4a->get_child("esds"));
        assert(esds != nullptr);
        auto decoder_config_descriptor =
            esds->descriptor()->decoder_config_descriptor();

        return decoder_config_descriptor->frequency_index();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioChannelCount(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto c = get_native_context(env);
        auto root = c->root;

        auto mp4a = root->get_child("mp4a");
        assert(mp4a != nullptr);

        auto esds = std::dynamic_pointer_cast<const petro::box::esds>(
            mp4a->get_child("esds"));
        assert(esds != nullptr);
        auto decoder_config_descriptor =
            esds->descriptor()->decoder_config_descriptor();

        return decoder_config_descriptor->channel_configuration();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleCount(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto mp4a = get_native_context(env)->root->get_child("mp4a");
        assert(mp4a != nullptr);

        auto trak = mp4a->get_parent("trak");
        assert(trak != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        return stsz->sample_count();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleTime(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;

        auto avc1 = get_native_context(env)->root->get_child("mp4a");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        auto tkhd = std::dynamic_pointer_cast<const petro::box::tkhd>(
            trak->get_child("tkhd"));
        assert(tkhd != nullptr);

        return tkhd->duration() / stsz->sample_count();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getAudioSample(
        JNIEnv* env, jobject thiz, jint index)
    {
        (void)thiz;

        auto c = get_native_context(env);

        std::ifstream mp4_file(c->mp4_file, std::ios::binary);

        auto root = c->root;

        auto mp4a = root->get_child("mp4a");
        assert(mp4a != nullptr);

        auto esds = std::dynamic_pointer_cast<const petro::box::esds>(
            mp4a->get_child("esds"));
        assert(esds != nullptr);
        auto decoder_config_descriptor =
            esds->descriptor()->decoder_config_descriptor();

        auto trak = mp4a->get_parent("trak");
        assert(trak != nullptr);

        auto stco = std::dynamic_pointer_cast<const petro::box::stco>(
            trak->get_child("stco"));
        assert(stco != nullptr);

        auto stsc = std::dynamic_pointer_cast<const petro::box::stsc>(
            trak->get_child("stsc"));
        assert(stsc != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        // fill output file with data.
        std::vector<char> sample;
        auto found_samples = 0;
        for (uint32_t i = 0; i < stco->entry_count(); ++i)
        {
            auto samples_for_chunk = stsc->samples_for_chunk(i);
            if (found_samples + samples_for_chunk > (uint32_t)index)
            {
                auto offset = stco->chunk_offset(i);
                for (uint32_t j = 0; j < stsc->samples_for_chunk(i); ++j)
                {
                    uint16_t sample_size = stsz->sample_size(found_samples);
                    if (found_samples == index)
                    {
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

        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(jsample, 0, sample.size(), (const jbyte*)sample.data());
        return jsample;
    }

    void Java_com_steinwurf_petro_NativeInterface_nativeFinalize(
        JNIEnv* env, jobject thiz)
    {
        (void) env;
        (void) thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeFinalize";
    }

    jint JNI_OnLoad(JavaVM* vm, void* reserved)
    {
        (void)reserved;
        LOGI << "JNI_OnLoad";

        java_vm = vm;

        JNIEnv* env;
        if (java_vm->GetEnv(
            reinterpret_cast<void**>(&env), JNI_VERSION_1_4) != JNI_OK)
        {
            LOGW << "Failed to get the environment using GetEnv()";
            return -1;
        }

        jclass java_iface_class = env->FindClass(
            "com/steinwurf/petro/NativeInterface");

        if (!java_iface_class)
        {
            LOGF << "Failed to find callback class";
            return -1;
        }

        native_interface_class =(jclass)env->NewGlobalRef(java_iface_class);

        native_context_field = env->GetStaticFieldID(native_interface_class,
            "native_context", "J");

        if (!native_context_field)
        {
            LOGF << "Failed to find native parser field.";
        }

        on_initialized_method = get_static_method_id(
            env, native_interface_class, "onInitialized", "()V");

        if(pthread_key_create(&current_jni_env, detach_current_thread))
        {
            LOGF << "Error initializing pthread key.";
        }

        return JNI_VERSION_1_4;
    }

#ifdef __cplusplus
}
#endif

#endif
