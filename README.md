# opus-jni

A JNI wrapper for the [Opus codec](https://opus-codec.org/), used as the audio decoder for
Remote Play. Originally created for [LabyMod](https://github.com/LabyMod/opus-jni) and since
adapted: the Java API is reduced to a separate encoder and decoder, a zero-copy direct
`ByteBuffer` decode path was added, and the native pointer lookups are cached.

[![CI](https://github.com/streamingdv/opus-jni/workflows/CI/badge.svg)](https://github.com/streamingdv/opus-jni/actions?query=workflow%3ACI)

## Coordinates

```groovy
implementation 'com.grill.opuscodec:opus-jni-java:1.0.0'
```

The published jar bundles the native binary for every supported platform, so a single
dependency works everywhere.

## Requirements

| | Version |
| --- | --- |
| JDK | 25 |
| Gradle | 9.6.1 (via the wrapper) |
| Opus | 1.6.1 |
| CMake | 3.16 or newer (only to build the native library) |

### Required runtime flag

The library calls `System.load` to load its native binary. That is a restricted method, so
consumers must run with:

```
--enable-native-access=ALL-UNNAMED
```

Without it JDK 24 and later print a warning, and a future release will refuse the call
outright. A library cannot enable this for itself, so it has to be set by the application.

## Supported platforms

The native binaries are built so they load on these minimums:

| Platform | Floor | How it is enforced |
| --- | --- | --- |
| Windows | 10 | `_WIN32_WINNT=0x0A00`, PE subsystem version checked in CI |
| Linux | Ubuntu 20.04 (glibc 2.31) | built in an `ubuntu:20.04` container, symbol versions checked in CI |
| macOS | 12.7 | `CMAKE_OSX_DEPLOYMENT_TARGET=12.0`, `minos` checked in CI |

Architectures: Windows x64, Linux x64, macOS x86_64 and arm64.

The Windows DLL links the CRT statically, so the Visual C++ redistributable is **not**
required on the target machine. CI fails if that ever regresses.

Only 64-bit is shipped. Windows x86 is not supported: JDK 24 removed the Windows 32-bit x86
port ([JEP 479](https://openjdk.org/jeps/479)), so no Java 25 runtime could load such a
binary. The `-P32bit` flag still exists in the build, but it is not built, tested or
published.

## Usage

Load the native library once per process, before creating any codec:

```java
OpusCodec.setupWithTemporaryFolder();
```

### Decoding

```java
OpusDecoder decoder = new OpusDecoder();
decoder.initDecoder(48000, 2);

short[] pcm = new short[960 * 2];          // 20 ms at 48 kHz, stereo
int frames = decoder.decode(packet, pcm, 960);

decoder.closeDecoder();
```

`decode` returns the number of decoded frames, or a negative Opus error code. Use
`OpusError.throwIfError(int)` to turn one into an exception.

A zero-copy variant takes direct buffers. The native side derives the packet length from the
input buffer's capacity, so that buffer has to be sized to the packet exactly:

```java
ByteBuffer encoded = ByteBuffer.allocateDirect(packetLength);
encoded.put(packet, 0, packetLength).flip();

ByteBuffer out = ByteBuffer
        .allocateDirect(960 * 2 * Short.BYTES)
        .order(ByteOrder.nativeOrder());

decoder.decode(encoded, out, 960);
```

### Encoding

```java
OpusEncoder encoder = new OpusEncoder();
encoder.initEncoder(48000, 2, 2049);       // 2049 = OPUS_APPLICATION_AUDIO
encoder.setEncoderBitrate(64000);
encoder.setEncoderComplexity(10);

byte[] packet = new byte[4000];
int packetLength = encoder.encode(pcm, 960, packet);

encoder.closeEncoder();
```

The array-based `decode` overloads read the packet length from the array length, so a
partially filled buffer must be trimmed to `packetLength` before being decoded.

### Which Opus is compiled in

```java
OpusCodec.getOpusVersion();   // "libopus 1.6.1"
```

## Good to know

- Opus is stateful, so every audio stream needs its own encoder and decoder instance.
- Supported sample rates are 8, 12, 16, 24 and 48 kHz.
- Always call `closeDecoder` / `closeEncoder`. They free the native allocation, and leaking
  them leaks native memory that the garbage collector cannot reclaim.
- See the [recommended bitrate settings](https://wiki.xiph.org/index.php?title=Opus_Recommended_Settings&mobileaction=toggle_view_desktop).

## Building

### Cloning

The Opus source is a submodule, so clone with `--recurse-submodules`, or run
`git submodule update --init` afterwards.

The submodule needs its tags: Opus derives its version with `git describe`, and a shallow
clone makes it report `0` instead of `1.6.1`.

### For your own platform

Install [CMake](https://cmake.org/download/) 3.16+ and a C compiler (Visual Studio on
Windows, gcc or clang elsewhere), then:

```
./gradlew opus-jni-java:build
```

The jar in `opus-jni-java/build/libs` only contains the native binary for the platform it was
built on. If CMake is not on the `PATH`, point at it with `-PcmakeExecutable=/path/to/cmake`.

### For all platforms

One machine per platform is needed. Run this on each:

```
./gradlew -PCI=true opus-jni-native:build
```

Collect the resulting binaries from `build/nativeBinaries` into a single directory on one
machine and run:

```
./gradlew -PnativeBinaryExternalDir=/path/to/binaries opus-jni-java:build
```

The binaries are named after the architecture suffix the loader looks for:

| Platform | File |
| --- | --- |
| Windows x64 | `opus-jni-native-64.dll` |
| Linux x64 | `libopus-jni-native-64.so` |
| macOS x86_64 | `libopus-jni-native-64.dylib` |
| macOS arm64 | `libopus-jni-native-arm64.dylib` |

In practice, letting [CI](./.github/workflows/ci.yml) do this is easier: it builds every
platform, verifies the OS floors, runs the codec tests on each one, and uploads the combined
jar as the `opus-jni-java` artifact.

### Tests

`opus-jni-java:test` runs an encode/decode round trip against the real native binary and
asserts the bundled Opus version. It needs a native binary for the current platform, so it
only runs as part of a build that has one.
