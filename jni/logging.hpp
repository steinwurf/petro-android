#pragma once

#include <android/log.h>
#include <algorithm>
#include <iostream>
#include <string>
#include <fstream>
#include <set>
#include <sstream>
#include <vector>

namespace logging
{
    // The message_logger acts as a stream and routes the contents of
    // the final stream to the Android logcat output.
    // If ANDROID is not defined, the output is only written to std::cerr.
    // This class should not be directly instantiated in code,
    // rather it should be invoked through the LOGW logging macro.
    class message_logger
    {
    public:

        message_logger(const char* file, int line, const char* tag,
                       android_LogPriority severity) :
            m_file(file), m_tag(tag), m_severity(severity)
        {
            // Prepend the stream with the file and line number.
            strip_basename(std::string(file), m_filename_only);
            m_stream << m_filename_only << ":" << line << " > ";
        }

        /// Output the contents of the stream on destruction.
        ~message_logger()
        {
            m_stream << std::endl;

            // Output the log string the Android log at the appropriate level.
            __android_log_print(m_severity, m_tag.c_str(), m_stream.str().c_str());

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
#define LOGI logging::message_logger((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_INFO).stream()
#define LOGW logging::message_logger((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_WARN).stream()
#define LOGE logging::message_logger((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_ERROR).stream()
#define LOGF logging::message_logger((char*)__FILE__, __LINE__, \
             "native", ANDROID_LOG_FATAL).stream()
