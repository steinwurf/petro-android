// Copyright (c) 2016 Steinwurf ApS
// All Rights Reserved
//
// THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
// The copyright notice above does not evidence any
// actual or intended publication of such source code.

#include <cassert>
#include <cstdint>
#include <functional>
#include <system_error>

#include <jni.h>

#include <jutils/utils.hpp>
#include <jutils/ptr_container.hpp>
#include <jutils/logging.hpp>

#include <petro/extractor/aac_sample_extractor.hpp>

using aac_sample_extractor_jni = petro::extractor::aac_sample_extractor;

jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/)
{
    jutils::init(vm);
    return JNI_VERSION_1_4;
}

// To allow for overloading of functions, C++ uses something called name
// mangling.
// This means that function names are not the same in C++ as in plain C.
// To inhibit this name mangling, you have to declare functions as extern "C"
#ifdef __cplusplus
extern "C" {
#endif

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_init(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return reinterpret_cast<jlong>(new aac_sample_extractor_jni());
}

jbyteArray Java_com_steinwurf_mediaextractor_AACSampleExtractor_getADTSHeader(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    std::vector<uint8_t> adts(extractor->adts_header_size());
    extractor->write_adts_header(adts.data());
    jbyteArray jadts = env->NewByteArray(adts.size());
    env->SetByteArrayRegion(jadts, 0, adts.size(), (const jbyte*)adts.data());
    return jadts;
}

jint Java_com_steinwurf_mediaextractor_AACSampleExtractor_getMPEGAudioObjectType(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->mpeg_audio_object_type();
}

jint Java_com_steinwurf_mediaextractor_AACSampleExtractor_getFrequencyIndex(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->frequency_index();
}

jint Java_com_steinwurf_mediaextractor_AACSampleExtractor_getChannelConfiguration(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->channel_configuration();
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_setFilePath(
    JNIEnv* env, jobject thiz, jstring file_path)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    extractor->set_file_path(jutils::java_string_to_string(env, file_path));
}

jstring Java_com_steinwurf_mediaextractor_AACSampleExtractor_getFilePath(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return jutils::string_to_java_string(env, extractor->file_path());
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_open(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    std::error_code error;
    extractor->open(error);
    if (error)
    {
        auto exception_class = jutils::get_class(
            env, "com/steinwurf/mediaextractor/Extractor$UnableToOpenException");
        env->ThrowNew(exception_class, error.message().c_str());
    }
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_reset(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    extractor->reset();
}

jbyteArray Java_com_steinwurf_mediaextractor_AACSampleExtractor_getSample(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    jbyteArray jsample = env->NewByteArray(extractor->sample_size());
    env->SetByteArrayRegion(
        jsample, 0, extractor->sample_size(), (jbyte*)extractor->sample_data());
    return jsample;
}

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_getDecodingTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->decoding_timestamp();
}

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_getPresentationTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->presentation_timestamp();
}

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_getSampleIndex(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->sample_index();
}

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_getSampleCount(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->samples();
}

jlong Java_com_steinwurf_mediaextractor_AACSampleExtractor_getDuration(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->media_duration();
}

jboolean Java_com_steinwurf_mediaextractor_AACSampleExtractor_atEnd(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->at_end();
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_advance(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->advance();
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_close(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<aac_sample_extractor_jni>(env, thiz);
    return extractor->close();
}

void Java_com_steinwurf_mediaextractor_AACSampleExtractor_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<aac_sample_extractor_jni*>(pointer);
    assert(client);
    delete client;
}

#ifdef __cplusplus
}
#endif
