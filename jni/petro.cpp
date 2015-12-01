#ifdef ANDROID

#include <unistd.h>

#include <jni.h>
#include <pthread.h>
#include <thread>
#include <cassert>
#include <fstream>

#include <vector>
#include <cstdlib>

#include <petro/byte_stream.hpp>
#include <petro/box/all.hpp>
#include <petro/parser.hpp>

#include "jni_utils.hpp"
#include "logging.hpp"

static JavaVM* java_vm = 0;

static jclass native_interface_class = 0;

static jmethodID on_message_method = 0;
static jmethodID on_initialized_method = 0;
static jfieldID native_context_field = 0;

static std::thread main_thread;
static pthread_key_t current_jni_env;

struct context
{
    std::shared_ptr<petro::box::root> root;
    std::string mp4_file;
};


static uint32_t read_uint32_t(const uint8_t* data)
{
    uint32_t result =
       (uint32_t) data[0] << 24 |
       (uint32_t) data[1] << 16 |
       (uint32_t) data[2] << 8 |
       (uint32_t) data[3];
    return result;
}

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

static context* get_native_context(JNIEnv* env)
{
    LOGI << "get_native_context";

    return (context*)
        env->GetStaticLongField(native_interface_class, native_context_field);
}

static void set_native_context(JNIEnv* env, context* context)
{
    LOGI << "set_native_context";

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

        auto c = new context();
        c->mp4_file = std::string(mp4_file_str);

        env->ReleaseStringUTFChars(jmp4_file, mp4_file_str);

        std::ifstream mp4_file(c->mp4_file, std::ios::binary);
        LOGI << " good()=" << mp4_file.good();
        LOGI << " eof()=" << mp4_file.eof();
        LOGI << " fail()=" << mp4_file.fail();
        LOGI << " bad()=" << mp4_file.bad();
        LOGI << " open()=" << mp4_file.is_open();
        if (!mp4_file.good())
        {
            LOGF << "Error reading file, it's not good?";
        }

        if (!mp4_file.is_open())
        {
            LOGF << "Error reading file, it's not open";
        }

        auto data = std::vector<char>((
            std::istreambuf_iterator<char>(mp4_file)),
            std::istreambuf_iterator<char>());

        petro::parser<
            petro::box::moov<petro::parser<
                petro::box::trak<petro::parser<
                    petro::box::mdia<petro::parser<
                        petro::box::hdlr,
                        petro::box::minf<petro::parser<
                            petro::box::stbl<petro::parser<
                                petro::box::stsd,
                                petro::box::stsc,
                                petro::box::stsz,
                                petro::box::stco
                            >>
                        >>
                    >>
                >>
            >>
        > parser;

        auto root = std::make_shared<petro::box::root>();

        parser.read(root, (uint8_t*)data.data(), data.size());


        c->root = root;
        set_native_context(env, c);

        env->CallStaticVoidMethod(native_interface_class, on_initialized_method);
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getPPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getPPS";

        std::vector<char> pps_buffer = {0, 0, 0, 1};

        auto avcc = std::dynamic_pointer_cast<const petro::box::avcc>(
            get_native_context(env)->root->get_child("avcC"));

        assert(avcc != nullptr);

        auto pps = avcc->picture_parameter_set(0);
        pps_buffer.insert(pps_buffer.end(), pps.begin(), pps.end());

        auto jpps = env->NewByteArray(pps_buffer.size());
        env->SetByteArrayRegion(jpps, 0, pps_buffer.size(), (const jbyte*)pps_buffer.data());
        return jpps;
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getSPS(
        JNIEnv* env, jobject thiz)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_getSPS";

        std::vector<char> sps_buffer = {0, 0, 0, 1};

        auto avcc = std::dynamic_pointer_cast<const petro::box::avcc>(
            get_native_context(env)->root->get_child("avcC"));

        assert(avcc != nullptr);

        auto sps = avcc->sequence_parameter_set(0);
        sps_buffer.insert(sps_buffer.end(), sps.begin(), sps.end());

        auto jpps = env->NewByteArray(sps_buffer.size());
        env->SetByteArrayRegion(jpps, 0, sps_buffer.size(), (const jbyte*)sps_buffer.data());
        return jpps;
    }

    jbyteArray Java_com_steinwurf_petro_NativeInterface_getSample(
        JNIEnv* env, jobject thiz, jint index)
    {
        (void)thiz;
        LOGI << "Java_com_steinwurf_petro_NativeInterface_get_sample " << index;

        std::vector<char> nalu_seperator = {0, 0, 0, 1};

        auto c = get_native_context(env);

        std::ifstream mp4_file(c->mp4_file, std::ios::binary);

        auto data = std::vector<char>((
            std::istreambuf_iterator<char>(mp4_file)),
            std::istreambuf_iterator<char>());

        auto root = c->root;

        LOGI << c->mp4_file;

        // don't handle special case with fragmented samples
        assert(root->get_child("mvex") == nullptr);

        auto avc1 = root->get_child("avc1");
        assert(avc1 != nullptr);

        auto trak = avc1->get_parent("trak");
        assert(trak != nullptr);

        auto stco = std::dynamic_pointer_cast<const petro::box::stco>(
            trak->get_child("stco"));
        assert(stco != nullptr);

        auto stsz = std::dynamic_pointer_cast<const petro::box::stsz>(
            trak->get_child("stsz"));
        assert(stsz != nullptr);

        auto stsc = std::dynamic_pointer_cast<const petro::box::stsc>(
            trak->get_child("stsc"));
        assert(stsc != nullptr);

        LOGI << "looking for sample";
        std::vector<char> sample;
        auto found_samples = 0;
        for (uint32_t i = 0; i < stco->entry_count(); ++i)
        {
            auto offset = stco->chunk_offset(i);
            for (uint32_t j = 0; j < stsc->samples_for_chunk(i); ++j)
            {
                if (found_samples == index)
                {
                    LOGI << "found sample";
                    auto actual_size = read_uint32_t((uint8_t*)(data.data() + offset));
                    LOGI << "actual_size " << actual_size;
                    actual_size += 10;
                    sample.insert(sample.begin(), nalu_seperator.begin(), nalu_seperator.end());

                    if (found_samples == 50 ||
                        found_samples == 51 ||
                        found_samples == 52 ||
                        found_samples == 53 ||
                        found_samples == 54 ||
                        found_samples == 55 ||
                        found_samples == 56 ||
                        found_samples == 57 ||
                        // found_samples == 0 ||
                        // found_samples == 1 ||
                        found_samples == 58 ||
                        found_samples == 59 ||
                        found_samples == 60)
                    {
                        std::vector<char> dummy(actual_size, 0);
                        // std::generate(dummy.begin(), dummy.end(), rand);
                        sample.insert(sample.end(), dummy.begin(), dummy.end());
                    }
                    else
                    {
                        auto data_from = data.begin() + (offset + 4);
                        sample.insert(sample.end(), data_from, data_from + actual_size);
                    }
                }
                offset += stsz->sample_size(found_samples);
                found_samples += 1;
            }
        }

        auto jsample = env->NewByteArray(sample.size());
        env->SetByteArrayRegion(jsample, 0, sample.size(), (const jbyte*)sample.data());
        return jsample;
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

        native_context_field = env->GetStaticFieldID(native_interface_class,
            "native_context", "J");

        if (!native_context_field)
        {
            LOGF << "Failed to find native parser field.";
        }

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
