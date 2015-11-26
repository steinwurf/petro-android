#pragma once

#ifdef ANDROID

#include <jni.h>
#include "logging.hpp"

#ifdef __cplusplus
extern "C"
{
#endif
    jmethodID get_static_method_id(JNIEnv* env, const jclass& clazz,
        const char* methodName, const char* methodSignature)
    {
        jmethodID method =
            env->GetStaticMethodID(clazz, methodName,
                methodSignature);

        if (!method)
        {
            LOGF << "Failed to find method:" << methodName
                 << methodSignature;
        }
        return method;
    }

#ifdef __cplusplus
}
#endif

#endif
