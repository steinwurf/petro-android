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

#include <petro/sequence_parameter_set.hpp>

using sequence_parameter_set_jni =
    jutils::ptr_container<petro::sequence_parameter_set>;

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

jobject Java_com_steinwurf_mediaextractor_SequenceParameterSet_parse(
    JNIEnv* env, jclass clazz, jbyteArray jbuffer)
{
    auto buffer = jutils::java_byte_array_to_vector(env, jbuffer);

    std::error_code error;
    auto sps = petro::sequence_parameter_set::parse(
        buffer.data(), buffer.size(), error);
    if (error)
    {
        LOGE << error.message();
        return nullptr;
    }

    auto pointer = reinterpret_cast<jlong>(new sequence_parameter_set_jni(sps));
    auto constructor = jutils::get_method(env, clazz, "<init>", "(J)V");
    auto jsps = env->NewObject(clazz, constructor, pointer);
    return jsps;
}

jint Java_com_steinwurf_mediaextractor_SequenceParameterSet_getVideoWidth(
    JNIEnv* env, jobject thiz)
{
    auto sps = jutils::get_native<sequence_parameter_set_jni>(env, thiz);
    return sps->width();
}

jint Java_com_steinwurf_mediaextractor_SequenceParameterSet_getVideoHeight(
    JNIEnv* env, jobject thiz)
{
    auto sps = jutils::get_native<sequence_parameter_set_jni>(env, thiz);
    return sps->height();
}

void Java_com_steinwurf_mediaextractor_SequenceParameterSet_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<sequence_parameter_set_jni*>(pointer);
    assert(client);
    delete client;
}

#ifdef __cplusplus
}
#endif
