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

#include <petro/extractor/avc_sample_extractor.hpp>
#include <boost/iostreams/device/mapped_file.hpp>

struct avc_sample_extractor_jni
{
    petro::extractor::avc_sample_extractor e;
    boost::iostreams::mapped_file_source file;
};

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



jlong Java_com_steinwurf_petro_AVCSampleExtractor_init(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return reinterpret_cast<jlong>(new avc_sample_extractor_jni());
}

void Java_com_steinwurf_petro_AVCSampleExtractor_open(
    JNIEnv* env, jobject thiz, jstring jfile_path, jint track_id)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);

    auto file_path = jutils::java_string_to_string(env, jfile_path);

    std::error_code error;
    try
    {
        extractor->file.open(file_path);
    }
    catch (std::ios::failure& e)
    {
        error = std::make_error_code(
            static_cast<std::errc>(e.code().value()));
    }

    if (!error)
        extractor->e.open(
            (uint8_t*)extractor->file.data(),
            extractor->file.size(),
            track_id,
            error);

    if (error)
    {
        auto exception_class = jutils::get_class(
            env, "com/steinwurf/petro/UnableToOpenException");
        env->ThrowNew(exception_class, error.message().c_str());
    }
}

void Java_com_steinwurf_petro_AVCSampleExtractor_close(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    extractor->file.close();
    return extractor->e.close();
}

void Java_com_steinwurf_petro_AVCSampleExtractor_reset(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    extractor->e.reset();
}

jint Java_com_steinwurf_petro_AVCSampleExtractor_getTrackID(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.track_id();
}

jlong Java_com_steinwurf_petro_AVCSampleExtractor_getDuration(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.media_duration();
}

jlong Java_com_steinwurf_petro_AVCSampleExtractor_getDecodingTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.decoding_timestamp();
}

jlong Java_com_steinwurf_petro_AVCSampleExtractor_getPresentationTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.presentation_timestamp();
}

void Java_com_steinwurf_petro_AVCSampleExtractor_advance(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.advance();
}

jboolean Java_com_steinwurf_petro_AVCSampleExtractor_atEnd(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.at_end();
}

jbyteArray Java_com_steinwurf_petro_AVCSampleExtractor_getSample(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    jbyteArray jsample = env->NewByteArray(extractor->e.sample_size());
    env->SetByteArrayRegion(
        jsample, 0, extractor->e.sample_size(), (jbyte*)extractor->e.sample_data());
    return jsample;
}

jlong Java_com_steinwurf_petro_AVCSampleExtractor_getSampleIndex(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.sample_index();
}

jlong Java_com_steinwurf_petro_AVCSampleExtractor_getSampleCount(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.samples();
}

void Java_com_steinwurf_petro_AVCSampleExtractor_setLoopingEnabled(
    JNIEnv* env, jobject thiz, jboolean enabled)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    if (enabled)
        extractor->e.enable_looping();
    else
        extractor->e.disable_looping();
}

jint Java_com_steinwurf_petro_AVCSampleExtractor_getLoopCount(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.loops();
}

jbyteArray Java_com_steinwurf_petro_AVCSampleExtractor_getPPS(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    jbyteArray jpps = env->NewByteArray(extractor->e.pps_size());
    env->SetByteArrayRegion(
        jpps, 0, extractor->e.pps_size(), (jbyte*)extractor->e.pps_data());
    return jpps;
}

jbyteArray Java_com_steinwurf_petro_AVCSampleExtractor_getSPS(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    jbyteArray jsps = env->NewByteArray(extractor->e.sps_size());
    env->SetByteArrayRegion(
        jsps, 0, extractor->e.sps_size(), (jbyte*)extractor->e.sps_data());
    return jsps;
}

jint Java_com_steinwurf_petro_AVCSampleExtractor_getNALULengthSize(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->e.nalu_length_size();
}

void Java_com_steinwurf_petro_AVCSampleExtractor_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<avc_sample_extractor_jni*>(pointer);
    assert(client);
    delete client;
}

#ifdef __cplusplus
}
#endif
