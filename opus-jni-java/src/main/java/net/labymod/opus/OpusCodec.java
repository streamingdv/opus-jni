package com.grill.opuscodec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;

public class OpusCodec {

    /* Native pointer to OpusDecoder */
    private long addressDecoder;

    /* Native pointer to OpusEncoder */
    private long addressEncoder;

    /***********************/
    /*** decoder methods ***/
    /***********************/

    private native int nativeInitDecoder(int samplingRate, int numberOfChannels);

    private native int nativeDecodeShorts(byte[] in, short[] out, int frames);

    private native int nativeDecodeBytes(byte[] in, byte[] out, int frames);

    private native int nativeDecodeByteBuffer(ByteBuffer encodedBuffer, final ByteBuffer buffer, int frames);

    private native boolean nativeReleaseDecoder();

    void initDecoder(int sampleRate, int channels) {
        OpusError.throwIfError(this.nativeInitDecoder(sampleRate, channels));
    }

    int decode(byte[] encodedBuffer, short[] buffer, int frames) {
        return this.nativeDecodeShorts(encodedBuffer, buffer, frames);
    }

    int decode(byte[] encodedBuffer, byte[] buffer, int frames) {
        return this.nativeDecodeBytes(encodedBuffer, buffer, frames);
    }

    int decode(ByteBuffer encodedBuffer, final ByteBuffer buffer, final int frames) {
        return this.nativeDecodeByteBuffer(encodedBuffer, buffer, frames);
    }

    void closeDecoder() {
        this.nativeReleaseDecoder();
    }

    /***********************/
    /*** encoder methods ***/
    /***********************/

    private native int nativeInitEncoder(int samplingRate, int numberOfChannels, int application);

    private native int nativeSetBitrate(int bitrate);

    private native int nativeSetComplexity(int complexity);

    private native int nativeEncodeShorts(short[] in, int frames, byte[] out);

    private native int nativeEncodeBytes(byte[] in, int frames, byte[] out);

    private native boolean nativeReleaseEncoder();

    void initEncoder(int sampleRate, int channels, int application) {
        OpusError.throwIfError(this.nativeInitEncoder(sampleRate, channels, application));
    }

    void setEncoderBitrate(int bitrate) {
        OpusError.throwIfError(this.nativeSetBitrate(bitrate));
    }

    void setEncoderComplexity(int complexity) {
        OpusError.throwIfError(this.nativeSetComplexity(complexity));
    }

    int encode(short[] buffer, int frames, byte[] out) {
        return OpusError.throwIfError(this.nativeEncodeShorts(buffer, frames, out));
    }

    int encode(byte[] buffer, int frames, byte[] out) {
        return OpusError.throwIfError(this.nativeEncodeBytes(buffer, frames, out));
    }

    void closeEncoder() {
        this.nativeReleaseEncoder();
    }

    /************************/
    /*** load lib methods ***/
    /************************/

    public static void loadNative(File directory) throws IOException {
        loadNative(directory, true);
    }

    public static void loadNative(File directory, boolean allowArm) throws IOException {
        String nativeLibraryName = getNativeLibraryName(allowArm);
        InputStream source = OpusCodec.class.getResourceAsStream("/native-binaries/" + nativeLibraryName);
        if (source == null) {
            throw new IOException("Could not find native library " + nativeLibraryName);
        }

        Path destination = directory.toPath().resolve(nativeLibraryName);
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException ignored) {
            // The file already exists, or we don't have permission to write to the directory
        }
        System.load(new File(directory, nativeLibraryName).getAbsolutePath());
    }

    /**
     * Extract the native library and load it
     *
     * @throws IOException          In case an error occurs while extracting the native library
     * @throws UnsatisfiedLinkError In case the native libraries fail to load
     */
    public static void setupWithTemporaryFolder() throws IOException {
        File temporaryDir = Files.createTempDirectory("opus-jni").toFile();
        temporaryDir.deleteOnExit();

        try {
            loadNative(temporaryDir);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();

            // Try without ARM support
            loadNative(temporaryDir, false);
        }
    }

    /***********************/
    /*** private methods ***/
    /***********************/

    private static String getNativeLibraryName(boolean allowArm) {
        String bitnessArch = System.getProperty("os.arch").toLowerCase();
        String bitnessDataModel = System.getProperty("sun.arch.data.model", null);

        boolean is64bit = bitnessArch.contains("64") || (bitnessDataModel != null && bitnessDataModel.contains("64"));
        String arch = bitnessArch.startsWith("aarch") && allowArm ? "arm" : "";

        if (is64bit) {
            String library64 = processLibraryName("opus-jni-native-" + arch + "64");
            if (hasResource("/native-binaries/" + library64)) {
                return library64;
            }
        } else {
            String library32 = processLibraryName("opus-jni-native-" + arch + "32");
            if (hasResource("/native-binaries/" + library32)) {
                return library32;
            }
        }

        String library = processLibraryName("opus-jni-native");
        if (!hasResource("/native-binaries/" + library)) {
            throw new NoSuchElementException("No binary for the current system found, even after trying bit neutral names");
        } else {
            return library;
        }
    }

    private static String processLibraryName(String library) {
        String systemName = System.getProperty("os.name", "bare-metal?").toLowerCase();

        if (systemName.contains("nux") || systemName.contains("nix")) {
            return "lib" + library + ".so";
        } else if (systemName.contains("mac")) {
            return "lib" + library + ".dylib";
        } else if (systemName.contains("windows")) {
            return library + ".dll";
        } else {
            throw new NoSuchElementException("No native library for system " + systemName);
        }
    }

    private static boolean hasResource(String resource) {
        return OpusCodec.class.getResource(resource) != null;
    }

}
