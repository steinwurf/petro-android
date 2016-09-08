// Copyright (c) 2014 Steinwurf ApS
// All Rights Reserved
//
// Distributed under the "BSD License". See the accompanying LICENSE.rst file.

#include <jni.h>
#include <pthread.h>

#include <memory>

#include <petro/extractor/aac_extractor.hpp>
#include <petro/extractor/h264_extractor.hpp>

#include "logging.hpp"

static JavaVM* java_vm = 0;
static jclass native_interface_class = 0;
static jmethodID on_initialized_method = 0;
static pthread_key_t current_jni_env;

std::shared_ptr<petro::extractor::h264_extractor> video;
std::shared_ptr<petro::extractor::aac_extractor> audio;

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
extern "C"
{
#endif

    void Java_com_steinwurf_petro_NativeInterface_nativeInitialize(
        JNIEnv* env, jobject thiz, jstring jmp4_file)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeInitialize";

        const char* mp4_file_str = env->GetStringUTFChars(jmp4_file, 0);
        auto mp4 = std::string(mp4_file_str);
        env->ReleaseStringUTFChars(jmp4_file, mp4_file_str);

        video = std::make_shared<petro::extractor::h264_extractor>(mp4);
        audio = std::make_shared<petro::extractor::aac_extractor>(mp4);
        // Make sure that the extractor provides raw samples (without
        // the ADTS header)
        audio->use_adts_header(false);

        env->CallStaticVoidMethod(
            native_interface_class, on_initialized_method);
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getPPS(
        JNIEnv* env, jobject /*thiz*/)
    {
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getPPS";
        auto pps_buffer = video->pps();
        auto jpps = env->NewByteArray(pps_buffer.size());
        env->SetByteArrayRegion(
            jpps, 0, pps_buffer.size(), (const jbyte*)pps_buffer.data());
        return jpps;
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getSPS(
        JNIEnv* env, jobject /*thiz*/)
    {
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getSPS";
        auto sps_buffer = video->sps();
        auto jsps = env->NewByteArray(sps_buffer.size());
        env->SetByteArrayRegion(
            jsps, 0, sps_buffer.size(), (const jbyte*)sps_buffer.data());
        return jsps;
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoWidth(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return video->video_width();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoHeight(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return video->video_height();
    }

    jboolean Java_com_steinwurf_petro_NativeInterface_advanceVideo(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return video->load_next_sample();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoPresentationTime(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return video->presentation_timestamp();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoSample(
        JNIEnv* env, jobject /*thiz*/)
    {
        auto sample = video->sample_data();
        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(
            jsample, 0, sample.size(), (const jbyte*)sample.data());
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

    jboolean Java_com_steinwurf_petro_NativeInterface_advanceAudio(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return audio->load_next_sample();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioPresentationTime(
        JNIEnv* /*env*/, jobject /*thiz*/)
    {
        return audio->decoding_timestamp();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getAudioSample(
        JNIEnv* env, jobject /*thiz*/)
    {
        auto sample = audio->sample_data();
        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(
            jsample, 0, sample.size(), (const jbyte*)sample.data());
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

        if(pthread_key_create(&current_jni_env, detach_current_thread))
        {
            LOGF << "Error initializing pthread key.";
        }

        return JNI_VERSION_1_4;
    }

#ifdef __cplusplus
}
#endif
