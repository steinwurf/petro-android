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

#include <petro/extractor/nalu_extractor.hpp>
#include <petro/extractor/annex_b_writer.hpp>

// using avc_sample_extractor_jni = petro::extractor::nalu_extractor;
using avc_sample_extractor_jni = petro::extractor::annex_b_writer;

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

jlong Java_com_steinwurf_mediaextractor_NALUSampleExtractor_init(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return reinterpret_cast<jlong>(new avc_sample_extractor_jni());
}

// jboolean Java_com_steinwurf_mediaextractor_NALUSampleExtractor_isBeginningOfAVCSample(
//     JNIEnv* env, jobject thiz)
// {
//     auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
//     return extractor->is_beginning_of_avc_sample();
// }

jbyteArray Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getPPSData(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    jbyteArray jpps = env->NewByteArray(extractor->pps_size());
    env->SetByteArrayRegion(
        jpps, 0, extractor->pps_size(), (jbyte*)extractor->pps_data());
    return jpps;
}

jbyteArray Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getSPSData(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    jbyteArray jsps = env->NewByteArray(extractor->sps_size());
    env->SetByteArrayRegion(
        jsps, 0, extractor->sps_size(), (jbyte*)extractor->sps_data());
    return jsps;
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<avc_sample_extractor_jni*>(pointer);
    assert(client);
    delete client;
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_setFilePath(
    JNIEnv* env, jobject thiz, jstring file_path)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    extractor->set_file_path(jutils::java_string_to_string(env, file_path));
}

jstring Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getFilePath(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return jutils::string_to_java_string(env, extractor->file_path());
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_open(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    std::error_code error;
    extractor->open(error);

    if (error)
    {
        LOGE << "unable to open extractor: " << error.message();
        auto exception_class = jutils::get_class(
            env, "com/steinwurf/mediaextractor/NALUSampleExtractor$UnableToOpenException");
        env->ThrowNew(exception_class, error.message().c_str());
    }
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_reset(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    extractor->reset();
}

jbyteArray Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getSample(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    assert(!extractor->at_end());

    // LOGI << extractor->nalu_size();
    // LOGI << extractor->sample_size();
    // LOGI << extractor->is_beginning_of_avc_sample();

    // std::vector<uint8_t> sample(
    //     extractor->nalu_data(),
    //     extractor->nalu_data() + extractor->nalu_size());
    std::vector<uint8_t> sample(extractor->annex_b_size());
    extractor->write_annex_b(sample.data());

    jbyteArray jsample = env->NewByteArray(sample.size());
    env->SetByteArrayRegion(jsample, 0, sample.size(), (jbyte*)sample.data());
    return jsample;
}

jlong Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getDecodingTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    assert(!extractor->at_end());
    return extractor->decoding_timestamp();
}

jlong Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getPresentationTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    assert(!extractor->at_end());
    return extractor->presentation_timestamp();
}

jlong Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getSampleIndex(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->sample_index();
}

jlong Java_com_steinwurf_mediaextractor_NALUSampleExtractor_getDuration(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->media_duration();
}

jboolean Java_com_steinwurf_mediaextractor_NALUSampleExtractor_atEnd(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->at_end();
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_advance(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->advance();
}

void Java_com_steinwurf_mediaextractor_NALUSampleExtractor_close(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<avc_sample_extractor_jni>(env, thiz);
    return extractor->close();
}

#ifdef __cplusplus
}
#endif
