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

using nalu_extractor_jni = petro::extractor::nalu_extractor;

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

jlong Java_com_steinwurf_petro_NALUExtractor_init(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return reinterpret_cast<jlong>(new nalu_extractor_jni());
}

jboolean Java_com_steinwurf_petro_NALUExtractor_isBeginningOfAVCSample(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->is_beginning_of_avc_sample();
}

jbyteArray Java_com_steinwurf_petro_NALUExtractor_getPPS(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    // std::vector<uint8_t>
    jbyteArray jpps = env->NewByteArray(extractor->pps_size());
    env->SetByteArrayRegion(
        jpps, 0, extractor->pps_size(), (jbyte*)extractor->pps_data());
    return jpps;
}

jbyteArray Java_com_steinwurf_petro_NALUExtractor_getSPS(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    jbyteArray jsps = env->NewByteArray(extractor->sps_size());
    env->SetByteArrayRegion(
        jsps, 0, extractor->sps_size(), (jbyte*)extractor->sps_data());
    return jsps;
}

void Java_com_steinwurf_petro_NALUExtractor_setFilePath(
    JNIEnv* env, jobject thiz, jstring file_path)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    extractor->set_file_path(jutils::java_string_to_string(env, file_path));
}

jstring Java_com_steinwurf_petro_NALUExtractor_getFilePath(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return jutils::string_to_java_string(env, extractor->file_path());
}

void Java_com_steinwurf_petro_NALUExtractor_open(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    std::error_code error;
    extractor->open(error);

    if (error)
    {
        LOGE << "unable to open extractor: " << error.message();
        auto exception_class = jutils::get_class(
            env, "com/steinwurf/petro/Extractor$UnableToOpenException");
        env->ThrowNew(exception_class, error.message().c_str());
    }
}

void Java_com_steinwurf_petro_NALUExtractor_reset(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    extractor->reset();
}

jbyteArray Java_com_steinwurf_petro_NALUExtractor_getNalu(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    assert(!extractor->at_end());

    std::vector<uint8_t> nalu(
        extractor->nalu_data(),
        extractor->nalu_data() + extractor->nalu_size());

    jbyteArray jsample = env->NewByteArray(nalu.size());
    env->SetByteArrayRegion(jsample, 0, nalu.size(), (jbyte*)nalu.data());
    return jsample;
}

jlong Java_com_steinwurf_petro_NALUExtractor_getDecodingTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    assert(!extractor->at_end());
    return extractor->decoding_timestamp();
}

jlong Java_com_steinwurf_petro_NALUExtractor_getPresentationTimestamp(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    assert(!extractor->at_end());
    return extractor->presentation_timestamp();
}

jlong Java_com_steinwurf_petro_NALUExtractor_getSampleIndex(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->sample_index();
}

jlong Java_com_steinwurf_petro_NALUExtractor_getSampleCount(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->samples();
}

jlong Java_com_steinwurf_petro_NALUExtractor_getDuration(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->media_duration();
}

jboolean Java_com_steinwurf_petro_NALUExtractor_atEnd(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->at_end();
}

void Java_com_steinwurf_petro_NALUExtractor_advance(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->advance();
}

void Java_com_steinwurf_petro_NALUExtractor_close(
    JNIEnv* env, jobject thiz)
{
    auto extractor = jutils::get_native_pointer<nalu_extractor_jni>(env, thiz);
    return extractor->close();
}

void Java_com_steinwurf_petro_NALUExtractor_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<nalu_extractor_jni*>(pointer);
    assert(client);
    delete client;
}

#ifdef __cplusplus
}
#endif
