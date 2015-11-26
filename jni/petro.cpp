#ifdef ANDROID

#include <unistd.h>

#include <jni.h>
#include <pthread.h>

#include <vector>
#include <cstdlib>

#include "jni_utils.hpp"
#include "logging.hpp"

static JavaVM* java_vm = 0;

static jclass native_interface_class = 0;

static jmethodID on_message_method = 0;
static jmethodID on_initialized_method = 0;

static std::thread main_thread;
static pthread_key_t current_jni_env;

// Register this thread with the VM
static JNIEnv* attach_current_thread()
{
    LOGI << "Attaching thread";

    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_4;
    args.name = NULL;
    args.group = NULL;

    JNIEnv* env;
    if (java_vm->AttachCurrentThread(&env, &args) < 0)
    {
        LOGE << "Failed to attach current thread";
        return NULL;
    }

    return env;
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

// Retrieve the JNI environment for this thread
static JNIEnv* get_jni_env()
{
    LOGI << "get_jni_env";

    JNIEnv* env = (JNIEnv*)pthread_getspecific(current_jni_env);

    if (env == NULL)
    {
        env = attach_current_thread();
        pthread_setspecific(current_jni_env, env);
    }

    return env;
}

static void message(const char* message)
{
    JNIEnv* env = get_jni_env();
    LOGI << "Messaging java: " << message;
    jstring jmessage = env->NewStringUTF(message);
    env->CallStaticVoidMethod(
        native_interface_class, on_message_method, jmessage);
    env->DeleteLocalRef(jmessage);
}

#ifdef __cplusplus
extern "C"
{
#endif

    void Java_com_steinwurf_petro_NativeInterface_nativeInitialize(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_nativeInitialize";

        env->CallStaticVoidMethod(native_interface_class, on_initialized_method);
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

        on_message_method = get_static_method_id(env, native_interface_class,
            "onMessage", "(Ljava/lang/String;)V");

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
