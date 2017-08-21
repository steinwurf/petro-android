// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

#include <jni.h>
#include <pthread.h>

#include <memory>

#include <petro/extractor/aac_sample_extractor.hpp>
#include <petro/extractor/annex_b_writer.hpp>
#include <petro/sequence_parameter_set.hpp>

#include "logging.hpp"

static JavaVM* java_vm = 0;
static jclass native_interface_class = 0;
static jmethodID on_initialized_method = 0;
static pthread_key_t current_jni_env;

std::shared_ptr<petro::extractor::annex_b_writer> video;
std::shared_ptr<petro::extractor::aac_sample_extractor> audio;

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

#ifdef __cplusplus
extern "C" {
#endif

void Java_com_steinwurf_petro_NativeInterface_nativeInitialize(
    JNIEnv* env, jobject thiz, jstring jmp4_file)
{
    (void)thiz;
    LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeInitialize";

    const char* mp4_file_str = env->GetStringUTFChars(jmp4_file, 0);
    auto mp4 = std::string(mp4_file_str);
    env->ReleaseStringUTFChars(jmp4_file, mp4_file_str);

    video = std::make_shared<petro::extractor::annex_b_writer>();
    audio = std::make_shared<petro::extractor::aac_sample_extractor>();

    video->set_file_path(mp4);
    audio->set_file_path(mp4);

    std::error_code video_error;
    video->open(video_error);
    if (video_error)
    {
        video.reset();
        LOGI << "Unable to open video: " << video_error.message();
    }

    std::error_code audio_error;
    audio->open(audio_error);
    if (audio_error)
    {
        audio.reset();
        LOGI << "Unable to open audio: " << audio_error.message();
    }

    env->CallStaticVoidMethod(
        native_interface_class, on_initialized_method);
}

jbyteArray Java_com_steinwurf_petro_NativeInterface_getPPS(
    JNIEnv* env, jobject /*thiz*/)
{
    LOGI << "Java_com_steinwurf_petro_NativeInterface_getPPS";
    std::vector<uint8_t> pps =
        {
            0, 0, 0, 1
        };
    auto pps_data = video->pps_data();
    auto pps_size = video->pps_size();
    pps.insert(pps.end(), pps_data, pps_data + pps_size);

    auto jpps = env->NewByteArray(pps.size());
    env->SetByteArrayRegion(
        jpps, 0, pps.size(), (const jbyte*)pps.data());
    return jpps;
}

jbyteArray Java_com_steinwurf_petro_NativeInterface_getSPS(
    JNIEnv* env, jobject /*thiz*/)
{
    LOGI << "Java_com_steinwurf_petro_NativeInterface_getSPS";
    std::vector<uint8_t> sps =
        {
            0, 0, 0, 1
        };
    auto sps_data = video->sps_data();
    auto sps_size = video->sps_size();
    sps.insert(sps.end(), sps_data, sps_data + sps_size);
    auto jsps = env->NewByteArray(sps.size());
    env->SetByteArrayRegion(
        jsps, 0, sps.size(), (const jbyte*)sps.data());
    return jsps;
}

jint Java_com_steinwurf_petro_NativeInterface_getVideoWidth(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    auto sps = petro::sequence_parameter_set(
        video->sps_data(), video->sps_size());
    return sps.width();
}

jint Java_com_steinwurf_petro_NativeInterface_getVideoHeight(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    auto sps = petro::sequence_parameter_set(
        video->sps_data(), video->sps_size());
    return sps.height();
}

void Java_com_steinwurf_petro_NativeInterface_advanceVideo(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    video->advance();
}

jboolean Java_com_steinwurf_petro_NativeInterface_videoAtEnd(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return video->at_end();
}

jint Java_com_steinwurf_petro_NativeInterface_getVideoPresentationTime(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return video->presentation_timestamp();
}

jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoSample(
    JNIEnv* env, jobject /*thiz*/)
{
    std::vector<uint8_t> s(video->annex_b_size());
    video->write_annex_b(s.data());

    auto jsample = env->NewByteArray(s.size());
    env->SetByteArrayRegion(jsample, 0, s.size(), (const jbyte*)s.data());
    return jsample;
}

jint Java_com_steinwurf_petro_NativeInterface_getAudioCodecProfileLevel(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return audio->mpeg_audio_object_type();
}

jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleRate(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return audio->frequency_index();
}

jint Java_com_steinwurf_petro_NativeInterface_getAudioChannelCount(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return audio->channel_configuration();
}

void Java_com_steinwurf_petro_NativeInterface_advanceAudio(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    audio->advance();
}

jboolean Java_com_steinwurf_petro_NativeInterface_audioAtEnd(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return audio->at_end();
}

jint Java_com_steinwurf_petro_NativeInterface_getAudioPresentationTime(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    return audio->presentation_timestamp();
}

jbyteArray Java_com_steinwurf_petro_NativeInterface_getAudioSample(
    JNIEnv* env, jobject /*thiz*/)
{
    auto size = audio->sample_size();
    auto data = audio->sample_data();

    auto jsample = env->NewByteArray(size);
    env->SetByteArrayRegion(jsample, 0, size, (const jbyte*)data);
    return jsample;
}

void Java_com_steinwurf_petro_NativeInterface_nativeFinalize(
    JNIEnv* /*env*/, jobject /*thiz*/)
{
    LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeFinalize";

    if (video) video.reset();
    if (audio) audio.reset();
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

    on_initialized_method = env->GetStaticMethodID(native_interface_class,
                                                   "onInitialized", "()V");

    if (!on_initialized_method)
    {
        LOGF << "Failed to find method onInitialized()";
    }

    if (pthread_key_create(&current_jni_env, detach_current_thread))
    {
        LOGF << "Error initializing pthread key.";
    }

    return JNI_VERSION_1_4;
}

#ifdef __cplusplus
}
#endif
