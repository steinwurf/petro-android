cmake_minimum_required(VERSION 3.10)
project(petro_android)

if(NOT DEFINED STEINWURF_RESOLVE)
    message(STATUS "Resolving dependencies...")
    execute_process(
        COMMAND python waf resolve
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../")
    set(STEINWURF_RESOLVE "${CMAKE_CURRENT_SOURCE_DIR}/../resolve_symlinks")
endif()

# jutils

file(GLOB_RECURSE jutils_sources ./src/main/cpp/jutils/*.cpp)
add_library(jutils STATIC ${jutils_sources})
add_library(steinwurf::jutils ALIAS jutils)
target_include_directories(jutils INTERFACE src/main/cpp/)
target_compile_features(jutils PUBLIC cxx_std_11)

# boost

add_library(boost STATIC "${STEINWURF_RESOLVE}/boost/libs/iostreams/src/mapped_file.cpp")
target_include_directories(boost
    PUBLIC "${STEINWURF_RESOLVE}/boost/")

add_library(steinwurf::boost ALIAS boost)
target_compile_definitions(boost
    PRIVATE BOOST_ALL_NO_LIB=1
    PRIVATE BOOST_DETAIL_NO_CONTAINER_FWD
    PRIVATE BOOST_SYSTEM_NO_DEPRECATED
    PRIVATE BOOST_NO_AUTO_PTR)

# petro

if (NOT TARGET petro)
add_subdirectory("${STEINWURF_RESOLVE}/petro" petro)
endif()

# sequence_parameter_set_jni

add_library(sequence_parameter_set_jni SHARED src/main/cpp/sequence_parameter_set_jni.cpp)

target_link_libraries(sequence_parameter_set_jni
    PRIVATE steinwurf::jutils
    PRIVATE steinwurf::endian
    PRIVATE steinwurf::petro
    PRIVATE log)

target_compile_features(sequence_parameter_set_jni PUBLIC cxx_std_14)

target_include_directories(sequence_parameter_set_jni INTERFACE cpp)

# track_extractor_jni

add_library(track_extractor_jni SHARED src/main/cpp/track_extractor_jni.cpp)

target_link_libraries(track_extractor_jni
    PRIVATE steinwurf::jutils
    PRIVATE steinwurf::endian
    PRIVATE steinwurf::petro
    PRIVATE steinwurf::boost
    PRIVATE log)

target_compile_features(track_extractor_jni PUBLIC cxx_std_14)

target_include_directories(track_extractor_jni INTERFACE cpp)

# aac_sample_extractor_jni

add_library(aac_sample_extractor_jni SHARED src/main/cpp/aac_sample_extractor_jni.cpp)

target_link_libraries(aac_sample_extractor_jni
    PRIVATE steinwurf::jutils
    PRIVATE steinwurf::endian
    PRIVATE steinwurf::petro
    PRIVATE steinwurf::boost
    PRIVATE log)

target_compile_features(aac_sample_extractor_jni PUBLIC cxx_std_14)

target_include_directories(aac_sample_extractor_jni INTERFACE cpp)

# avc_sample_extractor_jni

add_library(avc_sample_extractor_jni SHARED src/main/cpp/avc_sample_extractor_jni.cpp)

target_link_libraries(avc_sample_extractor_jni
    PRIVATE steinwurf::jutils
    PRIVATE steinwurf::endian
    PRIVATE steinwurf::petro
    PRIVATE steinwurf::boost
    PRIVATE log)

target_compile_features(avc_sample_extractor_jni PUBLIC cxx_std_14)

target_include_directories(avc_sample_extractor_jni INTERFACE cpp)
