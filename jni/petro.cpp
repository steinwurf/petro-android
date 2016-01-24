#ifdef ANDROID

#include <jni.h>
#include <pthread.h>

#include "logging.hpp"

#include "video_interface.hpp"
#include "audio_interface.hpp"

#include "mp4_video.hpp"
#include "mp4_audio.hpp"
#include "c4m_video.hpp"

static JavaVM* java_vm = 0;

static jclass native_interface_class = 0;

static jmethodID on_initialized_method = 0;
static jfieldID native_context_field = 0;

static pthread_key_t current_jni_env;

struct context
{
    std::shared_ptr<petro_android::video_interface> video;
    std::shared_ptr<petro_android::audio_interface> audio;
};

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
    return (context*)
        env->GetStaticLongField(native_interface_class, native_context_field);
}

static void set_native_context(JNIEnv* env, context* context)
{
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
        auto mp4 = std::string(mp4_file_str);
        env->ReleaseStringUTFChars(jmp4_file, mp4_file_str);

        auto c = new context();
        c->video = std::make_shared<petro_android::mp4_video>(mp4);

        /// @todo fix again - just for a quick test
        // c->video = std::make_shared<petro_android::c4m_video>(
            // "/storage/emulated/0/custom_capture.h264");

        c->audio = std::make_shared<petro_android::mp4_audio>(mp4);
        set_native_context(env, c);

        env->CallStaticVoidMethod(native_interface_class, on_initialized_method);
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getPPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getPPS";
        auto pps_buffer = get_native_context(env)->video->pps();
        auto jpps = env->NewByteArray(pps_buffer.size());
        env->SetByteArrayRegion(jpps, 0, pps_buffer.size(), (const jbyte*)pps_buffer.data());
        return jpps;
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getSPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getSPS";
        auto sps_buffer = get_native_context(env)->video->sps();
        auto jsps = env->NewByteArray(sps_buffer.size());
        env->SetByteArrayRegion(jsps, 0, sps_buffer.size(), (const jbyte*)sps_buffer.data());
        return jsps;
    }

    jint Java_com_steinwurf_petro_NativeInterface_getWidth(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->video->width();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getHeight(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->video->height();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getVideoSampleTime(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->video->sample_time();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getVideoSample(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        auto sample = get_native_context(env)->video->next_sample();
        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(jsample, 0, sample.size(), (const jbyte*)sample.data());
        return jsample;
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioCodecProfileLevel(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->audio->codec_profile_level();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleRate(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->audio->sample_rate_index();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioChannelCount(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->audio->channel_config();
    }

    jint Java_com_steinwurf_petro_NativeInterface_getAudioSampleTime(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        return get_native_context(env)->audio->sample_time();
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getAudioSample(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        auto sample = get_native_context(env)->audio->next_sample();

        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(jsample, 0, sample.size(), (const jbyte*)sample.data());
        return jsample;
    }

    void Java_com_steinwurf_petro_NativeInterface_nativeFinalize(
        JNIEnv* env, jobject thiz)
    {
        LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeFinalize";
        (void) env;
        (void) thiz;

        auto c = get_native_context(env);
        delete c;
        set_native_context(env, nullptr);
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

#endif
