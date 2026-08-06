############
# Add Opus #
############

# Opus is built in floating-point mode using its own SIMD defaults, which on
# x86-64 presume SSE and SSE2 (both are part of the architecture baseline) and
# leave SSE4.1/AVX2 runtime-detected, so older CPUs still work.
#
# Only the settings this project actually depends on are pinned here. Anything
# left unset follows upstream, so a future Opus bump does not silently inherit a
# stale override.
set(OPUS_FIXED_POINT OFF CACHE BOOL "" FORCE)
set(OPUS_ENABLE_FLOAT_API ON CACHE BOOL "" FORCE)

# The JNI library links Opus statically, so no separate shared object ships.
set(OPUS_BUILD_SHARED_LIBRARY OFF CACHE BOOL "" FORCE)

# Only the library itself is needed.
set(OPUS_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(OPUS_INSTALL_PKG_CONFIG_MODULE OFF CACHE BOOL "" FORCE)
set(OPUS_INSTALL_CMAKE_CONFIG_MODULE OFF CACHE BOOL "" FORCE)

# DRED and OSCE embed neural-network weights, adding megabytes of binary size
# for features this audio path does not use.
set(OPUS_DRED OFF CACHE BOOL "" FORCE)
set(OPUS_OSCE OFF CACHE BOOL "" FORCE)

add_subdirectory(opus)
