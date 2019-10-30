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
#include <jutils/logging.hpp>

#include <petro/extractor/track_extractor.hpp>
#include <boost/iostreams/device/mapped_file.hpp>

struct track_extractor_jni
{
    petro::extractor::track_extractor e;
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

jlong Java_com_steinwurf_petro_TrackExtractor_init(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return reinterpret_cast<jlong>(new track_extractor_jni());
}

void Java_com_steinwurf_petro_TrackExtractor_open(
    JNIEnv* env, jobject thiz, jstring jfile_path)
{
    auto extractor =
        jutils::get_native_pointer<track_extractor_jni>(env, thiz);

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
            error);

    if (error)
    {
        auto exception_class = jutils::get_class(
            env, "com/steinwurf/petro/UnableToOpenException");
        env->ThrowNew(exception_class, "File not found");
    }
}

jobject type_track_to_jtype_track(JNIEnv* env, petro::extractor::track_type type)
{

    auto track_type_class = jutils::get_class(
        env, "com/steinwurf/petro/TrackExtractor$TrackType");

    auto type_str = "UNKNOWN";
    switch (type)
    {
    case petro::extractor::track_type::unknown:
        type_str = "UNKNOWN";
        break;
    case petro::extractor::track_type::unknown_audio:
        type_str = "UNKNOWN_AUDIO";
        break;
    case petro::extractor::track_type::text:
        type_str = "TEXT";
        break;
    case petro::extractor::track_type::aac:
        type_str = "AAC";
        break;
    case petro::extractor::track_type::avc1:
        type_str = "AVC1";
        break;
    case petro::extractor::track_type::hvc1:
        type_str = "HVC1";
        break;
    default:
        assert(false && "Unkown track type");
        break;
    }

    jfieldID field_id = env->GetStaticFieldID(
        track_type_class,
        type_str,
        "Lcom/steinwurf/petro/TrackExtractor$TrackType;");
    return env->GetStaticObjectField(track_type_class, field_id);
}

jobjectArray Java_com_steinwurf_petro_TrackExtractor_getTracks(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<track_extractor_jni>(env, thiz);

    auto track_class = jutils::get_class(
        env, "com/steinwurf/petro/TrackExtractor$Track");
    auto track_constructor = jutils::get_method(
        env,
        track_class,
        "<init>",
        "(ILcom/steinwurf/petro/TrackExtractor$TrackType;)V");

    auto tracks = extractor->e.tracks();
    jobjectArray jtrack_array = env->NewObjectArray(tracks.size(), track_class, NULL);
    for (uint32_t i = 0; i < tracks.size(); i++)
    {
        auto& track = tracks.at(i);
        auto jtrack_type = type_track_to_jtype_track(env, track.type);
        jobject jtrack = env->NewObject(track_class, track_constructor, track.id, jtrack_type);
        env->SetObjectArrayElement(jtrack_array, i, jtrack);
    }

    return jtrack_array;
}

void Java_com_steinwurf_petro_TrackExtractor_close(
    JNIEnv* env, jobject thiz)
{
    auto extractor =
        jutils::get_native_pointer<track_extractor_jni>(env, thiz);
    extractor->file.close();
    return extractor->e.close();
}

void Java_com_steinwurf_petro_TrackExtractor_finalize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer)
{
    auto client = reinterpret_cast<track_extractor_jni*>(pointer);
    assert(client);
    delete client;
}

#ifdef __cplusplus
}
#endif
