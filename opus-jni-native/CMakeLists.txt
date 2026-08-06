#############################
# Minimum supported tooling #
#############################
# 3.16 matches the floor declared by the bundled Opus. Anything lower is also
# rejected outright by CMake 4, which dropped compatibility with < 3.5.
cmake_minimum_required(VERSION 3.16)

###################
# Policy settings #
###################
# CMP0091 lets MSVC_RUNTIME_LIBRARY select the runtime instead of hard-coded
# flags. Both policies are NEW by default at 3.16, but stay explicit so the
# intent survives a future floor change.
cmake_policy(SET CMP0091 NEW)
cmake_policy(SET CMP0048 NEW)

####################
# Set architecture #
####################
# Compared against the variable name rather than its expansion: an unset
# CMAKE_OSX_ARCHITECTURES would otherwise expand to nothing and make if() fail
# with a syntax error instead of falling through to the default.
if(APPLE)
    if(CMAKE_OSX_ARCHITECTURES STREQUAL "x86_64")
        message(STATUS "Building for x86_64")
        set(CMAKE_OSX_ARCHITECTURES "x86_64" CACHE STRING "Build architectures for Mac" FORCE)
    elseif(CMAKE_OSX_ARCHITECTURES STREQUAL "arm64")
        message(STATUS "Building for arm64")
        set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "Build architectures for Mac" FORCE)
    else()
        message(STATUS "Fallback to arm64")
        set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "Build architectures for Mac" FORCE)
    endif()
endif()

#####################
# Pre project setup #
#####################
# Supported platform floor: macOS 12.7. Must be set before project() to apply.
set(CMAKE_OSX_DEPLOYMENT_TARGET "12.0" CACHE STRING "Minimum OS X deployment version" FORCE)

# Ensure the build type is always Release
set(CMAKE_BUILD_TYPE Release CACHE STRING "Force the build type to Release" FORCE)

#################
# Project setup #
#################
project(opus-jni-native LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

##############################
# Set global compile options #
##############################
if(UNIX OR MINGW)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fstack-protector")
endif()

# Opus is linked statically, so without this its symbols would be re-exported
# from this library and could collide with another Opus already loaded into the
# same process. JNIEXPORT carries default visibility, so the JNI entry points
# stay reachable.
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

# Supported platform floor: Windows 10.
if(WIN32)
    add_compile_definitions(_WIN32_WINNT=0x0A00 WINVER=0x0A00)
endif()

############################
# Add 3rd party libraries #
###########################
add_subdirectory(3rdparty)

################
# Requires JNI #
################
find_package(JNI REQUIRED)

#####################
# Configure project #
#####################
if(NOT DEFINED GENERATED_JNI_HEADERS_DIR)
    message(FATAL_ERROR "Please run cmake with -DGENERATED_JNI_HEADERS_DIR=/path/to/generated/headers or simply use the"
            " gradle build task")
endif()

###################
# Project sources #
###################
set(OPUS_JNI_NATIVE_SOURCES
        src/opus_jni.c)

##############
# Add target #
##############
add_library(opus-jni-native SHARED ${OPUS_JNI_NATIVE_SOURCES})

# Only the JNI headers are needed, deliberately not JNI_LIBRARIES. Every call
# this library makes goes through the function table in the JNIEnv it is handed,
# so nothing is imported from the JVM; only a launcher calling JNI_CreateJavaVM
# would have to link against it. Linking it anyway would add a load-time
# dependency on libjvm that the loader has to resolve by name.
target_link_libraries(opus-jni-native PUBLIC opus)
target_include_directories(opus-jni-native PUBLIC ${JNI_INCLUDE_DIRS} ${GENERATED_JNI_HEADERS_DIR})

if(MSVC)
    # Static CRT, so the shipped DLL does not require the Visual C++
    # redistributable to be installed on the target machine.
    set_property(
            TARGET opus
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded"
    )

    set_property(
            TARGET opus-jni-native
            PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded"
    )
endif()
