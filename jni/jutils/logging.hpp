// Copyright (c) 2016 Steinwurf ApS
// All Rights Reserved
//
// THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
// The copyright notice above does not evidence any
// actual or intended publication of such source code.

#pragma once

#include <android/log.h>
#include <algorithm>
#include <iostream>
#include <string>
#include <sstream>

namespace jutils
{
// The logging class acts as a stream and routes the contents of
// the final stream to the Android logcat output.
// If ANDROID is not defined, the output is only written to std::cerr.
// This class should not be directly instantiated in code,
// rather it should be invoked through the LOGW logging macro.
class logging
{
public:

    logging(const char* file, int line, const char* tag,
            android_LogPriority severity) :
        m_file(file), m_tag(tag), m_severity(severity)
    {
        // Prepend the stream with the file and line number.
        strip_basename(std::string(file), m_filename_only);
        m_stream << m_filename_only << ":" << line << " > ";
    }

    /// Output the contents of the stream on destruction.
    ~logging()
    {
        m_stream << std::endl;

        // Output the log string the Android log at the appropriate level.
        __android_log_print(m_severity, m_tag.c_str(), "%s",
                            m_stream.str().c_str());

        // Indicate termination if needed.
        if (m_severity == ANDROID_LOG_FATAL)
        {
            __android_log_print(m_severity, m_tag.c_str(), "Terminating.\n");
            abort();
        }
    }

    // Return the stream associated with the logger object.
    std::stringstream& stream() { return m_stream; }

private:

    void strip_basename(const std::string& full_path, std::string& filename)
    {
        // Try to find the last '/' character
        const char k_separator = '/';
        size_t pos = full_path.rfind(k_separator);
        if (pos != std::string::npos)
        {
            filename = full_path.substr(pos + 1, std::string::npos);
        }
        else
        {
            // Try to find the last '\' character on Windows
            pos = full_path.rfind('\\');
            if (pos != std::string::npos)
                filename = full_path.substr(pos + 1, std::string::npos);
            else
                filename = full_path;
        }
    }

private:

    std::string m_file;
    std::string m_filename_only;
    std::string m_tag;
    std::stringstream m_stream;
    int m_severity;
};
}

// ---------------------- Macro definitions --------------------------
#define LOGI jutils::logging((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_INFO).stream()
#define LOGW jutils::logging((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_WARN).stream()
#define LOGE jutils::logging((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_ERROR).stream()
#define LOGF jutils::logging((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_FATAL).stream()
