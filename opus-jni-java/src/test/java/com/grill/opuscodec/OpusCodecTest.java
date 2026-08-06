package com.grill.opuscodec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the bundled native binary end to end.
 *
 * <p>These tests are what actually proves that the Opus version, the JNI entry
 * points and the shared-library extraction still line up after an Opus, compiler
 * or JDK bump. They only pass when a native binary for the current platform is
 * present in the resources.
 */
class OpusCodecTest {

    private static final String EXPECTED_OPUS_VERSION = "1.6.1";

    private static final int SAMPLE_RATE = 48_000;
    private static final int CHANNELS = 2;

    /** 20 ms at 48 kHz. */
    private static final int FRAMES = 960;

    private static final int OPUS_APPLICATION_AUDIO = 2049;
    private static final int MAX_PACKET_BYTES = 4000;

    @BeforeAll
    static void loadNativeLibrary() throws IOException {
        OpusCodec.setupWithTemporaryFolder();
    }

    @Test
    void reportsBundledOpusVersion() {
        String version = OpusCodec.getOpusVersion();
        System.out.println("native Opus version: " + version);

        assertNotNull(version, "version string must not be null");
        assertTrue(version.contains(EXPECTED_OPUS_VERSION),
                "expected Opus " + EXPECTED_OPUS_VERSION + " but the native binary reports: " + version);
    }

    @Test
    void encodeDecodeRoundTripPreservesSignal() {
        OpusEncoder encoder = new OpusEncoder();
        OpusDecoder decoder = new OpusDecoder();

        try {
            encoder.initEncoder(SAMPLE_RATE, CHANNELS, OPUS_APPLICATION_AUDIO);
            encoder.setEncoderBitrate(64_000);
            encoder.setEncoderComplexity(10);
            decoder.initDecoder(SAMPLE_RATE, CHANNELS);

            short[] decoded = new short[FRAMES * CHANNELS];
            int loudestSample = 0;

            // Opus delays the signal by a few milliseconds, so the first packets
            // decode to near silence. Feeding a continuous tone across several
            // frames gets past that ramp-up.
            for (int frame = 0; frame < 10; frame++) {
                short[] pcm = tone(frame * FRAMES);
                byte[] packet = new byte[MAX_PACKET_BYTES];

                int packetLength = encoder.encode(pcm, FRAMES, packet);
                assertTrue(packetLength > 0, "encoder produced an empty packet");
                assertTrue(packetLength <= MAX_PACKET_BYTES, "encoder overran the packet buffer");

                int decodedFrames = decoder.decode(exactly(packet, packetLength), decoded, FRAMES);
                assertEquals(FRAMES, decodedFrames, "decoder returned an unexpected frame count");

                for (short sample : decoded) {
                    loudestSample = Math.max(loudestSample, Math.abs(sample));
                }
            }

            assertTrue(loudestSample > 1000,
                    "round trip produced near silence, loudest sample was " + loudestSample);
        } finally {
            encoder.closeEncoder();
            decoder.closeDecoder();
        }
    }

    @Test
    void decodesIntoDirectByteBuffer() {
        OpusEncoder encoder = new OpusEncoder();
        OpusDecoder decoder = new OpusDecoder();

        try {
            encoder.initEncoder(SAMPLE_RATE, CHANNELS, OPUS_APPLICATION_AUDIO);
            decoder.initDecoder(SAMPLE_RATE, CHANNELS);

            byte[] packet = new byte[MAX_PACKET_BYTES];
            int packetLength = encoder.encode(tone(0), FRAMES, packet);
            assertTrue(packetLength > 0, "encoder produced an empty packet");

            // The native side takes the packet length from the buffer's capacity,
            // so the input buffer has to be sized to the packet exactly.
            ByteBuffer encoded = ByteBuffer.allocateDirect(packetLength);
            encoded.put(packet, 0, packetLength);
            encoded.flip();

            ByteBuffer out = ByteBuffer
                    .allocateDirect(FRAMES * CHANNELS * Short.BYTES)
                    .order(ByteOrder.nativeOrder());

            assertEquals(FRAMES, decoder.decode(encoded, out, FRAMES),
                    "direct-buffer decode returned an unexpected frame count");
        } finally {
            encoder.closeEncoder();
            decoder.closeDecoder();
        }
    }

    @Test
    void decodingBeforeInitReportsErrorInsteadOfCrashing() {
        OpusDecoder decoder = new OpusDecoder();

        int result = decoder.decode(new byte[] {1, 2, 3}, new short[FRAMES * CHANNELS], FRAMES);

        assertTrue(result < 0, "expected a negative error code but got " + result);
    }

    /** Builds one frame of a 440 Hz tone, interleaved across all channels. */
    private static short[] tone(int startSample) {
        short[] pcm = new short[FRAMES * CHANNELS];

        for (int i = 0; i < FRAMES; i++) {
            double seconds = (startSample + i) / (double) SAMPLE_RATE;
            short value = (short) (Math.sin(2 * Math.PI * 440 * seconds) * 12_000);

            for (int channel = 0; channel < CHANNELS; channel++) {
                pcm[i * CHANNELS + channel] = value;
            }
        }

        return pcm;
    }

    /**
     * The array decode path reads the packet length from the array length, so a
     * partially filled buffer has to be trimmed before being handed over.
     */
    private static byte[] exactly(byte[] packet, int length) {
        byte[] trimmed = new byte[length];
        System.arraycopy(packet, 0, trimmed, 0, length);
        return trimmed;
    }
}
